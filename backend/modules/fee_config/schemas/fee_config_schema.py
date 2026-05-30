class CreateFeeConfigSchema:
    """
    Validates input for fee config creation.

    Required fields: region_id, cart_type_id, booking_fee,
                     cancellation_fee_pct, platform_fee_pct

    Structural / type validation only.  Business rules (FK existence,
    pct sum <= 100) live in the service layer.
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
            self.errors.append(
                "'region_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["region_id"] = region_id

        # cart_type_id — required, positive int
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

        # booking_fee — required, number >= 0
        booking_fee = self._data.get("booking_fee")
        if booking_fee is None or not isinstance(booking_fee, (int, float)):
            self.errors.append("'booking_fee' is required and must be a number.")
        else:
            self.validated_data["booking_fee"] = float(booking_fee)

        # cancellation_fee_pct — required, number
        cancellation_fee_pct = self._data.get("cancellation_fee_pct")
        if cancellation_fee_pct is None or not isinstance(
            cancellation_fee_pct, (int, float)
        ):
            self.errors.append(
                "'cancellation_fee_pct' is required and must be a number."
            )
        else:
            self.validated_data["cancellation_fee_pct"] = float(cancellation_fee_pct)

        # platform_fee_pct — required, number
        platform_fee_pct = self._data.get("platform_fee_pct")
        if platform_fee_pct is None or not isinstance(platform_fee_pct, (int, float)):
            self.errors.append("'platform_fee_pct' is required and must be a number.")
        else:
            self.validated_data["platform_fee_pct"] = float(platform_fee_pct)

        # ── Time-based billing fields (all optional, defaulted in DB) ──
        for num_field in ("matching_fee", "rate_per_block"):
            if num_field in self._data:
                val = self._data[num_field]
                if not isinstance(val, (int, float)) or isinstance(val, bool) or val < 0:
                    self.errors.append(f"'{num_field}' must be a non-negative number.")
                else:
                    self.validated_data[num_field] = val

        for int_field in ("block_duration_minutes", "max_duration_minutes"):
            if int_field in self._data:
                val = self._data[int_field]
                if not isinstance(val, int) or isinstance(val, bool) or val <= 0:
                    self.errors.append(f"'{int_field}' must be a positive integer.")
                else:
                    self.validated_data[int_field] = val

        if "surge_enabled" in self._data:
            val = self._data["surge_enabled"]
            if not isinstance(val, bool):
                self.errors.append("'surge_enabled' must be a boolean.")
            else:
                self.validated_data["surge_enabled"] = val

        if "surge_multiplier" in self._data:
            val = self._data["surge_multiplier"]
            if not isinstance(val, (int, float)) or isinstance(val, bool) or val < 1.0 or val > 3.0:
                self.errors.append("'surge_multiplier' must be between 1.0 and 3.0.")
            else:
                self.validated_data["surge_multiplier"] = val

        return len(self.errors) == 0


class UpdateFeeConfigSchema:
    """
    Validates input for fee config update.

    All fields are optional.  At least one must be provided.
    Structural / type validation only.
    """

    ALLOWED_FIELDS = {
        "booking_fee",
        "cancellation_fee_pct",
        "platform_fee_pct",
        "is_active",
    }

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # booking_fee — optional, number
        booking_fee = self._data.get("booking_fee")
        if booking_fee is not None:
            if not isinstance(booking_fee, (int, float)):
                self.errors.append("'booking_fee' must be a number.")
            else:
                self.validated_data["booking_fee"] = float(booking_fee)

        # cancellation_fee_pct — optional, number
        cancellation_fee_pct = self._data.get("cancellation_fee_pct")
        if cancellation_fee_pct is not None:
            if not isinstance(cancellation_fee_pct, (int, float)):
                self.errors.append("'cancellation_fee_pct' must be a number.")
            else:
                self.validated_data["cancellation_fee_pct"] = float(
                    cancellation_fee_pct
                )

        # platform_fee_pct — optional, number
        platform_fee_pct = self._data.get("platform_fee_pct")
        if platform_fee_pct is not None:
            if not isinstance(platform_fee_pct, (int, float)):
                self.errors.append("'platform_fee_pct' must be a number.")
            else:
                self.validated_data["platform_fee_pct"] = float(platform_fee_pct)

        # is_active — optional, bool
        is_active = self._data.get("is_active")
        if is_active is not None:
            if not isinstance(is_active, bool):
                self.errors.append("'is_active' must be a boolean.")
            else:
                self.validated_data["is_active"] = is_active

        # ── Time-based billing fields (all optional, defaulted in DB) ──
        for num_field in ("matching_fee", "rate_per_block"):
            if num_field in self._data:
                val = self._data[num_field]
                if not isinstance(val, (int, float)) or isinstance(val, bool) or val < 0:
                    self.errors.append(f"'{num_field}' must be a non-negative number.")
                else:
                    self.validated_data[num_field] = val

        for int_field in ("block_duration_minutes", "max_duration_minutes"):
            if int_field in self._data:
                val = self._data[int_field]
                if not isinstance(val, int) or isinstance(val, bool) or val <= 0:
                    self.errors.append(f"'{int_field}' must be a positive integer.")
                else:
                    self.validated_data[int_field] = val

        if "surge_enabled" in self._data:
            val = self._data["surge_enabled"]
            if not isinstance(val, bool):
                self.errors.append("'surge_enabled' must be a boolean.")
            else:
                self.validated_data["surge_enabled"] = val

        if "surge_multiplier" in self._data:
            val = self._data["surge_multiplier"]
            if not isinstance(val, (int, float)) or isinstance(val, bool) or val < 1.0 or val > 3.0:
                self.errors.append("'surge_multiplier' must be between 1.0 and 3.0.")
            else:
                self.validated_data["surge_multiplier"] = val

        if not self.validated_data and not self.errors:
            self.errors.append("At least one field must be provided for update.")

        return len(self.errors) == 0
