class BookingItemInputSchema:
    """
    Validates a single booking item entry from client input.

    Required fields: item_id (positive int), quantity (positive int).
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # item_id — required, positive int
        item_id = self._data.get("item_id")
        if item_id is None or not isinstance(item_id, int) or item_id <= 0:
            self.errors.append(
                "'item_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["item_id"] = item_id

        # quantity — required, positive int
        quantity = self._data.get("quantity")
        if quantity is None or not isinstance(quantity, int) or quantity <= 0:
            self.errors.append(
                "'quantity' is required and must be a positive integer."
            )
        else:
            self.validated_data["quantity"] = quantity

        return len(self.errors) == 0
