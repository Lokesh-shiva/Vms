"""
Domain-named input schemas for the /api/v1/grounds routes.

These schemas accept domain field names (sport_id, location_id, name)
and translate them to the internal names the CartService expects
(cart_type_id, region_id, label).

No business logic lives here — validation mirrors CreateCartSchema /
UpdateCartSchema exactly, only the field names differ.
"""


def _to_ground(cart: dict) -> dict:
    """
    Map a raw cart dict (internal naming) to a GroundResponse dict.

    Internal → Domain:
        label        → name
        cart_type_id → sport_id
        region_id    → location_id
    """
    return {
        "id": cart["id"],
        "name": cart.get("label") or "",
        "sport_id": cart.get("cart_type_id"),
        "location_id": cart.get("region_id"),
        "status": cart.get("status"),
        "is_active": cart.get("is_active"),
        "latitude": cart.get("latitude"),
        "longitude": cart.get("longitude"),
        "created_at": cart.get("created_at"),
        "updated_at": cart.get("updated_at"),
    }


class CreateGroundSchema:
    """
    Validates input for ground (cart) creation using domain field names.

    Required: location_id, sport_id
    Optional: name, is_active, latitude, longitude
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}   # translated to internal names

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # name → label (optional)
        if "name" in self._data:
            name = self._data["name"]
            if not isinstance(name, str) or not name.strip():
                self.errors.append("'name' must be a non-empty string when provided.")
            else:
                self.validated_data["label"] = name.strip()

        # location_id → region_id (required)
        location_id = self._data.get("location_id")
        if location_id is None or not isinstance(location_id, int) or location_id <= 0:
            self.errors.append("'location_id' is required and must be a positive integer.")
        else:
            self.validated_data["region_id"] = location_id

        # sport_id → cart_type_id (required)
        sport_id = self._data.get("sport_id")
        if sport_id is None or not isinstance(sport_id, int) or sport_id <= 0:
            self.errors.append("'sport_id' is required and must be a positive integer.")
        else:
            self.validated_data["cart_type_id"] = sport_id

        # status is system-controlled — reject if supplied
        if "status" in self._data:
            self.errors.append(
                "'status' cannot be set manually. Ground status is managed by the system."
            )

        # is_active (optional)
        is_active = self._data.get("is_active", True)
        if not isinstance(is_active, bool):
            self.errors.append("'is_active' must be a boolean.")
        else:
            self.validated_data["is_active"] = is_active

        # latitude / longitude (optional)
        for coord in ("latitude", "longitude"):
            if coord in self._data:
                val = self._data[coord]
                if not isinstance(val, (int, float)):
                    self.errors.append(f"'{coord}' must be a number when provided.")
                else:
                    self.validated_data[coord] = val

        return len(self.errors) == 0


class UpdateGroundSchema:
    """
    Validates input for ground (cart) updates using domain field names.

    All fields optional. Only provided fields are validated.
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
                self.validated_data["label"] = name.strip()

        if "location_id" in self._data:
            location_id = self._data["location_id"]
            if not isinstance(location_id, int) or location_id <= 0:
                self.errors.append("'location_id' must be a positive integer.")
            else:
                self.validated_data["region_id"] = location_id

        if "sport_id" in self._data:
            sport_id = self._data["sport_id"]
            if not isinstance(sport_id, int) or sport_id <= 0:
                self.errors.append("'sport_id' must be a positive integer.")
            else:
                self.validated_data["cart_type_id"] = sport_id

        if "status" in self._data:
            self.errors.append(
                "'status' cannot be set manually. Ground status is managed by the system."
            )

        if "is_active" in self._data:
            is_active = self._data["is_active"]
            if not isinstance(is_active, bool):
                self.errors.append("'is_active' must be a boolean.")
            else:
                self.validated_data["is_active"] = is_active

        for coord in ("latitude", "longitude"):
            if coord in self._data:
                val = self._data[coord]
                if not isinstance(val, (int, float)):
                    self.errors.append(f"'{coord}' must be a number when provided.")
                else:
                    self.validated_data[coord] = val

        return len(self.errors) == 0
