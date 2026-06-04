"""Tests for BookingRepository.find_by_owner() — owner-level booking isolation."""
import unittest
from datetime import date
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.booking.repository.booking_repository import BookingRepository
from modules.cart.repository.cart_repository import CartRepository


def _make_factory():
    engine = create_engine("sqlite:///:memory:", connect_args={"check_same_thread": False})
    # Import all models so SQLite creates all tables
    import modules.cart.model.cart_model  # noqa: F401
    import modules.booking.model.booking_model  # noqa: F401
    import modules.user.model.user_model  # noqa: F401
    import modules.location.model.location_model  # noqa: F401
    import modules.cart_type.model.cart_type_model  # noqa: F401
    import modules.timeslot.model.timeslot_model  # noqa: F401
    import modules.booking_item.model.booking_item_model  # noqa: F401
    import modules.payment.model.payment_model  # noqa: F401
    import modules.fee_config.model.fee_config_model  # noqa: F401
    import modules.item.model.item_model  # noqa: F401
    import modules.captain.model.captain_model  # noqa: F401
    import modules.match.model.match_model  # noqa: F401
    import modules.match.model.match_event_model  # noqa: F401
    import modules.matchmaking.model.queue_entry_model  # noqa: F401
    import modules.sport.model.sport_model  # noqa: F401
    import modules.payment.model.system_config_model  # noqa: F401
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestBookingOwnerIsolation(unittest.TestCase):

    def setUp(self):
        factory = _make_factory()
        self.booking_repo = BookingRepository(session_factory=factory)
        self.cart_repo = CartRepository(session_factory=factory)
        self.cart_a = self.cart_repo.create({"region_id": 1, "cart_type_id": 1, "owner_user_id": 10})
        self.cart_b = self.cart_repo.create({"region_id": 1, "cart_type_id": 1, "owner_user_id": 99})

    def _booking(self, assigned_cart_id: int, user_id: int = 1) -> dict:
        """Create a minimal booking with assigned_cart_id pointing to an owned cart."""
        return self.booking_repo.create({
            "user_id": user_id,
            "region_id": 1,
            "cart_type_id": 1,
            "timeslot_id": 1,
            "assigned_cart_id": assigned_cart_id,
            "date": str(date.today()),
            "status": "CONFIRMED",
            "payment_status": "SUCCESS",
            "refund_status": "NONE",
            "booking_fee": 0.0,
            "estimated_total": 0.0,
            "address": "Test Address",
        })

    def test_find_by_owner_returns_only_bookings_for_owned_carts(self):
        self._booking(self.cart_a["id"])
        self._booking(self.cart_a["id"])
        self._booking(self.cart_b["id"])

        results = self.booking_repo.find_by_owner(10)
        self.assertEqual(len(results), 2)
        self.assertTrue(all(b["assigned_cart_id"] == self.cart_a["id"] for b in results))

    def test_find_by_owner_returns_empty_if_no_owned_carts(self):
        self._booking(self.cart_a["id"])
        results = self.booking_repo.find_by_owner(999)
        self.assertEqual(results, [])
