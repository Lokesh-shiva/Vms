from datetime import datetime

from sqlalchemy.exc import IntegrityError

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
                label=cart_data.get("label", ""),
                region_id=cart_data.get("region_id"),
                cart_type_id=cart_data.get("cart_type_id"),
                status=cart_data.get("status", "AVAILABLE"),
                is_active=cart_data.get("is_active", True),
            )
            session.add(cart)
            session.commit()
            if not cart.label:
                cart.label = f"CART-{cart.id}"
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

    def claim_available_cart(self, region_id: int, cart_type_id: int, session=None):
        """Atomically find one AVAILABLE+active cart, lock it, and set it to BUSY.

        Uses SELECT ... FOR UPDATE SKIP LOCKED on PostgreSQL so concurrent
        callers never pick the same row.  Falls back to a plain SELECT on
        SQLite (used in tests) where row-level locking is unavailable.

        Returns the Cart ORM object (still attached to *session*) or None
        if nothing is available.

        The caller owns the transaction — commit/rollback happens outside.
        """
        own_session = session is None
        session = session or self._session_factory()
        try:
            query = session.query(Cart).filter(
                Cart.region_id == region_id,
                Cart.cart_type_id == cart_type_id,
                Cart.status == "AVAILABLE",
                Cart.is_active == True,  # noqa: E712
            )

            dialect = session.bind.dialect.name
            if dialect != "sqlite":
                query = query.with_for_update(skip_locked=True)

            cart = query.first()
            if cart is None:
                return None

            cart.status = "BUSY"
            cart.updated_at = datetime.utcnow()
            session.flush()
            return cart
        except Exception:
            if own_session:
                session.rollback()
            raise
        finally:
            if own_session:
                session.close()

    def delete(self, cart_id: int) -> bool:
        """Delete a cart record by ID.

        Raises ValueError when the cart is referenced by bookings (FK
        constraint) so the service/route layer can return a 409 instead
        of an unhandled 500.
        """
        session = self._session_factory()
        try:
            cart = session.query(Cart).filter(Cart.id == cart_id).first()
            if not cart:
                return False
            session.delete(cart)
            session.commit()
            return True
        except IntegrityError:
            session.rollback()
            raise ValueError(
                "Cannot delete cart because it is referenced by existing "
                "bookings. Disable it instead."
            )
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


# ── Shared module-level instance ──────────────────────────────────────
# All services must import and use this singleton so they share the same
# DB-backed repository.  Tests inject their own session_factory instead.

cart_repository = CartRepository()
