import unittest
from datetime import date
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.tournament.model.tournament_model import Tournament  # noqa: F401
from modules.tournament.model.tournament_team_model import TournamentTeam  # noqa: F401
from modules.tournament.model.tournament_participant_model import TournamentParticipant  # noqa: F401
from modules.tournament.repository.tournament_repository import TournamentRepository
from modules.tournament.repository.tournament_team_repository import TournamentTeamRepository
from modules.tournament.repository.tournament_participant_repository import TournamentParticipantRepository
from modules.tournament.service.tournament_service import TournamentService
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.user.model.user_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestTournamentRegistration(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        t_repo = TournamentRepository(session_factory=factory)
        team_repo = TournamentTeamRepository(session_factory=factory)
        p_repo = TournamentParticipantRepository(session_factory=factory)
        self.service = TournamentService(
            repository=t_repo,
            team_repository=team_repo,
            participant_repository=p_repo,
        )
        self.tournament = self.service.create_tournament({
            "name": "Test League",
            "organizer": "Plixo",
            "start_date": date(2026, 8, 1),
            "end_date": date(2026, 8, 31),
            "max_teams": 4,
            "format_type": "LEAGUE",
            "participant_type": "INDIVIDUAL",
            "team_size": 1,
        })

    def test_register_individual(self):
        result = self.service.register(self.tournament["id"], user_id=1)
        self.assertEqual(result["user_id"], 1)
        self.assertEqual(result["status"], "REGISTERED")

    def test_double_register_raises(self):
        self.service.register(self.tournament["id"], user_id=1)
        with self.assertRaises(ValueError):
            self.service.register(self.tournament["id"], user_id=1)

    def test_capacity_full_raises(self):
        for uid in range(1, 5):
            self.service.register(self.tournament["id"], user_id=uid)
        with self.assertRaises(ValueError):
            self.service.register(self.tournament["id"], user_id=99)

    def test_withdraw(self):
        self.service.register(self.tournament["id"], user_id=1)
        result = self.service.withdraw(self.tournament["id"], user_id=1)
        self.assertEqual(result["status"], "WITHDRAWN")

    def test_withdraw_not_registered_raises(self):
        with self.assertRaises(ValueError):
            self.service.withdraw(self.tournament["id"], user_id=999)
