import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.user.model.user_model import User  # noqa: F401 — registers model
from modules.location.model.location_model import Location  # noqa: F401 — registers model
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401 — registers model
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401 — registers model
from modules.cart.model.cart_model import Cart  # noqa: F401 — registers model
from modules.item.model.item_model import Item  # noqa: F401 — registers model
from modules.booking.model.booking_model import Booking  # noqa: F401 — registers model
from modules.booking_item.model.booking_item_model import BookingItem  # noqa: F401 — registers model

from modules.booking.repository.booking_repository import BookingRepository
from modules.booking.service.booking_service import BookingService
from modules.user.repository.user_repository import UserRepository
from modules.location.repository.location_repository import LocationRepository
from modules.cart_type.repository.cart_type_repository import CartTypeRepository
from modules.timeslot.repository.timeslot_repository import TimeslotRepository
from modules.cart.repository.cart_repository import CartRepository
from modules.booking_item.repository.booking_item_repository import BookingItemRepository
from modules.booking_item.service.booking_item_service import BookingItemService
from modules.item.repository.item_repository import ItemRepository


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestBookingService(unittest.TestCase):
    """Unit tests for BookingService operations."""

    def setUp(self):
        """Set up isolated repositories pre-populated with test data."""
        # SQLite session factory for DB-backed repos (User, Location, CartType)
        test_session_factory = _make_test_session_factory()

        # User repo — backed by SQLite, not Neon
        self.user_repo = UserRepository(session_factory=test_session_factory)
        self.user_repo.create({"name": "Alice", "phone": "9000000001"})  # id=1

        # Location (region) repo — backed by SQLite
        self.location_repo = LocationRepository(session_factory=test_session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})  # id=1

        # Cart type repo — backed by SQLite
        self.cart_type_repo = CartTypeRepository(session_factory=test_session_factory)
        self.cart_type_repo.create({"name": "Standard"})  # id=1

        # Timeslot repo — capacity=2 (SQLite-backed)
        self.timeslot_repo = TimeslotRepository(session_factory=test_session_factory)
        self.timeslot_repo.create({
            "location_id": 1,
            "date": "2026-03-01",
            "start_time": "09:00",
            "end_time": "10:00",
            "capacity": 2,
        })  # id=1

        # Cart repo — one available cart (SQLite-backed)
        self.cart_repo = CartRepository(session_factory=test_session_factory)
        self.cart_repo.create({
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        })  # id=1

        # Item repo (for BookingItemService) — SQLite-backed
        self.item_repo = ItemRepository(session_factory=test_session_factory)

        # BookingItem service with isolated repos
        booking_item_repo = BookingItemRepository(session_factory=test_session_factory)
        booking_item_service = BookingItemService(
            booking_item_repository=booking_item_repo,
            item_repository=self.item_repo,
        )

        # Booking service with all isolated repos
        self.service = BookingService(
            booking_repository=BookingRepository(session_factory=test_session_factory),
            user_repository=self.user_repo,
            location_repository=self.location_repo,
            cart_type_repository=self.cart_type_repo,
            timeslot_repository=self.timeslot_repo,
            cart_repository=self.cart_repo,
            booking_item_service=booking_item_service,
        )

    def _valid_data(self, **overrides) -> dict:
        """Return a baseline valid booking payload, with optional overrides."""
        base = {
            "user_id": 1,
            "region_id": 1,
            "cart_type_id": 1,
            "timeslot_id": 1,
            "address": "123 Main Street",
            "booking_fee": 50.0,
        }
        base.update(overrides)
        return base

    # ── Create: Valid Booking ─────────────────────────────────────────

    def test_create_booking_success(self):
        """Creating a booking with valid data assigns a cart and confirms."""
        result = self.service.create_booking(self._valid_data())
        self.assertIsNotNone(result)
        self.assertEqual(result["status"], "CONFIRMED")
        self.assertEqual(result["payment_status"], "SUCCESS")
        self.assertEqual(result["assigned_cart_id"], 1)
        self.assertEqual(result["refund_status"], "NONE")
        self.assertEqual(result["address"], "123 Main Street")
        self.assertEqual(result["booking_fee"], 50.0)
        # No items → estimated_total is 0.0 (server-computed)
        self.assertEqual(result["estimated_total"], 0.0)
        self.assertIn("id", result)
        self.assertIn("created_at", result)
        self.assertIn("updated_at", result)

        # Verify cart status changed to BUSY
        cart = self.cart_repo.find_by_id(1)
        self.assertEqual(cart["status"], "BUSY")

    # ── Create: No Cart Available ─────────────────────────────────────

    def test_create_booking_no_cart_available(self):
        """Booking fails entirely when no AVAILABLE cart exists."""
        # Make the only cart BUSY
        self.cart_repo.update(1, {"status": "BUSY"})

        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data())
        self.assertIn("No available cart", str(ctx.exception))

    # ── Create: Slot Full ─────────────────────────────────────────────

    def test_create_booking_slot_full(self):
        """Booking fails when timeslot is at capacity."""
        # Add a second cart so cart availability doesn't block us
        self.cart_repo.create({
            "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
        })  # id=2

        # Fill the slot (capacity=2)
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())

        # Third booking should fail — slot is full
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data())
        self.assertIn("fully booked", str(ctx.exception))

    # ── Create: User Daily Limit Exceeded ─────────────────────────────

    def test_create_booking_user_limit_exceeded(self):
        """Booking fails when user exceeds 3 bookings per day."""
        # Create a high-capacity slot and enough carts
        self.timeslot_repo.update(1, {"capacity": 10})
        for _ in range(4):
            self.cart_repo.create({
                "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
            })

        # Create 3 bookings (max per day)
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())

        # Fourth booking should fail
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data())
        self.assertIn("maximum", str(ctx.exception))

    # ── Create: Invalid FK ────────────────────────────────────────────

    def test_create_booking_invalid_user(self):
        """Booking fails with non-existent user."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(user_id=999))
        self.assertIn("user does not exist", str(ctx.exception))

    def test_create_booking_invalid_region(self):
        """Booking fails with non-existent region."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(region_id=999))
        self.assertIn("region does not exist", str(ctx.exception))

    def test_create_booking_invalid_cart_type(self):
        """Booking fails with non-existent cart type."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(cart_type_id=999))
        self.assertIn("cart type does not exist", str(ctx.exception))

    def test_create_booking_invalid_timeslot(self):
        """Booking fails with non-existent timeslot."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(timeslot_id=999))
        self.assertIn("timeslot does not exist", str(ctx.exception))

    # ── Cancel: Releases Cart ─────────────────────────────────────────

    def test_cancel_booking_releases_cart(self):
        """Cancelling a booking releases the assigned cart back to AVAILABLE."""
        booking = self.service.create_booking(self._valid_data())

        # Cart should be BUSY after booking
        cart = self.cart_repo.find_by_id(booking["assigned_cart_id"])
        self.assertEqual(cart["status"], "BUSY")

        # Cancel the booking
        cancelled = self.service.cancel_booking(booking["id"])
        self.assertEqual(cancelled["status"], "CANCELLED")
        self.assertEqual(cancelled["refund_status"], "REFUNDED")
        self.assertEqual(cancelled["refund_amount"], 50.0)

        # Cart should now be AVAILABLE
        cart = self.cart_repo.find_by_id(booking["assigned_cart_id"])
        self.assertEqual(cart["status"], "AVAILABLE")

    def test_cancel_booking_not_found(self):
        """Cancelling a non-existent booking raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.cancel_booking(999)
        self.assertIn("not found", str(ctx.exception))

    def test_cancel_booking_already_cancelled(self):
        """Cannot cancel an already cancelled booking."""
        booking = self.service.create_booking(self._valid_data())
        self.service.cancel_booking(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.service.cancel_booking(booking["id"])
        self.assertIn("Only confirmed", str(ctx.exception))

    # ── Complete: Releases Cart ───────────────────────────────────────

    def test_complete_booking_releases_cart(self):
        """Completing a booking releases the assigned cart back to AVAILABLE."""
        booking = self.service.create_booking(self._valid_data())

        # Cart should be BUSY after booking
        cart = self.cart_repo.find_by_id(booking["assigned_cart_id"])
        self.assertEqual(cart["status"], "BUSY")

        # Complete the booking
        completed = self.service.complete_booking(booking["id"])
        self.assertEqual(completed["status"], "COMPLETED")

        # Cart should now be AVAILABLE
        cart = self.cart_repo.find_by_id(booking["assigned_cart_id"])
        self.assertEqual(cart["status"], "AVAILABLE")

    def test_complete_booking_not_found(self):
        """Completing a non-existent booking raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.complete_booking(999)
        self.assertIn("not found", str(ctx.exception))

    def test_complete_booking_already_completed(self):
        """Cannot complete an already completed booking."""
        booking = self.service.create_booking(self._valid_data())
        self.service.complete_booking(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.service.complete_booking(booking["id"])
        self.assertIn("Only confirmed", str(ctx.exception))

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_booking_found(self):
        """Retrieving an existing booking returns the correct record."""
        created = self.service.create_booking(self._valid_data())
        fetched = self.service.get_booking(created["id"])
        self.assertEqual(fetched["user_id"], 1)
        self.assertEqual(fetched["status"], "CONFIRMED")

    def test_get_booking_not_found(self):
        """Retrieving a non-existent booking returns None."""
        result = self.service.get_booking(999)
        self.assertIsNone(result)

    def test_list_bookings(self):
        """Listing bookings returns all created records."""
        # Add a second cart for the second booking
        self.cart_repo.create({
            "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
        })
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())
        bookings = self.service.list_bookings()
        self.assertEqual(len(bookings), 2)


if __name__ == "__main__":
    unittest.main()
