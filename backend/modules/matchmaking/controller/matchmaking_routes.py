from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse
from core.database.db_connection import SessionLocal
from modules.auth.dependencies.auth_dependencies import require_user
from modules.matchmaking.service.matchmaking_service import matchmaking_service
from modules.matchmaking.schemas.matchmaking_schema import JoinQueueRequest
from modules.cart_type.model.cart_type_model import CartType


router = APIRouter(prefix="/api/v1/matchmaking", tags=["Matchmaking"])


# -- Response helpers ────────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


def _error(message: str, status_code: int = 400):
    return JSONResponse(
        status_code=status_code,
        content={"success": False, "data": None, "message": message},
    )


# -- Endpoints ──────────────────────────────────────────────────────────


@router.post("/play-now", status_code=201)
def join_queue(request: JoinQueueRequest, current_user: dict = Depends(require_user)):
    """
    Join the matchmaking queue for a sport.

    Accepts either sport name (string) or sport_id (int).
    skill_level is case-insensitive (Intermediate == INTERMEDIATE).
    region_id is injected from authenticated user's profile.
    """
    region_id = current_user.get("region_id")
    if not region_id:
        # Fallback: try to resolve from user's city string for accounts created before region_id was set
        city = current_user.get("city")
        if city:
            db = SessionLocal()
            try:
                from modules.location.model.location_model import Location
                loc = db.query(Location).filter(Location.name.ilike(city)).first()
                if loc:
                    region_id = loc.id
            finally:
                db.close()
    if not region_id:
        return _error("Your account has no region set. Please update your profile.")

    # Resolve sport_id (cart_type_id) from name if only name was provided
    sport_id = request.sport_id
    if sport_id is None and request.sport:
        db = SessionLocal()
        try:
            cart_type = db.query(CartType).filter(CartType.name.ilike(request.sport)).first()
            if not cart_type:
                return _error(f"Unknown sport: {request.sport}. Check admin app for available sports.")
            sport_id = cart_type.id
        finally:
            db.close()
    if sport_id is None:
        return _error("Provide either sport (name) or sport_id.")

    # Normalise skill_level casing — mobile sends "Intermediate", service expects "INTERMEDIATE"
    skill_level = request.skill_level.upper()

    try:
        result = matchmaking_service.join_queue(
            user_id=current_user["id"],
            region_id=region_id,
            sport_id=sport_id,
            skill_level=skill_level,
            instant_captain=request.instant_captain,
        )
    except ValueError as e:
        status_code = 503 if "No captains are available" in str(e) else 400
        return _error(str(e), status_code)

    entry = result["entry"]
    mode = result.get("mode", "STRANGER_MATCH")
    match_id = result.get("match_id")

    data: dict = {
        "entry_id": entry["id"],
        "in_queue": True,
        "match_found": match_id is not None,
        "match_id": match_id,
        "players_searching": result["players_searching"],
        "estimated_wait_seconds": result["estimated_wait_seconds"],
        "mode": mode,
    }
    if mode == "INSTANT_CAPTAIN":
        data["captain_id"] = result["captain_id"]

    return _success(data, "Captain assigned! Head to your ground." if mode == "INSTANT_CAPTAIN" else "Joined queue.")


@router.delete("/leave")
def leave_queue(current_user: dict = Depends(require_user)):
    """
    Leave (cancel) the current user's active queue entry.

    - Returns the cancelled QueueEntry dict.
    - 400 if user has no active queue entry.
    """
    try:
        updated = matchmaking_service.leave_queue(user_id=current_user["id"])
    except ValueError as e:
        return _error(str(e))

    return _success(updated, "You have left the queue.")


@router.get("/status")
def queue_status(current_user: dict = Depends(require_user)):
    """
    Poll the current user's queue position and updated wait time.

    - Returns entry, players_searching, estimated_wait_seconds, and current pricing.
    - 400 if user has no active queue entry.
    """
    try:
        result = matchmaking_service.get_queue_status(user_id=current_user["id"])
    except ValueError as e:
        return _error(str(e))

    entry = result["entry"]
    match_id = result.get("match_id")
    data = {
        "entry_id": entry["id"],
        "in_queue": True,
        "match_found": match_id is not None,
        "match_id": match_id,
        "players_searching": result["players_searching"],
        "estimated_wait_seconds": result["estimated_wait_seconds"],
    }

    return _success(data, "Queue status retrieved.")
