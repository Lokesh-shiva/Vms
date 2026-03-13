import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.location.model.location_model import Location  # noqa: F401 — registers model
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401 — registers model
from modules.cart.model.cart_model import Cart  # noqa: F401 — registers model
from modules.cart.repository.cart_repository import CartRepository
from modules.cart.service.cart_service import CartService
from modules.cart.schemas.cart_schema import CreateCartSchema, UpdateCartSchema
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
        session_factory = _make_test_session_factory()

        self.location_repo = LocationRepository(session_factory=session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})   # id=1

        self.cart_type_repo = CartTypeRepository(session_factory=session_factory)
        self.cart_type_repo.create({"name": "Standard"})  # id=1

        self.cart_repo = CartRepository(session_factory=session_factory)
        self.service = CartService(
            cart_repository=self.cart_repo,
            location_repository=self.location_repo,
            cart_type_repository=self.cart_type_repo,
        )

    def _valid_data(self, **overrides) -> dict:
        """Return a baseline valid cart payload, with optional overrides."""
        base = {
            "region_id": 1,
            "cart_type_id": 1,
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

    def test_create_cart_defaults_to_available(self):
        """Creating a cart without explicit status defaults to AVAILABLE."""
        result = self.service.create_cart(self._valid_data())
        self.assertEqual(result["status"], "AVAILABLE")

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
        self.service.create_cart(self._valid_data())
        carts = self.service.list_carts()
        self.assertEqual(len(carts), 2)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_cart_success(self):
        """Updating an existing cart modifies the record and refreshes updated_at."""
        created = self.service.create_cart(self._valid_data())
        original_updated_at = created["updated_at"]
        updated = self.service.update_cart(created["id"], {"is_active": False})
        self.assertFalse(updated["is_active"])
        self.assertGreaterEqual(updated["updated_at"], original_updated_at)

    def test_update_cart_not_found(self):
        """Updating a non-existent cart returns None."""
        result = self.service.update_cart(999, {"is_active": False})
        self.assertIsNone(result)

    def test_update_preserves_created_at(self):
        """Updating a cart must not change created_at."""
        created = self.service.create_cart(self._valid_data())
        original_created_at = created["created_at"]
        updated = self.service.update_cart(created["id"], {"is_active": False})
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


class TestCartSchemaLockdown(unittest.TestCase):
    """Verify that cart schemas reject admin-supplied status values."""

    def test_create_schema_rejects_status(self):
        """CreateCartSchema must reject payloads containing 'status'."""
        schema = CreateCartSchema({
            "region_id": 1,
            "cart_type_id": 1,
            "status": "AVAILABLE",
        })
        self.assertFalse(schema.is_valid())
        self.assertTrue(
            any("status" in e.lower() for e in schema.errors),
            f"Expected status rejection error, got: {schema.errors}",
        )
        self.assertNotIn("status", schema.validated_data)

    def test_create_schema_valid_without_status(self):
        """CreateCartSchema accepts payloads without 'status'."""
        schema = CreateCartSchema({
            "region_id": 1,
            "cart_type_id": 1,
        })
        self.assertTrue(schema.is_valid())
        self.assertNotIn("status", schema.validated_data)

    def test_update_schema_rejects_status(self):
        """UpdateCartSchema must reject payloads containing 'status'."""
        schema = UpdateCartSchema({
            "status": "BUSY",
        })
        self.assertFalse(schema.is_valid())
        self.assertTrue(
            any("status" in e.lower() for e in schema.errors),
            f"Expected status rejection error, got: {schema.errors}",
        )
        self.assertNotIn("status", schema.validated_data)

    def test_update_schema_valid_with_is_active(self):
        """UpdateCartSchema accepts is_active without status."""
        schema = UpdateCartSchema({"is_active": False})
        self.assertTrue(schema.is_valid())
        self.assertEqual(schema.validated_data["is_active"], False)


class TestClaimAvailableCart(unittest.TestCase):
    """Tests for CartRepository.claim_available_cart atomic claiming."""

    def setUp(self):
        self.session_factory = _make_test_session_factory()
        self.cart_repo = CartRepository(session_factory=self.session_factory)

        self.location_repo = LocationRepository(session_factory=self.session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})

        self.cart_type_repo = CartTypeRepository(session_factory=self.session_factory)
        self.cart_type_repo.create({"name": "Standard"})

    def _create_cart(self, **overrides):
        base = {"region_id": 1, "cart_type_id": 1}
        base.update(overrides)
        return self.cart_repo.create(base)

    def test_claim_returns_cart_and_sets_busy(self):
        """Claiming a cart returns the ORM object and marks it BUSY."""
        self._create_cart()
        session = self.session_factory()
        try:
            cart = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNotNone(cart)
            self.assertEqual(cart.status, "BUSY")
            session.commit()
        finally:
            session.close()

        refreshed = self.cart_repo.find_by_id(1)
        self.assertEqual(refreshed["status"], "BUSY")

    def test_claim_returns_none_when_no_carts(self):
        """Returns None when no AVAILABLE carts exist."""
        session = self.session_factory()
        try:
            cart = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNone(cart)
        finally:
            session.close()

    def test_claim_skips_busy_carts(self):
        """BUSY carts are never claimed."""
        self._create_cart(status="BUSY")
        session = self.session_factory()
        try:
            cart = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNone(cart)
        finally:
            session.close()

    def test_claim_skips_inactive_carts(self):
        """Inactive carts (is_active=False) are never claimed."""
        self._create_cart(is_active=False)
        session = self.session_factory()
        try:
            cart = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNone(cart)
        finally:
            session.close()

    def test_claim_filters_by_region(self):
        """Only carts matching region_id are claimed."""
        self.location_repo.create({"name": "Uptown", "is_serviceable": True})
        self._create_cart(region_id=2)

        session = self.session_factory()
        try:
            cart = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNone(cart)
        finally:
            session.close()

    def test_claim_filters_by_cart_type(self):
        """Only carts matching cart_type_id are claimed."""
        self.cart_type_repo.create({"name": "Premium"})
        self._create_cart(cart_type_id=2)

        session = self.session_factory()
        try:
            cart = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNone(cart)
        finally:
            session.close()

    def test_sequential_claims_consume_different_carts(self):
        """Two sequential claims pick two different carts."""
        self._create_cart()  # id=1
        self._create_cart()  # id=2

        session = self.session_factory()
        try:
            cart1 = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNotNone(cart1)
            cart1_id = cart1.id

            cart2 = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNotNone(cart2)
            cart2_id = cart2.id

            session.commit()
        finally:
            session.close()

        self.assertNotEqual(cart1_id, cart2_id)

    def test_claim_exhaustion(self):
        """After all carts are claimed, the next claim returns None."""
        self._create_cart()

        session = self.session_factory()
        try:
            cart1 = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNotNone(cart1)
            session.flush()
            cart2 = self.cart_repo.claim_available_cart(1, 1, session=session)
            self.assertIsNone(cart2)
            session.commit()
        finally:
            session.close()


class TestCartDeleteConflict(unittest.TestCase):
    """Test that deleting a cart with booking references raises ValueError."""

    def setUp(self):
        self.session_factory = _make_test_session_factory()
        self.cart_repo = CartRepository(session_factory=self.session_factory)

        self.location_repo = LocationRepository(session_factory=self.session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})

        self.cart_type_repo = CartTypeRepository(session_factory=self.session_factory)
        self.cart_type_repo.create({"name": "Standard"})

    def test_delete_unreferenced_cart_succeeds(self):
        """Deleting a cart with no booking references works."""
        cart = self.cart_repo.create({"region_id": 1, "cart_type_id": 1})
        self.assertTrue(self.cart_repo.delete(cart["id"]))
        self.assertIsNone(self.cart_repo.find_by_id(cart["id"]))


if __name__ == "__main__":
    unittest.main()
