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


class _FakeUserRepository:
    def __init__(self, names: dict[int, str]):
        self.names = names

    def find_by_id(self, user_id: int) -> dict | None:
        name = self.names.get(user_id)
        return {"id": user_id, "name": name} if name else None


class TestTournamentRegistrationsList(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        self.t_repo = TournamentRepository(session_factory=factory)
        self.team_repo = TournamentTeamRepository(session_factory=factory)
        self.p_repo = TournamentParticipantRepository(session_factory=factory)
        self.user_repo = _FakeUserRepository({1: "Aarav", 2: "Priya", 3: "Rahul"})
        self.service = TournamentService(
            repository=self.t_repo,
            team_repository=self.team_repo,
            participant_repository=self.p_repo,
            user_repository=self.user_repo,
        )

    def test_list_registrations_individual_tournament(self):
        tournament = self.service.create_tournament({
            "name": "Solo Cup", "organizer": "Plixo",
            "start_date": date(2026, 8, 1), "end_date": date(2026, 8, 31),
            "max_teams": 8, "format_type": "LEAGUE", "participant_type": "INDIVIDUAL", "team_size": 1,
        })
        self.service.register(tournament["id"], user_id=1)
        self.service.register(tournament["id"], user_id=2)

        registrations = self.service.list_registrations(tournament["id"])
        self.assertEqual(len(registrations), 2)
        names = {r["name"] for r in registrations}
        self.assertEqual(names, {"Aarav", "Priya"})

    def test_list_registrations_team_tournament_groups_members(self):
        tournament = self.service.create_tournament({
            "name": "Team Cup", "organizer": "Plixo",
            "start_date": date(2026, 8, 1), "end_date": date(2026, 8, 31),
            "max_teams": 4, "format_type": "KNOCKOUT", "participant_type": "TEAM", "team_size": 2,
        })
        self.service.register(tournament["id"], user_id=1, team_data={"team_name": "Falcons", "member_user_ids": [2]})

        registrations = self.service.list_registrations(tournament["id"])
        self.assertEqual(len(registrations), 1)
        self.assertEqual(registrations[0]["team_name"], "Falcons")
        self.assertEqual(registrations[0]["captain_name"], "Aarav")
        member_names = {m["name"] for m in registrations[0]["members"]}
        self.assertEqual(member_names, {"Aarav", "Priya"})

    def test_list_registrations_unknown_tournament_raises(self):
        with self.assertRaises(ValueError):
            self.service.list_registrations(999)
