---
phase: "04"
plan: "01"
subsystem: match-lifecycle
tags: [match, lifecycle, arrive, finish, gps, cart-coordinates]
dependency_graph:
  requires: [match-model, match-service, cart-model, match-repository]
  provides: [arrive-endpoint, finish-endpoint, cart-coordinates, match-lifecycle-transitions]
  affects: [match-routes, cart-model, match-service]
tech_stack:
  added: []
  patterns: [select-for-update-locking, gps-proximity-validation, pydantic-request-schema]
key_files:
  created:
    - backend/core/database/add_coordinates_to_carts.py
    - backend/modules/match/tests/__init__.py
    - backend/modules/match/tests/test_match_lifecycle.py
  modified:
    - backend/modules/cart/model/cart_model.py
    - backend/modules/match/service/match_service.py
    - backend/modules/match/controller/match_routes.py
    - backend/modules/match/schemas/match_schema.py
decisions:
  - GPS proximity uses simple coordinate diff (~500m) rather than Haversine for v1 simplicity
  - Cart without coordinates bypasses GPS check gracefully (v1 tolerance)
  - Any player in the match can trigger finish_match (not restricted to creator)
  - Used Pydantic BaseModel for MatchArriveSchema (consistent with matchmaking module pattern)
metrics:
  duration: "224s"
  completed: "2026-03-31"
  tasks_completed: 4
  tasks_total: 4
  tests_added: 12
  tests_passing: 12
---

# Phase 04 Plan 01: Match Lifecycle States & Operations Summary

GPS-validated arrival detection with per-player tracking, automatic status transitions (MATCHED -> ARRIVED -> IN_PROGRESS -> COMPLETED), and cart freeing on match completion.

## What Was Built

### Task 1: DB Schema Enhancements
- Added `latitude` (Float, nullable) and `longitude` (Float, nullable) columns to the Cart model
- Updated `Cart.to_dict()` to include the new coordinate fields
- Created idempotent migration script `add_coordinates_to_carts.py` following existing migration pattern

### Task 2: MatchService Lifecycle Methods
- **`arrive_match(user_id, match_id, user_lat, user_lng)`**: Validates match status (MATCHED/ARRIVED), verifies player membership, performs GPS proximity check against cart coordinates (~500m radius), marks `MatchPlayer.has_arrived = True`, transitions to ARRIVED (partial) or IN_PROGRESS (all arrived)
- **`finish_match(user_id, match_id)`**: Validates player membership, ensures match not already COMPLETED/CANCELLED, sets status to COMPLETED, frees cart back to AVAILABLE
- Both methods use `SELECT FOR UPDATE` row locking for concurrency safety

### Task 3: Match Routes
- `POST /api/v1/matches/{match_id}/arrive` - GPS-validated arrival with Pydantic schema
- `POST /api/v1/matches/{match_id}/finish` - Match completion by any player
- Added `MatchArriveSchema` (Pydantic BaseModel) for latitude/longitude request body validation

### Task 4: Verification
- 12 unit tests covering full lifecycle flow
- Tests validate: status transitions, GPS proximity rejection, duplicate arrival guard, non-player rejection, cart freeing, coordinate-less cart bypass
- All 12 tests passing

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | b8d2df1 | Add latitude/longitude coordinates to Cart model |
| 2 | e7f628a | Add arrive_match and finish_match lifecycle methods |
| 3 | 1c7b57e | Add arrive and finish match REST endpoints |
| 4 | b84baab | Add match lifecycle unit tests (12 tests) |

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - all endpoints are fully wired to service layer with real business logic.

## Self-Check: PASSED
