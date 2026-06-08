# Plan: Societies / Groups Module
**Date:** 2026-06-08  
**Phase:** 02  
**Feature:** Social group layer — named societies with membership, internal leaderboard, and tournament bridge

---

## Goals

Build `backend/modules/society/` from scratch: a persistent named-group feature allowing users to form sports societies, manage membership, view a leaderboard of members' global scores, and optionally register their society as a team in an existing tournament.

Zero existing code for societies. Two new DB tables: `societies` + `society_members`.

---

## Data Model

### `societies` table — `Society` ORM model
| Column | Type | Constraints |
|---|---|---|
| id | Integer PK | autoincrement |
| name | String(100) | nullable=False |
| description | Text | nullable=True |
| owner_user_id | Integer FK→users CASCADE | nullable=False, index=True |
| region_id | Integer FK→locations CASCADE | nullable=False, index=True |
| sport_id | Integer FK→sports CASCADE | nullable=False, index=True |
| is_public | Boolean | nullable=False, default=True |
| max_members | Integer | nullable=False, default=50 |
| is_active | Boolean | nullable=False, default=True |
| created_at | DateTime | nullable=False, default=utcnow |
| updated_at | DateTime | nullable=False, default=utcnow, onupdate=utcnow |

### `society_members` table — `SocietyMember` ORM model + `SocietyRole` class
| Column | Type | Constraints |
|---|---|---|
| id | Integer PK | autoincrement |
| society_id | Integer FK→societies CASCADE | nullable=False, index=True |
| user_id | Integer FK→users CASCADE | nullable=False, index=True |
| role | String(20) | nullable=False |
| joined_at | DateTime | nullable=False, default=utcnow |
| UNIQUE | (society_id, user_id) | |

`SocietyRole` class:
- `OWNER = "OWNER"`
- `MEMBER = "MEMBER"`
- `ALL = frozenset({OWNER, MEMBER})`

---

## File Structure

```
backend/modules/society/
  __init__.py
  model/
    __init__.py
    society_model.py          # Society ORM + to_dict()
    society_member_model.py   # SocietyMember ORM + SocietyRole class + to_dict()
  repository/
    __init__.py
    society_repository.py     # SocietyRepository
    society_member_repository.py  # SocietyMemberRepository
  service/
    __init__.py
    society_service.py           # SocietyService
    society_member_service.py    # SocietyMemberService
    society_tournament_service.py  # SocietyTournamentService
  schemas/
    __init__.py
    society_schema.py         # CreateSocietySchema, UpdateSocietySchema
  controller/
    __init__.py
    society_routes.py         # All 12 endpoints
  tests/
    __init__.py
    test_society_service.py
    test_society_member_service.py
    test_society_tournament_service.py
```

---

## Repositories

### `SocietyRepository`
```python
create(data: dict) -> dict
find_by_id(society_id: int) -> dict | None
find_all(region_id: int | None, sport_id: int | None, active_only: bool = True) -> list[dict]
update(society_id: int, data: dict) -> dict | None
delete(society_id: int) -> bool
```

### `SocietyMemberRepository`
```python
add_member(society_id: int, user_id: int, role: str) -> dict
find_member(society_id: int, user_id: int) -> dict | None
get_members(society_id: int) -> list[dict]           # ordered by joined_at asc
remove_member(society_id: int, user_id: int) -> bool
update_role(society_id: int, user_id: int, new_role: str) -> dict | None
count_members(society_id: int) -> int
```

---

## Services

### `SocietyService`
```python
create(data: dict, owner_user_id: int) -> dict
    # Validates name non-empty, region_id, sport_id present
    # Creates Society record
    # Adds owner as SocietyMember(role=OWNER)
    # Returns society dict

get_by_id(society_id: int) -> dict
    # Returns society dict or raises ValueError("Society not found.")

list(region_id: int | None = None, sport_id: int | None = None) -> list[dict]
    # Returns active societies, optionally filtered

update(society_id: int, data: dict, requester_id: int, requester_role: str) -> dict
    # Allowed if requester is OWNER member of society OR requester_role in (SUPER_ADMIN, OPS_MANAGER)
    # Cannot change owner_user_id, region_id, sport_id directly (structural fields immutable after creation)
    # Can change: name, description, is_public, max_members
    # If max_members lowered below current count → raises ValueError

deactivate(society_id: int, requester_role: str) -> dict
    # Only SUPER_ADMIN or OPS_MANAGER
    # Sets is_active=False

delete(society_id: int, requester_role: str) -> bool
    # Only SUPER_ADMIN
```

### `SocietyMemberService`
```python
join(society_id: int, user_id: int) -> dict
    # Society must exist + is_active
    # is_public must be True (private societies: only via invite — not in v1, so raise ValueError("This society is private."))
    # Check capacity: count_members < max_members
    # Check not already a member
    # Adds as MEMBER
    # Returns member dict

leave(society_id: int, user_id: int) -> bool
    # Must be a member
    # Owner cannot leave (must transfer ownership first) → raise ValueError("Transfer ownership before leaving.")
    # Removes member record

kick(society_id: int, target_user_id: int, requester_id: int, requester_role: str) -> bool
    # Requester must be OWNER of the society OR requester_role is SUPER_ADMIN
    # Cannot kick the OWNER
    # Removes target member record

transfer_ownership(society_id: int, new_owner_id: int, requester_id: int) -> dict
    # Requester must be current OWNER
    # New owner must already be a MEMBER
    # Updates old owner → MEMBER, new owner → OWNER
    # Updates society.owner_user_id = new_owner_id

get_members(society_id: int) -> list[dict]
    # Returns members list

get_leaderboard(society_id: int) -> list[dict]
    # Gets all member user_ids
    # Fetches PlayerScore for each member filtered by society.region_id + society.sport_id
    # Returns list sorted by total_points DESC, then matches_played DESC
    # Members with no PlayerScore included at bottom with 0 points
```

### `SocietyTournamentService`
```python
register_as_team(
    society_id: int,
    tournament_id: int,
    member_ids: list[int],
    requester_id: int,
) -> dict
    # Requester must be OWNER of society
    # Society must be active
    # All member_ids must be current MEMBER or OWNER of society
    # Delegates to TournamentService.register() with participant_type=TEAM
    # team_data = {"team_name": society.name, "member_user_ids": member_ids}
    # Returns created TournamentTeam dict
```

---

## Schemas

### `CreateSocietySchema`
```python
name: str              # min_length=1, max_length=100
description: str | None
region_id: int
sport_id: int
is_public: bool = True
max_members: int = 50  # ge=2, le=500
```

### `UpdateSocietySchema`
```python
name: str | None
description: str | None
is_public: bool | None
max_members: int | None  # ge=2, le=500
```

---

## API Endpoints

All under `/api/v1/societies`.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | require_user | Create a new society |
| GET | `/` | require_user | List societies (filter: region_id, sport_id) |
| GET | `/{id}` | require_user | Get society by ID |
| PUT | `/{id}` | require_user (service enforces owner/admin) | Update society |
| DELETE | `/{id}` | require_role(SUPER_ADMIN) | Hard delete |
| POST | `/{id}/deactivate` | require_role(SUPER_ADMIN, OPS_MANAGER) | Soft deactivate |
| POST | `/{id}/join` | require_user | Join society |
| DELETE | `/{id}/leave` | require_user | Leave society |
| DELETE | `/{id}/members/{uid}` | require_user (service enforces) | Kick member |
| POST | `/{id}/transfer-owner` | require_user (service enforces) | Transfer ownership |
| GET | `/{id}/members` | require_user | List members |
| GET | `/{id}/leaderboard` | require_user | Society leaderboard |
| POST | `/{id}/tournament-register` | require_user (service enforces owner) | Register society as tournament team |

Response format: `{"success": True, "data": ..., "message": "..."}`  
Status 201 on: create, join, tournament-register.  
404 on missing society: `raise HTTPException(status_code=404, detail="Society not found.")`  
400 on business rule violations.

---

## main.py additions

1. Import `Society` model (triggers table creation)
2. Import `SocietyMember` model (triggers table creation)
3. Import `society_router`
4. `app.include_router(society_router)`

---

## Task Breakdown (9 tasks, subagent-driven)

### Task 1 — `Society` model + schemas
**Files:**
- `backend/modules/society/__init__.py`
- `backend/modules/society/model/__init__.py`
- `backend/modules/society/model/society_model.py`
- `backend/modules/society/schemas/__init__.py`
- `backend/modules/society/schemas/society_schema.py`

**Deliverables:**
- `Society` ORM model with all columns + `to_dict()`
- `CreateSocietySchema` and `UpdateSocietySchema` (Pydantic, validated)

**No tests needed for this task** (models are tested via service tests).  
**Commit after task.**

---

### Task 2 — `SocietyMember` model
**Files:**
- `backend/modules/society/model/society_member_model.py`

**Deliverables:**
- `SocietyRole` class with `OWNER`, `MEMBER`, `ALL`
- `SocietyMember` ORM model with all columns + `to_dict()`

**Commit after task.**

---

### Task 3 — `SocietyRepository`
**Files:**
- `backend/modules/society/repository/__init__.py`
- `backend/modules/society/repository/society_repository.py`

**Deliverables:**
- `SocietyRepository` with: `create`, `find_by_id`, `find_all(region_id, sport_id, active_only)`, `update`, `delete`
- Module-level singleton: `society_repository = SocietyRepository()`
- Follows `TournamentRepository` pattern exactly (session per call, manual commit, rollback on exception)

**Commit after task.**

---

### Task 4 — `SocietyMemberRepository`
**Files:**
- `backend/modules/society/repository/society_member_repository.py`

**Deliverables:**
- `SocietyMemberRepository` with: `add_member`, `find_member`, `get_members`, `remove_member`, `update_role`, `count_members`
- Module-level singleton

**Commit after task.**

---

### Task 5 — `SocietyService` + tests
**Files:**
- `backend/modules/society/service/__init__.py`
- `backend/modules/society/service/society_service.py`
- `backend/modules/society/tests/__init__.py`
- `backend/modules/society/tests/test_society_service.py`

**Tests (SQLite in-memory, inject factories):**
- `test_create_society` — happy path, owner auto-added as OWNER member
- `test_create_missing_name_raises`
- `test_create_missing_region_raises`
- `test_get_by_id_not_found_raises`
- `test_list_with_filters`
- `test_update_by_owner`
- `test_update_by_super_admin`
- `test_update_by_non_owner_raises`
- `test_update_max_members_below_count_raises`
- `test_deactivate_by_ops_manager`
- `test_deactivate_by_non_admin_raises`
- `test_delete_by_super_admin`
- `test_delete_by_non_super_admin_raises`

**Commit after task.**

---

### Task 6 — `SocietyMemberService` + tests
**Files:**
- `backend/modules/society/service/society_member_service.py`
- `backend/modules/society/tests/test_society_member_service.py`

**Tests:**
- `test_join_success`
- `test_join_private_society_raises`
- `test_join_over_capacity_raises`
- `test_join_already_member_raises`
- `test_leave_success`
- `test_leave_owner_raises`
- `test_kick_by_owner`
- `test_kick_owner_raises`
- `test_kick_by_non_owner_raises`
- `test_transfer_ownership_success`
- `test_transfer_ownership_non_member_raises`
- `test_transfer_ownership_not_owner_raises`
- `test_get_leaderboard_returns_sorted_by_points`

**Commit after task.**

---

### Task 7 — `SocietyTournamentService` + tests
**Files:**
- `backend/modules/society/service/society_tournament_service.py`
- `backend/modules/society/tests/test_society_tournament_service.py`

**Tests:**
- `test_register_as_team_success`
- `test_register_non_owner_raises`
- `test_register_non_member_included_raises`
- `test_register_inactive_society_raises`

**Commit after task.**

---

### Task 8 — Routes + main.py registration
**Files:**
- `backend/modules/society/controller/__init__.py`
- `backend/modules/society/controller/society_routes.py`
- `backend/main.py` (modified)

**Deliverables:**
- All 13 endpoints (including deactivate)
- Imports in main.py: `Society`, `SocietyMember` models + `society_router`
- `app.include_router(society_router)`

**Commit after task.**

---

### Task 9 — DEV_LOG entry
**Files:**
- `backend/DEV_LOG.md` (append only)

**Entry format:** date, phase tag, Added/Modified sections, exact files listed, architectural decisions.

**Commit after task.**

---

## Architectural Decisions

1. **Leaderboard reuses `PlayerScore`** — no separate society points table. The global score from tournament results is filtered by society.region_id + society.sport_id. Simple, no new data.

2. **Private societies in v1**: `is_public=False` creates society but join is blocked (`ValueError("This society is private.")`). Invite flow is out of scope. This leaves the door open without requiring invite infrastructure now.

3. **Tournament bridge delegates to `TournamentService.register()`** — `SocietyTournamentService` is a thin adapter. It validates society-level preconditions (is owner, all members belong to society), then calls the existing `TournamentService.register()` with `participant_type=TEAM`. No duplication.

4. **Owner can't leave** — must `transfer_ownership` first. Prevents ownerless societies.

5. **Structural fields immutable post-creation** — `owner_user_id`, `region_id`, `sport_id` cannot be changed via `update()`. This prevents a society from silently jumping regions/sports.

6. **`SUPER_ADMIN`-only hard delete** — `OPS_MANAGER` can only deactivate (soft). Follows same pattern as other sensitive resources.

7. **Session-per-call pattern** — matches all existing repositories (no session-injection anti-pattern).
