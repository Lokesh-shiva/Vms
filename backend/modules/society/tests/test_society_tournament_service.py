import unittest
from datetime import date

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base

# Models required for table creation
from modules.society.model.society_model import Society  # noqa: F401
from modules.society.model.society_member_model import SocietyMember, SocietyRole  # noqa: F401
from modules.tournament.model.tournament_model import Tournament  # noqa: F401
from modules.tournament.model.tournament_team_model import TournamentTeam  # noqa: F401
from modules.tournament.model.tournament_participant_model import TournamentParticipant  # noqa: F401
import modules.user.model.user_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401

# Repositories
from modules.society.repository.society_repository import SocietyRepository
from modules.society.repository.society_member_repository import SocietyMemberRepository
from modules.tournament.repository.tournament_repository import TournamentRepository
from modules.tournament.repository.tournament_team_repository import TournamentTeamRepository
from modules.tournament.repository.tournament_participant_repository import TournamentParticipantRepository

# Services
from modules.tournament.service.tournament_service import TournamentService
from modules.society.service.society_tournament_service import SocietyTournamentService


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestSocietyTournamentService(unittest.TestCase):

    def setUp(self) -> None:
        factory = _factory()

        # Society repos
        self.s_repo = SocietyRepository(session_factory=factory)
        self.m_repo = SocietyMemberRepository(session_factory=factory)

        # Tournament repos
        t_repo = TournamentRepository(session_factory=factory)
        team_repo = TournamentTeamRepository(session_factory=factory)
        p_repo = TournamentParticipantRepository(session_factory=factory)

        # Tournament service
        t_service = TournamentService(
            repository=t_repo,
            team_repository=team_repo,
            participant_repository=p_repo,
        )

        # Service under test
        self.service = SocietyTournamentService(
            society_repository=self.s_repo,
            member_repository=self.m_repo,
            tournament_service=t_service,
        )

        # 1. Create a TEAM-type tournament
        tournament = t_service.create_tournament({
            "name": "Society Cup",
            "organizer": "Plixo",
            "start_date": date(2026, 9, 1),
            "end_date": date(2026, 9, 30),
            "participant_type": "TEAM",
            "team_size": 5,
            "max_teams": 4,
            "format_type": "LEAGUE",
        })
        self.tournament_id: int = tournament["id"]

        # 2. Create a society with owner user_id=1
        society = self.s_repo.create({
            "name": "Alpha Society",
            "region_id": 1,
            "sport_id": 1,
            "owner_user_id": 1,
        })
        self.society_id: int = society["id"]

        # Add owner as OWNER member
        self.m_repo.add_member(self.society_id, 1, SocietyRole.OWNER)

        # 3. Add user_id=2 as MEMBER
        self.m_repo.add_member(self.society_id, 2, SocietyRole.MEMBER)

    # ------------------------------------------------------------------
    # test 1: happy path
    # ------------------------------------------------------------------

    def test_register_as_team_success(self) -> None:
        """Owner registers with valid member → returns TournamentTeam dict."""
        result = self.service.register_as_team(
            society_id=self.society_id,
            tournament_id=self.tournament_id,
            member_ids=[2],
            requester_id=1,
        )
        self.assertIn("id", result)
        self.assertEqual(result["name"], "Alpha Society")

    # ------------------------------------------------------------------
    # test 2: non-owner raises
    # ------------------------------------------------------------------

    def test_register_non_owner_raises(self) -> None:
        """MEMBER (user_id=2) trying to register raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.register_as_team(
                society_id=self.society_id,
                tournament_id=self.tournament_id,
                member_ids=[2],
                requester_id=2,
            )
        self.assertIn(
            "Only the society owner can register for tournaments.",
            str(ctx.exception),
        )

    # ------------------------------------------------------------------
    # test 3: non-member in member_ids raises
    # ------------------------------------------------------------------

    def test_register_includes_non_member_raises(self) -> None:
        """Passing user_id=99 who is not in the society raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.register_as_team(
                society_id=self.society_id,
                tournament_id=self.tournament_id,
                member_ids=[99],
                requester_id=1,
            )
        self.assertIn("not a member", str(ctx.exception))

    # ------------------------------------------------------------------
    # test 4: inactive society raises
    # ------------------------------------------------------------------

    def test_register_inactive_society_raises(self) -> None:
        """Deactivated society raises ValueError('Society is not active.')."""
        self.s_repo.update(self.society_id, {"is_active": False})
        with self.assertRaises(ValueError) as ctx:
            self.service.register_as_team(
                society_id=self.society_id,
                tournament_id=self.tournament_id,
                member_ids=[2],
                requester_id=1,
            )
        self.assertEqual(str(ctx.exception), "Society is not active.")


if __name__ == "__main__":
    unittest.main()
