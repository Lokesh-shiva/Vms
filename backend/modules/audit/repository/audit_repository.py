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

    def find_all(self, limit: int = 200) -> list[dict]:
        session = self._session_factory()
        try:
            entries = (
                session.query(AuditLog)
                .order_by(AuditLog.id.desc())
                .limit(limit)
                .all()
            )
            return [e.to_dict() for e in entries]
        finally:
            session.close()


audit_repository = AuditRepository()
