VALID_STATUSES = {"AVAILABLE", "BUSY", "BUFFER", "OFFLINE"}


class CreateCartSchema:
    """
    Validates input for cart creation.

    Required fields: region_id, cart_type_id, status
    Optional fields: is_active
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # region_id — required, positive int
        region_id = self._data.get("region_id")
        if region_id is None or not isinstance(region_id, int) or region_id <= 0:
            self.errors.append("'region_id' is required and must be a positive integer.")
        else:
            self.validated_data["region_id"] = region_id

        # cart_type_id — required, positive int
        cart_type_id = self._data.get("cart_type_id")
        if cart_type_id is None or not isinstance(cart_type_id, int) or cart_type_id <= 0:
            self.errors.append("'cart_type_id' is required and must be a positive integer.")
        else:
            self.validated_data["cart_type_id"] = cart_type_id

        # status — required, must be a valid status
        status = self._data.get("status")
        if not status or not isinstance(status, str):
            self.errors.append(
                f"'status' is required and must be one of: {', '.join(sorted(VALID_STATUSES))}."
            )
        else:
            normalized = status.strip().upper()
            if normalized not in VALID_STATUSES:
                self.errors.append(
                    f"'status' must be one of: {', '.join(sorted(VALID_STATUSES))}."
                )
            else:
                self.validated_data["status"] = normalized

        # is_active — optional, defaults to True
        is_active = self._data.get("is_active", True)
        if not isinstance(is_active, bool):
            self.errors.append("'is_active' must be a boolean.")
        else:
            self.validated_data["is_active"] = is_active

        return len(self.errors) == 0


class UpdateCartSchema:
    """
    Validates input for cart updates.

    All fields are optional. Only provided fields are validated and returned.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "region_id" in self._data:
            region_id = self._data["region_id"]
            if not isinstance(region_id, int) or region_id <= 0:
                self.errors.append("'region_id' must be a positive integer.")
            else:
                self.validated_data["region_id"] = region_id

        if "cart_type_id" in self._data:
            cart_type_id = self._data["cart_type_id"]
            if not isinstance(cart_type_id, int) or cart_type_id <= 0:
                self.errors.append("'cart_type_id' must be a positive integer.")
            else:
                self.validated_data["cart_type_id"] = cart_type_id

        if "status" in self._data:
            status = self._data["status"]
            if not isinstance(status, str):
                self.errors.append(
                    f"'status' must be one of: {', '.join(sorted(VALID_STATUSES))}."
                )
            else:
                normalized = status.strip().upper()
                if normalized not in VALID_STATUSES:
                    self.errors.append(
                        f"'status' must be one of: {', '.join(sorted(VALID_STATUSES))}."
                    )
                else:
                    self.validated_data["status"] = normalized

        if "is_active" in self._data:
            is_active = self._data["is_active"]
            if not isinstance(is_active, bool):
                self.errors.append("'is_active' must be a boolean.")
            else:
                self.validated_data["is_active"] = is_active

        return len(self.errors) == 0
