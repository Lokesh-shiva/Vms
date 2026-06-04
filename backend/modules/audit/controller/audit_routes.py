from fastapi import APIRouter, Depends, Query
from modules.auth.dependencies.auth_dependencies import require_role
from modules.audit.service.audit_service import audit_service
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/audit-logs", tags=["Audit"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_audit_logs(
    limit: int = Query(default=200, le=500),
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    """Return recent audit log entries. SUPER_ADMIN only."""
    return _success(audit_service.list_logs(limit=limit))
