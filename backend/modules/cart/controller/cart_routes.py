from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_admin
from modules.cart.service.cart_service import CartService
from modules.cart.schemas.cart_schema import CreateCartSchema, UpdateCartSchema


router = APIRouter(
    prefix="/api/v1/carts",
    tags=["Carts (deprecated — use /api/v1/grounds)"],
)

cart_service = CartService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post(
    "", status_code=201, dependencies=[Depends(require_admin)], deprecated=True
)
def create_cart(request_data: dict):
    """**Deprecated** — use `POST /api/v1/grounds` instead."""
    schema = CreateCartSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart = cart_service.create_cart(schema.validated_data)
        return _success(cart, "Cart created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("", deprecated=True)
def list_carts():
    """**Deprecated** — use `GET /api/v1/grounds` instead."""
    carts = cart_service.list_carts()
    return _success(carts)


@router.get("/{cart_id}", deprecated=True)
def get_cart(cart_id: int):
    """**Deprecated** — use `GET /api/v1/grounds/{ground_id}` instead."""
    cart = cart_service.get_cart(cart_id)
    if not cart:
        raise HTTPException(status_code=404, detail="Cart not found.")
    return _success(cart)


@router.put("/{cart_id}", dependencies=[Depends(require_admin)], deprecated=True)
def update_cart(cart_id: int, request_data: dict):
    """**Deprecated** — use `PUT /api/v1/grounds/{ground_id}` instead."""
    schema = UpdateCartSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart = cart_service.update_cart(cart_id, schema.validated_data)
        if not cart:
            raise HTTPException(status_code=404, detail="Cart not found.")
        return _success(cart, "Cart updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{cart_id}", dependencies=[Depends(require_admin)], deprecated=True)
def delete_cart(cart_id: int):
    """**Deprecated** — use `DELETE /api/v1/grounds/{ground_id}` instead."""
    try:
        deleted = cart_service.delete_cart(cart_id)
    except ValueError as e:
        raise HTTPException(status_code=409, detail=str(e))
    if not deleted:
        raise HTTPException(status_code=404, detail="Cart not found.")
    return _success(None, "Cart deleted successfully.")
