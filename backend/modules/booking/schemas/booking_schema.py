from modules.booking_item.schemas.booking_item_schema import BookingItemInputSchema


class CreateBookingSchema:
    """
    Validates input for booking creation.

    Required fields: user_id, region_id, cart_type_id, timeslot_id, address
    Optional fields: items (list of {item_id, quantity})

    Note: booking_fee and estimated_total are always computed server-side
    and never accepted from the client.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # user_id — required, positive int
        user_id = self._data.get("user_id")
        if user_id is None or not isinstance(user_id, int) or user_id <= 0:
            self.errors.append("'user_id' is required and must be a positive integer.")
        else:
            self.validated_data["user_id"] = user_id

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

        # timeslot_id — required, positive int
        timeslot_id = self._data.get("timeslot_id")
        if timeslot_id is None or not isinstance(timeslot_id, int) or timeslot_id <= 0:
            self.errors.append(
                "'timeslot_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["timeslot_id"] = timeslot_id

        # address — required, non-empty string
        address = self._data.get("address")
        if not address or not isinstance(address, str) or not address.strip():
            self.errors.append("'address' is required and must be a non-empty string.")
        else:
            self.validated_data["address"] = address.strip()

        # items — optional list of {item_id, quantity}
        items = self._data.get("items")
        if items is not None:
            if not isinstance(items, list):
                self.errors.append("'items' must be a list.")
            else:
                validated_items = []
                for idx, entry in enumerate(items):
                    if not isinstance(entry, dict):
                        self.errors.append(f"'items[{idx}]' must be an object.")
                        continue
                    item_schema = BookingItemInputSchema(entry)
                    if not item_schema.is_valid():
                        for err in item_schema.errors:
                            self.errors.append(f"items[{idx}]: {err}")
                    else:
                        validated_items.append(item_schema.validated_data)
                if not self.errors:
                    self.validated_data["items"] = validated_items

        return len(self.errors) == 0


class CancelBookingSchema:
    """
    Validates input for booking cancellation.

    Required fields: booking_id
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        booking_id = self._data.get("booking_id")
        if booking_id is None or not isinstance(booking_id, int) or booking_id <= 0:
            self.errors.append(
                "'booking_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["booking_id"] = booking_id

        return len(self.errors) == 0
