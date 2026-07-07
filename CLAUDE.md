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
- `backend/modules/captain/` — full CRUD module (model, repo, service, routes) ✓
- `backend/DEV_LOG.md` — append-only development history
- `Vmsadminapp/.../` — `MainActivity`, `navigation/AppNavigation.kt`, `ui/screens/`, `viewmodel/`, `data/`, `network/ApiService.kt`, `models/Models.kt`
- `.claude/context/` — rbac-roles.md, phase01-scope.md, memory.md

## Commands
- Run backend: `venv\Scripts\python.exe -m uvicorn backend.main:app --reload --port 8000` (**venv launcher broken** — use python.exe directly)
- Test backend: `venv\Scripts\python.exe -m pytest`
- Admin app build: **DO NOT build the APK** — user builds and installs via Android Studio directly. Never run `gradlew assembleDebug` unless explicitly verifying a compile error.
- DB migrations: `venv\Scripts\python.exe backend/run_migrations.py`

## Current focus — Phase 02
Finance reporting · CSR_PARTNER screens

## Known gaps (priority order)

### Needs rebuild — DB schema exists, source code was lost uncommitted (see DEV_LOG 2026-07-06 incident)
1. **Notifications module** (`backend/modules/notification/`) — `notifications`/`fcm_tokens` tables exist, no code
2. **Chat module** (`backend/modules/chat/`) — `messages` table exists, no code
3. **Captain KYC onboarding** — `captains.kyc_document_url/kyc_status/payout_upi_id/verification_method` columns exist, `captain_model.py` back to 3-field original
4. **Captain earnings wallet** — `captain_earnings` table + `system_configs.CAPTAIN_FEE_PER_MATCH` exist, no model/repo/service/routes; `matches.captain_id` column exists, unused

### Done ✓
- Tournament admin management — match scheduling, result entry, standings, registrations wired into a new admin `TournamentDetailScreen`; `GET /tournaments/{id}/registrations` added
- Audit log — filtering (action/actor/resource type/date range) + pagination; expanded coverage (tournament CRUD, match results, ground edits)
- Ground Owner panel — `GROUND_OWNER` was in `_ADMIN_ROLES`, so `require_admin` let any ground owner edit any ground with full field access; now scoped to own grounds + `is_active`/lat/long only; admin app gets a working Active/Offline switch

## RBAC roles (full rules in [.claude/context/rbac-roles.md](.claude/context/rbac-roles.md))
SUPER_ADMIN · OPS_MANAGER · GROUND_OWNER · TOURNAMENT_MANAGER · SUPPORT · FINANCE · CSR_PARTNER

Permissions must be enforced at **four** layers — backend endpoint, ViewModel, navigation, UI. Frontend hiding alone is not sufficient.

## Verify-backend-first rule (hard rule)
**Before implementing ANY admin feature, verify backend support already exists.**
1. Search `backend/modules/<domain>/` and `ApiService.kt` for the endpoint.
2. If missing: implement backend first (model → repository → service → controller → register in `backend/main.py`).
3. Then wire admin app (repository → ViewModel → screen → navigation).
4. Complete vertical slices only. No placeholder buttons. No UI-only fakes.
5. Reuse existing screens/repos/services wherever possible.

## DEV_LOG.md update requirement
Every change set must append an entry to `backend/DEV_LOG.md` with date, phase tag, Added/Modified/Removed sections, exact files (backend + app separately), backend changes, app changes, architectural decisions + reason. **Append only. Never overwrite.**

## Critical constraints
- PostgreSQL: parameterised queries only — no f-string SQL
- Python: type hints always, `asyncio` for I/O, specific exceptions (never bare `except:`)
- Kotlin: no debug prints; use proper logger
- Never hardcode secrets — use `.env` / `python-dotenv`
- Read the file before editing it
- For changes touching 3+ files: write a plan in `docs/plan-<feature>.md` first
- Ground Owner data isolation enforced at **repository / query level**, not just route filtering
- Permission middleware lives in `backend/core/security/` and `backend/core/middleware/`; extend, don't fork
- Git identity per-repo: `git config user.email lokeshwara.rao2972005@gmail.com && git config user.name Lokesh`
- **Commit after every completed vertical slice** — do not batch a full session's work uncommitted. An entire day of work (Notifications, Chat, Captain KYC/wallet) was lost on 2026-07-06 because it sat uncommitted and got overwritten by a separate session. Ask before committing per the user's standing workflow preferences, but raise it — don't silently let hours of work stay uncommitted.

## Persistent memory
- Read `.claude/context/memory.md` at the start of every session before doing anything
- Use semantic_search before exploring any file manually
- Never re-scan the codebase — check memory.md + RAG first
