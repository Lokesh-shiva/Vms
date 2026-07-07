from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_role, require_user
from modules.tournament.service.tournament_service import TournamentService
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/tournaments", tags=["Tournament Registration"])
_service = TournamentService()

_MANAGER_ROLES = (UserRole.TOURNAMENT_MANAGER, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN)


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/{tournament_id}/registrations")
def list_registrations(
    tournament_id: int,
    current_user: dict = require_role(*_MANAGER_ROLES),
):
    """Admin/manager view of who's registered for a tournament."""
    try:
        return _success(_service.list_registrations(tournament_id))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{tournament_id}/register", status_code=201)
def register(
    tournament_id: int,
    request_data: dict = None,
    current_user: dict = Depends(require_user),
):
    """Register the current user (individual) or a team in a tournament."""
    try:
        result = _service.register(
            tournament_id=tournament_id,
            user_id=current_user["id"],
            team_data=request_data or {},
        )
        return _success(result, "Registered successfully.")
    except ValueError as e:
        status_code = 409 if "already registered" in str(e) else 400
        raise HTTPException(status_code=status_code, detail=str(e))


@router.delete("/{tournament_id}/register")
def withdraw(
    tournament_id: int,
    current_user: dict = Depends(require_user),
):
    """Withdraw the current user from a tournament."""
    try:
        result = _service.withdraw(tournament_id=tournament_id, user_id=current_user["id"])
        return _success(result, "Withdrawn successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
