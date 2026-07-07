from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.audit.model.audit_model import AuditLog


class AuditRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            entry = AuditLog(
                action=data["action"],
                actor_user_id=data.get("actor_user_id"),
                target_resource_type=data.get("target_resource_type"),
                target_resource_id=data.get("target_resource_id"),
                details=data.get("details"),
            )
            session.add(entry)
            session.commit()
            session.refresh(entry)
            return entry.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_all(
        self,
        limit: int = 200,
        offset: int = 0,
        action: str | None = None,
        actor_user_id: int | None = None,
        target_resource_type: str | None = None,
        start_date: datetime | None = None,
        end_date: datetime | None = None,
    ) -> list[dict]:
        session = self._session_factory()
        try:
            query = session.query(AuditLog)
            if action is not None:
                query = query.filter(AuditLog.action == action)
            if actor_user_id is not None:
                query = query.filter(AuditLog.actor_user_id == actor_user_id)
            if target_resource_type is not None:
                query = query.filter(AuditLog.target_resource_type == target_resource_type)
            if start_date is not None:
                query = query.filter(AuditLog.created_at >= start_date)
            if end_date is not None:
                query = query.filter(AuditLog.created_at <= end_date)
            entries = (
                query.order_by(AuditLog.id.desc())
                .offset(offset)
                .limit(limit)
                .all()
            )
            return [e.to_dict() for e in entries]
        finally:
            session.close()


audit_repository = AuditRepository()
