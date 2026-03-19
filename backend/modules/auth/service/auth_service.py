import os
from datetime import datetime, timedelta, timezone
from pathlib import Path

import bcrypt
from dotenv import load_dotenv
from jose import jwt

from modules.user.repository.user_repository import user_repository as _default_repo

# ── Load .env ─────────────────────────────────────────────────────────

_env_path = Path(__file__).resolve().parents[3] / ".env"
load_dotenv(_env_path)

SECRET_KEY = os.getenv("SECRET_KEY")
if not SECRET_KEY:
    raise RuntimeError("SECRET_KEY is not set. Check your .env file.")

ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60


def _hash_password(password: str) -> str:
    """Hash a password using bcrypt."""
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def _verify_password(plain: str, hashed: str) -> bool:
    """Verify a password against a bcrypt hash."""
    return bcrypt.checkpw(plain.encode("utf-8"), hashed.encode("utf-8"))


class AuthService:
    """
    Authentication service handling registration and login.

    Delegates all DB access to UserRepository (shared instance).
    """

    def __init__(self, user_repository=None):
        self.user_repository = user_repository or _default_repo

    def register_user(self, data: dict) -> dict:
        """Register a new user with bcrypt-hashed password.

        Args:
            data: Validated dict with name, phone, password.

        Returns:
            Safe user dict (no password_hash).

        Raises:
            ValueError: If phone is already registered.
        """
        existing = self.user_repository.find_by_phone(data["phone"])
        if existing:
            raise ValueError("A user with this phone number already exists.")

        hashed = _hash_password(data["password"])

        user = self.user_repository.create({
            "name": data["name"],
            "phone": data["phone"],
            "password_hash": hashed,
            "role": "user",
        })

        return user

    def login_user(self, data: dict) -> dict:
        """Authenticate a user and return a JWT.

        Args:
            data: Validated dict with phone, password.

        Returns:
            Dict with access_token and token_type.

        Raises:
            ValueError: If credentials are invalid.
        """
        user = self.user_repository.find_by_phone_with_hash(data["phone"])
        if not user:
            raise ValueError("Invalid phone number or password.")

        if not _verify_password(data["password"], user["password_hash"]):
            raise ValueError("Invalid phone number or password.")

        expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        payload = {
            "sub": str(user["id"]),
            "role": user["role"],
            "exp": expire,
        }
        token = jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)

        return {"access_token": token, "token_type": "bearer"}
