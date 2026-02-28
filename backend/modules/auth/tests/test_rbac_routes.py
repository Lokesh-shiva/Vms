import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.location.controller.location_routes import router as location_router
from modules.cart_type.controller.cart_type_routes import router as cart_type_router
from modules.item.controller.item_routes import router as item_router
from modules.cart.controller.cart_routes import router as cart_router
from modules.booking.controller.booking_routes import router as booking_router


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
    """Build a minimal FastAPI app with only the routers under test."""
    app = FastAPI()
    app.include_router(location_router)
    app.include_router(cart_type_router)
    app.include_router(item_router)
    app.include_router(cart_router)
    app.include_router(booking_router)
    return app


# ── Admin Route Protection ────────────────────────────────────────────


class TestAdminRouteProtection(unittest.TestCase):
    """Verify that POST/PUT/DELETE on system routes require admin role."""

    def setUp(self):
        self.app = _build_test_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    # ── Location ──────────────────────────────────────────────────────

    def test_user_cannot_create_location(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post("/api/v1/locations", json={"name": "Test"})
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_update_location(self):
        self._set_user(REGULAR_USER)
        resp = self.client.put("/api/v1/locations/1", json={"name": "New"})
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_delete_location(self):
        self._set_user(REGULAR_USER)
        resp = self.client.delete("/api/v1/locations/1")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.location.controller.location_routes.location_service")
    def test_admin_can_create_location(self, mock_svc):
        self._set_user(ADMIN_USER)
        mock_svc.create_location.return_value = {"id": 1, "name": "Test"}
        resp = self.client.post("/api/v1/locations", json={"name": "Test"})
        self.assertNotEqual(resp.status_code, 403)
        mock_svc.create_location.assert_called_once()

    # ── Cart Type ─────────────────────────────────────────────────────

    def test_user_cannot_create_cart_type(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post("/api/v1/cart-types", json={"name": "Standard"})
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_update_cart_type(self):
        self._set_user(REGULAR_USER)
        resp = self.client.put("/api/v1/cart-types/1", json={"name": "New"})
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_delete_cart_type(self):
        self._set_user(REGULAR_USER)
        resp = self.client.delete("/api/v1/cart-types/1")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.cart_type.controller.cart_type_routes.cart_type_service")
    def test_admin_can_create_cart_type(self, mock_svc):
        self._set_user(ADMIN_USER)
        mock_svc.create_cart_type.return_value = {"id": 1, "name": "Standard"}
        resp = self.client.post("/api/v1/cart-types", json={"name": "Standard"})
        self.assertNotEqual(resp.status_code, 403)
        mock_svc.create_cart_type.assert_called_once()

    # ── Item ──────────────────────────────────────────────────────────

    def test_user_cannot_create_item(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post("/api/v1/items", json={
            "name": "Widget", "cart_type_id": 1, "price": 10.0,
        })
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_update_item(self):
        self._set_user(REGULAR_USER)
        resp = self.client.put("/api/v1/items/1", json={"name": "New"})
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_delete_item(self):
        self._set_user(REGULAR_USER)
        resp = self.client.delete("/api/v1/items/1")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.item.controller.item_routes.item_service")
    def test_admin_can_create_item(self, mock_svc):
        self._set_user(ADMIN_USER)
        mock_svc.create_item.return_value = {
            "id": 1, "name": "Widget", "cart_type_id": 1, "price": 10.0,
        }
        resp = self.client.post("/api/v1/items", json={
            "name": "Widget", "cart_type_id": 1, "price": 10.0,
        })
        self.assertNotEqual(resp.status_code, 403)
        mock_svc.create_item.assert_called_once()

    # ── Cart ──────────────────────────────────────────────────────────

    def test_user_cannot_create_cart(self):
        self._set_user(REGULAR_USER)
        resp = self.client.post("/api/v1/carts", json={
            "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
        })
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_update_cart(self):
        self._set_user(REGULAR_USER)
        resp = self.client.put("/api/v1/carts/1", json={"status": "BUSY"})
        self.assertEqual(resp.status_code, 403)

    def test_user_cannot_delete_cart(self):
        self._set_user(REGULAR_USER)
        resp = self.client.delete("/api/v1/carts/1")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.cart.controller.cart_routes.cart_service")
    def test_admin_can_create_cart(self, mock_svc):
        self._set_user(ADMIN_USER)
        mock_svc.create_cart.return_value = {
            "id": 1, "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
        }
        resp = self.client.post("/api/v1/carts", json={
            "region_id": 1, "cart_type_id": 1, "status": "AVAILABLE",
        })
        self.assertNotEqual(resp.status_code, 403)
        mock_svc.create_cart.assert_called_once()


# ── Booking RBAC ──────────────────────────────────────────────────────


class TestBookingRBAC(unittest.TestCase):
    """Verify RBAC enforcement on booking endpoints."""

    def setUp(self):
        self.app = _build_test_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)

    def tearDown(self):
        self.app.dependency_overrides.clear()

    def _set_user(self, user):
        self.app.dependency_overrides[get_current_user] = lambda: user

    # ── Booking Creation ──────────────────────────────────────────────

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_create_booking_overrides_user_id(self, mock_svc):
        """Client-provided user_id is overridden by authenticated user's id."""
        self._set_user(REGULAR_USER)
        mock_svc.create_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CONFIRMED",
        }
        resp = self.client.post("/api/v1/bookings", json={
            "user_id": 999,
            "region_id": 1, "cart_type_id": 1, "timeslot_id": 1,
            "address": "123 Main St", "booking_fee": 50.0,
        })
        self.assertNotEqual(resp.status_code, 403)
        call_data = mock_svc.create_booking.call_args[0][0]
        self.assertEqual(call_data["user_id"], REGULAR_USER["id"])

    def test_admin_cannot_create_booking(self):
        """Admin role is rejected by require_user (booking creation is user-only)."""
        self._set_user(ADMIN_USER)
        resp = self.client.post("/api/v1/bookings", json={
            "user_id": 1, "region_id": 1, "cart_type_id": 1,
            "timeslot_id": 1, "address": "123 Main St", "booking_fee": 50.0,
        })
        self.assertEqual(resp.status_code, 403)

    # ── Booking Cancellation ──────────────────────────────────────────

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_user_can_cancel_own_booking(self, mock_svc):
        """User can cancel their own confirmed booking."""
        self._set_user(REGULAR_USER)
        mock_svc.get_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CONFIRMED",
        }
        mock_svc.cancel_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CANCELLED",
        }
        resp = self.client.post("/api/v1/bookings/1/cancel")
        self.assertEqual(resp.status_code, 200)

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_user_cannot_cancel_other_users_booking(self, mock_svc):
        """User gets 403 when trying to cancel another user's booking."""
        self._set_user(REGULAR_USER)
        mock_svc.get_booking.return_value = {
            "id": 1, "user_id": OTHER_USER["id"], "status": "CONFIRMED",
        }
        resp = self.client.post("/api/v1/bookings/1/cancel")
        self.assertEqual(resp.status_code, 403)
        mock_svc.cancel_booking.assert_not_called()

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_admin_can_cancel_any_booking(self, mock_svc):
        """Admin can cancel any user's booking."""
        self._set_user(ADMIN_USER)
        mock_svc.get_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CONFIRMED",
        }
        mock_svc.cancel_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CANCELLED",
        }
        resp = self.client.post("/api/v1/bookings/1/cancel")
        self.assertEqual(resp.status_code, 200)

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_cancel_non_confirmed_booking_returns_400(self, mock_svc):
        """Cannot cancel a booking that is not in CONFIRMED status."""
        self._set_user(REGULAR_USER)
        mock_svc.get_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CANCELLED",
        }
        resp = self.client.post("/api/v1/bookings/1/cancel")
        self.assertEqual(resp.status_code, 400)

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_cancel_nonexistent_booking_returns_404(self, mock_svc):
        """Cancelling a non-existent booking returns 404."""
        self._set_user(REGULAR_USER)
        mock_svc.get_booking.return_value = None
        resp = self.client.post("/api/v1/bookings/999/cancel")
        self.assertEqual(resp.status_code, 404)

    # ── Booking Completion ────────────────────────────────────────────

    def test_user_cannot_complete_booking(self):
        """User role cannot complete bookings (admin only)."""
        self._set_user(REGULAR_USER)
        resp = self.client.post("/api/v1/bookings/1/complete")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_admin_can_complete_booking(self, mock_svc):
        """Admin can complete a confirmed booking."""
        self._set_user(ADMIN_USER)
        mock_svc.complete_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "COMPLETED",
        }
        resp = self.client.post("/api/v1/bookings/1/complete")
        self.assertEqual(resp.status_code, 200)

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_admin_cannot_complete_cancelled_booking(self, mock_svc):
        """Completing a cancelled booking returns 400 (service rejects)."""
        self._set_user(ADMIN_USER)
        mock_svc.complete_booking.side_effect = ValueError(
            "Only confirmed bookings can be completed."
        )
        resp = self.client.post("/api/v1/bookings/1/complete")
        self.assertEqual(resp.status_code, 400)

    # ── Booking Detail (Get by ID) ───────────────────────────────────

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_user_can_view_own_booking(self, mock_svc):
        """User can retrieve their own booking by ID."""
        self._set_user(REGULAR_USER)
        mock_svc.get_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CONFIRMED",
        }
        resp = self.client.get("/api/v1/bookings/1")
        self.assertEqual(resp.status_code, 200)

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_user_cannot_view_other_users_booking(self, mock_svc):
        """User gets 403 when trying to view another user's booking."""
        self._set_user(REGULAR_USER)
        mock_svc.get_booking.return_value = {
            "id": 1, "user_id": OTHER_USER["id"], "status": "CONFIRMED",
        }
        resp = self.client.get("/api/v1/bookings/1")
        self.assertEqual(resp.status_code, 403)

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_admin_can_view_any_booking(self, mock_svc):
        """Admin can retrieve any user's booking by ID."""
        self._set_user(ADMIN_USER)
        mock_svc.get_booking.return_value = {
            "id": 1, "user_id": REGULAR_USER["id"], "status": "CONFIRMED",
        }
        resp = self.client.get("/api/v1/bookings/1")
        self.assertEqual(resp.status_code, 200)

    # ── Booking List Filtering ────────────────────────────────────────

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_admin_sees_all_bookings(self, mock_svc):
        """Admin user receives all bookings via list_bookings."""
        self._set_user(ADMIN_USER)
        mock_svc.list_bookings.return_value = [
            {"id": 1, "user_id": 2, "status": "CONFIRMED"},
            {"id": 2, "user_id": 3, "status": "CONFIRMED"},
        ]
        resp = self.client.get("/api/v1/bookings")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()["data"]
        self.assertEqual(len(data), 2)
        mock_svc.list_bookings.assert_called_once()
        mock_svc.list_bookings_by_user.assert_not_called()

    @patch("modules.booking.controller.booking_routes.booking_service")
    def test_user_sees_only_own_bookings(self, mock_svc):
        """Regular user receives only their own bookings via list_bookings_by_user."""
        self._set_user(REGULAR_USER)
        mock_svc.list_bookings_by_user.return_value = [
            {"id": 1, "user_id": REGULAR_USER["id"], "status": "CONFIRMED"},
        ]
        resp = self.client.get("/api/v1/bookings")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()["data"]
        self.assertEqual(len(data), 1)
        mock_svc.list_bookings_by_user.assert_called_once_with(REGULAR_USER["id"])
        mock_svc.list_bookings.assert_not_called()


if __name__ == "__main__":
    unittest.main()
