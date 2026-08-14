from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_user
from modules.order.service.order_service import order_service

router = APIRouter(prefix="/api/v1/orders", tags=["Orders"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.post("", status_code=201)
def create_order(body: dict, current_user: dict = Depends(require_user)):
    """Checkout: body = {"items": [{"item_id": int, "quantity": int}, ...]}."""
    try:
        result = order_service.create_order(current_user["id"], body.get("items", []))
        return _success(result, "Order created.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/mine")
def list_my_orders(current_user: dict = Depends(require_user)):
    return _success(order_service.list_my_orders(current_user["id"]))


@router.get("/{order_id}")
def get_order(order_id: int, current_user: dict = Depends(require_user)):
    try:
        return _success(order_service.get_order(order_id, current_user["id"]))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{order_id}/submit-payment")
def submit_payment(order_id: int, body: dict, current_user: dict = Depends(require_user)):
    try:
        result = order_service.submit_payment(order_id, current_user["id"], body.get("transaction_id"))
        return _success(result, "Payment submitted for review.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
