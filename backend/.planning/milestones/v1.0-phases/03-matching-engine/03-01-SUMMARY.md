---
phase: "03"
plan: "01"
subsystem: "matching-engine"
tags: [matching, queue, booking, concurrency, transaction]
dependency_graph:
  requires:
    - modules/matchmaking/repository/queue_entry_repository.py
    - modules/match/model/match_model.py
    - modules/booking/service/booking_service.py
    - modules/cart_type/repository/cart_type_repository.py
    - modules/timeslot/repository/timeslot_repository.py
  provides:
    - modules/match/service/match_engine_service.py
  affects:
    - modules/match/service/
    - modules/matchmaking/repository/
tech_stack:
  added: []
  patterns:
    - SELECT FOR UPDATE SKIP LOCKED (deadlock-free concurrency)
    - Isolated per-group DB transactions
    - FIFO queue processing via created_at ASC ordering
    - Rollback-on-booking-failure pattern (D-03)
    - Module-level singleton export for dependency injection
key_files:
  created:
    - backend/modules/match/service/match_engine_service.py
  modified:
    - backend/modules/matchmaking/repository/queue_entry_repository.py
decisions:
  - "SKIP LOCKED chosen over explicit advisory locks for deadlock-free concurrent matching"
  - "One transaction per matchable group ensures isolation with no cross-group interference"
  - "BookingService.create_booking() called before outer session.commit() so booking failures trigger full match rollback"
  - "cart_type resolved as first active global type (falls back from region-specific) since CartTypeRepository has no region filter"
  - "timeslot resolved by filtering all timeslots to today + matching location_id, sorted by start_time"
metrics:
  duration: "~15 minutes"
  completed_date: "2026-03-30"
  tasks_completed: 3
  files_created: 1
  files_modified: 1
---

# Phase 03 Plan 01: Core Matching Service and Transaction Logic Summary

## One-liner

`MatchEngineService` with SKIP LOCKED pair-matching, FIFO group scanning, and `BookingService` integration with rollback-on-failure for ground securing.

## What Was Built

### Task 1: Batch Locking Query in QueueEntryRepository (commit `06fafce`)

Added two methods to `queue_entry_repository.py`:

- `get_matchable_groups(session)`: Returns distinct `(region_id, sport_id, skill_level)` tuples where `COUNT(WAITING) >= 2` — used by the engine to discover which groups to attempt matching for.
- `find_and_lock_compatible_pair(region_id, sport_id, skill_level, session)`: Queries WAITING entries matching all three dimensions, orders by `created_at ASC` (FIFO), applies `with_for_update(skip_locked=True)`, and limits to 2 rows. Returns ORM instances for mutation in the caller's session.

### Task 2: MatchEngineService Grouping Logic (commit `282ef95`)

Created `modules/match/service/match_engine_service.py` with:

- `MatchEngineService` class with constructor accepting DI for all dependencies.
- `process_matching_cycle()`: Opens a discovery session, calls `get_matchable_groups()`, then runs `_attempt_match_for_group()` per group in isolated transactions.
- `_attempt_match_for_group()`: Locks 2 entries via SKIP LOCKED; creates `Match(status='MATCHED')` and two `MatchPlayer` records; marks both `QueueEntry` rows as `MATCHED`; calls `_secure_booking_for_match()`. Commits on success, rollbacks on `ValueError` or any exception.
- `_resolve_cart_type_id()`: Finds first active cart type globally.
- `_find_earliest_timeslot_id()`: Finds today's earliest timeslot by `location_id`.

### Task 3: BookingService Integration (commit `282ef95`)

`_secure_booking_for_match(match, creator_id, session)`:

1. Resolves earliest timeslot for today in the match's region.
2. Constructs booking payload: `{user_id, region_id, cart_type_id, timeslot_id}`.
3. Calls `self._booking_service.create_booking(payload)`.
4. On `ValueError` (no capacity, daily limit, no fee config): the caller's `except ValueError` block catches and rolls back the entire transaction, keeping `QueueEntry` rows in `WAITING` (D-03 compliance).

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

1. **SKIP LOCKED over advisory locks**: SQLAlchemy's `.with_for_update(skip_locked=True)` maps directly to Postgres `SELECT FOR UPDATE SKIP LOCKED` — zero deadlock risk, workers skip contested rows.

2. **BookingService called before outer commit**: Ensures booking failure triggers full match rollback. BookingService opens its own internal transaction, but its `ValueError` propagates to the match engine's catch block before `session.commit()`.

3. **Cart type resolution is global**: `CartTypeRepository.find_all()` returns all types; no region filter. First active type is used. Future improvement: add region-aware cart type lookup.

4. **Timeslot resolution uses `location_id` field**: `timeslot.location_id == region_id` since locations and regions are the same entity in this codebase.

## Known Stubs

None — all data flows are wired to real repositories and services.

## Self-Check: PASSED

- `backend/modules/match/service/match_engine_service.py` — FOUND
- `backend/modules/matchmaking/repository/queue_entry_repository.py` — FOUND (Task 1 methods verified)
- Commit `06fafce` — FOUND (feat(03-01): add batch locking query)
- Commit `282ef95` — FOUND (feat(03-01): implement MatchEngineService)
