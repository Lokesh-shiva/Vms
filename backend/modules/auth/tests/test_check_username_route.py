import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.controller.auth_routes import router as auth_router
from modules.auth.dependencies.auth_dependencies import get_current_user

REGULAR_USER = {"id": 2, "name": "User", "phone": "+2222222222", "role": "user", "is_active": True}


def _build_test_app() -> FastAPI:
    app = FastAPI()
    app.include_router(auth_router)
    return app


class TestCheckUsernameRoute(unittest.TestCase):
    def setUp(self):
        self.app = _build_test_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)
        self.app.dependency_overrides[get_current_user] = lambda: REGULAR_USER

    def tearDown(self):
        self.app.dependency_overrides.clear()

    @patch("modules.auth.controller.auth_routes.user_repository")
    def test_free_username_is_available(self, mock_repo):
        mock_repo.find_by_username.return_value = None
        resp = self.client.get("/api/v1/auth/check-username", params={"username": "freehandle"})
        self.assertEqual(resp.status_code, 200)
        body = resp.json()["data"]
        self.assertTrue(body["available"])
        self.assertIsNone(body["reason"])

    @patch("modules.auth.controller.auth_routes.user_repository")
    def test_taken_by_someone_else_is_unavailable(self, mock_repo):
        mock_repo.find_by_username.return_value = {"id": 99, "username": "taken"}
        resp = self.client.get("/api/v1/auth/check-username", params={"username": "taken"})
        self.assertEqual(resp.status_code, 200)
        body = resp.json()["data"]
        self.assertFalse(body["available"])
        self.assertEqual(body["reason"], "This username is already taken.")

    @patch("modules.auth.controller.auth_routes.user_repository")
    def test_own_current_username_is_available(self, mock_repo):
        mock_repo.find_by_username.return_value = {"id": 2, "username": "myname"}
        resp = self.client.get("/api/v1/auth/check-username", params={"username": "myname"})
        self.assertEqual(resp.status_code, 200)
        body = resp.json()["data"]
        self.assertTrue(body["available"])

    @patch("modules.auth.controller.auth_routes.user_repository")
    def test_invalid_format_returns_reason_without_db_lookup(self, mock_repo):
        resp = self.client.get("/api/v1/auth/check-username", params={"username": "a"})
        self.assertEqual(resp.status_code, 200)
        body = resp.json()["data"]
        self.assertFalse(body["available"])
        self.assertIn("3-20 characters", body["reason"])
        mock_repo.find_by_username.assert_not_called()


if __name__ == "__main__":
    unittest.main()
