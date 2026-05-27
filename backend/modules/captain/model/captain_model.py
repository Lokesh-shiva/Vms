from datetime import datetime

from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String, Text

from core.database.db_connection import Base


class CaptainStatus:
    """Allowed captain status values."""

    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    SUSPENDED = "SUSPENDED"

    ALL: frozenset[str] = frozenset({ACTIVE, INACTIVE, SUSPENDED})


class Captain(Base):
    """
    SQLAlchemy model for the 'captains' table.

    Fields:
        id (int): Primary key, auto-incremented.
        user_id (int): FK → users.id (one captain per user, CASCADE on delete).
        region_id (int | None): FK → locations.id (SET NULL on delete).
        status (str): One of ACTIVE / INACTIVE / SUSPENDED.
        rating (float): Average rating (0.0–5.0).
        total_trips (int): Cumulative completed trips.
        bio (str | None): Optional free-text bio.
        created_at (datetime): Record creation timestamp.
        updated_at (datetime): Last update timestamp.
    """

    __tablename__ = "captains"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        unique=True,
        nullable=False,
        index=True,
    )
    region_id = Column(
        Integer,
        ForeignKey("locations.id", ondelete="SET NULL"),
        nullable=True,
    )
    status = Column(String(50), nullable=False, default=CaptainStatus.ACTIVE)
    rating = Column(Float, nullable=False, default=0.0)
    total_trips = Column(Integer, nullable=False, default=0)
    bio = Column(Text, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize captain to dictionary."""
        return {
            "id": self.id,
            "user_id": self.user_id,
            "region_id": self.region_id,
            "status": self.status,
            "rating": self.rating,
            "total_trips": self.total_trips,
            "bio": self.bio,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<Captain id={self.id} user_id={self.user_id} status={self.status}>"
