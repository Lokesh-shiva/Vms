---
phase: 02-queue-management
plan: "04"
subsystem: auth-and-tests
tags: [auth, testsprite, login, seed, phone]
dependency_graph:
  requires: []
  provides: [flat-login-response, seeded-test-users, phone-based-test-login]
  affects: [TC001, TC002, TC003, TC004, TC005, TC006, TC007, TC008]
tech_stack:
  added: []
  patterns: [flat-dict-response, idempotent-seed-script]
key_files:
  created:
    - core/database/seed_test_users.py
  modified:
    - modules/auth/controller/auth_routes.py
    - testsprite_tests/TC001_join_queue_with_valid_data.py
    - testsprite_tests/TC002_join_queue_without_region_should_fail.py
    - testsprite_tests/TC003_join_queue_with_duplicate_waiting_entry_should_fail.py
    - testsprite_tests/TC004_join_queue_with_invalid_skill_level_should_fail.py
    - testsprite_tests/TC005_leave_queue_with_active_waiting_entry.py
    - testsprite_tests/TC006_leave_queue_without_active_waiting_entry_should_fail.py
    - testsprite_tests/TC007_get_queue_status_with_active_entry.py
    - testsprite_tests/TC008_get_queue_status_without_active_entry_should_fail.py
decisions:
  - Login endpoint now returns flat dict (access_token, token_type, role, message) instead of _success() wrapper
  - Test users seeded by phone; seed script is idempotent (updates if exists)
  - TC007 token key standardized from "token" to "access_token" to match flat response
  - TC004/TC005 passwords standardized to "testpassword" to match seeded value for +10000000001
metrics:
  duration: "~15 minutes"
  completed_date: "2026-03-30"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 9
---

# Phase 02 Plan 04: Fix Login for TestSprite Tests (Phone + Flat Response) Summary

**One-liner:** Flattened login response to top-level access_token, seeded 4 phone-based test users, and updated TC001-TC008 to send phone instead of email in login payloads.

## What Was Done

### Task 1: Flatten login response in auth_routes.py

Changed the `/api/v1/auth/login` endpoint to return a flat dict literal instead of wrapping with `_success()`. Before: `{"success": true, "data": {"access_token": ...}, "message": ...}`. After: `{"access_token": ..., "token_type": ..., "role": ..., "message": ...}`. The `/register` and `/me` endpoints continue using `_success()` unchanged.

Commit: `ef1aba4`

### Task 2: Create seed_test_users.py and seed the DB

Created `core/database/seed_test_users.py` with an idempotent `run()` function that seeds 4 test users by phone number. Script updates `password_hash` and `region_id` if user already exists (safe to re-run). All 4 users created successfully on first run.

- `+10000000001` / testpassword / region_id=1 (TC001, TC003, TC004, TC005, TC007)
- `+10000000002` / TestPass123! / region_id=NULL (TC002)
- `+10000000003` / TestPassword123! / region_id=1 (TC008)
- `+10000000004` / StrongPassw0rd! / region_id=1 (TC006)

Commit: `34026df`

### Task 3: Update TC001-TC008 test files to use phone login

Updated all 8 test files to replace email-based login credentials with phone numbers. Additional fixes:
- TC004: password standardized from "TestPass123!" to "testpassword" (matches seeded user +10000000001)
- TC005: password standardized from "TestPassword123!" to "testpassword" (same user +10000000001)
- TC007: token extraction changed from `.get("token")` to `.get("access_token")` to match flat response

Commit: `e7d4ebf`

## Decisions Made

1. **Flat login response** — Tests expected `access_token` at top level; wrapping in `_success()` was the root cause of all TC001-TC008 login failures. Only the login endpoint changed; register and me endpoints are unaffected.
2. **Idempotent seed** — Seed script checks for existing phone before insert, updates if found. Safe to re-run without duplicates.
3. **Standardized testuser password** — Multiple test files referenced different passwords for the same user (+10000000001). Standardized to "testpassword" across TC001/TC003/TC004/TC005/TC007.

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

```
=== 1. Login response flat ===
MATCH: "access_token": token_data["access_token"],

=== 2. No wrapped login return ===
OK - flat

=== 3. Seed script has phone numbers ===
MATCH: +10000000001

=== 4. No email in test login payloads ===
OK - no email in tests
```

## Known Stubs

None. All 4 test users are seeded with real bcrypt-hashed passwords and valid region_id values. The seed script runs against the actual database.

## Self-Check: PASSED

- `modules/auth/controller/auth_routes.py` — modified, exists
- `core/database/seed_test_users.py` — created, exists
- TC001-TC008 test files — all modified, no email in login payloads
- Commits: ef1aba4, 34026df, e7d4ebf — all present in git log
