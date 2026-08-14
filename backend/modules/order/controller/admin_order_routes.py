from fastapi import APIRouter, HTTPException, Query
from modules.auth.dependencies.auth_dependencies import require_role
from modules.order.service.order_service import order_service
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/admin/orders", tags=["Admin Orders"])

_ORDER_ADMIN_ROLES = (UserRole.FINANCE, UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN)


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_orders(
    status: str | None = Query(default=None),
    current_user: dict = require_role(*_ORDER_ADMIN_ROLES),
):
    return _success(order_service.list_all_orders(status))


@router.get("/{order_id}")
def get_order(order_id: int, current_user: dict = require_role(*_ORDER_ADMIN_ROLES)):
    try:
        return _success(order_service.get_order(order_id, current_user["id"], is_admin=True))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{order_id}/approve")
def approve_order(order_id: int, current_user: dict = require_role(*_ORDER_ADMIN_ROLES)):
    try:
        result = order_service.approve_order(order_id)
        return _success(result, "Order approved.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{order_id}/reject")
def reject_order(order_id: int, current_user: dict = require_role(*_ORDER_ADMIN_ROLES)):
    try:
        result = order_service.reject_order(order_id)
        return _success(result, "Order rejected.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
