from modules.user.service.user_service import UserService
from modules.user.schemas.user_schema import CreateUserSchema, UpdateUserSchema


class UserController:
    """
    REST API handler for User endpoints.

    Responsibilities:
    - Receives HTTP requests and extracts input.
    - Validates input via schemas.
    - Delegates to UserService for business logic.
    - Returns standardized API responses.

    Endpoints:
        POST   /api/v1/users      → create_user
        GET    /api/v1/users       → list_users
        GET    /api/v1/users/:id   → get_user
        PUT    /api/v1/users/:id   → update_user
        DELETE /api/v1/users/:id   → delete_user

    Response format:
        { "success": bool, "data": object | null, "message": str }
    """

    def __init__(self):
        self.user_service = UserService()

    def create_user(self, request_data: dict) -> dict:
        """Handle POST /api/v1/users"""
        schema = CreateUserSchema(request_data)
        if not schema.is_valid():
            return self._error_response(schema.errors)

        try:
            user = self.user_service.create_user(schema.validated_data)
            return self._success_response(user, "User created successfully.")
        except ValueError as e:
            return self._error_response(str(e))

    def get_user(self, user_id: int) -> dict:
        """Handle GET /api/v1/users/:id"""
        user = self.user_service.get_user(user_id)
        if not user:
            return self._error_response("User not found.")
        return self._success_response(user)

    def list_users(self) -> dict:
        """Handle GET /api/v1/users"""
        users = self.user_service.list_users()
        return self._success_response(users)

    def update_user(self, user_id: int, request_data: dict) -> dict:
        """Handle PUT /api/v1/users/:id"""
        schema = UpdateUserSchema(request_data)
        if not schema.is_valid():
            return self._error_response(schema.errors)

        try:
            user = self.user_service.update_user(user_id, schema.validated_data)
            if not user:
                return self._error_response("User not found.")
            return self._success_response(user, "User updated successfully.")
        except ValueError as e:
            return self._error_response(str(e))

    def delete_user(self, user_id: int) -> dict:
        """Handle DELETE /api/v1/users/:id"""
        deleted = self.user_service.delete_user(user_id)
        if not deleted:
            return self._error_response("User not found.")
        return self._success_response(None, "User deleted successfully.")

    # ── Response helpers ──────────────────────────────────────────────

    @staticmethod
    def _success_response(data, message: str = "Success") -> dict:
        return {"success": True, "data": data, "message": message}

    @staticmethod
    def _error_response(message) -> dict:
        return {"success": False, "data": None, "message": message}
