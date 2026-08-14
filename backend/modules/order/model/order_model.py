from datetime import datetime

from sqlalchemy import Column, DateTime, ForeignKey, Integer, Numeric, String

from core.database.db_connection import Base


class OrderStatus:
    """Mirrors the existing manual-UPI payment workflow's status names
    (PaymentModel: PENDING -> UNDER_REVIEW -> SUCCESS/FAILED)."""

    PENDING_PAYMENT = "PENDING_PAYMENT"
    UNDER_REVIEW = "UNDER_REVIEW"
    PAID = "PAID"
    REJECTED = "REJECTED"
    CANCELLED = "CANCELLED"

    ALL: frozenset[str] = frozenset(
        {PENDING_PAYMENT, UNDER_REVIEW, PAID, REJECTED, CANCELLED}
    )


class Order(Base):
    """A shop order — food/equipment items bought via the manual-UPI flow
    (no payment gateway, matching the rest of this project's architecture)."""

    __tablename__ = "orders"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    status = Column(String(30), nullable=False, default=OrderStatus.PENDING_PAYMENT)
    total_amount = Column(Numeric(10, 2), nullable=False)
    reference_code = Column(String, nullable=False, unique=True, index=True)
    transaction_id = Column(String, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "status": self.status,
            "total_amount": float(self.total_amount) if self.total_amount is not None else 0.0,
            "reference_code": self.reference_code,
            "transaction_id": self.transaction_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<Order id={self.id} user_id={self.user_id} status={self.status}>"
