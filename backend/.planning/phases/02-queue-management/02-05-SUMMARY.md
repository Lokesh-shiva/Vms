---
phase: 02-queue-management
plan: "05"
subsystem: matchmaking-api
tags: [matchmaking, pricing, response-shape, error-handling, api-fix]
dependency_graph:
  requires: [02-04-PLAN.md]
  provides: [correct-matchmaking-shapes, pricing-endpoint]
  affects: [TC001, TC002, TC003, TC004, TC005, TC006, TC007, TC008, TC009, TC010]
tech_stack:
  added: []
  patterns:
    - Flat API response for join_queue (fields at top level instead of nested in "data")
    - JSONResponse _error helper for consistent 400 error shape with "message" key
    - FastAPI HTTPException handler overriding default "detail" key with "message" key
    - Pydantic Field(gt=0) for auto-422 validation on numeric inputs
key_files:
  created:
    - modules/pricing/controller/__init__.py
    - modules/pricing/controller/pricing_routes.py
  modified:
    - modules/matchmaking/controller/matchmaking_routes.py
    - modules/matchmaking/service/matchmaking_service.py
    - main.py
decisions:
  - join_queue returns flat response (entry_id at top level) matching TestSprite TC001/TC003 expectations
  - _error helper returns JSONResponse directly, bypassing FastAPI's HTTPException "detail" wrapping
  - HTTPException handler added to main.py with both "message" and "detail" keys for compatibility
  - Pricing endpoint is public (no auth) — pricing is informational data
  - sport_id gt=0 Pydantic constraint enables FastAPI auto-422 for TC010
metrics:
  duration: "~10 minutes"
  completed_date: "2026-03-30"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 5
---

# Phase 02 Plan 05: Fix Matchmaking Response Shapes and Pricing Endpoint Summary

Resolved matchmaking endpoint response shape mismatches and created the missing POST /api/v1/pricing/calculate endpoint, enabling TC001-TC010 to pass.

## What Was Built

### Task 1: Fix matchmaking routes — flat join response, standardized error shape, corrected messages

Three files were updated to correct response shapes and error messages:

**modules/matchmaking/service/matchmaking_service.py:**
- `get_queue_status()` error message changed from `f"User {user_id} has no active queue entry."` to `"User has no active queue entry."` (TC008 fix — no user_id interpolation)
- `join_queue()` skill_level error message changed to `"Invalid skill_level. Must be one of BEGINNER|INTERMEDIATE|ADVANCED."` (TC004 fix — exact expected format)

**modules/matchmaking/controller/matchmaking_routes.py:**
- Added `_error(message, status_code=400)` helper that returns JSONResponse with `{"success": False, "data": None, "message": message}` — bypasses FastAPI's default `{"detail": "..."}` wrapping
- `join_queue` endpoint refactored to return FLAT response: all fields (`entry_id`, `user_id`, `region_id`, etc.) at the top level rather than nested inside `"data"` (TC001/TC003 fix)
- Region check error message corrected to exactly `"Your account has no region set. Please update your profile."` (TC002 fix — removed "before joining a match" suffix)
- All three endpoints (`join_queue`, `leave_queue`, `queue_status`) now use `_error(str(e))` for ValueError handling instead of raising HTTPException
- Removed unused `HTTPException` import from matchmaking_routes (no longer needed since errors go through `_error`)

**main.py:**
- Added `HTTPException` to the fastapi import
- Added `http_exception_handler` registered BEFORE `global_exception_handler` (order matters) that returns `{"success": False, "data": None, "message": exc.detail, "detail": exc.detail}` — ensures any remaining HTTPException raises across the codebase also return a `"message"` key

### Task 2: Create pricing controller and register in main.py

**modules/pricing/controller/__init__.py:** Empty package init file created.

**modules/pricing/controller/pricing_routes.py:**
- `CalculatePricingRequest` Pydantic model with `region_id: int = Field(..., gt=0)` and `sport_id: int = Field(..., gt=0)` — the `gt=0` constraint causes FastAPI to auto-return 422 when `sport_id=0` is passed (TC010 fix)
- `POST /api/v1/pricing/calculate` endpoint calls `PricingService(db).calculate_price(region_id, sport_id)` and returns the dict directly: `{base_price, time_factor, demand_factor, queue_count, final_price}` (TC009 fix)
- No auth required — pricing is public information

**main.py:**
- Imported `router as pricing_router` from `modules.pricing.controller.pricing_routes`
- Registered with `app.include_router(pricing_router)` after matchmaking_router

## Commits

| Task | Commit | Message |
|------|--------|---------|
| 1 | `0a12bb6` | fix(02-05): flat join response, standardized error shape, corrected messages |
| 2 | `398b10b` | feat(02-05): create pricing controller and register in main.py |

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — all endpoints wire to real service implementations with real DB calls.

## Self-Check: PASSED

Files verified to exist:
- `modules/matchmaking/controller/matchmaking_routes.py` — modified, contains `_error` helper and flat join response
- `modules/matchmaking/service/matchmaking_service.py` — modified, contains corrected error messages
- `modules/pricing/controller/__init__.py` — created
- `modules/pricing/controller/pricing_routes.py` — created, contains `sport_id gt=0` and `/calculate` endpoint
- `main.py` — modified, contains `http_exception_handler` and `pricing_router` registration

Commits verified: `0a12bb6` and `398b10b` both present in git log.
