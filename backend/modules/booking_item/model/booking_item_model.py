from datetime import datetime

from sqlalchemy import Column, DateTime, ForeignKey, Integer, Numeric

from core.database.db_connection import Base


class BookingItem(Base):
    """
    BookingItem entity representing a line item within a booking.

    Fields:
        id (int): Unique identifier.
        booking_id (int): Reference to the parent booking.
        item_id (int): Reference to the catalogue item.
        quantity (int): Quantity ordered (must be > 0).
        unit_price (Decimal): Price snapshot taken from item.price at booking time.
        created_at (datetime): Timestamp of record creation (immutable).
    """

    __tablename__ = "booking_items"

    id = Column(Integer, primary_key=True, autoincrement=True)
    booking_id = Column(Integer, ForeignKey("bookings.id"), nullable=False, index=True)
    item_id = Column(Integer, ForeignKey("items.id"), nullable=False, index=True)
    quantity = Column(Integer, nullable=False)
    unit_price = Column(Numeric(10, 2), nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self):
        """Serialize booking item to dictionary."""
        return {
            "id": self.id,
            "booking_id": self.booking_id,
            "item_id": self.item_id,
            "quantity": self.quantity,
            "unit_price": float(self.unit_price),
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self):
        return (
            f"<BookingItem id={self.id} booking_id={self.booking_id} "
            f"item_id={self.item_id} qty={self.quantity}>"
        )
