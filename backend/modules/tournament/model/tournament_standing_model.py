from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, UniqueConstraint
from core.database.db_connection import Base


class TournamentStanding(Base):
    __tablename__ = "tournament_standings"

    id = Column(Integer, primary_key=True, autoincrement=True)
    tournament_id = Column(
        Integer,
        ForeignKey("tournaments.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=True)
    team_id = Column(Integer, ForeignKey("tournament_teams.id", ondelete="CASCADE"), nullable=True)
    played = Column(Integer, nullable=False, default=0)
    won = Column(Integer, nullable=False, default=0)
    drawn = Column(Integer, nullable=False, default=0)
    lost = Column(Integer, nullable=False, default=0)
    points = Column(Integer, nullable=False, default=0)
    rank = Column(Integer, nullable=True)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("tournament_id", "user_id", name="uq_standing_user"),
        UniqueConstraint("tournament_id", "team_id", name="uq_standing_team"),
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "tournament_id": self.tournament_id,
            "user_id": self.user_id,
            "team_id": self.team_id,
            "played": self.played,
            "won": self.won,
            "drawn": self.drawn,
            "lost": self.lost,
            "points": self.points,
            "rank": self.rank,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<TournamentStanding id={self.id} tournament_id={self.tournament_id} points={self.points}>"
