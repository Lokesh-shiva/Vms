from modules.location.service.location_service import LocationService
from modules.location.schemas.location_schema import (
    CreateLocationSchema,
    UpdateLocationSchema,
)


class LocationController:
    """
    REST API handler for Location endpoints.

    Responsibilities:
    - Receives HTTP requests and extracts input.
    - Validates input via schemas.
    - Delegates to LocationService for business logic.
    - Returns standardized API responses.

    Endpoints:
        POST   /api/v1/locations      → create_location
        GET    /api/v1/locations       → list_locations
        GET    /api/v1/locations/:id   → get_location
        PUT    /api/v1/locations/:id   → update_location
        DELETE /api/v1/locations/:id   → delete_location

    Response format:
        { "success": bool, "data": object | null, "message": str }
    """

    def __init__(self):
        self.location_service = LocationService()

    def create_location(self, request_data: dict) -> dict:
        """Handle POST /api/v1/locations"""
        schema = CreateLocationSchema(request_data)
        if not schema.is_valid():
            return self._error_response(schema.errors)

        try:
            location = self.location_service.create_location(schema.validated_data)
            return self._success_response(location, "Location created successfully.")
        except ValueError as e:
            return self._error_response(str(e))

    def get_location(self, location_id: int) -> dict:
        """Handle GET /api/v1/locations/:id"""
        location = self.location_service.get_location(location_id)
        if not location:
            return self._error_response("Location not found.")
        return self._success_response(location)

    def list_locations(self) -> dict:
        """Handle GET /api/v1/locations"""
        locations = self.location_service.list_locations()
        return self._success_response(locations)

    def update_location(self, location_id: int, request_data: dict) -> dict:
        """Handle PUT /api/v1/locations/:id"""
        schema = UpdateLocationSchema(request_data)
        if not schema.is_valid():
            return self._error_response(schema.errors)

        try:
            location = self.location_service.update_location(
                location_id, schema.validated_data
            )
            if not location:
                return self._error_response("Location not found.")
            return self._success_response(location, "Location updated successfully.")
        except ValueError as e:
            return self._error_response(str(e))

    def delete_location(self, location_id: int) -> dict:
        """Handle DELETE /api/v1/locations/:id"""
        deleted = self.location_service.delete_location(location_id)
        if not deleted:
            return self._error_response("Location not found.")
        return self._success_response(None, "Location deleted successfully.")

    # ── Response helpers ──────────────────────────────────────────────

    @staticmethod
    def _success_response(data, message: str = "Success") -> dict:
        return {"success": True, "data": data, "message": message}

    @staticmethod
    def _error_response(message) -> dict:
        return {"success": False, "data": None, "message": message}
