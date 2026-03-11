from datetime import datetime

from sqlalchemy import func

from core.database.db_connection import SessionLocal
from modules.location.model.location_model import Location


class LocationRepository:
    """
    Data access layer for Location entities.

    Responsibilities:
    - Performs CRUD operations against the PostgreSQL database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via Location.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, location_data: dict) -> dict:
        """Insert a new location record. Returns the created location as a dict."""
        session = self._session_factory()
        try:
            location = Location(
                name=location_data.get("name"),
                is_serviceable=location_data.get("is_serviceable", True),
            )
            session.add(location)
            session.commit()
            session.refresh(location)
            return location.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, location_id: int) -> dict | None:
        """Retrieve a location by ID."""
        session = self._session_factory()
        try:
            location = session.query(Location).filter(Location.id == location_id).first()
            return location.to_dict() if location else None
        finally:
            session.close()

    def find_by_name(self, name: str) -> dict | None:
        """Retrieve a location by name (case-insensitive)."""
        session = self._session_factory()
        try:
            location = session.query(Location).filter(
                func.lower(Location.name) == name.lower()
            ).first()
            return location.to_dict() if location else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        """Retrieve all locations."""
        session = self._session_factory()
        try:
            locations = session.query(Location).all()
            return [loc.to_dict() for loc in locations]
        finally:
            session.close()

    def update(self, location_id: int, update_data: dict) -> dict | None:
        """Update an existing location record. Automatically refreshes updated_at."""
        session = self._session_factory()
        try:
            location = session.query(Location).filter(Location.id == location_id).first()
            if not location:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(location, key):
                    setattr(location, key, value)

            location.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(location)
            return location.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, location_id: int) -> bool:
        """Delete a location record by ID."""
        session = self._session_factory()
        try:
            location = session.query(Location).filter(Location.id == location_id).first()
            if not location:
                return False
            session.delete(location)
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

location_repository = LocationRepository()
