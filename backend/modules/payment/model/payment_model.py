from datetime import datetime

from sqlalchemy import (
    Column,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    UniqueConstraint,
)

from core.database.db_connection import Base


class Payment(Base):
    """
    SQLAlchemy model for the 'payments' table.

    Represents the manual payment approval workflow.
    This is the single source of truth for payment state.
    bookings.payment_status is a mirror field updated only by PaymentService.

    Fields:
        id (int): Primary key, auto-incremented.
        booking_id (int): Reference to the associated booking (FK -> bookings.id).
        user_id (int | None): Reference to the player this payment belongs to (FK -> users.id).
                              Populated for post-match split payments; NULL for legacy payments.
        match_id (int | None): Reference to the match that triggered this payment (FK -> matches.id).
                               NULL for legacy manual payments.
        provider (str): Payment provider identifier (default MANUAL_UPI).
        amount (Decimal): Payment amount.
        reference_code (str): Unique payment reference code.
        transaction_id (str | None): User-submitted UPI transaction ID.
        status (str): Current payment workflow status.
        created_at (datetime): Timestamp of record creation.
        updated_at (datetime): Timestamp of last update.
    """

    __tablename__ = "payments"

    VALID_STATUSES = ["PENDING", "UNDER_REVIEW", "SUCCESS", "FAILED", "REFUNDED"]

    __table_args__ = (
        UniqueConstraint("booking_id", "user_id", name="uq_payment_booking_user"),
    )

    id = Column(Integer, primary_key=True, autoincrement=True)
    booking_id = Column(Integer, ForeignKey("bookings.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
    match_id = Column(Integer, ForeignKey("matches.id"), nullable=True, index=True)
    provider = Column(String, nullable=False, default="MANUAL_UPI")
    payment_type = Column(String, nullable=False, default="MATCHING_FEE")
    amount = Column(Numeric(10, 2), nullable=False)
    reference_code = Column(String, nullable=False, unique=True, index=True)
    transaction_id = Column(String, nullable=True)
    status = Column(String, nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self):
        """Serialize payment to dictionary."""
        return {
            "id": self.id,
            "booking_id": self.booking_id,
            "user_id": self.user_id,
            "match_id": self.match_id,
            "provider": self.provider,
            "payment_type": self.payment_type,
            "amount": float(self.amount) if self.amount is not None else 0.0,
            "reference_code": self.reference_code,
            "transaction_id": self.transaction_id,
            "status": self.status,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return (
            f"<Payment id={self.id} booking_id={self.booking_id} "
            f"user_id={self.user_id} status={self.status} ref={self.reference_code}>"
        )
