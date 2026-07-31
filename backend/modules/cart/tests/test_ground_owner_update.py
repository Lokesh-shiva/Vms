"""Route-level tests for PUT /api/v1/grounds/{id} ownership scoping.

Verifies the fix for a real authorization bug: GROUND_OWNER was a member of
_ADMIN_ROLES, so require_admin previously let any ground owner edit ANY
ground with unrestricted field access (including reassigning ownership).
"""
import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.cart.controller.ground_routes import router as ground_router


GROUND_OWNER = {"id": 10, "name": "Owner", "phone": "+1000000000", "role": "ground_owner", "is_active": True}
OTHER_GROUND_OWNER = {"id": 11, "name": "Other Owner", "phone": "+1000000001", "role": "ground_owner", "is_active": True}
SUPER_ADMIN = {"id": 1, "name": "Admin", "phone": "+2000000000", "role": "super_admin", "is_active": True}
REGULAR_USER = {"id": 2, "name": "User", "phone": "+3000000000", "role": "user", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(ground_router)
    return app


class TestGroundOwnerUpdateScoping(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_owner_can_toggle_own_ground(self, mock_service):
        self._set_user(GROUND_OWNER)
        mock_service.get_cart.return_value = {"id": 5, "owner_user_id": 10}
        mock_service.update_cart.return_value = {"id": 5, "owner_user_id": 10, "is_active": False, "label": "X", "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE", "latitude": None, "longitude": None, "created_at": None, "updated_at": None}
        resp = self.client.put("/api/v1/grounds/5", json={"is_active": False})
        self.assertEqual(resp.status_code, 200)
        mock_service.update_cart.assert_called_once()

    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_owner_cannot_update_someone_elses_ground(self, mock_service):
        self._set_user(OTHER_GROUND_OWNER)
        mock_service.get_cart.return_value = {"id": 5, "owner_user_id": 10}
        resp = self.client.put("/api/v1/grounds/5", json={"is_active": False})
        self.assertEqual(resp.status_code, 403)
        mock_service.update_cart.assert_not_called()

    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_owner_cannot_reassign_ownership(self, mock_service):
        self._set_user(GROUND_OWNER)
        mock_service.get_cart.return_value = {"id": 5, "owner_user_id": 10}
        resp = self.client.put("/api/v1/grounds/5", json={"owner_user_id": 99})
        self.assertEqual(resp.status_code, 403)
        mock_service.update_cart.assert_not_called()

    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_owner_cannot_change_region_or_sport(self, mock_service):
        self._set_user(GROUND_OWNER)
        mock_service.get_cart.return_value = {"id": 5, "owner_user_id": 10}
        resp = self.client.put("/api/v1/grounds/5", json={"location_id": 2, "sport_id": 3})
        self.assertEqual(resp.status_code, 403)
        mock_service.update_cart.assert_not_called()

    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_super_admin_retains_full_access(self, mock_service):
        self._set_user(SUPER_ADMIN)
        mock_service.update_cart.return_value = {"id": 5, "owner_user_id": 99, "is_active": True, "label": "X", "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE", "latitude": None, "longitude": None, "created_at": None, "updated_at": None}
        resp = self.client.put("/api/v1/grounds/5", json={"owner_user_id": 99})
        self.assertEqual(resp.status_code, 200)
        mock_service.get_cart.assert_not_called()  # admins skip the ownership check entirely

    def test_regular_user_forbidden(self):
        self._set_user(REGULAR_USER)
        resp = self.client.put("/api/v1/grounds/5", json={"is_active": False})
        self.assertEqual(resp.status_code, 403)


class TestGroundImageUpload(unittest.TestCase):
    """POST /api/v1/grounds/{id}/image and the public GET serve route."""

    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    def _base_cart(self, owner_user_id=None):
        return {
            "id": 5, "owner_user_id": owner_user_id, "label": "X", "region_id": 1,
            "cart_type_id": 1, "status": "AVAILABLE", "is_active": True,
            "latitude": None, "longitude": None, "image_url": None,
            "created_at": None, "updated_at": None,
        }

    @patch("modules.cart.controller.ground_routes.save_media")
    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_owner_can_upload_own_ground_image(self, mock_service, mock_save):
        self._set_user(GROUND_OWNER)
        mock_service.get_cart.return_value = self._base_cart(owner_user_id=10)
        mock_service.update_cart.return_value = {
            **self._base_cart(owner_user_id=10), "image_url": "/api/v1/grounds/5/image",
        }
        resp = self.client.post(
            "/api/v1/grounds/5/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 200)
        mock_save.assert_called_once_with("grounds", 5, "photo.jpg", b"fake-bytes")
        mock_service.update_cart.assert_called_once_with(5, {"image_url": "/api/v1/grounds/5/image"})

    @patch("modules.cart.controller.ground_routes.save_media")
    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_owner_cannot_upload_someone_elses_ground_image(self, mock_service, mock_save):
        self._set_user(OTHER_GROUND_OWNER)
        mock_service.get_cart.return_value = self._base_cart(owner_user_id=10)
        resp = self.client.post(
            "/api/v1/grounds/5/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 403)
        mock_save.assert_not_called()

    @patch("modules.cart.controller.ground_routes.save_media")
    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_super_admin_can_upload_any_ground_image(self, mock_service, mock_save):
        self._set_user(SUPER_ADMIN)
        mock_service.get_cart.return_value = self._base_cart(owner_user_id=99)
        mock_service.update_cart.return_value = {
            **self._base_cart(owner_user_id=99), "image_url": "/api/v1/grounds/5/image",
        }
        resp = self.client.post(
            "/api/v1/grounds/5/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 200)

    def test_regular_user_cannot_upload(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post(
            "/api/v1/grounds/5/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 403)

    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_upload_rejects_non_image(self, mock_service):
        self._set_user(SUPER_ADMIN)
        mock_service.get_cart.return_value = self._base_cart(owner_user_id=99)
        resp = self.client.post(
            "/api/v1/grounds/5/image", files={"file": ("doc.pdf", b"not-an-image", "application/pdf")}
        )
        self.assertEqual(resp.status_code, 400)

    @patch("modules.cart.controller.ground_routes._cart_service")
    def test_upload_unknown_ground_404s(self, mock_service):
        self._set_user(SUPER_ADMIN)
        mock_service.get_cart.return_value = None
        resp = self.client.post(
            "/api/v1/grounds/999/image", files={"file": ("photo.jpg", b"fake-bytes", "image/jpeg")}
        )
        self.assertEqual(resp.status_code, 404)

    @patch("modules.cart.controller.ground_routes.read_media")
    def test_get_image_success_no_auth_required(self, mock_read):
        mock_read.return_value = (b"fake-bytes", "image/jpeg")
        resp = self.client.get("/api/v1/grounds/5/image")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b"fake-bytes")

    @patch("modules.cart.controller.ground_routes.read_media")
    def test_get_image_missing_returns_404(self, mock_read):
        mock_read.return_value = None
        resp = self.client.get("/api/v1/grounds/999/image")
        self.assertEqual(resp.status_code, 404)


if __name__ == "__main__":
    unittest.main()
