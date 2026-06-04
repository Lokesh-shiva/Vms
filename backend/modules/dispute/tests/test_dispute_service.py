import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.dispute.model.dispute_model import Dispute  # noqa: F401
from modules.dispute.repository.dispute_repository import DisputeRepository
from modules.dispute.service.dispute_service import DisputeService
import modules.user.model.user_model  # noqa: F401
import modules.booking.model.booking_model  # noqa: F401
import modules.cart.model.cart_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.cart_type.model.cart_type_model  # noqa: F401
import modules.timeslot.model.timeslot_model  # noqa: F401
import modules.match.model.match_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401
import modules.tournament.model.tournament_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestDisputeService(unittest.TestCase):
    def setUp(self):
        repo = DisputeRepository(session_factory=_factory())
        self.service = DisputeService(repository=repo)

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
