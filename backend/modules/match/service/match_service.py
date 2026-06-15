from core.base.base_service import BaseService
from modules.match.repository.match_repository import (
    match_repository as _default_match_repo,
)
from modules.match.repository.match_event_repository import (
    match_event_repository as _default_event_repo,
)
from modules.cart.repository.cart_repository import (
    cart_repository as _default_cart_repo,
)
from modules.timeslot.repository.timeslot_repository import (
    timeslot_repository as _default_timeslot_repo,
)
from modules.cart_type.repository.cart_type_repository import (
    cart_type_repository as _default_cart_type_repo,
)
from modules.location.repository.location_repository import (
    location_repository as _default_location_repo,
)


class MatchService(BaseService):
    """
    Business logic layer for Match operations.

    Key rules enforced here:
    - Cart only goes BUSY when match is FULL.
    - join_match uses SELECT FOR UPDATE to prevent race-condition overflow.
    - Double-booking (same cart+timeslot) is blocked at service level.
    - Ground fairness: least-recently-used cart is picked first.
    - Capacity: max_players must not exceed timeslot.capacity.
    """

    def __init__(
        self,
        match_repository=None,
        cart_repository=None,
        timeslot_repository=None,
        cart_type_repository=None,
        location_repository=None,
        event_repository=None,
    ):
        super().__init__()
        self.match_repo = match_repository or _default_match_repo
        self.cart_repo = cart_repository or _default_cart_repo
        self.timeslot_repo = timeslot_repository or _default_timeslot_repo
        self.cart_type_repo = cart_type_repository or _default_cart_type_repo
        self.location_repo = location_repository or _default_location_repo
        self.event_repo = event_repository or _default_event_repo

    # ── Helpers ───────────────────────────────────────────────────────

    def _get_timeslot(self, timeslot_id: int) -> dict:
        ts = self.timeslot_repo.find_by_id(timeslot_id)
        if not ts:
            raise ValueError("Timeslot not found.")
        return ts

    def _get_cart_type(self, cart_type_id: int) -> dict:
        ct = self.cart_type_repo.find_by_id(cart_type_id)
        if not ct:
            raise ValueError("Sport (cart type) not found.")
        if not ct.get("is_active", True):
            raise ValueError("This sport is currently inactive.")
        return ct

    def _get_region(self, region_id: int) -> dict:
        r = self.location_repo.find_by_id(region_id)
        if not r:
            raise ValueError("Region not found.")
        return r

    # ── Create ────────────────────────────────────────────────────────

    def create_match(self, user_id: int, data: dict) -> dict:
        """
        Create a new match for a user.

        Flow:
        1. Validate timeslot, sport, region
        2. Enforce max_players <= timeslot.capacity
        3. Find least-recently-used AVAILABLE cart (fairness rotation)
        4. Guard: no existing OPEN/FULL match for same (cart, timeslot)
        5. Create match (cart stays AVAILABLE — not locked yet)
        6. Add creator to MatchPlayer
        """
        timeslot = self._get_timeslot(data["timeslot_id"])
        self._get_cart_type(data["cart_type_id"])
        self._get_region(data["region_id"])

        # Capacity validation
        max_players = data["max_players"]
        ts_capacity = timeslot.get("capacity", 0)
        if max_players > ts_capacity:
            raise ValueError(
                f"max_players ({max_players}) cannot exceed timeslot capacity ({ts_capacity})."
            )

        # Use a transaction to safely find cart and check conflicts
        session = self.match_repo._session_factory()
        try:
            # Find fairness-rotated available cart
            cart = self.match_repo.find_available_cart(
                data["region_id"], data["cart_type_id"], session=session
            )
            if cart is None:
                raise ValueError(
                    "No available ground found for this sport and region. Try a different timeslot."
                )

            # Double-booking guard: same cart + same timeslot
            if self.match_repo.has_conflicting_match(cart.id, data["timeslot_id"]):
                raise ValueError(
                    "This ground is already booked for the selected timeslot. "
                    "Please choose a different timeslot or wait for another ground."
                )

            # Create match record
            match_data = {
                "created_by": user_id,
                "region_id": data["region_id"],
                "cart_type_id": data["cart_type_id"],
                "cart_id": cart.id,
                "timeslot_id": data["timeslot_id"],
                "skill_level": data.get("skill_level"),
                "max_players": max_players,
            }
            match = self.match_repo.create(match_data)

            # Add creator to MatchPlayer inside existing session
            # (create() already used its own session, open a new call)
            self.match_repo.add_player_standalone(match["id"], user_id)

            session.commit()

            self.event_repo.log(
                match["id"],
                "MATCH_CREATED",
                user_id=user_id,
                meta={"max_players": data["max_players"]},
            )
            self.event_repo.log(match["id"], "PLAYER_JOINED", user_id=user_id)
            return match
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    # ── Join ──────────────────────────────────────────────────────────

    def join_match(self, user_id: int, match_id: int) -> dict:
        """
        Join an existing match.

        Uses SELECT FOR UPDATE row lock to prevent race-condition overflow.

        Flow (all inside one transaction):
        1. Lock match row
        2. Validate status == OPEN
        3. Validate joined_players < max_players
        4. Skill level enforcement (if set on match)
        5. Guard: user not already joined
        6. Add to MatchPlayer, increment count
        7. If full → status = FULL, cart → BUSY
        8. Commit
        """
        session = self.match_repo._session_factory()
        try:
            # 1. Row lock
            match_orm = self.match_repo.find_by_id_orm(match_id, session)
            if match_orm is None:
                raise ValueError("Match not found.")

            # 2. Status check
            if match_orm.status != "OPEN":
                raise ValueError(f"Match is not joinable (status: {match_orm.status}).")

            # 3. Capacity check
            if match_orm.joined_players >= match_orm.max_players:
                raise ValueError("This match is already full.")

            # 4. Skill level enforcement
            if match_orm.skill_level:
                # MVP: client sends user skill level; strict enforcement is future work.
                # The field is available on match for display — no strict block for now.
                pass

            # 5. Double-join guard
            if self.match_repo.has_player(match_id, user_id):
                raise ValueError("You have already joined this match.")

            # 6. Add player + increment
            self.match_repo.add_player(match_id, user_id, session)
            self.match_repo.increment_player_count(match_id, session)

            # 7. Check if full → lock cart
            session.refresh(match_orm)
            if match_orm.joined_players >= match_orm.max_players:
                match_orm.status = "FULL"
                session.flush()

                # Lock the cart (BUSY)
                if match_orm.cart_id:
                    from modules.cart.model.cart_model import Cart

                    cart = (
                        session.query(Cart).filter(Cart.id == match_orm.cart_id).first()
                    )
                    if cart:
                        from datetime import datetime

                        cart.status = "BUSY"
                        cart.updated_at = datetime.utcnow()
                        session.flush()

            # 8. Commit
            session.commit()
            session.refresh(match_orm)
            result = match_orm.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

        self.event_repo.log(match_id, "PLAYER_JOINED", user_id=user_id)
        return result

    # ── Leave ─────────────────────────────────────────────────────────

    def leave_match(self, user_id: int, match_id: int) -> dict:
        """
        Leave a match.

        Rules:
        - Cannot leave a FULL match (committed players).
        - If creator leaves → match is CANCELLED, cart freed.
        - Otherwise → remove from MatchPlayer, decrement count.
        """
        session = self.match_repo._session_factory()
        try:
            from modules.match.model.match_model import Match as MatchORM

            match_orm = (
                session.query(MatchORM)
                .filter(MatchORM.id == match_id)
                .with_for_update()
                .first()
            )
            if match_orm is None:
                raise ValueError("Match not found.")

            if match_orm.status == "FULL":
                raise ValueError("Cannot leave a match that is already full.")

            if match_orm.status not in ("OPEN",):
                raise ValueError(
                    f"Match is not in a leavable state (status: {match_orm.status})."
                )

            if not self.match_repo.has_player(match_id, user_id):
                raise ValueError("You are not a member of this match.")

            if match_orm.created_by == user_id:
                # Creator leaving → cancel and free cart
                match_orm.status = "CANCELLED"
                if match_orm.cart_id:
                    from modules.cart.model.cart_model import Cart
                    from datetime import datetime

                    cart = (
                        session.query(Cart).filter(Cart.id == match_orm.cart_id).first()
                    )
                    if cart:
                        cart.status = "AVAILABLE"
                        cart.updated_at = datetime.utcnow()
                        session.flush()
            else:
                # Regular player leaving
                self.match_repo.remove_player(match_id, user_id, session)
                self.match_repo.decrement_player_count(match_id, session)

            is_creator_leaving = match_orm.created_by == user_id
            from datetime import datetime

            match_orm.updated_at = datetime.utcnow()
            session.flush()
            session.commit()
            session.refresh(match_orm)
            result = match_orm.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

        if is_creator_leaving:
            self.event_repo.log(
                match_id,
                "MATCH_CANCELLED",
                user_id=user_id,
                meta={"reason": "creator_left"},
            )
        else:
            self.event_repo.log(match_id, "PLAYER_LEFT", user_id=user_id)
        return result

    # ── Cancel ────────────────────────────────────────────────────────

    def cancel_match(self, user_id: int, match_id: int, is_admin: bool = False) -> dict:
        """
        Cancel a match. Only the creator or an admin can cancel.
        Frees the cart back to AVAILABLE.
        """
        session = self.match_repo._session_factory()
        try:
            from modules.match.model.match_model import Match as MatchORM

            match_orm = (
                session.query(MatchORM)
                .filter(MatchORM.id == match_id)
                .with_for_update()
                .first()
            )
            if match_orm is None:
                raise ValueError("Match not found.")

            if not is_admin and match_orm.created_by != user_id:
                raise ValueError(
                    "Only the match creator or an admin can cancel this match."
                )

            if match_orm.status in ("COMPLETED", "CANCELLED"):
                raise ValueError(f"Match is already {match_orm.status}.")

            match_orm.status = "CANCELLED"

            if match_orm.cart_id:
                from modules.cart.model.cart_model import Cart
                from datetime import datetime

                cart = session.query(Cart).filter(Cart.id == match_orm.cart_id).first()
                if cart:
                    cart.status = "AVAILABLE"
                    cart.updated_at = datetime.utcnow()

            # Free any captain assigned to this match
            from modules.captain.repository.captain_repository import captain_repository
            captain_repository.release_captain_for_match(match_id, session)

            from datetime import datetime

            match_orm.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(match_orm)
            result = match_orm.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

        self.event_repo.log(
            match_id, "MATCH_CANCELLED", user_id=user_id, meta={"by_admin": is_admin}
        )
        return result

    # ── Complete (Admin) ──────────────────────────────────────────────

    def complete_match(self, match_id: int) -> dict:
        """
        Admin force-completes a match. Frees the cart.
        """
        session = self.match_repo._session_factory()
        try:
            from modules.match.model.match_model import Match as MatchORM

            match_orm = (
                session.query(MatchORM)
                .filter(MatchORM.id == match_id)
                .with_for_update()
                .first()
            )
            if match_orm is None:
                raise ValueError("Match not found.")

            if match_orm.status in ("COMPLETED", "CANCELLED"):
                raise ValueError(f"Match is already {match_orm.status}.")

            match_orm.status = "COMPLETED"

            if match_orm.cart_id:
                from modules.cart.model.cart_model import Cart
                from datetime import datetime

                cart = session.query(Cart).filter(Cart.id == match_orm.cart_id).first()
                if cart:
                    cart.status = "AVAILABLE"
                    cart.updated_at = datetime.utcnow()

            from datetime import datetime

            match_orm.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(match_orm)
            return match_orm.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    # ── List ──────────────────────────────────────────────────────────

    def list_matches(
        self, cart_type_id: int = None, region_id: int = None
    ) -> list[dict]:
        """Return all OPEN matches whose timeslot is in the future."""
        matches = self.match_repo.find_open_matches(
            cart_type_id=cart_type_id, region_id=region_id
        )
        return self._enrich_matches(matches)

    def list_all_matches(self) -> list[dict]:
        """Return all matches across all statuses (admin use). Enriched with names."""
        matches = self.match_repo.find_all_matches()
        return self._enrich_matches(matches)

    # ── Arrive ────────────────────────────────────────────────────────

    def arrive_match(
        self, user_id: int, match_id: int, user_lat: float, user_lng: float
    ) -> dict:
        """
        Mark a player as arrived at the match ground.

        Flow:
        1. Lock match row, validate status (MATCHED or ARRIVED).
        2. Verify user is a player in the match.
        3. If cart has GPS coordinates, validate proximity (~500m radius).
        4. Set MatchPlayer.has_arrived = True.
        5. If all players arrived -> status = IN_PROGRESS, else -> ARRIVED.
        """
        session = self.match_repo._session_factory()
        try:
            from modules.match.model.match_model import Match as MatchORM, MatchPlayer
            from modules.cart.model.cart_model import Cart

            match_orm = (
                session.query(MatchORM)
                .filter(MatchORM.id == match_id)
                .with_for_update()
                .first()
            )
            if match_orm is None:
                raise ValueError("Match not found.")

            if match_orm.status not in ("MATCHED", "ARRIVED"):
                raise ValueError(
                    f"Cannot arrive at match in status '{match_orm.status}'. "
                    "Match must be MATCHED or ARRIVED."
                )

            # Verify user is a player
            player = (
                session.query(MatchPlayer)
                .filter(
                    MatchPlayer.match_id == match_id,
                    MatchPlayer.user_id == user_id,
                )
                .first()
            )
            if player is None:
                raise ValueError("You are not a player in this match.")

            if player.has_arrived:
                raise ValueError("You have already marked your arrival.")

            # GPS proximity check (if cart has coordinates)
            if match_orm.cart_id:
                cart = session.query(Cart).filter(Cart.id == match_orm.cart_id).first()
                if cart and cart.latitude is not None and cart.longitude is not None:
                    from core.config.app_config import get_float

                    proximity = get_float("MATCH_PROXIMITY_DEGREES")
                    lat_diff = abs(user_lat - cart.latitude)
                    lng_diff = abs(user_lng - cart.longitude)
                    if lat_diff > proximity or lng_diff > proximity:
                        raise ValueError(
                            "You are too far from the ground. "
                            "Please move closer to mark your arrival."
                        )

            # Mark player as arrived
            player.has_arrived = True
            session.flush()

            # Check if all players have arrived
            all_players = (
                session.query(MatchPlayer)
                .filter(MatchPlayer.match_id == match_id)
                .all()
            )
            all_arrived = all(p.has_arrived for p in all_players)

            from datetime import datetime

            now = datetime.utcnow()
            if all_arrived and len(all_players) >= match_orm.max_players:
                match_orm.status = "IN_PROGRESS"
                match_orm.started_at = now
            else:
                match_orm.status = "ARRIVED"
            match_orm.updated_at = now

            session.commit()
            session.refresh(match_orm)
            new_status = match_orm.status
            result = match_orm.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

        self.event_repo.log(match_id, "PLAYER_ARRIVED", user_id=user_id)
        if new_status == "IN_PROGRESS":
            self.event_repo.log(match_id, "MATCH_STARTED")
        return result

    # ── Finish ────────────────────────────────────────────────────────

    def finish_match(self, user_id: int, match_id: int) -> dict:
        """
        Mark a match as completed by a player.

        Flow:
        1. Lock match row, verify user is a player.
        2. Status must not be COMPLETED or CANCELLED.
        3. Set match status to COMPLETED.
        4. Free the assigned cart (set status back to AVAILABLE).
        5. Trigger split payment creation (one payment per player).
        """
        session = self.match_repo._session_factory()
        try:
            from modules.match.model.match_model import Match as MatchORM, MatchPlayer
            from modules.cart.model.cart_model import Cart
            from datetime import datetime

            match_orm = (
                session.query(MatchORM)
                .filter(MatchORM.id == match_id)
                .with_for_update()
                .first()
            )
            if match_orm is None:
                raise ValueError("Match not found.")

            # Verify user is a player
            player = (
                session.query(MatchPlayer)
                .filter(
                    MatchPlayer.match_id == match_id,
                    MatchPlayer.user_id == user_id,
                )
                .first()
            )
            if player is None:
                raise ValueError("You are not a player in this match.")

            if match_orm.status in ("COMPLETED", "CANCELLED"):
                raise ValueError(f"Match is already {match_orm.status}.")

            now = datetime.utcnow()
            match_orm.status = "COMPLETED"
            match_orm.completed_at = now
            match_orm.updated_at = now

            # Free the cart back to AVAILABLE
            if match_orm.cart_id:
                cart = session.query(Cart).filter(Cart.id == match_orm.cart_id).first()
                if cart:
                    cart.status = "AVAILABLE"
                    cart.updated_at = now

            # Free any captain assigned to this match
            from modules.captain.repository.captain_repository import captain_repository
            captain_repository.release_captain_for_match(match_id, session)

            session.commit()
            session.refresh(match_orm)
            result = match_orm.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

        self.event_repo.log(match_id, "MATCH_COMPLETED", user_id=user_id)

        # Trigger split payment creation after the match transaction commits.
        # Lazy import avoids circular dependency: MatchService -> PaymentService.
        try:
            from modules.payment.service.payment_service import PaymentService

            payment_service = PaymentService()
            payment_service.create_split_payments(match_id)
            self.event_repo.log(match_id, "PAYMENT_CREATED")
        except Exception:
            # Payment creation failure must not roll back match completion.
            # The match is already COMPLETED; payments can be retried separately.
            import logging

            logging.getLogger(__name__).exception(
                "finish_match: split payment creation failed for match %d (non-fatal)",
                match_id,
            )

        return result

    # ── Enrichment ────────────────────────────────────────────────────

    def _enrich_matches(self, matches: list[dict]) -> list[dict]:
        """Attach human-readable names to a list of match dicts."""
        if not matches:
            return matches

        cart_types = {ct["id"]: ct for ct in self.cart_type_repo.find_all()}
        timeslots = {ts["id"]: ts for ts in self.timeslot_repo.find_all()}
        regions = {r["id"]: r for r in self.location_repo.find_all()}

        from modules.cart.repository.cart_repository import cart_repository

        carts = {c["id"]: c for c in cart_repository.find_all()}

        for m in matches:
            ct = cart_types.get(m.get("cart_type_id"))
            m["sport_name"] = ct.get("name") if ct else None

            ts = timeslots.get(m.get("timeslot_id"))
            if ts:
                m["timeslot_label"] = (
                    f"{ts.get('start_time', '?')} - {ts.get('end_time', '?')}"
                )
            else:
                m["timeslot_label"] = None

            r = regions.get(m.get("region_id"))
            m["region_name"] = r.get("name") if r else None

            c = carts.get(m.get("cart_id"))
            m["ground_label"] = c.get("label") if c else None

        return matches


# ── Shared singleton ───────────────────────────────────────────────────
match_service = MatchService()
