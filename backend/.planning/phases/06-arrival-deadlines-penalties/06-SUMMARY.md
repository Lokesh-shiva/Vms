---
phase: "06"
plan: "06"
status: complete
completed: 2026-04-02
subsystem: matchmaking
tags: [penalties, no-show, arrival-deadline, match-engine, queue]
key-files:
  created:
    - backend/modules/match/model/match_model.py (MatchPenalty model added)
  modified:
    - backend/modules/match/service/match_engine_service.py
    - backend/modules/matchmaking/service/matchmaking_service.py
decisions:
  - Ghosting reason string hardcoded as "Ghosting" for consistency
  - Penalty check opens a separate DB session (not reusing pricing session) to isolate concerns
  - cleanup_stale_matches() runs as Phase 0 in process_matching_cycle() before group discovery
---

# Phase 06 Plan 06: Arrival Deadlines & Penalties Summary

Implemented arrival deadline enforcement and 4-hour no-show penalty system for the matchmaking engine.

## What Was Built

- **MatchPenalty model** (`match_penalties` table): tracks per-user 4-hour blocks issued when a player ghosts a matched game. Columns: id, user_id, match_id, reason, expires_at, created_at.
- **CANCELLED_NO_SHOW status**: added to `Match.VALID_STATUSES` to represent matches abandoned due to no-shows.
- **cleanup_stale_matches()**: scans for MATCHED games older than 20 minutes, issues a MatchPenalty (reason="Ghosting", expires_at=now+4h) for every non-arrived MatchPlayer, and marks the match CANCELLED_NO_SHOW. Called as Phase 0 of `process_matching_cycle()` before group discovery.
- **join_queue() penalty check**: before creating a QueueEntry, queries MatchPenalty for an active penalty (expires_at > now). If found, raises ValueError with the expiry timestamp, blocking re-queue until the penalty lapses.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | e5a2804 | feat(06-06): add MatchPenalty model and CANCELLED_NO_SHOW status |
| 2 | 5467117 | feat(06-06): add cleanup_stale_matches to MatchEngineService |
| 3 | 6e07e29 | feat(06-06): block penalized users from joining queue in join_queue() |

## Key Files

- `backend/modules/match/model/match_model.py` — MatchPenalty model, CANCELLED_NO_SHOW status
- `backend/modules/match/service/match_engine_service.py` — cleanup_stale_matches(), called from process_matching_cycle()
- `backend/modules/matchmaking/service/matchmaking_service.py` — penalty check in join_queue()

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED
