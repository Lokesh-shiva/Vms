from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_user
from modules.tournament.service.sport_vote_service import sport_vote_service

router = APIRouter(prefix="/api/v1/tournaments", tags=["Sport Voting"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/votes")
def get_votes(current_user: dict = Depends(require_user)):
    """Current results + the caller's own vote for their region."""
    try:
        return _success(sport_vote_service.get_state(current_user["id"]))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/votes")
def cast_vote(body: dict, current_user: dict = Depends(require_user)):
    """Cast (or change) the caller's vote for which sport the next city-wide
    tournament in their region should be."""
    try:
        result = sport_vote_service.cast_vote(current_user["id"], body.get("sport"))
        return _success(result, "Vote recorded.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
