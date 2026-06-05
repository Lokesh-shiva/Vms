from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String
from core.database.db_connection import Base


class TournamentTeam(Base):
    __tablename__ = "tournament_teams"

    id = Column(Integer, primary_key=True, autoincrement=True)
    tournament_id = Column(
        Integer,
        ForeignKey("tournaments.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    name = Column(String, nullable=False)
    captain_user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "tournament_id": self.tournament_id,
            "name": self.name,
            "captain_user_id": self.captain_user_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self) -> str:
        return f"<TournamentTeam id={self.id} name={self.name}>"
