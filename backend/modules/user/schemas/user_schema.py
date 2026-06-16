import re

from modules.user.model.user_model import UserRole


# Derive allowed roles directly from the enum — single source of truth.
VALID_ROLES = {role.value for role in UserRole}


class CreateUserSchema:
    """
    Validates input for user creation.

    Required fields: name, phone
    Optional fields: role, is_active
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

        # Phone — required, basic format check
        phone = self._data.get("phone")
        if not phone or not isinstance(phone, str):
            self.errors.append("'phone' is required and must be a string.")
        elif not re.match(r"^\+?[\d\s\-]{7,15}$", phone.strip()):
            self.errors.append("'phone' must be a valid phone number (7-15 digits).")
        else:
            self.validated_data["phone"] = phone.strip()

        # Role — optional, normalized to lowercase before validation
        role = self._data.get("role", UserRole.USER.value)
        if isinstance(role, str):
            role = role.strip().lower()
        if role not in VALID_ROLES:
            self.errors.append(f"'role' must be one of {sorted(VALID_ROLES)}.")
        else:
            self.validated_data["role"] = role

        # is_active — optional, defaults to True
        is_active = self._data.get("is_active", True)
        if not isinstance(is_active, bool):
            self.errors.append("'is_active' must be a boolean.")
        else:
            self.validated_data["is_active"] = is_active

        return len(self.errors) == 0


class UpdateUserSchema:
    """
    Validates input for user updates.

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

        if "phone" in self._data:
            phone = self._data["phone"]
            if not isinstance(phone, str):
                self.errors.append("'phone' must be a string.")
            elif not re.match(r"^\+?[\d\s\-]{7,15}$", phone.strip()):
                self.errors.append(
                    "'phone' must be a valid phone number (7-15 digits)."
                )
            else:
                self.validated_data["phone"] = phone.strip()

        if "role" in self._data:
            role = self._data["role"]
            if isinstance(role, str):
                role = role.strip().lower()
            if role not in VALID_ROLES:
                self.errors.append(f"'role' must be one of {sorted(VALID_ROLES)}.")
            else:
                self.validated_data["role"] = role

        if "is_active" in self._data:
            is_active = self._data["is_active"]
            if not isinstance(is_active, bool):
                self.errors.append("'is_active' must be a boolean.")
            else:
                self.validated_data["is_active"] = is_active

        if "region_id" in self._data:
            region_id = self._data["region_id"]
            if region_id is not None and not isinstance(region_id, int):
                self.errors.append("'region_id' must be an integer or null.")
            else:
                self.validated_data["region_id"] = region_id

        if "city" in self._data:
            city = self._data["city"]
            if city is not None and not isinstance(city, str):
                self.errors.append("'city' must be a string or null.")
            else:
                self.validated_data["city"] = city.strip() if isinstance(city, str) else city

        if "can_create_society" in self._data:
            val = self._data["can_create_society"]
            if not isinstance(val, bool):
                self.errors.append("'can_create_society' must be a boolean.")
            else:
                self.validated_data["can_create_society"] = val

        return len(self.errors) == 0
