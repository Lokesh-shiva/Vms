from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, DateTime

from core.database.db_connection import Base


class CartType(Base):
    """
    SQLAlchemy model for the 'cart_types' table.

    Fields:
        id (int): Primary key, auto-incremented.
        name (str): Cart type name (unique, indexed).
        description (str): Optional description of the cart type.
        is_active (bool): Whether the cart type is currently active.
        created_at (datetime): Timestamp of record creation (immutable).
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "cart_types"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True, nullable=False, index=True)
    description = Column(String, nullable=True, default="")
    is_active = Column(Boolean, nullable=False, default=True)
    image_url = Column(String, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize cart type to dictionary (backward-compatible with service layer)."""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description or "",
            "is_active": self.is_active,
            "image_url": self.image_url,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<CartType id={self.id} name={self.name}>"
