from datetime import datetime

from sqlalchemy import (
    Column, Integer, String, Boolean, DateTime, ForeignKey, Numeric, JSON,
)

from core.database.db_connection import Base


class Item(Base):
    """
    SQLAlchemy model for the 'items' table.

    Fields:
        id (int): Primary key, auto-incremented.
        cart_type_id (int): Reference to the associated cart type (FK -> cart_types.id).
        name (str): Item name (unique per cart_type, enforced in service layer).
        description (str): Optional description of the item.
        price (Decimal): Item price (10 digits, 2 decimal places).
        image_urls (list): Optional list of image URL strings (stored as JSON).
        is_available (bool): Whether the item is currently available.
        created_at (datetime): Timestamp of record creation (immutable).
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "items"

    id = Column(Integer, primary_key=True, autoincrement=True)
    cart_type_id = Column(
        Integer, ForeignKey("cart_types.id"), nullable=False, index=True,
    )
    name = Column(String, nullable=False)
    description = Column(String, nullable=True, default="")
    price = Column(Numeric(10, 2), nullable=False)
    image_urls = Column(JSON, nullable=True, default=list)
    is_available = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow,
    )

    def to_dict(self) -> dict:
        """Serialize item to dictionary (backward-compatible with service layer)."""
        return {
            "id": self.id,
            "cart_type_id": self.cart_type_id,
            "name": self.name,
            "description": self.description or "",
            "price": float(self.price) if self.price is not None else 0.0,
            "image_urls": self.image_urls or [],
            "is_available": self.is_available,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<Item id={self.id} name={self.name} cart_type_id={self.cart_type_id}>"
