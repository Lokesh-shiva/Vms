from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.cart_type.model.cart_type_model import CartType


class CartTypeRepository:
    """
    Data access layer for CartType entities.

    Responsibilities:
    - Performs CRUD operations against the PostgreSQL database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via CartType.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, cart_type_data: dict) -> dict:
        """Insert a new cart type record. Returns the created cart type as a dict."""
        session = self._session_factory()
        try:
            cart_type = CartType(
                name=cart_type_data.get("name"),
                description=cart_type_data.get("description", ""),
                is_active=cart_type_data.get("is_active", True),
            )
            session.add(cart_type)
            session.commit()
            session.refresh(cart_type)
            return cart_type.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, cart_type_id: int) -> dict | None:
        """Retrieve a cart type by ID."""
        session = self._session_factory()
        try:
            cart_type = (
                session.query(CartType).filter(CartType.id == cart_type_id).first()
            )
            return cart_type.to_dict() if cart_type else None
        finally:
            session.close()

    def find_by_name(self, name: str) -> dict | None:
        """Retrieve a cart type by name."""
        session = self._session_factory()
        try:
            cart_type = session.query(CartType).filter(CartType.name == name).first()
            return cart_type.to_dict() if cart_type else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        """Retrieve all cart types."""
        session = self._session_factory()
        try:
            cart_types = session.query(CartType).all()
            return [ct.to_dict() for ct in cart_types]
        finally:
            session.close()

    def update(self, cart_type_id: int, update_data: dict) -> dict | None:
        """Update an existing cart type record. Automatically refreshes updated_at."""
        session = self._session_factory()
        try:
            cart_type = (
                session.query(CartType).filter(CartType.id == cart_type_id).first()
            )
            if not cart_type:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(
                    cart_type, key
                ):
                    setattr(cart_type, key, value)

            cart_type.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(cart_type)
            return cart_type.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, cart_type_id: int) -> bool:
        """Delete a cart type record by ID."""
        session = self._session_factory()
        try:
            cart_type = (
                session.query(CartType).filter(CartType.id == cart_type_id).first()
            )
            if not cart_type:
                return False
            session.delete(cart_type)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


# ── Shared module-level instance ──────────────────────────────────────
# All services must import and use this singleton so they share the same
# DB-backed repository.  Tests inject their own session_factory instead.

cart_type_repository = CartTypeRepository()
