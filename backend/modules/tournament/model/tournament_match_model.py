from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text
from core.database.db_connection import Base


class TournamentMatchStatus:
    SCHEDULED = "SCHEDULED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    ALL: frozenset[str] = frozenset({SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED})


class TournamentMatch(Base):
    __tablename__ = "tournament_matches"

    id = Column(Integer, primary_key=True, autoincrement=True)
    tournament_id = Column(
        Integer,
        ForeignKey("tournaments.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    round = Column(Integer, nullable=False, default=1)
    # Individual tournaments
    home_user_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    away_user_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    # Team tournaments
    home_team_id = Column(Integer, ForeignKey("tournament_teams.id", ondelete="SET NULL"), nullable=True)
    away_team_id = Column(Integer, ForeignKey("tournament_teams.id", ondelete="SET NULL"), nullable=True)
    # Result
    home_score = Column(Integer, nullable=True)
    away_score = Column(Integer, nullable=True)
    home_points_awarded = Column(Integer, nullable=True)
    away_points_awarded = Column(Integer, nullable=True)
    status = Column(String(50), nullable=False, default=TournamentMatchStatus.SCHEDULED)
    scheduled_at = Column(DateTime, nullable=True)
    completed_at = Column(DateTime, nullable=True)
    notes = Column(Text, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "tournament_id": self.tournament_id,
            "round": self.round,
            "home_user_id": self.home_user_id,
            "away_user_id": self.away_user_id,
            "home_team_id": self.home_team_id,
            "away_team_id": self.away_team_id,
            "home_score": self.home_score,
            "away_score": self.away_score,
            "home_points_awarded": self.home_points_awarded,
            "away_points_awarded": self.away_points_awarded,
            "status": self.status,
            "scheduled_at": self.scheduled_at.isoformat() if self.scheduled_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "notes": self.notes,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self) -> str:
        return f"<TournamentMatch id={self.id} tournament_id={self.tournament_id} status={self.status}>"
