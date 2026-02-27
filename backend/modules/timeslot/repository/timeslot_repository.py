from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.timeslot.model.timeslot_model import Timeslot


class TimeslotRepository:
    """
    Data access layer for Timeslot entities.

    Responsibilities:
    - Performs CRUD operations against the PostgreSQL database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via Timeslot.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, timeslot_data: dict) -> dict:
        """Insert a new timeslot record. Returns the created timeslot as a dict."""
        session = self._session_factory()
        try:
            timeslot = Timeslot(
                location_id=timeslot_data.get("location_id"),
                date=timeslot_data.get("date"),
                start_time=timeslot_data.get("start_time"),
                end_time=timeslot_data.get("end_time"),
                capacity=timeslot_data.get("capacity"),
            )
            session.add(timeslot)
            session.commit()
            session.refresh(timeslot)
            return timeslot.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, timeslot_id: int) -> dict | None:
        """Retrieve a timeslot by ID."""
        session = self._session_factory()
        try:
            timeslot = session.query(Timeslot).filter(Timeslot.id == timeslot_id).first()
            return timeslot.to_dict() if timeslot else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        """Retrieve all timeslots."""
        session = self._session_factory()
        try:
            timeslots = session.query(Timeslot).all()
            return [ts.to_dict() for ts in timeslots]
        finally:
            session.close()

    def find_by_location_date_start(self, location_id: int, date: str,
                                     start_time: str) -> dict | None:
        """Find a timeslot matching the unique constraint (location, date, start_time)."""
        session = self._session_factory()
        try:
            timeslot = (
                session.query(Timeslot)
                .filter(
                    Timeslot.location_id == location_id,
                    Timeslot.date == date,
                    Timeslot.start_time == start_time,
                )
                .first()
            )
            return timeslot.to_dict() if timeslot else None
        finally:
            session.close()

    def update(self, timeslot_id: int, update_data: dict) -> dict | None:
        """Update an existing timeslot record. Automatically refreshes updated_at."""
        session = self._session_factory()
        try:
            timeslot = session.query(Timeslot).filter(Timeslot.id == timeslot_id).first()
            if not timeslot:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(timeslot, key):
                    setattr(timeslot, key, value)

            timeslot.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(timeslot)
            return timeslot.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, timeslot_id: int) -> bool:
        """Delete a timeslot record by ID."""
        session = self._session_factory()
        try:
            timeslot = session.query(Timeslot).filter(Timeslot.id == timeslot_id).first()
            if not timeslot:
                return False
            session.delete(timeslot)
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

timeslot_repository = TimeslotRepository()
