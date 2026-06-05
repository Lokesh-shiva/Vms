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
from modules.tournament.repository.tournament_participant_repository import TournamentParticipantRepository
from modules.tournament.repository.tournament_match_repository import TournamentMatchRepository
from modules.tournament.repository.tournament_standing_repository import TournamentStandingRepository
from modules.tournament.repository.player_score_repository import PlayerScoreRepository
from modules.tournament.repository.tournament_team_repository import TournamentTeamRepository
from modules.tournament.service.tournament_service import TournamentService
from modules.tournament.service.tournament_match_service import TournamentMatchService
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.user.model.user_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestTournamentMatchService(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        t_repo = TournamentRepository(session_factory=factory)
        team_repo = TournamentTeamRepository(session_factory=factory)
        p_repo = TournamentParticipantRepository(session_factory=factory)
        m_repo = TournamentMatchRepository(session_factory=factory)
        s_repo = TournamentStandingRepository(session_factory=factory)
        ps_repo = PlayerScoreRepository(session_factory=factory)
        self.t_service = TournamentService(
            repository=t_repo,
            team_repository=team_repo,
            participant_repository=p_repo,
        )
        self.service = TournamentMatchService(
            tournament_repository=t_repo,
            participant_repository=p_repo,
            match_repository=m_repo,
            standing_repository=s_repo,
            player_score_repository=ps_repo,
        )
        self.tournament = self.t_service.create_tournament({
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
        self.t_service.register(self.tournament["id"], user_id=1)
        self.t_service.register(self.tournament["id"], user_id=2)
        self.tid = self.tournament["id"]

    def test_create_match(self):
        m = self.service.create_match(self.tid, {"home_user_id": 1, "away_user_id": 2, "round": 1})
        self.assertEqual(m["status"], "SCHEDULED")
        self.assertEqual(m["home_user_id"], 1)

    def test_create_match_same_player_raises(self):
        with self.assertRaises(ValueError):
            self.service.create_match(self.tid, {"home_user_id": 1, "away_user_id": 1, "round": 1})

    def test_record_result_sets_points_from_rules(self):
        m = self.service.create_match(self.tid, {"home_user_id": 1, "away_user_id": 2, "round": 1})
        result = self.service.record_result(self.tid, m["id"], home_score=3, away_score=1)
        self.assertEqual(result["home_points_awarded"], 3)  # win_points default
        self.assertEqual(result["away_points_awarded"], 0)  # loss_points default
        self.assertEqual(result["status"], "COMPLETED")

    def test_record_result_draw(self):
        m = self.service.create_match(self.tid, {"home_user_id": 1, "away_user_id": 2, "round": 1})
        result = self.service.record_result(self.tid, m["id"], home_score=2, away_score=2)
        self.assertEqual(result["home_points_awarded"], 1)
        self.assertEqual(result["away_points_awarded"], 1)

    def test_record_result_manual_override(self):
        m = self.service.create_match(self.tid, {"home_user_id": 1, "away_user_id": 2, "round": 1})
        result = self.service.record_result(
            self.tid, m["id"], home_score=3, away_score=1,
            overrides={"home_points": 10, "away_points": 2, "notes": "admin adjustment"},
        )
        self.assertEqual(result["home_points_awarded"], 10)
        self.assertEqual(result["away_points_awarded"], 2)
        self.assertEqual(result["notes"], "admin adjustment")

    def test_record_result_already_completed_raises(self):
        m = self.service.create_match(self.tid, {"home_user_id": 1, "away_user_id": 2, "round": 1})
        self.service.record_result(self.tid, m["id"], home_score=3, away_score=1)
        with self.assertRaises(ValueError):
            self.service.record_result(self.tid, m["id"], home_score=2, away_score=0)

    def test_knockout_draw_raises(self):
        ko = self.t_service.create_tournament({
            "name": "KO Cup",
            "organizer": "Plixo",
            "start_date": date(2026, 8, 1),
            "end_date": date(2026, 8, 10),
            "format_type": "KNOCKOUT",
            "participant_type": "INDIVIDUAL",
            "team_size": 1,
        })
        self.t_service.register(ko["id"], user_id=1)
        self.t_service.register(ko["id"], user_id=2)
        m = self.service.create_match(ko["id"], {"home_user_id": 1, "away_user_id": 2, "round": 1})
        with self.assertRaises(ValueError):
            self.service.record_result(ko["id"], m["id"], home_score=2, away_score=2)

    def test_list_matches(self):
        self.service.create_match(self.tid, {"home_user_id": 1, "away_user_id": 2, "round": 1})
        matches = self.service.list_matches(self.tid)
        self.assertEqual(len(matches), 1)
