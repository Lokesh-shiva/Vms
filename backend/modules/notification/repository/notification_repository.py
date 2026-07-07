from core.database.db_connection import SessionLocal
from modules.notification.model.notification_model import Notification


class NotificationRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, user_id: int, title: str, body: str, type_: str = "GENERAL", data: dict | None = None) -> dict:
        session = self._session_factory()
        try:
            n = Notification(user_id=user_id, title=title, body=body, type=type_, data_json=data or {})
            session.add(n)
            session.commit()
            session.refresh(n)
            return n.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_user(self, user_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = (
                session.query(Notification)
                .filter(Notification.user_id == user_id)
                .order_by(Notification.id.desc())
                .all()
            )
            return [n.to_dict() for n in rows]
        finally:
            session.close()

    def mark_read(self, notification_id: int, user_id: int) -> dict | None:
        session = self._session_factory()
        try:
            n = (
                session.query(Notification)
                .filter(Notification.id == notification_id, Notification.user_id == user_id)
                .first()
            )
            if not n:
                return None
            n.read = True
            session.commit()
            session.refresh(n)
            return n.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


notification_repository = NotificationRepository()
