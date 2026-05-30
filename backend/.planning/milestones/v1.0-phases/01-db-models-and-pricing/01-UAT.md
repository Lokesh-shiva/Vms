---
status: complete
phase: 01-db-models-and-pricing
source: implementation-plan
started: 2026-03-27T22:40:24+05:30
updated: 2026-03-27T22:40:24+05:30
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

## Current Test

[testing complete]

## Tests

### 1. Cold Start Smoke Test
expected: Server starts cleanly with uvicorn and the /health endpoint returns {"success": true, "data": null, "message": "Server is running."}. The `queue_entries` and `sports` tables are auto-created in the database on startup (visible in Neon console).
result: pass

### 2. QueueEntry Model Integrity
expected: The `queue_entries` table exists in the DB with columns: id, user_id, region_id, sport_id, skill_level, status, created_at. The `sport_id` column references `sports.id` (foreign key). Status defaults to "WAITING".
result: pass

### 3. Sport Model Integrity
expected: The `sports` table exists in the DB with columns: id, name (unique), is_active (default true).
result: pass

### 4. Pricing Service - Base Price
expected: Calling PricingService.calculate_price(region_id=1, sport_id=1) with an empty queue returns a price of 200.0 (or the configured BASE_MATCH_PRICE) with time_factor and demand_factor both at 1.0.
result: pass

### 5. Pricing Service - Peak Hour Multiplier
expected: Calling PricingService.calculate_price() at a time between 17:00 and 21:00 UTC returns time_factor=1.5. Outside those hours, time_factor=1.0.
result: pass

### 6. Pricing Service - Demand Surge
expected: When get_active_queue_count(region_id, sport_id) returns >= 10, demand_factor is 1.5. When 5-9, it's 1.25. When < 5, it's 1.0.
result: pass

### 7. Match Model Statuses Updated
expected: The `Match` model/VALID_STATUSES now includes: WAITING, MATCHED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED. Old statuses OPEN and FULL are removed.
result: pass

### 8. Match Model sport_id Field
expected: The `Match` model has a `sport_id` column (FK → sports.id) and the `timeslot_id` column is removed.
result: pass

### 9. MatchPlayer has_arrived Field
expected: The `MatchPlayer` model has a `has_arrived` boolean field (default False).
result: pass
note: Transient instance returns None before DB insert — correct SQLAlchemy behavior. Default of False applies at insert time.

### 10. New Models Registered in main.py
expected: `main.py` imports `QueueEntry` from `modules.matchmaking.model.queue_entry_model` and `Sport` from `modules.sport.model.sport_model` so that `Base.metadata.create_all()` creates their tables at startup.
result: pass
note: Raw REPL showed True/False because Sport wasn't imported in that session. Both tables confirmed created in Neon DB (Test 1). main.py imports both models correctly.

## Summary

total: 10
passed: 10
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]
