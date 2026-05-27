from typing import Optional

from fastapi import APIRouter, HTTPException, Query

from modules.auth.dependencies.auth_dependencies import require_role
from modules.captain.schemas.captain_schema import CreateCaptainSchema, UpdateCaptainSchema
from modules.captain.service.captain_service import CaptainService
from modules.user.model.user_model import UserRole


router = APIRouter(prefix="/api/v1/captains", tags=["Captains"])

captain_service = CaptainService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.get("")
def list_captains(
    region_id: Optional[int] = Query(None, description="Filter by region ID"),
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """List all captains. Optionally filter by region_id."""
    captains = captain_service.list_captains(region_id=region_id)
    return _success(captains)


@router.post("", status_code=201)
def create_captain(
    request_data: dict,
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """Register a user as a captain."""
    schema = CreateCaptainSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        captain = captain_service.create_captain(schema.validated_data)
        return _success(captain, "Captain created successfully.")
    except HTTPException:
        raise
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{captain_id}")
def get_captain(
    captain_id: int,
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """Retrieve a single captain by ID."""
    captain = captain_service.get_captain(captain_id)
    return _success(captain)


@router.put("/{captain_id}")
def update_captain(
    captain_id: int,
    request_data: dict,
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """Update an existing captain record."""
    schema = UpdateCaptainSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        captain = captain_service.update_captain(captain_id, schema.validated_data)
        return _success(captain, "Captain updated successfully.")
    except HTTPException:
        raise
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{captain_id}")
def delete_captain(
    captain_id: int,
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    """Delete a captain. Restricted to SUPER_ADMIN."""
    try:
        captain_service.delete_captain(captain_id)
        return _success(None, "Captain deleted successfully.")
    except HTTPException:
        raise
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
