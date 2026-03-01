import unittest
from datetime import datetime, timedelta

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
from modules.payment.model.payment_model import Payment  # noqa: F401 — registers model

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
from modules.payment.repository.payment_repository import PaymentRepository
from modules.payment.service.payment_service import PaymentService


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestBookingService(unittest.TestCase):
    """Unit tests for BookingService operations."""

    def setUp(self):
        """Set up isolated repositories pre-populated with test data."""
        test_session_factory = _make_test_session_factory()

        self.user_repo = UserRepository(session_factory=test_session_factory)
        self.user_repo.create({"name": "Alice", "phone": "9000000001"})  # id=1

        self.location_repo = LocationRepository(session_factory=test_session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})  # id=1

        self.cart_type_repo = CartTypeRepository(session_factory=test_session_factory)
        self.cart_type_repo.create({"name": "Standard"})  # id=1

        self.timeslot_repo = TimeslotRepository(session_factory=test_session_factory)
        self.timeslot_repo.create({
            "location_id": 1,
            "date": "2026-03-01",
            "start_time": "09:00",
            "end_time": "10:00",
            "capacity": 2,
        })  # id=1

        self.cart_repo = CartRepository(session_factory=test_session_factory)
        self.cart_repo.create({
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        })  # id=1

        self.item_repo = ItemRepository(session_factory=test_session_factory)

        booking_item_repo = BookingItemRepository(session_factory=test_session_factory)
        booking_item_service = BookingItemService(
            booking_item_repository=booking_item_repo,
            item_repository=self.item_repo,
        )

        self.booking_repo = BookingRepository(session_factory=test_session_factory)
        self.payment_repo = PaymentRepository(session_factory=test_session_factory)

        self.payment_service = PaymentService(
            payment_repository=self.payment_repo,
            booking_repository=self.booking_repo,
        )

        self.service = BookingService(
            booking_repository=self.booking_repo,
            user_repository=self.user_repo,
            location_repository=self.location_repo,
            cart_type_repository=self.cart_type_repo,
            timeslot_repository=self.timeslot_repo,
            cart_repository=self.cart_repo,
            booking_item_service=booking_item_service,
            payment_repository=self.payment_repo,
            payment_service=self.payment_service,
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

    def _create_and_pay(self, **overrides) -> dict:
        """Helper: create a booking, initiate payment, confirm payment, approve, then confirm booking."""
        booking = self.service.create_booking(self._valid_data(**overrides))
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        payment = self.payment_repo.find_by_booking_id(booking["id"])
        self.payment_service.approve_payment(payment["id"])
        confirmed = self.service.confirm_booking(booking["id"])
        return confirmed

    # ── Create: PENDING_PAYMENT ──────────────────────────────────────

    def test_create_booking_success(self):
        """Creating a booking starts in PENDING_PAYMENT with no cart assigned."""
        result = self.service.create_booking(self._valid_data())
        self.assertIsNotNone(result)
        self.assertEqual(result["status"], "PENDING_PAYMENT")
        self.assertEqual(result["payment_status"], "PENDING")
        self.assertIsNone(result["assigned_cart_id"])
        self.assertEqual(result["refund_status"], "NONE")
        self.assertEqual(result["address"], "123 Main Street")
        self.assertEqual(result["booking_fee"], 50.0)
        self.assertEqual(result["estimated_total"], 0.0)
        self.assertIn("id", result)
        self.assertIn("created_at", result)

        # Cart should still be AVAILABLE (not locked)
        cart = self.cart_repo.find_by_id(1)
        self.assertEqual(cart["status"], "AVAILABLE")

    # ── Create: Validation Failures ──────────────────────────────────

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

    # ── Create: Daily User Limit ─────────────────────────────────────

    def test_create_booking_user_limit_exceeded(self):
        """Booking fails when user exceeds 3 bookings per day."""
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())

        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data())
        self.assertIn("maximum", str(ctx.exception))

    def test_daily_limit_ignores_cancelled_bookings(self):
        """Cancelled bookings do not count toward the daily limit."""
        b1 = self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())

        self.service.cancel_booking(b1["id"])

        # Should succeed since one was cancelled
        result = self.service.create_booking(self._valid_data())
        self.assertEqual(result["status"], "PENDING_PAYMENT")

    # ── Confirm: Full Flow ───────────────────────────────────────────

    def test_confirm_booking_success(self):
        """Confirm booking after payment approval assigns cart and sets CONFIRMED."""
        booking = self.service.create_booking(self._valid_data())
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        payment = self.payment_repo.find_by_booking_id(booking["id"])
        self.payment_service.approve_payment(payment["id"])

        confirmed = self.service.confirm_booking(booking["id"])
        self.assertEqual(confirmed["status"], "CONFIRMED")
        self.assertEqual(confirmed["assigned_cart_id"], 1)

        # Cart should now be BUSY
        cart = self.cart_repo.find_by_id(1)
        self.assertEqual(cart["status"], "BUSY")

    def test_confirm_booking_without_payment_fails(self):
        """Cannot confirm a booking that has no payment."""
        booking = self.service.create_booking(self._valid_data())
        with self.assertRaises(ValueError) as ctx:
            self.service.confirm_booking(booking["id"])
        self.assertIn("No payment found", str(ctx.exception))

    def test_confirm_booking_with_pending_payment_fails(self):
        """Cannot confirm a booking whose payment is still PENDING."""
        booking = self.service.create_booking(self._valid_data())
        self.payment_service.initiate_payment(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.service.confirm_booking(booking["id"])
        self.assertIn("PENDING", str(ctx.exception))

    def test_confirm_booking_no_cart_available(self):
        """Cannot confirm when no cart is available at confirmation time."""
        booking = self.service.create_booking(self._valid_data())
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        payment = self.payment_repo.find_by_booking_id(booking["id"])
        self.payment_service.approve_payment(payment["id"])

        # Make the only cart BUSY
        self.cart_repo.update(1, {"status": "BUSY"})

        with self.assertRaises(ValueError) as ctx:
            self.service.confirm_booking(booking["id"])
        self.assertIn("No cart available", str(ctx.exception))

    def test_confirm_expired_booking_fails(self):
        """Cannot confirm an expired booking."""
        booking = self.service.create_booking(self._valid_data())

        # Manually expire it
        self.booking_repo.update(booking["id"], {"status": "EXPIRED"})

        with self.assertRaises(ValueError) as ctx:
            self.service.confirm_booking(booking["id"])
        self.assertIn("EXPIRED", str(ctx.exception))

    # ── Capacity: Counts CONFIRMED + IN_PROGRESS only ────────────────

    def test_capacity_does_not_count_pending_payment(self):
        """PENDING_PAYMENT bookings do NOT consume slot capacity."""
        self.cart_repo.create({
            "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
        })

        # Create 2 PENDING_PAYMENT bookings (capacity=2)
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())

        # Capacity is NOT exhausted because PENDING_PAYMENT doesn't count
        # But we'll hit daily limit with 3, so create a third
        # Actually just check the count directly
        count = self.booking_repo.count_by_timeslot(1)
        self.assertEqual(count, 0)

    # ── Cancel: Hardened Rules ───────────────────────────────────────

    def test_cancel_pending_payment_booking(self):
        """Cancelling a PENDING_PAYMENT booking succeeds without side effects."""
        booking = self.service.create_booking(self._valid_data())
        cancelled = self.service.cancel_booking(booking["id"])
        self.assertEqual(cancelled["status"], "CANCELLED")

        # Cart still AVAILABLE (was never assigned)
        cart = self.cart_repo.find_by_id(1)
        self.assertEqual(cart["status"], "AVAILABLE")

    def test_cancel_confirmed_booking_releases_cart(self):
        """Cancelling a CONFIRMED booking releases the cart."""
        confirmed = self._create_and_pay()

        cart = self.cart_repo.find_by_id(confirmed["assigned_cart_id"])
        self.assertEqual(cart["status"], "BUSY")

        cancelled = self.service.cancel_booking(confirmed["id"])
        self.assertEqual(cancelled["status"], "CANCELLED")

        cart = self.cart_repo.find_by_id(confirmed["assigned_cart_id"])
        self.assertEqual(cart["status"], "AVAILABLE")

    def test_cancel_confirmed_booking_triggers_refund(self):
        """Cancelling a CONFIRMED booking with SUCCESS payment triggers refund."""
        confirmed = self._create_and_pay()

        self.service.cancel_booking(confirmed["id"])

        payment = self.payment_repo.find_by_booking_id(confirmed["id"])
        self.assertEqual(payment["status"], "REFUNDED")

    def test_cancel_booking_not_found(self):
        """Cancelling a non-existent booking raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.cancel_booking(999)
        self.assertIn("not found", str(ctx.exception))

    def test_cancel_already_cancelled(self):
        """Cannot cancel an already cancelled booking."""
        booking = self.service.create_booking(self._valid_data())
        self.service.cancel_booking(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.service.cancel_booking(booking["id"])
        self.assertIn("Cannot cancel", str(ctx.exception))

    def test_cancel_completed_booking_blocked(self):
        """Cannot cancel a completed booking."""
        confirmed = self._create_and_pay()
        self.service.complete_booking(confirmed["id"])

        with self.assertRaises(ValueError) as ctx:
            self.service.cancel_booking(confirmed["id"])
        self.assertIn("Cannot cancel", str(ctx.exception))

    # ── Complete ─────────────────────────────────────────────────────

    def test_complete_booking_releases_cart(self):
        """Completing a booking releases the assigned cart back to AVAILABLE."""
        confirmed = self._create_and_pay()

        cart = self.cart_repo.find_by_id(confirmed["assigned_cart_id"])
        self.assertEqual(cart["status"], "BUSY")

        completed = self.service.complete_booking(confirmed["id"])
        self.assertEqual(completed["status"], "COMPLETED")

        cart = self.cart_repo.find_by_id(confirmed["assigned_cart_id"])
        self.assertEqual(cart["status"], "AVAILABLE")

    def test_complete_booking_not_found(self):
        """Completing a non-existent booking raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.complete_booking(999)
        self.assertIn("not found", str(ctx.exception))

    def test_complete_booking_already_completed(self):
        """Cannot complete an already completed booking."""
        confirmed = self._create_and_pay()
        self.service.complete_booking(confirmed["id"])

        with self.assertRaises(ValueError) as ctx:
            self.service.complete_booking(confirmed["id"])
        self.assertIn("Only confirmed", str(ctx.exception))

    # ── Read ─────────────────────────────────────────────────────────

    def test_get_booking_found(self):
        """Retrieving an existing booking returns the correct record."""
        created = self.service.create_booking(self._valid_data())
        fetched = self.service.get_booking(created["id"])
        self.assertEqual(fetched["user_id"], 1)
        self.assertEqual(fetched["status"], "PENDING_PAYMENT")

    def test_get_booking_not_found(self):
        """Retrieving a non-existent booking returns None."""
        result = self.service.get_booking(999)
        self.assertIsNone(result)

    def test_list_bookings(self):
        """Listing bookings returns all created records."""
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())
        bookings = self.service.list_bookings()
        self.assertEqual(len(bookings), 2)


if __name__ == "__main__":
    unittest.main()
