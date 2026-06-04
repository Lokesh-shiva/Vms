from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text
from core.database.db_connection import Base


class AuditLog(Base):
    __tablename__ = "audit_logs"

    id = Column(Integer, primary_key=True, autoincrement=True)
    action = Column(String, nullable=False, index=True)
    actor_user_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)
    target_resource_type = Column(String, nullable=True)
    target_resource_id = Column(Integer, nullable=True)
    details = Column(Text, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "action": self.action,
            "actor_user_id": self.actor_user_id,
            "target_resource_type": self.target_resource_type,
            "target_resource_id": self.target_resource_id,
            "details": self.details,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self) -> str:
        return f"<AuditLog id={self.id} action={self.action}>"
