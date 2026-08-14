import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.tournament.controller.admin_vote_round_routes import router as admin_vote_router

MANAGER = {"id": 1, "name": "Manager", "phone": "+1000000001", "role": "tournament_manager", "is_active": True}
REGULAR_USER = {"id": 2, "name": "User", "phone": "+1000000002", "role": "user", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(admin_vote_router)
    return app


class TestAdminVoteRoundRoutes(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    @patch("modules.tournament.controller.admin_vote_round_routes.sport_vote_service")
    def test_manager_can_create_round(self, mock_service):
        self._set_user(MANAGER)
        mock_service.create_round.return_value = {"round_id": 1, "options": ["Badminton", "Tennis"]}
        resp = self.client.post(
            "/api/v1/admin/vote-rounds",
            json={"options": ["Badminton", "Tennis"], "closes_at": "2026-09-01T12:00:00"},
        )
        self.assertEqual(resp.status_code, 201)

    def test_regular_user_forbidden_from_creating_round(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post(
            "/api/v1/admin/vote-rounds",
            json={"options": ["Badminton", "Tennis"], "closes_at": "2026-09-01T12:00:00"},
        )
        self.assertEqual(resp.status_code, 403)

    @patch("modules.tournament.controller.admin_vote_round_routes.sport_vote_service")
    def test_create_round_validation_error_returns_400(self, mock_service):
        self._set_user(MANAGER)
        mock_service.create_round.side_effect = ValueError("Pick between 2 and 8 sports.")
        resp = self.client.post(
            "/api/v1/admin/vote-rounds",
            json={"options": ["Badminton"], "closes_at": "2026-09-01T12:00:00"},
        )
        self.assertEqual(resp.status_code, 400)

    @patch("modules.tournament.controller.admin_vote_round_routes.sport_vote_service")
    def test_manager_can_close_round(self, mock_service):
        self._set_user(MANAGER)
        mock_service.close_round.return_value = {"round_id": 1, "status": "CLOSED"}
        resp = self.client.post("/api/v1/admin/vote-rounds/1/close")
        self.assertEqual(resp.status_code, 200)

    @patch("modules.tournament.controller.admin_vote_round_routes.sport_vote_service")
    def test_close_unknown_round_returns_404(self, mock_service):
        self._set_user(MANAGER)
        mock_service.close_round.side_effect = ValueError("Vote round not found.")
        resp = self.client.post("/api/v1/admin/vote-rounds/999/close")
        self.assertEqual(resp.status_code, 404)

    @patch("modules.tournament.controller.admin_vote_round_routes.sport_vote_service")
    def test_get_current_round(self, mock_service):
        self._set_user(MANAGER)
        mock_service.get_admin_state.return_value = {"status": "OPEN", "options": ["Badminton", "Tennis"]}
        resp = self.client.get("/api/v1/admin/vote-rounds/current")
        self.assertEqual(resp.status_code, 200)


if __name__ == "__main__":
    unittest.main()
