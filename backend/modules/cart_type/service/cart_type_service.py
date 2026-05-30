from core.base.base_service import BaseService
from modules.cart_type.repository.cart_type_repository import (
    cart_type_repository as _default_repo,
)


class CartTypeService(BaseService):
    """
    Business logic layer for CartType operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to the CartTypeRepository.
    - Returns formatted results to the controller.
    """

    def __init__(self, cart_type_repository=None):
        super().__init__()
        self.cart_type_repository = cart_type_repository or _default_repo

    def create_cart_type(self, cart_type_data: dict) -> dict:
        """
        Create a new cart type after applying business rules.

        Args:
            cart_type_data: Validated cart type input.

        Returns:
            The created cart type record as a dict.

        Raises:
            ValueError: If business validation fails.
        """
        if not cart_type_data.get("name"):
            raise ValueError("Cart type name is required.")

        # Enforce unique cart type name
        existing = self.cart_type_repository.find_by_name(cart_type_data["name"])
        if existing:
            raise ValueError("A cart type with this name already exists.")

        return self.cart_type_repository.create(cart_type_data)

    def get_cart_type(self, cart_type_id: int) -> dict | None:
        """Retrieve a single cart type by ID."""
        return self.cart_type_repository.find_by_id(cart_type_id)

    def list_cart_types(self) -> list[dict]:
        """Retrieve all cart types."""
        return self.cart_type_repository.find_all()

    def update_cart_type(self, cart_type_id: int, update_data: dict) -> dict | None:
        """
        Update an existing cart type.

        Args:
            cart_type_id: Target cart type ID.
            update_data: Fields to update.

        Returns:
            The updated cart type record, or None if not found.

        Raises:
            ValueError: If name uniqueness is violated.
        """
        existing = self.cart_type_repository.find_by_id(cart_type_id)
        if not existing:
            return None

        # If name is being changed, enforce uniqueness
        new_name = update_data.get("name")
        if new_name and new_name != existing["name"]:
            conflict = self.cart_type_repository.find_by_name(new_name)
            if conflict:
                raise ValueError("A cart type with this name already exists.")

        return self.cart_type_repository.update(cart_type_id, update_data)

    def delete_cart_type(self, cart_type_id: int) -> bool:
        """
        Delete a cart type by ID.

        Returns:
            True if deleted, False if cart type was not found.
        """
        return self.cart_type_repository.delete(cart_type_id)
