import json
from modules.audit.repository.audit_repository import audit_repository as _default_repo


class AuditService:
    def __init__(self, repository=None):
        self.repository = repository or _default_repo

    def log(
        self,
        action: str,
        actor_user_id: int | None = None,
        target_resource_type: str | None = None,
        target_resource_id: int | None = None,
        details: dict | None = None,
    ) -> dict:
        """Append one audit entry. Never raises — audit failure must not break operations."""
        try:
            return self.repository.create({
                "action": action,
                "actor_user_id": actor_user_id,
                "target_resource_type": target_resource_type,
                "target_resource_id": target_resource_id,
                "details": json.dumps(details) if details else None,
            })
        except Exception:
            return {}

    def list_logs(self, limit: int = 200) -> list[dict]:
        return self.repository.find_all(limit=limit)


audit_service = AuditService()
