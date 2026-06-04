from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, Float, ForeignKey, DateTime

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

    Cart lifecycle rules:
        - ``status`` is **system-controlled** (AVAILABLE → BUSY → AVAILABLE).
          Transitions are driven exclusively by booking operations
          (confirm → BUSY, cancel/complete → AVAILABLE).
        - ``is_active`` is **admin-controlled**.  Admins toggle this to
          take a cart out of rotation without disrupting in-progress
          bookings.
        - The admin API must never allow direct modification of ``status``.
        - New carts always start as AVAILABLE (model default).
    """

    __tablename__ = "carts"

    VALID_STATUSES = {"AVAILABLE", "BUSY", "BUFFER", "OFFLINE"}

    id = Column(Integer, primary_key=True, autoincrement=True)
    label = Column(String, nullable=False, default="")
    region_id = Column(Integer, ForeignKey("locations.id"), nullable=False, index=True)
    cart_type_id = Column(
        Integer, ForeignKey("cart_types.id"), nullable=False, index=True
    )
    status = Column(String, nullable=False, default="AVAILABLE")
    is_active = Column(Boolean, nullable=False, default=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)
    owner_user_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize cart to dictionary (backward-compatible with service layer)."""
        return {
            "id": self.id,
            "label": self.label,
            "region_id": self.region_id,
            "cart_type_id": self.cart_type_id,
            "status": self.status,
            "is_active": self.is_active,
            "latitude": self.latitude,
            "longitude": self.longitude,
            "owner_user_id": self.owner_user_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<Cart id={self.id} region_id={self.region_id} status={self.status}>"
