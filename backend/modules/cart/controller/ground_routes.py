"""
/api/v1/grounds — domain-named API for physical sports grounds.

These routes are a clean-named facade over the existing CartService.
No business logic lives here; all calls delegate to cart_service.
Internal field names (label, cart_type_id, region_id) are mapped
to domain names (name, sport_id, location_id) at the boundary.
"""

from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query

from modules.auth.dependencies.auth_dependencies import get_current_user, require_admin
from modules.cart.service.cart_service import CartService
from modules.cart.schemas.ground_schema import (
    CreateGroundSchema,
    UpdateGroundSchema,
    _to_ground,
)


router = APIRouter(prefix="/api/v1/grounds", tags=["Grounds"])

_cart_service = CartService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_ground(request_data: dict):
    """
    Create a new ground (physical sports court / pitch).

    Field names:
      - name        : display label for the ground
      - location_id : region the ground belongs to
      - sport_id    : sport type this ground supports (cart_type)
      - is_active   : whether the ground is in rotation (default true)
      - latitude    : GPS latitude (optional, used for arrival validation)
      - longitude   : GPS longitude (optional)
    """
    schema = CreateGroundSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart = _cart_service.create_cart(schema.validated_data)
        return _success(_to_ground(cart), "Ground created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_grounds(
    region_id: Optional[int] = Query(None, description="Filter by region ID"),
    current_user: dict = Depends(get_current_user),
):
    """List grounds.

    - ground_owner: forced to their assigned region (query param ignored).
    - Other roles: optional region_id filter.
    """
    effective_region = region_id
    if current_user["role"] == "ground_owner":
        effective_region = current_user.get("region_id")
        if effective_region is None:
            return _success([])

    carts = _cart_service.list_carts()
    grounds = [_to_ground(c) for c in carts]

    if effective_region is not None:
        grounds = [g for g in grounds if g.get("location_id") == effective_region]

    return _success(grounds)


@router.get("/{ground_id}")
def get_ground(ground_id: int):
    """Get a single ground by ID."""
    cart = _cart_service.get_cart(ground_id)
    if not cart:
        raise HTTPException(status_code=404, detail="Ground not found.")
    return _success(_to_ground(cart))


@router.put("/{ground_id}", dependencies=[Depends(require_admin)])
def update_ground(ground_id: int, request_data: dict):
    """Update an existing ground."""
    schema = UpdateGroundSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart = _cart_service.update_cart(ground_id, schema.validated_data)
        if not cart:
            raise HTTPException(status_code=404, detail="Ground not found.")
        return _success(_to_ground(cart), "Ground updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{ground_id}", dependencies=[Depends(require_admin)])
def delete_ground(ground_id: int):
    """Delete a ground by ID."""
    try:
        deleted = _cart_service.delete_cart(ground_id)
    except ValueError as e:
        raise HTTPException(status_code=409, detail=str(e))
    if not deleted:
        raise HTTPException(status_code=404, detail="Ground not found.")
    return _success(None, "Ground deleted successfully.")
