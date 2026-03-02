from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.payment.model.system_config_model import SystemConfig


class SystemConfigRepository:
    """
    Data access layer for SystemConfig key-value store.

    Provides get/set operations for runtime configuration values.
    Accepts optional session_factory for test injection.
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def get(self, key: str) -> str | None:
        """Retrieve a config value by key. Returns None if not found."""
        session = self._session_factory()
        try:
            config = (
                session.query(SystemConfig)
                .filter(SystemConfig.key == key)
                .first()
            )
            return config.value if config else None
        finally:
            session.close()

    def set(self, key: str, value: str) -> dict:
        """Set a config value. Creates if not exists, updates if it does."""
        session = self._session_factory()
        try:
            config = (
                session.query(SystemConfig)
                .filter(SystemConfig.key == key)
                .first()
            )
            if config:
                config.value = value
                config.updated_at = datetime.utcnow()
            else:
                config = SystemConfig(key=key, value=value)
                session.add(config)
            session.commit()
            session.refresh(config)
            return config.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


system_config_repository = SystemConfigRepository()
