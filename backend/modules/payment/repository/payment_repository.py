from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.payment.model.payment_model import Payment


class PaymentRepository:
    """
    Data access layer for Payment entities.

    Responsibilities:
    - Performs CRUD operations against the database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via Payment.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, payment_data: dict, session=None) -> dict:
        """Insert a new payment record. Returns the created payment as a dict."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            payment = Payment(
                booking_id=payment_data.get("booking_id"),
                provider=payment_data.get("provider", "MANUAL_UPI"),
                amount=payment_data.get("amount", 0.0),
                reference_code=payment_data.get("reference_code"),
                transaction_id=payment_data.get("transaction_id"),
                status=payment_data.get("status", "PENDING"),
            )
            session.add(payment)
            if own_session:
                session.commit()
                session.refresh(payment)
            else:
                session.flush()
                session.refresh(payment)
            return payment.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def find_by_id(self, payment_id: int, session=None) -> dict | None:
        """Retrieve a payment by ID."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            payment = session.query(Payment).filter(Payment.id == payment_id).first()
            return payment.to_dict() if payment else None
        finally:
            if own_session:
                session.close()

    def find_by_booking_id(self, booking_id: int, session=None) -> dict | None:
        """Retrieve the payment associated with a booking."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            payment = (
                session.query(Payment)
                .filter(Payment.booking_id == booking_id)
                .first()
            )
            return payment.to_dict() if payment else None
        finally:
            if own_session:
                session.close()

    def update(self, payment_id: int, update_data: dict, session=None) -> dict | None:
        """Update an existing payment record. Automatically refreshes updated_at."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            payment = session.query(Payment).filter(Payment.id == payment_id).first()
            if not payment:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(payment, key):
                    setattr(payment, key, value)

            payment.updated_at = datetime.utcnow()
            if own_session:
                session.commit()
                session.refresh(payment)
            else:
                session.flush()
                session.refresh(payment)
            return payment.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def delete(self, payment_id: int, session=None) -> bool:
        """Delete a payment record by ID."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            payment = session.query(Payment).filter(Payment.id == payment_id).first()
            if not payment:
                return False
            session.delete(payment)
            if own_session:
                session.commit()
            else:
                session.flush()
            return True
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()


payment_repository = PaymentRepository()
