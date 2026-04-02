# Milestone Execution Roadmap
*Milestone: v1.1 System Health & Player Retention*

## Roadmap Summary
**3 phases** | **6 requirements mapped** | All covered ✓

| Phase | Title | Goal | Requirements | Criteria | Status |
|-------|-------|------|--------------|----------|--------|
| [06] | Arrival Deadlines & Penalties | Build a background task to enforce check-in timers | EDGE-01, EDGE-02 | 3 | COMPLETE |
| [07] | Match Teardown & Re-Queue | Correctly handle inventory release and innocent player re-queueing | EDGE-03, EDGE-04 | 3 | COMPLETE |
| [08] | Pricing & Queue UX Perception | Inject human-readable reasons and queue estimates into REST APIs | UX-01, UX-02 | 2 | PLANNED |

---

## Phase Details

### Phase 06: Arrival Deadlines & Penalties
**Goal:** Build a background task or trigger to check `MATCHED` states against an `arrival_deadline` threshold (e.g., 15 mins) and issue penalties for ghosting.
**Requirements:** EDGE-01, EDGE-02
**Success Criteria:**
1. A cron or active trigger detects matches where `status == 'MATCHED'` and `now() > arrival_deadline`.
2. The match state is updated to `CANCELLED_NO_SHOW`.
3. A penalty flag/record is created for the `MatchPlayer` id who failed to arrive.

### Phase 07: Match Teardown & Re-Queue
**Goal:** Recover locked ground inventory upon cancellation and prioritize non-ghosting players immediately back into the queue.
**Requirements:** EDGE-03, EDGE-04
**Success Criteria:**
1. If a match cancels due to no-show, the backend immediately calls `BookingService.release_booking(booking_id)`.
2. The player who actively `has_arrived = true` is placed back in `QueueEntry` with a priority timestamp and existing parameters.
3. The re-queued player isn't charged a second booking hold.

### Phase 08: Pricing & Queue UX Perception
**Goal:** Enhance existing API payloads with localized, intelligent natural-language descriptions for wait estimates and dynamic pricing spikes.
**Requirements:** UX-01, UX-02
**Plans:** 1/1 plans complete
**Success Criteria:**
1. `matchmaking/status` endpoint includes a `"wait_estimation_msg"` field (e.g., "Match likely in 1 min", "Searching...").
2. `PricingService` response appends a `"reason"` string describing why demand pricing is active.

Plans:
- [ ] 08-01-PLAN.md — Add `reason` to PricingService.calculate_price() and `wait_estimation_msg` to MatchmakingService.get_queue_status()
