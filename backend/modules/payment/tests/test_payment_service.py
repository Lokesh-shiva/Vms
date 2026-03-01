import unittest
import re

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.user.model.user_model import User  # noqa: F401
from modules.location.model.location_model import Location  # noqa: F401
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.item.model.item_model import Item  # noqa: F401
from modules.booking.model.booking_model import Booking  # noqa: F401
from modules.booking_item.model.booking_item_model import BookingItem  # noqa: F401
from modules.payment.model.payment_model import Payment  # noqa: F401

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


class TestPaymentService(unittest.TestCase):
    """Unit tests for PaymentService — manual admin approval workflow."""

    def setUp(self):
        test_session_factory = _make_test_session_factory()

        self.user_repo = UserRepository(session_factory=test_session_factory)
        self.user_repo.create({"name": "Alice", "phone": "9000000001"})

        self.location_repo = LocationRepository(session_factory=test_session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})

        self.cart_type_repo = CartTypeRepository(session_factory=test_session_factory)
        self.cart_type_repo.create({"name": "Standard"})

        self.timeslot_repo = TimeslotRepository(session_factory=test_session_factory)
        self.timeslot_repo.create({
            "location_id": 1, "date": "2026-03-01",
            "start_time": "09:00", "end_time": "10:00", "capacity": 5,
        })

        self.cart_repo = CartRepository(session_factory=test_session_factory)
        self.cart_repo.create({
            "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
        })

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

        self.booking_service = BookingService(
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

    def _create_booking(self, **overrides) -> dict:
        base = {
            "user_id": 1, "region_id": 1, "cart_type_id": 1,
            "timeslot_id": 1, "address": "123 Main St", "booking_fee": 50.0,
        }
        base.update(overrides)
        return self.booking_service.create_booking(base)

    # ── Initiate Payment ─────────────────────────────────────────────

    def test_initiate_payment_creates_pending(self):
        """Initiating payment creates a PENDING payment (no INITIATED state)."""
        booking = self._create_booking()
        result = self.payment_service.initiate_payment(booking["id"])

        self.assertEqual(result["booking_id"], booking["id"])
        self.assertIn("reference_code", result)
        self.assertIn("upi_id", result)
        self.assertEqual(result["payment"]["status"], "PENDING")

    def test_reference_code_format(self):
        """Reference code follows VMS-{booking_id}-{4 digits} format."""
        booking = self._create_booking()
        result = self.payment_service.initiate_payment(booking["id"])

        pattern = rf"^VMS-{booking['id']}-\d{{4}}$"
        self.assertRegex(result["reference_code"], pattern)

    def test_initiate_payment_for_non_pending_booking_fails(self):
        """Cannot initiate payment for a non-PENDING_PAYMENT booking."""
        booking = self._create_booking()
        self.booking_repo.update(booking["id"], {"status": "CANCELLED"})

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.initiate_payment(booking["id"])
        self.assertIn("PENDING_PAYMENT", str(ctx.exception))

    def test_initiate_payment_duplicate_fails(self):
        """Cannot initiate payment twice for the same booking."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.initiate_payment(booking["id"])
        self.assertIn("already exists", str(ctx.exception))

    def test_initiate_payment_booking_not_found(self):
        """Initiating payment for non-existent booking fails."""
        with self.assertRaises(ValueError) as ctx:
            self.payment_service.initiate_payment(999)
        self.assertIn("not found", str(ctx.exception))

    # ── Submit Manual Confirmation ───────────────────────────────────

    def test_submit_manual_confirmation_success(self):
        """Submitting transaction ID moves payment to UNDER_REVIEW."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])

        result = self.payment_service.submit_manual_confirmation(
            booking["id"], "UPI-TXN-12345"
        )
        self.assertEqual(result["status"], "UNDER_REVIEW")
        self.assertEqual(result["transaction_id"], "UPI-TXN-12345")

    def test_submit_confirmation_without_pending_payment_fails(self):
        """Cannot submit confirmation when payment is not PENDING."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.submit_manual_confirmation(booking["id"], "UPI456")
        self.assertIn("not in PENDING", str(ctx.exception))

    def test_submit_confirmation_empty_transaction_id_fails(self):
        """Transaction ID must be non-empty."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.submit_manual_confirmation(booking["id"], "  ")
        self.assertIn("required", str(ctx.exception))

    def test_submit_confirmation_no_payment_fails(self):
        """Cannot submit confirmation when no payment exists."""
        booking = self._create_booking()

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        self.assertIn("No payment found", str(ctx.exception))

    # ── Approve Payment ──────────────────────────────────────────────

    def test_approve_payment_success(self):
        """Admin approval moves payment to SUCCESS and mirrors to booking."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        payment = self.payment_repo.find_by_booking_id(booking["id"])

        result = self.payment_service.approve_payment(payment["id"])
        self.assertEqual(result["status"], "SUCCESS")

        # Verify booking.payment_status is mirrored
        updated_booking = self.booking_repo.find_by_id(booking["id"])
        self.assertEqual(updated_booking["payment_status"], "SUCCESS")

    def test_approve_payment_not_under_review_fails(self):
        """Cannot approve a payment that is not UNDER_REVIEW."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        payment = self.payment_repo.find_by_booking_id(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.approve_payment(payment["id"])
        self.assertIn("under review", str(ctx.exception))

    def test_cannot_approve_payment_twice(self):
        """Cannot approve an already-approved payment."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        payment = self.payment_repo.find_by_booking_id(booking["id"])
        self.payment_service.approve_payment(payment["id"])

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.approve_payment(payment["id"])
        self.assertIn("under review", str(ctx.exception))

    # ── Reject Payment ───────────────────────────────────────────────

    def test_reject_payment_success(self):
        """Admin rejection moves payment to FAILED and mirrors to booking."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        payment = self.payment_repo.find_by_booking_id(booking["id"])

        result = self.payment_service.reject_payment(payment["id"])
        self.assertEqual(result["status"], "FAILED")

        updated_booking = self.booking_repo.find_by_id(booking["id"])
        self.assertEqual(updated_booking["payment_status"], "FAILED")

    def test_reject_payment_not_under_review_fails(self):
        """Cannot reject a payment that is not UNDER_REVIEW."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        payment = self.payment_repo.find_by_booking_id(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.reject_payment(payment["id"])
        self.assertIn("under review", str(ctx.exception))

    # ── Refund ───────────────────────────────────────────────────────

    def test_refund_payment_success(self):
        """Refund moves payment to REFUNDED and updates booking refund fields."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        self.payment_service.submit_manual_confirmation(booking["id"], "UPI123")
        payment = self.payment_repo.find_by_booking_id(booking["id"])
        self.payment_service.approve_payment(payment["id"])

        result = self.payment_service.process_refund(payment["id"])
        self.assertEqual(result["status"], "REFUNDED")

        updated_booking = self.booking_repo.find_by_id(booking["id"])
        self.assertEqual(updated_booking["refund_status"], "REFUNDED")

    def test_refund_non_success_payment_fails(self):
        """Cannot refund a payment that is not in SUCCESS status."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])
        payment = self.payment_repo.find_by_booking_id(booking["id"])

        with self.assertRaises(ValueError) as ctx:
            self.payment_service.process_refund(payment["id"])
        self.assertIn("successful", str(ctx.exception))

    # ── Payment Not Found ────────────────────────────────────────────

    def test_approve_nonexistent_payment_fails(self):
        """Approving a non-existent payment fails."""
        with self.assertRaises(ValueError) as ctx:
            self.payment_service.approve_payment(999)
        self.assertIn("not found", str(ctx.exception))

    def test_reject_nonexistent_payment_fails(self):
        """Rejecting a non-existent payment fails."""
        with self.assertRaises(ValueError) as ctx:
            self.payment_service.reject_payment(999)
        self.assertIn("not found", str(ctx.exception))

    def test_refund_nonexistent_payment_fails(self):
        """Refunding a non-existent payment fails."""
        with self.assertRaises(ValueError) as ctx:
            self.payment_service.process_refund(999)
        self.assertIn("not found", str(ctx.exception))

    # ── Get Payment by Booking ID ────────────────────────────────────

    def test_get_payment_by_booking_id(self):
        """Admin can retrieve payment details by booking ID."""
        booking = self._create_booking()
        self.payment_service.initiate_payment(booking["id"])

        result = self.payment_service.get_payment_by_booking_id(booking["id"])
        self.assertIsNotNone(result)
        self.assertEqual(result["booking_id"], booking["id"])

    def test_get_payment_by_booking_id_not_found(self):
        """Returns None when no payment exists for a booking."""
        result = self.payment_service.get_payment_by_booking_id(999)
        self.assertIsNone(result)

    # ── Full Happy Path ──────────────────────────────────────────────

    def test_full_happy_path(self):
        """End-to-end: create -> initiate -> confirm-manual -> approve -> confirm-booking."""
        # 1. Create booking
        booking = self._create_booking()
        self.assertEqual(booking["status"], "PENDING_PAYMENT")
        self.assertEqual(booking["payment_status"], "PENDING")

        # 2. Initiate payment
        pay_result = self.payment_service.initiate_payment(booking["id"])
        self.assertIn("reference_code", pay_result)

        # 3. User submits UPI transaction ID
        self.payment_service.submit_manual_confirmation(
            booking["id"], "UPI-TXN-HAPPY"
        )
        payment = self.payment_repo.find_by_booking_id(booking["id"])
        self.assertEqual(payment["status"], "UNDER_REVIEW")

        # 4. Admin approves
        approved = self.payment_service.approve_payment(payment["id"])
        self.assertEqual(approved["status"], "SUCCESS")

        # 5. Admin confirms booking
        confirmed = self.booking_service.confirm_booking(booking["id"])
        self.assertEqual(confirmed["status"], "CONFIRMED")
        self.assertIsNotNone(confirmed["assigned_cart_id"])

        # 6. Verify final states
        final_booking = self.booking_repo.find_by_id(booking["id"])
        self.assertEqual(final_booking["status"], "CONFIRMED")
        self.assertEqual(final_booking["payment_status"], "SUCCESS")

        cart = self.cart_repo.find_by_id(confirmed["assigned_cart_id"])
        self.assertEqual(cart["status"], "BUSY")


if __name__ == "__main__":
    unittest.main()
