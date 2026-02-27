import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.location.model.location_model import Location  # noqa: F401 — registers model
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401 — registers model
from modules.cart.model.cart_model import Cart  # noqa: F401 — registers model
from modules.cart.repository.cart_repository import CartRepository
from modules.cart.service.cart_service import CartService
from modules.location.repository.location_repository import LocationRepository
from modules.cart_type.repository.cart_type_repository import CartTypeRepository


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestCartService(unittest.TestCase):
    """Unit tests for CartService CRUD operations."""

    def setUp(self):
        # Isolated repositories pre-populated with test data (SQLite-backed).
        session_factory = _make_test_session_factory()

        self.location_repo = LocationRepository(session_factory=session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})   # id=1

        self.cart_type_repo = CartTypeRepository(session_factory=session_factory)
        self.cart_type_repo.create({"name": "Standard"})  # id=1

        self.service = CartService(
            cart_repository=CartRepository(session_factory=session_factory),
            location_repository=self.location_repo,
            cart_type_repository=self.cart_type_repo,
        )

    def _valid_data(self, **overrides) -> dict:
        """Return a baseline valid cart payload, with optional overrides."""
        base = {
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        }
        base.update(overrides)
        return base

    # ── Create ────────────────────────────────────────────────────────

    def test_create_cart_success(self):
        """Creating a cart with valid data returns the record with all fields."""
        result = self.service.create_cart(self._valid_data())
        self.assertIsNotNone(result)
        self.assertEqual(result["region_id"], 1)
        self.assertEqual(result["cart_type_id"], 1)
        self.assertEqual(result["status"], "AVAILABLE")
        self.assertTrue(result["is_active"])
        self.assertIn("id", result)
        self.assertIn("created_at", result)
        self.assertIn("updated_at", result)

    def test_create_cart_invalid_region(self):
        """Creating a cart referencing a non-existent region raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_cart(self._valid_data(region_id=999))
        self.assertIn("region does not exist", str(ctx.exception))

    def test_create_cart_invalid_cart_type(self):
        """Creating a cart referencing a non-existent cart type raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_cart(self._valid_data(cart_type_id=999))
        self.assertIn("cart type does not exist", str(ctx.exception))

    def test_create_cart_with_status(self):
        """Creating a cart with a specific valid status stores it correctly."""
        result = self.service.create_cart(self._valid_data(status="BUSY"))
        self.assertEqual(result["status"], "BUSY")

    def test_create_cart_inactive(self):
        """Creating a cart with is_active=False succeeds."""
        result = self.service.create_cart(self._valid_data(is_active=False))
        self.assertFalse(result["is_active"])

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_cart_not_found(self):
        """Retrieving a non-existent cart returns None."""
        result = self.service.get_cart(999)
        self.assertIsNone(result)

    def test_get_cart_found(self):
        """Retrieving an existing cart returns the correct record."""
        created = self.service.create_cart(self._valid_data())
        fetched = self.service.get_cart(created["id"])
        self.assertEqual(fetched["region_id"], 1)

    def test_list_carts(self):
        """Listing carts returns all created records."""
        self.service.create_cart(self._valid_data())
        self.service.create_cart(self._valid_data(status="BUSY"))
        carts = self.service.list_carts()
        self.assertEqual(len(carts), 2)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_cart_success(self):
        """Updating an existing cart modifies the record and refreshes updated_at."""
        created = self.service.create_cart(self._valid_data())
        original_updated_at = created["updated_at"]
        updated = self.service.update_cart(created["id"], {"status": "OFFLINE"})
        self.assertEqual(updated["status"], "OFFLINE")
        self.assertGreaterEqual(updated["updated_at"], original_updated_at)

    def test_update_cart_not_found(self):
        """Updating a non-existent cart returns None."""
        result = self.service.update_cart(999, {"status": "BUSY"})
        self.assertIsNone(result)

    def test_update_preserves_created_at(self):
        """Updating a cart must not change created_at."""
        created = self.service.create_cart(self._valid_data())
        original_created_at = created["created_at"]
        updated = self.service.update_cart(created["id"], {"status": "BUFFER"})
        self.assertEqual(updated["created_at"], original_created_at)

    def test_update_cart_invalid_region(self):
        """Updating region_id to a non-existent region raises ValueError."""
        created = self.service.create_cart(self._valid_data())
        with self.assertRaises(ValueError) as ctx:
            self.service.update_cart(created["id"], {"region_id": 999})
        self.assertIn("region does not exist", str(ctx.exception))

    def test_update_cart_invalid_cart_type(self):
        """Updating cart_type_id to a non-existent cart type raises ValueError."""
        created = self.service.create_cart(self._valid_data())
        with self.assertRaises(ValueError) as ctx:
            self.service.update_cart(created["id"], {"cart_type_id": 999})
        self.assertIn("cart type does not exist", str(ctx.exception))

    # ── Delete ────────────────────────────────────────────────────────

    def test_delete_cart_success(self):
        """Deleting an existing cart returns True."""
        created = self.service.create_cart(self._valid_data())
        self.assertTrue(self.service.delete_cart(created["id"]))

    def test_delete_cart_not_found(self):
        """Deleting a non-existent cart returns False."""
        self.assertFalse(self.service.delete_cart(999))


if __name__ == "__main__":
    unittest.main()
