"""
Domain-named input schemas for the /api/v1/sports routes.

These schemas accept domain field names and pass them through to
CartTypeService unchanged (CartType fields already use domain-neutral
names: name, description, is_active).

The _to_sport() adapter is kept here alongside the schemas so the
mapping logic is co-located with the response shape definition.
"""


def _to_sport(cart_type: dict) -> dict:
    """
    Map a raw cart_type dict (internal naming) to a SportResponse dict.

    CartType fields are already domain-neutral (name, description,
    is_active) so only the field selection changes — no renaming needed.
    """
    return {
        "id": cart_type["id"],
        "name": cart_type.get("name"),
        "description": cart_type.get("description") or "",
        "is_active": cart_type.get("is_active"),
        "created_at": cart_type.get("created_at"),
        "updated_at": cart_type.get("updated_at"),
    }


class CreateSportSchema:
    """
    Validates input for sport (cart_type) creation.

    Required: name
    Optional: description, is_active
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        name = self._data.get("name")
        if not name or not isinstance(name, str) or not name.strip():
            self.errors.append("'name' is required and must be a non-empty string.")
        else:
            self.validated_data["name"] = name.strip()

        description = self._data.get("description", "")
        if not isinstance(description, str):
            self.errors.append("'description' must be a string.")
        else:
            self.validated_data["description"] = description.strip()

        is_active = self._data.get("is_active", True)
        if not isinstance(is_active, bool):
            self.errors.append("'is_active' must be a boolean.")
        else:
            self.validated_data["is_active"] = is_active

        return len(self.errors) == 0


class UpdateSportSchema:
    """
    Validates input for sport (cart_type) updates.

    All fields optional.
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
