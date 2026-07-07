"""
/api/v1/grounds — domain-named API for physical sports grounds.

These routes are a clean-named facade over the existing CartService.
No business logic lives here; all calls delegate to cart_service.
Internal field names (label, cart_type_id, region_id) are mapped
to domain names (name, sport_id, location_id) at the boundary.
"""

from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query

from modules.audit.service.audit_service import audit_service
from modules.auth.dependencies.auth_dependencies import _ADMIN_ROLES, get_current_user, require_admin
from modules.cart.service.cart_service import CartService
from modules.cart.schemas.ground_schema import (
    CreateGroundSchema,
    UpdateGroundSchema,
    _to_ground,
)
from modules.user.model.user_model import UserRole


router = APIRouter(prefix="/api/v1/grounds", tags=["Grounds"])

_cart_service = CartService()

_GROUND_OWNER_ALLOWED_FIELDS = frozenset({"is_active", "latitude", "longitude"})
# Same admin roles that previously had full access via require_admin, minus
# GROUND_OWNER — which is now scoped to its own grounds + a limited field set.
_GROUND_ADMIN_ROLES = _ADMIN_ROLES - {UserRole.GROUND_OWNER.value}


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("", status_code=201)
def create_ground(request_data: dict, current_user: dict = Depends(require_admin)):
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
        audit_service.log(
            action="GROUND_CREATED",
            actor_user_id=current_user["id"],
            target_resource_type="ground",
            target_resource_id=cart["id"],
            details={"name": cart.get("label")},
        )
        return _success(_to_ground(cart), "Ground created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_grounds(
    region_id: Optional[int] = Query(None, description="Filter by region ID"),
    current_user: dict = Depends(get_current_user),
):
    """List grounds.

    - ground_owner: sees only their owned grounds (DB-level isolation).
    - Other roles: optional region_id filter.
    """
    if current_user["role"].lower() == "ground_owner":
        carts = _cart_service.list_carts_by_owner(current_user["id"])
        return _success([_to_ground(c) for c in carts])

    if region_id is not None:
        carts = _cart_service.list_carts_by_region(region_id)
    else:
        carts = _cart_service.list_carts()
    return _success([_to_ground(c) for c in carts])


@router.get("/{ground_id}")
def get_ground(ground_id: int):
    """Get a single ground by ID."""
    cart = _cart_service.get_cart(ground_id)
    if not cart:
        raise HTTPException(status_code=404, detail="Ground not found.")
    return _success(_to_ground(cart))


@router.put("/{ground_id}")
def update_ground(
    ground_id: int,
    request_data: dict,
    current_user: dict = Depends(get_current_user),
):
    """
    Update an existing ground.

    - Admin roles: full access to all fields.
    - ground_owner: may only update their OWN grounds, and only
      is_active / latitude / longitude (self-service operational fields).
      Reassigning ownership, region, or sport type stays admin-only.
    """
    is_ground_owner = current_user["role"].lower() == "ground_owner"

    if is_ground_owner:
        existing = _cart_service.get_cart(ground_id)
        if not existing:
            raise HTTPException(status_code=404, detail="Ground not found.")
        if existing.get("owner_user_id") != current_user["id"]:
            raise HTTPException(status_code=403, detail="You do not own this ground.")
        disallowed = set(request_data.keys()) - _GROUND_OWNER_ALLOWED_FIELDS
        if disallowed:
            raise HTTPException(
                status_code=403,
                detail=f"Ground owners cannot update: {', '.join(sorted(disallowed))}.",
            )
    elif current_user["role"] not in _GROUND_ADMIN_ROLES:
        raise HTTPException(status_code=403, detail="Admin privileges required.")

    schema = UpdateGroundSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart = _cart_service.update_cart(ground_id, schema.validated_data)
        if not cart:
            raise HTTPException(status_code=404, detail="Ground not found.")
        if not is_ground_owner:
            audit_service.log(
                action="GROUND_UPDATED",
                actor_user_id=current_user["id"],
                target_resource_type="ground",
                target_resource_id=ground_id,
                details={"fields": list(request_data.keys())},
            )
        return _success(_to_ground(cart), "Ground updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{ground_id}")
def delete_ground(ground_id: int, current_user: dict = Depends(require_admin)):
    """Delete a ground by ID."""
    try:
        deleted = _cart_service.delete_cart(ground_id)
    except ValueError as e:
        raise HTTPException(status_code=409, detail=str(e))
    if not deleted:
        raise HTTPException(status_code=404, detail="Ground not found.")
    audit_service.log(
        action="GROUND_DELETED",
        actor_user_id=current_user["id"],
        target_resource_type="ground",
        target_resource_id=ground_id,
    )
    return _success(None, "Ground deleted successfully.")
