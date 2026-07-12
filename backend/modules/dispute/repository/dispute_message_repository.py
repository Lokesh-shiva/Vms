from core.database.db_connection import SessionLocal
from modules.dispute.model.dispute_message_model import DisputeMessage


class DisputeMessageRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, dispute_id: int, sender_id: int, body: str) -> dict:
        session = self._session_factory()
        try:
            m = DisputeMessage(dispute_id=dispute_id, sender_id=sender_id, body=body)
            session.add(m)
            session.commit()
            session.refresh(m)
            return m.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_dispute(self, dispute_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = (
                session.query(DisputeMessage)
                .filter(DisputeMessage.dispute_id == dispute_id)
                .order_by(DisputeMessage.created_at.asc())
                .all()
            )
            return [m.to_dict() for m in rows]
        finally:
            session.close()


dispute_message_repository = DisputeMessageRepository()
