from datetime import date, datetime
from sqlalchemy import Column, Date, DateTime, ForeignKey, Integer, JSON, String, Text
from core.database.db_connection import Base


class TournamentStatus:
    UPCOMING = "UPCOMING"
    ONGOING = "ONGOING"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    ALL: frozenset[str] = frozenset({UPCOMING, ONGOING, COMPLETED, CANCELLED})


class TournamentFormat:
    KNOCKOUT = "KNOCKOUT"
    ROUND_ROBIN = "ROUND_ROBIN"
    LEAGUE = "LEAGUE"
    ALL: frozenset[str] = frozenset({KNOCKOUT, ROUND_ROBIN, LEAGUE})


class TournamentParticipantType:
    INDIVIDUAL = "INDIVIDUAL"
    TEAM = "TEAM"
    ALL: frozenset[str] = frozenset({INDIVIDUAL, TEAM})


RULES_JSON_DEFAULTS: dict = {
    "win_points": 3,
    "draw_points": 1,
    "loss_points": 0,
    "tiebreaker": "head_to_head",
    "age_limit": None,
    "skill_cap": None,
    "global_points_per_win": 10,
}


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
    format_type = Column(String(50), nullable=False, default=TournamentFormat.LEAGUE)
    participant_type = Column(String(50), nullable=False, default=TournamentParticipantType.INDIVIDUAL)
    team_size = Column(Integer, nullable=False, default=1)
    entry_fee = Column(Integer, nullable=False, default=0)
    prize_pool = Column(String(100), nullable=False, default="")
    banner_url = Column(String(500), nullable=True)
    description = Column(Text, nullable=True)
    sponsor_user_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)
    rules_json = Column(JSON, nullable=False, default=dict)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "sport_id": self.sport_id,
            "region_id": self.region_id,
            "organizer": self.organizer,
            "organizer_name": self.organizer,
            "start_date": self.start_date.isoformat() if self.start_date else None,
            "end_date": self.end_date.isoformat() if self.end_date else None,
            "max_teams": self.max_teams,
            "status": self.status,
            "format_type": self.format_type,
            "format": self.format_type,
            "participant_type": self.participant_type,
            "team_size": self.team_size,
            "entry_fee": self.entry_fee or 0,
            "prize_pool": self.prize_pool or "",
            "banner_url": self.banner_url,
            "description": self.description or "",
            "sponsor_user_id": self.sponsor_user_id,
            "rules_json": self.rules_json or {},
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<Tournament id={self.id} name={self.name} status={self.status} format={self.format_type}>"
