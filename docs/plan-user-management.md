# Plan — SUPER_ADMIN User Management

_Status: PLAN — not yet implemented. Awaiting review + "implement" confirmation._
_Scope owner: Phase 01 RBAC item #1 follow-up (see [.claude/context/phase01-scope.md](../.claude/context/phase01-scope.md))._
_Note: user referenced `docs/phase01-spec.md` — that file does not exist; the actual spec is [.claude/context/phase01-scope.md](../.claude/context/phase01-scope.md) + [.claude/context/rbac-roles.md](../.claude/context/rbac-roles.md). This plan follows those._

---

## 1. Feature summary
A drawer-/Manage-accessible **Users** screen for `SUPER_ADMIN` only. Three actions:
1. **List** all users with id · name · phone · role · is_active · created_at.
2. **Promote / demote role** — pick from the 8 `UserRole` values.
3. **Deactivate / reactivate** — toggle `is_active`.

Out of scope here: create user (already exists), delete user (already exists, don't surface in UI), audit logging (separate phase 01 item #9 — we leave a TODO hook), bulk operations, search/filter, pagination (small dataset for now).

---

## 2. Verify-backend-first audit

| Need | Backend endpoint | Status |
|---|---|---|
| List all users | `GET /api/v1/users` | ✅ exists ([backend/modules/user/controller/user_routes.py:40](../backend/modules/user/controller/user_routes.py)) — returns all users for any admin role, returns only self for `user` |
| Change role | `PUT /api/v1/users/{id}` with `{"role": "..."}` | ✅ exists ([user_routes.py:64](../backend/modules/user/controller/user_routes.py)) — but **role-change guard is too loose**: any `_ADMIN_ROLES` member can change roles. RBAC spec restricts this to `SUPER_ADMIN` only |
| Deactivate / reactivate | `PUT /api/v1/users/{id}` with `{"is_active": bool}` | ✅ exists — `is_active` already in `UpdateUserSchema` ([user_schema.py:106](../backend/modules/user/schemas/user_schema.py)) and the repository's `update()` writes it ([user_repository.py:93](../backend/modules/user/repository/user_repository.py)) |
| Prevent self-demotion / self-deactivation | not enforced | ❌ missing — needed to avoid super_admin locking themselves out |
| Audit hook | n/a — audit module not built yet (phase 01 item #9) | deferred — leave clearly-marked TODO comments at the two privileged actions |

**Verdict**: backend mostly ready. Two small backend hardenings are required before exposing the UI; one new endpoint optional. No schema migration needed.

---

## 3. Backend changes

### 3.1 Tighten role-change authorization (REQUIRED)
File: [backend/modules/user/controller/user_routes.py](../backend/modules/user/controller/user_routes.py)

In `update_user` (line ~64), the existing guard says "any admin can change roles". Replace with: **only `SUPER_ADMIN` can change `role`**. Other admin roles may still update non-role fields on themselves.

```python
# pseudocode — DO NOT IMPLEMENT YET
if "role" in request_data and current_user["role"] != UserRole.SUPER_ADMIN.value:
    raise HTTPException(status_code=403, detail="Only super admins can change user roles.")
```

Mirror the same guard in `UserService.update_user` ([user_service.py:54](../backend/modules/user/service/user_service.py)) — the service-layer check at line 72 currently uses `_ADMIN_ROLES`; replace with explicit `SUPER_ADMIN` for defense-in-depth.

### 3.2 Self-lockout protection (REQUIRED)
Same file, `update_user`. Block the caller from:
- changing **their own** role
- setting **their own** `is_active = False`

Mirror in service layer too. Reuse the existing `current_user["id"] == user_id` check pattern that `delete_user` uses.

### 3.3 Optional: dedicated admin endpoints (NICE-TO-HAVE — skip unless reviewer asks)
A cleaner audit story would add:
- `POST /api/v1/users/{id}/role` — body `{role: "..."}` — `require_role(SUPER_ADMIN)`
- `POST /api/v1/users/{id}/deactivate` and `POST /api/v1/users/{id}/reactivate` — `require_role(SUPER_ADMIN)`

These would keep `PUT /users/{id}` for self-profile updates and isolate privileged ops. Recommend deferring — current `PUT` is enough for phase 01, and adding them is a refactor that should land with the audit-log work in phase 01 item #9.

### 3.4 Tests to add
File: [backend/modules/auth/tests/test_user_rbac_routes.py](../backend/modules/auth/tests/test_user_rbac_routes.py)

- `test_super_admin_can_change_role` — PUT updates role, returns 200
- `test_ops_manager_cannot_change_role` — same PUT body, returns 403
- `test_super_admin_cannot_change_own_role` — caller_id == path id with role body → 403
- `test_super_admin_cannot_deactivate_self` — caller_id == path id with `is_active=False` → 403
- `test_super_admin_can_deactivate_other_user` → 200, user is_active flips to False
- `test_super_admin_can_reactivate_user` → 200, flips back to True
- `test_invalid_role_value_rejected` — body `{"role": "warlord"}` → 400 (already covered by schema; verify)

Service-layer unit test in [backend/modules/user/tests/test_user_service.py](../backend/modules/user/tests/test_user_service.py):
- `test_update_user_only_super_admin_can_change_role`
- `test_update_user_blocks_self_role_change`

### 3.5 DEV_LOG.md entry (REQUIRED)
Append a 2026-05-19 / Phase 01 entry to [backend/DEV_LOG.md](../backend/DEV_LOG.md) covering:
- Modified: `user_routes.py`, `user_service.py`, `test_user_rbac_routes.py`, `test_user_service.py`
- App added: see §4
- Architectural note: super_admin is now the **only** role permitted to change roles or toggle `is_active`; self-lockout blocked.

---

## 4. Admin app changes

### 4.1 Networking — [Vmsadminapp/.../network/ApiService.kt](../Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt)
Add three Retrofit endpoints:
```kotlin
@GET("/api/v1/users")
suspend fun getUsers(): ApiResponse<List<AppUser>>

@PUT("/api/v1/users/{id}")
suspend fun updateUserRole(
    @Path("id") id: Int,
    @Body request: UpdateUserRoleRequest
): ApiResponse<AppUser>

@PUT("/api/v1/users/{id}")
suspend fun setUserActive(
    @Path("id") id: Int,
    @Body request: UpdateUserActiveRequest
): ApiResponse<AppUser>
```
Two distinct Retrofit signatures even though they hit the same URL — keeps request bodies type-safe.

### 4.2 Models — [Vmsadminapp/.../models/Models.kt](../Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt)
Add:
```kotlin
@Serializable
data class AppUser(
    val id: Int,
    val name: String,
    val phone: String,
    val role: String,
    val is_active: Boolean,
    val region_id: Int? = null,
    val ghost_strikes: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UpdateUserRoleRequest(val role: String)

@Serializable
data class UpdateUserActiveRequest(val is_active: Boolean)
```
Class is `AppUser` not `User` — avoid colliding with any future Compose `User`-named imports and the JWT-claim "user" string.

### 4.3 Repository — `Vmsadminapp/.../data/UserManagementRepository.kt` (NEW)
Mirror `RegionRepository.kt` pattern:
- `suspend fun getUsers(): List<AppUser>`
- `suspend fun updateRole(id: Int, role: String): AppUser`
- `suspend fun setActive(id: Int, active: Boolean): AppUser`
- Includes `parseErrorDetail()` helper for FastAPI 4xx body parsing.

Name = `UserManagementRepository` (not `UserRepository`) to make intent obvious and avoid future confusion with a self-profile repository.

### 4.4 ViewModel — `Vmsadminapp/.../viewmodel/UserManagementViewModel.kt` (NEW)
- `StateFlow<UserManagementState>` with `Loading / Success(list) / Error(message)`
- `loadUsers()`, `changeRole(id, newRole)`, `toggleActive(id, current)`
- Per-row action state to disable the row while a mutation is in flight
- Receives `currentUserId: Int` so it can block self-mutation client-side too (ViewModel layer — third layer of the four-layer RBAC enforcement)
- ViewModel-level role check: refuse to even call backend if `currentRole != "super_admin"`. Tied to `AuthViewModel.effectiveRole` via constructor or function param.
- Factory class mirroring `RegionViewModelFactory`.

### 4.5 Screen — `Vmsadminapp/.../ui/screens/UsersScreen.kt` (NEW)
- TopAppBar "Users" + back arrow
- `LazyColumn` of `AppCard` rows
- Each row: name + phone (primary), small role chip + status chip, overflow menu with:
  - "Change role" → opens `AlertDialog` with `RadioButton`s for the 8 roles (reuse the `DebugRoleSwitcher` list from [MainScreen.kt:290](../Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/MainScreen.kt))
  - "Deactivate" / "Reactivate" toggle
- Self-row visually disabled with "(you)" suffix; menu hidden — fourth layer of RBAC enforcement (UI hide).
- Error snackbar fed from ViewModel state.
- Pull-to-refresh on the list.

### 4.6 Navigation — [Vmsadminapp/.../navigation/AppNavigation.kt](../Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt) + [ui/screens/MainScreen.kt](../Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/MainScreen.kt)
- Add a `manage/users` composable route inside `MainScreen.kt`, guarded by a new `USERS_ROLES = setOf("super_admin")` (NOT in `MANAGE_ROLES` — even ops_manager is denied)
- Add a "Users" tile to `ManageScreen` in [PlaceholderScreens.kt:23](../Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/PlaceholderScreens.kt) — but only render the tile when `role == "super_admin"` (UI-hide layer). Wire a new `onNavigateToUsers` callback through.
- Guard at the `composable("manage/users")` level: if `role !in USERS_ROLES` → `onForbidden()` — navigation layer.

### 4.7 Wiring — [MainActivity.kt](../Vmsadminapp/app/src/main/java/com/example/vmsadmin/MainActivity.kt)
Instantiate `UserManagementRepository`, `UserManagementViewModel`, pass into `AppNavigation` → `MainScreen`. Follow the exact pattern lines 77-79 use for region.

`UserManagementViewModel` also needs `currentUserId`. Source it from JWT — `TokenManager` currently stores token + role but not user id. Two options:
- **(A) preferred**: extend `TokenManager` + `AuthViewModel` to also persist `user_id` from the JWT `sub` claim at login. Small, reusable change.
- **(B) fallback**: call `GET /api/v1/users` then pick the row whose phone matches the logged-in phone — fragile, skip.

Go with (A). Touches: `TokenManager.kt`, `AuthViewModel.kt` (write id on successful login), `AppNavigation.kt` (collect into a flow and hand to `MainScreen`).

### 4.8 Files modified vs added (admin app)
**Added**
- `Vmsadminapp/.../data/UserManagementRepository.kt`
- `Vmsadminapp/.../viewmodel/UserManagementViewModel.kt` (+ factory)
- `Vmsadminapp/.../ui/screens/UsersScreen.kt`

**Modified**
- `Vmsadminapp/.../network/ApiService.kt` — 3 endpoints
- `Vmsadminapp/.../models/Models.kt` — `AppUser`, `UpdateUserRoleRequest`, `UpdateUserActiveRequest`
- `Vmsadminapp/.../data/TokenManager.kt` — persist user id
- `Vmsadminapp/.../viewmodel/AuthViewModel.kt` — write user id on login; expose `currentUserId: StateFlow<Int?>`
- `Vmsadminapp/.../ui/screens/PlaceholderScreens.kt` — Users tile in `ManageScreen` (super_admin only) + new `onNavigateToUsers` callback
- `Vmsadminapp/.../ui/screens/MainScreen.kt` — `USERS_ROLES`, `manage/users` route, prop drilling
- `Vmsadminapp/.../navigation/AppNavigation.kt` — pass `UserManagementViewModel` + `currentUserId` through
- `Vmsadminapp/.../MainActivity.kt` — instantiate the new repo/VM

---

## 5. RBAC enforcement matrix (four layers)
| Layer | Where | What it does |
|---|---|---|
| Backend endpoint | `user_routes.py` `update_user` + service guard | 403 unless caller is `super_admin` for role change / `is_active` flip on others; 403 on self-mutation |
| Repository / query | n/a — `users` table has no ownership scope | not applicable here |
| ViewModel | `UserManagementViewModel` | refuses to call backend if `effectiveRole != "super_admin"`; blocks mutating self |
| Navigation | `composable("manage/users")` guard | non-super_admin → `ForbiddenScreen` |
| UI | `ManageScreen` tile conditional render + self-row hidden menu | super_admin sees the tile; everyone else doesn't |

---

## 6. What stays the same
- `User` model — no schema changes; `is_active` and `role` fields already exist.
- `UserRepository` — already exposes `find_all` and `update`.
- `UserService.create_user` and `UserService.delete_user` — untouched.
- `CreateUserSchema`, `UpdateUserSchema` — already validate role + is_active.
- All other admin screens.
- The non-admin `/api/v1/users` self-profile flow (regular user gets `[self]`) — preserved by keeping a single `PUT /users/{id}` and self-mutation guards inside it.

---

## 7. Implementation order (dependency-respecting)

1. **Backend hardening** (no UI dependency)
   1. Tighten `update_user` role-change guard to `SUPER_ADMIN` only — controller + service
   2. Add self-lockout guards — controller + service
   3. Write 7 RBAC tests + 2 service tests
   4. Run `python -m pytest backend/modules/auth/tests/test_user_rbac_routes.py backend/modules/user/tests/` — all green
2. **Models** — add `AppUser` + 2 request DTOs to `Models.kt`
3. **Network** — add 3 endpoints to `ApiService.kt`
4. **Token plumbing** — `TokenManager.kt` persists user id; `AuthViewModel.kt` writes it on login and exposes `currentUserId`
5. **Repository** — `UserManagementRepository.kt`
6. **ViewModel** — `UserManagementViewModel.kt` + factory
7. **Screen** — `UsersScreen.kt`
8. **Navigation/wiring** — `MainScreen.kt`, `AppNavigation.kt`, `PlaceholderScreens.kt`, `MainActivity.kt`
9. **Manual smoke test** in admin app under each role via the debug role switcher — confirm tile/route/menu only show for super_admin and Forbidden screen fires otherwise
10. **DEV_LOG.md** append entry
11. `cd Vmsadminapp && ./gradlew assembleDebug` — must succeed

Each numbered step is a single commit. Steps 1-4 are independent of each other after step 1's guard fix; the rest are strictly sequential.

---

## 8. Risks / open questions
- **No audit log yet** (phase 01 item #9 not built). Two privileged actions here (role change, deactivate) are exactly the actions that should be audited. Recommend: leave a `# TODO(phase01-audit): emit audit event here` next to both mutations in the service, so the audit work picks them up.
- **JWT does not currently embed role expiration / refresh** — if a super_admin demotes themselves… wait, blocked. If they demote another super_admin, the demoted user's JWT will still claim super_admin until expiry. Acceptable for phase 01; flag for token-revocation discussion later.
- **`UpdateUserSchema` accepts both role and is_active in one body** — UI sends each separately, but the schema doesn't prevent combined edits. That's fine; just noting.

---

## 9. Review request

Review this plan and annotate it. Reply **"implement"** when ready.
