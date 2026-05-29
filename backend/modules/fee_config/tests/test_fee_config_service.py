import unittest

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
from modules.sport.model.sport_model import Sport  # noqa: F401
from modules.match.model.match_model import Match  # noqa: F401
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig  # noqa: F401

from modules.location.repository.location_repository import LocationRepository
from modules.cart_type.repository.cart_type_repository import CartTypeRepository
from modules.fee_config.repository.fee_config_repository import FeeConfigRepository
from modules.fee_config.service.fee_config_service import FeeConfigService


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestFeeConfigService(unittest.TestCase):
    """Unit tests for FeeConfigService — admin-controlled fee configuration."""

    def setUp(self):
        test_session_factory = _make_test_session_factory()

        self.location_repo = LocationRepository(session_factory=test_session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})  # id=1
        self.location_repo.create({"name": "Suburbs", "is_serviceable": True})  # id=2

        self.cart_type_repo = CartTypeRepository(session_factory=test_session_factory)
        self.cart_type_repo.create({"name": "Standard"})  # id=1
        self.cart_type_repo.create({"name": "Premium"})  # id=2

        self.fee_config_repo = FeeConfigRepository(session_factory=test_session_factory)

        self.service = FeeConfigService(
            fee_config_repository=self.fee_config_repo,
            location_repository=self.location_repo,
            cart_type_repository=self.cart_type_repo,
        )

    def _valid_data(self, **overrides) -> dict:
        base = {
            "region_id": 1,
            "cart_type_id": 1,
            "booking_fee": 50.0,
            "cancellation_fee_pct": 10.0,
            "platform_fee_pct": 5.0,
        }
        base.update(overrides)
        return base

    # ── Create ────────────────────────────────────────────────────────

    def test_create_config_success(self):
        """Successfully create a fee config."""
        result = self.service.create_config(self._valid_data())
        self.assertIsNotNone(result)
        self.assertEqual(result["region_id"], 1)
        self.assertEqual(result["cart_type_id"], 1)
        self.assertEqual(result["booking_fee"], 50.0)
        self.assertEqual(result["cancellation_fee_pct"], 10.0)
        self.assertEqual(result["platform_fee_pct"], 5.0)
        self.assertTrue(result["is_active"])

    def test_create_config_invalid_region(self):
        """Creating config with non-existent region fails."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_config(self._valid_data(region_id=999))
        self.assertIn("region does not exist", str(ctx.exception))

    def test_create_config_invalid_cart_type(self):
        """Creating config with non-existent cart type fails."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_config(self._valid_data(cart_type_id=999))
        self.assertIn("cart type does not exist", str(ctx.exception))

    # ── Uniqueness ────────────────────────────────────────────────────

    def test_create_config_duplicate_fails(self):
        """Cannot create two configs for the same region + cart type."""
        self.service.create_config(self._valid_data())
        with self.assertRaises(ValueError) as ctx:
            self.service.create_config(self._valid_data(booking_fee=100.0))
        self.assertIn("already exists", str(ctx.exception))

    def test_create_different_combos_succeed(self):
        """Different region + cart type combos are allowed."""
        self.service.create_config(self._valid_data(region_id=1, cart_type_id=1))
        self.service.create_config(self._valid_data(region_id=1, cart_type_id=2))
        self.service.create_config(self._valid_data(region_id=2, cart_type_id=1))

        configs = self.service.list_configs()
        self.assertEqual(len(configs), 3)

    # ── Percentage Validation ─────────────────────────────────────────

    def test_create_config_pct_sum_exceeds_100(self):
        """Sum of cancellation + platform pct > 100 is blocked."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_config(
                self._valid_data(
                    cancellation_fee_pct=60.0,
                    platform_fee_pct=50.0,
                )
            )
        self.assertIn("cannot exceed 100", str(ctx.exception))

    def test_create_config_negative_cancellation_pct(self):
        """Negative cancellation_fee_pct is blocked."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_config(self._valid_data(cancellation_fee_pct=-5.0))
        self.assertIn("cannot be negative", str(ctx.exception))

    def test_create_config_negative_platform_pct(self):
        """Negative platform_fee_pct is blocked."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_config(self._valid_data(platform_fee_pct=-3.0))
        self.assertIn("cannot be negative", str(ctx.exception))

    def test_create_config_negative_booking_fee(self):
        """Negative booking_fee is blocked."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_config(self._valid_data(booking_fee=-10.0))
        self.assertIn("cannot be negative", str(ctx.exception))

    def test_create_config_pct_sum_exactly_100(self):
        """Sum of cancellation + platform pct == 100 is allowed."""
        result = self.service.create_config(
            self._valid_data(
                cancellation_fee_pct=60.0,
                platform_fee_pct=40.0,
            )
        )
        self.assertEqual(result["cancellation_fee_pct"], 60.0)
        self.assertEqual(result["platform_fee_pct"], 40.0)

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_config_by_id(self):
        """Retrieve a config by ID."""
        created = self.service.create_config(self._valid_data())
        fetched = self.service.get_config(created["id"])
        self.assertIsNotNone(fetched)
        self.assertEqual(fetched["booking_fee"], 50.0)

    def test_get_config_by_region_and_cart_type(self):
        """Retrieve config by region + cart type."""
        self.service.create_config(self._valid_data())
        fetched = self.service.get_config_by_region_and_cart_type(1, 1)
        self.assertIsNotNone(fetched)
        self.assertEqual(fetched["booking_fee"], 50.0)

    def test_get_config_not_found(self):
        """Returns None for non-existent config."""
        result = self.service.get_config(999)
        self.assertIsNone(result)

    def test_list_configs(self):
        """List all configs."""
        self.service.create_config(self._valid_data(region_id=1, cart_type_id=1))
        self.service.create_config(self._valid_data(region_id=2, cart_type_id=1))
        configs = self.service.list_configs()
        self.assertEqual(len(configs), 2)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_config_success(self):
        """Successfully update a config."""
        created = self.service.create_config(self._valid_data())
        updated = self.service.update_config(created["id"], {"booking_fee": 75.0})
        self.assertEqual(updated["booking_fee"], 75.0)

    def test_update_config_pct_sum_exceeds_100_blocked(self):
        """Update that causes pct sum > 100 is blocked."""
        created = self.service.create_config(
            self._valid_data(cancellation_fee_pct=30.0, platform_fee_pct=30.0)
        )
        with self.assertRaises(ValueError) as ctx:
            self.service.update_config(created["id"], {"cancellation_fee_pct": 80.0})
        self.assertIn("cannot exceed 100", str(ctx.exception))

    def test_update_config_not_found(self):
        """Updating non-existent config returns None."""
        result = self.service.update_config(999, {"booking_fee": 10.0})
        self.assertIsNone(result)

    # ── Surge ─────────────────────────────────────────────────────────

    def test_set_surge_updates_multiplier(self):
        """set_surge enables surge and sets the multiplier."""
        created = self.service.create_config(self._valid_data())
        result = self.service.set_surge(created["id"], enabled=True, multiplier=1.5)
        self.assertIsNotNone(result)
        self.assertIs(result["surge_enabled"], True)
        self.assertEqual(result["surge_multiplier"], 1.5)

    def test_set_surge_rejects_out_of_range(self):
        """set_surge rejects a multiplier outside [1.0, 3.0]."""
        created = self.service.create_config(self._valid_data())
        with self.assertRaises(ValueError):
            self.service.set_surge(created["id"], enabled=True, multiplier=5.0)

    # ── Soft-Delete ───────────────────────────────────────────────────

    def test_delete_config_soft_disables(self):
        """Deleting a config sets is_active = False (soft-delete)."""
        created = self.service.create_config(self._valid_data())
        result = self.service.delete_config(created["id"])
        self.assertTrue(result)

        fetched = self.service.get_config(created["id"])
        self.assertFalse(fetched["is_active"])

    def test_delete_config_not_found(self):
        """Deleting non-existent config returns False."""
        result = self.service.delete_config(999)
        self.assertFalse(result)


if __name__ == "__main__":
    unittest.main()
