---
plan: 08-01
phase: 08-pricing-queue-ux-perception
status: complete
completed: 2026-04-02
tasks_completed: 2/2
self_check: PASSED
---

# Plan 08-01: UX String Injection — SUMMARY

## What Was Built

Two human-readable string fields injected into existing API return dicts:

### Task 1 — `reason` in `PricingService.calculate_price()`
Added demand-precedence reason string derived from existing `demand_f` and `time_f` locals:
- `demand_f > 1.0` → `"High demand in your area"`
- `time_f > 1.0` (peak only) → `"Peak hours (5–9 PM)"`
- Both = 1.0 → `"Standard pricing"`
- When both active, demand takes precedence (D-05)

Field added to the `calculate_price()` return dict — automatically propagates into `get_queue_status()` pricing sub-object since it calls `calculate_price()` internally.

### Task 2 — `wait_estimation_msg` in `MatchmakingService.get_queue_status()`
Added top-level `wait_estimation_msg` string derived from `entry["status"]` and `players_searching`:
- `MATCHED` → `"You're matched — arrive in 20 mins or lose your spot"`
- `WAITING` + `players_searching == 0` → `"No players nearby yet — hang tight"`
- `WAITING` + `players_searching > 0` → `"{N} players nearby — match likely in {X} mins"`
  - `X = max(1, round(players_searching * 120 / 60))` — uses existing `_WAIT_PER_PLAYER_SECONDS`

## Commits

- `c7fa58b` feat(08-01): add reason field to PricingService.calculate_price()
- `393ae6d` feat(08-01): add wait_estimation_msg to MatchmakingService.get_queue_status()

## Key Files

- `modules/pricing/service/pricing_service.py` — `calculate_price()` now returns `reason`
- `modules/matchmaking/service/matchmaking_service.py` — `get_queue_status()` now returns `wait_estimation_msg`

## Deviations

None — plan executed exactly as specified.

## Self-Check

- [x] `reason` key present in `calculate_price()` return dict
- [x] All three reason strings present with correct conditions
- [x] `wait_estimation_msg` key present as top-level field in `get_queue_status()` return
- [x] All three wait message cases covered (MATCHED, empty queue, active queue)
- [x] No existing keys removed or renamed
- [x] No new endpoints, models, or routes added
- [x] Smoke checks pass (`inspect.getsource` assertions)
