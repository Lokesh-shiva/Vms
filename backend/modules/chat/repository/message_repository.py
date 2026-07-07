from core.database.db_connection import SessionLocal
from modules.chat.model.message_model import Message


class MessageRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, match_id: int, sender_id: int, body: str) -> dict:
        session = self._session_factory()
        try:
            m = Message(match_id=match_id, sender_id=sender_id, body=body)
            session.add(m)
            session.commit()
            session.refresh(m)
            return m.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_match(self, match_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = (
                session.query(Message)
                .filter(Message.match_id == match_id)
                .order_by(Message.created_at.asc())
                .all()
            )
            return [m.to_dict() for m in rows]
        finally:
            session.close()

    def find_last_by_match(self, match_id: int) -> dict | None:
        session = self._session_factory()
        try:
            m = (
                session.query(Message)
                .filter(Message.match_id == match_id)
                .order_by(Message.created_at.desc())
                .first()
            )
            return m.to_dict() if m else None
        finally:
            session.close()


message_repository = MessageRepository()
