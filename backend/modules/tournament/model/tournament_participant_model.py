from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, UniqueConstraint
from core.database.db_connection import Base


class ParticipantStatus:
    REGISTERED = "REGISTERED"
    WITHDRAWN = "WITHDRAWN"
    DISQUALIFIED = "DISQUALIFIED"
    ALL: frozenset[str] = frozenset({REGISTERED, WITHDRAWN, DISQUALIFIED})


class TournamentParticipant(Base):
    __tablename__ = "tournament_participants"

    id = Column(Integer, primary_key=True, autoincrement=True)
    tournament_id = Column(
        Integer,
        ForeignKey("tournaments.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    team_id = Column(
        Integer,
        ForeignKey("tournament_teams.id", ondelete="CASCADE"),
        nullable=True,
    )
    status = Column(String(50), nullable=False, default=ParticipantStatus.REGISTERED)
    joined_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("tournament_id", "user_id", name="uq_tournament_participant"),
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "tournament_id": self.tournament_id,
            "user_id": self.user_id,
            "team_id": self.team_id,
            "status": self.status,
            "joined_at": self.joined_at.isoformat() if self.joined_at else None,
        }

    def __repr__(self) -> str:
        return f"<TournamentParticipant id={self.id} user_id={self.user_id} status={self.status}>"
