from datetime import datetime

from sqlalchemy import Boolean, Column, DateTime, ForeignKey, Integer, String, Text

from core.database.db_connection import Base


class Society(Base):
    __tablename__ = "societies"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)
    owner_user_id = Column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    region_id = Column(
        Integer, ForeignKey("locations.id", ondelete="CASCADE"), nullable=False, index=True
    )
    sport_id = Column(
        Integer, ForeignKey("sports.id", ondelete="CASCADE"), nullable=False, index=True
    )
    is_public = Column(Boolean, nullable=False, default=True)
    max_members = Column(Integer, nullable=False, default=50)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "owner_user_id": self.owner_user_id,
            "region_id": self.region_id,
            "sport_id": self.sport_id,
            "is_public": self.is_public,
            "max_members": self.max_members,
            "is_active": self.is_active,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return (
            f"<Society id={self.id} name={self.name!r} "
            f"owner_user_id={self.owner_user_id} is_active={self.is_active}>"
        )
