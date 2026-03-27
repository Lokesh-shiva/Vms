# Matchmaking Platform MVP

## What This Is
Transforming the existing sports matchmaking backend into an **instant matchmaking platform (Uber-style)**.
Currently, the system has booking, manual match joining, payments, and RBAC. We are upgrading the Match module to an automated, queue-based system with dynamic pricing and location-based matching for 2-player games.

## Core Value
Frictionless, instant 2-player matchmaking based on sport, skill, and GPS region with dynamic pricing.

## Requirements

### Validated
- ✓ Booking module (timeslot + ground allocation) — existing
- ✓ Payment module (post-match payment MVP) — existing
- ✓ RBAC (Admin/User authentication) — existing

### Active
- [ ] Automated queue-based matching (2 players)
- [ ] Location (region) and skill-based matching
- [ ] Automated ground allocation *only* after match formation
- [ ] Arrival detection & match flow (WAITING -> MATCHED -> ARRIVED -> IN_PROGRESS -> COMPLETED)
- [ ] Dynamic pricing engine based on demand and time
- [ ] Automatic payment split post-match completion

### Out of Scope
- Matches with >2 players (deferred for v1 MVP)
- Real-time websocket tracking (GPS polling via REST for now)

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Uber-style matchmaking | Removes friction of finding players manually | — Pending |
| Post-match payment | Accommodates dynamic pricing and potential no-shows | — Pending |
| Ground lock strictly post-match | Maximizes ground utilization, avoids holding slots for unfulfilled queues | — Pending |

---
*Last updated: 2026-03-27 after initialization*

## Evolution
This document evolves at phase transitions and milestone boundaries.
