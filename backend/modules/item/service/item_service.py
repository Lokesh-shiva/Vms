from core.base.base_service import BaseService
from modules.item.repository.item_repository import item_repository as _default_item_repo
from modules.cart_type.repository.cart_type_repository import cart_type_repository as _default_cart_type_repo


class ItemService(BaseService):
    """
    Business logic layer for Item operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to the ItemRepository.
    - Cross-validates against shared CartTypeRepository.
    - Returns formatted results to the controller.
    """

    def __init__(self, item_repository=None, cart_type_repository=None):
        super().__init__()
        self.item_repository = item_repository or _default_item_repo
        self.cart_type_repository = cart_type_repository or _default_cart_type_repo

    # ── Helpers ───────────────────────────────────────────────────────

    def _validate_cart_type(self, cart_type_id: int) -> None:
        """Ensure the referenced cart type exists."""
        cart_type = self.cart_type_repository.find_by_id(cart_type_id)
        if not cart_type:
            raise ValueError("Referenced cart type does not exist.")

    def _validate_unique_name(self, name: str, cart_type_id: int,
                              exclude_item_id: int | None = None) -> None:
        """Ensure item name is unique within the given cart type."""
        existing = self.item_repository.find_by_name_and_cart_type(name, cart_type_id)
        if existing and existing["id"] != exclude_item_id:
            raise ValueError("An item with this name already exists for this cart type.")

    # ── CRUD ──────────────────────────────────────────────────────────

    def create_item(self, item_data: dict) -> dict:
        """
        Create a new item after applying business rules.

        Args:
            item_data: Validated item input.

        Returns:
            The created item record as a dict.

        Raises:
            ValueError: If business validation fails.
        """
        # Validate cart type exists
        self._validate_cart_type(item_data["cart_type_id"])

        # Enforce unique item name per cart type
        self._validate_unique_name(item_data["name"], item_data["cart_type_id"])

        return self.item_repository.create(item_data)

    def get_item(self, item_id: int) -> dict | None:
        """Retrieve a single item by ID."""
        return self.item_repository.find_by_id(item_id)

    def list_items(self) -> list[dict]:
        """Retrieve all items."""
        return self.item_repository.find_all()

    def list_items_by_cart_type(self, cart_type_id: int) -> list[dict]:
        """Retrieve all items belonging to a specific cart type."""
        return self.item_repository.find_by_cart_type_id(cart_type_id)

    def update_item(self, item_id: int, update_data: dict) -> dict | None:
        """
        Update an existing item.

        Args:
            item_id: Target item ID.
            update_data: Fields to update.

        Returns:
            The updated item record, or None if not found.

        Raises:
            ValueError: If business validation fails.
        """
        existing = self.item_repository.find_by_id(item_id)
        if not existing:
            return None

        # If cart_type_id is being changed, re-validate
        new_cart_type_id = update_data.get("cart_type_id")
        if new_cart_type_id and new_cart_type_id != existing["cart_type_id"]:
            self._validate_cart_type(new_cart_type_id)

        # Determine effective cart_type_id and name for uniqueness check
        effective_cart_type_id = new_cart_type_id or existing["cart_type_id"]
        new_name = update_data.get("name")
        if new_name and (new_name != existing["name"]
                         or effective_cart_type_id != existing["cart_type_id"]):
            self._validate_unique_name(new_name, effective_cart_type_id, exclude_item_id=item_id)

        return self.item_repository.update(item_id, update_data)

    def delete_item(self, item_id: int) -> bool:
        """
        Delete an item by ID.

        Returns:
            True if deleted, False if item was not found.
        """
        return self.item_repository.delete(item_id)
