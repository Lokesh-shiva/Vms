# Phase 04: Match Lifecycle - Context

**Gathered:** 2026-03-31
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement the endpoints enabling a match to transition through its lifecycle states: from `MATCHED` to `ARRIVED` (verifying location), into `IN_PROGRESS` when both users have arrived, and finalizing in `COMPLETED` when users voluntarily end the game, handling no-show abandonment.

</domain>

<decisions>
## Implementation Decisions

### Arrival Verification
- **D-01:** Arrival is enforced using a **GPS bounding box validation**. The client sends its current `lat`/`lng` to the `/match/arrive` endpoint, which verifies proximity to the booked Ground's coordinates.

### No-Show Grace Period
- **D-02:** A **hard timeout** cancels the match if User A arrives and User B is a no-show after a set duration (e.g. 10 minutes past start). This should either be an async task/cron check or checked lazily.

### Completion Trigger
- **D-03:** The match is resolved to `COMPLETED` manually when either user taps **"Finish Game"** in their app, terminating the session. (While timeslots are strictly booked via `BookingService`, letting the user actively conclude the Match state accommodates early finishing and clear player agency).

### Folded Todos
None
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Match Lifecycle Requirements
- `.planning/ROADMAP.md` — Phase 4 expected endpoints and goals
- `.planning/REQUIREMENTS.md` — `LIFECYCLE-01`, `LIFECYCLE-02`, `LIFECYCLE-03`
- `modules/match/model/match_model.py` — Lifecycle statuses and relations
- `modules/location/model/location_model.py` — For the GPS coordinates to bound user arrivals.
</canonical_refs>
