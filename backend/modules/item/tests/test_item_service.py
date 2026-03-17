import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401 — registers model
from modules.item.model.item_model import Item  # noqa: F401 — registers model
from modules.item.repository.item_repository import ItemRepository
from modules.item.service.item_service import ItemService
from modules.cart_type.repository.cart_type_repository import CartTypeRepository


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestItemService(unittest.TestCase):
    """Unit tests for ItemService CRUD operations."""

    def setUp(self):
        # Isolated repositories pre-populated with test data (SQLite-backed).
        session_factory = _make_test_session_factory()

        self.cart_type_repo = CartTypeRepository(session_factory=session_factory)
        self.cart_type_repo.create({"name": "Standard"})   # id=1
        self.cart_type_repo.create({"name": "Premium"})     # id=2

        self.service = ItemService(
            item_repository=ItemRepository(session_factory=session_factory),
            cart_type_repository=self.cart_type_repo,
        )

    def _valid_data(self, **overrides) -> dict:
        """Return a baseline valid item payload, with optional overrides."""
        base = {
            "cart_type_id": 1,
            "name": "Chai",
            "price": 20.0,
        }
        base.update(overrides)
        return base

    # ── Create ────────────────────────────────────────────────────────

    def test_create_item_success(self):
        """Creating an item with valid data returns the record with all fields."""
        result = self.service.create_item(self._valid_data())
        self.assertIsNotNone(result)
        self.assertEqual(result["cart_type_id"], 1)
        self.assertEqual(result["name"], "Chai")
        self.assertEqual(result["price"], 20.0)
        self.assertEqual(result["description"], "")
        self.assertEqual(result["image_urls"], [])
        self.assertTrue(result["is_available"])
        self.assertIn("id", result)
        self.assertIn("created_at", result)
        self.assertIn("updated_at", result)

    def test_create_item_invalid_cart_type(self):
        """Creating an item referencing a non-existent cart type raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_item(self._valid_data(cart_type_id=999))
        self.assertIn("cart type does not exist", str(ctx.exception))

    def test_create_item_negative_price(self):
        """Price validation is handled at schema level; service trusts validated data.
        This test verifies the schema rejects negative prices."""
        from modules.item.schemas.item_schema import CreateItemSchema
        schema = CreateItemSchema(self._valid_data(price=-5.0))
        self.assertFalse(schema.is_valid())
        self.assertTrue(any("price" in e for e in schema.errors))

    def test_create_item_invalid_image_urls(self):
        """Schema rejects image_urls containing non-URL strings."""
        from modules.item.schemas.item_schema import CreateItemSchema
        schema = CreateItemSchema(self._valid_data(image_urls=["not-a-url"]))
        self.assertFalse(schema.is_valid())
        self.assertTrue(any("image_urls" in e for e in schema.errors))

    def test_create_item_valid_image_urls(self):
        """Valid image URLs are stored correctly."""
        urls = ["https://example.com/img1.png", "http://example.com/img2.jpg"]
        result = self.service.create_item(self._valid_data(image_urls=urls))
        self.assertEqual(result["image_urls"], urls)

    def test_create_item_duplicate_name_per_cart_type(self):
        """Creating an item with the same name in the same cart type raises ValueError."""
        self.service.create_item(self._valid_data())
        with self.assertRaises(ValueError) as ctx:
            self.service.create_item(self._valid_data())
        self.assertIn("already exists", str(ctx.exception))

    def test_create_item_same_name_different_cart_type(self):
        """Creating an item with the same name but different cart type succeeds."""
        self.service.create_item(self._valid_data())
        result = self.service.create_item(self._valid_data(cart_type_id=2))
        self.assertIsNotNone(result)
        self.assertEqual(result["cart_type_id"], 2)

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_item_not_found(self):
        """Retrieving a non-existent item returns None."""
        result = self.service.get_item(999)
        self.assertIsNone(result)

    def test_get_item_found(self):
        """Retrieving an existing item returns the correct record."""
        created = self.service.create_item(self._valid_data())
        fetched = self.service.get_item(created["id"])
        self.assertEqual(fetched["name"], "Chai")

    def test_list_items(self):
        """Listing items returns all created records."""
        self.service.create_item(self._valid_data())
        self.service.create_item(self._valid_data(name="Coffee", price=30.0))
        items = self.service.list_items()
        self.assertEqual(len(items), 2)

    def test_list_items_by_cart_type(self):
        """Filtering items by cart_type_id returns only matching records."""
        self.service.create_item(self._valid_data())
        self.service.create_item(self._valid_data(name="Chai", cart_type_id=2))
        self.service.create_item(self._valid_data(name="Coffee", cart_type_id=1, price=30.0))

        type1_items = self.service.list_items_by_cart_type(1)
        type2_items = self.service.list_items_by_cart_type(2)
        self.assertEqual(len(type1_items), 2)
        self.assertEqual(len(type2_items), 1)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_item_success(self):
        """Updating an existing item modifies the record and refreshes updated_at."""
        created = self.service.create_item(self._valid_data())
        original_updated_at = created["updated_at"]
        updated = self.service.update_item(created["id"], {"price": 25.0})
        self.assertEqual(updated["price"], 25.0)
        self.assertGreaterEqual(updated["updated_at"], original_updated_at)

    def test_update_item_not_found(self):
        """Updating a non-existent item returns None."""
        result = self.service.update_item(999, {"price": 25.0})
        self.assertIsNone(result)

    def test_update_preserves_created_at(self):
        """Updating an item must not change created_at."""
        created = self.service.create_item(self._valid_data())
        original_created_at = created["created_at"]
        updated = self.service.update_item(created["id"], {"name": "Updated Chai"})
        self.assertEqual(updated["created_at"], original_created_at)

    # ── Delete ────────────────────────────────────────────────────────

    def test_delete_item_success(self):
        """Deleting an existing item returns True."""
        created = self.service.create_item(self._valid_data())
        self.assertTrue(self.service.delete_item(created["id"]))

    def test_delete_item_not_found(self):
        """Deleting a non-existent item returns False."""
        self.assertFalse(self.service.delete_item(999))

    # ── image_url ─────────────────────────────────────────────────────

    def test_create_item_with_image_url(self):
        """Creating an item with image_url stores and returns it correctly."""
        url = "https://example.com/chai.jpg"
        result = self.service.create_item(self._valid_data(image_url=url))
        self.assertEqual(result["image_url"], url)

    def test_update_item_image_url(self):
        """Updating image_url persists the new value."""
        created = self.service.create_item(self._valid_data())
        new_url = "https://example.com/new-image.png"
        updated = self.service.update_item(created["id"], {"image_url": new_url})
        self.assertEqual(updated["image_url"], new_url)

    def test_item_without_image_still_valid(self):
        """Creating an item without image_url succeeds and returns image_url as None."""
        result = self.service.create_item(self._valid_data())
        self.assertIsNone(result["image_url"])

    def test_to_dict_fallback_image(self):
        """to_dict() falls back to first image_urls entry when image_url is None."""
        created = self.service.create_item(
            self._valid_data(image_urls=["https://example.com/fallback.jpg"])
        )
        self.assertEqual(created["image_url"], "https://example.com/fallback.jpg")


if __name__ == "__main__":
    unittest.main()
