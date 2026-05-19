from typing import Callable

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt

from modules.auth.service.auth_service import SECRET_KEY, ALGORITHM
from modules.user.model.user_model import UserRole
from modules.user.repository.user_repository import user_repository
from core.security.auth_manager import auth_manager

security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> dict:
    """Decode JWT, then verify the user still exists and is active."""
    token = credentials.credentials
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token.",
        )

    user_id_raw = payload.get("sub")
    if user_id_raw is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token.",
        )
    try:
        user_id = int(user_id_raw)
    except (TypeError, ValueError):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token.",
        )

    user = user_repository.find_by_id(user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User no longer exists.",
        )

    if not user.get("is_active", False):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account is deactivated.",
        )

    return user


_ADMIN_ROLES: frozenset[str] = frozenset(
    {
        UserRole.SUPER_ADMIN.value,
        UserRole.OPS_MANAGER.value,
        UserRole.GROUND_OWNER.value,
        UserRole.TOURNAMENT_MANAGER.value,
        UserRole.SUPPORT.value,
        UserRole.FINANCE.value,
        UserRole.CSR_PARTNER.value,
    }
)

_ALL_AUTHENTICATED_ROLES: frozenset[str] = _ADMIN_ROLES | {UserRole.USER.value}


def require_admin(current_user: dict = Depends(get_current_user)) -> dict:
    """Restrict access to any recognised admin role."""
    if current_user.get("role") not in _ADMIN_ROLES:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin privileges required.",
        )
    return current_user


def require_user(current_user: dict = Depends(get_current_user)) -> dict:
    """Restrict access to authenticated users (any role: regular user or admin)."""
    if current_user.get("role") not in _ALL_AUTHENTICATED_ROLES:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Valid user role required.",
        )
    return current_user


def require_role(*allowed_roles: UserRole) -> Callable:
    """Factory: returns a dependency that enforces role membership."""
    _role_values = {r.value for r in allowed_roles}

    def dependency(current_user: dict = Depends(get_current_user)) -> dict:
        if current_user.get("role") not in _role_values:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Insufficient role.",
            )
        return current_user

    return Depends(dependency)


def require_permission(permission: str) -> Callable:
    """Factory: returns a dependency that enforces a named permission."""

    def dependency(current_user: dict = Depends(get_current_user)) -> dict:
        role = current_user.get("role", "")
        if not auth_manager.has_permission(role, permission):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You do not have permission to perform this action.",
            )
        return current_user

    return Depends(dependency)
