from fastapi import APIRouter, Depends, HTTPException, Query
from modules.user.service.user_service import UserService
from modules.user.schemas.user_schema import CreateUserSchema, UpdateUserSchema
from modules.user.model.user_model import User, UserRole
from modules.auth.dependencies.auth_dependencies import (
    _ADMIN_ROLES,
    get_current_user,
    require_admin,
    require_role,
)
from core.database.db_connection import get_db
from sqlalchemy.orm import Session


router = APIRouter(prefix="/api/v1/users", tags=["Users"])

user_service = UserService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.get("/search")
def search_user_by_phone(
    phone: str = Query(..., description="Phone number to search"),
    current_user: dict = require_role(UserRole.SUPER_ADMIN, UserRole.SUPPORT),
    db: Session = Depends(get_db),
):
    """Search for a user by exact phone number. Restricted to SUPER_ADMIN and SUPPORT."""
    user = db.query(User).filter(User.phone == phone).first()
    if not user:
        raise HTTPException(
            status_code=404,
            detail={"success": False, "message": "User not found"},
        )
    return _success(user.to_dict())


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_user(request_data: dict):
    """Create a new user. Requires admin role."""
    schema = CreateUserSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        user = user_service.create_user(schema.validated_data)
        return _success(user, "User created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_users(current_user: dict = Depends(get_current_user)):
    """Retrieve users. Admins see all; regular users see only themselves."""
    if current_user["role"] in _ADMIN_ROLES:
        users = user_service.list_users()
    else:
        own = user_service.get_user(current_user["id"])
        users = [own] if own else []
    return _success(users)


@router.get("/{user_id}")
def get_user(user_id: int, current_user: dict = Depends(get_current_user)):
    """Retrieve a user by ID. Regular users can only view themselves."""
    if current_user["role"] not in _ADMIN_ROLES and current_user["id"] != user_id:
        raise HTTPException(
            status_code=403, detail="You can only view your own profile."
        )
    user = user_service.get_user(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")
    return _success(user)


@router.put("/{user_id}")
def update_user(
    user_id: int,
    request_data: dict,
    current_user: dict = Depends(get_current_user),
):
    """Update a user. Regular users can update own non-role fields only."""
    caller_id: int = current_user["id"]
    caller_role: str = current_user["role"]

    # Guard 1: Self-lockout — block any attempt to change own role or deactivate self.
    if caller_id == user_id and (
        "role" in request_data or request_data.get("is_active") is False
    ):
        raise HTTPException(
            status_code=403,
            detail="Cannot change your own role or deactivate yourself.",
        )

    # Guard 2: Role-change — only SUPER_ADMIN may assign/change roles.
    # TODO(phase01-audit): emit audit event here — role change
    if "role" in request_data and caller_role != UserRole.SUPER_ADMIN.value:
        raise HTTPException(
            status_code=403, detail="Only super admins can change user roles."
        )

    # Guard 3: Deactivation — only SUPER_ADMIN may set is_active=False.
    # TODO(phase01-audit): emit audit event here — deactivation
    if (
        request_data.get("is_active") is False
        and caller_role != UserRole.SUPER_ADMIN.value
    ):
        raise HTTPException(
            status_code=403, detail="Only super admins can deactivate users."
        )

    # Guard 4: Ownership — non-admin callers may only touch their own record.
    if caller_role not in _ADMIN_ROLES and caller_id != user_id:
        raise HTTPException(
            status_code=403, detail="You can only update your own profile."
        )

    schema = UpdateUserSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        user = user_service.update_user(
            user_id, schema.validated_data, current_user=current_user
        )
        if not user:
            raise HTTPException(status_code=404, detail="User not found.")
        return _success(user, "User updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{user_id}")
def delete_user(user_id: int, current_user: dict = Depends(require_admin)):
    """Delete a user by ID. Requires admin role. Self-deletion blocked."""
    try:
        deleted = user_service.delete_user(user_id, current_user=current_user)
        if not deleted:
            raise HTTPException(status_code=404, detail="User not found.")
        return _success(None, "User deleted successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
