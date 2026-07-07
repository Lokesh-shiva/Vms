from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, Text
from core.database.db_connection import Base


class Message(Base):
    """A single chat message, scoped to the match its sender/recipient share."""

    __tablename__ = "messages"

    id = Column(Integer, primary_key=True, autoincrement=True)
    match_id = Column(Integer, ForeignKey("matches.id", ondelete="CASCADE"), nullable=False, index=True)
    sender_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)
    body = Column(Text, nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "match_id": self.match_id,
            "sender_id": self.sender_id,
            "body": self.body,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self) -> str:
        return f"<Message id={self.id} match_id={self.match_id} sender_id={self.sender_id}>"
