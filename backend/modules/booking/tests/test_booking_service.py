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
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig  # noqa: F401 — registers model

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
from modules.fee_config.repository.fee_config_repository import FeeConfigRepository


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
            "capacity": 5,
        })  # id=1

        self.cart_repo = CartRepository(session_factory=test_session_factory)
        self.cart_repo.create({
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        })  # id=1
        self.cart_repo.create({
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        })  # id=2
        self.cart_repo.create({
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        })  # id=3

        self.item_repo = ItemRepository(session_factory=test_session_factory)

        booking_item_repo = BookingItemRepository(session_factory=test_session_factory)
        booking_item_service = BookingItemService(
            booking_item_repository=booking_item_repo,
            item_repository=self.item_repo,
        )

        self.booking_repo = BookingRepository(session_factory=test_session_factory)
        self.payment_repo = PaymentRepository(session_factory=test_session_factory)

        # Fee config — active for region=1, cart_type=1
        self.fee_config_repo = FeeConfigRepository(session_factory=test_session_factory)
        self.fee_config_repo.create({
            "region_id": 1,
            "cart_type_id": 1,
            "booking_fee": 50.0,
            "cancellation_fee_pct": 10.0,
            "platform_fee_pct": 5.0,
            "is_active": True,
        })

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
            fee_config_repository=self.fee_config_repo,
        )

    def _valid_data(self, **overrides) -> dict:
        """Return a baseline valid booking payload, with optional overrides."""
        base = {
            "user_id": 1,
            "region_id": 1,
            "cart_type_id": 1,
            "timeslot_id": 1,
            "address": "123 Main Street",
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
        """Creating a booking starts in PENDING_PAYMENT with fee from config."""
        result = self.service.create_booking(self._valid_data())
        self.assertIsNotNone(result)
        self.assertEqual(result["status"], "PENDING_PAYMENT")
        self.assertEqual(result["payment_status"], "PENDING")
        self.assertIsNone(result["assigned_cart_id"])
        self.assertEqual(result["refund_status"], "NONE")
        self.assertEqual(result["address"], "123 Main Street")
        # Fee comes from config, not user input
        self.assertEqual(result["booking_fee"], 50.0)
        self.assertEqual(result["estimated_total"], 0.0)
        # Snapshot fields captured from config
        self.assertEqual(result["cancellation_fee_pct_snapshot"], 10.0)
        self.assertEqual(result["platform_fee_pct_snapshot"], 5.0)
        self.assertIn("id", result)
        self.assertIn("created_at", result)

        # Cart should still be AVAILABLE (not locked)
        cart = self.cart_repo.find_by_id(1)
        self.assertEqual(cart["status"], "AVAILABLE")

    def test_create_booking_fee_from_config_not_user(self):
        """Booking fee is always from config, never from client input."""
        # Pass a fake booking_fee — it should be ignored
        result = self.service.create_booking(self._valid_data(booking_fee=999.99))
        self.assertEqual(result["booking_fee"], 50.0)   # config value, not 999.99

    def test_create_booking_snapshots_pct_from_config(self):
        """Snapshot fields must match config values at booking creation time."""
        result = self.service.create_booking(self._valid_data())
        self.assertEqual(result["cancellation_fee_pct_snapshot"], 10.0)
        self.assertEqual(result["platform_fee_pct_snapshot"], 5.0)

    def test_create_booking_no_config_fails(self):
        """Booking creation fails when no fee config exists for region + cart type."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(cart_type_id=999))
        self.assertIn("cart type does not exist", str(ctx.exception))

    def test_create_booking_inactive_config_fails(self):
        """Booking creation fails when fee config exists but is inactive."""
        # Deactivate the config
        self.fee_config_repo.delete(1)  # soft-delete sets is_active=False

        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data())
        self.assertIn("No active fee configuration", str(ctx.exception))

    # ── Edge Case: Config deactivated after booking ──────────────────

    def test_config_deactivated_old_booking_still_works(self):
        """Old booking uses snapshot; new booking fails after config deactivation."""
        # Create booking while config is active
        booking = self.service.create_booking(self._valid_data())
        self.assertEqual(booking["booking_fee"], 50.0)
        self.assertEqual(booking["cancellation_fee_pct_snapshot"], 10.0)

        # Deactivate config
        self.fee_config_repo.delete(1)

        # New booking should fail
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data())
        self.assertIn("No active fee configuration", str(ctx.exception))

        # Old booking's snapshot is preserved
        old_booking = self.booking_repo.find_by_id(booking["id"])
        self.assertEqual(old_booking["cancellation_fee_pct_snapshot"], 10.0)
        self.assertEqual(old_booking["platform_fee_pct_snapshot"], 5.0)

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

    def _confirm_booking(self, booking):
        """Helper: run a booking through payment + confirmation."""
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI-OK")
        payment = self.payment_repo.find_by_booking_id(booking["id"])
        self.payment_service.approve_payment(payment["id"])
        return self.service.confirm_booking(booking["id"])

    def test_create_booking_user_limit_exceeded(self):
        """Booking fails when user exceeds 3 CONFIRMED bookings per day."""
        b1 = self.service.create_booking(self._valid_data())
        b2 = self.service.create_booking(self._valid_data())
        b3 = self.service.create_booking(self._valid_data())

        # Confirm all 3 — only confirmed bookings count toward limit
        self._confirm_booking(b1)
        self._confirm_booking(b2)
        self._confirm_booking(b3)

        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data())
        self.assertIn("maximum", str(ctx.exception))

    def test_daily_limit_ignores_cancelled_bookings(self):
        """Cancelled bookings do not count toward the daily limit."""
        b1 = self.service.create_booking(self._valid_data())
        b2 = self.service.create_booking(self._valid_data())
        b3 = self.service.create_booking(self._valid_data())

        self._confirm_booking(b1)
        self._confirm_booking(b2)
        self._confirm_booking(b3)

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

        # Make all carts BUSY
        self.cart_repo.update(1, {"status": "BUSY"})
        self.cart_repo.update(2, {"status": "BUSY"})
        self.cart_repo.update(3, {"status": "BUSY"})

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

        # Create 2 PENDING_PAYMENT bookings (capacity=5, doesn't matter)
        self.service.create_booking(self._valid_data())
        self.service.create_booking(self._valid_data())

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

    def test_cancel_confirmed_booking_refund_uses_snapshot(self):
        """Refund uses snapshot pct values, not current config."""
        confirmed = self._create_and_pay()

        # Change config to extreme values — should NOT affect existing booking
        config = self.fee_config_repo.find_by_region_and_cart_type(1, 1)
        self.fee_config_repo.update(config["id"], {
            "cancellation_fee_pct": 90.0,
            "platform_fee_pct": 10.0,
        })

        self.service.cancel_booking(confirmed["id"])

        # Refund should use original snapshot (10% + 5% = 15% deduction)
        updated_booking = self.booking_repo.find_by_id(confirmed["id"])
        payment = self.payment_repo.find_by_booking_id(confirmed["id"])
        total_paid = float(payment["amount"])
        expected_refund = round(total_paid * (1 - 15.0 / 100), 2)
        self.assertEqual(updated_booking["refund_amount"], expected_refund)

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
