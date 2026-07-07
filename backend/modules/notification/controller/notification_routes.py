from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_user
from modules.notification.service.notification_service import notification_service

router = APIRouter(prefix="/api/v1/notifications", tags=["Notifications"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_notifications(current_user: dict = Depends(require_user)):
    return _success(notification_service.list_for_user(current_user["id"]))


@router.put("/{notification_id}/read")
def mark_notification_read(notification_id: int, current_user: dict = Depends(require_user)):
    result = notification_service.mark_read(notification_id, current_user["id"])
    if not result:
        raise HTTPException(status_code=404, detail="Notification not found.")
    return _success(result, "Marked as read.")
