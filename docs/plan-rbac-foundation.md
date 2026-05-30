# Plan: RBAC Foundation — Plixo Control Centre Phase 01

**Feature**: RBAC middleware, role extension, and four-layer enforcement wiring  
**Date**: 2026-05-19  
**Phase**: Phase 01 — RBAC Foundation

---

## Current state (verified)

| Component | Current state |
|-----------|--------------|
| `UserRole` enum | Only `USER = "user"` and `ADMIN = "admin"` |
| `auth_manager.py` | Empty stub — `class AuthManager: pass` |
| `auth_dependencies.py` | Has `get_current_user`, `require_admin` (checks `role == "admin"`), `require_user` |
| JWT payload | Already includes `role` field (set in `auth_service.py`) |
| Login response | Already returns `role` from `/api/v1/auth/login` |
| `TokenManager.kt` | Stores JWT token only — no role |
| `AuthViewModel.kt` | Blocks login unless `role == "admin"` — all 7 Plixo roles are blocked |
| `LoginResponse` model | Already has `role: String? = null` |
| `AppNavigation.kt` | No route guards — flat `login` → `main` only |

---

## What stays the same

- DB schema: `users.role` is already a `String` column — no migration needed
- JWT structure: `role` is already in the payload — no token change needed
- Login endpoint: already returns `role` — no change needed
- All existing screens, repositories, and services that don't depend on role
- `ErrorHandlerMiddleware`, `db_connection.py`, `app_config.py`

---

## Backend changes

### 1. `backend/modules/user/model/user_model.py`
Extend `UserRole` enum to the seven Plixo roles:
```python
class UserRole(str, Enum):
    USER = "user"
    SUPER_ADMIN = "super_admin"
    OPS_MANAGER = "ops_manager"
    GROUND_OWNER = "ground_owner"
    TOURNAMENT_MANAGER = "tournament_manager"
    SUPPORT = "support"
    FINANCE = "finance"
    CSR_PARTNER = "csr_partner"
```
Keep `USER` for backwards compatibility. Remove `ADMIN = "admin"` — it is replaced by `SUPER_ADMIN`.  
**Risk**: Any existing DB rows with `role = "admin"` will no longer match the enum. Add migration shim in startup (see step 2).

### 2. `backend/main.py` — startup migration shim
Add to the `startup()` function after `create_all`:
```python
conn.execute(text(
    "UPDATE users SET role = 'super_admin' WHERE role = 'admin'"
))
conn.commit()
```
This is a one-time idempotent migration. Safe to leave in permanently.

### 3. `backend/core/security/auth_manager.py`
Implement the `AuthManager` class with:
- A `ROLE_PERMISSIONS: dict[str, set[str]]` map covering all 7 roles
- A `has_permission(role: str, permission: str) -> bool` method
- Permission strings (snake_case): `manage_roles`, `view_finance`, `manage_finance`, `view_operations`, `manage_operations`, `view_grounds`, `manage_grounds`, `view_tournaments`, `manage_tournaments`, `view_users`, `manage_users`, `view_config`, `manage_config`, `view_audit_log`, `manage_audit_log`, `view_disputes`, `manage_disputes`, `issue_refund`, `approve_refund`, `view_bookings`, `view_matches`, `view_queue`, `manage_queue`, `assign_captain`, `manage_captain`, `view_csr`, `manage_csr`

Role → permission sets:
- `super_admin`: all permissions
- `ops_manager`: `view_operations`, `manage_operations`, `view_matches`, `view_queue`, `manage_queue`, `view_disputes`, `manage_disputes`, `view_bookings`
- `ground_owner`: `view_grounds` (scoped), `view_bookings` (scoped)
- `tournament_manager`: `view_tournaments`, `manage_tournaments`, `view_matches`
- `support`: `view_users`, `view_bookings`, `view_matches`, `view_disputes`, `manage_disputes`, `issue_refund`
- `finance`: `view_finance`, `manage_finance`, `approve_refund`, `issue_refund`, `view_bookings`
- `csr_partner`: `view_csr`, `view_tournaments`

### 4. `backend/modules/auth/dependencies/auth_dependencies.py`
Add two new factory dependencies (keep `get_current_user`, `require_admin`, `require_user` intact for now — do not break existing routes):

```python
def require_role(*allowed_roles: UserRole):
    """Factory: returns a dependency that enforces role membership."""
    def dependency(current_user: dict = Depends(get_current_user)) -> dict:
        if current_user.get("role") not in [r.value for r in allowed_roles]:
            raise HTTPException(status_code=403, detail="Insufficient role.")
        return current_user
    return dependency

def require_permission(permission: str):
    """Factory: returns a dependency that enforces a named permission."""
    def dependency(current_user: dict = Depends(get_current_user)) -> dict:
        role = current_user.get("role", "")
        if not auth_manager.has_permission(role, permission):
            raise HTTPException(status_code=403, detail=f"Permission denied: {permission}")
        return current_user
    return dependency
```

Import `auth_manager` from `core.security.auth_manager`.  
Also import `UserRole` from `modules.user.model.user_model`.

### 5. Apply guards to controllers (highest-risk routes first)

**`backend/modules/payment/controller/payment_routes.py`**  
All write routes: `Depends(require_role(UserRole.FINANCE, UserRole.SUPER_ADMIN))`  
Read routes: also include `OPS_MANAGER`, `SUPPORT`

**`backend/modules/admin/controller/admin_routes.py`**  
Verify what's in this file. Apply `require_role(UserRole.SUPER_ADMIN)` or `OPS_MANAGER` depending on action.

**`backend/modules/payment/model/system_config_model.py` routes (if separate)**  
`manage_config` permission → `SUPER_ADMIN` only.

**Other controllers**: leave existing `require_admin` in place for now. Full sweep is a follow-on task.

---

## Admin app changes

### 6. `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/TokenManager.kt`
Add `ROLE_KEY` alongside `JWT_TOKEN_KEY`:
```kotlin
private val ROLE_KEY = stringPreferencesKey("user_role")

val roleFlow: Flow<String?> = context.dataStore.data.map { it[ROLE_KEY] }

suspend fun saveRole(role: String) {
    context.dataStore.edit { it[ROLE_KEY] = role }
}

suspend fun clearRole() {
    context.dataStore.edit { it.remove(ROLE_KEY) }
}
```
Update `clearToken()` to also call `clearRole()` (or combine into a single `clearSession()`).

### 7. `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/AuthViewModel.kt`
Changes:
- After successful login, call `tokenManager.saveRole(response.data.role ?: "")` alongside `saveToken`.
- Change the role-gate from `role != "admin"` to a check that allows all 7 Plixo admin roles:
  ```kotlin
  val adminRoles = setOf("super_admin","ops_manager","ground_owner","tournament_manager","support","finance","csr_partner")
  if (response.data.role == null || response.data.role !in adminRoles) {
      _loginState.value = LoginState.Error("Access denied: this account does not have admin access.")
      return@launch
  }
  ```
- Add `currentRole: StateFlow<String?>` backed by `tokenManager.roleFlow`.
- Pass `currentRole` out so `AppNavigation` can consume it.

### 8. `Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt`
Changes:
- Accept `currentRole: StateFlow<String>` (or `String`) as parameter.
- Pass role down to `MainScreen`.
- Add a `"forbidden"` composable destination pointing at `ForbiddenScreen`.
- Gate `"main"` composable: if role is not in admin set on navigation, redirect to `"forbidden"`.

### 9. `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/ForbiddenScreen.kt` (new file)
Minimal screen: "You don't have permission to view this." + logout button.

### 10. `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/MainScreen.kt`
- Accept `role: String` parameter (passed from `AppNavigation`).
- Wrap drawer items and nav tabs in `if (role in setOf(...))` guards per the rbac-roles spec.
- Finance tab: only `FINANCE`, `SUPER_ADMIN`.
- Configurations/Permissions: only `SUPER_ADMIN`.
- Operations: `OPS_MANAGER`, `SUPER_ADMIN`.
- Grounds: `GROUND_OWNER`, `OPS_MANAGER`, `SUPER_ADMIN`.

---

## DB schema changes

None. `users.role` is already `String`. The startup migration shim (step 2) renames existing `"admin"` rows to `"super_admin"`.

---

## API changes

None. `/api/v1/auth/login` already returns `role`. `/api/v1/auth/me` already returns `role`. No new endpoints in this task.

---

## Test cases to write

| # | Test | Expected |
|---|------|----------|
| 1 | Login with a `super_admin` user | Token returned, role = `super_admin`, app enters main |
| 2 | Login with a `finance` user | Token returned, role = `finance`, app enters main |
| 3 | Login with a `user` (non-admin) | App shows "Access denied" error, no navigation |
| 4 | Call payment write endpoint as `ops_manager` | 403 returned |
| 5 | Call payment write endpoint as `finance` | 200 returned |
| 6 | Call config endpoint as `ops_manager` | 403 returned |
| 7 | Call config endpoint as `super_admin` | 200 returned |
| 8 | `auth_manager.has_permission("finance", "approve_refund")` | True |
| 9 | `auth_manager.has_permission("support", "manage_config")` | False |
| 10 | `require_role(UserRole.FINANCE)(user_with_ops_role)` | HTTPException 403 |

---

## Implementation order (dependencies first)

1. **`user_model.py`** — extend `UserRole` enum (no other file depends on new values yet)
2. **`main.py`** — add startup migration shim (`admin` → `super_admin`)
3. **`auth_manager.py`** — implement `AuthManager` with permission map
4. **`auth_dependencies.py`** — add `require_role()` and `require_permission()` factories
5. **`payment_routes.py`** — apply guards (verify file first, then patch)
6. **`admin_routes.py`** — apply guards (verify file first, then patch)
7. **`TokenManager.kt`** — add role storage
8. **`AuthViewModel.kt`** — save role, expose `currentRole`, fix role gate
9. **`AppNavigation.kt`** — accept role, add `ForbiddenScreen` route
10. **`ForbiddenScreen.kt`** — new file
11. **`MainScreen.kt`** — role-conditional drawer and tabs
12. **`DEV_LOG.md`** — append entry

---

## Dependency risks

- `require_admin` is currently used on routes. Do NOT remove it until all controllers are migrated to `require_role`. Keep both in `auth_dependencies.py` during transition.
- Existing `role = "admin"` DB rows: the startup shim handles this. Any test fixtures that create `admin` users need updating.
- `AuthViewModel` login gate change: the old `role != "admin"` check gates `LoginState.Success`. Changing it means anyone with a new Plixo role can reach `MainScreen`. Ensure `MainScreen` role guards land in the same PR.

---

## DEV_LOG entry (draft)

```
## 2026-05-19 | Phase 01 | RBAC Foundation

### Added
- `UserRole` extended to 7 Plixo roles (super_admin, ops_manager, ground_owner, tournament_manager, support, finance, csr_partner)
- `auth_manager.py` — AuthManager with role→permission map, has_permission()
- `auth_dependencies.py` — require_role() and require_permission() FastAPI dependency factories
- `ForbiddenScreen.kt` — denial screen for unauthorized navigation
- `TokenManager.kt` — role persistence alongside JWT token

### Modified
- `backend/main.py` — startup shim: renames role='admin' → 'super_admin' 
- `backend/modules/payment/controller/payment_routes.py` — require_role guards on write endpoints
- `backend/modules/auth/dependencies/auth_dependencies.py` — new factories added
- `Vmsadminapp/.../AuthViewModel.kt` — saves role, exposes currentRole, updated login gate
- `Vmsadminapp/.../AppNavigation.kt` — role parameter, forbidden route
- `Vmsadminapp/.../MainScreen.kt` — conditional nav items per role

### Architecture decisions
- Kept require_admin() in auth_dependencies.py during transition to avoid breaking existing routes. Will remove in follow-on once all controllers use require_role().
- Chose permission strings over integer bitmask: readable, extensible, no DB column needed.
- Role stored in DataStore alongside token: avoids re-decoding JWT on every screen load.
```
