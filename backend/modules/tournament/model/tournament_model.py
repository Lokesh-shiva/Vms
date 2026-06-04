from datetime import date, datetime
from sqlalchemy import Column, Date, DateTime, ForeignKey, Integer, String
from core.database.db_connection import Base


class TournamentStatus:
    UPCOMING = "UPCOMING"
    ONGOING = "ONGOING"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    ALL: frozenset[str] = frozenset({UPCOMING, ONGOING, COMPLETED, CANCELLED})


class Tournament(Base):
    __tablename__ = "tournaments"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, nullable=False)
    sport_id = Column(Integer, ForeignKey("sports.id", ondelete="SET NULL"), nullable=True, index=True)
    region_id = Column(Integer, ForeignKey("locations.id", ondelete="SET NULL"), nullable=True, index=True)
    organizer = Column(String, nullable=False)
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False)
    max_teams = Column(Integer, nullable=False, default=8)
    status = Column(String(50), nullable=False, default=TournamentStatus.UPCOMING)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "sport_id": self.sport_id,
            "region_id": self.region_id,
            "organizer": self.organizer,
            "start_date": self.start_date.isoformat() if self.start_date else None,
            "end_date": self.end_date.isoformat() if self.end_date else None,
            "max_teams": self.max_teams,
            "status": self.status,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<Tournament id={self.id} name={self.name} status={self.status}>"
