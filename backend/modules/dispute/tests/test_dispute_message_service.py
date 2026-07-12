import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.dispute.model.dispute_model import Dispute  # noqa: F401
from modules.dispute.model.dispute_message_model import DisputeMessage  # noqa: F401
from modules.dispute.repository.dispute_repository import DisputeRepository
from modules.dispute.repository.dispute_message_repository import DisputeMessageRepository
from modules.dispute.service.dispute_message_service import DisputeMessageService
from modules.user.repository.user_repository import UserRepository
import modules.user.model.user_model  # noqa: F401
import modules.booking.model.booking_model  # noqa: F401
import modules.cart.model.cart_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.cart_type.model.cart_type_model  # noqa: F401
import modules.timeslot.model.timeslot_model  # noqa: F401
import modules.match.model.match_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401
import modules.tournament.model.tournament_model  # noqa: F401
import modules.society.model.society_model  # noqa: F401
import modules.captain.model.captain_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestDisputeMessageService(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        self.dispute_repo = DisputeRepository(session_factory=factory)
        self.message_repo = DisputeMessageRepository(session_factory=factory)
        self.user_repo = UserRepository(session_factory=factory)
        self.service = DisputeMessageService(
            message_repository=self.message_repo,
            dispute_repository=self.dispute_repo,
            user_repository=self.user_repo,
        )
        self.dispute = self.dispute_repo.create({
            "title": "Late booking", "description": "Ground unavailable.", "raised_by": 1,
        })

    def test_raiser_can_send_and_list_messages(self):
        self.service.send_message(self.dispute["id"], sender_id=1, role="user", body="Any update?")
        messages = self.service.list_messages(self.dispute["id"], user_id=1, role="user")
        self.assertEqual(len(messages), 1)
        self.assertEqual(messages[0]["body"], "Any update?")

    def test_support_staff_can_reply(self):
        self.service.send_message(self.dispute["id"], sender_id=99, role="support", body="Looking into it.")
        messages = self.service.list_messages(self.dispute["id"], user_id=99, role="support")
        self.assertEqual(len(messages), 1)

    def test_other_regular_user_cannot_access(self):
        with self.assertRaises(PermissionError):
            self.service.list_messages(self.dispute["id"], user_id=2, role="user")
        with self.assertRaises(PermissionError):
            self.service.send_message(self.dispute["id"], sender_id=2, role="user", body="hi")

    def test_unknown_dispute_raises(self):
        with self.assertRaises(ValueError):
            self.service.list_messages(999, user_id=1, role="user")

    def test_blank_body_rejected(self):
        with self.assertRaises(ValueError):
            self.service.send_message(self.dispute["id"], sender_id=1, role="user", body="   ")

    def test_messages_ordered_oldest_first(self):
        self.service.send_message(self.dispute["id"], sender_id=1, role="user", body="first")
        self.service.send_message(self.dispute["id"], sender_id=99, role="support", body="second")
        messages = self.service.list_messages(self.dispute["id"], user_id=1, role="user")
        self.assertEqual([m["body"] for m in messages], ["first", "second"])
