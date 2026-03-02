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


class TestBookingItemService(unittest.TestCase):
    """Unit tests for BookingItem integration within BookingService."""

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
        self.cart_type_repo.create({"name": "Premium"})   # id=2

        # Timeslot repo — capacity=5 (SQLite-backed)
        self.timeslot_repo = TimeslotRepository(session_factory=test_session_factory)
        self.timeslot_repo.create({
            "location_id": 1,
            "date": "2026-03-01",
            "start_time": "09:00",
            "end_time": "10:00",
            "capacity": 5,
        })  # id=1

        # Cart repo — one available cart (SQLite-backed)
        self.cart_repo = CartRepository(session_factory=test_session_factory)
        self.cart_repo.create({
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        })  # id=1

        # Item repo — items belonging to cart_type 1 (SQLite-backed)
        self.item_repo = ItemRepository(session_factory=test_session_factory)
        self.item_repo.create({
            "cart_type_id": 1,
            "name": "Water Bottle",
            "description": "500ml",
            "price": 25.50,
            "is_available": True,
        })  # id=1

        self.item_repo.create({
            "cart_type_id": 1,
            "name": "Snack Pack",
            "description": "Mixed nuts",
            "price": 40.00,
            "is_available": True,
        })  # id=2

        # Item belonging to cart_type 2 (wrong cart type for Standard bookings)
        self.item_repo.create({
            "cart_type_id": 2,
            "name": "Premium Meal",
            "description": "Full meal",
            "price": 150.00,
            "is_available": True,
        })  # id=3

        # Unavailable item in cart_type 1
        self.item_repo.create({
            "cart_type_id": 1,
            "name": "Discontinued Item",
            "description": "No longer sold",
            "price": 10.00,
            "is_available": False,
        })  # id=4

        # BookingItem service with isolated repos
        self.booking_item_repo = BookingItemRepository(session_factory=test_session_factory)
        self.booking_item_service = BookingItemService(
            booking_item_repository=self.booking_item_repo,
            item_repository=self.item_repo,
        )

        # Payment repos for BookingService dependency
        booking_repo = BookingRepository(session_factory=test_session_factory)
        payment_repo = PaymentRepository(session_factory=test_session_factory)
        payment_service = PaymentService(
            payment_repository=payment_repo,
            booking_repository=booking_repo,
        )

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

        # Booking service with all isolated repos
        self.service = BookingService(
            booking_repository=booking_repo,
            user_repository=self.user_repo,
            location_repository=self.location_repo,
            cart_type_repository=self.cart_type_repo,
            timeslot_repository=self.timeslot_repo,
            cart_repository=self.cart_repo,
            booking_item_service=self.booking_item_service,
            payment_repository=payment_repo,
            payment_service=payment_service,
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

    # ── Valid Booking With Items ──────────────────────────────────────

    def test_create_booking_with_items_success(self):
        """Booking with valid items creates BookingItem records."""
        items = [
            {"item_id": 1, "quantity": 2},
            {"item_id": 2, "quantity": 1},
        ]
        result = self.service.create_booking(self._valid_data(items=items))

        self.assertEqual(result["status"], "PENDING_PAYMENT")
        self.assertIn("items", result)
        self.assertEqual(len(result["items"]), 2)

        # Verify item records
        item1 = result["items"][0]
        self.assertEqual(item1["booking_id"], result["id"])
        self.assertEqual(item1["item_id"], 1)
        self.assertEqual(item1["quantity"], 2)
        self.assertEqual(item1["unit_price"], 25.50)

        item2 = result["items"][1]
        self.assertEqual(item2["item_id"], 2)
        self.assertEqual(item2["quantity"], 1)
        self.assertEqual(item2["unit_price"], 40.00)

    # ── Booking Without Items ────────────────────────────────────────

    def test_create_booking_without_items(self):
        """Booking without items still works; estimated_total = 0.0."""
        result = self.service.create_booking(self._valid_data())

        self.assertEqual(result["status"], "PENDING_PAYMENT")
        self.assertEqual(result["estimated_total"], 0.0)
        self.assertNotIn("items", result)

    # ── Invalid Item ID ──────────────────────────────────────────────

    def test_create_booking_invalid_item_id(self):
        """Booking fails when referencing a non-existent item."""
        items = [{"item_id": 999, "quantity": 1}]
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(items=items))
        self.assertIn("does not exist", str(ctx.exception))

    # ── Wrong Cart Type Item ─────────────────────────────────────────

    def test_create_booking_wrong_cart_type_item(self):
        """Booking fails when item belongs to a different cart type."""
        items = [{"item_id": 3, "quantity": 1}]  # Premium item in Standard booking
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(items=items))
        self.assertIn("does not belong to cart type", str(ctx.exception))

    # ── Invalid Quantity ─────────────────────────────────────────────

    def test_create_booking_invalid_quantity_zero(self):
        """Booking fails when item quantity is zero."""
        items = [{"item_id": 1, "quantity": 0}]
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(items=items))
        self.assertIn("positive integer", str(ctx.exception))

    def test_create_booking_invalid_quantity_negative(self):
        """Booking fails when item quantity is negative."""
        items = [{"item_id": 1, "quantity": -3}]
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(items=items))
        self.assertIn("positive integer", str(ctx.exception))

    # ── Correct Estimated Total Calculation ──────────────────────────

    def test_estimated_total_single_item(self):
        """estimated_total = quantity * unit_price for a single item."""
        items = [{"item_id": 1, "quantity": 3}]  # 3 * 25.50 = 76.50
        result = self.service.create_booking(self._valid_data(items=items))
        self.assertEqual(result["estimated_total"], 76.50)

    def test_estimated_total_multiple_items(self):
        """estimated_total = sum(qty * price) across all items."""
        items = [
            {"item_id": 1, "quantity": 2},  # 2 * 25.50 = 51.00
            {"item_id": 2, "quantity": 3},  # 3 * 40.00 = 120.00
        ]                                   # Total: 171.00
        result = self.service.create_booking(self._valid_data(items=items))
        self.assertEqual(result["estimated_total"], 171.00)

    def test_client_estimated_total_ignored(self):
        """Server ignores any client-provided estimated_total."""
        items = [{"item_id": 1, "quantity": 1}]  # 1 * 25.50 = 25.50
        data = self._valid_data(items=items, estimated_total=9999.99)
        result = self.service.create_booking(data)
        # Server-computed value, not client's 9999.99
        self.assertEqual(result["estimated_total"], 25.50)

    # ── Unavailable Item ─────────────────────────────────────────────

    def test_create_booking_unavailable_item(self):
        """Booking fails when an item is marked as not available."""
        items = [{"item_id": 4, "quantity": 1}]
        with self.assertRaises(ValueError) as ctx:
            self.service.create_booking(self._valid_data(items=items))
        self.assertIn("not currently available", str(ctx.exception))

    # ── Items Validated Before Side Effects ───────────────────────────

    def test_invalid_items_do_not_assign_cart(self):
        """Cart should not be assigned if item validation fails."""
        items = [{"item_id": 999, "quantity": 1}]
        with self.assertRaises(ValueError):
            self.service.create_booking(self._valid_data(items=items))

        # Cart should still be AVAILABLE — no side effects from failure
        cart = self.cart_repo.find_by_id(1)
        self.assertEqual(cart["status"], "AVAILABLE")


if __name__ == "__main__":
    unittest.main()
