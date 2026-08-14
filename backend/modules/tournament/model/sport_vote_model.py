from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, UniqueConstraint
from core.database.db_connection import Base


class SportVote(Base):
    """One user's current vote for which sport the next city-wide tournament in their
    region should be. A user has exactly one active vote per region — casting a new
    vote updates the existing row rather than creating a duplicate."""

    __tablename__ = "sport_votes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    region_id = Column(
        Integer,
        ForeignKey("locations.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    sport_name = Column(String(100), nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    __table_args__ = (
        UniqueConstraint("user_id", "region_id", name="uq_sport_vote_user_region"),
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "region_id": self.region_id,
            "sport_name": self.sport_name,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<SportVote id={self.id} user_id={self.user_id} sport={self.sport_name}>"
