from core.base.base_service import BaseService
from modules.user.repository.user_repository import user_repository as _default_repo


class UserService(BaseService):
    """
    Business logic layer for User operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to the UserRepository.
    - Returns formatted results to the controller.
    """

    def __init__(self, user_repository=None):
        super().__init__()
        self.user_repository = user_repository or _default_repo

    def create_user(self, user_data: dict) -> dict:
        """
        Create a new user after applying business rules.

        Args:
            user_data: Validated user input.

        Returns:
            The created user record as a dict.

        Raises:
            ValueError: If business validation fails.
        """
        if not user_data.get("name"):
            raise ValueError("User name is required.")

        if not user_data.get("phone"):
            raise ValueError("Phone number is required.")

        # Enforce unique phone number
        existing = self.user_repository.find_by_phone(user_data["phone"])
        if existing:
            raise ValueError("A user with this phone number already exists.")

        return self.user_repository.create(user_data)

    def get_user(self, user_id: int) -> dict | None:
        """Retrieve a single user by ID."""
        return self.user_repository.find_by_id(user_id)

    def list_users(self) -> list[dict]:
        """Retrieve all users."""
        return self.user_repository.find_all()

    def update_user(self, user_id: int, update_data: dict, current_user: dict = None) -> dict | None:
        """
        Update an existing user.

        Args:
            user_id: Target user ID.
            update_data: Fields to update.
            current_user: Authenticated caller (defense-in-depth for role guard).

        Returns:
            The updated user record, or None if not found.

        Raises:
            ValueError: If phone uniqueness is violated or non-admin attempts role change.
        """
        if "role" in update_data:
            if not current_user or current_user.get("role") != "admin":
                raise ValueError("Only admins can change user roles.")

        existing = self.user_repository.find_by_id(user_id)
        if not existing:
            return None

        # If phone is being changed, enforce uniqueness
        new_phone = update_data.get("phone")
        if new_phone and new_phone != existing["phone"]:
            conflict = self.user_repository.find_by_phone(new_phone)
            if conflict:
                raise ValueError("A user with this phone number already exists.")

        return self.user_repository.update(user_id, update_data)

    def delete_user(self, user_id: int, current_user: dict = None) -> bool:
        """
        Delete a user by ID.

        Args:
            current_user: Authenticated caller (defense-in-depth for self-delete guard).

        Returns:
            True if deleted, False if user was not found.

        Raises:
            ValueError: If admin attempts to delete themselves.
        """
        if current_user and current_user.get("id") == user_id:
            raise ValueError("Cannot delete your own account.")

        return self.user_repository.delete(user_id)
