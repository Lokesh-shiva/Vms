---
phase: 07-match-teardown-re-queue
plan: "07"
subsystem: matchmaking
tags: [strike-system, queue, re-queue, booking-cancel, match-teardown, no-show]

# Dependency graph
requires:
  - phase: 06-arrival-deadlines-penalties
    provides: MatchPenalty model, cleanup_stale_matches skeleton, join_queue penalty gate

provides:
  - 2-strike forgiveness rule before MatchPenalty is issued
  - Ghost strike tracking on User model (ghost_strikes column)
  - Booking cancellation on stale match teardown (ground inventory release)
  - Priority re-queue for arrived players (created_at = now-2h, reason = RE_QUEUE_OPPONENT_NO_SHOW)
  - QueueEntry reason column for frontend toast notifications

affects:
  - matchmaking queue behavior
  - future analytics on player reliability
  - frontend matchmaking toast/notification handling

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "2-strike forgiveness: increment strikes on ghost, reset + penalize on 3rd offence"
    - "Priority re-queue via historical created_at timestamp (FIFO guarantees arrive-first priority)"
    - "Session-bound strike mutations passed through to UserRepository for transaction safety"

key-files:
  created: []
  modified:
    - backend/modules/user/model/user_model.py
    - backend/modules/user/repository/user_repository.py
    - backend/modules/matchmaking/model/queue_entry_model.py
    - backend/modules/matchmaking/repository/queue_entry_repository.py
    - backend/modules/match/service/match_engine_service.py

key-decisions:
  - "2-strike forgiveness: players excused for first 2 ghosts, MatchPenalty issued only on 3rd"
  - "Strikes reset to 0 when penalty is issued (clean slate after serving ban)"
  - "Priority re-queue uses created_at = now - 2 hours so FIFO sorting grants immediate front-of-line position"
  - "reason field on QueueEntry is nullable — normal joins have reason=None, re-queues carry RE_QUEUE_OPPONENT_NO_SHOW"
  - "cancel_booking() called on every stale match to release ground cart back to AVAILABLE"

patterns-established:
  - "Optional session parameter on UserRepository strike methods enables transaction-safe mutations within cleanup_stale_matches"
  - "QueueEntry.create() accepts created_at override for priority injection without separate backdating method"

requirements-completed: [EDGE-03, EDGE-04]

# Metrics
duration: 4min
completed: 2026-04-02
---

# Phase 07: Match Teardown & Re-Queue Summary

**2-strike no-show forgiveness system with ghost tracking, booking release, and priority re-queue for innocent arrived players.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-04-02T03:40:58Z
- **Completed:** 2026-04-02T03:44:10Z
- **Tasks:** 3/3
- **Files modified:** 5

## Accomplishments

### TASK-01: Add ghost_strikes to User Model
- Added `ghost_strikes = Column(Integer, nullable=False, default=0)` to the `User` class.
- Added `"ghost_strikes": self.ghost_strikes` to `User.to_dict()`.

### TASK-02: Implement Strike Management in UserRepository
- Added `increment_ghost_strikes(user_id, session=None)` — increments by 1 with optional session for transaction safety.
- Added `reset_ghost_strikes(user_id, session=None)` — sets to 0 with optional session for transaction safety.
- Both methods flush (not commit) when called with an external session, preserving atomicity.

### TASK-03: Update Match Teardown Logic (2-Strike & Re-Queue)
- Rewrote `cleanup_stale_matches()` with full teardown orchestration:
  - `cancel_booking(booking_id)` called for every stale match — releases ground cart to AVAILABLE.
  - Ghost players (has_arrived=False):
    - strikes < 2 → `increment_ghost_strikes()` (excuse, no penalty issued).
    - strikes >= 2 → `reset_ghost_strikes()` + `MatchPenalty` (reason="Ghosting", 4h block).
  - Arrived players (has_arrived=True) → re-queued via `queue_entry_repository.create()` with:
    - `created_at = now - 2 hours` (FIFO priority D-02).
    - `reason = "RE_QUEUE_OPPONENT_NO_SHOW"` (frontend toast D-03).
- Added `reason` (nullable String) to `QueueEntry` model and `to_dict()`.
- Updated `QueueEntryRepository.create()` to accept `created_at` and `reason` overrides.
- Imported `User` model and `user_repository` into `MatchEngineService`; added `user_repository` DI param.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| TASK-01 | 2abd31d | feat(07-07): add ghost_strikes column to User model |
| TASK-02 | 8a6ed38 | feat(07-07): add increment_ghost_strikes and reset_ghost_strikes to UserRepository |
| TASK-03 | 4d83e10 | feat(07-07): implement 2-strike teardown with booking cancel and priority re-queue |

## Deviations from Plan

### Auto-added Functionality

**1. [Rule 2 - Missing Critical Functionality] Add reason column to QueueEntry**
- **Found during:** TASK-03
- **Issue:** Plan D-03 requires flagging re-queued entries with `RE_QUEUE_OPPONENT_NO_SHOW` for frontend toast notifications, but `QueueEntry` model had no `reason` field.
- **Fix:** Added nullable `reason` column to `QueueEntry` model and updated `to_dict()`. Updated `QueueEntryRepository.create()` to accept `reason` and `created_at` overrides.
- **Files modified:** `queue_entry_model.py`, `queue_entry_repository.py`
- **Commit:** 4d83e10

## Known Stubs

None — all teardown logic is fully wired.

## Self-Check: PASSED

- FOUND: backend/modules/user/model/user_model.py
- FOUND: backend/modules/user/repository/user_repository.py
- FOUND: backend/modules/matchmaking/model/queue_entry_model.py
- FOUND: backend/modules/matchmaking/repository/queue_entry_repository.py
- FOUND: backend/modules/match/service/match_engine_service.py
- FOUND: commit 2abd31d (TASK-01)
- FOUND: commit 8a6ed38 (TASK-02)
- FOUND: commit 4d83e10 (TASK-03)
