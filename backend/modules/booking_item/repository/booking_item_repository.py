from core.database.db_connection import SessionLocal
from modules.booking_item.model.booking_item_model import BookingItem


class BookingItemRepository:
    """
    Data access layer for BookingItem entities.

    Responsibilities:
    - Performs CRUD operations against the database.
    - Contains no business logic.
    - Returns raw data or model instances to the service layer.
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, booking_item_data: dict, session=None) -> dict:
        """Insert a new booking item record. Returns the created item as a dict."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            booking_item = BookingItem(
                booking_id=booking_item_data.get("booking_id"),
                item_id=booking_item_data.get("item_id"),
                quantity=booking_item_data.get("quantity", 0),
                unit_price=booking_item_data.get("unit_price", 0.0),
            )
            session.add(booking_item)
            if own_session:
                session.commit()
                session.refresh(booking_item)
            else:
                session.flush()
                session.refresh(booking_item)
            return booking_item.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def find_by_id(self, booking_item_id: int, session=None) -> dict | None:
        """Retrieve a booking item by ID."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            booking_item = (
                session.query(BookingItem)
                .filter(BookingItem.id == booking_item_id)
                .first()
            )
            return booking_item.to_dict() if booking_item else None
        finally:
            if own_session:
                session.close()

    def find_by_booking_id(self, booking_id: int, session=None) -> list[dict]:
        """Retrieve all booking items belonging to a specific booking."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            rows = (
                session.query(BookingItem)
                .filter(BookingItem.booking_id == booking_id)
                .all()
            )
            return [row.to_dict() for row in rows]
        finally:
            if own_session:
                session.close()

    def delete(self, booking_item_id: int, session=None) -> bool:
        """Delete a booking item record by ID."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            booking_item = (
                session.query(BookingItem)
                .filter(BookingItem.id == booking_item_id)
                .first()
            )
            if not booking_item:
                return False
            session.delete(booking_item)
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

booking_item_repository = BookingItemRepository()
