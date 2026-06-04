# Role Change Dropdown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hardcoded 8-role list in UsersScreen with a backend-driven assignable-roles list filtered by the logged-in user's role.

**Architecture:** A new `GET /api/v1/users/assignable-roles` endpoint reads the caller's JWT role and returns the roles they are allowed to assign. The ViewModel fetches this list on init and exposes it as a StateFlow. The dialog in UsersScreen consumes the flow instead of the hardcoded list. Role changes continue to hit the DB via the existing `PUT /api/v1/users/{id}` path — no changes there.

**Tech Stack:** Python 3.12 / FastAPI / pytest (backend); Kotlin / Jetpack Compose / Retrofit / StateFlow (admin app)

---

## File Map

| File | Change |
|------|--------|
| `backend/modules/user/controller/user_routes.py` | Add `GET /assignable-roles` route |
| `backend/modules/user/tests/test_user_routes.py` | Add 3 tests for the new endpoint |
| `Vmsadminapp/.../models/Models.kt` | Add `AssignableRolesResponse` data class |
| `Vmsadminapp/.../network/ApiService.kt` | Add `getAssignableRoles()` suspend fun |
| `Vmsadminapp/.../data/UserManagementRepository.kt` | Add `getAssignableRoles()` method |
| `Vmsadminapp/.../viewmodel/UserManagementViewModel.kt` | Add `_assignableRoles` flow + `loadAssignableRoles()` |
| `Vmsadminapp/.../ui/screens/UsersScreen.kt` | Replace hardcoded roles list with ViewModel flow |
| `backend/DEV_LOG.md` | Append Phase 02 entry |

---

## Task 1: Backend — `GET /assignable-roles` endpoint

**Files:**
- Modify: `backend/modules/user/controller/user_routes.py`
- Create: `backend/modules/user/tests/test_user_routes.py`

### Context
- `user_routes.py` prefix is `/api/v1/users`
- Existing imports: `get_current_user`, `require_role`, `UserRole`, `_ADMIN_ROLES`
- Pattern for protected routes: `current_user: dict = Depends(get_current_user)` (any auth'd user) or `require_role(...)` (specific roles)
- `_success(data)` helper already exists in the file — use it

### Role matrix
```
"super_admin"        → all 8 roles (see list below)
anything else        → []
```

All 8 roles in order: `["super_admin", "ops_manager", "ground_owner", "tournament_manager", "support", "finance", "csr_partner", "user"]`

- [ ] **Step 1: Write the failing tests**

Create `backend/modules/user/tests/test_user_routes.py`:

```python
"""
Tests for GET /api/v1/users/assignable-roles.

Uses TestClient with a dependency override to simulate different caller roles
without hitting a real DB or issuing real JWTs.
"""
import pytest
from fastapi.testclient import TestClient

from backend.main import app
from modules.auth.dependencies.auth_dependencies import get_current_user


def _override_role(role: str):
    """Return a dependency override that injects a fake current_user dict."""
    def _dep():
        return {"id": 1, "role": role, "is_active": True}
    return _dep


@pytest.fixture()
def super_admin_client():
    app.dependency_overrides[get_current_user] = _override_role("super_admin")
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture()
def ops_manager_client():
    app.dependency_overrides[get_current_user] = _override_role("ops_manager")
    yield TestClient(app)
    app.dependency_overrides.clear()


def test_super_admin_gets_all_roles(super_admin_client):
    """SUPER_ADMIN receives all 8 assignable roles."""
    resp = super_admin_client.get("/api/v1/users/assignable-roles")
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    roles = data["data"]["assignable_roles"]
    assert set(roles) == {
        "super_admin", "ops_manager", "ground_owner",
        "tournament_manager", "support", "finance", "csr_partner", "user",
    }


def test_non_admin_gets_empty_list(ops_manager_client):
    """Non-SUPER_ADMIN caller gets an empty assignable-roles list."""
    resp = ops_manager_client.get("/api/v1/users/assignable-roles")
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    assert data["data"]["assignable_roles"] == []


def test_unauthenticated_gets_401():
    """No token → 401."""
    client = TestClient(app)
    resp = client.get("/api/v1/users/assignable-roles")
    assert resp.status_code == 401
```

- [ ] **Step 2: Run tests to verify they fail**

```
cd "C:\Users\Lokesh\Desktop\Pojects\Vms project"
venv\Scripts\python.exe -m pytest backend/modules/user/tests/test_user_routes.py -v
```

Expected: 3 FAILs — route does not exist yet (404).

- [ ] **Step 3: Implement the endpoint**

In `backend/modules/user/controller/user_routes.py`, add this route **before** the existing `@router.post("")` route (after the `/search` route):

```python
_ALL_ROLES: list[str] = [
    "super_admin",
    "ops_manager",
    "ground_owner",
    "tournament_manager",
    "support",
    "finance",
    "csr_partner",
    "user",
]


@router.get("/assignable-roles")
def get_assignable_roles(
    current_user: dict = Depends(get_current_user),
):
    """Return the list of roles this caller is allowed to assign.

    SUPER_ADMIN → all 8 roles.
    All other roles → empty list (screen is already blocked by 4-layer guard).
    """
    if current_user.get("role") == UserRole.SUPER_ADMIN.value:
        return _success({"assignable_roles": _ALL_ROLES})
    return _success({"assignable_roles": []})
```

Also add `Depends` to the imports at the top of the file if not already present:
```python
from fastapi import APIRouter, Depends, HTTPException, Query
```
(`Depends` is already imported — no change needed.)

- [ ] **Step 4: Run tests to verify they pass**

```
venv\Scripts\python.exe -m pytest backend/modules/user/tests/test_user_routes.py -v
```

Expected: 3 PASSes.

- [ ] **Step 5: Run full test suite — no regressions**

```
venv\Scripts\python.exe -m pytest --tb=short -q
```

Expected: all previously-passing tests still pass.

- [ ] **Step 6: Commit**

```
git add backend/modules/user/controller/user_routes.py backend/modules/user/tests/test_user_routes.py
git commit -m "feat(backend): GET /users/assignable-roles — role-filtered list"
```

---

## Task 2: Admin app — Models + ApiService + Repository

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/UserManagementRepository.kt`

### Context
- `Models.kt` uses `@Serializable` data classes (kotlinx.serialization)
- `ApiService.kt` uses Retrofit `@GET` annotations with `suspend fun` returning `ApiResponse<T>`
- `UserManagementRepository.kt` wraps ApiService calls and throws `Exception` on failure; has `parseErrorDetail` for HTTP errors
- Base URL already includes `/api/v1/` prefix in Retrofit setup — routes in `@GET` should start from there (check existing: `@GET("users")` → hits `/api/v1/users`)

- [ ] **Step 1: Add `AssignableRolesResponse` to Models.kt**

Find the block of data classes in `Models.kt`. Add after `AppUser` or near the user-related models:

```kotlin
@Serializable
data class AssignableRolesResponse(
    val assignable_roles: List<String>
)
```

- [ ] **Step 2: Add `getAssignableRoles()` to ApiService.kt**

Find the existing user-related methods (around `suspend fun getUsers()`). Add immediately after:

```kotlin
@GET("users/assignable-roles")
suspend fun getAssignableRoles(): ApiResponse<AssignableRolesResponse>
```

Import `AssignableRolesResponse` if needed (it lives in `com.example.vmsadmin.models`).

- [ ] **Step 3: Add `getAssignableRoles()` to UserManagementRepository.kt**

Add after `getUsers()`:

```kotlin
suspend fun getAssignableRoles(): List<String> {
    return try {
        val response = apiService.getAssignableRoles()
        if (response.success && response.data != null) {
            response.data.assignable_roles
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()  // Non-fatal — ViewModel will show empty state
    }
}
```

Note: this method swallows exceptions and returns `emptyList()` — failure to fetch roles is non-fatal.

- [ ] **Step 4: Commit**

```
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/UserManagementRepository.kt
git commit -m "feat(app): AssignableRolesResponse model + ApiService + Repository method"
```

---

## Task 3: Admin app — ViewModel + Screen

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/UserManagementViewModel.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/UsersScreen.kt`

### Context
- `UserManagementViewModel` already has `_state`, `_isRefreshing`, `_pendingIds` as `MutableStateFlow`
- `init { loadUsers() }` — add `loadAssignableRoles()` call here
- In `UsersScreen.kt`, the hardcoded list is at approximately line 365:
  ```kotlin
  val roles = listOf(
      "super_admin", "ops_manager", "ground_owner",
      "tournament_manager", "support", "finance", "csr_partner", "user"
  )
  ```
  This is inside the `if (showRoleDialog)` block inside `UserCard` composable.

- [ ] **Step 1: Add `_assignableRoles` flow to UserManagementViewModel**

In `UserManagementViewModel`, add after the `_pendingIds` declaration:

```kotlin
private val _assignableRoles = MutableStateFlow<List<String>>(emptyList())
val assignableRoles: StateFlow<List<String>> = _assignableRoles.asStateFlow()
```

- [ ] **Step 2: Add `loadAssignableRoles()` to UserManagementViewModel**

Add after `loadUsers()`:

```kotlin
private fun loadAssignableRoles() {
    viewModelScope.launch {
        _assignableRoles.value = repository.getAssignableRoles()
    }
}
```

- [ ] **Step 3: Call `loadAssignableRoles()` in `init`**

Change:
```kotlin
init {
    loadUsers()
}
```
To:
```kotlin
init {
    loadUsers()
    loadAssignableRoles()
}
```

- [ ] **Step 4: Thread `assignableRoles` into the dialog in UsersScreen.kt**

`UserCard` composable receives `onChangeRole: (String) -> Unit`. It needs the roles list too. Change its signature from:

```kotlin
@Composable
fun UserCard(
    user: AppUser,
    isPending: Boolean,
    isCurrentUser: Boolean,
    onChangeRole: (String) -> Unit,
    onToggleActive: () -> Unit,
)
```

to:

```kotlin
@Composable
fun UserCard(
    user: AppUser,
    isPending: Boolean,
    isCurrentUser: Boolean,
    assignableRoles: List<String>,
    onChangeRole: (String) -> Unit,
    onToggleActive: () -> Unit,
)
```

- [ ] **Step 5: Replace the hardcoded roles list in UsersScreen.kt**

Inside `UserCard`, find:

```kotlin
if (showRoleDialog) {
    val roles = listOf(
        "super_admin",
        "ops_manager",
        "ground_owner",
        "tournament_manager",
        "support",
        "finance",
        "csr_partner",
        "user"
    )
    var selectedRole by remember(user.role) { mutableStateOf(user.role) }
```

Replace with:

```kotlin
if (showRoleDialog) {
    val roles = assignableRoles
    var selectedRole by remember(user.role) { mutableStateOf(user.role) }
```

Then in the `AlertDialog` `text` block, add an empty-state:

```kotlin
text = {
    if (roles.isEmpty()) {
        Text(
            "No roles available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column {
            roles.forEach { role ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRole = role }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedRole == role,
                        onClick = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = role, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
},
```

- [ ] **Step 6: Update the call sites that pass `UserCard` to include `assignableRoles`**

Find every call to `UserCard(...)` in `UsersScreen.kt` (there are 2 — one for the success state, one for the active/inactive filter). Add `assignableRoles = assignableRoles` to each call.

At the top of the success-state block, collect the flow:

```kotlin
val assignableRoles by viewModel.assignableRoles.collectAsState()
```

Then pass it into each `UserCard`:

```kotlin
UserCard(
    user = user,
    isPending = user.id in pendingIds,
    isCurrentUser = user.id == currentUserId,
    assignableRoles = assignableRoles,
    onChangeRole = { newRole -> viewModel.changeRole(user.id, newRole) },
    onToggleActive = { viewModel.toggleActive(user.id, user.is_active) }
)
```

- [ ] **Step 7: Commit**

```
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/UserManagementViewModel.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/UsersScreen.kt
git commit -m "feat(app): backend-driven assignable-roles dropdown in UsersScreen"
```

---

## Task 4: DEV_LOG + final test run

**Files:**
- Modify: `backend/DEV_LOG.md`

- [ ] **Step 1: Run full backend test suite**

```
venv\Scripts\python.exe -m pytest --tb=short -q
```

Expected: all tests pass including the 3 new route tests.

- [ ] **Step 2: Append to DEV_LOG.md**

Add this entry at the **top** of `backend/DEV_LOG.md` (file is reverse-chronological):

```markdown
---
## [2026-06-04] Phase 02 — Role change dropdown (backend-driven)

### Backend
**Added:**
- `GET /api/v1/users/assignable-roles` — returns roles the caller can assign, filtered by JWT role (SUPER_ADMIN → all 8, others → [])
- `backend/modules/user/tests/test_user_routes.py` — 3 tests: super_admin gets all roles, non-admin gets empty list, unauthenticated gets 401

**Modified:**
- `backend/modules/user/controller/user_routes.py` — new route + `_ALL_ROLES` constant

### Admin App
**Modified:**
- `models/Models.kt` — added `AssignableRolesResponse`
- `network/ApiService.kt` — added `getAssignableRoles()`
- `data/UserManagementRepository.kt` — added `getAssignableRoles()` (non-fatal, returns emptyList on error)
- `viewmodel/UserManagementViewModel.kt` — added `_assignableRoles` StateFlow + `loadAssignableRoles()` called in init
- `ui/screens/UsersScreen.kt` — replaced hardcoded roles list with `assignableRoles` from ViewModel; added empty-state text in dialog

### Architecture decisions
- Role policy lives on the server (not hardcoded in the app) — future role-matrix changes require only a backend deploy, no app update
- Repository failure is non-fatal (returns emptyList) — broken backend doesn't crash the Users screen
- No changes to the role-change submit path (changeRole → updateRole → PUT /users/{id} → DB)
---
```

- [ ] **Step 3: Commit and push**

```
git add backend/DEV_LOG.md
git commit -m "chore: DEV_LOG Phase 02 role dropdown"
git push
```

---

## Self-Review

**Spec coverage:**
- ✅ `GET /assignable-roles` endpoint with role matrix
- ✅ SUPER_ADMIN → all 8 roles
- ✅ Others → empty list
- ✅ Unauthenticated → 401 (handled by `get_current_user` dependency)
- ✅ `AssignableRolesResponse` model
- ✅ `ApiService.getAssignableRoles()`
- ✅ `UserManagementRepository.getAssignableRoles()` (non-fatal)
- ✅ `_assignableRoles` StateFlow in ViewModel, loaded in init
- ✅ Hardcoded list replaced in UsersScreen dialog
- ✅ Empty-state in dialog when list is empty
- ✅ Both `UserCard` call sites updated
- ✅ DEV_LOG entry

**Placeholder scan:** None found.

**Type consistency:**
- `AssignableRolesResponse` — defined Task 2, used Task 2 (ApiService), consumed as `List<String>` in Task 3 (ViewModel exposes `StateFlow<List<String>>`, not `StateFlow<AssignableRolesResponse>`)
- `assignableRoles: List<String>` — consistent across Repository, ViewModel, Screen
