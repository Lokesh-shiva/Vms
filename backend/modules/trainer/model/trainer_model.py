from datetime import datetime

from sqlalchemy import Boolean, Column, DateTime, Integer, Numeric, String

from core.database.db_connection import Base


class Trainer(Base):
    """Admin-managed trainer/coach profile — v1 has no self-signup or KYC,
    admin adds trainers directly (same trust model as a captain being
    manually reviewed, but without the applicant flow)."""

    __tablename__ = "trainers"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, nullable=False)
    bio = Column(String, nullable=True, default="")
    specialties = Column(String, nullable=True, default="")  # comma-separated sport names
    rate_per_session = Column(Numeric(10, 2), nullable=False)
    image_url = Column(String, nullable=True)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "bio": self.bio or "",
            "specialties": self.specialties or "",
            "rate_per_session": float(self.rate_per_session) if self.rate_per_session is not None else 0.0,
            "image_url": self.image_url,
            "is_active": self.is_active,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<Trainer id={self.id} name={self.name}>"
