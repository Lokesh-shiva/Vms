"""
Tests for MatchRepository.find_joinable_playnow — the query that lets
POST /matchmaking/play-now actually pair players together instead of
always starting a new solo session.
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
from modules.society.model.society_model import Society  # noqa: F401

from modules.match.repository.match_repository import MatchRepository


def _make_test_session_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestFindJoinablePlaynow(unittest.TestCase):
    def setUp(self):
        self.factory = _make_test_session_factory()
        self.repo = MatchRepository(session_factory=self.factory)

    def _seed_match(self, **overrides) -> int:
        session = self.factory()
        defaults = dict(
            created_by=1,
            region_id=1,
            cart_type_id=1,
            max_players=2,
            joined_players=1,
            status="WAITING",
            visibility="OPEN",
        )
        defaults.update(overrides)
        match = Match(**defaults)
        session.add(match)
        session.commit()
        session.refresh(match)
        session.add(MatchPlayer(match_id=match.id, user_id=defaults["created_by"]))
        session.commit()
        match_id = match.id
        session.close()
        return match_id

    def test_finds_compatible_waiting_match(self):
        match_id = self._seed_match()
        found = self.repo.find_joinable_playnow(
            region_id=1, cart_type_id=1, max_players=2, exclude_user_id=2,
        )
        self.assertIsNotNone(found)
        self.assertEqual(found["id"], match_id)

    def test_no_match_in_different_region(self):
        self._seed_match(region_id=1)
        found = self.repo.find_joinable_playnow(
            region_id=99, cart_type_id=1, max_players=2, exclude_user_id=2,
        )
        self.assertIsNone(found)

    def test_no_match_for_different_sport(self):
        self._seed_match(cart_type_id=1)
        found = self.repo.find_joinable_playnow(
            region_id=1, cart_type_id=99, max_players=2, exclude_user_id=2,
        )
        self.assertIsNone(found)

    def test_no_match_when_max_players_differ(self):
        self._seed_match(max_players=2)
        found = self.repo.find_joinable_playnow(
            region_id=1, cart_type_id=1, max_players=6, exclude_user_id=2,
        )
        self.assertIsNone(found)

    def test_no_match_when_already_full(self):
        self._seed_match(max_players=2, joined_players=2)
        found = self.repo.find_joinable_playnow(
            region_id=1, cart_type_id=1, max_players=2, exclude_user_id=2,
        )
        self.assertIsNone(found)

    def test_no_match_when_private_visibility(self):
        self._seed_match(visibility="PRIVATE")
        found = self.repo.find_joinable_playnow(
            region_id=1, cart_type_id=1, max_players=2, exclude_user_id=2,
        )
        self.assertIsNone(found)

    def test_excludes_match_the_user_already_joined(self):
        match_id = self._seed_match(created_by=1)
        # user 1 is already the creator/first player of this match
        found = self.repo.find_joinable_playnow(
            region_id=1, cart_type_id=1, max_players=2, exclude_user_id=1,
        )
        self.assertIsNone(found)

    def test_returns_oldest_first(self):
        from datetime import datetime, timedelta

        older_id = self._seed_match(created_by=1, created_at=datetime(2026, 1, 1))
        self._seed_match(created_by=3, created_at=datetime(2026, 1, 1) + timedelta(hours=1))
        found = self.repo.find_joinable_playnow(
            region_id=1, cart_type_id=1, max_players=2, exclude_user_id=2,
        )
        self.assertEqual(found["id"], older_id)


if __name__ == "__main__":
    unittest.main()
