"""
MatchEngineService — automated queue-based matching (MATCH-01 through MATCH-04).

Responsibilities:
- Scans WAITING queue entries grouped by (region_id, sport_id, skill_level).
- Acquires row-level locks via SKIP LOCKED to prevent concurrent double-matching.
- Creates Match + MatchPlayer records when a compatible pair is found.
- Delegates ground allocation to BookingService after match formation.
- Rolls back gracefully when no ground is available, keeping users in WAITING.
"""

import logging
from datetime import date, datetime, timedelta

from core.database.db_connection import SessionLocal
from modules.matchmaking.repository.queue_entry_repository import (
    QueueEntryRepository,
    queue_entry_repository as _default_queue_repo,
)
from modules.match.model.match_model import Match, MatchPenalty, MatchPlayer
from modules.booking.service.booking_service import BookingService
from modules.cart_type.repository.cart_type_repository import (
    cart_type_repository as _default_cart_type_repo,
)
from modules.timeslot.repository.timeslot_repository import (
    timeslot_repository as _default_timeslot_repo,
)
from modules.user.model.user_model import User
from modules.user.repository.user_repository import user_repository as _default_user_repo

logger = logging.getLogger(__name__)


class MatchEngineService:
    """
    Core matching algorithm.

    Design decisions:
    - One DB transaction per matchable group (isolation, no cross-group interference).
    - SKIP LOCKED on queue rows — concurrent workers skip rows already being processed.
    - Exact match strictness: BEGINNER only pairs with BEGINNER (D-02).
    - FIFO: oldest created_at entry becomes creator_id (D-04).
    - Failed booking → full rollback so QueueEntry rows remain WAITING (D-03).
    """

    def __init__(
        self,
        queue_repo: QueueEntryRepository = None,
        booking_service: BookingService = None,
        cart_type_repo=None,
        timeslot_repo=None,
        session_factory=None,
        user_repository=None,
    ):
        self._queue_repo = queue_repo or _default_queue_repo
        self._booking_service = booking_service or BookingService()
        self._cart_type_repo = cart_type_repo or _default_cart_type_repo
        self._timeslot_repo = timeslot_repo or _default_timeslot_repo
        self._session_factory = session_factory or SessionLocal
        self._user_repository = user_repository or _default_user_repo

    # ── Public API ────────────────────────────────────────────────────

    def process_matching_cycle(self) -> list[dict]:
        """
        Run one full matching sweep across all eligible groups.

        Returns a list of outcome dicts — one per group attempted — for
        observability (logs / scheduler feedback).

        Each outcome has the shape:
            {"group": (region_id, sport_id, skill_level), "result": "matched" | "no_ground" | "skipped", ...}
        """
        outcomes = []

        # Phase 0: cancel stale MATCHED matches and issue no-show penalties
        cleanup_session = self._session_factory()
        try:
            self.cleanup_stale_matches(cleanup_session)
            cleanup_session.commit()
        except Exception as exc:
            cleanup_session.rollback()
            logger.exception("MatchEngineService: cleanup_stale_matches failed — %s", exc)
        finally:
            cleanup_session.close()

        # Phase 1: discover matchable groups (no transaction needed for read)
        discovery_session = self._session_factory()
        try:
            groups = self._queue_repo.get_matchable_groups(discovery_session)
        finally:
            discovery_session.close()

        if not groups:
            logger.debug("MatchEngineService: no matchable groups found — queue idle")
            return outcomes

        logger.info("MatchEngineService: %d matchable group(s) found", len(groups))

        # Phase 2: attempt matching per group inside isolated transactions
        for region_id, sport_id, skill_level in groups:
            outcome = self._attempt_match_for_group(region_id, sport_id, skill_level)
            outcomes.append(outcome)
            logger.info(
                "Group (%s, %s, %s) → %s",
                region_id, sport_id, skill_level, outcome["result"],
            )

        return outcomes

    # ── Internal helpers ──────────────────────────────────────────────

    def cleanup_stale_matches(self, session) -> None:
        """
        Cancel MATCHED matches where players failed to arrive within 20 minutes.

        For every Match that is still in MATCHED status and whose created_at is
        older than (now - 20 minutes):

        1. Cancel the associated booking via BookingService (D-01).
        2. For ghost_players (has_arrived=False):
           - Fetch user and check ghost_strikes.
           - If strikes < 2: increment strikes (excuse — no penalty).
           - If strikes >= 2: reset strikes to 0 and issue a MatchPenalty (4h block).
        3. For arrived_players (has_arrived=True):
           - Re-queue with created_at = now - 2h for FIFO priority (D-02).
           - Flag entry with reason=RE_QUEUE_OPPONENT_NO_SHOW for frontend toast (D-03).
        4. Set Match status to CANCELLED_NO_SHOW.

        The caller is responsible for committing or rolling back the session.
        """
        deadline = datetime.utcnow() - timedelta(minutes=20)
        stale_matches = (
            session.query(Match)
            .filter(Match.status == "MATCHED", Match.created_at < deadline)
            .all()
        )

        if not stale_matches:
            return

        penalty_expiry = datetime.utcnow() + timedelta(hours=4)
        priority_timestamp = datetime.utcnow() - timedelta(hours=2)

        for match in stale_matches:
            # D-01: cancel the booking to release ground inventory
            if match.booking_id:
                try:
                    self._booking_service.cancel_booking(match.booking_id)
                    logger.info(
                        "Match %d: booking %d cancelled (ground released)",
                        match.id, match.booking_id,
                    )
                except Exception as exc:
                    logger.warning(
                        "Match %d: failed to cancel booking %d — %s",
                        match.id, match.booking_id, exc,
                    )

            all_players = (
                session.query(MatchPlayer)
                .filter(MatchPlayer.match_id == match.id)
                .all()
            )

            arrived_players = [p for p in all_players if p.has_arrived]
            ghost_players = [p for p in all_players if not p.has_arrived]

            # Process ghost players — 2-strike rule
            for player in ghost_players:
                user = session.query(User).filter(User.id == player.user_id).first()
                if user is None:
                    logger.warning(
                        "Match %d: ghost player user_id=%d not found — skipping",
                        match.id, player.user_id,
                    )
                    continue

                current_strikes = user.ghost_strikes or 0
                if current_strikes < 2:
                    # Excuse: increment strikes, no penalty
                    self._user_repository.increment_ghost_strikes(player.user_id, session=session)
                    logger.info(
                        "Match %d: user_id=%d excused (strike %d/2)",
                        match.id, player.user_id, current_strikes + 1,
                    )
                else:
                    # 3rd offence: reset strikes and issue MatchPenalty
                    self._user_repository.reset_ghost_strikes(player.user_id, session=session)
                    penalty = MatchPenalty(
                        user_id=player.user_id,
                        match_id=match.id,
                        reason="Ghosting",
                        expires_at=penalty_expiry,
                    )
                    session.add(penalty)
                    logger.info(
                        "Match %d: MatchPenalty issued for user_id=%d (strikes reset, 4h block)",
                        match.id, player.user_id,
                    )

            # Process arrived players — re-queue with priority
            for player in arrived_players:
                # Look up match player's original queue entry to get sport/skill context
                original_entry = (
                    session.query(MatchPlayer)
                    .filter(
                        MatchPlayer.match_id == match.id,
                        MatchPlayer.user_id == player.user_id,
                    )
                    .first()
                )
                self._queue_repo.create(
                    {
                        "user_id": player.user_id,
                        "region_id": match.region_id,
                        "sport_id": match.sport_id,
                        "skill_level": match.skill_level or "BEGINNER",
                        "status": "WAITING",
                        "reason": "RE_QUEUE_OPPONENT_NO_SHOW",
                        "created_at": priority_timestamp,
                    },
                    session=session,
                )
                logger.info(
                    "Match %d: user_id=%d re-queued with priority (created_at=%s, reason=RE_QUEUE_OPPONENT_NO_SHOW)",
                    match.id, player.user_id, priority_timestamp.isoformat(),
                )

            match.status = "CANCELLED_NO_SHOW"
            logger.info(
                "Match %d marked CANCELLED_NO_SHOW (%d ghost(s), %d re-queued)",
                match.id, len(ghost_players), len(arrived_players),
            )

    def _attempt_match_for_group(
        self, region_id: int, sport_id: int, skill_level: str
    ) -> dict:
        """
        Attempt to form one match for a (region, sport, skill_level) group.

        Opens a new transaction.  If exactly 2 rows are locked:
        1. Creates a Match in MATCHED status.
        2. Creates two MatchPlayer records.
        3. Marks both QueueEntry rows as MATCHED.
        4. Attempts to secure a booking via BookingService.
        5. Commits on success, rolls back on any failure.

        If booking fails (ValueError), rolls back so users stay WAITING.
        """
        session = self._session_factory()
        group_key = (region_id, sport_id, skill_level)
        try:
            entries = self._queue_repo.find_and_lock_compatible_pair(
                region_id, sport_id, skill_level, session
            )

            if len(entries) < 2:
                # Another worker may have just grabbed these rows — skip silently
                session.rollback()
                return {"group": group_key, "result": "skipped", "reason": "insufficient_locked_rows"}

            entry_a, entry_b = entries[0], entries[1]  # FIFO: a is older
            creator_id = entry_a.user_id

            # 4a. Create the Match record
            match = Match(
                created_by=creator_id,
                sport_id=sport_id,
                region_id=region_id,
                cart_type_id=self._resolve_cart_type_id(region_id),
                skill_level=skill_level,
                max_players=2,
                joined_players=2,
                status="MATCHED",
            )
            session.add(match)
            session.flush()  # assign match.id without committing

            # 4b. Create two MatchPlayer records
            for entry in (entry_a, entry_b):
                player = MatchPlayer(match_id=match.id, user_id=entry.user_id)
                session.add(player)

            # 4c. Mark both QueueEntry rows as MATCHED
            entry_a.status = "MATCHED"
            entry_b.status = "MATCHED"
            session.flush()

            # 4d. Secure a booking — may raise ValueError if no ground available
            self._secure_booking_for_match(match, creator_id, session)

            session.commit()
            session.refresh(match)
            return {
                "group": group_key,
                "result": "matched",
                "match_id": match.id,
                "players": [entry_a.user_id, entry_b.user_id],
            }

        except ValueError as exc:
            session.rollback()
            logger.warning(
                "Group %s: booking failed, users returned to WAITING — %s",
                group_key, exc,
            )
            return {"group": group_key, "result": "no_ground", "reason": str(exc)}

        except Exception as exc:
            session.rollback()
            logger.exception("Group %s: unexpected error during matching — %s", group_key, exc)
            return {"group": group_key, "result": "error", "reason": str(exc)}

        finally:
            session.close()

    def _resolve_cart_type_id(self, region_id: int) -> int:
        """
        Return the first active cart type available for this region.

        Falls back to the globally first active cart type if no region-specific
        lookup is available.  Raises ValueError if none exists.
        """
        all_types = self._cart_type_repo.find_all()
        active_types = [ct for ct in all_types if ct.get("is_active", True)]
        if not active_types:
            raise ValueError("No active cart type found for region.")
        return active_types[0]["id"]

    def _find_earliest_timeslot_id(self, region_id: int) -> int:
        """
        Return the id of the earliest available timeslot for today in the given region.

        Raises ValueError if no timeslot is found for today.
        """
        today = date.today().isoformat()
        all_timeslots = self._timeslot_repo.find_all()
        # Filter to today's timeslots for this region, sort by start_time
        todays_slots = sorted(
            [
                ts for ts in all_timeslots
                if ts.get("location_id") == region_id and ts.get("date") == today
            ],
            key=lambda ts: ts.get("start_time", ""),
        )
        if not todays_slots:
            raise ValueError(
                f"No timeslots found for region {region_id} on {today}."
            )
        return todays_slots[0]["id"]

    def _secure_booking_for_match(
        self, match: Match, creator_id: int, session
    ) -> None:
        """
        Atomically create a CONFIRMED booking (with a claimed cart) for the match.

        Uses BookingService.create_instant_match_booking() which:
        - Claims an AVAILABLE cart immediately (no manual payment-approval step)
        - Sets booking status to CONFIRMED in one atomic transaction

        The returned booking's cart_id is written back to match.cart_id so that
        MatchService.finish_match() can release the ground when the game ends.

        On ValueError (no ground, no fee config, etc.):
        the caller's except block catches the exception and rolls back the
        outer transaction so QueueEntry rows remain WAITING (D-03).
        """
        timeslot_id = self._find_earliest_timeslot_id(match.region_id)

        payload = {
            "user_id": creator_id,
            "region_id": match.region_id,
            "cart_type_id": match.cart_type_id,
            "timeslot_id": timeslot_id,
        }

        booking = self._booking_service.create_instant_match_booking(payload)

        # Write the assigned cart and booking back into the Match record
        match.cart_id = booking.get("cart_id")
        match.cart_type_id = booking.get("cart_type_id", match.cart_type_id)
        match.booking_id = booking.get("id")
        logger.info(
            "Match %d: instant booking %d (cart %s) CONFIRMED for creator user %d",
            match.id, booking["id"], match.cart_id, creator_id,
        )



# ── Shared singleton ──────────────────────────────────────────────────
match_engine_service = MatchEngineService()
