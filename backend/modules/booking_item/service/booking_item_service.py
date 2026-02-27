from decimal import Decimal

from core.base.base_service import BaseService
from modules.booking_item.repository.booking_item_repository import (
    booking_item_repository as _default_booking_item_repo,
)
from modules.item.repository.item_repository import (
    item_repository as _default_item_repo,
)


class BookingItemService(BaseService):
    """
    Business logic layer for BookingItem operations.

    Responsibilities:
    - Validates item existence, cart_type ownership, availability, and quantity.
    - Snapshots authoritative unit_price from ItemRepository.
    - Calculates estimated_total from validated snapshots.
    - Persists BookingItem records only after the parent booking exists.
    """

    def __init__(self, booking_item_repository=None, item_repository=None):
        super().__init__()
        self.booking_item_repository = booking_item_repository or _default_booking_item_repo
        self.item_repository = item_repository or _default_item_repo

    # ── Validation (no persistence) ──────────────────────────────────

    def validate_items(self, cart_type_id: int,
                       items_input: list[dict]) -> list[dict]:
        """
        Validate each item entry and return sanitized snapshots.

        For each item entry:
        1. quantity must be > 0
        2. Item must exist in the repository
        3. Item's cart_type_id must match the booking's cart_type_id
        4. Item must be available (is_available == True)

        Args:
            cart_type_id: The booking's cart type ID.
            items_input: List of dicts with 'item_id' and 'quantity'.

        Returns:
            List of validated snapshots:
            [{"item_id": int, "quantity": int, "unit_price": Decimal}, ...]

        Raises:
            ValueError: If any validation fails.
        """
        snapshots = []

        for entry in items_input:
            quantity = entry.get("quantity", 0)
            item_id = entry.get("item_id")

            # 1. Validate quantity
            if not isinstance(quantity, int) or quantity <= 0:
                raise ValueError(
                    f"Quantity must be a positive integer (got {quantity})."
                )

            # 2. Validate item exists
            item = self.item_repository.find_by_id(item_id)
            if not item:
                raise ValueError(
                    f"Item with id {item_id} does not exist."
                )

            # 3. Validate item belongs to the booking's cart_type
            if item["cart_type_id"] != cart_type_id:
                raise ValueError(
                    f"Item {item_id} does not belong to cart type {cart_type_id}."
                )

            # 4. Validate item is available (authoritative state from repo)
            if not item.get("is_available", False):
                raise ValueError(
                    f"Item {item_id} is not currently available."
                )

            # Snapshot the authoritative price
            snapshots.append({
                "item_id": item_id,
                "quantity": quantity,
                "unit_price": Decimal(str(item["price"])),
            })

        return snapshots

    # ── Calculation ──────────────────────────────────────────────────

    @staticmethod
    def calculate_estimated_total(validated_snapshots: list[dict]) -> Decimal:
        """
        Compute estimated total from validated item snapshots.

        Args:
            validated_snapshots: Output of validate_items().

        Returns:
            Sum of (quantity * unit_price) across all snapshots.
        """
        return sum(
            Decimal(str(s["quantity"])) * s["unit_price"]
            for s in validated_snapshots
        )

    # ── Persistence (after booking exists) ───────────────────────────

    def create_booking_items(
        self, booking_id: int, validated_snapshots: list[dict], session=None
    ) -> list[dict]:
        """
        Persist BookingItem records for an already-created booking.

        Args:
            booking_id: The parent booking's ID.
            validated_snapshots: Output of validate_items().

        Returns:
            List of created booking item records.
        """
        created = []
        for snapshot in validated_snapshots:
            record = self.booking_item_repository.create({
                "booking_id": booking_id,
                "item_id": snapshot["item_id"],
                "quantity": snapshot["quantity"],
                "unit_price": snapshot["unit_price"],
            }, session=session)
            created.append(record)
        return created
