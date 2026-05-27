import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.user.model.user_model import User  # noqa: F401 — registers model
from modules.user.repository.user_repository import UserRepository
from modules.user.service.user_service import UserService


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestUserService(unittest.TestCase):
    """Unit tests for UserService CRUD operations (SQLite-backed)."""

    def setUp(self):
        session_factory = _make_test_session_factory()
        repo = UserRepository(session_factory=session_factory)
        self.service = UserService(user_repository=repo)

    # ── Create ────────────────────────────────────────────────────────

    def test_create_user_success(self):
        """Creating a user with valid data returns the record with an id."""
        result = self.service.create_user({"name": "Alice", "phone": "+1234567890"})
        self.assertIsNotNone(result)
        self.assertEqual(result["name"], "Alice")
        self.assertEqual(result["role"], "user")
        self.assertIn("id", result)
        self.assertIn("created_at", result)
        self.assertIn("updated_at", result)

    def test_create_user_missing_name(self):
        """Creating a user without a name raises ValueError."""
        with self.assertRaises(ValueError):
            self.service.create_user({"phone": "+1234567890"})

    def test_create_user_missing_phone(self):
        """Creating a user without a phone raises ValueError."""
        with self.assertRaises(ValueError):
            self.service.create_user({"name": "Alice"})

    def test_create_user_duplicate_phone(self):
        """Creating a user with an already-registered phone raises ValueError."""
        self.service.create_user({"name": "Alice", "phone": "+1234567890"})
        with self.assertRaises(ValueError) as ctx:
            self.service.create_user({"name": "Bob", "phone": "+1234567890"})
        self.assertIn("already exists", str(ctx.exception))

    def test_create_user_with_admin_role(self):
        """Creating a user with role 'admin' succeeds."""
        result = self.service.create_user(
            {"name": "Admin", "phone": "+1112223333", "role": "admin"}
        )
        self.assertEqual(result["role"], "admin")

    # ── Read ──────────────────────────────────────────────────────────

    def test_get_user_found(self):
        """Retrieving an existing user returns the correct record."""
        created = self.service.create_user({"name": "Bob", "phone": "+9876543210"})
        fetched = self.service.get_user(created["id"])
        self.assertEqual(fetched["name"], "Bob")

    def test_get_user_not_found(self):
        """Retrieving a non-existent user returns None."""
        result = self.service.get_user(999)
        self.assertIsNone(result)

    def test_list_users(self):
        """Listing users returns all created records."""
        self.service.create_user({"name": "User1", "phone": "+1111111111"})
        self.service.create_user({"name": "User2", "phone": "+2222222222"})
        users = self.service.list_users()
        self.assertEqual(len(users), 2)

    # ── Update ────────────────────────────────────────────────────────

    def test_update_user_success(self):
        """Updating an existing user modifies the record and refreshes updated_at."""
        created = self.service.create_user({"name": "Charlie", "phone": "+5555555555"})
        updated = self.service.update_user(created["id"], {"name": "Charles"})
        self.assertEqual(updated["name"], "Charles")

    def test_update_user_not_found(self):
        """Updating a non-existent user returns None."""
        result = self.service.update_user(999, {"name": "Ghost"})
        self.assertIsNone(result)

    def test_update_user_duplicate_phone(self):
        """Updating phone to one that already exists raises ValueError."""
        self.service.create_user({"name": "Alice", "phone": "+1111111111"})
        bob = self.service.create_user({"name": "Bob", "phone": "+2222222222"})
        with self.assertRaises(ValueError) as ctx:
            self.service.update_user(bob["id"], {"phone": "+1111111111"})
        self.assertIn("already exists", str(ctx.exception))

    def test_update_preserves_created_at(self):
        """Updating a user must not change created_at."""
        created = self.service.create_user({"name": "Eve", "phone": "+8888888888"})
        original_created_at = created["created_at"]
        updated = self.service.update_user(created["id"], {"name": "Eva"})
        self.assertEqual(updated["created_at"], original_created_at)

    # ── Delete ────────────────────────────────────────────────────────

    def test_delete_user_success(self):
        """Deleting an existing user returns True."""
        created = self.service.create_user({"name": "Dave", "phone": "+7777777777"})
        self.assertTrue(self.service.delete_user(created["id"]))

    def test_delete_user_not_found(self):
        """Deleting a non-existent user returns False."""
        self.assertFalse(self.service.delete_user(999))

    # ── SUPER_ADMIN-only role/deactivate guards ───────────────────────

    def test_update_user_only_super_admin_can_change_role(self):
        """Non-super-admin caller raising ValueError when attempting a role change."""
        created = self.service.create_user({"name": "Frank", "phone": "+6661112222"})
        with self.assertRaises(ValueError) as ctx:
            self.service.update_user(
                created["id"],
                {"role": "ops_manager"},
                current_user={"id": 99, "role": "ops_manager"},
            )
        self.assertIn("super admin", str(ctx.exception).lower())

    def test_update_user_blocks_self_role_change(self):
        """SUPER_ADMIN cannot change their own role (self-lockout guard)."""
        created = self.service.create_user({"name": "Grace", "phone": "+7771112222"})
        target_id = created["id"]
        with self.assertRaises(ValueError) as ctx:
            self.service.update_user(
                target_id,
                {"role": "ops_manager"},
                current_user={"id": target_id, "role": "super_admin"},
            )
        self.assertIn("Cannot change your own role", str(ctx.exception))


if __name__ == "__main__":
    unittest.main()
