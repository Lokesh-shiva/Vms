import unittest
from datetime import date
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.tournament.model.tournament_model import Tournament  # noqa: F401
from modules.tournament.model.tournament_team_model import TournamentTeam  # noqa: F401
from modules.tournament.model.tournament_participant_model import TournamentParticipant  # noqa: F401
from modules.tournament.repository.tournament_repository import TournamentRepository
from modules.tournament.service.tournament_service import TournamentService
from modules.tournament.schemas.tournament_schema import UpdateTournamentSchema
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.user.model.user_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


def _valid_data(**kwargs):
    base = {
        "name": "Test Cup",
        "organizer": "Plixo",
        "start_date": date(2026, 7, 1),
        "end_date": date(2026, 7, 10),
        "max_teams": 8,
    }
    base.update(kwargs)
    return base


class _FakeUserRepository:
    def __init__(self, users: dict[int, dict]):
        self.users = users

    def find_by_id(self, user_id: int) -> dict | None:
        return self.users.get(user_id)


class TestTournamentSponsorSchema(unittest.TestCase):
    def test_accepts_positive_int(self):
        schema = UpdateTournamentSchema({"sponsor_user_id": 5})
        self.assertTrue(schema.is_valid())
        self.assertEqual(schema.validated_data["sponsor_user_id"], 5)

    def test_accepts_null_to_unassign(self):
        schema = UpdateTournamentSchema({"sponsor_user_id": None})
        self.assertTrue(schema.is_valid())
        self.assertIsNone(schema.validated_data["sponsor_user_id"])

    def test_rejects_non_positive(self):
        schema = UpdateTournamentSchema({"sponsor_user_id": 0})
        self.assertFalse(schema.is_valid())

    def test_rejects_non_int(self):
        schema = UpdateTournamentSchema({"sponsor_user_id": "5"})
        self.assertFalse(schema.is_valid())


class TestTournamentSponsorService(unittest.TestCase):
    def setUp(self):
        self.repo = TournamentRepository(session_factory=_factory())
        self.users = {
            1: {"id": 1, "role": "csr_partner", "name": "Acme CSR"},
            2: {"id": 2, "role": "super_admin", "name": "Admin"},
        }
        self.service = TournamentService(
            repository=self.repo,
            user_repository=_FakeUserRepository(self.users),
        )

    def test_assign_sponsor_to_csr_partner_succeeds(self):
        t = self.service.create_tournament(_valid_data())
        updated = self.service.update_tournament(t["id"], {"sponsor_user_id": 1})
        self.assertEqual(updated["sponsor_user_id"], 1)

    def test_assign_sponsor_to_non_csr_user_rejected(self):
        t = self.service.create_tournament(_valid_data())
        with self.assertRaises(ValueError):
            self.service.update_tournament(t["id"], {"sponsor_user_id": 2})

    def test_assign_sponsor_unknown_user_rejected(self):
        t = self.service.create_tournament(_valid_data())
        with self.assertRaises(ValueError):
            self.service.update_tournament(t["id"], {"sponsor_user_id": 999})

    def test_unassign_sponsor_with_null(self):
        t = self.service.create_tournament(_valid_data())
        self.service.update_tournament(t["id"], {"sponsor_user_id": 1})
        updated = self.service.update_tournament(t["id"], {"sponsor_user_id": None})
        self.assertIsNone(updated["sponsor_user_id"])

    def test_list_sponsored_returns_only_own_tournaments(self):
        t1 = self.service.create_tournament(_valid_data(name="Sponsored by 1"))
        t2 = self.service.create_tournament(_valid_data(name="Unsponsored"))
        self.service.update_tournament(t1["id"], {"sponsor_user_id": 1})

        sponsored = self.service.list_sponsored(1)
        self.assertEqual(len(sponsored), 1)
        self.assertEqual(sponsored[0]["id"], t1["id"])
