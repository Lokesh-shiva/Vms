from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from modules.auth.dependencies.auth_dependencies import require_role, require_user
from modules.user.model.user_model import User, UserRole
from modules.society.service.society_service import society_service
from modules.society.service.society_member_service import society_member_service
from modules.society.service.society_tournament_service import society_tournament_service
from modules.society.schemas.society_schema import CreateSocietySchema, UpdateSocietySchema
from core.database.db_connection import get_db

router = APIRouter(prefix="/api/v1/societies", tags=["Societies"])

_ADMIN_BYPASS = frozenset({UserRole.SUPER_ADMIN.value, UserRole.OPS_MANAGER.value})


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.post("/", status_code=201)
def create_society(
    body: CreateSocietySchema,
    current_user: dict = Depends(require_user),
    db: Session = Depends(get_db),
):
    requester_role = current_user["role"]
    can_create = False

    if requester_role in _ADMIN_BYPASS:
        can_create = True
    else:
        user_row = db.query(User).filter(User.id == current_user["id"]).first()
        can_create = bool(user_row and user_row.can_create_society)

    # Admin can optionally specify a different owner; regular users always own their own society
    data = body.model_dump()
    if requester_role in _ADMIN_BYPASS and data.get("owner_user_id"):
        effective_owner = data.pop("owner_user_id")
    else:
        data.pop("owner_user_id", None)
        effective_owner = current_user["id"]

    try:
        result = society_service.create(
            data,
            owner_user_id=effective_owner,
            requester_role=requester_role,
            can_create_society=can_create,
        )
        return _success(result, "Society created.")
    except ValueError as e:
        code = 403 if "permission" in str(e).lower() else 400
        raise HTTPException(status_code=code, detail=str(e))


@router.get("/")
def list_societies(
    region_id: int | None = Query(default=None),
    sport_id: int | None = Query(default=None),
    current_user: dict = Depends(require_user),
):
    return _success(society_service.list(region_id=region_id, sport_id=sport_id))


@router.get("/mine")
def get_my_societies(current_user: dict = Depends(require_user)):
    """Return societies the current user belongs to — used for the captain society picker."""
    return _success(society_member_service.get_my_societies(current_user["id"]))


@router.get("/{society_id}")
def get_society(
    society_id: int,
    current_user: dict = Depends(require_user),
):
    try:
        return _success(society_service.get_by_id(society_id))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.put("/{society_id}")
def update_society(
    society_id: int,
    body: UpdateSocietySchema,
    current_user: dict = Depends(require_user),
):
    try:
        result = society_service.update(
            society_id,
            body.model_dump(exclude_none=True),
            requester_id=current_user["id"],
            requester_role=current_user["role"],
        )
        return _success(result, "Society updated.")
    except ValueError as e:
        status = 403 if "Permission denied" in str(e) else 400
        raise HTTPException(status_code=status, detail=str(e))


@router.delete("/{society_id}")
def delete_society(
    society_id: int,
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    try:
        society_service.delete(society_id, requester_role=current_user["role"])
        return _success(None, "Society deleted.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{society_id}/deactivate")
def deactivate_society(
    society_id: int,
    current_user: dict = require_role(UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER),
):
    try:
        result = society_service.deactivate(society_id, requester_role=current_user["role"])
        return _success(result, "Society deactivated.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{society_id}/join", status_code=201)
def join_society(
    society_id: int,
    current_user: dict = Depends(require_user),
):
    try:
        result = society_member_service.join(society_id, user_id=current_user["id"])
        return _success(result, "Joined society.")
    except ValueError as e:
        status = 409 if "Already a member" in str(e) else 400
        raise HTTPException(status_code=status, detail=str(e))


@router.delete("/{society_id}/leave")
def leave_society(
    society_id: int,
    current_user: dict = Depends(require_user),
):
    try:
        society_member_service.leave(society_id, user_id=current_user["id"])
        return _success(None, "Left society.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{society_id}/members/{member_user_id}")
def kick_member(
    society_id: int,
    member_user_id: int,
    current_user: dict = Depends(require_user),
):
    try:
        society_member_service.kick(
            society_id,
            target_user_id=member_user_id,
            requester_id=current_user["id"],
            requester_role=current_user["role"],
        )
        return _success(None, "Member removed.")
    except ValueError as e:
        status = 403 if "Permission denied" in str(e) else 400
        raise HTTPException(status_code=status, detail=str(e))


@router.post("/{society_id}/transfer-owner")
def transfer_owner(
    society_id: int,
    request_body: dict,
    current_user: dict = Depends(require_user),
):
    try:
        new_owner_id = request_body.get("new_owner_id")
        if not new_owner_id:
            raise HTTPException(status_code=400, detail="new_owner_id is required.")
        result = society_member_service.transfer_ownership(
            society_id,
            new_owner_id=new_owner_id,
            requester_id=current_user["id"],
        )
        return _success(result, "Ownership transferred.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{society_id}/members")
def list_members(
    society_id: int,
    current_user: dict = Depends(require_user),
):
    try:
        return _success(society_member_service.get_members(society_id))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/{society_id}/leaderboard")
def get_leaderboard(
    society_id: int,
    current_user: dict = Depends(require_user),
):
    try:
        return _success(society_member_service.get_leaderboard(society_id))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/{society_id}/matches")
def get_society_matches(
    society_id: int,
    current_user: dict = Depends(require_user),
):
    """List WAITING/MATCHED matches for a society. Members only."""
    member = society_member_service.member_repository.find_member(society_id, current_user["id"])
    if member is None:
        raise HTTPException(status_code=403, detail="You are not a member of this society.")

    from modules.match.repository.match_repository import match_repository
    return _success(match_repository.find_society_matches(society_id))


@router.post("/{society_id}/tournament-register", status_code=201)
def tournament_register(
    society_id: int,
    request_body: dict,
    current_user: dict = Depends(require_user),
):
    try:
        result = society_tournament_service.register_as_team(
            society_id=society_id,
            tournament_id=request_body["tournament_id"],
            member_ids=request_body.get("member_ids", []),
            requester_id=current_user["id"],
        )
        return _success(result, "Society registered as tournament team.")
    except (ValueError, KeyError) as e:
        raise HTTPException(status_code=400, detail=str(e))
