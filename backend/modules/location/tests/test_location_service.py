import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.location.model.location_model import Location  # noqa: F401 — registers model
from modules.location.repository.location_repository import LocationRepository
from modules.location.service.location_service import LocationService


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestLocationService(unittest.TestCase):
    """Unit tests for LocationService CRUD operations (SQLite-backed)."""

    def setUp(self):
        session_factory = _make_test_session_factory()
        repo = LocationRepository(session_factory=session_factory)
        self.service = LocationService(location_repository=repo)

    # ── Create ────────────────────────────────────────────────────────

    def test_create_location_success(self):
        """Creating a location with valid data returns the record with an id."""
        result = self.service.create_location({"name": "Downtown"})
        self.assertIsNotNone(result)
        self.assertEqual(result["name"], "Downtown")
        self.assertTrue(result["is_serviceable"])
        self.assertIn("id", result)
        self.assertIn("created_at", result)
        self.assertIn("updated_at", result)

    def test_create_location_missing_name(self):
        """Creating a location without a name raises ValueError."""
        with self.assertRaises(ValueError):
            self.service.create_location({})

    def test_create_location_duplicate_name(self):
        """Creating a location with an already-used name raises ValueError."""
        self.service.create_location({"name": "Airport"})
        with self.assertRaises(ValueError) as ctx:
            self.service.create_location({"name": "Airport"})
        self.assertIn("already exists", str(ctx.exception))

    def test_create_location_not_serviceable(self):
        """Creating a location with is_serviceable=False succeeds."""
        result = self.service.create_location(
            {"name": "Remote Area", "is_serviceable": False}
        )
        self.assertFalse(result["is_serviceable"])

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_location_found(self):
        """Retrieving an existing location returns the correct record."""
        created = self.service.create_location({"name": "Station"})
        fetched = self.service.get_location(created["id"])
        self.assertEqual(fetched["name"], "Station")

    def test_get_location_not_found(self):
        """Retrieving a non-existent location returns None."""
        result = self.service.get_location(999)
        self.assertIsNone(result)

    def test_list_locations(self):
        """Listing locations returns all created records."""
        self.service.create_location({"name": "Location1"})
        self.service.create_location({"name": "Location2"})
        locations = self.service.list_locations()
        self.assertEqual(len(locations), 2)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_location_success(self):
        """Updating an existing location modifies the record and refreshes updated_at."""
        created = self.service.create_location({"name": "Old Name"})
        original_updated_at = created["updated_at"]
        updated = self.service.update_location(created["id"], {"name": "New Name"})
        self.assertEqual(updated["name"], "New Name")
        self.assertGreaterEqual(updated["updated_at"], original_updated_at)

    def test_update_location_not_found(self):
        """Updating a non-existent location returns None."""
        result = self.service.update_location(999, {"name": "Ghost"})
        self.assertIsNone(result)

    def test_update_location_duplicate_name(self):
        """Updating name to one that already exists raises ValueError."""
        self.service.create_location({"name": "Alpha"})
        beta = self.service.create_location({"name": "Beta"})
        with self.assertRaises(ValueError) as ctx:
            self.service.update_location(beta["id"], {"name": "Alpha"})
        self.assertIn("already exists", str(ctx.exception))

    def test_update_preserves_created_at(self):
        """Updating a location must not change created_at."""
        created = self.service.create_location({"name": "Permanent"})
        original_created_at = created["created_at"]
        updated = self.service.update_location(created["id"], {"name": "Renamed"})
        self.assertEqual(updated["created_at"], original_created_at)

    # ── Delete ────────────────────────────────────────────────────────

    def test_delete_location_success(self):
        """Deleting an existing location returns True."""
        created = self.service.create_location({"name": "Temporary"})
        self.assertTrue(self.service.delete_location(created["id"]))

    def test_delete_location_not_found(self):
        """Deleting a non-existent location returns False."""
        self.assertFalse(self.service.delete_location(999))


if __name__ == "__main__":
    unittest.main()
