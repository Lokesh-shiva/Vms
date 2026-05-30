# 06-PLAN: Arrival Deadlines & Penalties
**Milestone:** v1.1 | **Status:** Waiting for approval

## 📦 Changes Required

### 1. Database Model Update
**File:** [match_model.py](file:///d:/Vms%20project/backend/modules/match/model/match_model.py)
- Import `datetime` and `timedelta` (if not present).
- Define `MatchPenalty` class:
    - Table: `match_penalties`.
    - Columns: `id` (int), `user_id` (fk to users), `match_id` (fk to matches), `reason` (string), `expires_at` (datetime), `created_at` (datetime).
- Add `CANCELLED_NO_SHOW` to `Match.VALID_STATUSES`.

### 2. Cleanup Logic (Engine)
**File:** [match_engine_service.py](file:///d:/Vms%20project/backend/modules/match/service/match_engine_service.py)
- Create `cleanup_stale_matches(self, session)` method:
    - Filter for `Match` rows in `MATCHED` status where `created_at < now() - 20 minutes`.
    - For each stale match:
        - Identify all `MatchPlayer` sub-records where `has_arrived == False`.
        - Create a `MatchPenalty` for these users (reason: "Ghosting", expires_at: 4h from now).
        - Update match status to `CANCELLED_NO_SHOW`.
- Call `self.cleanup_stale_matches(discovery_session)` inside `process_matching_cycle()` before the "Phase 1" group discovery begins.

### 3. Join Block Logic (Matchmaking)
**File:** [matchmaking_service.py](file:///d:/Vms%20project/backend/modules/matchmaking/service/matchmaking_service.py)
- Update `join_queue()` function:
    - Before creating a new `QueueEntry`, search the `MatchPenalty` table for the `user_id` where `expires_at > now()`.
    - If an active penalty exists, raise `ValueError` with a clear message: "You are temporarily restricted from matchmaking until [time] due to a previous no-show."

## 🧪 Verification Steps

### Step 1: Migration Verification
Run a simple script to check if the `match_penalties` table is created on startup.

### Step 2: Timeout Enforcement
1. Manually insert a `Match` into the database with `status = 'MATCHED'` and `created_at = (now - 25 minutes)`.
2. Add two `MatchPlayer` entries for that match with `has_arrived = False`.
3. Call the engine trigger: `curl -X POST http://localhost:8000/api/v1/engine/trigger -H "X-Cron-Secret: dev-secret"`.
4. Verify the database:
    - `matches` table should show status `CANCELLED_NO_SHOW`.
    - `match_penalties` table should have two new records.

### Step 3: Hard Block Check
1. Attempt to join the queue with a penalized user: `POST /api/v1/matchmaking/play-now`.
2. Verify response is `400 Bad Request` or `422 Unprocessable Entity` with the correct penalty message.
