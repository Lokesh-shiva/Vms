from fastapi import APIRouter, HTTPException
from modules.timeslot.service.timeslot_service import TimeslotService
from modules.timeslot.schemas.timeslot_schema import CreateTimeslotSchema, UpdateTimeslotSchema


router = APIRouter(prefix="/api/v1/timeslots", tags=["Timeslots"])

timeslot_service = TimeslotService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.post("", status_code=201)
def create_timeslot(request_data: dict):
    """Create a new timeslot."""
    schema = CreateTimeslotSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        timeslot = timeslot_service.create_timeslot(schema.validated_data)
        return _success(timeslot, "Timeslot created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_timeslots():
    """Retrieve all timeslots."""
    timeslots = timeslot_service.list_timeslots()
    return _success(timeslots)


@router.get("/{timeslot_id}")
def get_timeslot(timeslot_id: int):
    """Retrieve a timeslot by ID."""
    timeslot = timeslot_service.get_timeslot(timeslot_id)
    if not timeslot:
        raise HTTPException(status_code=404, detail="Timeslot not found.")
    return _success(timeslot)


@router.put("/{timeslot_id}")
def update_timeslot(timeslot_id: int, request_data: dict):
    """Update an existing timeslot."""
    schema = UpdateTimeslotSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        timeslot = timeslot_service.update_timeslot(timeslot_id, schema.validated_data)
        if not timeslot:
            raise HTTPException(status_code=404, detail="Timeslot not found.")
        return _success(timeslot, "Timeslot updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{timeslot_id}")
def delete_timeslot(timeslot_id: int):
    """Delete a timeslot by ID."""
    deleted = timeslot_service.delete_timeslot(timeslot_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Timeslot not found.")
    return _success(None, "Timeslot deleted successfully.")
