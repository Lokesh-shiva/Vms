# Milestones History

## v1.1 System Health & Player Retention (Shipped: 2026-04-02)

**Phases completed:** 3 phases, 3 plans
**Git range:** `feat(06-06): add MatchPenalty model` → `feat(08-01): add wait_estimation_msg`
**Files modified:** 20 | **LOC:** ~1,081 net

**Key accomplishments:**

1. Arrival deadline enforcement: `cleanup_stale_matches()` detects MATCHED games older than 20 mins and cancels with `CANCELLED_NO_SHOW` status
2. No-show penalty system: `MatchPenalty` model issues 4-hour queue blocks for ghosting players; `join_queue()` gates re-entry
3. 2-strike forgiveness rule: `ghost_strikes` on User model — MatchPenalty only issued on 3rd offence, strikes reset to 0 after ban
4. Booking release on teardown: `cancel_booking()` called on every stale match, returning ground to AVAILABLE inventory
5. Priority re-queue for arrived players: `created_at = now - 2h` gives FIFO front-of-line, `reason = RE_QUEUE_OPPONENT_NO_SHOW` drives frontend toast
6. Human-readable pricing + wait strings: `PricingService.calculate_price()` returns `reason`; `get_queue_status()` returns `wait_estimation_msg`

See `.planning/milestones/v1.1-ROADMAP.md` and `.planning/milestones/v1.1-REQUIREMENTS.md` for the full archive.

---

## v1.0 MVP

**Shipped:** 2026-04-01
**Phases:** 5 | **Plans:** 15

### What Was Built

- Database Models & Pricing Engine
- Queue Management System
- Core Matching Engine Loop
- Match Lifecycle Tracking (Arrived, In-Progress, Completed)
- Post-Match Split Payments (including booking fees)

### Known Gaps

- None. All 15 requirements for v1.0 have been satisfied and passed UAT.
- TestSprite automated tests require minor fixing of the login payload.

See `.planning/milestones/v1.0-ROADMAP.md` and `.planning/milestones/v1.0-REQUIREMENTS.md` for the full archive.
