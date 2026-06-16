from datetime import datetime
from enum import Enum

from sqlalchemy import Column, Integer, String, Boolean, DateTime, JSON

from core.database.db_connection import Base


class UserRole(str, Enum):
    """Allowed user roles. All role assignments must use these values."""

    USER = "user"
    SUPER_ADMIN = "super_admin"
    OPS_MANAGER = "ops_manager"
    GROUND_OWNER = "ground_owner"
    TOURNAMENT_MANAGER = "tournament_manager"
    SUPPORT = "support"
    FINANCE = "finance"
    CSR_PARTNER = "csr_partner"


class User(Base):
    """
    SQLAlchemy model for the 'users' table.

    Fields:
        id (int): Primary key, auto-incremented.
        name (str): Full name of the user.
        phone (str): Contact phone number (unique, indexed).
        password_hash (str): Bcrypt hash — never exposed via to_dict().
        role (str): One of the UserRole values (e.g. 'user', 'super_admin').
        is_active (bool): Whether the account is active.
        region_id (int | None): Optional region assignment.
        ghost_strikes (int): Counter for ghost/no-show incidents.
        can_create_society (bool): Whether the user is allowed to create a society.
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
    can_create_society = Column(Boolean, nullable=False, default=False)
    date_of_birth = Column(String, nullable=True)
    city = Column(String, nullable=True)
    sport_preferences = Column(JSON, nullable=True)
    profile_photo_url = Column(String, nullable=True)
    is_profile_complete = Column(Boolean, nullable=False, default=False)
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
            "can_create_society": self.can_create_society,
            "date_of_birth": self.date_of_birth,
            "city": self.city,
            "sport_preferences": self.sport_preferences or [],
            "profile_photo_url": self.profile_photo_url,
            "is_profile_complete": self.is_profile_complete,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<User id={self.id} name={self.name} role={self.role}>"
