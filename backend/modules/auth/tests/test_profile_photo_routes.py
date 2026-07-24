import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.controller.auth_routes import router as auth_router
from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.user.controller.user_routes import router as user_router

REGULAR_USER = {"id": 2, "name": "User", "phone": "+2222222222", "role": "user", "is_active": True}


def _build_test_app() -> FastAPI:
    app = FastAPI()
    app.include_router(auth_router)
    app.include_router(user_router)
    return app


class TestProfilePhotoRoutes(unittest.TestCase):
    def setUp(self):
        self.app = _build_test_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)
        self.app.dependency_overrides[get_current_user] = lambda: REGULAR_USER

    def tearDown(self):
        self.app.dependency_overrides.clear()

    # ── POST /auth/me/profile-photo ─────────────────────────────────────

    @patch("modules.auth.controller.auth_routes.user_repository")
    @patch("modules.auth.controller.auth_routes.save_profile_photo")
    def test_upload_success(self, mock_save, mock_repo):
        mock_repo.update.return_value = {
            "id": 2, "profile_photo_url": "/api/v1/users/2/profile-photo",
        }
        resp = self.client.post(
            "/api/v1/auth/me/profile-photo",
            files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")},
        )
        self.assertEqual(resp.status_code, 200)
        mock_save.assert_called_once_with(2, "photo.jpg", b"fake-bytes")
        mock_repo.update.assert_called_once_with(
            2, {"profile_photo_url": "/api/v1/users/2/profile-photo"}
        )

    def test_upload_rejects_non_image(self):
        resp = self.client.post(
            "/api/v1/auth/me/profile-photo",
            files={"file": ("doc.pdf", b"not-an-image", "application/pdf")},
        )
        self.assertEqual(resp.status_code, 400)

    def test_upload_rejects_oversized_file(self):
        big_content = b"x" * (5 * 1024 * 1024 + 1)
        resp = self.client.post(
            "/api/v1/auth/me/profile-photo",
            files={"file": ("photo.jpg", big_content, "image/jpeg")},
        )
        self.assertEqual(resp.status_code, 400)

    # ── GET /users/{id}/profile-photo ───────────────────────────────────

    @patch("modules.user.controller.user_routes.read_profile_photo")
    def test_get_photo_success_no_auth_required(self, mock_read):
        mock_read.return_value = (b"fake-bytes", "image/jpeg")
        self.app.dependency_overrides.clear()  # confirm no auth dependency needed
        resp = self.client.get("/api/v1/users/2/profile-photo")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b"fake-bytes")
        self.assertEqual(resp.headers["content-type"], "image/jpeg")

    @patch("modules.user.controller.user_routes.read_profile_photo")
    def test_get_photo_missing_returns_404(self, mock_read):
        mock_read.return_value = None
        self.app.dependency_overrides.clear()
        resp = self.client.get("/api/v1/users/999/profile-photo")
        self.assertEqual(resp.status_code, 404)


if __name__ == "__main__":
    unittest.main()
