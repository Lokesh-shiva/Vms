from datetime import datetime

from sqlalchemy import Boolean, Column, Integer, String, ForeignKey, DateTime

from core.database.db_connection import Base


class Timeslot(Base):
    """
    SQLAlchemy model for the 'timeslots' table.

    Fields:
        id (int): Primary key, auto-incremented.
        location_id (int): Reference to the associated location (FK → locations.id).
        date (str): Date of the timeslot (YYYY-MM-DD).
        start_time (str): Slot start time (HH:MM).
        end_time (str): Slot end time (HH:MM).
        capacity (int): Maximum capacity for this slot (must be > 0).
        is_active (bool): Whether this timeslot is open for bookings (default True).
        created_at (datetime): Timestamp of record creation (immutable).
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "timeslots"

    id = Column(Integer, primary_key=True, autoincrement=True)
    location_id = Column(
        Integer, ForeignKey("locations.id"), nullable=False, index=True
    )
    date = Column(String, nullable=False)
    start_time = Column(String, nullable=False)
    end_time = Column(String, nullable=False)
    capacity = Column(Integer, nullable=False)
    is_active = Column(Boolean, nullable=False, default=True, server_default="true")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize timeslot to dictionary (backward-compatible with service layer)."""
        return {
            "id": self.id,
            "location_id": self.location_id,
            "date": self.date,
            "start_time": self.start_time,
            "end_time": self.end_time,
            "capacity": self.capacity,
            "is_active": self.is_active,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return (
            f"<Timeslot id={self.id} location_id={self.location_id} date={self.date}>"
        )
