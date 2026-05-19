import re


# ── Helpers ───────────────────────────────────────────────────────────

_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
_TIME_RE = re.compile(r"^\d{2}:\d{2}$")


class CreateTimeslotSchema:
    """
    Validates input for timeslot creation.

    Required fields: location_id, date, start_time, end_time, capacity
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # location_id — required, positive int
        location_id = self._data.get("location_id")
        if location_id is None or not isinstance(location_id, int) or location_id <= 0:
            self.errors.append(
                "'location_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["location_id"] = location_id

        # date — required, YYYY-MM-DD
        date = self._data.get("date")
        if not date or not isinstance(date, str) or not _DATE_RE.match(date.strip()):
            self.errors.append("'date' is required and must be in YYYY-MM-DD format.")
        else:
            self.validated_data["date"] = date.strip()

        # start_time — required, HH:MM
        start_time = self._data.get("start_time")
        if (
            not start_time
            or not isinstance(start_time, str)
            or not _TIME_RE.match(start_time.strip())
        ):
            self.errors.append("'start_time' is required and must be in HH:MM format.")
        else:
            self.validated_data["start_time"] = start_time.strip()

        # end_time — required, HH:MM
        end_time = self._data.get("end_time")
        if (
            not end_time
            or not isinstance(end_time, str)
            or not _TIME_RE.match(end_time.strip())
        ):
            self.errors.append("'end_time' is required and must be in HH:MM format.")
        else:
            self.validated_data["end_time"] = end_time.strip()

        # capacity — required, positive int
        capacity = self._data.get("capacity")
        if capacity is None or not isinstance(capacity, int) or capacity <= 0:
            self.errors.append("'capacity' is required and must be a positive integer.")
        else:
            self.validated_data["capacity"] = capacity

        return len(self.errors) == 0


class UpdateTimeslotSchema:
    """
    Validates input for timeslot updates.

    All fields are optional. Only provided fields are validated and returned.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "location_id" in self._data:
            location_id = self._data["location_id"]
            if not isinstance(location_id, int) or location_id <= 0:
                self.errors.append("'location_id' must be a positive integer.")
            else:
                self.validated_data["location_id"] = location_id

        if "date" in self._data:
            date = self._data["date"]
            if not isinstance(date, str) or not _DATE_RE.match(date.strip()):
                self.errors.append("'date' must be in YYYY-MM-DD format.")
            else:
                self.validated_data["date"] = date.strip()

        if "start_time" in self._data:
            start_time = self._data["start_time"]
            if not isinstance(start_time, str) or not _TIME_RE.match(
                start_time.strip()
            ):
                self.errors.append("'start_time' must be in HH:MM format.")
            else:
                self.validated_data["start_time"] = start_time.strip()

        if "end_time" in self._data:
            end_time = self._data["end_time"]
            if not isinstance(end_time, str) or not _TIME_RE.match(end_time.strip()):
                self.errors.append("'end_time' must be in HH:MM format.")
            else:
                self.validated_data["end_time"] = end_time.strip()

        if "capacity" in self._data:
            capacity = self._data["capacity"]
            if not isinstance(capacity, int) or capacity <= 0:
                self.errors.append("'capacity' must be a positive integer.")
            else:
                self.validated_data["capacity"] = capacity

        return len(self.errors) == 0
