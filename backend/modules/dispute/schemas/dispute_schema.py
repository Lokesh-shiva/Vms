class CreateDisputeSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        title = self._data.get("title", "")
        if not isinstance(title, str) or not title.strip():
            self.errors.append("'title' is required.")
        else:
            self.validated_data["title"] = title.strip()

        description = self._data.get("description", "")
        if not isinstance(description, str) or not description.strip():
            self.errors.append("'description' is required.")
        else:
            self.validated_data["description"] = description.strip()

        for field in ("booking_id", "user_id", "raised_by"):
            if field in self._data and self._data[field] is not None:
                val = self._data[field]
                if not isinstance(val, int) or val <= 0:
                    self.errors.append(f"'{field}' must be a positive integer.")
                else:
                    self.validated_data[field] = val

        return len(self.errors) == 0


class UpdateDisputeSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "status" in self._data:
            self.validated_data["status"] = self._data["status"]

        if "resolution_note" in self._data:
            val = self._data["resolution_note"]
            if val is not None and not isinstance(val, str):
                self.errors.append("'resolution_note' must be a string.")
            else:
                self.validated_data["resolution_note"] = val

        return len(self.errors) == 0
