from fastapi import APIRouter, Depends, HTTPException
from modules.audit.service.audit_service import audit_service
from modules.auth.dependencies.auth_dependencies import require_role, require_user
from modules.tournament.service.tournament_match_service import tournament_match_service
from modules.tournament.service.tournament_standing_service import tournament_standing_service
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/tournaments", tags=["Tournament Matches"])

_MANAGER_ROLES = (UserRole.TOURNAMENT_MANAGER, UserRole.SUPER_ADMIN)


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/{tournament_id}/matches")
def list_matches(
    tournament_id: int,
    current_user: dict = Depends(require_user),
):
    return _success(tournament_match_service.list_matches(tournament_id))


@router.post("/{tournament_id}/matches", status_code=201)
def create_match(
    tournament_id: int,
    request_data: dict,
    current_user: dict = require_role(*_MANAGER_ROLES),
):
    try:
        m = tournament_match_service.create_match(tournament_id, request_data)
        return _success(m, "Match scheduled.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/{tournament_id}/matches/{match_id}/result")
def record_result(
    tournament_id: int,
    match_id: int,
    request_data: dict,
    current_user: dict = require_role(*_MANAGER_ROLES),
):
    try:
        result = tournament_match_service.record_result(
            tournament_id=tournament_id,
            match_id=match_id,
            home_score=request_data["home_score"],
            away_score=request_data["away_score"],
            overrides=request_data.get("overrides"),
        )
        audit_service.log(
            action="TOURNAMENT_MATCH_RESULT_RECORDED",
            actor_user_id=current_user["id"],
            target_resource_type="tournament_match",
            target_resource_id=match_id,
            details={
                "tournament_id": tournament_id,
                "home_score": request_data["home_score"],
                "away_score": request_data["away_score"],
            },
        )
        return _success(result, "Result recorded.")
    except (ValueError, KeyError) as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{tournament_id}/standings")
def get_standings(
    tournament_id: int,
    current_user: dict = Depends(require_user),
):
    return _success(tournament_standing_service.get_standings(tournament_id))
