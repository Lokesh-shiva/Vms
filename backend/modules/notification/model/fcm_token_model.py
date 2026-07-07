from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String
from core.database.db_connection import Base


class FcmToken(Base):
    """One row per user — a new registration overwrites the previous token (single-device push)."""

    __tablename__ = "fcm_tokens"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True, index=True)
    token = Column(String, nullable=False)
    platform = Column(String(20), nullable=False, default="android")
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "token": self.token,
            "platform": self.platform,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<FcmToken user_id={self.user_id} platform={self.platform}>"
