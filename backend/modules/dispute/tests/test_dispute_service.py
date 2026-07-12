import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.dispute.model.dispute_model import Dispute  # noqa: F401
from modules.dispute.repository.dispute_repository import DisputeRepository
from modules.dispute.service.dispute_service import DisputeService
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


class TestDisputeService(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        repo = DisputeRepository(session_factory=factory)
        user_repo = UserRepository(session_factory=factory)
        self.service = DisputeService(repository=repo, user_repository=user_repo)

    def _base(self, **kwargs):
        base = {"title": "Late booking", "description": "Ground was not available."}
        base.update(kwargs)
        return base

    def test_create_dispute(self):
        d = self.service.create_dispute(self._base())
        self.assertEqual(d["status"], "OPEN")
        self.assertEqual(d["title"], "Late booking")

    def test_list_disputes(self):
        self.service.create_dispute(self._base(title="A"))
        self.service.create_dispute(self._base(title="B"))
        self.assertEqual(len(self.service.list_disputes()), 2)

    def test_update_status(self):
        d = self.service.create_dispute(self._base())
        updated = self.service.update_dispute(d["id"], {"status": "RESOLVED", "resolution_note": "Fixed."})
        self.assertEqual(updated["status"], "RESOLVED")
        self.assertEqual(updated["resolution_note"], "Fixed.")

    def test_create_requires_title(self):
        with self.assertRaises(ValueError):
            self.service.create_dispute({"title": "", "description": "desc"})
