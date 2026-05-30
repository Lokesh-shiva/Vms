# Roadmap: Admin App Operational Features

## Milestones

- 📋 **v1.2 Operational Features** — Phases 01-05 (in progress)

---

## Phases

### 📋 v1.2 (In Progress)

| Phase | Name | Goal | Status |
|-------|------|------|--------|
| **01** | Grounds Management | Fetch, display, and control ground status | Planned |
| **02** | Match Monitoring | Monitor active matches and manage lifecycle | Planned |
| **03** | Queue Overview | Display active queue stats per sport/region | Planned |
| **04** | Payments Management | Display and approve/reject payments | Planned |
| **05** | System Config UI | Edit system configuration parameters | Planned |

---

## Phase Breakdown

### Phase 01: Grounds Management
- **Goal:** Implement grounds CRUD operations for ops staff
- **Scope:** 
  - API integration: GET /api/v1/grounds, PATCH grounds
  - UI: List screen with name, sport, status, is_active
  - Actions: Enable/disable, override status
  - Reuse existing list/detail patterns
- **Owner:** —
- **Depends on:** —
- **Blocks:** —
- **Estimated:** 1-2 days

### Phase 02: Match Monitoring
- **Goal:** Provide match visibility and management
- **Scope:**
  - API integration: GET /api/v1/matches, POST cancel/force-complete
  - UI: Match list with details (id, sport, status, players, timestamps)
  - Actions: Cancel match, force complete match
  - Status-based action visibility
- **Owner:** —
- **Depends on:** Phase 01
- **Blocks:** —
- **Estimated:** 1-2 days

### Phase 03: Queue Overview
- **Goal:** Monitor queue health at a glance
- **Scope:**
  - API integration: GET /api/v1/queues/status
  - UI: Simple list grouped by sport/region with counts
  - No real-time polling, refresh on demand
- **Owner:** —
- **Depends on:** Phase 01
- **Blocks:** —
- **Estimated:** 1 day

### Phase 04: Payments Management
- **Goal:** Enable ops to process pending payments
- **Scope:**
  - API integration: GET /api/v1/payments, POST approve/reject
  - UI: Payment list with user, match, amount, status
  - Actions: Approve/reject (PENDING status only)
  - Currency formatting
- **Owner:** —
- **Depends on:** Phase 01
- **Blocks:** —
- **Estimated:** 1-2 days

### Phase 05: System Config UI
- **Goal:** Allow ops to adjust system parameters without backend access
- **Scope:**
  - API integration: GET/PATCH /api/v1/system-config
  - UI: Form with 3 inputs (timeouts/penalties in minutes/hours)
  - Success/error feedback
- **Owner:** —
- **Depends on:** Phase 01
- **Blocks:** —
- **Estimated:** 1 day

---

## Timeline

- **Start:** 2026-04-08
- **Target:** 2026-04-30 (end of month)
- **Estimated Total:** 5-7 days (can run phases 02-05 in parallel after phase 01)

---

## Progress

| Phase | Estimated | Status   | Completed |
|-------|-----------|----------|-----------|
| 01. Grounds Management | 1-2d | Planned | — |
| 02. Match Monitoring | 1-2d | Planned | — |
| 03. Queue Overview | 1d | Planned | — |
| 04. Payments Management | 1-2d | Planned | — |
| 05. System Config UI | 1d | Planned | — |

---

## Notes

- All phases can be executed in parallel after Phase 01 (base pattern establishment)
- Each phase should reuse existing components and patterns
- No architectural changes
- Backend APIs are stable and documented
