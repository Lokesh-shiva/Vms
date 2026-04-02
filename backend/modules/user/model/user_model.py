from datetime import datetime
from enum import Enum

from sqlalchemy import Column, Integer, String, Boolean, DateTime

from core.database.db_connection import Base


class UserRole(str, Enum):
    """Allowed user roles. All role assignments must use these values."""
    USER = "user"
    ADMIN = "admin"


class User(Base):
    """
    SQLAlchemy model for the 'users' table.

    Fields:
        id (int): Primary key, auto-incremented.
        name (str): Full name of the user.
        phone (str): Contact phone number (unique, indexed).
        password_hash (str): Bcrypt hash — never exposed via to_dict().
        role (str): User role — 'user' or 'admin'.
        is_active (bool): Whether the account is active.
        created_at (datetime): Timestamp of account creation (immutable).
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, nullable=False)
    phone = Column(String, unique=True, nullable=False, index=True)
    password_hash = Column(String, nullable=False)
    role = Column(String, nullable=False, default=UserRole.USER.value)
    is_active = Column(Boolean, nullable=False, default=True)
    region_id = Column(Integer, nullable=True)
    ghost_strikes = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize user to dictionary (backward-compatible with service layer)."""
        return {
            "id": self.id,
            "name": self.name,
            "phone": self.phone,
            "role": self.role,
            "is_active": self.is_active,
            "region_id": self.region_id,
            "ghost_strikes": self.ghost_strikes,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<User id={self.id} name={self.name} role={self.role}>"
