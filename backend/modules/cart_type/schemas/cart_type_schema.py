class CreateCartTypeSchema:
    """
    Validates input for cart type creation.

    Required fields: name
    Optional fields: description, is_active
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

        # Description — optional, defaults to ""
        description = self._data.get("description", "")
        if not isinstance(description, str):
            self.errors.append("'description' must be a string.")
        else:
            self.validated_data["description"] = description.strip()

        # is_active — optional, defaults to True
        is_active = self._data.get("is_active", True)
        if not isinstance(is_active, bool):
            self.errors.append("'is_active' must be a boolean.")
        else:
            self.validated_data["is_active"] = is_active

        return len(self.errors) == 0


class UpdateCartTypeSchema:
    """
    Validates input for cart type updates.

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

        if "description" in self._data:
            description = self._data["description"]
            if not isinstance(description, str):
                self.errors.append("'description' must be a string.")
            else:
                self.validated_data["description"] = description.strip()

        if "is_active" in self._data:
            is_active = self._data["is_active"]
            if not isinstance(is_active, bool):
                self.errors.append("'is_active' must be a boolean.")
            else:
                self.validated_data["is_active"] = is_active

        return len(self.errors) == 0
