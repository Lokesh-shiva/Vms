import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.tournament.controller.tournament_vote_routes import router as vote_router

USER = {"id": 1, "name": "Player", "phone": "+1000000001", "role": "user", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(vote_router)
    return app


class TestTournamentVoteRoutes(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)
        self.app.dependency_overrides[get_current_user] = lambda: USER

    def tearDown(self):
        self.app.dependency_overrides.clear()

    @patch("modules.tournament.controller.tournament_vote_routes.sport_vote_service")
    def test_get_votes_success(self, mock_service):
        mock_service.get_state.return_value = {
            "results": [{"sport": "Badminton", "votes": 3}], "my_vote": "Badminton", "total_votes": 3,
        }
        resp = self.client.get("/api/v1/tournaments/votes")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()["data"]["total_votes"], 3)

    @patch("modules.tournament.controller.tournament_vote_routes.sport_vote_service")
    def test_get_votes_no_region_returns_400(self, mock_service):
        mock_service.get_state.side_effect = ValueError("Set your area in your profile before voting.")
        resp = self.client.get("/api/v1/tournaments/votes")
        self.assertEqual(resp.status_code, 400)

    @patch("modules.tournament.controller.tournament_vote_routes.sport_vote_service")
    def test_cast_vote_success(self, mock_service):
        mock_service.cast_vote.return_value = {
            "results": [{"sport": "Tennis", "votes": 1}], "my_vote": "Tennis", "total_votes": 1,
        }
        resp = self.client.post("/api/v1/tournaments/votes", json={"sport": "Tennis"})
        self.assertEqual(resp.status_code, 200)
        mock_service.cast_vote.assert_called_once_with(1, "Tennis")

    @patch("modules.tournament.controller.tournament_vote_routes.sport_vote_service")
    def test_cast_vote_invalid_sport_returns_400(self, mock_service):
        mock_service.cast_vote.side_effect = ValueError("'Chess' is not a currently active sport.")
        resp = self.client.post("/api/v1/tournaments/votes", json={"sport": "Chess"})
        self.assertEqual(resp.status_code, 400)


if __name__ == "__main__":
    unittest.main()
