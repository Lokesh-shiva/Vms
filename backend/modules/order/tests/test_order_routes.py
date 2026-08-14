import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.order.controller.order_routes import router as order_router
from modules.order.controller.admin_order_routes import router as admin_order_router

USER = {"id": 1, "name": "Player", "phone": "+1000000001", "role": "user", "is_active": True}
FINANCE = {"id": 2, "name": "Finance", "phone": "+1000000002", "role": "finance", "is_active": True}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(order_router)
    app.include_router(admin_order_router)
    return app


class TestOrderRoutes(unittest.TestCase):
    def setUp(self):
        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    @patch("modules.order.controller.order_routes.order_service")
    def test_create_order_success(self, mock_service):
        self._set_user(USER)
        mock_service.create_order.return_value = {"id": 1, "status": "PENDING_PAYMENT", "total_amount": 40.0}
        resp = self.client.post("/api/v1/orders", json={"items": [{"item_id": 1, "quantity": 2}]})
        self.assertEqual(resp.status_code, 201)
        mock_service.create_order.assert_called_once_with(1, [{"item_id": 1, "quantity": 2}])

    @patch("modules.order.controller.order_routes.order_service")
    def test_create_order_validation_error(self, mock_service):
        self._set_user(USER)
        mock_service.create_order.side_effect = ValueError("Cart is empty.")
        resp = self.client.post("/api/v1/orders", json={"items": []})
        self.assertEqual(resp.status_code, 400)

    @patch("modules.order.controller.order_routes.order_service")
    def test_mine_route_not_shadowed_by_order_id_route(self, mock_service):
        self._set_user(USER)
        mock_service.list_my_orders.return_value = [{"id": 1}]
        resp = self.client.get("/api/v1/orders/mine")
        self.assertEqual(resp.status_code, 200)
        mock_service.list_my_orders.assert_called_once_with(1)

    @patch("modules.order.controller.order_routes.order_service")
    def test_get_order_not_found(self, mock_service):
        self._set_user(USER)
        mock_service.get_order.side_effect = ValueError("Order not found.")
        resp = self.client.get("/api/v1/orders/999")
        self.assertEqual(resp.status_code, 404)

    @patch("modules.order.controller.order_routes.order_service")
    def test_submit_payment_success(self, mock_service):
        self._set_user(USER)
        mock_service.submit_payment.return_value = {"id": 1, "status": "UNDER_REVIEW"}
        resp = self.client.post("/api/v1/orders/1/submit-payment", json={"transaction_id": "TXN1"})
        self.assertEqual(resp.status_code, 200)

    def test_regular_user_forbidden_from_admin_orders(self):
        self._set_user(USER)
        resp = self.client.get("/api/v1/admin/orders")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.order.controller.admin_order_routes.order_service")
    def test_finance_can_list_orders(self, mock_service):
        self._set_user(FINANCE)
        mock_service.list_all_orders.return_value = []
        resp = self.client.get("/api/v1/admin/orders")
        self.assertEqual(resp.status_code, 200)

    @patch("modules.order.controller.admin_order_routes.order_service")
    def test_finance_can_approve_order(self, mock_service):
        self._set_user(FINANCE)
        mock_service.approve_order.return_value = {"id": 1, "status": "PAID"}
        resp = self.client.post("/api/v1/admin/orders/1/approve")
        self.assertEqual(resp.status_code, 200)

    @patch("modules.order.controller.admin_order_routes.order_service")
    def test_approve_order_validation_error(self, mock_service):
        self._set_user(FINANCE)
        mock_service.approve_order.side_effect = ValueError("Only orders under review can be approved.")
        resp = self.client.post("/api/v1/admin/orders/1/approve")
        self.assertEqual(resp.status_code, 400)


if __name__ == "__main__":
    unittest.main()
