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
    }
    base.update(kwargs)
    return base


class TestTournamentService(unittest.TestCase):
    def setUp(self):
        repo = TournamentRepository(session_factory=_factory())
        self.service = TournamentService(repository=repo)

    def test_create_tournament(self):
        t = self.service.create_tournament(_valid_data())
        self.assertEqual(t["name"], "Test Cup")
        self.assertEqual(t["status"], "UPCOMING")

    def test_list_tournaments(self):
        self.service.create_tournament(_valid_data(name="A"))
        self.service.create_tournament(_valid_data(name="B"))
        self.assertEqual(len(self.service.list_tournaments()), 2)

    def test_update_status(self):
        t = self.service.create_tournament(_valid_data())
        updated = self.service.update_tournament(t["id"], {"status": "ONGOING"})
        self.assertEqual(updated["status"], "ONGOING")

    def test_delete_tournament(self):
        t = self.service.create_tournament(_valid_data())
        self.assertTrue(self.service.delete_tournament(t["id"]))
        self.assertIsNone(self.service.get_tournament(t["id"]))

    def test_create_validates_dates(self):
        with self.assertRaises(ValueError):
            self.service.create_tournament(_valid_data(
                start_date=date(2026, 7, 10),
                end_date=date(2026, 7, 1),
            ))
