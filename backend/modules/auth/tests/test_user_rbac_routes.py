import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.user.controller.user_routes import router as user_router


# ── Test fixtures ─────────────────────────────────────────────────────

ADMIN_USER = {
    "id": 1, "name": "Admin", "phone": "+1111111111",
    "role": "admin", "is_active": True,
}

REGULAR_USER = {
    "id": 2, "name": "User", "phone": "+2222222222",
    "role": "user", "is_active": True,
}

OTHER_USER = {
    "id": 3, "name": "Other", "phone": "+3333333333",
    "role": "user", "is_active": True,
}


def _build_test_app() -> FastAPI:
    """Build a minimal FastAPI app with only the user router."""
    app = FastAPI()
    app.include_router(user_router)
    return app


class TestUserRBAC(unittest.TestCase):
    """Verify RBAC enforcement on user management endpoints."""

    def setUp(self):
        self.app = _build_test_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    # ── POST /users (admin only) ─────────────────────────────────────

    def test_user_cannot_create_user(self):
        """Regular user gets 403 on POST /users."""
        self._set_user(REGULAR_USER)
        resp = self.client.post("/api/v1/users", json={
            "name": "New", "phone": "+4444444444",
        })
        self.assertEqual(resp.status_code, 403)

    @patch("modules.user.controller.user_routes.user_service")
    def test_admin_can_create_user(self, mock_svc):
        """Admin can create a user via POST /users."""
        self._set_user(ADMIN_USER)
        mock_svc.create_user.return_value = {
            "id": 10, "name": "New", "phone": "+4444444444",
            "role": "user", "is_active": True,
        }
        resp = self.client.post("/api/v1/users", json={
            "name": "New", "phone": "+4444444444",
        })
        self.assertEqual(resp.status_code, 201)
        mock_svc.create_user.assert_called_once()

    # ── PUT /users/{id} (ownership + role guard) ─────────────────────

    def test_user_cannot_update_other_user(self):
        """Regular user gets 403 updating another user's profile."""
        self._set_user(REGULAR_USER)
        resp = self.client.put(
            f"/api/v1/users/{OTHER_USER['id']}",
            json={"name": "Hacked"},
        )
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_change_own_role(self):
        """Regular user gets 403 trying to escalate their own role."""
        self._set_user(REGULAR_USER)
        resp = self.client.put(
            f"/api/v1/users/{REGULAR_USER['id']}",
            json={"role": "admin"},
        )
        self.assertEqual(resp.status_code, 403)

    @patch("modules.user.controller.user_routes.user_service")
    def test_user_can_update_own_non_role_fields(self, mock_svc):
        """Regular user can update their own name."""
        self._set_user(REGULAR_USER)
        mock_svc.update_user.return_value = {
            "id": REGULAR_USER["id"], "name": "Updated",
            "phone": REGULAR_USER["phone"], "role": "user", "is_active": True,
        }
        resp = self.client.put(
            f"/api/v1/users/{REGULAR_USER['id']}",
            json={"name": "Updated"},
        )
        self.assertEqual(resp.status_code, 200)
        mock_svc.update_user.assert_called_once()

    @patch("modules.user.controller.user_routes.user_service")
    def test_admin_can_update_user_role(self, mock_svc):
        """Admin can change another user's role."""
        self._set_user(ADMIN_USER)
        mock_svc.update_user.return_value = {
            "id": REGULAR_USER["id"], "name": "User",
            "phone": "+2222222222", "role": "admin", "is_active": True,
        }
        resp = self.client.put(
            f"/api/v1/users/{REGULAR_USER['id']}",
            json={"role": "admin"},
        )
        self.assertEqual(resp.status_code, 200)
        mock_svc.update_user.assert_called_once()

    # ── DELETE /users/{id} (admin only + self-delete guard) ──────────

    def test_user_cannot_delete_user(self):
        """Regular user gets 403 on DELETE /users/{id}."""
        self._set_user(REGULAR_USER)
        resp = self.client.delete(f"/api/v1/users/{OTHER_USER['id']}")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.user.controller.user_routes.user_service")
    def test_admin_can_delete_user(self, mock_svc):
        """Admin can delete another user."""
        self._set_user(ADMIN_USER)
        mock_svc.delete_user.return_value = True
        resp = self.client.delete(f"/api/v1/users/{REGULAR_USER['id']}")
        self.assertEqual(resp.status_code, 200)
        mock_svc.delete_user.assert_called_once()

    @patch("modules.user.controller.user_routes.user_service")
    def test_admin_cannot_delete_self(self, mock_svc):
        """Admin gets 400 trying to delete their own account."""
        self._set_user(ADMIN_USER)
        mock_svc.delete_user.side_effect = ValueError(
            "Cannot delete your own account."
        )
        resp = self.client.delete(f"/api/v1/users/{ADMIN_USER['id']}")
        self.assertEqual(resp.status_code, 400)

    # ── GET /users/{id} (ownership) ──────────────────────────────────

    @patch("modules.user.controller.user_routes.user_service")
    def test_user_can_view_own_profile(self, mock_svc):
        """Regular user can view their own profile."""
        self._set_user(REGULAR_USER)
        mock_svc.get_user.return_value = {
            "id": REGULAR_USER["id"], "name": "User",
            "phone": "+2222222222", "role": "user", "is_active": True,
        }
        resp = self.client.get(f"/api/v1/users/{REGULAR_USER['id']}")
        self.assertEqual(resp.status_code, 200)

    def test_user_cannot_view_other_profile(self):
        """Regular user gets 403 viewing another user's profile."""
        self._set_user(REGULAR_USER)
        resp = self.client.get(f"/api/v1/users/{OTHER_USER['id']}")
        self.assertEqual(resp.status_code, 403)


if __name__ == "__main__":
    unittest.main()
