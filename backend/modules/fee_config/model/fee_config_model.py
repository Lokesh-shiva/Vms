from datetime import datetime

from sqlalchemy import (
    Boolean, Column, DateTime, ForeignKey, Integer, Numeric, UniqueConstraint,
)

from core.database.db_connection import Base


class RegionCartTypeConfig(Base):
    """
    SQLAlchemy model for the 'region_cart_type_configs' table.

    Stores admin-configurable booking fee and refund deduction percentages
    for each region + cart type combination.

    Fields:
        id (int): Primary key, auto-incremented.
        region_id (int): Reference to the associated region (FK -> locations.id).
        cart_type_id (int): Reference to the associated cart type (FK -> cart_types.id).
        booking_fee (Decimal): Fee charged for a booking (>= 0).
        cancellation_fee_pct (Decimal): Percentage deducted on cancellation (0-100).
        platform_fee_pct (Decimal): Platform fee percentage (0-100).
        is_active (bool): Whether this config is currently active.
        created_at (datetime): Timestamp of record creation.
        updated_at (datetime): Timestamp of last update.

    Constraints:
        Unique(region_id, cart_type_id) — one config per region + cart type combo.
    """

    __tablename__ = "region_cart_type_configs"

    __table_args__ = (
        UniqueConstraint("region_id", "cart_type_id", name="uq_region_cart_type"),
    )

    id = Column(Integer, primary_key=True, autoincrement=True)
    region_id = Column(Integer, ForeignKey("locations.id"), nullable=False, index=True)
    cart_type_id = Column(Integer, ForeignKey("cart_types.id"), nullable=False, index=True)
    booking_fee = Column(Numeric(10, 2), nullable=False)
    cancellation_fee_pct = Column(Numeric(5, 2), nullable=False, default=0)
    platform_fee_pct = Column(Numeric(5, 2), nullable=False, default=0)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        """Serialize config to dictionary."""
        return {
            "id": self.id,
            "region_id": self.region_id,
            "cart_type_id": self.cart_type_id,
            "booking_fee": float(self.booking_fee) if self.booking_fee is not None else 0.0,
            "cancellation_fee_pct": (
                float(self.cancellation_fee_pct)
                if self.cancellation_fee_pct is not None else 0.0
            ),
            "platform_fee_pct": (
                float(self.platform_fee_pct)
                if self.platform_fee_pct is not None else 0.0
            ),
            "is_active": self.is_active,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return (
            f"<RegionCartTypeConfig id={self.id} "
            f"region={self.region_id} cart_type={self.cart_type_id}>"
        )
