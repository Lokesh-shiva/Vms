from datetime import datetime
from sqlalchemy import Boolean, Column, DateTime, Integer, JSON, String

from core.database.db_connection import Base


class VoteRoundStatus:
    OPEN = "OPEN"
    CLOSED = "CLOSED"
    ALL: frozenset[str] = frozenset({OPEN, CLOSED})


class SportVoteRound(Base):
    """One admin-configured voting round: a shortlist of sports and a deadline.
    Only one round has is_current=True at a time — starting a new round
    archives the previous one rather than deleting its history."""

    __tablename__ = "sport_vote_rounds"

    id = Column(Integer, primary_key=True, autoincrement=True)
    options = Column(JSON, nullable=False)  # list[str] of sport names
    closes_at = Column(DateTime, nullable=False)
    status = Column(String(20), nullable=False, default=VoteRoundStatus.OPEN)
    is_current = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "options": self.options,
            "closes_at": self.closes_at.isoformat() if self.closes_at else None,
            "status": self.status,
            "is_current": self.is_current,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self) -> str:
        return f"<SportVoteRound id={self.id} status={self.status} options={self.options}>"
