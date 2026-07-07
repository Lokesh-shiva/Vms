from datetime import datetime


from core.database.db_connection import SessionLocal
from modules.match.model.match_model import Match, MatchPlayer


class MatchRepository:
    """
    Data access layer for Match and MatchPlayer entities.

    Returns raw dicts to the service layer.
    All session management follows the same pattern as other repositories.
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    # ── Match CRUD ─────────────────────────────────────────────────────

    def create(self, data: dict) -> dict:
        """Insert a new match record."""
        session = self._session_factory()
        try:
            match = Match(
                created_by=data["created_by"],
                region_id=data["region_id"],
                cart_type_id=data["cart_type_id"],
                cart_id=data.get("cart_id"),
                timeslot_id=data["timeslot_id"],
                skill_level=data.get("skill_level"),
                max_players=data["max_players"],
                joined_players=1,
                status="OPEN",
            )
            session.add(match)
            session.commit()
            session.refresh(match)
            return match.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, match_id: int, session=None) -> dict | None:
        """Retrieve a match by ID."""
        own = session is None
        session = session or self._session_factory()
        try:
            m = session.query(Match).filter(Match.id == match_id).first()
            return m.to_dict() if m else None
        finally:
            if own:
                session.close()

    def find_by_id_orm(self, match_id: int, session):
        """Return the ORM object (still attached to session) for row-locking."""
        return (
            session.query(Match).filter(Match.id == match_id).with_for_update().first()
        )

    def find_open_matches(
        self, cart_type_id: int = None, region_id: int = None
    ) -> list[dict]:
        """
        Return OPEN matches whose timeslot starts in the future.
        Optionally filtered by cart_type_id and/or region_id.
        """
        from modules.timeslot.model.timeslot_model import (
            Timeslot,
        )  # avoid circular import

        session = self._session_factory()
        try:
            now = datetime.utcnow()
            query = (
                session.query(Match)
                .join(Timeslot, Match.timeslot_id == Timeslot.id)
                .filter(
                    Match.status == "OPEN",
                    Timeslot.start_time > now.strftime("%H:%M"),  # future time
                )
            )
            if cart_type_id:
                query = query.filter(Match.cart_type_id == cart_type_id)
            if region_id:
                query = query.filter(Match.region_id == region_id)
            return [m.to_dict() for m in query.all()]
        finally:
            session.close()

    def create_play_now(
        self,
        user_id: int,
        region_id: int,
        cart_type_id: int,
        max_players: int = 2,
    ) -> dict:
        """
        Create a WAITING play-now match and add the creator as the first MatchPlayer.
        No timeslot or ground needed — captain brings those when they arrive.
        """
        session = self._session_factory()
        try:
            match = Match(
                created_by=user_id,
                region_id=region_id,
                cart_type_id=cart_type_id,
                sport_id=cart_type_id,  # unified post-migration-14
                max_players=max_players,
                joined_players=1,
                status="WAITING",
            )
            session.add(match)
            session.flush()
            session.add(MatchPlayer(match_id=match.id, user_id=user_id))
            session.commit()
            session.refresh(match)
            return match.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def create_captain_match(
        self,
        user_id: int,
        captain_id: int,
        region_id: int,
        cart_type_id: int,
        max_players: int,
        visibility: str,
        skill_level: str | None = None,
        society_id: int | None = None,
    ) -> dict:
        """
        Create a captain-organized WAITING match. Unlike create_play_now, the
        captain is NOT added as a MatchPlayer — they organize, they don't play.

        Sets created_by to the captain's own user_id (same field regular matches
        use for their creator) and immediately marks the captain busy via
        CaptainRepository.set_availability — mirroring the play-now auto-assign
        pattern, but applied at creation time instead of when the match fills up.

        For PRIVATE visibility, generates a unique 6-char alphanumeric invite_code.
        """
        import random
        import string

        from modules.captain.repository.captain_repository import captain_repository

        invite_code = None
        if visibility == "PRIVATE":
            invite_code = "".join(
                random.choices(string.ascii_uppercase + string.digits, k=6)
            )

        session = self._session_factory()
        try:
            match = Match(
                created_by=user_id,
                region_id=region_id,
                cart_type_id=cart_type_id,
                sport_id=cart_type_id,
                max_players=max_players,
                joined_players=0,
                status="WAITING",
                skill_level=skill_level,
                visibility=visibility,
                society_id=society_id,
                invite_code=invite_code,
                captain_id=captain_id,
            )
            session.add(match)
            session.flush()
            captain_repository.set_availability(
                captain_id=captain_id, available=False, match_id=match.id, session=session
            )
            session.commit()
            session.refresh(match)
            return match.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_society_matches(self, society_id: int) -> list[dict]:
        """Return WAITING/MATCHED matches for a specific society, newest first."""
        session = self._session_factory()
        try:
            rows = (
                session.query(Match)
                .filter(
                    Match.society_id == society_id,
                    Match.status.in_(["WAITING", "MATCHED"]),
                )
                .order_by(Match.created_at.desc())
                .all()
            )
            return [self._enrich(m, session) for m in rows]
        finally:
            session.close()

    def find_by_invite_code(self, invite_code: str) -> dict | None:
        """Look up a match by its private invite code."""
        session = self._session_factory()
        try:
            m = session.query(Match).filter(Match.invite_code == invite_code).first()
            return m.to_dict() if m else None
        finally:
            session.close()

    def find_waiting_in_region(
        self, region_id: int, sport_id: int | None = None
    ) -> list[dict]:
        """Return OPEN-visibility WAITING matches in a region, newest first."""
        session = self._session_factory()
        try:
            query = session.query(Match).filter(
                Match.region_id == region_id,
                Match.status == "WAITING",
                Match.visibility == "OPEN",
            )
            if sport_id:
                query = query.filter(Match.cart_type_id == sport_id)
            rows = query.order_by(Match.created_at.desc()).all()
            return [self._enrich(m, session) for m in rows]
        finally:
            session.close()

    def find_abandoned_waiting(self, cutoff: datetime) -> list[dict]:
        """WAITING matches with <=1 player, created before the cutoff — candidates for auto-cancel."""
        session = self._session_factory()
        try:
            rows = (
                session.query(Match)
                .filter(
                    Match.status == "WAITING",
                    Match.joined_players <= 1,
                    Match.created_at < cutoff,
                )
                .all()
            )
            return [m.to_dict() for m in rows]
        finally:
            session.close()

    def find_all_matches(self) -> list[dict]:
        """Return all matches across all statuses, newest first."""
        session = self._session_factory()
        try:
            return [
                m.to_dict()
                for m in session.query(Match).order_by(Match.created_at.desc()).all()
            ]
        finally:
            session.close()

    def find_by_user(self, user_id: int) -> list[dict]:
        """Return all matches this user has joined, enriched, newest first."""
        session = self._session_factory()
        try:
            rows = (
                session.query(Match)
                .join(MatchPlayer, MatchPlayer.match_id == Match.id)
                .filter(MatchPlayer.user_id == user_id)
                .order_by(Match.created_at.desc())
                .all()
            )
            return [self._enrich(m, session) for m in rows]
        finally:
            session.close()

    def find_active_by_user(self, user_id: int) -> dict | None:
        """Return the user's most recent non-terminal match, enriched."""
        active_statuses = {"WAITING", "MATCHED", "ARRIVED", "IN_PROGRESS"}
        session = self._session_factory()
        try:
            m = (
                session.query(Match)
                .join(MatchPlayer, MatchPlayer.match_id == Match.id)
                .filter(
                    MatchPlayer.user_id == user_id,
                    Match.status.in_(active_statuses),
                )
                .order_by(Match.created_at.desc())
                .first()
            )
            return self._enrich(m, session) if m else None
        finally:
            session.close()

    def find_by_id_enriched(self, match_id: int) -> dict | None:
        """Return a single match with enriched fields."""
        session = self._session_factory()
        try:
            m = session.query(Match).filter(Match.id == match_id).first()
            return self._enrich(m, session) if m else None
        finally:
            session.close()

    def _enrich(self, m: "Match", session) -> dict:
        """Attach human-readable fields to a Match ORM row."""
        from modules.cart.model.cart_model import Cart
        from modules.cart_type.model.cart_type_model import CartType
        from modules.location.model.location_model import Location
        from modules.timeslot.model.timeslot_model import Timeslot
        from modules.user.model.user_model import User

        base = m.to_dict()

        # Sport name via cart_type
        sport_name = ""
        if m.cart_type_id:
            ct = session.query(CartType).filter(CartType.id == m.cart_type_id).first()
            if ct:
                sport_name = ct.name

        # Ground name + address via cart → location
        ground_name = ""
        ground_address = ""
        if m.cart_id:
            cart = session.query(Cart).filter(Cart.id == m.cart_id).first()
            if cart:
                ground_name = cart.label or ""
                loc = session.query(Location).filter(Location.id == cart.region_id).first()
                if loc:
                    ground_address = loc.name

        # Scheduled time via timeslot
        scheduled_at = ""
        if m.timeslot_id:
            ts = session.query(Timeslot).filter(Timeslot.id == m.timeslot_id).first()
            if ts:
                scheduled_at = f"{ts.date}T{ts.start_time}"

        # Captain name via created_by
        captain_name = None
        if m.created_by:
            u = session.query(User).filter(User.id == m.created_by).first()
            if u:
                captain_name = u.name

        # Player IDs via match_players
        player_ids = [
            mp.user_id
            for mp in session.query(MatchPlayer).filter(MatchPlayer.match_id == m.id).all()
        ]

        return {
            **base,
            "sport": sport_name,
            "ground_name": ground_name,
            "ground_address": ground_address,
            "scheduled_at": scheduled_at,
            "captain_name": captain_name,
            "captain_id": m.created_by,
            "player_ids": player_ids,
            "price": 0,
        }

    def update(self, match_id: int, data: dict, session=None) -> dict | None:
        """Update match fields."""
        own = session is None
        session = session or self._session_factory()
        try:
            m = session.query(Match).filter(Match.id == match_id).first()
            if not m:
                return None
            for key, value in data.items():
                if key not in ("id", "created_at") and hasattr(m, key):
                    setattr(m, key, value)
            m.updated_at = datetime.utcnow()
            if own:
                session.commit()
            else:
                session.flush()
            session.refresh(m)
            return m.to_dict()
        except Exception:
            if own:
                session.rollback()
            raise
        finally:
            if own:
                session.close()

    def has_conflicting_match(self, cart_id: int, timeslot_id: int) -> bool:
        """
        Return True if an OPEN or FULL match already occupies
        this (cart_id, timeslot_id) combination.
        Prevents double-booking the same ground for the same slot.
        """
        session = self._session_factory()
        try:
            count = (
                session.query(Match)
                .filter(
                    Match.cart_id == cart_id,
                    Match.timeslot_id == timeslot_id,
                    Match.status.in_(["OPEN", "FULL"]),
                )
                .count()
            )
            return count > 0
        finally:
            session.close()

    # ── Cart selection ─────────────────────────────────────────────────

    def find_available_cart(self, region_id: int, cart_type_id: int, session=None):
        """
        Find the least-recently-used AVAILABLE + active cart for the
        given region and cart type.

        Ordered by updated_at ASC (fairness rotation) so the same
        ground isn't always picked first.

        Returns the Cart ORM object or None.
        """
        from modules.cart.model.cart_model import Cart  # avoid circular import

        own = session is None
        session = session or self._session_factory()
        try:
            return (
                session.query(Cart)
                .filter(
                    Cart.region_id == region_id,
                    Cart.cart_type_id == cart_type_id,
                    Cart.status == "AVAILABLE",
                    Cart.is_active == True,  # noqa: E712
                )
                .order_by(Cart.updated_at.asc())
                .first()
            )
        finally:
            if own:
                session.close()

    # ── MatchPlayer ────────────────────────────────────────────────────

    def add_player(self, match_id: int, user_id: int, session) -> None:
        """Add a player to a match inside an existing transaction."""
        player = MatchPlayer(match_id=match_id, user_id=user_id, has_arrived=False)
        session.add(player)
        session.flush()

    def add_player_standalone(self, match_id: int, user_id: int) -> None:
        """Add a player using its own session (used after match creation commit)."""
        session = self._session_factory()
        try:
            player = MatchPlayer(match_id=match_id, user_id=user_id, has_arrived=False)
            session.add(player)
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def has_player(self, match_id: int, user_id: int) -> bool:
        """Return True if the user has already joined this match."""
        session = self._session_factory()
        try:
            return (
                session.query(MatchPlayer)
                .filter(
                    MatchPlayer.match_id == match_id,
                    MatchPlayer.user_id == user_id,
                )
                .count()
            ) > 0
        finally:
            session.close()

    def remove_player(self, match_id: int, user_id: int, session) -> None:
        """Remove a player from a match inside an existing transaction."""
        player = (
            session.query(MatchPlayer)
            .filter(
                MatchPlayer.match_id == match_id,
                MatchPlayer.user_id == user_id,
            )
            .first()
        )
        if player:
            session.delete(player)
            session.flush()

    def increment_player_count(self, match_id: int, session) -> None:
        """Atomically increment joined_players inside an existing transaction."""
        m = session.query(Match).filter(Match.id == match_id).first()
        if m:
            m.joined_players += 1
            m.updated_at = datetime.utcnow()
            session.flush()

    def decrement_player_count(self, match_id: int, session) -> None:
        """Atomically decrement joined_players inside an existing transaction."""
        m = session.query(Match).filter(Match.id == match_id).first()
        if m and m.joined_players > 0:
            m.joined_players -= 1
            m.updated_at = datetime.utcnow()
            session.flush()


# ── Shared singleton ───────────────────────────────────────────────────
match_repository = MatchRepository()
