# Phase 05: Post-Match Payments - Plan

Trigger split payment records for both players upon match completion.

**Status:** Ready to execute
**Requirements:** PAY-01, PAY-02

## Goal
Automatically create two `PENDING` payment records (one per player) when a match is marked as `COMPLETED`. The total booking amount is split equally.

## Waves

| Wave | Plans | What it builds |
|------|-------|----------------|
| 1    | 01    | Payment Model & Repo Update |
| 2    | 02    | Split Payment Logic & Integration |

---

## Plan 01: Payment Model & Repo Update
**Wave:** 1
**Requirements:** PAY-01
**Autonomous:** true

### Tasks
<task>
<read_first>
- `backend/modules/payment/model/payment_model.py`
</read_first>
<action>
Modify the `Payment` model in `payment_model.py`:
- Add `user_id` column (Integer, ForeignKey to `users.id`, index=True).
- Add `match_id` column (Integer, ForeignKey to `matches.id`, nullable=True, index=True).
- Remove the `UniqueConstraint` on `booking_id` (if it exists) or replace it with a unique constraint on `(booking_id, user_id)`.
</action>
<acceptance_criteria>
- `payment_model.py` contains `user_id` and `match_id` columns.
- `UniqueConstraint` on `booking_id` is removed or updated.
</acceptance_criteria>
</task>

<task>
<read_first>
- `backend/modules/payment/repository/payment_repository.py`
</read_first>
<action>
Update `PaymentRepository`:
- Ensure `find_by_booking_id` returns a list of payments.
- Add `find_by_user_and_booking(user_id, booking_id)` method.
</action>
<acceptance_criteria>
- `payment_repository.py` has `find_by_user_and_booking` method.
</acceptance_criteria>
</task>

---

## Plan 02: Split Payment Logic & Integration
**Wave:** 2
**Requirements:** PAY-01, PAY-02
**Depends on:** 01
**Autonomous:** true

### Tasks
<task>
<read_first>
- `backend/modules/payment/service/payment_service.py`
- `backend/modules/match/service/match_service.py`
</read_first>
<action>
Implement `create_split_payments(match_id)` in `PaymentService`:
1. Fetch `Match` by ID with participants (`MatchPlayer`).
2. Fetch `Booking` associated with the match.
3. Calculate `amount = (booking.estimated_total + booking.booking_fee) / 2`.
4. Loop through participants and create a `Payment` record for each:
   - `status`: "PENDING"
   - `provider`: "MANUAL_UPI"
   - `amount`: calculated split
   - `reference_code`: `VMS-{booking_id}-P{user_id}-{rand}`
   - `user_id`: participant user ID
   - `match_id`: match ID
</action>
<acceptance_criteria>
- `payment_service.py` contains `create_split_payments` method with the specified logic.
</acceptance_criteria>
</task>

<task>
<read_first>
- `backend/modules/match/service/match_service.py`
</read_first>
<action>
Update `finish_match` in `MatchService`:
- After marking the match as `COMPLETED` and freeing the cart, call `payment_service.create_split_payments(match_id)`.
- Use a lazy import or ensure no circular dependency (MatchService -> PaymentService).
</action>
<acceptance_criteria>
- `match_service.py` calls `create_split_payments` at the end of `finish_match`.
</acceptance_criteria>
</task>

## Verification Plan

### Automated Tests
- **Unit Test**: Mock match with 2 players, mock booking with amount 100. Call `create_split_payments`. Verify 2 payments of 50 are created.
- **Integration Test**: Finish a match and verify DB state for payments.

### Manual Verification
- Execute `finish_match` endpoint via Postman.
- Check `payments` table for two new records linked to the same `booking_id` but different `user_id`.
