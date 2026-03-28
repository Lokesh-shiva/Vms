from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_user
from modules.matchmaking.service.matchmaking_service import matchmaking_service
from modules.matchmaking.schemas.matchmaking_schema import JoinQueueRequest


router = APIRouter(prefix="/api/v1/matchmaking", tags=["Matchmaking"])


# -- Response helper ────────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# -- Endpoints ──────────────────────────────────────────────────────────

@router.post("/play-now", status_code=201)
def join_queue(request: JoinQueueRequest, current_user: dict = Depends(require_user)):
    """
    Join the matchmaking queue for a sport.

    - sport_id and skill_level supplied by client.
    - region_id injected from authenticated user's profile.
    - Returns entry details, dynamic pricing, and estimated wait time.
    """
    region_id = current_user.get("region_id")
    if not region_id:
        raise HTTPException(
            status_code=400,
            detail="Your account has no region set. Please update your profile before joining a match."
        )

    try:
        result = matchmaking_service.join_queue(
            user_id=current_user["id"],
            region_id=region_id,
            sport_id=request.sport_id,
            skill_level=request.skill_level,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    entry = result["entry"]
    data = {
        "entry_id": entry["id"],
        "user_id": entry["user_id"],
        "region_id": entry["region_id"],
        "sport_id": entry["sport_id"],
        "skill_level": entry["skill_level"],
        "status": entry["status"],
        "players_searching": result["players_searching"],
        "estimated_wait_seconds": result["estimated_wait_seconds"],
        "pricing": result["pricing"],
        "created_at": entry["created_at"],
    }
    return _success(data, "You have joined the queue. Looking for a match...")


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
        raise HTTPException(status_code=400, detail=str(e))

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
        raise HTTPException(status_code=400, detail=str(e))

    entry = result["entry"]
    data = {
        "entry_id": entry["id"],
        "user_id": entry["user_id"],
        "region_id": entry["region_id"],
        "sport_id": entry["sport_id"],
        "skill_level": entry["skill_level"],
        "status": entry["status"],
        "players_searching": result["players_searching"],
        "estimated_wait_seconds": result["estimated_wait_seconds"],
        "pricing": result["pricing"],
        "created_at": entry["created_at"],
    }
    return _success(data, "Queue status retrieved.")
