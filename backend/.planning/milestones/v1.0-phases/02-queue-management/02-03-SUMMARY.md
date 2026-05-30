---
phase: 02-queue-management
plan: 03
gap_closure: true
subsystem: user-model
tags: [fix, migration, region_id, matchmaking]
dependency_graph:
  requires: [01-db-models, locations-table]
  provides: [region_id on User model and users table]
  affects: [matchmaking queue join, queue controller]
tech_stack:
  patterns: [one-shot idempotent migration, SQLAlchemy ForeignKey]
key_files:
  modified:
    - modules/user/model/user_model.py
  created:
    - core/database/add_region_id_to_users.py
decisions:
  - region_id is nullable=True so existing users without a region are not broken
  - Migration script checks information_schema before ALTER TABLE for idempotency
metrics:
  duration: ~5 minutes
  completed: 2026-03-28
---

# Phase 02 Plan 03: Fix: Add region_id to User model — Summary

**One-liner:** Added nullable `region_id` FK column to User ORM model and live `users` table so queue joins no longer fail with HTTP 400.

## Accomplishments

- **Task 1 (Model update):** Added `ForeignKey` to the sqlalchemy import in `user_model.py`, added `region_id = Column(Integer, ForeignKey("locations.id"), nullable=True)` after `is_active`, and included `"region_id": self.region_id` in `to_dict()`.
- **Task 2 (Live migration):** Created `core/database/add_region_id_to_users.py` — an idempotent one-shot migration that checks `information_schema.columns` before issuing `ALTER TABLE users ADD COLUMN region_id INTEGER REFERENCES locations(id)`. Ran successfully.

## Files Modified

| File | Change |
|------|--------|
| `modules/user/model/user_model.py` | Added ForeignKey import, region_id column, region_id in to_dict() |
| `core/database/add_region_id_to_users.py` | Created idempotent migration script |

## Commits

| Hash | Message |
|------|---------|
| `b1eadda` | fix(02-03): add region_id column to User model |
| `8643c2b` | fix(02-03): migrate users table to add region_id column |

## Self-Check Results

1. `grep "region_id" modules/user/model/user_model.py` — returned 2 lines (column definition + to_dict entry). PASS.
2. `grep '"region_id"' modules/user/model/user_model.py` — returned `"region_id": self.region_id`. PASS.
3. Migration script output: `SUCCESS: region_id column added to users table.` PASS.
4. Server health check `GET http://localhost:8002/health` — returned `{"success":true,"data":null,"message":"Server is running."}`. PASS.

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED
