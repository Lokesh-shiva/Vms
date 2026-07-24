import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.chat.model.message_model import Message  # noqa: F401
from modules.location.model.location_model import Location  # noqa: F401
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.match.model.match_model import Match, MatchPlayer  # noqa: F401
from modules.user.model.user_model import User  # noqa: F401
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401
from modules.sport.model.sport_model import Sport  # noqa: F401
from modules.captain.model.captain_model import Captain  # noqa: F401

from modules.chat.repository.message_repository import MessageRepository
from modules.chat.service.chat_service import ChatService
from modules.user.repository.user_repository import UserRepository


def _make_test_session_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestChatServiceAdminAccess(unittest.TestCase):
    def setUp(self):
        self.session_factory = _make_test_session_factory()
        self.message_repo = MessageRepository(session_factory=self.session_factory)
        self.user_repo = UserRepository(session_factory=self.session_factory)
        self.service = ChatService(
            message_repository=self.message_repo,
            session_factory=self.session_factory,
            user_repository=self.user_repo,
        )

        session = self.session_factory()
        session.add(Location(name="Downtown", is_serviceable=True))  # id=1
        session.add(CartType(name="Badminton"))  # id=1
        session.add(User(id=1, name="Alice", phone="+1000000001", password_hash=""))
        session.add(User(id=2, name="Bob", phone="+1000000002", password_hash=""))
        session.add(Match(id=1, created_by=1, region_id=1, cart_type_id=1, sport_id=1, max_players=2, joined_players=2, status="MATCHED"))
        session.add(MatchPlayer(match_id=1, user_id=1))
        session.add(MatchPlayer(match_id=1, user_id=2))
        session.commit()
        session.close()

    def test_get_messages_admin_bypasses_participant_check(self):
        self.message_repo.create(match_id=1, sender_id=1, body="hi")
        # user_id=999 is not a participant — get_messages() would reject them
        with self.assertRaises(PermissionError):
            self.service.get_messages(1, 999)
        # but the admin path returns the messages regardless
        messages = self.service.get_messages_admin(1)
        self.assertEqual(len(messages), 1)

    def test_get_messages_admin_enriches_sender_name(self):
        self.message_repo.create(match_id=1, sender_id=1, body="hello")
        self.message_repo.create(match_id=1, sender_id=2, body="hey")
        messages = self.service.get_messages_admin(1)
        names = [m["sender_name"] for m in messages]
        self.assertEqual(names, ["Alice", "Bob"])

    def test_get_messages_admin_empty_match_returns_empty_list(self):
        self.assertEqual(self.service.get_messages_admin(1), [])

    def test_get_messages_admin_unknown_sender_reports_unknown(self):
        session = self.session_factory()
        session.add(Message(match_id=1, sender_id=None, body="orphaned"))
        session.commit()
        session.close()
        messages = self.service.get_messages_admin(1)
        self.assertEqual(messages[0]["sender_name"], "Unknown")


if __name__ == "__main__":
    unittest.main()
