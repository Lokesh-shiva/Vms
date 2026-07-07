"""
Tests for Match Lifecycle: arrive_match and finish_match.

Verifies the full lifecycle flow:
  MATCHED -> ARRIVED (first player) -> IN_PROGRESS (all players) -> COMPLETED
"""

import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.location.model.location_model import Location  # noqa: F401
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.match.model.match_model import Match, MatchPlayer  # noqa: F401
from modules.user.model.user_model import User  # noqa: F401
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401
from modules.sport.model.sport_model import Sport  # noqa: F401
from modules.booking.model.booking_model import Booking  # noqa: F401
from modules.booking_item.model.booking_item_model import BookingItem  # noqa: F401
from modules.item.model.item_model import Item  # noqa: F401
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig  # noqa: F401
from modules.payment.model.payment_model import Payment  # noqa: F401
from modules.payment.model.system_config_model import SystemConfig  # noqa: F401
from modules.matchmaking.model.queue_entry_model import QueueEntry  # noqa: F401
from modules.captain.model.captain_model import Captain  # noqa: F401

from modules.match.repository.match_repository import MatchRepository
from modules.match.service.match_service import MatchService
from modules.cart.repository.cart_repository import CartRepository
from modules.timeslot.repository.timeslot_repository import TimeslotRepository
from modules.cart_type.repository.cart_type_repository import CartTypeRepository
from modules.location.repository.location_repository import LocationRepository


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestMatchLifecycle(unittest.TestCase):
    """Tests for arrive_match and finish_match lifecycle transitions."""

    def setUp(self):
        self.session_factory = _make_test_session_factory()

        self.location_repo = LocationRepository(session_factory=self.session_factory)
        self.cart_type_repo = CartTypeRepository(session_factory=self.session_factory)
        self.cart_repo = CartRepository(session_factory=self.session_factory)
        self.timeslot_repo = TimeslotRepository(session_factory=self.session_factory)
        self.match_repo = MatchRepository(session_factory=self.session_factory)

        self.service = MatchService(
            match_repository=self.match_repo,
            cart_repository=self.cart_repo,
            timeslot_repository=self.timeslot_repo,
            cart_type_repository=self.cart_type_repo,
            location_repository=self.location_repo,
        )

        # Seed required data
        self.location_repo.create({"name": "Downtown", "is_serviceable": True})  # id=1
        self.cart_type_repo.create({"name": "Badminton"})  # id=1

        # Create a cart with GPS coordinates
        session = self.session_factory()
        cart = Cart(
            label="Court A",
            region_id=1,
            cart_type_id=1,
            status="BUSY",
            is_active=True,
            latitude=12.9716,
            longitude=77.5946,
        )
        session.add(cart)
        session.commit()
        self.cart_id = cart.id
        session.close()

        # Create a match in MATCHED status with 2 players
        session = self.session_factory()
        match = Match(
            created_by=1,
            sport_id=None,
            region_id=1,
            cart_type_id=1,
            cart_id=self.cart_id,
            skill_level="BEGINNER",
            max_players=2,
            joined_players=2,
            status="MATCHED",
        )
        session.add(match)
        session.flush()
        self.match_id = match.id

        p1 = MatchPlayer(match_id=match.id, user_id=1, has_arrived=False)
        p2 = MatchPlayer(match_id=match.id, user_id=2, has_arrived=False)
        session.add_all([p1, p2])
        session.commit()
        session.close()

    # ── Arrive Tests ─────────────────────────────────────────────────

    def test_first_arrival_sets_status_to_arrived(self):
        """First player arriving sets match status to ARRIVED."""
        result = self.service.arrive_match(1, self.match_id, 12.9716, 77.5946)
        self.assertEqual(result["status"], "ARRIVED")

    def test_all_arrivals_set_status_to_in_progress(self):
        """When all players arrive, match transitions to IN_PROGRESS."""
        self.service.arrive_match(1, self.match_id, 12.9716, 77.5946)
        result = self.service.arrive_match(2, self.match_id, 12.9716, 77.5946)
        self.assertEqual(result["status"], "IN_PROGRESS")

    def test_arrive_invalid_match(self):
        """Arriving at a non-existent match raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.arrive_match(1, 9999, 12.97, 77.59)
        self.assertIn("Match not found", str(ctx.exception))

    def test_arrive_non_player(self):
        """A user who is not in the match cannot arrive."""
        with self.assertRaises(ValueError) as ctx:
            self.service.arrive_match(99, self.match_id, 12.97, 77.59)
        self.assertIn("not a player", str(ctx.exception))

    def test_arrive_duplicate_raises(self):
        """A player cannot mark arrival twice."""
        self.service.arrive_match(1, self.match_id, 12.9716, 77.5946)
        with self.assertRaises(ValueError) as ctx:
            self.service.arrive_match(1, self.match_id, 12.9716, 77.5946)
        self.assertIn("already marked", str(ctx.exception))

    def test_arrive_too_far_raises(self):
        """GPS check rejects player who is too far from the ground."""
        with self.assertRaises(ValueError) as ctx:
            self.service.arrive_match(1, self.match_id, 0.0, 0.0)
        self.assertIn("too far", str(ctx.exception))

    def test_arrive_wrong_status_raises(self):
        """Cannot arrive at a match that is already COMPLETED."""
        # Force match to COMPLETED
        session = self.session_factory()
        m = session.query(Match).filter(Match.id == self.match_id).first()
        m.status = "COMPLETED"
        session.commit()
        session.close()

        with self.assertRaises(ValueError) as ctx:
            self.service.arrive_match(1, self.match_id, 12.97, 77.59)
        self.assertIn("Cannot arrive", str(ctx.exception))

    # ── Finish Tests ─────────────────────────────────────────────────

    def test_finish_match_sets_completed(self):
        """Finishing a match sets status to COMPLETED."""
        # First get to IN_PROGRESS
        self.service.arrive_match(1, self.match_id, 12.9716, 77.5946)
        self.service.arrive_match(2, self.match_id, 12.9716, 77.5946)

        result = self.service.finish_match(1, self.match_id)
        self.assertEqual(result["status"], "COMPLETED")

    def test_finish_frees_cart(self):
        """Finishing a match sets the assigned cart back to AVAILABLE."""
        self.service.arrive_match(1, self.match_id, 12.9716, 77.5946)
        self.service.arrive_match(2, self.match_id, 12.9716, 77.5946)
        self.service.finish_match(1, self.match_id)

        # Check cart status
        session = self.session_factory()
        cart = session.query(Cart).filter(Cart.id == self.cart_id).first()
        self.assertEqual(cart.status, "AVAILABLE")
        session.close()

    def test_finish_non_player_raises(self):
        """A non-player cannot finish the match."""
        with self.assertRaises(ValueError) as ctx:
            self.service.finish_match(99, self.match_id)
        self.assertIn("not a player", str(ctx.exception))

    def test_finish_already_completed_raises(self):
        """Cannot finish a match that is already COMPLETED."""
        session = self.session_factory()
        m = session.query(Match).filter(Match.id == self.match_id).first()
        m.status = "COMPLETED"
        session.commit()
        session.close()

        with self.assertRaises(ValueError) as ctx:
            self.service.finish_match(1, self.match_id)
        self.assertIn("already COMPLETED", str(ctx.exception))

    # ── Cart Coordinates ─────────────────────────────────────────────

    def test_arrive_without_cart_coordinates_bypasses_gps(self):
        """If cart has no coordinates, GPS check is bypassed gracefully."""
        # Create a cart without coordinates
        session = self.session_factory()
        cart = Cart(
            label="Court B",
            region_id=1,
            cart_type_id=1,
            status="BUSY",
            is_active=True,
            latitude=None,
            longitude=None,
        )
        session.add(cart)
        session.flush()

        # Create a match pointing to this cart
        match = Match(
            created_by=1,
            region_id=1,
            cart_type_id=1,
            cart_id=cart.id,
            max_players=2,
            joined_players=2,
            status="MATCHED",
        )
        session.add(match)
        session.flush()
        mid = match.id

        p1 = MatchPlayer(match_id=mid, user_id=1, has_arrived=False)
        p2 = MatchPlayer(match_id=mid, user_id=2, has_arrived=False)
        session.add_all([p1, p2])
        session.commit()
        session.close()

        # Should succeed even with coordinates far away
        result = self.service.arrive_match(1, mid, 0.0, 0.0)
        self.assertEqual(result["status"], "ARRIVED")


if __name__ == "__main__":
    unittest.main()
