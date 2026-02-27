class CreateLocationSchema:
    """
    Validates input for location creation.

    Required fields: name
    Optional fields: is_serviceable
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # Name — required, non-empty string
        name = self._data.get("name")
        if not name or not isinstance(name, str) or not name.strip():
            self.errors.append("'name' is required and must be a non-empty string.")
        else:
            self.validated_data["name"] = name.strip()

        # is_serviceable — optional, defaults to True
        is_serviceable = self._data.get("is_serviceable", True)
        if not isinstance(is_serviceable, bool):
            self.errors.append("'is_serviceable' must be a boolean.")
        else:
            self.validated_data["is_serviceable"] = is_serviceable

        return len(self.errors) == 0


class UpdateLocationSchema:
    """
    Validates input for location updates.

    All fields are optional. Only provided fields are validated and returned.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "name" in self._data:
            name = self._data["name"]
            if not isinstance(name, str) or not name.strip():
                self.errors.append("'name' must be a non-empty string.")
            else:
                self.validated_data["name"] = name.strip()

        if "is_serviceable" in self._data:
            is_serviceable = self._data["is_serviceable"]
            if not isinstance(is_serviceable, bool):
                self.errors.append("'is_serviceable' must be a boolean.")
            else:
                self.validated_data["is_serviceable"] = is_serviceable

        return len(self.errors) == 0
