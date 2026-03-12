from core.base.base_service import BaseService
from modules.timeslot.repository.timeslot_repository import timeslot_repository as _default_timeslot_repo
from modules.location.repository.location_repository import location_repository as _default_location_repo


class TimeslotService(BaseService):
    """
    Business logic layer for Timeslot operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to the TimeslotRepository.
    - Cross-validates against the shared LocationRepository.
    - Returns formatted results to the controller.
    """

    def __init__(self, timeslot_repository=None, location_repository=None):
        super().__init__()
        self.timeslot_repository = timeslot_repository or _default_timeslot_repo
        self.location_repository = location_repository or _default_location_repo

    # ── Helpers ───────────────────────────────────────────────────────

    def _validate_location(self, location_id: int) -> None:
        """Ensure the referenced location exists and is serviceable."""
        location = self.location_repository.find_by_id(location_id)
        if not location:
            raise ValueError("Referenced location does not exist.")
        if not location.get("is_serviceable", False):
            raise ValueError("Referenced location is not serviceable.")

    @staticmethod
    def _validate_time_range(start_time: str, end_time: str) -> None:
        """Ensure end_time is strictly after start_time."""
        if end_time <= start_time:
            raise ValueError("'end_time' must be after 'start_time'.")

    # ── CRUD ──────────────────────────────────────────────────────────

    def create_timeslot(self, timeslot_data: dict) -> dict:
        """
        Create a new timeslot after applying business rules.

        Args:
            timeslot_data: Validated timeslot input.

        Returns:
            The created timeslot record as a dict.

        Raises:
            ValueError: If business validation fails.
        """
        # Validate location
        self._validate_location(timeslot_data["location_id"])

        # Validate time range
        self._validate_time_range(timeslot_data["start_time"],
                                  timeslot_data["end_time"])

        # Enforce unique (location_id, date, start_time)
        existing = self.timeslot_repository.find_by_location_date_start(
            timeslot_data["location_id"],
            timeslot_data["date"],
            timeslot_data["start_time"],
        )
        if existing:
            raise ValueError(
                "A timeslot already exists for this location, date, and start time."
            )

        return self.timeslot_repository.create(timeslot_data)

    def get_timeslot(self, timeslot_id: int) -> dict | None:
        """Retrieve a single timeslot by ID."""
        return self.timeslot_repository.find_by_id(timeslot_id)

    def list_timeslots(self) -> list[dict]:
        """Retrieve all timeslots."""
        return self.timeslot_repository.find_all()

    def update_timeslot(self, timeslot_id: int, update_data: dict) -> dict | None:
        """
        Update an existing timeslot.

        Args:
            timeslot_id: Target timeslot ID.
            update_data: Fields to update.

        Returns:
            The updated timeslot record, or None if not found.

        Raises:
            ValueError: If business validation fails.
        """
        existing = self.timeslot_repository.find_by_id(timeslot_id)
        if not existing:
            return None

        # If location_id is being changed, re-validate
        new_location_id = update_data.get("location_id")
        if new_location_id and new_location_id != existing["location_id"]:
            self._validate_location(new_location_id)

        # Resolve effective values for time-range check
        effective_start = update_data.get("start_time", existing["start_time"])
        effective_end = update_data.get("end_time", existing["end_time"])
        if "start_time" in update_data or "end_time" in update_data:
            self._validate_time_range(effective_start, effective_end)

        # Re-check duplicate constraint if key fields change
        eff_location = update_data.get("location_id", existing["location_id"])
        eff_date = update_data.get("date", existing["date"])
        eff_start = update_data.get("start_time", existing["start_time"])

        if (eff_location != existing["location_id"]
                or eff_date != existing["date"]
                or eff_start != existing["start_time"]):
            conflict = self.timeslot_repository.find_by_location_date_start(
                eff_location, eff_date, eff_start,
            )
            if conflict:
                raise ValueError(
                    "A timeslot already exists for this location, date, and start time."
                )

        return self.timeslot_repository.update(timeslot_id, update_data)

    def delete_timeslot(self, timeslot_id: int) -> bool:
        """
        Delete a timeslot by ID.

        Returns:
            True if deleted, False if timeslot was not found.

        Raises:
            ValueError: If the timeslot is still referenced by other records.
        """
        try:
            return self.timeslot_repository.delete(timeslot_id)
        except Exception as e:
            # Check if it's a foreign key violation (psycopg2.errors.ForeignKeyViolation)
            error_str = str(e)
            if "ForeignKeyViolation" in error_str or "violates foreign key constraint" in error_str:
                raise ValueError(
                    "Cannot delete timeslot because it is still referenced by existing bookings. "
                    "Try making it inactive instead."
                )
            raise
