# Time-Based Billing — Design Spec
**Date:** 2026-05-29  
**Status:** APPROVED — ready for implementation plan  
**Scope:** Replace fixed-fee payment with matching fee + 45-min block billing

---

## Problem

Current model: single fixed payment upfront (booking_fee + items). This doesn't reflect actual usage — a 20-min session costs the same as a 90-min one. The client wants demand-sensitive, time-based pricing.

---

## New Payment Model

### Two-payment flow

```
1. MATCHING FEE (upfront, before session)
   ↓ user pays via UPI
   ↓ admin approves
   → booking: CONFIRMED

2. SESSION STARTS (timer begins)
   → booking: IN_PROGRESS

3. SESSION ENDS (timer stops, system calculates bill)
   ↓ TIME BILL sent to user
   ↓ user pays via UPI
   ↓ admin approves
   → booking: COMPLETED
```

### Billing formula

```
matching_fee                      ← flat, paid upfront
time_bill = blocks × rate_per_block × surge_multiplier
blocks    = ceil(session_minutes / block_duration_minutes)
total     = matching_fee + time_bill
```

Example (rate = ₹60/45min, no surge):
- 30 min session → 1 block → ₹60
- 46 min session → 2 blocks → ₹120
- 90 min session → 2 blocks → ₹120
- 91 min session → 3 blocks → ₹180

---

## Session Timer — Start Trigger

**Decision: Captain confirms, with grace-period fallback.**

Three stakeholders could start the timer:

| Who | Trust issue | Decision |
|-----|------------|----------|
| User | Would delay to get more free time | ❌ |
| Admin | Would forget, adds ops burden | ❌ |
| **Captain** | Neutral 3rd party at the ground | ✅ |

**Flow:**
1. Booking reaches `CONFIRMED` → Captain assigned to that ground gets a notification
2. Captain taps "Start Session" in their panel → `session_started_at` set → timer begins
3. **Grace period:** if Captain doesn't start within 10 minutes of the booking's scheduled slot start, the system auto-starts (sets `session_started_at = booking.timeslot.start_time + 10 min`)
4. Captain also taps "End Session" OR user requests end → `session_ended_at` set

**Why Captain, not GPS:**
GPS check-in requires user location permission, has accuracy issues indoors, and adds complexity. Captains are already in the system and physically present. Phase 03 can layer GPS on top for verification.

---

## Session Timer — End Trigger

Any of these ends the session:
1. **Captain taps "End Session"** (primary)
2. **Admin taps "End Session"** from Bookings screen (fallback if captain unavailable)
3. **Max duration reached** — configurable per ground/sport (default: 3 hours) → auto-end, triggers dispute flag

---

## Pricing Config Changes

`region_cart_type_configs` gains new columns:

| Column | Type | Description |
|--------|------|-------------|
| `matching_fee` | NUMERIC(10,2) | Flat upfront fee |
| `rate_per_block` | NUMERIC(10,2) | Price per 45-min block |
| `block_duration_minutes` | INT (default 45) | Block size in minutes |
| `max_duration_minutes` | INT (default 180) | Session auto-end cap |
| `surge_enabled` | BOOLEAN (default false) | Whether surge is on |
| `surge_multiplier` | NUMERIC(4,2) (default 1.0) | Current surge multiplier |

**Existing `booking_fee` kept for backward compat but deprecated** — new bookings use `matching_fee`.

### Surge pricing (manual for now)
- OPS_MANAGER / SUPER_ADMIN can set `surge_multiplier` per region+sport via admin app
- Range: 1.0 (normal) to 3.0 (max surge)
- Displayed on user app as "Peak hours — 1.5× rate applies"
- Snapshot at session start (like cancellation_fee_pct_snapshot)

---

## Booking State Machine (updated)

```
PENDING_PAYMENT
    ↓ matching fee paid + admin approves
CONFIRMED
    ↓ captain starts session
IN_PROGRESS  ← timer running
    ↓ captain/admin ends session
AWAITING_TIME_PAYMENT  ← NEW state
    ↓ user pays time bill + admin approves
COMPLETED

Side exits:
PENDING_PAYMENT → CANCELLED (cancel before paying)
CONFIRMED       → CANCELLED (cancel before session, refund matching fee)
IN_PROGRESS     → CANCELLED (force cancel by admin, pro-rata bill may apply)
```

### New booking fields

| Field | Type | Description |
|-------|------|-------------|
| `session_started_at` | TIMESTAMP | When timer started |
| `session_ended_at` | TIMESTAMP | When timer stopped |
| `session_minutes` | INT | Calculated duration |
| `session_blocks` | INT | Calculated blocks (ceil) |
| `time_bill_amount` | NUMERIC(10,2) | Calculated time charge |
| `surge_multiplier_snapshot` | NUMERIC(4,2) | Surge at session start |
| `payment_type` on payments | VARCHAR(50) | MATCHING_FEE or TIME_BILL |

---

## API Changes

### New endpoints

```
POST /api/v1/bookings/{id}/captain-start
  → Captain starts session (require_role: CAPTAIN or OPS_MANAGER/SUPER_ADMIN)
  → Sets session_started_at, booking → IN_PROGRESS

POST /api/v1/bookings/{id}/captain-end
  → Captain ends session (require_role: CAPTAIN or OPS_MANAGER/SUPER_ADMIN)
  → Sets session_ended_at, calculates bill, booking → AWAITING_TIME_PAYMENT
  → Creates a TIME_BILL payment record for the user

GET /api/v1/bookings/{id}/session-status
  → Returns current elapsed time, current block count, running bill estimate
  → Used by admin app timer display + user app

PUT /api/v1/admin/config/pricing/{config_id}/surge
  → OPS_MANAGER sets surge_multiplier for a region+sport
```

### Modified endpoints

```
POST /api/v1/payments/initiate/{booking_id}
  → Now creates MATCHING_FEE payment (not full booking_fee+items)
  → Amount = matching_fee from config

POST /api/v1/payments/approve/{payment_id}
  → If payment_type=MATCHING_FEE → CONFIRMED (existing behaviour)
  → If payment_type=TIME_BILL → COMPLETED (new branch)
```

---

## Admin App Changes

### Bookings screen

**CONFIRMED card:**
- Shows: Region, Sport, Ground, Date, Time, Matching Fee paid ✅
- Actions: Cancel | (waiting for captain to start)

**IN_PROGRESS card:**
- Shows live timer: `⏱ 00:47:23` (elapsed)
- Shows running estimate: `Current bill: ₹120 (2 blocks)`
- Actions: End Session (admin fallback only)

**AWAITING_TIME_PAYMENT card:**
- Shows: Duration, Blocks, Amount due
- No action for admin (waiting for user to pay)
- Once user pays → shows UNDER_REVIEW → admin approves

### Pricing screen (Manage → Pricing)

Extend FeeConfigScreen to show + edit:
- Matching fee
- Rate per block
- Block duration
- Max duration
- Surge toggle + multiplier

### Captain panel

Captain screen gains a new "Active Sessions" section:
- Bookings assigned to their region in CONFIRMED state → "Start Session" button
- Bookings in IN_PROGRESS → shows timer + "End Session" button

---

## What's NOT in this spec (Phase 03)

- Automated surge based on live demand (queue depth → auto-multiplier)
- GPS-based check-in to replace/supplement captain confirmation
- In-app UPI payment (currently manual submit)
- Dispute resolution for timer disagreements

---

## Implementation Order

```
1. DB migration: add new columns to region_cart_type_configs + bookings + payments
2. Backend: update pricing config service + schema
3. Backend: captain-start / captain-end endpoints + billing calculation
4. Backend: booking state machine (add AWAITING_TIME_PAYMENT)
5. Backend: session-status endpoint (elapsed time + running bill)
6. Admin app: Bookings screen — live timer on IN_PROGRESS, new AWAITING state
7. Admin app: Pricing screen — new fields
8. Admin app: Captain panel — active sessions section
9. Tests: billing calculation edge cases, state machine transitions
```
