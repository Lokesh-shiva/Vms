from datetime import datetime

from sqlalchemy import Column, Integer, String, ForeignKey, DateTime

from core.database.db_connection import Base


class QueueEntry(Base):
    """
    Represents a user waiting in the matchmaking queue.

    Status lifecycle:
        WAITING   → user is actively queued
        MATCHED   → user has been paired into a Match
        CANCELLED → user left the queue before matching
    """

    __tablename__ = "queue_entries"

    VALID_STATUSES = {"WAITING", "MATCHED", "CANCELLED"}
    VALID_SKILL_LEVELS = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    region_id = Column(Integer, ForeignKey("locations.id"), nullable=False, index=True)
    sport_id = Column(Integer, ForeignKey("sports.id"), nullable=False, index=True)
    skill_level = Column(String, nullable=False)
    status = Column(String, nullable=False, default="WAITING")
    reason = Column(String, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "region_id": self.region_id,
            "sport_id": self.sport_id,
            "skill_level": self.skill_level,
            "status": self.status,
            "reason": self.reason,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self):
        return f"<QueueEntry id={self.id} user_id={self.user_id} status={self.status}>"
