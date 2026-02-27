from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, DateTime

from core.database.db_connection import Base


class Cart(Base):
    """
    SQLAlchemy model for the 'carts' table.

    Fields:
        id (int): Primary key, auto-incremented.
        region_id (int): Reference to the associated location/region (FK → locations.id).
        cart_type_id (int): Reference to the associated cart type (FK → cart_types.id).
        status (str): Current status (AVAILABLE, BUSY, BUFFER, OFFLINE).
        is_active (bool): Whether the cart is currently active.
        created_at (datetime): Timestamp of record creation (immutable).
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "carts"

    VALID_STATUSES = {"AVAILABLE", "BUSY", "BUFFER", "OFFLINE"}

    id = Column(Integer, primary_key=True, autoincrement=True)
    region_id = Column(Integer, ForeignKey("locations.id"), nullable=False, index=True)
    cart_type_id = Column(Integer, ForeignKey("cart_types.id"), nullable=False, index=True)
    status = Column(String, nullable=False, default="AVAILABLE")
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize cart to dictionary (backward-compatible with service layer)."""
        return {
            "id": self.id,
            "region_id": self.region_id,
            "cart_type_id": self.cart_type_id,
            "status": self.status,
            "is_active": self.is_active,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<Cart id={self.id} region_id={self.region_id} status={self.status}>"
