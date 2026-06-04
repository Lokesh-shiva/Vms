"""Tests for CartRepository.find_by_owner() — owner-level isolation."""
import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.user.model.user_model import User  # noqa: F401 — registers users table
from modules.location.model.location_model import Location  # noqa: F401 — registers locations table
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401 — registers cart_types table
from modules.cart.model.cart_model import Cart  # noqa: F401
# Import all models referenced by FK chains so SQLite can create all tables
import modules.timeslot.model.timeslot_model  # noqa: F401
import modules.booking.model.booking_model  # noqa: F401
from modules.cart.repository.cart_repository import CartRepository


def _make_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestCartOwnerIsolation(unittest.TestCase):

    def setUp(self):
        self.repo = CartRepository(session_factory=_make_factory())

    def _create(self, region_id=1, cart_type_id=1, owner_user_id=None):
        return self.repo.create({
            "region_id": region_id,
            "cart_type_id": cart_type_id,
            "owner_user_id": owner_user_id,
        })

    def test_find_by_owner_returns_only_owned_carts(self):
        """find_by_owner returns only carts whose owner_user_id matches."""
        self._create(owner_user_id=10)
        self._create(owner_user_id=10)
        self._create(owner_user_id=99)

        results = self.repo.find_by_owner(10)
        self.assertEqual(len(results), 2)
        self.assertTrue(all(c["owner_user_id"] == 10 for c in results))

    def test_find_by_owner_returns_empty_when_no_match(self):
        """find_by_owner returns [] when the user owns no carts."""
        self._create(owner_user_id=10)
        results = self.repo.find_by_owner(999)
        self.assertEqual(results, [])

    def test_find_by_owner_excludes_unowned_carts(self):
        """find_by_owner excludes carts with owner_user_id=None."""
        self._create(owner_user_id=None)
        self._create(owner_user_id=10)
        results = self.repo.find_by_owner(10)
        self.assertEqual(len(results), 1)
