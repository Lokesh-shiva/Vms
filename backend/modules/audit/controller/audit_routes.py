from datetime import date, datetime
from typing import Optional

from fastapi import APIRouter, HTTPException, Query
from modules.auth.dependencies.auth_dependencies import require_role
from modules.audit.service.audit_service import audit_service
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/audit-logs", tags=["Audit"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_audit_logs(
    limit: int = Query(default=200, le=500),
    offset: int = Query(default=0, ge=0),
    action: Optional[str] = Query(default=None),
    actor_user_id: Optional[int] = Query(default=None),
    target_resource_type: Optional[str] = Query(default=None),
    start_date: Optional[date] = Query(default=None, description="YYYY-MM-DD, inclusive"),
    end_date: Optional[date] = Query(default=None, description="YYYY-MM-DD, inclusive"),
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    """Return audit log entries, optionally filtered. SUPER_ADMIN only."""
    try:
        start_dt = datetime.combine(start_date, datetime.min.time()) if start_date else None
        end_dt = datetime.combine(end_date, datetime.max.time()) if end_date else None
        return _success(audit_service.list_logs(
            limit=limit,
            offset=offset,
            action=action,
            actor_user_id=actor_user_id,
            target_resource_type=target_resource_type,
            start_date=start_dt,
            end_date=end_dt,
        ))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
