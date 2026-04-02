# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

---

## Milestone: v1.1 — System Health & Player Retention

**Shipped:** 2026-04-02
**Phases:** 3 | **Plans:** 3 | **Sessions:** 1 (single-session blitz)

### What Was Built

- `MatchPenalty` model with 4-hour ghosting blocks enforced in `join_queue()`
- `cleanup_stale_matches()` as Phase 0 of the matching cycle — detects and cancels no-show matches with `CANCELLED_NO_SHOW`
- 2-strike forgiveness: `ghost_strikes` column on User model; `MatchPenalty` only on 3rd offence
- Automatic booking release on stale match teardown — `cancel_booking()` returns ground to AVAILABLE
- Priority re-queue for arrived players: FIFO front-of-line via `created_at = now - 2h`, `RE_QUEUE_OPPONENT_NO_SHOW` reason for frontend toast
- `PricingService.calculate_price()` returns human-readable `reason` string (demand/peak/standard)
- `MatchmakingService.get_queue_status()` returns `wait_estimation_msg` (matched/empty/searching variants)

### What Worked

- **Layered build order** (Phase 06 → 07 → 08): each phase built on exactly what the previous left. No circular dependencies.
- **Yolo mode with atomic commits**: fast execution with clear per-task commits made rollback trivial if needed.
- **Nullable `reason` on QueueEntry**: added opportunistically in Phase 07 without a separate plan — self-identified deviation that saved a future phase.
- **UX string injection as pure additive change** (Phase 08): no model/endpoint changes, only dict key additions — zero regression risk.

### What Was Inefficient

- **No automated tests**: acceptance criteria were validated manually via code review only. No pytest suite means confidence is limited to unit-level eyeballing.
- **`cleanup_stale_matches()` polling model**: runs inside the matching cycle — not a true background cron. Works for dev/MVP but isn't production-grade.
- **ROADMAP Phase 08 checkbox left unchecked** after execution — caused ROADMAP vs STATE.md inconsistency at milestone close.

### Patterns Established

- `session=None` optional parameter on repository mutation methods enables transaction-safe composition without forcing a new commit boundary.
- `QueueEntry.create()` accepting `created_at` override enables priority injection without a separate backdating method.
- Phase 0 concept in `process_matching_cycle()` for pre-processing tasks (cleanup) before matchmaking logic.

### Key Lessons

1. **Add `reason`/metadata columns proactively when building state machines** — the `QueueEntry.reason` column was missed in Phase 06/07 design but caught in execution. Adding signal fields early avoids a future additive migration.
2. **Polling-based background work is fine for MVP**, but document it as tech debt in the milestone summary rather than the code.
3. **UX string phases are fast and high-leverage** — Phase 08 took ~10 min, touches no data model, and directly improves user retention perception.

### Cost Observations

- Sessions: 1 (all 3 phases in one session)
- Model: claude-sonnet-4-6 throughout
- Notable: entire v1.1 milestone shipped in a single session — yolo mode + pre-planned phases = maximum velocity

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.0 | 5 | 15 | Established queue/matchmaking/payment foundation |
| v1.1 | 3 | 3 | Focused edge-case + UX hardening; smaller, faster phases |

### Top Lessons (Verified Across Milestones)

1. **Layered phase order matters**: dependencies between phases should be one-directional. Both milestones succeeded partly because each phase consumed clean outputs from the previous.
2. **Small, tightly-scoped plans execute faster** than large multi-task plans — v1.1's 3 single-plan phases completed faster than v1.0's multi-plan phases.
