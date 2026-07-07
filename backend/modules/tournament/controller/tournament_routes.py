from fastapi import APIRouter, Depends, HTTPException
from modules.audit.service.audit_service import audit_service
from modules.auth.dependencies.auth_dependencies import require_role, require_user
from modules.tournament.schemas.tournament_schema import CreateTournamentSchema, UpdateTournamentSchema
from modules.tournament.service.tournament_service import TournamentService
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/tournaments", tags=["Tournaments"])
tournament_service = TournamentService()


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_tournaments(
    current_user: dict = require_role(
        UserRole.TOURNAMENT_MANAGER, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN
    ),
):
    return _success(tournament_service.list_tournaments())


@router.post("", status_code=201)
def create_tournament(
    request_data: dict,
    current_user: dict = require_role(UserRole.TOURNAMENT_MANAGER, UserRole.SUPER_ADMIN),
):
    schema = CreateTournamentSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        t = tournament_service.create_tournament(schema.validated_data)
        audit_service.log(
            action="TOURNAMENT_CREATED",
            actor_user_id=current_user["id"],
            target_resource_type="tournament",
            target_resource_id=t["id"],
            details={"name": t["name"]},
        )
        return _success(t, "Tournament created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{tournament_id}")
def get_tournament(
    tournament_id: int,
    current_user: dict = require_role(
        UserRole.TOURNAMENT_MANAGER, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN
    ),
):
    t = tournament_service.get_tournament(tournament_id)
    if not t:
        raise HTTPException(status_code=404, detail="Tournament not found.")
    return _success(t)


@router.put("/{tournament_id}")
def update_tournament(
    tournament_id: int,
    request_data: dict,
    current_user: dict = require_role(UserRole.TOURNAMENT_MANAGER, UserRole.SUPER_ADMIN),
):
    schema = UpdateTournamentSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        t = tournament_service.update_tournament(tournament_id, schema.validated_data)
        audit_service.log(
            action="TOURNAMENT_UPDATED",
            actor_user_id=current_user["id"],
            target_resource_type="tournament",
            target_resource_id=tournament_id,
            details={"fields": list(request_data.keys())},
        )
        return _success(t, "Tournament updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.delete("/{tournament_id}")
def delete_tournament(
    tournament_id: int,
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    try:
        tournament_service.delete_tournament(tournament_id)
        audit_service.log(
            action="TOURNAMENT_DELETED",
            actor_user_id=current_user["id"],
            target_resource_type="tournament",
            target_resource_id=tournament_id,
        )
        return _success(None, "Tournament deleted successfully.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
