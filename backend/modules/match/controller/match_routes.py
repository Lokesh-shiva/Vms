from fastapi import APIRouter, Depends, HTTPException, Query
from modules.auth.dependencies.auth_dependencies import (
    _ADMIN_ROLES,
    get_current_user,
    require_admin,
    require_user,
)
from modules.match.service.match_service import match_service
from modules.match.repository.match_repository import match_repository
from modules.match.repository.match_event_repository import match_event_repository
from modules.match.schemas.match_schema import CreateMatchSchema, MatchArriveSchema


router = APIRouter(prefix="/api/v1/matches", tags=["Matches"])


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("", status_code=201)
def create_match(request_data: dict, current_user: dict = Depends(require_user)):
    """Create a new match. System auto-assigns the ground. Requires user auth."""
    request_data["user_id"] = current_user["id"]

    schema = CreateMatchSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        match = match_service.create_match(current_user["id"], schema.validated_data)
        return _success(match, "Match created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{match_id}/join")
def join_match(match_id: int, current_user: dict = Depends(require_user)):
    """Join an existing match. Requires user auth."""
    try:
        match = match_service.join_match(current_user["id"], match_id)
        return _success(match, "Joined match successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{match_id}/leave")
def leave_match(match_id: int, current_user: dict = Depends(require_user)):
    """
    Leave a match.
    If the creator leaves, the match is auto-cancelled and cart freed.
    """
    try:
        match = match_service.leave_match(current_user["id"], match_id)
        return _success(match, "Left match successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{match_id}/cancel")
def cancel_match(match_id: int, current_user: dict = Depends(get_current_user)):
    """Cancel a match. Creator or admin only."""
    is_admin = current_user.get("role") in _ADMIN_ROLES
    try:
        match = match_service.cancel_match(
            current_user["id"], match_id, is_admin=is_admin
        )
        return _success(match, "Match cancelled successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{match_id}/complete", dependencies=[Depends(require_admin)])
def complete_match(match_id: int):
    """Force-complete a match. Admin only. Frees the cart."""
    try:
        match = match_service.complete_match(match_id)
        return _success(match, "Match completed successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{match_id}/arrive")
def arrive_match(
    match_id: int,
    body: MatchArriveSchema,
    current_user: dict = Depends(require_user),
):
    """
    Mark the current player as arrived at the match ground.
    Requires GPS coordinates for proximity validation.
    """
    try:
        match = match_service.arrive_match(
            current_user["id"], match_id, body.latitude, body.longitude
        )
        return _success(match, "Arrival recorded successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{match_id}/finish")
def finish_match(match_id: int, current_user: dict = Depends(require_user)):
    """
    Mark the match as completed. Frees the assigned ground.
    Any player in the match can trigger this.
    """
    try:
        match = match_service.finish_match(current_user["id"], match_id)
        return _success(match, "Match completed successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/open")
def get_open_matches(
    sport: str = Query(None),
    current_user: dict = Depends(require_user),
):
    """
    List WAITING play-now sessions in the current user's region.
    Used by OpenMatchesScreen so users can browse and join nearby games.
    """
    region_id = current_user.get("region_id")
    if not region_id:
        return _success([], "No region set — update your profile to see nearby matches.")

    sport_id = None
    if sport:
        from core.database.db_connection import SessionLocal
        from modules.cart_type.model.cart_type_model import CartType
        db = SessionLocal()
        try:
            ct = db.query(CartType).filter(CartType.name.ilike(sport)).first()
            if ct:
                sport_id = ct.id
        finally:
            db.close()

    matches = match_repository.find_waiting_in_region(region_id, sport_id)
    return _success(matches)


@router.get("/mine/active")
def get_my_active_match(current_user: dict = Depends(require_user)):
    """Return the current user's most recent active match, or null."""
    m = match_repository.find_active_by_user(current_user["id"])
    return _success(m)


@router.get("/mine")
def get_my_matches(current_user: dict = Depends(require_user)):
    """Return all matches the current user has joined, enriched, newest first."""
    matches = match_repository.find_by_user(current_user["id"])
    return _success(matches)


@router.get("/{match_id}")
def get_match(match_id: int, current_user: dict = Depends(require_user)):
    """Return a single match with enriched fields (sport, ground, captain, players)."""
    m = match_repository.find_by_id_enriched(match_id)
    if not m:
        raise HTTPException(status_code=404, detail="Match not found.")
    return _success(m)


@router.get("")
def list_matches(
    sport_id: int = Query(None, alias="sport_id"),
    region_id: int = Query(None, alias="region_id"),
    current_user: dict = Depends(get_current_user),
):
    """
    List all OPEN matches with future timeslots.
    Optional filters: sport_id, region_id.
    """
    matches = match_service.list_matches(cart_type_id=sport_id, region_id=region_id)
    return _success(matches)


@router.get("/{match_id}/events", dependencies=[Depends(require_admin)])
def get_match_events(match_id: int):
    """Return the full audit log for a match. Admin only."""
    events = match_event_repository.find_by_match_id(match_id)
    return _success(events)
