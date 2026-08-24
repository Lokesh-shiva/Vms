from fastapi import APIRouter, HTTPException, Query
from modules.auth.dependencies.auth_dependencies import require_role
from modules.trainer.service.trainer_booking_service import trainer_booking_service
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/admin/trainer-bookings", tags=["Admin Trainer Bookings"])

_BOOKING_ADMIN_ROLES = (UserRole.FINANCE, UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN)


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_bookings(
    status: str | None = Query(default=None),
    current_user: dict = require_role(*_BOOKING_ADMIN_ROLES),
):
    return _success(trainer_booking_service.list_all_bookings(status))


@router.get("/{booking_id}")
def get_booking(booking_id: int, current_user: dict = require_role(*_BOOKING_ADMIN_ROLES)):
    try:
        return _success(trainer_booking_service.get_booking(booking_id, current_user["id"], is_admin=True))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{booking_id}/approve")
def approve_booking(booking_id: int, current_user: dict = require_role(*_BOOKING_ADMIN_ROLES)):
    try:
        result = trainer_booking_service.approve_booking(booking_id)
        return _success(result, "Booking confirmed.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/reject")
def reject_booking(booking_id: int, current_user: dict = require_role(*_BOOKING_ADMIN_ROLES)):
    try:
        result = trainer_booking_service.reject_booking(booking_id)
        return _success(result, "Booking rejected.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
