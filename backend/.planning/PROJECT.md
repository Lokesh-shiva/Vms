# Matchmaking Platform

## Current Milestone: v1.1 System Health & Player Retention
**Goal:** Handle matchmaking edge cases gracefully (no-shows, cancellations) and improve queue/pricing UX to heavily optimize player retention.

**Target features:**
- No-show logic (Arrival deadline, cancellation, penalty)
- Empty ground auto-release (Booking integration)
- Pricing perception (Return human-readable 'reasons' for dynamic pricing)
- Queue drop-off prevention (Dynamic wait-time UX strings)
- Re-match Logic (Requeue non-ghosting players gracefully)

## What This Is
Transforming the existing sports matchmaking backend into an **instant matchmaking platform (Uber-style)**.
Currently, the system has booking, manual match joining, payments, and RBAC. We upgraded the Match module to an automated, queue-based system with dynamic pricing and location-based matching for 2-player games.

## Core Value
Frictionless, instant 2-player matchmaking based on sport, skill, and GPS region with dynamic pricing.

## Requirements

### Validated
- ✓ Booking module (timeslot + ground allocation) — existing
- ✓ Payment module (post-match payment MVP) — existing
- ✓ RBAC (Admin/User authentication) — existing
- ✓ Queue management (join/leave/status with duplicate guard) — v1.0
- ✓ Matchmaking REST API — v1.0
- ✓ Automated queue-based matching (2 players) — v1.0
- ✓ Location (region) and skill-based matching — v1.0
- ✓ Automated ground allocation *only* after match formation — v1.0
- ✓ Arrival detection & match flow (WAITING -> MATCHED -> ARRIVED -> IN_PROGRESS -> COMPLETED) — v1.0
- ✓ Automatic payment split post-match completion — v1.0
- ✓ Dynamic pricing engine based on demand and time — v1.0

### Active
- [ ] Multi-player (>2) matchmaking logic
- [ ] Penalty system for players who abandon matching queue post-locking

### Out of Scope
- Real-time websocket tracking (GPS polling via REST for now)

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Uber-style matchmaking | Removes friction of finding players manually | ✓ Good |
| Post-match payment | Accommodates dynamic pricing and potential no-shows | ✓ Good |
| Ground lock strictly post-match | Maximizes ground utilization, avoids holding slots for unfulfilled queues | ✓ Good |

---
*Last updated: 2026-04-01 after v1.0 milestone completion*

## Evolution
This document evolves at phase transitions and milestone boundaries.
