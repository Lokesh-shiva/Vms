from fastapi import APIRouter, HTTPException
from modules.user.service.user_service import UserService
from modules.user.schemas.user_schema import CreateUserSchema, UpdateUserSchema


router = APIRouter(prefix="/api/v1/users", tags=["Users"])

user_service = UserService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.post("", status_code=201)
def create_user(request_data: dict):
    """Create a new user."""
    schema = CreateUserSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        user = user_service.create_user(schema.validated_data)
        return _success(user, "User created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_users():
    """Retrieve all users."""
    users = user_service.list_users()
    return _success(users)


@router.get("/{user_id}")
def get_user(user_id: int):
    """Retrieve a user by ID."""
    user = user_service.get_user(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")
    return _success(user)


@router.put("/{user_id}")
def update_user(user_id: int, request_data: dict):
    """Update an existing user."""
    schema = UpdateUserSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        user = user_service.update_user(user_id, schema.validated_data)
        if not user:
            raise HTTPException(status_code=404, detail="User not found.")
        return _success(user, "User updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{user_id}")
def delete_user(user_id: int):
    """Delete a user by ID."""
    deleted = user_service.delete_user(user_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="User not found.")
    return _success(None, "User deleted successfully.")
