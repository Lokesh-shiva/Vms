import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.trainer.controller.trainer_routes import router as trainer_router
from modules.trainer.controller.trainer_booking_routes import router as booking_router
from modules.trainer.controller.admin_trainer_booking_routes import router as admin_booking_router

USER = {"id": 1, "name": "Player", "phone": "+1000000001", "role": "user", "is_active": True}
ADMIN = {"id": 2, "name": "Admin", "phone": "+1000000002", "role": "ops_manager", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(trainer_router)
    app.include_router(booking_router)
    app.include_router(admin_booking_router)
    return app


class TestTrainerRoutes(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    @patch("modules.trainer.controller.trainer_routes.trainer_service")
    def test_list_trainers_public(self, mock_service):
        mock_service.list_trainers.return_value = [{"id": 1, "name": "Coach Ravi"}]
        resp = self.client.get("/api/v1/trainers")
        self.assertEqual(resp.status_code, 200)

    def test_create_trainer_requires_admin(self):
        self._set_user(USER)
        resp = self.client.post("/api/v1/trainers", json={"name": "Coach Ravi", "rate_per_session": 500})
        self.assertEqual(resp.status_code, 403)

    @patch("modules.trainer.controller.trainer_routes.trainer_service")
    def test_admin_can_create_trainer(self, mock_service):
        self._set_user(ADMIN)
        mock_service.create_trainer.return_value = {"id": 1, "name": "Coach Ravi"}
        resp = self.client.post("/api/v1/trainers", json={"name": "Coach Ravi", "rate_per_session": 500})
        self.assertEqual(resp.status_code, 201)

    @patch("modules.trainer.controller.trainer_booking_routes.trainer_booking_service")
    def test_create_booking_success(self, mock_service):
        self._set_user(USER)
        mock_service.create_booking.return_value = {"id": 1, "status": "PENDING_PAYMENT"}
        resp = self.client.post(
            "/api/v1/trainer-bookings",
            json={"trainer_id": 1, "session_date": "2026-09-01", "session_time": "18:00"},
        )
        self.assertEqual(resp.status_code, 201)

    @patch("modules.trainer.controller.trainer_booking_routes.trainer_booking_service")
    def test_mine_route_not_shadowed_by_booking_id_route(self, mock_service):
        self._set_user(USER)
        mock_service.list_my_bookings.return_value = []
        resp = self.client.get("/api/v1/trainer-bookings/mine")
        self.assertEqual(resp.status_code, 200)
        mock_service.list_my_bookings.assert_called_once_with(1)

    def test_regular_user_forbidden_from_admin_bookings(self):
        self._set_user(USER)
        resp = self.client.get("/api/v1/admin/trainer-bookings")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.trainer.controller.admin_trainer_booking_routes.trainer_booking_service")
    def test_admin_can_approve_booking(self, mock_service):
        self._set_user(ADMIN)
        mock_service.approve_booking.return_value = {"id": 1, "status": "CONFIRMED"}
        resp = self.client.post("/api/v1/admin/trainer-bookings/1/approve")
        self.assertEqual(resp.status_code, 200)


if __name__ == "__main__":
    unittest.main()
