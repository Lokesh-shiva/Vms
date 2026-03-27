from datetime import datetime

from sqlalchemy.exc import IntegrityError

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
        return session.query(Match).filter(Match.id == match_id).with_for_update().first()

    def find_open_matches(self, cart_type_id: int = None, region_id: int = None) -> list[dict]:
        """
        Return OPEN matches whose timeslot starts in the future.
        Optionally filtered by cart_type_id and/or region_id.
        """
        from modules.timeslot.model.timeslot_model import Timeslot  # avoid circular import

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
        player = MatchPlayer(match_id=match_id, user_id=user_id, has_paid=False)
        session.add(player)
        session.flush()

    def add_player_standalone(self, match_id: int, user_id: int) -> None:
        """Add a player using its own session (used after match creation commit)."""
        session = self._session_factory()
        try:
            player = MatchPlayer(match_id=match_id, user_id=user_id, has_paid=False)
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
