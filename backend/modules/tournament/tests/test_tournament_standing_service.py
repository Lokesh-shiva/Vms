import unittest
from datetime import date
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.tournament.model.tournament_model import Tournament  # noqa: F401
from modules.tournament.model.tournament_team_model import TournamentTeam  # noqa: F401
from modules.tournament.model.tournament_participant_model import TournamentParticipant  # noqa: F401
from modules.tournament.model.tournament_match_model import TournamentMatch  # noqa: F401
from modules.tournament.model.tournament_standing_model import TournamentStanding  # noqa: F401
from modules.tournament.model.player_score_model import PlayerScore  # noqa: F401
from modules.tournament.repository.tournament_repository import TournamentRepository
from modules.tournament.repository.tournament_team_repository import TournamentTeamRepository
from modules.tournament.repository.tournament_participant_repository import TournamentParticipantRepository
from modules.tournament.repository.tournament_match_repository import TournamentMatchRepository
from modules.tournament.repository.tournament_standing_repository import TournamentStandingRepository
from modules.tournament.repository.player_score_repository import PlayerScoreRepository
from modules.tournament.service.tournament_service import TournamentService
from modules.tournament.service.tournament_match_service import TournamentMatchService
from modules.tournament.service.tournament_standing_service import TournamentStandingService
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.user.model.user_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestTournamentStandingService(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        t_repo = TournamentRepository(session_factory=factory)
        team_repo = TournamentTeamRepository(session_factory=factory)
        p_repo = TournamentParticipantRepository(session_factory=factory)
        m_repo = TournamentMatchRepository(session_factory=factory)
        s_repo = TournamentStandingRepository(session_factory=factory)
        ps_repo = PlayerScoreRepository(session_factory=factory)
        t_service = TournamentService(
            repository=t_repo, team_repository=team_repo, participant_repository=p_repo
        )
        self.match_service = TournamentMatchService(
            tournament_repository=t_repo,
            participant_repository=p_repo,
            match_repository=m_repo,
            standing_repository=s_repo,
            player_score_repository=ps_repo,
        )
        self.standing_service = TournamentStandingService(
            standing_repository=s_repo,
            player_score_repository=ps_repo,
        )
        self.tournament = t_service.create_tournament({
            "name": "League",
            "organizer": "Plixo",
            "start_date": date(2026, 8, 1),
            "end_date": date(2026, 8, 31),
            "format_type": "LEAGUE",
            "participant_type": "INDIVIDUAL",
            "team_size": 1,
            "sport_id": 1,
            "region_id": 1,
        })
        t_service.register(self.tournament["id"], user_id=1)
        t_service.register(self.tournament["id"], user_id=2)
        m = self.match_service.create_match(
            self.tournament["id"], {"home_user_id": 1, "away_user_id": 2, "round": 1}
        )
        self.match_service.record_result(self.tournament["id"], m["id"], home_score=3, away_score=1)

    def test_get_standings_returns_ranked_list(self):
        standings = self.standing_service.get_standings(self.tournament["id"])
        self.assertEqual(len(standings), 2)
        self.assertEqual(standings[0]["rank"], 1)
        self.assertEqual(standings[0]["points"], 3)  # winner (win_points default=3)
        self.assertEqual(standings[1]["points"], 0)  # loser (loss_points default=0)

    def test_standings_sorted_by_points_desc(self):
        standings = self.standing_service.get_standings(self.tournament["id"])
        points_list = [s["points"] for s in standings]
        self.assertEqual(points_list, sorted(points_list, reverse=True))

    def test_global_leaderboard(self):
        board = self.standing_service.get_global_leaderboard(region_id=1, sport_id=1)
        self.assertGreaterEqual(len(board), 1)
        # Winner (user 1 won 3-1) should be at top with global_points_per_win (default=10)
        self.assertEqual(board[0]["total_points"], 10)

    def test_global_leaderboard_empty_region(self):
        board = self.standing_service.get_global_leaderboard(region_id=99, sport_id=99)
        self.assertEqual(board, [])

    def test_standings_fallback_name_when_user_missing(self):
        standings = self.standing_service.get_standings(self.tournament["id"])
        names = {s["user_id"]: s["name"] for s in standings}
        self.assertEqual(names[1], "Player #1")
        self.assertEqual(names[2], "Player #2")

    def test_standings_resolves_real_user_name(self):
        from modules.user.model.user_model import User

        session = self.standing_service._s_repo._session_factory()
        try:
            session.add(User(id=1, name="Alice", phone="+1000000001", password_hash="", role="user"))
            session.commit()
        finally:
            session.close()

        standings = self.standing_service.get_standings(self.tournament["id"])
        names = {s["user_id"]: s["name"] for s in standings}
        self.assertEqual(names[1], "Alice")
        self.assertEqual(names[2], "Player #2")
