from datetime import datetime
from sqlalchemy.orm import Session
from core.database.db_connection import SessionLocal
from modules.matchmaking.model.queue_entry_model import QueueEntry


class QueueEntryRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, entry_data: dict, session=None) -> dict:
        """Insert a new QueueEntry. Returns created entry as dict."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            entry = QueueEntry(
                user_id=entry_data["user_id"],
                region_id=entry_data["region_id"],
                sport_id=entry_data["sport_id"],
                skill_level=entry_data["skill_level"],
                status=entry_data.get("status", "WAITING"),
            )
            session.add(entry)
            if own_session:
                session.commit()
                session.refresh(entry)
            else:
                session.flush()
                session.refresh(entry)
            return entry.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def find_by_id(self, entry_id: int, session=None) -> dict | None:
        """Find a QueueEntry by its primary key."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            entry = session.query(QueueEntry).filter(QueueEntry.id == entry_id).first()
            return entry.to_dict() if entry else None
        finally:
            if own_session:
                session.close()

    def find_waiting_by_user(self, user_id: int, session=None) -> dict | None:
        """Find a WAITING entry for a user (at most one active queue per user)."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            entry = (
                session.query(QueueEntry)
                .filter(QueueEntry.user_id == user_id, QueueEntry.status == "WAITING")
                .first()
            )
            return entry.to_dict() if entry else None
        finally:
            if own_session:
                session.close()

    def find_waiting_orm(self, entry_id: int, session: Session) -> QueueEntry | None:
        """Return ORM instance (not dict) for mutation — requires caller-managed session."""
        return (
            session.query(QueueEntry)
            .filter(QueueEntry.id == entry_id, QueueEntry.status == "WAITING")
            .first()
        )

    def update_status(self, entry_id: int, new_status: str, session=None) -> dict | None:
        """Update the status of a QueueEntry. Returns updated dict or None if not found."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            entry = session.query(QueueEntry).filter(QueueEntry.id == entry_id).first()
            if not entry:
                return None
            entry.status = new_status
            if own_session:
                session.commit()
                session.refresh(entry)
            else:
                session.flush()
                session.refresh(entry)
            return entry.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def count_waiting(self, region_id: int, sport_id: int, session=None) -> int:
        """Count WAITING entries for region+sport — used for pricing and wait estimate."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            return (
                session.query(QueueEntry)
                .filter(
                    QueueEntry.region_id == region_id,
                    QueueEntry.sport_id == sport_id,
                    QueueEntry.status == "WAITING",
                )
                .count()
            )
        finally:
            if own_session:
                session.close()


# Shared singleton — import this in the service layer
queue_entry_repository = QueueEntryRepository()
