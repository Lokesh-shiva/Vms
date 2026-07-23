from fastapi import APIRouter, Depends, HTTPException, Query
from modules.auth.dependencies.auth_dependencies import require_admin, require_user
from modules.location.service.location_service import LocationService
from modules.location.schemas.location_schema import (
    CreateLocationSchema,
    UpdateLocationSchema,
)


router = APIRouter(prefix="/api/v1/locations", tags=["Locations"])

location_service = LocationService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_location(request_data: dict):
    """Create a new location."""
    schema = CreateLocationSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        location = location_service.create_location(schema.validated_data)
        return _success(location, "Location created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_locations():
    """Retrieve all locations."""
    locations = location_service.list_locations()
    return _success(locations)


@router.get("/nearest")
def get_nearest_locations(
    lat: float = Query(...),
    lng: float = Query(...),
    current_user: dict = Depends(require_user),
):
    """Serviceable locations nearest to a GPS point, closest first. Used to
    auto-suggest the user's area during onboarding."""
    try:
        locations = location_service.find_nearest_locations(lat, lng)
        return _success(locations)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{location_id}")
def get_location(location_id: int):
    """Retrieve a location by ID."""
    location = location_service.get_location(location_id)
    if not location:
        raise HTTPException(status_code=404, detail="Location not found.")
    return _success(location)


@router.put("/{location_id}", dependencies=[Depends(require_admin)])
def update_location(location_id: int, request_data: dict):
    """Update an existing location."""
    schema = UpdateLocationSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        location = location_service.update_location(location_id, schema.validated_data)
        if not location:
            raise HTTPException(status_code=404, detail="Location not found.")
        return _success(location, "Location updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{location_id}", dependencies=[Depends(require_admin)])
def delete_location(location_id: int):
    """Delete a location by ID."""
    deleted = location_service.delete_location(location_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Location not found.")
    return _success(None, "Location deleted successfully.")
