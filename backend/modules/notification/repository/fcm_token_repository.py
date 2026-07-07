from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.notification.model.fcm_token_model import FcmToken


class FcmTokenRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def upsert(self, user_id: int, token: str, platform: str = "android") -> dict:
        session = self._session_factory()
        try:
            row = session.query(FcmToken).filter(FcmToken.user_id == user_id).first()
            if row:
                row.token = token
                row.platform = platform
                row.updated_at = datetime.utcnow()
            else:
                row = FcmToken(user_id=user_id, token=token, platform=platform)
                session.add(row)
            session.commit()
            session.refresh(row)
            return row.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_user(self, user_id: int) -> dict | None:
        session = self._session_factory()
        try:
            row = session.query(FcmToken).filter(FcmToken.user_id == user_id).first()
            return row.to_dict() if row else None
        finally:
            session.close()


fcm_token_repository = FcmTokenRepository()
