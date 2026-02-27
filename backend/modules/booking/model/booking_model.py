from datetime import datetime

from sqlalchemy import Column, DateTime, ForeignKey, Integer, Numeric, String

from core.database.db_connection import Base


class Booking(Base):
    """
    SQLAlchemy model for the 'bookings' table.

    Fields:
        id (int): Primary key, auto-incremented.
        user_id (int): Reference to the associated user (FK -> users.id).
        region_id (int): Reference to the associated location/region (FK -> locations.id).
        cart_type_id (int): Reference to the associated cart type (FK -> cart_types.id).
        timeslot_id (int): Reference to the associated timeslot (FK -> timeslots.id).
        assigned_cart_id (int | None): Optional assigned cart (FK -> carts.id).
        address (str): Delivery/service address.
        booking_fee (Decimal): Booking fee.
        estimated_total (Decimal): Estimated total for booked items.
        status (str): Current booking status.
        payment_status (str): Current payment status.
        refund_status (str): Current refund status.
        refund_amount (Decimal): Refunded amount.
        date (str): Booking date snapshot for daily-limit checks.
        created_at (datetime): Timestamp of record creation.
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "bookings"

    VALID_STATUSES = {
        "PENDING_PAYMENT", "CONFIRMED", "CANCELLED", "COMPLETED", "PAYMENT_FAILED"
    }
    VALID_PAYMENT_STATUSES = {"PENDING", "SUCCESS", "FAILED"}
    VALID_REFUND_STATUSES = {"NONE", "PENDING", "REFUNDED"}

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    region_id = Column(Integer, ForeignKey("locations.id"), nullable=False, index=True)
    cart_type_id = Column(Integer, ForeignKey("cart_types.id"), nullable=False, index=True)
    timeslot_id = Column(Integer, ForeignKey("timeslots.id"), nullable=False, index=True)
    assigned_cart_id = Column(Integer, ForeignKey("carts.id"), nullable=True, index=True)
    address = Column(String, nullable=False)
    booking_fee = Column(Numeric(10, 2), nullable=False)
    estimated_total = Column(Numeric(10, 2), nullable=False)
    status = Column(String, nullable=False)
    payment_status = Column(String, nullable=False)
    refund_status = Column(String, nullable=False)
    refund_amount = Column(Numeric(10, 2), nullable=False, default=0)
    date = Column(String, nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self):
        """Serialize booking to dictionary (backward-compatible with API contract)."""
        return {
            "id": self.id,
            "user_id": self.user_id,
            "region_id": self.region_id,
            "cart_type_id": self.cart_type_id,
            "timeslot_id": self.timeslot_id,
            "assigned_cart_id": self.assigned_cart_id,
            "address": self.address,
            "booking_fee": float(self.booking_fee) if self.booking_fee is not None else 0.0,
            "estimated_total": (
                float(self.estimated_total) if self.estimated_total is not None else 0.0
            ),
            "status": self.status,
            "payment_status": self.payment_status,
            "refund_status": self.refund_status,
            "refund_amount": (
                float(self.refund_amount) if self.refund_amount is not None else 0.0
            ),
            "date": self.date,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return (
            f"<Booking id={self.id} user_id={self.user_id} "
            f"status={self.status} cart={self.assigned_cart_id}>"
        )
