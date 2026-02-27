import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.location.model.location_model import Location  # noqa: F401 — registers model
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401 — registers model
from modules.location.repository.location_repository import LocationRepository
from modules.timeslot.repository.timeslot_repository import TimeslotRepository
from modules.timeslot.service.timeslot_service import TimeslotService


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestTimeslotService(unittest.TestCase):
    """Unit tests for TimeslotService CRUD operations."""

    def setUp(self):
        # Shared session factory for all repos (SQLite-backed, no Neon).
        session_factory = _make_test_session_factory()
        self.location_repo = LocationRepository(session_factory=session_factory)
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})   # id=1
        self.location_repo.create({"name": "Closed Area", "is_serviceable": False})  # id=2

        self.service = TimeslotService(
            timeslot_repository=TimeslotRepository(session_factory=session_factory),
            location_repository=self.location_repo,
        )

    def _valid_data(self, **overrides) -> dict:
        """Return a baseline valid timeslot payload, with optional overrides."""
        base = {
            "location_id": 1,
            "date": "2026-03-01",
            "start_time": "09:00",
            "end_time": "10:00",
            "capacity": 5,
        }
        base.update(overrides)
        return base

    # ── Create ────────────────────────────────────────────────────────

    def test_create_timeslot_success(self):
        """Creating a timeslot with valid data returns the record with all fields."""
        result = self.service.create_timeslot(self._valid_data())
        self.assertIsNotNone(result)
        self.assertEqual(result["location_id"], 1)
        self.assertEqual(result["date"], "2026-03-01")
        self.assertEqual(result["start_time"], "09:00")
        self.assertEqual(result["end_time"], "10:00")
        self.assertEqual(result["capacity"], 5)
        self.assertIn("id", result)
        self.assertIn("created_at", result)
        self.assertIn("updated_at", result)

    def test_create_timeslot_invalid_location(self):
        """Creating a timeslot referencing a non-existent location raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_timeslot(self._valid_data(location_id=999))
        self.assertIn("does not exist", str(ctx.exception))

    def test_create_timeslot_non_serviceable_location(self):
        """Creating a timeslot for a non-serviceable location raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_timeslot(self._valid_data(location_id=2))
        self.assertIn("not serviceable", str(ctx.exception))

    def test_create_timeslot_invalid_time_range(self):
        """Creating a timeslot where end_time <= start_time raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_timeslot(
                self._valid_data(start_time="10:00", end_time="09:00")
            )
        self.assertIn("end_time", str(ctx.exception))

    def test_create_timeslot_equal_times(self):
        """Creating a timeslot where end_time == start_time raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create_timeslot(
                self._valid_data(start_time="10:00", end_time="10:00")
            )
        self.assertIn("end_time", str(ctx.exception))

    def test_create_timeslot_duplicate(self):
        """Creating a duplicate timeslot (same location, date, start_time) raises ValueError."""
        self.service.create_timeslot(self._valid_data())
        with self.assertRaises(ValueError) as ctx:
            self.service.create_timeslot(self._valid_data())
        self.assertIn("already exists", str(ctx.exception))

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_timeslot_not_found(self):
        """Retrieving a non-existent timeslot returns None."""
        result = self.service.get_timeslot(999)
        self.assertIsNone(result)

    def test_get_timeslot_found(self):
        """Retrieving an existing timeslot returns the correct record."""
        created = self.service.create_timeslot(self._valid_data())
        fetched = self.service.get_timeslot(created["id"])
        self.assertEqual(fetched["date"], "2026-03-01")

    def test_list_timeslots(self):
        """Listing timeslots returns all created records."""
        self.service.create_timeslot(self._valid_data())
        self.service.create_timeslot(
            self._valid_data(start_time="11:00", end_time="12:00")
        )
        timeslots = self.service.list_timeslots()
        self.assertEqual(len(timeslots), 2)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_timeslot_success(self):
        """Updating an existing timeslot modifies the record and refreshes updated_at."""
        created = self.service.create_timeslot(self._valid_data())
        original_updated_at = created["updated_at"]
        updated = self.service.update_timeslot(created["id"], {"capacity": 10})
        self.assertEqual(updated["capacity"], 10)
        self.assertGreaterEqual(updated["updated_at"], original_updated_at)

    def test_update_timeslot_not_found(self):
        """Updating a non-existent timeslot returns None."""
        result = self.service.update_timeslot(999, {"capacity": 10})
        self.assertIsNone(result)

    def test_update_preserves_created_at(self):
        """Updating a timeslot must not change created_at."""
        created = self.service.create_timeslot(self._valid_data())
        original_created_at = created["created_at"]
        updated = self.service.update_timeslot(created["id"], {"capacity": 20})
        self.assertEqual(updated["created_at"], original_created_at)

    # ── Delete ────────────────────────────────────────────────────────

    def test_delete_timeslot_success(self):
        """Deleting an existing timeslot returns True."""
        created = self.service.create_timeslot(self._valid_data())
        self.assertTrue(self.service.delete_timeslot(created["id"]))

    def test_delete_timeslot_not_found(self):
        """Deleting a non-existent timeslot returns False."""
        self.assertFalse(self.service.delete_timeslot(999))


if __name__ == "__main__":
    unittest.main()
