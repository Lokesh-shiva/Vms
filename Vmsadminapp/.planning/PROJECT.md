# VMS Admin App

## Current Milestone: v1.2 Operational Features
**Goal:** Add minimal operational UI features to monitor and control the matchmaking system in production.

**Target features:**
- Grounds Management (fetch, enable/disable, override status)
- Match Monitoring (display, cancel, force complete)
- Queue Overview (active count per sport/region)
- Payments (display, approve/reject)
- System Config (edit MATCH_ARRIVAL_TIMEOUT_MINUTES, GHOST_PENALTY_HOURS, MATCH_IN_PROGRESS_TIMEOUT_HOURS)

## What This Is
Android admin application for the VMS matchmaking platform. Integrates with backend APIs to monitor queue health, manage grounds, oversee matches, process payments, and adjust system configuration in production.

## Core Value
Operational visibility and control for ops teams to keep the matchmaking system healthy and responsive.

## Requirements

### Validated
- ✓ Admin RBAC authentication — existing
- ✓ Backend APIs stable (grounds, matches, payments, queues, config) — v1.1

### Active
- [ ] Grounds Management UI
- [ ] Match Monitoring UI
- [ ] Queue Overview UI
- [ ] Payments Management UI
- [ ] System Config UI

### Out of Scope
- Player/user management
- Booking management
- Real-time websocket updates (REST polling only)
- Advanced analytics

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Reuse existing components | Minimize dev time, maintain consistency | TBD |
| Simple tables + forms UI | Ops staff may be non-technical | TBD |
| No architecture refactor | Backend is stable, admin is isolated | TBD |

---
*Milestone started: 2026-04-08. Backend v1.1 stable. Admin app skeleton exists.*

## Evolution
This document evolves at phase transitions and milestone boundaries.
