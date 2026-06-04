from core.base.base_service import BaseService
from modules.cart.repository.cart_repository import (
    cart_repository as _default_cart_repo,
)
from modules.location.repository.location_repository import (
    location_repository as _default_location_repo,
)
from modules.cart_type.repository.cart_type_repository import (
    cart_type_repository as _default_cart_type_repo,
)


class CartService(BaseService):
    """
    Business logic layer for Cart operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to the CartRepository.
    - Cross-validates against shared LocationRepository and CartTypeRepository.
    - Returns formatted results to the controller.
    """

    def __init__(
        self, cart_repository=None, location_repository=None, cart_type_repository=None
    ):
        super().__init__()
        self.cart_repository = cart_repository or _default_cart_repo
        self.location_repository = location_repository or _default_location_repo
        self.cart_type_repository = cart_type_repository or _default_cart_type_repo

    # ── Helpers ───────────────────────────────────────────────────────

    def _validate_region(self, region_id: int) -> None:
        """Ensure the referenced region (location) exists."""
        region = self.location_repository.find_by_id(region_id)
        if not region:
            raise ValueError("Referenced region does not exist.")

    def _validate_cart_type(self, cart_type_id: int) -> None:
        """Ensure the referenced cart type exists."""
        cart_type = self.cart_type_repository.find_by_id(cart_type_id)
        if not cart_type:
            raise ValueError("Referenced cart type does not exist.")

    # ── CRUD ──────────────────────────────────────────────────────────

    def create_cart(self, cart_data: dict) -> dict:
        """
        Create a new cart after applying business rules.

        Args:
            cart_data: Validated cart input.

        Returns:
            The created cart record as a dict.

        Raises:
            ValueError: If business validation fails.
        """
        # Validate region exists
        self._validate_region(cart_data["region_id"])

        # Validate cart type exists
        self._validate_cart_type(cart_data["cart_type_id"])

        return self.cart_repository.create(cart_data)

    def get_cart(self, cart_id: int) -> dict | None:
        """Retrieve a single cart by ID."""
        return self.cart_repository.find_by_id(cart_id)

    def list_carts(self) -> list[dict]:
        """Retrieve all carts."""
        return self.cart_repository.find_all()

    def list_carts_by_owner(self, owner_user_id: int) -> list[dict]:
        """Return all carts owned by the given user. Used for GROUND_OWNER isolation."""
        return self.cart_repository.find_by_owner(owner_user_id)

    def list_carts_by_region(self, region_id: int) -> list[dict]:
        """Return all carts in a specific region. DB-level filter."""
        return self.cart_repository.find_by_region(region_id)

    def update_cart(self, cart_id: int, update_data: dict) -> dict | None:
        """
        Update an existing cart.

        Args:
            cart_id: Target cart ID.
            update_data: Fields to update.

        Returns:
            The updated cart record, or None if not found.

        Raises:
            ValueError: If business validation fails.
        """
        existing = self.cart_repository.find_by_id(cart_id)
        if not existing:
            return None

        # If region_id is being changed, re-validate
        new_region_id = update_data.get("region_id")
        if new_region_id and new_region_id != existing["region_id"]:
            self._validate_region(new_region_id)

        # If cart_type_id is being changed, re-validate
        new_cart_type_id = update_data.get("cart_type_id")
        if new_cart_type_id and new_cart_type_id != existing["cart_type_id"]:
            self._validate_cart_type(new_cart_type_id)

        return self.cart_repository.update(cart_id, update_data)

    def delete_cart(self, cart_id: int) -> bool:
        """
        Delete a cart by ID.

        Returns:
            True if deleted, False if cart was not found.
        """
        return self.cart_repository.delete(cart_id)
