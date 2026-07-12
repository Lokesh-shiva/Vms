from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, Text
from core.database.db_connection import Base


class DisputeMessage(Base):
    """A single reply on a support ticket thread — either from the person who
    raised it, or from support/admin staff handling it."""

    __tablename__ = "dispute_messages"

    id = Column(Integer, primary_key=True, autoincrement=True)
    dispute_id = Column(Integer, ForeignKey("disputes.id", ondelete="CASCADE"), nullable=False, index=True)
    sender_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)
    body = Column(Text, nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "dispute_id": self.dispute_id,
            "sender_id": self.sender_id,
            "body": self.body,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self) -> str:
        return f"<DisputeMessage id={self.id} dispute_id={self.dispute_id} sender_id={self.sender_id}>"
