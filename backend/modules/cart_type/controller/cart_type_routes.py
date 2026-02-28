from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_admin
from modules.cart_type.service.cart_type_service import CartTypeService
from modules.cart_type.schemas.cart_type_schema import CreateCartTypeSchema, UpdateCartTypeSchema


router = APIRouter(prefix="/api/v1/cart-types", tags=["Cart Types"])

cart_type_service = CartTypeService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_cart_type(request_data: dict):
    """Create a new cart type."""
    schema = CreateCartTypeSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart_type = cart_type_service.create_cart_type(schema.validated_data)
        return _success(cart_type, "Cart type created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_cart_types():
    """Retrieve all cart types."""
    cart_types = cart_type_service.list_cart_types()
    return _success(cart_types)


@router.get("/{cart_type_id}")
def get_cart_type(cart_type_id: int):
    """Retrieve a cart type by ID."""
    cart_type = cart_type_service.get_cart_type(cart_type_id)
    if not cart_type:
        raise HTTPException(status_code=404, detail="Cart type not found.")
    return _success(cart_type)


@router.put("/{cart_type_id}", dependencies=[Depends(require_admin)])
def update_cart_type(cart_type_id: int, request_data: dict):
    """Update an existing cart type."""
    schema = UpdateCartTypeSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart_type = cart_type_service.update_cart_type(cart_type_id, schema.validated_data)
        if not cart_type:
            raise HTTPException(status_code=404, detail="Cart type not found.")
        return _success(cart_type, "Cart type updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{cart_type_id}", dependencies=[Depends(require_admin)])
def delete_cart_type(cart_type_id: int):
    """Delete a cart type by ID."""
    deleted = cart_type_service.delete_cart_type(cart_type_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Cart type not found.")
    return _success(None, "Cart type deleted successfully.")
