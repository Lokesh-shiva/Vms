from datetime import datetime

from sqlalchemy import Column, DateTime, Integer, String

from core.database.db_connection import Base


class SystemConfig(Base):
    """
    Simple key-value configuration store.

    Used for runtime-configurable settings like UPI_ID that admins
    can modify without redeploying or restarting the server.

    Fields:
        id (int): Primary key.
        key (str): Configuration key (unique, indexed).
        value (str): Configuration value.
        updated_at (datetime): Last modification timestamp.
    """

    __tablename__ = "system_configs"

    id = Column(Integer, primary_key=True, autoincrement=True)
    key = Column(String, nullable=False, unique=True, index=True)
    value = Column(String, nullable=False)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self):
        return {
            "id": self.id,
            "key": self.key,
            "value": self.value,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<SystemConfig key={self.key} value={self.value}>"
