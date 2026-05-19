import re


def _normalize_phone(phone: str) -> str:
    """Strip all whitespace from a phone string for canonical comparison."""
    return phone.strip().replace(" ", "")


class RegisterSchema:
    """
    Validates input for user registration.

    Required fields: name, phone, password
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

        phone = self._data.get("phone")
        if not phone or not isinstance(phone, str):
            self.errors.append("'phone' is required and must be a string.")
        else:
            normalized = _normalize_phone(phone)
            if not re.match(r"^\+?[\d\-]{7,15}$", normalized):
                self.errors.append(
                    "'phone' must be a valid phone number (7-15 digits)."
                )
            else:
                self.validated_data["phone"] = normalized

        password = self._data.get("password")
        if not password or not isinstance(password, str):
            self.errors.append("'password' is required and must be a string.")
        elif len(password) < 6:
            self.errors.append("'password' must be at least 6 characters.")
        else:
            self.validated_data["password"] = password

        return len(self.errors) == 0


class LoginSchema:
    """
    Validates input for user login.

    Required fields: phone, password
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        phone = self._data.get("phone")
        if not phone or not isinstance(phone, str):
            self.errors.append("'phone' is required and must be a string.")
        else:
            self.validated_data["phone"] = _normalize_phone(phone)

        password = self._data.get("password")
        if not password or not isinstance(password, str):
            self.errors.append("'password' is required and must be a string.")
        else:
            self.validated_data["password"] = password

        return len(self.errors) == 0
