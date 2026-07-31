import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.cart_type.controller.sport_routes import router as sport_router

SUPER_ADMIN = {"id": 1, "name": "Admin", "phone": "+2000000000", "role": "super_admin", "is_active": True}
REGULAR_USER = {"id": 2, "name": "User", "phone": "+3000000000", "role": "user", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(sport_router)
    return app


class TestSportImageUpload(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    def _base_sport(self):
        return {"id": 3, "name": "Badminton", "description": "", "is_active": True, "image_url": None, "created_at": None, "updated_at": None}

    @patch("modules.cart_type.controller.sport_routes.save_media")
    @patch("modules.cart_type.controller.sport_routes._cart_type_service")
    def test_admin_can_upload_sport_image(self, mock_service, mock_save):
        self._set_user(SUPER_ADMIN)
        mock_service.get_cart_type.return_value = self._base_sport()
        mock_service.update_cart_type.return_value = {**self._base_sport(), "image_url": "/api/v1/sports/3/image"}
        resp = self.client.post(
            "/api/v1/sports/3/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 200)
        mock_save.assert_called_once_with("sports", 3, "photo.jpg", b"fake-bytes")
        mock_service.update_cart_type.assert_called_once_with(3, {"image_url": "/api/v1/sports/3/image"})

    def test_regular_user_forbidden(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post(
            "/api/v1/sports/3/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 403)

    @patch("modules.cart_type.controller.sport_routes._cart_type_service")
    def test_upload_rejects_non_image(self, mock_service):
        self._set_user(SUPER_ADMIN)
        mock_service.get_cart_type.return_value = self._base_sport()
        resp = self.client.post(
            "/api/v1/sports/3/image", files={"file": ("doc.pdf", b"not-an-image", "application/pdf")}
        )
        self.assertEqual(resp.status_code, 400)

    @patch("modules.cart_type.controller.sport_routes._cart_type_service")
    def test_upload_unknown_sport_404s(self, mock_service):
        self._set_user(SUPER_ADMIN)
        mock_service.get_cart_type.return_value = None
        resp = self.client.post(
            "/api/v1/sports/999/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 404)

    @patch("modules.cart_type.controller.sport_routes.read_media")
    def test_get_image_success_no_auth_required(self, mock_read):
        mock_read.return_value = (b"fake-bytes", "image/jpeg")
        resp = self.client.get("/api/v1/sports/3/image")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b"fake-bytes")

    @patch("modules.cart_type.controller.sport_routes.read_media")
    def test_get_image_missing_returns_404(self, mock_read):
        mock_read.return_value = None
        resp = self.client.get("/api/v1/sports/999/image")
        self.assertEqual(resp.status_code, 404)


if __name__ == "__main__":
    unittest.main()
