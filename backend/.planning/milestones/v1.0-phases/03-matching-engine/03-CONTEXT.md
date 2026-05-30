# Phase 03: Matching Engine - Context

**Gathered:** 2026-03-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement the background logic to group waiting queue entries into pairs based on region, sport, and skill level, secure ground bookings for them via BookingService, and create Match and MatchPlayer records cleanly using DB concurrency locking.

</domain>

<decisions>
## Implementation Decisions

### Engine Invocation
- **D-01:** REST endpoint triggered continuously by an external cron/service (keeps the API stateless and deployment simple).

### Skill Strictness
- **D-02:** Exact match only (e.g., BEGINNER matches only with BEGINNER). Simple and predictable for v1.

### Booking Fallback
- **D-03:** Keep matched users in the `WAITING` queue implicitly until a ground opens up on the next matching cycle if booking fails due to no availability.

### Queue Prioritization
- **D-04:** Strict FIFO matching by `created_at` (prioritize users who waited the longest).

### Folded Todos
None
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Matching Scope & Goals
- `.planning/ROADMAP.md` — Defines phase requirements and success criteria for Phase 03
- `.planning/REQUIREMENTS.md` — Specs for automated match creation and booking sync
- `modules/matchmaking/service/matchmaking_service.py` — Current queue tracking implementation that this engine builds upon
</canonical_refs>
