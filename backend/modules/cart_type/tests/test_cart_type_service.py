import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401 — registers model
from modules.cart_type.repository.cart_type_repository import CartTypeRepository
from modules.cart_type.service.cart_type_service import CartTypeService


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestCartTypeService(unittest.TestCase):
    """Unit tests for CartTypeService CRUD operations (SQLite-backed)."""

    def setUp(self):
        session_factory = _make_test_session_factory()
        repo = CartTypeRepository(session_factory=session_factory)
        self.service = CartTypeService(cart_type_repository=repo)

    # ── Create ────────────────────────────────────────────────────────

    def test_create_cart_type_success(self):
        """Creating a cart type with valid data returns the record with an id."""
        result = self.service.create_cart_type({"name": "Standard"})
        self.assertIsNotNone(result)
        self.assertEqual(result["name"], "Standard")
        self.assertEqual(result["description"], "")
        self.assertTrue(result["is_active"])
        self.assertIn("id", result)
        self.assertIn("created_at", result)
        self.assertIn("updated_at", result)

    def test_create_cart_type_with_description(self):
        """Creating a cart type with a description stores it correctly."""
        result = self.service.create_cart_type(
            {"name": "Premium", "description": "Premium cart with extras"}
        )
        self.assertEqual(result["description"], "Premium cart with extras")

    def test_create_cart_type_duplicate_name(self):
        """Creating a cart type with an already-used name raises ValueError."""
        self.service.create_cart_type({"name": "Standard"})
        with self.assertRaises(ValueError) as ctx:
            self.service.create_cart_type({"name": "Standard"})
        self.assertIn("already exists", str(ctx.exception))

    def test_create_cart_type_inactive(self):
        """Creating a cart type with is_active=False succeeds."""
        result = self.service.create_cart_type({"name": "Archived", "is_active": False})
        self.assertFalse(result["is_active"])

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_cart_type_found(self):
        """Retrieving an existing cart type returns the correct record."""
        created = self.service.create_cart_type({"name": "Standard"})
        fetched = self.service.get_cart_type(created["id"])
        self.assertEqual(fetched["name"], "Standard")

    def test_get_cart_type_not_found(self):
        """Retrieving a non-existent cart type returns None."""
        result = self.service.get_cart_type(999)
        self.assertIsNone(result)

    def test_list_cart_types(self):
        """Listing cart types returns all created records."""
        self.service.create_cart_type({"name": "Type1"})
        self.service.create_cart_type({"name": "Type2"})
        cart_types = self.service.list_cart_types()
        self.assertEqual(len(cart_types), 2)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_cart_type_success(self):
        """Updating an existing cart type modifies the record and refreshes updated_at."""
        created = self.service.create_cart_type({"name": "Old Name"})
        original_updated_at = created["updated_at"]
        updated = self.service.update_cart_type(created["id"], {"name": "New Name"})
        self.assertEqual(updated["name"], "New Name")
        self.assertGreaterEqual(updated["updated_at"], original_updated_at)

    def test_update_cart_type_not_found(self):
        """Updating a non-existent cart type returns None."""
        result = self.service.update_cart_type(999, {"name": "Ghost"})
        self.assertIsNone(result)

    def test_update_cart_type_duplicate_name(self):
        """Updating name to one that already exists raises ValueError."""
        self.service.create_cart_type({"name": "Alpha"})
        beta = self.service.create_cart_type({"name": "Beta"})
        with self.assertRaises(ValueError) as ctx:
            self.service.update_cart_type(beta["id"], {"name": "Alpha"})
        self.assertIn("already exists", str(ctx.exception))

    def test_update_preserves_created_at(self):
        """Updating a cart type must not change created_at."""
        created = self.service.create_cart_type({"name": "Permanent"})
        original_created_at = created["created_at"]
        updated = self.service.update_cart_type(created["id"], {"name": "Renamed"})
        self.assertEqual(updated["created_at"], original_created_at)

    # ── Delete ────────────────────────────────────────────────────────

    def test_delete_cart_type_success(self):
        """Deleting an existing cart type returns True."""
        created = self.service.create_cart_type({"name": "Temporary"})
        self.assertTrue(self.service.delete_cart_type(created["id"]))

    def test_delete_cart_type_not_found(self):
        """Deleting a non-existent cart type returns False."""
        self.assertFalse(self.service.delete_cart_type(999))


if __name__ == "__main__":
    unittest.main()
