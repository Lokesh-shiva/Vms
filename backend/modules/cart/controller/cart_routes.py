from fastapi import APIRouter, HTTPException
from modules.cart.service.cart_service import CartService
from modules.cart.schemas.cart_schema import CreateCartSchema, UpdateCartSchema


router = APIRouter(prefix="/api/v1/carts", tags=["Carts"])

cart_service = CartService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.post("", status_code=201)
def create_cart(request_data: dict):
    """Create a new cart."""
    schema = CreateCartSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart = cart_service.create_cart(schema.validated_data)
        return _success(cart, "Cart created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_carts():
    """Retrieve all carts."""
    carts = cart_service.list_carts()
    return _success(carts)


@router.get("/{cart_id}")
def get_cart(cart_id: int):
    """Retrieve a cart by ID."""
    cart = cart_service.get_cart(cart_id)
    if not cart:
        raise HTTPException(status_code=404, detail="Cart not found.")
    return _success(cart)


@router.put("/{cart_id}")
def update_cart(cart_id: int, request_data: dict):
    """Update an existing cart."""
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


@router.delete("/{cart_id}")
def delete_cart(cart_id: int):
    """Delete a cart by ID."""
    deleted = cart_service.delete_cart(cart_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Cart not found.")
    return _success(None, "Cart deleted successfully.")
