from datetime import datetime

from sqlalchemy import func

from core.database.db_connection import SessionLocal
from modules.booking.model.booking_model import Booking


class BookingRepository:
    """
    Data access layer for Booking entities.

    Responsibilities:
    - Performs CRUD operations against the database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via Booking.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, booking_data: dict, session=None) -> dict:
        """Insert a new booking record. Returns the created booking as a dict."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            booking = Booking(
                user_id=booking_data.get("user_id"),
                region_id=booking_data.get("region_id"),
                cart_type_id=booking_data.get("cart_type_id"),
                timeslot_id=booking_data.get("timeslot_id"),
                assigned_cart_id=booking_data.get("assigned_cart_id"),
                address=booking_data.get("address", ""),
                booking_fee=booking_data.get("booking_fee", 0.0),
                estimated_total=booking_data.get("estimated_total", 0.0),
                status=booking_data.get("status", "PENDING_PAYMENT"),
                payment_status=booking_data.get("payment_status", "PENDING"),
                refund_status=booking_data.get("refund_status", "NONE"),
                refund_amount=booking_data.get("refund_amount", 0.0),
                cancellation_fee_pct_snapshot=booking_data.get(
                    "cancellation_fee_pct_snapshot", 0
                ),
                platform_fee_pct_snapshot=booking_data.get(
                    "platform_fee_pct_snapshot", 0
                ),
                date=booking_data.get("date"),
            )
            session.add(booking)
            if own_session:
                session.commit()
                session.refresh(booking)
            else:
                session.flush()
                session.refresh(booking)
            return booking.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def find_by_id(self, booking_id: int, session=None) -> dict | None:
        """Retrieve a booking by ID."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            booking = session.query(Booking).filter(Booking.id == booking_id).first()
            return booking.to_dict() if booking else None
        finally:
            if own_session:
                session.close()

    def find_all(self, session=None) -> list[dict]:
        """Retrieve all bookings."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            bookings = session.query(Booking).all()
            return [b.to_dict() for b in bookings]
        finally:
            if own_session:
                session.close()

    def find_by_user_id(self, user_id: int, session=None) -> list[dict]:
        """Retrieve all bookings for a specific user."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            bookings = session.query(Booking).filter(Booking.user_id == user_id).all()
            return [b.to_dict() for b in bookings]
        finally:
            if own_session:
                session.close()

    def find_by_user_and_date(
        self, user_id: int, date: str, session=None
    ) -> list[dict]:
        """Retrieve bookings for a specific user on a specific date."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            bookings = (
                session.query(Booking)
                .filter(Booking.user_id == user_id, Booking.date == date)
                .all()
            )
            return [b.to_dict() for b in bookings]
        finally:
            if own_session:
                session.close()

    def count_by_timeslot(self, timeslot_id: int, session=None) -> int:
        """Count capacity-consuming bookings for a specific timeslot.

        Only CONFIRMED and IN_PROGRESS bookings consume slot capacity.
        PENDING_PAYMENT, CANCELLED, and EXPIRED do not.
        """
        own_session = session is None
        session = session or self._session_factory()
        try:
            return (
                session.query(func.count(Booking.id))
                .filter(
                    Booking.timeslot_id == timeslot_id,
                    Booking.status.in_(("CONFIRMED", "IN_PROGRESS")),
                )
                .scalar()
                or 0
            )
        finally:
            if own_session:
                session.close()

    def update(self, booking_id: int, update_data: dict, session=None) -> dict | None:
        """Update an existing booking record. Automatically refreshes updated_at."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            booking = session.query(Booking).filter(Booking.id == booking_id).first()
            if not booking:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(
                    booking, key
                ):
                    setattr(booking, key, value)

            booking.updated_at = datetime.utcnow()
            if own_session:
                session.commit()
                session.refresh(booking)
            else:
                session.flush()
                session.refresh(booking)
            return booking.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def delete(self, booking_id: int, session=None) -> bool:
        """Delete a booking record by ID."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            booking = session.query(Booking).filter(Booking.id == booking_id).first()
            if not booking:
                return False
            session.delete(booking)
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


# ── Shared module-level instance ──────────────────────────────────────
# All services must import and use this singleton so they share the same
# in-memory store during V1.  When a real DB replaces the store, this
# pattern naturally becomes a session-scoped dependency instead.

booking_repository = BookingRepository()
