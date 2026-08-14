from datetime import datetime

from fastapi import APIRouter, HTTPException
from modules.auth.dependencies.auth_dependencies import require_role
from modules.tournament.service.sport_vote_service import sport_vote_service
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/admin/vote-rounds", tags=["Admin Sport Voting"])

_MANAGER_ROLES = (UserRole.TOURNAMENT_MANAGER, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN)


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/current")
def get_current_round(current_user: dict = require_role(*_MANAGER_ROLES)):
    """Admin view of the current round's options, deadline, and live tallies."""
    return _success(sport_vote_service.get_admin_state())


@router.post("", status_code=201)
def create_round(body: dict, current_user: dict = require_role(*_MANAGER_ROLES)):
    """Start a new voting round — replaces the current one (its history is kept,
    just no longer shown to users)."""
    try:
        closes_at_raw = body.get("closes_at")
        closes_at = datetime.fromisoformat(str(closes_at_raw)) if closes_at_raw else None
        result = sport_vote_service.create_round(body.get("options"), closes_at)
        return _success(result, "Vote round started.")
    except (ValueError, TypeError) as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{round_id}/close")
def close_round(round_id: int, current_user: dict = require_role(*_MANAGER_ROLES)):
    """Force-close a round before its deadline."""
    try:
        result = sport_vote_service.close_round(round_id)
        return _success(result, "Vote round closed.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
