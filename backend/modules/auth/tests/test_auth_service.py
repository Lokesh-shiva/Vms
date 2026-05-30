import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.user.model.user_model import User  # noqa: F401 — registers model
from modules.user.repository.user_repository import UserRepository
from modules.auth.service.auth_service import AuthService


def _make_test_session_factory():
    """Create an isolated SQLite in-memory session factory for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestAuthService(unittest.TestCase):
    """Unit tests for AuthService registration and login (SQLite-backed)."""

    def setUp(self):
        session_factory = _make_test_session_factory()
        repo = UserRepository(session_factory=session_factory)
        self.service = AuthService(user_repository=repo)

    # ── Register ──────────────────────────────────────────────────────

    def test_register_success(self):
        """Registering with valid data returns user dict with id and role."""
        result = self.service.register_user(
            {
                "name": "Alice",
                "phone": "+1234567890",
                "password": "secret123",
            }
        )
        self.assertIsNotNone(result)
        self.assertEqual(result["name"], "Alice")
        self.assertEqual(result["phone"], "+1234567890")
        self.assertEqual(result["role"], "user")
        self.assertIn("id", result)
        self.assertNotIn("password_hash", result)

    def test_register_duplicate_phone(self):
        """Registering with an already-used phone raises ValueError."""
        self.service.register_user(
            {
                "name": "Alice",
                "phone": "+1234567890",
                "password": "secret123",
            }
        )
        with self.assertRaises(ValueError) as ctx:
            self.service.register_user(
                {
                    "name": "Bob",
                    "phone": "+1234567890",
                    "password": "other456",
                }
            )
        self.assertIn("already exists", str(ctx.exception))

    # ── Login ─────────────────────────────────────────────────────────

    def test_login_success(self):
        """Logging in with correct credentials returns an access token."""
        self.service.register_user(
            {
                "name": "Alice",
                "phone": "+1234567890",
                "password": "secret123",
            }
        )
        result = self.service.login_user(
            {
                "phone": "+1234567890",
                "password": "secret123",
            }
        )
        self.assertIn("access_token", result)
        self.assertEqual(result["token_type"], "bearer")
        self.assertIsInstance(result["access_token"], str)
        self.assertTrue(len(result["access_token"]) > 0)

    def test_login_invalid_password(self):
        """Logging in with wrong password raises ValueError."""
        self.service.register_user(
            {
                "name": "Alice",
                "phone": "+1234567890",
                "password": "secret123",
            }
        )
        with self.assertRaises(ValueError) as ctx:
            self.service.login_user(
                {
                    "phone": "+1234567890",
                    "password": "wrongpass",
                }
            )
        self.assertIn("Invalid", str(ctx.exception))

    def test_login_nonexistent_phone(self):
        """Logging in with unregistered phone raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.login_user(
                {
                    "phone": "+9999999999",
                    "password": "anything",
                }
            )
        self.assertIn("Invalid", str(ctx.exception))


if __name__ == "__main__":
    unittest.main()
