import unittest
from datetime import date
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.tournament.model.tournament_model import Tournament  # noqa: F401
from modules.tournament.repository.tournament_repository import TournamentRepository
from modules.tournament.service.tournament_service import TournamentService
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401


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
        "format_type": "LEAGUE",
        "participant_type": "INDIVIDUAL",
        "team_size": 1,
    }
    base.update(kwargs)
    return base


class TestTournamentExtended(unittest.TestCase):
    def setUp(self):
        repo = TournamentRepository(session_factory=_factory())
        self.service = TournamentService(repository=repo)

    def test_create_with_format_type(self):
        t = self.service.create_tournament(_valid_data())
        self.assertEqual(t["format_type"], "LEAGUE")
        self.assertEqual(t["participant_type"], "INDIVIDUAL")
        self.assertEqual(t["team_size"], 1)

    def test_rules_json_defaults_merged(self):
        t = self.service.create_tournament(_valid_data())
        self.assertEqual(t["rules_json"]["win_points"], 3)
        self.assertEqual(t["rules_json"]["draw_points"], 1)
        self.assertEqual(t["rules_json"]["loss_points"], 0)
        self.assertEqual(t["rules_json"]["global_points_per_win"], 10)

    def test_rules_json_custom_overrides_default(self):
        t = self.service.create_tournament(_valid_data(rules_json={"win_points": 5}))
        self.assertEqual(t["rules_json"]["win_points"], 5)
        self.assertEqual(t["rules_json"]["draw_points"], 1)  # default preserved

    def test_invalid_format_type_raises(self):
        with self.assertRaises(ValueError):
            self.service.create_tournament(_valid_data(format_type="INVALID"))

    def test_invalid_participant_type_raises(self):
        with self.assertRaises(ValueError):
            self.service.create_tournament(_valid_data(participant_type="INVALID"))
