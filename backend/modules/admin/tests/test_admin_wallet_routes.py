import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.admin.controller.admin_routes import router as admin_router

SUPPORT = {"id": 1, "name": "Staff", "phone": "+1000000001", "role": "support", "is_active": True}
FINANCE = {"id": 2, "name": "Finance", "phone": "+1000000002", "role": "finance", "is_active": True}
REGULAR_USER = {"id": 3, "name": "User", "phone": "+1000000003", "role": "user", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(admin_router)
    return app


class TestAdminWalletRoute(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    @patch("modules.wallet.service.wallet_service.wallet_service")
    @patch("modules.user.repository.user_repository.user_repository")
    def test_support_can_view_any_wallet(self, mock_user_repo, mock_wallet_service):
        self._set_user(SUPPORT)
        mock_user_repo.find_by_id.return_value = {"id": 7, "name": "Player"}
        mock_wallet_service.get_balance.return_value = 120
        mock_wallet_service.get_transactions.return_value = [
            {"id": 1, "type": "credit", "amount": 20, "description": "Match bonus", "created_at": "2026-01-01T00:00:00"}
        ]
        resp = self.client.get("/api/v1/admin/wallet/7")
        self.assertEqual(resp.status_code, 200)
        body = resp.json()
        self.assertEqual(body["data"]["balance"], 120)
        self.assertEqual(len(body["data"]["transactions"]), 1)

    @patch("modules.wallet.service.wallet_service.wallet_service")
    @patch("modules.user.repository.user_repository.user_repository")
    def test_finance_can_view_any_wallet(self, mock_user_repo, mock_wallet_service):
        self._set_user(FINANCE)
        mock_user_repo.find_by_id.return_value = {"id": 7, "name": "Player"}
        mock_wallet_service.get_balance.return_value = 0
        mock_wallet_service.get_transactions.return_value = []
        resp = self.client.get("/api/v1/admin/wallet/7")
        self.assertEqual(resp.status_code, 200)

    def test_regular_user_forbidden(self):
        self._set_user(REGULAR_USER)
        resp = self.client.get("/api/v1/admin/wallet/7")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.user.repository.user_repository.user_repository")
    def test_unknown_user_404s(self, mock_user_repo):
        self._set_user(SUPPORT)
        mock_user_repo.find_by_id.return_value = None
        resp = self.client.get("/api/v1/admin/wallet/999")
        self.assertEqual(resp.status_code, 404)


if __name__ == "__main__":
    unittest.main()
