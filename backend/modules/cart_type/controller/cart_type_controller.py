from modules.cart_type.service.cart_type_service import CartTypeService
from modules.cart_type.schemas.cart_type_schema import (
    CreateCartTypeSchema,
    UpdateCartTypeSchema,
)


class CartTypeController:
    """
    REST API handler for CartType endpoints.

    Responsibilities:
    - Receives HTTP requests and extracts input.
    - Validates input via schemas.
    - Delegates to CartTypeService for business logic.
    - Returns standardized API responses.

    Endpoints:
        POST   /api/v1/cart-types      → create_cart_type
        GET    /api/v1/cart-types       → list_cart_types
        GET    /api/v1/cart-types/:id   → get_cart_type
        PUT    /api/v1/cart-types/:id   → update_cart_type
        DELETE /api/v1/cart-types/:id   → delete_cart_type

    Response format:
        { "success": bool, "data": object | null, "message": str }
    """

    def __init__(self):
        self.cart_type_service = CartTypeService()

    def create_cart_type(self, request_data: dict) -> dict:
        """Handle POST /api/v1/cart-types"""
        schema = CreateCartTypeSchema(request_data)
        if not schema.is_valid():
            return self._error_response(schema.errors)

        try:
            cart_type = self.cart_type_service.create_cart_type(schema.validated_data)
            return self._success_response(cart_type, "Cart type created successfully.")
        except ValueError as e:
            return self._error_response(str(e))

    def get_cart_type(self, cart_type_id: int) -> dict:
        """Handle GET /api/v1/cart-types/:id"""
        cart_type = self.cart_type_service.get_cart_type(cart_type_id)
        if not cart_type:
            return self._error_response("Cart type not found.")
        return self._success_response(cart_type)

    def list_cart_types(self) -> dict:
        """Handle GET /api/v1/cart-types"""
        cart_types = self.cart_type_service.list_cart_types()
        return self._success_response(cart_types)

    def update_cart_type(self, cart_type_id: int, request_data: dict) -> dict:
        """Handle PUT /api/v1/cart-types/:id"""
        schema = UpdateCartTypeSchema(request_data)
        if not schema.is_valid():
            return self._error_response(schema.errors)

        try:
            cart_type = self.cart_type_service.update_cart_type(
                cart_type_id, schema.validated_data
            )
            if not cart_type:
                return self._error_response("Cart type not found.")
            return self._success_response(cart_type, "Cart type updated successfully.")
        except ValueError as e:
            return self._error_response(str(e))

    def delete_cart_type(self, cart_type_id: int) -> dict:
        """Handle DELETE /api/v1/cart-types/:id"""
        deleted = self.cart_type_service.delete_cart_type(cart_type_id)
        if not deleted:
            return self._error_response("Cart type not found.")
        return self._success_response(None, "Cart type deleted successfully.")

    # ── Response helpers ──────────────────────────────────────────────

    @staticmethod
    def _success_response(data, message: str = "Success") -> dict:
        return {"success": True, "data": data, "message": message}

    @staticmethod
    def _error_response(message) -> dict:
        return {"success": False, "data": None, "message": message}
