# Requirements: Admin App Operational Features

## Milestone: v1.2
**Timeline:** End of month (2026-04-30)
**Users:** Mix of ops + non-technical staff
**App:** Skeleton with existing screens (reuse components)
**Backend:** Stable & ready

---

## Feature 1: Grounds Management

### Acceptance Criteria
- [ ] Fetch ground list from `/api/v1/grounds`
- [ ] Display: name, sport_id, status (AVAILABLE/BUSY), is_active
- [ ] Enable/disable ground action
- [ ] Manual override ground status action
- [ ] UI: Simple table or list view with action buttons

### API Endpoints
- GET `/api/v1/grounds` — list all grounds
- PATCH `/api/v1/grounds/{ground_id}` — enable/disable or update status

### Notes
- Reuse existing list/detail screen patterns
- No animations, minimal styling

---

## Feature 2: Match Monitoring

### Acceptance Criteria
- [ ] Fetch match list (paginated or with filters)
- [ ] Display: match_id, sport, status, player count, timestamps (created, started, completed)
- [ ] Cancel match action
- [ ] Force complete match action
- [ ] UI: Table with match details and action buttons

### API Endpoints
- GET `/api/v1/matches` — list all matches (with filtering/pagination)
- POST `/api/v1/matches/{match_id}/cancel` — cancel match
- POST `/api/v1/matches/{match_id}/force-complete` — force complete match

### Notes
- Status values: WAITING, MATCHED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
- Timestamp display: human-readable format (e.g., "2 hours ago")
- Actions only available for specific statuses (cancel = WAITING/MATCHED; force complete = IN_PROGRESS)

---

## Feature 3: Queue Overview

### Acceptance Criteria
- [ ] Fetch active queue count per sport and region
- [ ] Display: sport name, region, active queue count
- [ ] UI: Simple list or grouped view (no complex animations)

### API Endpoints
- GET `/api/v1/queues/status` — queue stats per sport/region

### Notes
- Just counts, no individual queue member details
- Refresh on demand (no real-time polling)

---

## Feature 4: Payments

### Acceptance Criteria
- [ ] Fetch payment list (paginated)
- [ ] Display: user_id, match_id, amount, status
- [ ] Approve payment action
- [ ] Reject payment action
- [ ] UI: Table with payment details and action buttons

### API Endpoints
- GET `/api/v1/payments` — list all payments (with filtering/pagination)
- POST `/api/v1/payments/{payment_id}/approve` — approve payment
- POST `/api/v1/payments/{payment_id}/reject` — reject payment

### Notes
- Status values: PENDING, APPROVED, REJECTED, COMPLETED
- Amount display: formatted currency (e.g., "$25.50")
- Actions only available for PENDING status

---

## Feature 5: System Config

### Acceptance Criteria
- [ ] Fetch system config values:
  - MATCH_ARRIVAL_TIMEOUT_MINUTES
  - GHOST_PENALTY_HOURS
  - MATCH_IN_PROGRESS_TIMEOUT_HOURS
- [ ] Update each config value via simple form inputs
- [ ] Display current value + input field + save button
- [ ] Show success/error feedback on update

### API Endpoints
- GET `/api/v1/system-config` — fetch all config values
- PATCH `/api/v1/system-config` — update config (bulk or per-key)

### Notes
- Inputs: numeric fields (minutes/hours)
- No complex validation (assume backend handles)
- Ops staff may adjust these frequently

---

## Technical Constraints

- **Architecture:** No refactoring. Use existing patterns (Activity, Fragment, ViewModel, Repository).
- **UI Framework:** Android XML layouts.
- **Components:** Reuse existing UI components where possible.
- **No Backend Changes:** APIs used as-is, no new endpoints.
- **No Animations:** Keep UI simple and functional.
- **State Management:** Use existing ViewModel/LiveData pattern.

---

## Out of Scope

- Real-time websocket updates
- Advanced filtering/search
- Player/user management
- Booking management
- Analytics or dashboards

---

## Success Criteria

- ✓ All 5 features implemented and tested
- ✓ Simple, functional UI (no redesign)
- ✓ Ops staff can monitor and control system in production
- ✓ No architectural changes to admin app
- ✓ Code reuses existing patterns and components
