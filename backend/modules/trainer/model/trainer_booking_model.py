from datetime import datetime

from sqlalchemy import Column, DateTime, ForeignKey, Integer, Numeric, String

from core.database.db_connection import Base


class TrainerBooking(Base):
    """A session booking with a trainer, paid via the same manual-UPI-reference +
    admin-approval workflow used for shop orders and booking payments."""

    __tablename__ = "trainer_bookings"

    id = Column(Integer, primary_key=True, autoincrement=True)
    trainer_id = Column(Integer, ForeignKey("trainers.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    session_date = Column(String(10), nullable=False)  # "YYYY-MM-DD" — no calendar validation in v1
    session_time = Column(String(5), nullable=False)   # "HH:MM"
    status = Column(String(30), nullable=False, default="PENDING_PAYMENT")
    amount = Column(Numeric(10, 2), nullable=False)
    reference_code = Column(String, nullable=False, unique=True, index=True)
    transaction_id = Column(String, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "trainer_id": self.trainer_id,
            "user_id": self.user_id,
            "session_date": self.session_date,
            "session_time": self.session_time,
            "status": self.status,
            "amount": float(self.amount) if self.amount is not None else 0.0,
            "reference_code": self.reference_code,
            "transaction_id": self.transaction_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<TrainerBooking id={self.id} trainer_id={self.trainer_id} status={self.status}>"
