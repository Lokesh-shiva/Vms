from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.captain.model.captain_model import Captain


class CaptainRepository:
    """
    Data access layer for Captain entities.

    Responsibilities:
    - Performs CRUD operations against the PostgreSQL database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via Captain.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def get_all(self, region_id: int | None = None) -> list[dict]:
        """Retrieve all captains, optionally filtered by region_id."""
        session = self._session_factory()
        try:
            query = session.query(Captain)
            if region_id is not None:
                query = query.filter(Captain.region_id == region_id)
            captains = query.all()
            return [c.to_dict() for c in captains]
        finally:
            session.close()

    def get_by_id(self, captain_id: int) -> dict | None:
        """Retrieve a captain by ID."""
        session = self._session_factory()
        try:
            captain = (
                session.query(Captain).filter(Captain.id == captain_id).first()
            )
            return captain.to_dict() if captain else None
        finally:
            session.close()

    def get_by_user_id(self, user_id: int) -> dict | None:
        """Retrieve a captain by their user_id (unique)."""
        session = self._session_factory()
        try:
            captain = (
                session.query(Captain).filter(Captain.user_id == user_id).first()
            )
            return captain.to_dict() if captain else None
        finally:
            session.close()

    def create(self, data: dict) -> dict:
        """Insert a new captain record. Returns the created captain as a dict."""
        session = self._session_factory()
        try:
            captain = Captain(
                user_id=data.get("user_id"),
                region_id=data.get("region_id"),
                status=data.get("status", "ACTIVE"),
                rating=data.get("rating", 0.0),
                total_trips=data.get("total_trips", 0),
                bio=data.get("bio"),
            )
            session.add(captain)
            session.commit()
            session.refresh(captain)
            return captain.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def update(self, captain_id: int, data: dict) -> dict | None:
        """Update an existing captain record. Returns updated dict or None if not found."""
        session = self._session_factory()
        try:
            captain = (
                session.query(Captain).filter(Captain.id == captain_id).first()
            )
            if not captain:
                return None

            for key, value in data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(
                    captain, key
                ):
                    setattr(captain, key, value)

            captain.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(captain)
            return captain.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, captain_id: int) -> bool:
        """Delete a captain record by ID. Returns True if deleted, False if not found."""
        session = self._session_factory()
        try:
            captain = (
                session.query(Captain).filter(Captain.id == captain_id).first()
            )
            if not captain:
                return False
            session.delete(captain)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


# ── Shared module-level instance ──────────────────────────────────────
# All services must import and use this singleton so they share the same
# DB-backed repository.  Tests inject their own session_factory instead.

captain_repository = CaptainRepository()
