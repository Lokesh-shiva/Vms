---
phase: 06-arrival-deadlines-penalties
verified: 2026-04-02T00:00:00Z
status: passed
score: 4/4 must-haves verified
gaps: []
human_verification:
  - test: "End-to-end no-show flow via running server"
    expected: "POST /engine/trigger with a 25-minute-old MATCHED match cancels it and creates MatchPenalty rows in the DB"
    why_human: "Requires a live server + seeded DB rows; cannot test purely from static code analysis"
  - test: "Penalized user blocked at /play-now"
    expected: "POST /api/v1/matchmaking/play-now returns 400 with penalty message when an active MatchPenalty row exists for that user"
    why_human: "Requires seeded penalty row and authenticated request; cannot verify response body format from static analysis"
---

# Phase 06: Arrival Deadlines & Penalties Verification Report

**Phase Goal:** Implement automated check-in enforcement for matchmaking. Cancel matches where players fail to arrive within 20 minutes and apply a temporary 4-hour block to ghosting accounts.
**Verified:** 2026-04-02T00:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| #  | Truth                                                                                   | Status     | Evidence                                                                                                      |
|----|-----------------------------------------------------------------------------------------|------------|---------------------------------------------------------------------------------------------------------------|
| 1  | Matches older than 20m in MATCHED status are cancelled (CANCELLED_NO_SHOW)              | VERIFIED   | `cleanup_stale_matches()` filters `Match.status == "MATCHED"` and `Match.created_at < now()-20m`, sets `match.status = "CANCELLED_NO_SHOW"` (match_engine_service.py lines 121-158) |
| 2  | Non-arrived players receive a 4-hour block record (MatchPenalty)                        | VERIFIED   | For each `MatchPlayer` with `has_arrived == False`, a `MatchPenalty(reason="Ghosting", expires_at=now+4h)` is created and added to session (lines 132-153) |
| 3  | Blocked players cannot join the queue via /play-now (join_queue() penalty check)        | VERIFIED   | `join_queue()` queries `MatchPenalty` for `user_id` where `expires_at > now()` before creating a QueueEntry; raises `ValueError` if found (matchmaking_service.py lines 44-59); `ValueError` is caught and returned as HTTP 400 in the `/play-now` route (matchmaking_routes.py line 46) |
| 4  | Cleanup is triggered automatically by the matching engine cron (process_matching_cycle) | VERIFIED   | `cleanup_stale_matches()` is called as Phase 0 inside `process_matching_cycle()` before group discovery, with its own session, commit, and rollback handling (match_engine_service.py lines 72-81) |

**Score:** 4/4 truths verified

---

### Required Artifacts

| Artifact                                                                          | Expected                                                  | Status   | Details                                                                                        |
|-----------------------------------------------------------------------------------|-----------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------|
| `backend/modules/match/model/match_model.py`                                      | MatchPenalty model, CANCELLED_NO_SHOW in VALID_STATUSES   | VERIFIED | `MatchPenalty` class defined at line 100 with all required columns. `CANCELLED_NO_SHOW` present in `Match.VALID_STATUSES` set at line 27. |
| `backend/modules/match/service/match_engine_service.py`                           | cleanup_stale_matches(), called from process_matching_cycle | VERIFIED | Method defined at line 109; called at line 75 inside `process_matching_cycle()`. Import of `MatchPenalty` at line 20. |
| `backend/modules/matchmaking/service/matchmaking_service.py`                      | penalty check in join_queue()                             | VERIFIED | Penalty guard at lines 44-59 using `MatchPenalty` imported at line 5. Raises `ValueError` with expiry timestamp. |
| `backend/modules/matchmaking/controller/matchmaking_routes.py`                    | /play-now routes to join_queue()                          | VERIFIED | `POST /play-now` calls `matchmaking_service.join_queue()`; `ValueError` caught and returned as `_error()` (HTTP 400). |

---

### Key Link Verification

| From                                 | To                              | Via                                      | Status   | Details                                                                                                   |
|--------------------------------------|---------------------------------|------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------|
| `process_matching_cycle()`           | `cleanup_stale_matches()`       | direct method call with session          | WIRED    | Lines 72-81: dedicated session opened, `cleanup_stale_matches(session)` called, committed, then closed.  |
| `cleanup_stale_matches()`            | `MatchPenalty` model            | `session.add(penalty)` inside loop       | WIRED    | Line 148: `session.add(penalty)` called for each non-arrived player.                                     |
| `cleanup_stale_matches()`            | `Match.status`                  | direct attribute assignment              | WIRED    | Line 155: `match.status = "CANCELLED_NO_SHOW"` sets the final status.                                   |
| `join_queue()`                       | `MatchPenalty` table            | SQLAlchemy query with `expires_at` guard | WIRED    | Lines 46-59: `db.query(MatchPenalty).filter(user_id, expires_at > now).first()` with ValueError raise.   |
| `/play-now` route                    | `join_queue()` penalty check    | `matchmaking_service.join_queue()` call  | WIRED    | Route at line 40 calls `matchmaking_service.join_queue()`; penalty ValueError surfaced as HTTP 400 at line 46-47. |

---

### Data-Flow Trace (Level 4)

| Artifact                              | Data Variable     | Source                                        | Produces Real Data | Status   |
|---------------------------------------|-------------------|-----------------------------------------------|--------------------|----------|
| `cleanup_stale_matches()`             | `stale_matches`   | `session.query(Match).filter(...)` — DB query | Yes                | FLOWING  |
| `cleanup_stale_matches()`             | `no_show_players` | `session.query(MatchPlayer).filter(...)` — DB query | Yes           | FLOWING  |
| `join_queue()` penalty guard          | `active_penalty`  | `db.query(MatchPenalty).filter(...)` — DB query | Yes             | FLOWING  |

---

### Behavioral Spot-Checks

Step 7b: SKIPPED — checks require a live server + seeded DB rows. Routed to human verification.

---

### Requirements Coverage

| Requirement                               | Source Plan   | Description                                                      | Status    | Evidence                                                                                |
|-------------------------------------------|---------------|------------------------------------------------------------------|-----------|-----------------------------------------------------------------------------------------|
| CANCELLED_NO_SHOW status in Match model   | 06-PLAN §1    | Add CANCELLED_NO_SHOW to Match.VALID_STATUSES                    | SATISFIED | match_model.py line 27                                                                  |
| MatchPenalty model                        | 06-PLAN §1    | match_penalties table with user_id, match_id, reason, expires_at | SATISFIED | match_model.py lines 100-131                                                           |
| cleanup_stale_matches()                   | 06-PLAN §2    | Scan MATCHED>20min, issue MatchPenalty, set CANCELLED_NO_SHOW    | SATISFIED | match_engine_service.py lines 109-159                                                   |
| cleanup called from process_matching_cycle | 06-PLAN §2   | Phase 0 call before group discovery                              | SATISFIED | match_engine_service.py lines 72-81                                                     |
| join_queue() penalty block               | 06-PLAN §3    | Block users with active penalty from joining queue               | SATISFIED | matchmaking_service.py lines 44-59                                                      |

---

### Anti-Patterns Found

| File                          | Line | Pattern                                   | Severity | Impact |
|-------------------------------|------|-------------------------------------------|----------|--------|
| None found                    | —    | —                                         | —        | —      |

No TODOs, FIXMEs, placeholder returns, or hardcoded empty data found in the three key files.

---

### Human Verification Required

#### 1. End-to-end no-show cancellation via engine trigger

**Test:** Insert a `Match` with `status='MATCHED'` and `created_at = now()-25min`, insert two `MatchPlayer` rows with `has_arrived=False`, then call `POST /api/v1/engine/trigger` with `X-Cron-Secret` header.
**Expected:** The match row transitions to `CANCELLED_NO_SHOW` and two `MatchPenalty` rows appear in `match_penalties` with `reason='Ghosting'` and `expires_at` approximately 4 hours from now.
**Why human:** Requires a live server with a real database connection and seeded rows. Cannot simulate DB state transitions from static analysis.

#### 2. Penalized user blocked at /play-now

**Test:** With an active `MatchPenalty` row for a user (expires_at in the future), authenticate as that user and call `POST /api/v1/matchmaking/play-now`.
**Expected:** HTTP 400 response with message containing "temporarily restricted from matchmaking until [expiry time] due to a previous no-show".
**Why human:** Requires a seeded penalty row, JWT token for the user, and a live endpoint.

---

### Gaps Summary

No gaps. All four success criteria are fully implemented and wired end-to-end:

1. `CANCELLED_NO_SHOW` is present in `Match.VALID_STATUSES` and `cleanup_stale_matches()` correctly assigns it to stale matches.
2. `MatchPenalty` records are created for every non-arrived `MatchPlayer` in stale matches, with `reason="Ghosting"` and `expires_at=now+4h`.
3. `join_queue()` performs a live DB query for active penalties before creating a `QueueEntry`, and the `ValueError` propagates correctly to an HTTP 400 response at the `/play-now` route.
4. `cleanup_stale_matches()` is invoked as Phase 0 of `process_matching_cycle()` with proper session lifecycle (open, commit-or-rollback, close).

One note on DB table registration: `main.py` line 39 imports only `Match` and `MatchPlayer` by name, but since `MatchPenalty` is defined in the same module file (`match_model.py`), importing that module causes all three classes to be defined and self-register with `Base.metadata`. The `create_all` call at startup will therefore create the `match_penalties` table. This is confirmed by standard Python module semantics — the entire file executes on first import.

---

_Verified: 2026-04-02T00:00:00Z_
_Verifier: Claude (gsd-verifier)_
