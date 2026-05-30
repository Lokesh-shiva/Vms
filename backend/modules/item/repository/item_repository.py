from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.item.model.item_model import Item


class ItemRepository:
    """
    Data access layer for Item entities.

    Responsibilities:
    - Performs CRUD operations against the PostgreSQL database.
    - Contains no business logic.
    - Returns raw dicts to the service layer (via Item.to_dict()).

    Accepts an optional session_factory for dependency injection
    (e.g. SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, item_data: dict) -> dict:
        """Insert a new item record. Returns the created item as a dict."""
        session = self._session_factory()
        try:
            item = Item(
                cart_type_id=item_data.get("cart_type_id"),
                name=item_data.get("name"),
                description=item_data.get("description", ""),
                price=item_data.get("price", 0.0),
                image_urls=item_data.get("image_urls", []),
                image_url=item_data.get("image_url"),
                is_available=item_data.get("is_available", True),
            )
            session.add(item)
            session.commit()
            session.refresh(item)
            return item.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, item_id: int) -> dict | None:
        """Retrieve an item by ID."""
        session = self._session_factory()
        try:
            item = session.query(Item).filter(Item.id == item_id).first()
            return item.to_dict() if item else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        """Retrieve all items."""
        session = self._session_factory()
        try:
            items = session.query(Item).all()
            return [i.to_dict() for i in items]
        finally:
            session.close()

    def find_by_cart_type_id(self, cart_type_id: int) -> list[dict]:
        """Retrieve all items belonging to a specific cart type."""
        session = self._session_factory()
        try:
            items = session.query(Item).filter(Item.cart_type_id == cart_type_id).all()
            return [i.to_dict() for i in items]
        finally:
            session.close()

    def find_by_name_and_cart_type(self, name: str, cart_type_id: int) -> dict | None:
        """Retrieve an item by name within a specific cart type (uniqueness check)."""
        session = self._session_factory()
        try:
            item = (
                session.query(Item)
                .filter(Item.name == name, Item.cart_type_id == cart_type_id)
                .first()
            )
            return item.to_dict() if item else None
        finally:
            session.close()

    def update(self, item_id: int, update_data: dict) -> dict | None:
        """Update an existing item record. Automatically refreshes updated_at."""
        session = self._session_factory()
        try:
            item = session.query(Item).filter(Item.id == item_id).first()
            if not item:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(item, key):
                    setattr(item, key, value)

            item.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(item)
            return item.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, item_id: int) -> bool:
        """Delete an item record by ID."""
        session = self._session_factory()
        try:
            item = session.query(Item).filter(Item.id == item_id).first()
            if not item:
                return False
            session.delete(item)
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

item_repository = ItemRepository()
