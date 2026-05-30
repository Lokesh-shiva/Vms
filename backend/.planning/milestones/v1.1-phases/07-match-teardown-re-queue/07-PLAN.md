# Phase 07: Match Teardown & Re-Queue (Strike System)
**Wave:** 1
**Autonomous:** true
**Requirements:** EDGE-03, EDGE-04
**Modified Files:** 
- modules/user/model/user_model.py
- modules/user/repository/user_repository.py
- modules/match/service/match_engine_service.py
- modules/matchmaking/repository/queue_entry_repository.py

## Objective
Implement match teardown logic that recovers ground inventory via `BookingService` and re-queues arrived players with priority. Incorporate a **2-Strike Rule** for no-shows: players are excused for 2 ghosting instances, with the penalty triggering only on the 3rd.

---

## Wave 1: Schema & Repositories

### [TASK-01] Add ghost_strikes to User Model
<read_first>
- modules/user/model/user_model.py
</read_first>
<action>
Add `ghost_strikes = Column(Integer, nullable=False, default=0)` to the `User` class. Update `to_dict()` to include `ghost_strikes`.
</action>
<acceptance_criteria>
- `User` model contains `ghost_strikes` column.
- `User.to_dict()` contains `"ghost_strikes"`.
- Codebase greps `ghost_strikes` in `user_model.py`.
</acceptance_criteria>

### [TASK-02] Implement Strike Management in UserRepository
<read_first>
- modules/user/repository/user_repository.py
</read_first>
<action>
Add `increment_ghost_strikes(self, user_id: int, session=None)` and `reset_ghost_strikes(self, user_id: int, session=None)` methods.
- `increment_ghost_strikes`: Increments the count by 1.
- `reset_ghost_strikes`: Sets the count to 0.
</action>
<acceptance_criteria>
- `UserRepository` has `increment_ghost_strikes` and `reset_ghost_strikes`.
- Methods use session-bound updates for transaction safety.
</acceptance_criteria>

---

## Wave 2: Match Engine Orchestration

### [TASK-03] Update Match Teardown Logic (2-Strike & Re-Queue)
<read_first>
- modules/match/service/match_engine_service.py
- modules/booking/service/booking_service.py (cancel_booking)
</read_first>
<action>
Modify `cleanup_stale_matches()`:
1. For match in `stale_matches`:
   - Call `self._booking_service.cancel_booking(match.booking_id)` (D-01).
   - Identify players: `arrived_players` (has_arrived=True) and `ghost_players` (has_arrived=False).
   - **For ghost_players:**
     - Fetch user. Check `ghost_strikes`.
     - If `strikes < 2`: call `user_repository.increment_ghost_strikes()`. Notify user (Excuse).
     - If `strikes >= 2`: call `user_repository.reset_ghost_strikes()`, then apply `MatchPenalty` (4h block).
   - **For arrived_players:**
     - Call `matchmaking_service.join_queue()` (or repo create) with a historical `created_at` = `now - 2 hours` to grant FIFO priority (D-02).
     - Flag the entry with a reason `RE_QUEUE_OPPONENT_NO_SHOW` for frontend toast (D-03).
</action>
<acceptance_criteria>
- `cancel_booking` is called for every stale match.
- Users with < 2 strikes do NOT get a `MatchPenalty` record.
- Users with 2 strikes get a `MatchPenalty` and strikes are reset.
- Arrived players have a new `QueueEntry` with a timestamp > 1 hour old.
</acceptance_criteria>

---

## Verification Plan

### Automated Tests
1. **Scenario: Strike 1 (Excuse)**
   - Create match. Player A arrives, Player B doesnt.
   - Run cleanup.
   - Result: Booking cancelled, Player A re-queued (priority), Player B strikes = 1, No penalty.
2. **Scenario: Strike 3 (Penalty)**
   - Player B already has strikes = 2.
   - Run cleanup.
   - Result: Booking cancelled, Player B strikes = 0, MatchPenalty created (4h).

### Manual Verification
1. Mock a stale match in DB.
2. Trigger `/engine/trigger` via Swagger UI.
3. Verify `bookings` table shows status `CANCELLED`.
4. Verify `queue_entries` has the arrived player.
5. Verify `users` table `ghost_strikes` field.

## must_haves
- All stale match bookings MUST be released to 'AVAILABLE'.
- Re-queued players MUST have priority over new joiners.
- Penalties MUST NOT be issued before the 3rd strike.
- Strikes MUST reset after a penalty is issued.
