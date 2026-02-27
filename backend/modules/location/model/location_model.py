from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, DateTime

from core.database.db_connection import Base


class Location(Base):
    """
    SQLAlchemy model for the 'locations' table.

    Fields:
        id (int): Primary key, auto-incremented.
        name (str): Location name (unique, indexed).
        is_serviceable (bool): Whether the location is currently serviceable.
        created_at (datetime): Timestamp of record creation (immutable).
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "locations"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True, nullable=False, index=True)
    is_serviceable = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize location to dictionary (backward-compatible with service layer)."""
        return {
            "id": self.id,
            "name": self.name,
            "is_serviceable": self.is_serviceable,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<Location id={self.id} name={self.name}>"
