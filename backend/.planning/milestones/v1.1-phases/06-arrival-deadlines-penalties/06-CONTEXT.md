# Phase 06 Context: Arrival Deadlines & Penalties

## 🎯 Goal
Implement automated check-in enforcement for matchmaking. Cancel matches where players fail to arrive within 20 minutes and apply a temporary 4-hour block to ghosting accounts.

## 🛠️ Decisions

### 1. Penalty tracking
- **Table:** `match_penalties` (SQLAlchemy: `MatchPenalty`)
- **Fields:** `id`, `user_id`, `match_id`, `reason` (string), `expires_at` (datetime), `created_at` (datetime).
- **Duration:** 4 hours from creation.

### 2. Timer Logic
- **Deadline:** 20 minutes.
- **Reference point:** `Match.created_at` (since `MATCHED` status is set at creation).
- **Condition:** If `Match.status == 'MATCHED'` and `now() > (created_at + 20min)`, and both players haven't arrived.

### 3. Execution
- **Cleanup Trigger:** `MatchEngineService.process_matching_cycle()` will call `self.cleanup_stale_matches(session)` at the start of every sweep.
- **Enforcement:** `MatchmakingService.join_queue()` will check for active (unexpired) penalties for the user before allowing them to join.

## 📦 Success Criteria
- [ ] Matches older than 20m in `MATCHED` status are cancelled.
- [ ] Non-arrived players receive a 4-hour block record.
- [ ] Blocked players cannot join the queue via `/play-now`.
- [ ] Cleanup is triggered automatically by the matching engine cron.
