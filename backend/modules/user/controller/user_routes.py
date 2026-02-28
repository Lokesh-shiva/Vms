from fastapi import APIRouter, Depends, HTTPException
from modules.user.service.user_service import UserService
from modules.user.schemas.user_schema import CreateUserSchema, UpdateUserSchema
from modules.auth.dependencies.auth_dependencies import (
    get_current_user,
    require_admin,
)


router = APIRouter(prefix="/api/v1/users", tags=["Users"])

user_service = UserService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

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
    if current_user["role"] == "admin":
        users = user_service.list_users()
    else:
        own = user_service.get_user(current_user["id"])
        users = [own] if own else []
    return _success(users)


@router.get("/{user_id}")
def get_user(user_id: int, current_user: dict = Depends(get_current_user)):
    """Retrieve a user by ID. Regular users can only view themselves."""
    if current_user["role"] != "admin" and current_user["id"] != user_id:
        raise HTTPException(status_code=403, detail="You can only view your own profile.")
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
    if current_user["role"] != "admin" and current_user["id"] != user_id:
        raise HTTPException(status_code=403, detail="You can only update your own profile.")
    if "role" in request_data and current_user["role"] != "admin":
        raise HTTPException(status_code=403, detail="Only admins can change user roles.")

    schema = UpdateUserSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        user = user_service.update_user(user_id, schema.validated_data, current_user=current_user)
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
