from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.dispute.model.dispute_model import Dispute


class DisputeRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            d = Dispute(
                booking_id=data.get("booking_id"),
                user_id=data.get("user_id"),
                raised_by=data.get("raised_by"),
                title=data["title"],
                description=data["description"],
                status=data.get("status", "OPEN"),
            )
            session.add(d)
            session.commit()
            session.refresh(d)
            return d.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, dispute_id: int) -> dict | None:
        session = self._session_factory()
        try:
            d = session.query(Dispute).filter(Dispute.id == dispute_id).first()
            return d.to_dict() if d else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        session = self._session_factory()
        try:
            return [d.to_dict() for d in session.query(Dispute).order_by(Dispute.id.desc()).all()]
        finally:
            session.close()

    def find_by_raised_by(self, user_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = (
                session.query(Dispute)
                .filter(Dispute.raised_by == user_id)
                .order_by(Dispute.id.desc())
                .all()
            )
            return [d.to_dict() for d in rows]
        finally:
            session.close()

    def update(self, dispute_id: int, data: dict) -> dict | None:
        session = self._session_factory()
        try:
            d = session.query(Dispute).filter(Dispute.id == dispute_id).first()
            if not d:
                return None
            for key, value in data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(d, key):
                    setattr(d, key, value)
            d.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(d)
            return d.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


dispute_repository = DisputeRepository()
