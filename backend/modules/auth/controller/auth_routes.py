from fastapi import APIRouter, Depends, HTTPException
from modules.auth.service.auth_service import AuthService
from modules.auth.schemas.auth_schema import RegisterSchema, LoginSchema
from modules.auth.dependencies.auth_dependencies import get_current_user


router = APIRouter(prefix="/api/v1/auth", tags=["Auth"])

auth_service = AuthService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.get("/me")
def get_me(current_user: dict = Depends(get_current_user)):
    """Return the currently authenticated user's profile. Token validation."""
    return _success({
        "id": current_user.get("id"),
        "name": current_user.get("name"),
        "phone": current_user.get("phone"),
        "role": current_user.get("role"),
    }, "Authenticated user retrieved.")


@router.post("/register", status_code=201)
def register(request_data: dict):
    """Register a new user account."""
    schema = RegisterSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        user = auth_service.register_user(schema.validated_data)
        return _success(user, "User registered successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/login")
def login(request_data: dict):
    """Authenticate and receive a JWT access token."""
    schema = LoginSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        token_data = auth_service.login_user(schema.validated_data)
        return _success(token_data, "Login successful.")
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))
