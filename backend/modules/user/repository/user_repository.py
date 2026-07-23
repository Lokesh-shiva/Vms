from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.user.model.user_model import User, UserRole


class UserRepository:
    """
    Data access layer for User entities.

    Responsibilities:
    - Performs CRUD operations against the PostgreSQL database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via User.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, user_data: dict) -> dict:
        """Insert a new user record. Returns the created user as a dict."""
        session = self._session_factory()
        try:
            user = User(
                name=user_data.get("name"),
                phone=user_data.get("phone"),
                username=user_data.get("username"),
                email=user_data.get("email"),
                password_hash=user_data.get("password_hash", ""),
                role=user_data.get("role", UserRole.USER.value),
                is_active=user_data.get("is_active", True),
            )
            session.add(user)
            session.commit()
            session.refresh(user)
            return user.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, user_id: int) -> dict | None:
        """Retrieve a user by ID."""
        session = self._session_factory()
        try:
            user = session.query(User).filter(User.id == user_id).first()
            return user.to_dict() if user else None
        finally:
            session.close()

    def find_by_phone(self, phone: str) -> dict | None:
        """Retrieve a user by phone number."""
        session = self._session_factory()
        try:
            user = session.query(User).filter(User.phone == phone).first()
            return user.to_dict() if user else None
        finally:
            session.close()

    def find_by_username(self, username: str) -> dict | None:
        """Retrieve a user by username (for uniqueness checks)."""
        session = self._session_factory()
        try:
            user = session.query(User).filter(User.username == username).first()
            return user.to_dict() if user else None
        finally:
            session.close()

    def find_by_email(self, email: str) -> dict | None:
        """Retrieve a user by email (for uniqueness checks)."""
        session = self._session_factory()
        try:
            user = session.query(User).filter(User.email == email).first()
            return user.to_dict() if user else None
        finally:
            session.close()

    def find_by_phone_with_hash(self, phone: str) -> dict | None:
        """Retrieve a user by phone including password_hash (auth only).

        Uses explicit field list instead of to_dict() to prevent
        future refactors from accidentally leaking sensitive data.
        """
        session = self._session_factory()
        try:
            user = session.query(User).filter(User.phone == phone).first()
            if not user:
                return None
            return {
                "id": user.id,
                "name": user.name,
                "phone": user.phone,
                "role": user.role,
                "is_active": user.is_active,
                "password_hash": user.password_hash,
            }
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        """Retrieve all users."""
        session = self._session_factory()
        try:
            users = session.query(User).all()
            return [u.to_dict() for u in users]
        finally:
            session.close()

    def update(self, user_id: int, update_data: dict) -> dict | None:
        """Update an existing user record. Automatically refreshes updated_at."""
        session = self._session_factory()
        try:
            user = session.query(User).filter(User.id == user_id).first()
            if not user:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(user, key):
                    setattr(user, key, value)

            user.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(user)
            return user.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, user_id: int) -> bool:
        """Delete a user record by ID."""
        session = self._session_factory()
        try:
            user = session.query(User).filter(User.id == user_id).first()
            if not user:
                return False
            session.delete(user)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def increment_ghost_strikes(self, user_id: int, session=None) -> dict | None:
        """Increment ghost_strikes by 1 for the given user.

        Accepts an optional caller-managed session for transaction safety.
        Returns the updated user dict, or None if user not found.
        """
        own_session = session is None
        session = session or self._session_factory()
        try:
            user = session.query(User).filter(User.id == user_id).first()
            if not user:
                return None
            user.ghost_strikes = (user.ghost_strikes or 0) + 1
            user.updated_at = datetime.utcnow()
            if own_session:
                session.commit()
                session.refresh(user)
            else:
                session.flush()
                session.refresh(user)
            return user.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def reset_ghost_strikes(self, user_id: int, session=None) -> dict | None:
        """Reset ghost_strikes to 0 for the given user.

        Accepts an optional caller-managed session for transaction safety.
        Returns the updated user dict, or None if user not found.
        """
        own_session = session is None
        session = session or self._session_factory()
        try:
            user = session.query(User).filter(User.id == user_id).first()
            if not user:
                return None
            user.ghost_strikes = 0
            user.updated_at = datetime.utcnow()
            if own_session:
                session.commit()
                session.refresh(user)
            else:
                session.flush()
                session.refresh(user)
            return user.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()


# ── Shared module-level instance ──────────────────────────────────────
# All services must import and use this singleton so they share the same
# DB-backed repository.  Tests inject their own session_factory instead.

user_repository = UserRepository()
