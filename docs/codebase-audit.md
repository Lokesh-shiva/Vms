# Plixo Control Centre — Codebase Audit
_Last updated: 2026-05-19 | Phase 01 RBAC foundation complete_

---

## What's built and working

| Area | Status |
|---|---|
| Auth + RBAC foundation | ✅ JWT, role enforcement, dependencies, all 8 roles defined |
| Dashboard | ✅ Basic stats screen |
| Bookings | ✅ List view |
| Payments | ✅ List, approve, reject, refund (finance/super_admin) |
| Manage → Regions | ✅ List + CRUD |
| Manage → Sports/Cart Types | ✅ List + CRUD |
| Manage → Timeslots | ✅ List + CRUD |
| Manage → Fee Config | ✅ List + CRUD |
| Manage → Items | ✅ List + CRUD |
| Manage → Matches | ✅ List view |
| Manage → Grounds | ✅ List + toggle active |
| Nav filtering by role | ✅ Payments tab (finance/super_admin), Manage tab (ops_manager/super_admin) |
| Route-level guards | ✅ manage/* and payments composables enforce role, redirect to ForbiddenScreen |
| Debug role switcher | ✅ Visible to super_admin only, in-memory override for testing |

---

## What's empty or half-done per role

### SUPER_ADMIN
- No user management screen (list users, change role, deactivate/reactivate)
- No system config screen (beyond payment config)
- No audit log view

### OPS_MANAGER
- Manage screens exist but are view/CRUD only — no operational controls
- No queue management UI (can't drain queue, force-close slots)
- No cart assignment override screen
- No dispute resolution screen

### GROUND_OWNER
- No dedicated screens at all
- Supposed to see only their own grounds and bookings — no data isolation at query level yet
- No ownership concept wired into the backend for this role

### TOURNAMENT_MANAGER
- Tournaments referenced in permissions but no tournament module exists anywhere
- No bracket, schedule, or tournament screen

### SUPPORT
- Can view Dashboard and Bookings but cannot act on anything
- No dispute/ticket screen
- No user lookup / user profile drill-down
- No tools to issue manual actions (cancel booking, flag user, etc.)

### FINANCE
- Most complete role after super_admin — can view and approve/reject/refund payments
- Missing: revenue summary / reporting view
- Missing: refund history screen
- Missing: CSV/export functionality

### CSR_PARTNER
- Almost completely empty — sees Dashboard only
- No CSR-specific screens defined

---

## Backend gaps

| Gap | Notes |
|---|---|
| Captain module | Scaffolded, no model file |
| Tournament module | Does not exist — no model, service, controller, or routes |
| Dispute/ticket system | Not started |
| Reporting/analytics endpoints | Not started |
| Admin user management endpoints | No list-users, change-role, or deactivate endpoints |
| Ground owner data isolation | No `WHERE ground_owner_id = ?` scoping at repository level |

---

## Recommended priority order

1. **User management** (super_admin) — list users, promote/demote role, deactivate.
   Small scope, high value. Unblocks all role testing without DB scripts.

2. **Support tools** — dispute screen, user profile lookup, booking detail drill-down.
   Gives the Support role something to actually do.

3. **Ground owner panel** — scoped grounds + bookings view with repository-level isolation.
   Required before any Ground Owner can use the app safely.

4. **Finance reporting** — revenue summary, refund history, basic export.
   Completes the Finance role beyond just approve/reject.

5. **Tournament backend** — full module (model → repo → service → controller).
   Largest chunk, blocks Tournament Manager entirely.

---

## Known pre-existing test failures (not regressions)

```
FAILED modules/auth/tests/test_rbac_routes.py::TestAdminRouteProtection::test_admin_can_create_cart
FAILED modules/auth/tests/test_rbac_routes.py::TestBookingRBAC::test_admin_cannot_create_booking
```

Both confirmed pre-existing via `git stash` isolation. Not introduced by Phase 01 work.
