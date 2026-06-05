from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, UniqueConstraint
from core.database.db_connection import Base


class PlayerScore(Base):
    __tablename__ = "player_scores"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    region_id = Column(Integer, ForeignKey("locations.id", ondelete="CASCADE"), nullable=False)
    sport_id = Column(Integer, ForeignKey("sports.id", ondelete="CASCADE"), nullable=False)
    total_points = Column(Integer, nullable=False, default=0)
    matches_played = Column(Integer, nullable=False, default=0)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("user_id", "region_id", "sport_id", name="uq_player_score"),
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "region_id": self.region_id,
            "sport_id": self.sport_id,
            "total_points": self.total_points,
            "matches_played": self.matches_played,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<PlayerScore user_id={self.user_id} region_id={self.region_id} sport_id={self.sport_id} pts={self.total_points}>"
