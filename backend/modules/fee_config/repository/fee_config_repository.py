from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig


class FeeConfigRepository:
    """
    Data access layer for RegionCartTypeConfig entities.

    Responsibilities:
    - Performs CRUD operations against the database.
    - Contains no business logic.
    - Returns raw dicts via RegionCartTypeConfig.to_dict().

    Accepts an optional session_factory for dependency injection (SQLite in tests).
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, config_data: dict) -> dict:
        """Insert a new fee config record. Returns the created config as a dict."""
        session = self._session_factory()
        try:
            config = RegionCartTypeConfig(
                region_id=config_data["region_id"],
                cart_type_id=config_data["cart_type_id"],
                booking_fee=config_data["booking_fee"],
                cancellation_fee_pct=config_data.get("cancellation_fee_pct", 0),
                platform_fee_pct=config_data.get("platform_fee_pct", 0),
                is_active=config_data.get("is_active", True),
            )
            session.add(config)
            session.commit()
            session.refresh(config)
            return config.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, config_id: int) -> dict | None:
        """Retrieve a fee config by ID."""
        session = self._session_factory()
        try:
            config = (
                session.query(RegionCartTypeConfig)
                .filter(RegionCartTypeConfig.id == config_id)
                .first()
            )
            return config.to_dict() if config else None
        finally:
            session.close()

    def find_by_region_and_cart_type(
        self, region_id: int, cart_type_id: int
    ) -> dict | None:
        """Retrieve the active config for a region + cart type combination."""
        session = self._session_factory()
        try:
            config = (
                session.query(RegionCartTypeConfig)
                .filter(
                    RegionCartTypeConfig.region_id == region_id,
                    RegionCartTypeConfig.cart_type_id == cart_type_id,
                )
                .first()
            )
            return config.to_dict() if config else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        """Retrieve all fee configs."""
        session = self._session_factory()
        try:
            configs = session.query(RegionCartTypeConfig).all()
            return [c.to_dict() for c in configs]
        finally:
            session.close()

    def update(self, config_id: int, update_data: dict) -> dict | None:
        """Update an existing fee config record."""
        session = self._session_factory()
        try:
            config = (
                session.query(RegionCartTypeConfig)
                .filter(RegionCartTypeConfig.id == config_id)
                .first()
            )
            if not config:
                return None

            for key, value in update_data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(
                    config, key
                ):
                    setattr(config, key, value)

            config.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(config)
            return config.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, config_id: int) -> bool:
        """Soft-delete a fee config by setting is_active = False."""
        session = self._session_factory()
        try:
            config = (
                session.query(RegionCartTypeConfig)
                .filter(RegionCartTypeConfig.id == config_id)
                .first()
            )
            if not config:
                return False

            config.is_active = False
            config.updated_at = datetime.utcnow()
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

fee_config_repository = FeeConfigRepository()
