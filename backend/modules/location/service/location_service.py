from core.base.base_service import BaseService
from modules.location.repository.location_repository import (
    location_repository as _default_repo,
)


class LocationService(BaseService):
    """
    Business logic layer for Location operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to the LocationRepository.
    - Returns formatted results to the controller.
    """

    def __init__(self, location_repository=None):
        super().__init__()
        self.location_repository = location_repository or _default_repo

    def create_location(self, location_data: dict) -> dict:
        """
        Create a new location after applying business rules.

        Args:
            location_data: Validated location input.

        Returns:
            The created location record as a dict.

        Raises:
            ValueError: If business validation fails.
        """
        if not location_data.get("name"):
            raise ValueError("Location name is required.")

        # Enforce unique location name
        existing = self.location_repository.find_by_name(location_data["name"])
        if existing:
            raise ValueError("A location with this name already exists.")

        return self.location_repository.create(location_data)

    def get_location(self, location_id: int) -> dict | None:
        """Retrieve a single location by ID."""
        return self.location_repository.find_by_id(location_id)

    def list_locations(self) -> list[dict]:
        """Retrieve all locations."""
        return self.location_repository.find_all()

    def find_nearest_locations(self, lat: float, lng: float, limit: int = 5) -> list[dict]:
        """Return serviceable locations nearest to (lat, lng), closest first.

        Raises:
            ValueError: if lat/lng are out of valid range.
        """
        if not (-90 <= lat <= 90) or not (-180 <= lng <= 180):
            raise ValueError("lat must be in [-90, 90] and lng must be in [-180, 180].")
        return self.location_repository.find_nearest(lat, lng, limit)

    def update_location(self, location_id: int, update_data: dict) -> dict | None:
        """
        Update an existing location.

        Args:
            location_id: Target location ID.
            update_data: Fields to update.

        Returns:
            The updated location record, or None if not found.

        Raises:
            ValueError: If name uniqueness is violated.
        """
        existing = self.location_repository.find_by_id(location_id)
        if not existing:
            return None

        # If name is being changed, enforce uniqueness
        new_name = update_data.get("name")
        if new_name and new_name != existing["name"]:
            conflict = self.location_repository.find_by_name(new_name)
            if conflict:
                raise ValueError("A location with this name already exists.")

        return self.location_repository.update(location_id, update_data)

    def delete_location(self, location_id: int) -> bool:
        """
        Delete a location by ID.

        Returns:
            True if deleted, False if location was not found.
        """
        return self.location_repository.delete(location_id)
