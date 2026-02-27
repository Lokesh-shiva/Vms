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

    def update_user(self, user_id: int, update_data: dict) -> dict | None:
        """
        Update an existing user.

        Args:
            user_id: Target user ID.
            update_data: Fields to update.

        Returns:
            The updated user record, or None if not found.

        Raises:
            ValueError: If phone uniqueness is violated.
        """
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

    def delete_user(self, user_id: int) -> bool:
        """
        Delete a user by ID.

        Returns:
            True if deleted, False if user was not found.
        """
        return self.user_repository.delete(user_id)
