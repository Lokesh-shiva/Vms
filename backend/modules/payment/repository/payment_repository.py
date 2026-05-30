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
                user_id=payment_data.get("user_id"),
                match_id=payment_data.get("match_id"),
                provider=payment_data.get("provider", "MANUAL_UPI"),
                payment_type=payment_data.get("payment_type", "MATCHING_FEE"),
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
        """Retrieve the **latest** payment associated with a booking.

        Multiple payments may exist for one booking (FAILED → retry, or split payments).
        Always returns the most recent one (highest id).

        For split payment records use find_by_booking_id_all() or find_by_user_and_booking().
        """
        own_session = session is None
        session = session or self._session_factory()
        try:
            payment = (
                session.query(Payment)
                .filter(Payment.booking_id == booking_id)
                .order_by(Payment.id.desc())
                .first()
            )
            return payment.to_dict() if payment else None
        finally:
            if own_session:
                session.close()

    def find_by_booking_id_all(self, booking_id: int, session=None) -> list[dict]:
        """Retrieve all payment records associated with a booking, ordered by id DESC."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            payments = (
                session.query(Payment)
                .filter(Payment.booking_id == booking_id)
                .order_by(Payment.id.desc())
                .all()
            )
            return [p.to_dict() for p in payments]
        finally:
            if own_session:
                session.close()

    def find_by_user_and_booking(
        self, user_id: int, booking_id: int, session=None
    ) -> dict | None:
        """Retrieve the payment record for a specific user and booking combination."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            payment = (
                session.query(Payment)
                .filter(
                    Payment.user_id == user_id,
                    Payment.booking_id == booking_id,
                )
                .order_by(Payment.id.desc())
                .first()
            )
            return payment.to_dict() if payment else None
        finally:
            if own_session:
                session.close()

    def find_all(self, status: str = None, session=None) -> list[dict]:
        """Retrieve all payments, optionally filtered by status. Ordered by id DESC."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            query = session.query(Payment)
            if status:
                query = query.filter(Payment.status == status)
            payments = query.order_by(Payment.id.desc()).all()
            return [p.to_dict() for p in payments]
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
                if key not in ("id", "created_at", "updated_at") and hasattr(
                    payment, key
                ):
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
