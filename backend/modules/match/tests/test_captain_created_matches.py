"""
Tests for captain-created match repository methods:
create_captain_match, find_waiting_in_region (visibility filter),
find_society_matches, find_by_invite_code.
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
from modules.society.model.society_member_model import SocietyMember  # noqa: F401

from modules.match.repository.match_repository import MatchRepository


def _make_test_session_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestCaptainCreatedMatches(unittest.TestCase):
    def setUp(self):
        self.session_factory = _make_test_session_factory()
        self.repo = MatchRepository(session_factory=self.session_factory)

        session = self.session_factory()
        session.add(Location(name="Downtown", is_serviceable=True))  # id=1
        session.add(CartType(name="Badminton"))  # id=1
        session.add(Captain(user_id=10, status="ACTIVE"))  # id=1
        session.add(Society(name="Weekend Warriors", owner_user_id=10, region_id=1, sport_id=1))  # id=1
        session.commit()
        session.close()

    def test_create_captain_match_open_no_player_row(self):
        """OPEN captain match: created_by + captain_id set, no MatchPlayer row for captain."""
        match = self.repo.create_captain_match(
            user_id=10,
            captain_id=1,
            region_id=1,
            cart_type_id=1,
            max_players=4,
            visibility="OPEN",
        )
        self.assertEqual(match["status"], "WAITING")
        self.assertEqual(match["created_by"], 10)
        self.assertEqual(match["captain_id"], 1)
        self.assertEqual(match["joined_players"], 0)
        self.assertEqual(match["visibility"], "OPEN")

        session = self.session_factory()
        players = session.query(MatchPlayer).filter(MatchPlayer.match_id == match["id"]).all()
        self.assertEqual(len(players), 0)
        session.close()

    def test_create_captain_match_sets_captain_busy(self):
        """Creating a match immediately marks the organizing captain unavailable."""
        match = self.repo.create_captain_match(
            user_id=10, captain_id=1, region_id=1, cart_type_id=1, max_players=4, visibility="OPEN"
        )
        session = self.session_factory()
        captain = session.query(Captain).filter(Captain.id == 1).first()
        self.assertFalse(captain.is_available)
        self.assertEqual(captain.current_match_id, match["id"])
        session.close()

    def test_create_captain_match_society_sets_society_id(self):
        match = self.repo.create_captain_match(
            user_id=10,
            captain_id=1,
            region_id=1,
            cart_type_id=1,
            max_players=4,
            visibility="SOCIETY",
            society_id=1,
        )
        self.assertEqual(match["society_id"], 1)
        self.assertEqual(match["visibility"], "SOCIETY")

    def test_create_captain_match_private_generates_invite_code(self):
        match = self.repo.create_captain_match(
            user_id=10,
            captain_id=1,
            region_id=1,
            cart_type_id=1,
            max_players=4,
            visibility="PRIVATE",
        )
        self.assertEqual(match["visibility"], "PRIVATE")
        self.assertIsNotNone(match["invite_code"])
        self.assertEqual(len(match["invite_code"]), 6)

    def test_find_waiting_in_region_only_returns_open(self):
        """SOCIETY and PRIVATE matches must not appear in the public open-matches feed."""
        self.repo.create_captain_match(
            user_id=10, captain_id=1, region_id=1, cart_type_id=1, max_players=4, visibility="OPEN"
        )
        self.repo.create_captain_match(
            user_id=10, captain_id=1, region_id=1, cart_type_id=1, max_players=4,
            visibility="SOCIETY", society_id=1,
        )
        self.repo.create_captain_match(
            user_id=10, captain_id=1, region_id=1, cart_type_id=1, max_players=4, visibility="PRIVATE"
        )
        results = self.repo.find_waiting_in_region(region_id=1)
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["visibility"], "OPEN")

    def test_find_society_matches_returns_only_that_society(self):
        match = self.repo.create_captain_match(
            user_id=10, captain_id=1, region_id=1, cart_type_id=1, max_players=4,
            visibility="SOCIETY", society_id=1,
        )
        results = self.repo.find_society_matches(society_id=1)
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["id"], match["id"])

    def test_find_by_invite_code_returns_match(self):
        match = self.repo.create_captain_match(
            user_id=10, captain_id=1, region_id=1, cart_type_id=1, max_players=4, visibility="PRIVATE"
        )
        found = self.repo.find_by_invite_code(match["invite_code"])
        self.assertIsNotNone(found)
        self.assertEqual(found["id"], match["id"])

    def test_find_by_invite_code_unknown_returns_none(self):
        self.assertIsNone(self.repo.find_by_invite_code("ZZZZZZ"))


if __name__ == "__main__":
    unittest.main()
