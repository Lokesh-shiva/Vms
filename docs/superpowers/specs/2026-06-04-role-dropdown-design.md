# Role Change Dropdown — Design Spec

**Date:** 2026-06-04
**Phase:** 02
**Feature:** Backend-driven assignable-roles dropdown in UsersScreen

---

## Goal

Replace the hardcoded 8-role list in the UsersScreen role-change dialog with a backend-driven list filtered by the logged-in user's role. Role changes continue to persist to the database via the existing `PUT /api/v1/users/{id}` path.

## Architecture

```
GET /api/v1/users/assignable-roles
        ↓
UserManagementViewModel.loadAssignableRoles()
        ↓
_assignableRoles: StateFlow<List<String>>
        ↓
UsersScreen radio-button dialog
```

Role changes (on confirm) continue through the existing path:
```
changeRole(id, role) → repository.updateRole() → PUT /api/v1/users/{id} → DB
```

---

## Backend

### New endpoint: `GET /api/v1/users/assignable-roles`

**File:** `backend/modules/user/controller/user_routes.py`

- Protected by `get_current_user` dependency (any authenticated user can call it)
- Reads `current_user["role"]` from JWT
- Returns `{"assignable_roles": [...]}`

**Role matrix:**

| Caller role   | Assignable roles                                                                 |
|---------------|---------------------------------------------------------------------------------|
| super_admin   | super_admin, ops_manager, ground_owner, tournament_manager, support, finance, csr_partner, user |
| All others    | [] (empty — screen is already blocked by 4-layer guard, but clean response)     |

**Response schema:**
```json
{ "assignable_roles": ["super_admin", "ops_manager", "ground_owner", "tournament_manager", "support", "finance", "csr_partner", "user"] }
```

No new model or service method needed — pure controller logic.

---

## Admin App

### Models.kt
Add:
```kotlin
data class AssignableRolesResponse(val assignable_roles: List<String>)
```

### ApiService.kt
Add:
```kotlin
@GET("users/assignable-roles")
suspend fun getAssignableRoles(): AssignableRolesResponse
```

### UserManagementViewModel.kt
Add:
```kotlin
private val _assignableRoles = MutableStateFlow<List<String>>(emptyList())
val assignableRoles: StateFlow<List<String>> = _assignableRoles.asStateFlow()

private fun loadAssignableRoles() {
    viewModelScope.launch {
        try {
            val response = repository.getAssignableRoles()
            _assignableRoles.value = response.assignable_roles
        } catch (e: Exception) {
            // Non-fatal — dialog will show empty list (no role change possible)
        }
    }
}
```

Called in `init { loadUsers(); loadAssignableRoles() }`.

`UserManagementRepository` gets a matching `getAssignableRoles()` method that calls `ApiService`.

### UsersScreen.kt
In the role-change dialog, replace:
```kotlin
val roles = listOf("super_admin", "ops_manager", ...)
```
with:
```kotlin
val assignableRoles by viewModel.assignableRoles.collectAsState()
```

If `assignableRoles` is empty: show a `Text("No roles available")` instead of the radio list (graceful empty state).

---

## Testing

**Backend:**
- `GET /assignable-roles` with SUPER_ADMIN token → 200, all 8 roles
- `GET /assignable-roles` with OPS_MANAGER token → 200, empty list
- `GET /assignable-roles` with no token → 401

**App (manual):**
- Log in as SUPER_ADMIN → open Users → tap role on any user → dialog shows 8 radio options
- Change role → confirm → user's role updates in list (DB persisted)
- Log in as other role → Users screen is unreachable (existing guard)

---

## Files Changed

**Backend:**
- `backend/modules/user/controller/user_routes.py` — add `GET /assignable-roles` route

**Admin app:**
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt` — add `AssignableRolesResponse`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt` — add `getAssignableRoles()`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/UserManagementRepository.kt` — add `getAssignableRoles()`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/UserManagementViewModel.kt` — add `_assignableRoles` flow + `loadAssignableRoles()`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/UsersScreen.kt` — consume `assignableRoles` from ViewModel

---

## Constraints

- No changes to the role-change submit path (`changeRole` → `updateRole` → DB)
- No new migrations required
- Backend endpoint must use parameterised role comparison, no string interpolation
- ViewModel failure to fetch roles is non-fatal (empty list, no crash)
