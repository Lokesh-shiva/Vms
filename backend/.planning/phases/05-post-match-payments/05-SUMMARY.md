---
phase: 05
plan: 01-02
subsystem: payments
tags: [payments, match-lifecycle, split-payments, post-match]
dependency_graph:
  requires: [04-01]
  provides: [split-payment-records]
  affects: [payment_model, match_model, match_service, payment_service]
tech_stack:
  added: []
  patterns: [lazy-import-circular-dep-avoidance, non-fatal-side-effect-after-commit]
key_files:
  created:
    - backend/modules/payment/tests/test_split_payments.py
  modified:
    - backend/modules/payment/model/payment_model.py
    - backend/modules/payment/repository/payment_repository.py
    - backend/modules/payment/service/payment_service.py
    - backend/modules/match/model/match_model.py
    - backend/modules/match/service/match_engine_service.py
    - backend/modules/match/service/match_service.py
decisions:
  - Added booking_id FK to Match model to enable PaymentService to find the booking without a separate cart lookup
  - Split payment creation is non-fatal in finish_match (payment failure must not roll back match completion)
  - Used lazy import in finish_match to avoid circular dependency MatchService -> PaymentService
  - Kept find_by_booking_id() returning single latest payment for backward compat; added find_by_booking_id_all() for list access
metrics:
  duration: ~25min
  completed: "2026-04-01"
  tasks_completed: 4
  files_modified: 6
  files_created: 1
---

# Phase 05: Post-Match Payments Summary

**One-liner:** Automatic equal-split PENDING payment records (one per player) created on match completion using MANUAL_UPI provider with VMS-{booking_id}-P{user_id}-{rand} reference codes.

## What Was Built

Phase 05 implements the post-match payment split flow for the matchmaking platform. When a player calls `POST /match/{id}/finish`, two PENDING payment records are automatically created — one per participant — each charged half of the total booking amount (estimated_total + booking_fee).

## Plan 01: Payment Model & Repo Update (commit bab04c9)

**Tasks completed:**

1. **Updated `Payment` model** (`payment_model.py`):
   - Added `user_id` column (Integer, FK -> `users.id`, nullable, indexed) — identifies which player this payment belongs to
   - Added `match_id` column (Integer, FK -> `matches.id`, nullable, indexed) — links payment back to the triggering match
   - Replaced the implicit unique-on-`booking_id` behavior with an explicit `UniqueConstraint("booking_id", "user_id")` — allows multiple split payments per booking (one per player) while blocking duplicates
   - Updated `to_dict()` to include `user_id` and `match_id`

2. **Updated `PaymentRepository`** (`payment_repository.py`):
   - `create()` now passes `user_id` and `match_id` through to the ORM constructor
   - Added `find_by_booking_id_all(booking_id)` — returns all payments for a booking (needed for split payment inspection)
   - Added `find_by_user_and_booking(user_id, booking_id)` — retrieves the payment for a specific player+booking combination
   - Preserved `find_by_booking_id()` returning latest single payment for full backward compatibility

## Plan 02: Split Payment Logic & Integration (commits bcb47df, 7d0c15b)

**Tasks completed:**

3. **Added `booking_id` FK to Match model** (`match_model.py`) — deviation from plan, required for PaymentService to look up the booking without a secondary cart-based join. Also updated `to_dict()` and `MatchEngineService._secure_booking_for_match()` to store `booking.id` on `match.booking_id`.

4. **Implemented `PaymentService.create_split_payments(match_id)`** (`payment_service.py`):
   - Fetches Match + MatchPlayer records via SessionLocal
   - Validates match exists and has a `booking_id`
   - Fetches Booking to get `estimated_total` + `booking_fee`
   - Calculates `split_amount = round(total / len(players), 2)`
   - Creates one PENDING MANUAL_UPI payment per player with reference `VMS-{booking_id}-P{user_id}-{rand}`
   - Added `_generate_split_reference_code()` helper distinct from the legacy `_generate_reference_code()`

5. **Updated `MatchService.finish_match()`** (`match_service.py`):
   - After committing match COMPLETED + freeing cart, calls `PaymentService().create_split_payments(match_id)` via lazy import
   - Payment failure is caught and logged (non-fatal) — match stays COMPLETED regardless

6. **8 unit tests** (`test_split_payments.py`) — all passing:
   - Two payments created per 2-player match
   - Split amount = 50.0 for total=100.0
   - Status = PENDING on both
   - Correct match_id and booking_id linkage
   - Reference code format regex validated
   - Distinct user_id per payment
   - ValueError on missing match
   - ValueError when match has no booking_id

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical Functionality] Added booking_id to Match model**
- **Found during:** Task 3 (implementing create_split_payments)
- **Issue:** `PaymentService.create_split_payments` needs to find the Booking for a match. The Match model had no `booking_id` field — the only link was through `cart_id` / `assigned_cart_id` which is indirect and not reliable post-game (cart may have been reassigned).
- **Fix:** Added `booking_id` FK column to Match model. Updated `MatchEngineService._secure_booking_for_match()` to write `match.booking_id = booking["id"]` when an instant booking is secured.
- **Files modified:** `match_model.py`, `match_engine_service.py`
- **Commit:** bcb47df

## Known Stubs

None — all payment records are fully created with real data wired from match and booking records.

## Self-Check: PASSED
