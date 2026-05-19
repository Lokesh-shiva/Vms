# Plixo Control Centre — Claude Rules

Project pivot: the existing **VMS Admin App** is being restructured into the **Plixo Control Centre** — a role-based operations platform. Backend and admin app already exist and are functional. Do not rebuild — extend.

## Stack
- **Backend**: Python 3.12+, FastAPI, SQLAlchemy, PostgreSQL
- **Admin app**: Kotlin + Jetpack Compose (Android), Retrofit/OkHttp
- **User app**: Kotlin + Jetpack Compose (separate, out of scope here)

## Entry points
- Backend: `backend/main.py` (run: `uvicorn backend.main:app --reload --port 8000`)
- Admin app: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/MainActivity.kt`

## Layout
- `backend/main.py` — FastAPI app, registers all routers
- `backend/core/` — `database/`, `middleware/`, `security/auth_manager.py` (RBAC stub), `config/`
- `backend/modules/<domain>/` — `controller/`, `service/`, `repository/`, `model/`, `schemas/` per domain
- `backend/modules/auth/` — JWT auth, dependencies
- `backend/modules/user/model/user_model.py` — `User`, `UserRole` (currently only USER/ADMIN)
- `backend/modules/captain/` — scaffolded but **no model file yet**
- `backend/DEV_LOG.md` — append-only development history
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/` — `MainActivity`, `navigation/AppNavigation.kt`, `ui/screens/`, `viewmodel/`, `data/` (repositories), `network/ApiService.kt`, `network/AuthInterceptor.kt`, `data/TokenManager.kt`, `models/Models.kt`
- `docs/` — specs, plans, lessons
- `.claude/context/` — extended Plixo context (rbac-roles, phase01-scope)

## Commands
- Run backend: `uvicorn backend.main:app --reload --port 8000`
- Test backend: `python -m pytest`
- Lint Python: `ruff check .`
- Format Python: `ruff format .`
- Admin app build: `cd Vmsadminapp && ./gradlew assembleDebug`

## Phase 01 scope (Plixo Control Centre Phase 01)
Focus = **RBAC + navigation restructure + role-locked panels + architecture cleanup + backend verification**. Full scope in [.claude/context/phase01-scope.md](.claude/context/phase01-scope.md).

Out of scope this phase: redesigns, animations, visual overhaul, tournament automation/brackets, captain UI beyond lightweight.

## RBAC roles (full rules in [.claude/context/rbac-roles.md](.claude/context/rbac-roles.md))
SUPER_ADMIN · OPS_MANAGER · GROUND_OWNER · TOURNAMENT_MANAGER · SUPPORT · FINANCE · CSR_PARTNER

Permissions must be enforced at **four** layers — backend endpoint, ViewModel, navigation, UI. Frontend hiding alone is not sufficient.

## Verify-backend-first rule (hard rule)
**Before implementing ANY admin feature, verify backend support already exists.**
1. Search `backend/modules/<domain>/` and `ApiService.kt` for the endpoint.
2. If backend support is missing: implement backend first (model → repository → service → controller → register router in `backend/main.py`).
3. Then wire admin app (repository → ViewModel → screen → navigation).
4. Always implement complete vertical slices. No placeholder buttons. No UI-only fakes.
5. Reuse existing screens, repositories, services wherever possible. Minimum rewrite, maximum leverage.

## DEV_LOG.md update requirement
Every change set must append an entry to `backend/DEV_LOG.md` with:
- Date (YYYY-MM-DD) + phase tag
- Added / Modified / Removed sections
- Exact files modified (backend + admin app separately)
- Backend changes (schema, routes, middleware)
- App changes (screens, viewmodels, navigation)
- Architectural decisions + reason

**Append only. Never overwrite previous history.** No vague entries.

## Critical constraints
- PostgreSQL: parameterised queries only — no f-string SQL
- Python: type hints always, `asyncio` for I/O, specific exceptions (never bare `except:`)
- Kotlin: no `console.log`-equivalent debug prints left in; use proper logger
- Never hardcode secrets — use `.env` / `python-dotenv`
- Read the file before editing it
- For changes touching 3+ files: write a plan in `docs/plan-<feature>.md` first
- Ground Owner data isolation must be enforced at the **repository / query level**, not just route filtering
- Permission middleware lives in `backend/core/security/` and `backend/core/middleware/`; extend, don't fork

## Current focus
Phase 01 — RBAC foundation, navigation restructure, role-locked panels, captain + tournament backend foundations.

## Persistent memory
- Read .claude/context/memory.md at the start of every session before doing anything
- Use semantic_search before exploring any file manually
- Never re-scan the codebase — check memory.md + RAG first