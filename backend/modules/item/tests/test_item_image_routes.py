import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.item.controller.item_routes import router as item_router

SUPER_ADMIN = {"id": 1, "name": "Admin", "phone": "+2000000000", "role": "super_admin", "is_active": True}
REGULAR_USER = {"id": 2, "name": "User", "phone": "+3000000000", "role": "user", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(item_router)
    return app


class TestItemImageUpload(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    def _base_item(self):
        return {
            "id": 7, "cart_type_id": 1, "name": "Water Bottle", "description": "", "price": 20.0,
            "image_urls": [], "image_url": None, "is_available": True,
            "created_at": None, "updated_at": None,
        }

    @patch("modules.item.controller.item_routes.save_media")
    @patch("modules.item.controller.item_routes.item_service")
    def test_admin_can_upload_item_image(self, mock_service, mock_save):
        self._set_user(SUPER_ADMIN)
        mock_service.get_item.return_value = self._base_item()
        mock_service.update_item.return_value = {**self._base_item(), "image_url": "/api/v1/items/7/image"}
        resp = self.client.post(
            "/api/v1/items/7/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 200)
        mock_save.assert_called_once_with("items", 7, "photo.jpg", b"fake-bytes")
        mock_service.update_item.assert_called_once_with(7, {"image_url": "/api/v1/items/7/image"})

    def test_regular_user_forbidden(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post(
            "/api/v1/items/7/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 403)

    @patch("modules.item.controller.item_routes.item_service")
    def test_upload_rejects_non_image(self, mock_service):
        self._set_user(SUPER_ADMIN)
        mock_service.get_item.return_value = self._base_item()
        resp = self.client.post(
            "/api/v1/items/7/image", files={"file": ("doc.pdf", b"not-an-image", "application/pdf")}
        )
        self.assertEqual(resp.status_code, 400)

    @patch("modules.item.controller.item_routes.item_service")
    def test_upload_unknown_item_404s(self, mock_service):
        self._set_user(SUPER_ADMIN)
        mock_service.get_item.return_value = None
        resp = self.client.post(
            "/api/v1/items/999/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 404)

    @patch("modules.item.controller.item_routes.read_media")
    def test_get_image_success_no_auth_required(self, mock_read):
        mock_read.return_value = (b"fake-bytes", "image/jpeg")
        resp = self.client.get("/api/v1/items/7/image")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b"fake-bytes")

    @patch("modules.item.controller.item_routes.read_media")
    def test_get_image_missing_returns_404(self, mock_read):
        mock_read.return_value = None
        resp = self.client.get("/api/v1/items/999/image")
        self.assertEqual(resp.status_code, 404)


if __name__ == "__main__":
    unittest.main()
