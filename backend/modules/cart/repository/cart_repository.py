from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.cart.model.cart_model import Cart


class CartRepository:
    """
    Data access layer for Cart entities.

    Responsibilities:
    - Performs CRUD operations against the PostgreSQL database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via Cart.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, cart_data: dict) -> dict:
        """Insert a new cart record. Returns the created cart as a dict."""
        session = self._session_factory()
        try:
            cart = Cart(
                region_id=cart_data.get("region_id"),
                cart_type_id=cart_data.get("cart_type_id"),
                status=cart_data.get("status", "AVAILABLE"),
                is_active=cart_data.get("is_active", True),
            )
            session.add(cart)
            session.commit()
            session.refresh(cart)
            return cart.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, cart_id: int) -> dict | None:
        """Retrieve a cart by ID."""
        session = self._session_factory()
        try:
            cart = session.query(Cart).filter(Cart.id == cart_id).first()
            return cart.to_dict() if cart else None
        finally:
            session.close()

    def find_all(self, session=None) -> list[dict]:
        """Retrieve all carts."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            carts = session.query(Cart).all()
            return [c.to_dict() for c in carts]
        finally:
            if own_session:
                session.close()

    def update(self, cart_id: int, update_data: dict, session=None) -> dict | None:
        """Update an existing cart record. Automatically refreshes updated_at."""
        own_session = session is None
        session = session or self._session_factory()
        try:
            cart = session.query(Cart).filter(Cart.id == cart_id).first()
            if not cart:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(cart, key):
                    setattr(cart, key, value)

            cart.updated_at = datetime.utcnow()
            if own_session:
                session.commit()
            else:
                session.flush()
            session.refresh(cart)
            return cart.to_dict()
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def delete(self, cart_id: int) -> bool:
        """Delete a cart record by ID."""
        session = self._session_factory()
        try:
            cart = session.query(Cart).filter(Cart.id == cart_id).first()
            if not cart:
                return False
            session.delete(cart)
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

cart_repository = CartRepository()
