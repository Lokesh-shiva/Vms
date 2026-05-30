class CreateCartSchema:
    """
    Validates input for cart creation.

    Required fields: region_id, cart_type_id
    Optional fields: label, is_active

    Note: 'status' is intentionally excluded.  Cart status is system-
    controlled and must only be set by booking lifecycle operations.
    New carts always start as AVAILABLE (model default).
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "label" in self._data:
            label = self._data.get("label")
            if not isinstance(label, str) or not label.strip():
                self.errors.append("'label' must be a non-empty string when provided.")
            else:
                self.validated_data["label"] = label.strip()

        region_id = self._data.get("region_id")
        if region_id is None or not isinstance(region_id, int) or region_id <= 0:
            self.errors.append(
                "'region_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["region_id"] = region_id

        cart_type_id = self._data.get("cart_type_id")
        if (
            cart_type_id is None
            or not isinstance(cart_type_id, int)
            or cart_type_id <= 0
        ):
            self.errors.append(
                "'cart_type_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["cart_type_id"] = cart_type_id

        if "status" in self._data:
            self.errors.append(
                "'status' cannot be set manually. Cart status is managed by the system."
            )

        is_active = self._data.get("is_active", True)
        if not isinstance(is_active, bool):
            self.errors.append("'is_active' must be a boolean.")
        else:
            self.validated_data["is_active"] = is_active

        return len(self.errors) == 0


class UpdateCartSchema:
    """
    Validates input for cart updates.

    Allowed optional fields: label, region_id, cart_type_id, is_active

    Note: 'status' is intentionally excluded.  Cart status is system-
    controlled and must only be changed by booking lifecycle operations.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "label" in self._data:
            label = self._data["label"]
            if not isinstance(label, str) or not label.strip():
                self.errors.append("'label' must be a non-empty string.")
            else:
                self.validated_data["label"] = label.strip()

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
            self.errors.append(
                "'status' cannot be set manually. Cart status is managed by the system."
            )

        if "is_active" in self._data:
            is_active = self._data["is_active"]
            if not isinstance(is_active, bool):
                self.errors.append("'is_active' must be a boolean.")
            else:
                self.validated_data["is_active"] = is_active

        return len(self.errors) == 0
