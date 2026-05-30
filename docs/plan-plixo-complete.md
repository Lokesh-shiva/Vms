# Plixo Control Centre — Complete Build Plan
**Date:** 2026-05-27  
**Scope:** Phase 01 completion + Phase 02 new features  
**Status:** PLANNING — do not implement until annotated and "implement" reply received

---

## Current State Summary

### What's done
- Full backend with auth, bookings, payments, locations, timeslots, sports, grounds, items, fee config, matches, matchmaking, queue, pricing, system config
- Admin app with: Login, Dashboard, Bookings, Payments, Users, Regions, Sports, Timeslots, FeeConfig, Items, Grounds, Matches screens
- RBAC 4-layer enforcement (backend → service → ViewModel → nav → UI)
- 46 backend tests passing

### What's broken / missing
- `QueueOverviewScreen` + `SystemConfigScreen` exist but have no nav routes — orphaned
- No logout button visible anywhere in the main app
- Captain module: directory exists, zero source files, not registered
- 5 roles (`ground_owner`, `support`, `tournament_manager`, `finance`, `csr_partner`) log in and see near-empty app
- Two parallel sport taxonomies: `cart_types` table (bookings) vs `sports` table (matchmaking) — architectural debt
- No society/residential feature
- No tournament ticket sales / commission tracking

---

## PHASE 01 — Completion (Fix what exists, no new features)

### P01-A: Quick Fixes (no DB changes)

#### A1 — Wire orphaned screens into navigation
**Files to modify:**
- `Vmsadminapp/.../ui/screens/MainScreen.kt`
  - Add routes: `"manage/system-config"`, `"manage/queue"`
  - Guard: `system-config` → `SUPER_ADMIN` only; `queue` → `SUPER_ADMIN`, `OPS_MANAGER`
- `Vmsadminapp/.../ui/screens/PlaceholderScreens.kt`
  - Add two tiles to `ManageScreen`: "System Config" (super_admin), "Queue Overview" (super_admin, ops_manager)
  - Pass `onNavigateToSystemConfig` and `onNavigateToQueue` callbacks
- `Vmsadminapp/.../navigation/AppNavigation.kt`
  - Pass the two new callbacks down to `ManageScreen`

**No backend changes needed.**

#### A2 — Logout / Profile header
**Files to modify:**
- `Vmsadminapp/.../ui/screens/MainScreen.kt`
  - Add `TopAppBar` to the main scaffold with:
    - App name "Plixo" left-aligned
    - Role chip (e.g. `SUPER ADMIN`) center or right
    - Logout `IconButton` (Icons.AutoMirrored.Outlined.ExitToApp) top-right
  - `onLogout` callback already flows from `AppNavigation` → `MainScreen` → `authViewModel.logout()`
- No new files needed

#### A3 — Add missing timeslot `is_active` flag
**Backend:**
- `backend/modules/timeslot/model/timeslot_model.py` — add `is_active: bool = True` column
- `backend/modules/timeslot/schemas/timeslot_schema.py` — add `is_active` to response schema
- `backend/modules/timeslot/service/timeslot_service.py` — honor `is_active` in create/update
- **DB migration:** `ALTER TABLE timeslots ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;`

---

### P01-B: Captain Management (full vertical slice)

#### Backend
**New files:**
- `backend/modules/captain/model/captain_model.py`
  ```
  Table: captains
  Fields:
    id          (PK)
    user_id     (FK → users, unique — one captain profile per user)
    region_id   (FK → locations, nullable — assigned region)
    status      (enum: ACTIVE, INACTIVE, SUSPENDED)
    rating      (Float, default 0.0)
    total_trips (Int, default 0)
    bio         (Text, nullable)
    created_at
    updated_at
  ```
- `backend/modules/captain/schemas/captain_schema.py`
  - `CaptainCreate(user_id, region_id?)`
  - `CaptainUpdate(region_id?, status?, bio?)`
  - `CaptainResponse(id, user_id, region_id, status, rating, total_trips, bio, created_at, updated_at)`
  - `CaptainWithUser(captain fields + name, phone from joined user)`

- `backend/modules/captain/repository/captain_repository.py`
  - `get_all() → List[Captain]`
  - `get_by_id(id) → Captain | None`
  - `get_by_user_id(user_id) → Captain | None`
  - `get_by_region(region_id) → List[Captain]`
  - `create(data) → Captain`
  - `update(id, data) → Captain`
  - `delete(id)`

- `backend/modules/captain/service/captain_service.py`
  - `list_captains(region_id?) → List[CaptainWithUser]`
  - `get_captain(id) → CaptainWithUser`
  - `create_captain(data) → CaptainWithUser` — validates user exists, not already a captain
  - `update_captain(id, data) → CaptainWithUser`
  - `delete_captain(id)`

- `backend/modules/captain/controller/captain_routes.py`
  ```
  GET    /api/v1/captains          → list (OPS_MANAGER, SUPER_ADMIN)
  POST   /api/v1/captains          → create (OPS_MANAGER, SUPER_ADMIN)
  GET    /api/v1/captains/{id}     → get one (OPS_MANAGER, SUPER_ADMIN)
  PUT    /api/v1/captains/{id}     → update (OPS_MANAGER, SUPER_ADMIN)
  DELETE /api/v1/captains/{id}     → delete (SUPER_ADMIN only)
  ```

**Files to modify:**
- `backend/main.py` — import and register `captain_router`
- **DB migration:** `CREATE TABLE captains (...)`

#### Admin App
**New files:**
- `Vmsadminapp/.../models/Models.kt` — add `Captain`, `CaptainWithUser`, `CreateCaptainRequest`, `UpdateCaptainRequest`
- `Vmsadminapp/.../network/ApiService.kt` — add 5 captain endpoints
- `Vmsadminapp/.../data/CaptainRepository.kt` — mirrors RegionRepository pattern
- `Vmsadminapp/.../viewmodel/CaptainViewModel.kt` — list, add, update, delete, per-row pending state
- `Vmsadminapp/.../ui/screens/CaptainScreen.kt`
  - Shimmer skeleton on load
  - Pull-to-refresh
  - Per-captain card: name, phone, region, status badge (ACTIVE/INACTIVE/SUSPENDED), rating
  - FAB to add new captain (pick from existing users)
  - Long-press or overflow menu: Edit region/status, Delete (super_admin)

**Files to modify:**
- `Vmsadminapp/.../ui/screens/MainScreen.kt` — add `manage/captains` route, `CAPTAIN_ROLES = setOf("super_admin", "ops_manager")`
- `Vmsadminapp/.../ui/screens/PlaceholderScreens.kt` — add Captains tile
- `Vmsadminapp/.../navigation/AppNavigation.kt` — instantiate `CaptainViewModel`, wire route
- `Vmsadminapp/.../MainActivity.kt` — instantiate `CaptainRepository`

---

### P01-C: Role-Specific Panels

Each role needs a dedicated experience when they log in. Currently they see a near-empty app.

#### Finance Panel (already has Payments tab — extend it)
**Currently:** Finance sees Payments tab (list of UNDER_REVIEW payments, approve/reject).  
**Missing:**
- Payment history (all statuses, not just UNDER_REVIEW)
- Revenue summary card (total collected today / this month)
- Refund management

**Files to modify (admin app only):**
- `Vmsadminapp/.../ui/screens/PaymentsScreen.kt`
  - Add filter tabs: "Pending Review" | "All Payments" | "Refunds"
  - Add revenue summary row at top (total amount from SUCCESS payments today)
- `Vmsadminapp/.../viewmodel/PaymentViewModel.kt`
  - Add `allPayments: StateFlow`, `refunds: StateFlow`
  - `initiateRefund(paymentId)`

#### Ground Owner Panel
**Currently:** Logs in, sees Dashboard + Bookings (all bookings — wrong, should be region-filtered).  
**Missing:** Their grounds, their region's bookings only.

**Backend changes:**
- `GET /api/v1/bookings` — already role-filters by `region_id` for ground_owner (check this is actually implemented)
- `GET /api/v1/grounds` — add `?region_id=` filter support if not already there

**Admin app changes:**
- `Vmsadminapp/.../ui/screens/MainScreen.kt`
  - Ground Owner gets: Dashboard + Bookings + a "My Grounds" tab (new bottom nav item)
  - Conditionally show "My Grounds" only for `ground_owner`
- New screen: `GroundOwnerScreen.kt`
  - Shows their assigned region's grounds with status (AVAILABLE/BUSY/OFFLINE)
  - Toggle ground active/offline
  - Show today's bookings for their region

#### Support Panel
**Currently:** Logs in, sees Dashboard + Bookings.  
**Missing:** User lookup, dispute resolution, match viewing.

**Backend (new endpoint needed):**
- `GET /api/v1/users/search?phone=` — search user by phone (SUPPORT, SUPER_ADMIN)

**Admin app:**
- `Vmsadminapp/.../ui/screens/MainScreen.kt` — Support gets Dashboard + Bookings + "Support" tab
- New screen: `SupportScreen.kt`
  - Search bar: lookup user by phone → show their profile + booking history
  - Active disputes list (bookings with CANCELLED or disputed status)
  - Quick actions: view booking detail, note refund needed

**New models (backend):**
- No new tables needed for MVP support panel

#### Tournament Manager Panel
**Currently:** Logs in, sees Dashboard + Bookings.  
**Missing:** Matches management, tournament view.

**Admin app (backend exists):**
- `Vmsadminapp/.../ui/screens/MainScreen.kt` — Tournament Manager gets Dashboard + "Matches" tab (already exists, just not visible to this role)
- Wire `MatchesScreen` as a bottom nav item for `tournament_manager`
- Add match creation from admin side (form: sport, region, skill level, max players, date/time)

**New backend endpoint:**
- `POST /api/v1/admin/matches` — create match from admin (OPS_MANAGER, SUPER_ADMIN, TOURNAMENT_MANAGER)

#### CSR Partner Panel
**Currently:** Logs in, sees Dashboard only.  
**Defined permissions:** `view_csr`, `view_tournaments`

**Admin app:**
- New screen: `CsrScreen.kt`
  - Read-only tournament list
  - Read-only active matches
  - Download/export bookings summary (Phase 02 — just placeholder for now)

---

## PHASE 02 — New Features

### P02-A: Society / Residential Module

**Client ask:** Separate app for societies.  
**Our counter-proposal:** Built-in premium feature with subscription paywall.

#### DB Schema (new tables)
```sql
CREATE TABLE societies (
  id              SERIAL PRIMARY KEY,
  name            VARCHAR(200) NOT NULL,
  address         TEXT,
  city            VARCHAR(100),
  admin_user_id   INT REFERENCES users(id),
  subscription_tier VARCHAR(50) DEFAULT 'free',  -- free | basic | premium
  subscription_expires_at TIMESTAMP,
  is_active       BOOLEAN DEFAULT TRUE,
  created_at      TIMESTAMP DEFAULT NOW(),
  updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE society_members (
  id              SERIAL PRIMARY KEY,
  society_id      INT REFERENCES societies(id) ON DELETE CASCADE,
  user_id         INT REFERENCES users(id) ON DELETE CASCADE,
  apartment_no    VARCHAR(50),
  is_approved     BOOLEAN DEFAULT FALSE,
  joined_at       TIMESTAMP DEFAULT NOW(),
  UNIQUE(society_id, user_id)
);

CREATE TABLE society_sport_access (
  id              SERIAL PRIMARY KEY,
  society_id      INT REFERENCES societies(id) ON DELETE CASCADE,
  sport_id        INT REFERENCES sports(id),
  region_id       INT REFERENCES locations(id),
  locked          BOOLEAN DEFAULT FALSE,  -- locked = members-only
  created_at      TIMESTAMP DEFAULT NOW()
);
```

#### Backend (new module: `backend/modules/society/`)
```
society_model.py       — Society, SocietyMember, SocietySportAccess models
schemas/               — Create/Update/Response schemas
repository/            — CRUD queries
service/               — Business logic (join, approve member, subscription check)
controller/society_routes.py
  GET    /api/v1/societies                    (SUPER_ADMIN, OPS_MANAGER)
  POST   /api/v1/societies                    (SUPER_ADMIN)
  GET    /api/v1/societies/{id}               (SUPER_ADMIN, OPS_MANAGER)
  PUT    /api/v1/societies/{id}               (SUPER_ADMIN)
  DELETE /api/v1/societies/{id}               (SUPER_ADMIN)
  GET    /api/v1/societies/{id}/members       (SUPER_ADMIN, OPS_MANAGER)
  POST   /api/v1/societies/{id}/members       (society admin)
  PUT    /api/v1/societies/{id}/members/{uid}/approve  (SUPER_ADMIN, OPS_MANAGER)
  DELETE /api/v1/societies/{id}/members/{uid} (SUPER_ADMIN)
  PUT    /api/v1/societies/{id}/subscription  (SUPER_ADMIN) — set tier + expiry
```

#### Admin App (new screens)
- `SocietyScreen.kt` — list societies with tier badges (FREE/BASIC/PREMIUM), subscription expiry, member count
- `SocietyDetailScreen.kt` — society info, member list, approve/reject pending, sport access toggles, subscription management
- New models: `Society`, `SocietyMember`, `CreateSocietyRequest`, etc.
- New repo + ViewModel
- Wire into ManageScreen (SUPER_ADMIN, OPS_MANAGER)

---

### P02-B: Tournament Registration + Ticket Sales

**How it works:**
- Users/organizers register tournaments in the app
- They set ticket price and capacity
- We take a platform commission % (configured in system_configs)
- Tickets are sold through the app; revenue split happens at settlement

#### DB Schema (new tables)
```sql
CREATE TABLE tournaments (
  id                  SERIAL PRIMARY KEY,
  name                VARCHAR(200) NOT NULL,
  description         TEXT,
  sport_id            INT REFERENCES sports(id),
  region_id           INT REFERENCES locations(id),
  organizer_user_id   INT REFERENCES users(id),
  start_date          DATE NOT NULL,
  end_date            DATE NOT NULL,
  registration_deadline DATE,
  max_teams           INT,
  entry_fee           NUMERIC(10,2) DEFAULT 0,
  prize_pool          NUMERIC(10,2) DEFAULT 0,
  status              VARCHAR(50) DEFAULT 'draft',
                      -- draft | open | closed | in_progress | completed | cancelled
  is_public           BOOLEAN DEFAULT TRUE,
  society_id          INT REFERENCES societies(id) NULLABLE,  -- null = open, set = society-locked
  created_at          TIMESTAMP DEFAULT NOW(),
  updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tournament_registrations (
  id              SERIAL PRIMARY KEY,
  tournament_id   INT REFERENCES tournaments(id) ON DELETE CASCADE,
  user_id         INT REFERENCES users(id),
  team_name       VARCHAR(200),
  status          VARCHAR(50) DEFAULT 'pending',  -- pending | confirmed | rejected
  payment_id      INT REFERENCES payments(id) NULLABLE,
  registered_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tickets (
  id              SERIAL PRIMARY KEY,
  tournament_id   INT REFERENCES tournaments(id) ON DELETE CASCADE,
  ticket_type     VARCHAR(100),   -- e.g. "General", "VIP"
  price           NUMERIC(10,2) NOT NULL,
  total_quantity  INT NOT NULL,
  sold_quantity   INT DEFAULT 0,
  is_active       BOOLEAN DEFAULT TRUE,
  created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE ticket_purchases (
  id              SERIAL PRIMARY KEY,
  ticket_id       INT REFERENCES tickets(id),
  user_id         INT REFERENCES users(id),
  quantity        INT DEFAULT 1,
  amount_paid     NUMERIC(10,2),
  platform_commission_pct NUMERIC(5,2),  -- snapshot at time of purchase
  platform_commission_amount NUMERIC(10,2),
  payment_id      INT REFERENCES payments(id) NULLABLE,
  status          VARCHAR(50) DEFAULT 'pending',  -- pending | confirmed | refunded
  purchased_at    TIMESTAMP DEFAULT NOW()
);
```

#### Backend (new module: `backend/modules/tournament/`)
```
model/tournament_model.py    — Tournament, TournamentRegistration, Ticket, TicketPurchase
schemas/
repository/
service/
  - validate registration eligibility (society check, deadline, capacity)
  - calculate commission on ticket purchase
  - settlement report per tournament
controller/tournament_routes.py
  GET    /api/v1/tournaments                      (public)
  POST   /api/v1/tournaments                      (TOURNAMENT_MANAGER, SUPER_ADMIN)
  GET    /api/v1/tournaments/{id}                 (public)
  PUT    /api/v1/tournaments/{id}                 (TOURNAMENT_MANAGER, SUPER_ADMIN)
  DELETE /api/v1/tournaments/{id}                 (SUPER_ADMIN)
  POST   /api/v1/tournaments/{id}/open            (TOURNAMENT_MANAGER, SUPER_ADMIN)
  POST   /api/v1/tournaments/{id}/close           (TOURNAMENT_MANAGER, SUPER_ADMIN)
  GET    /api/v1/tournaments/{id}/registrations   (TOURNAMENT_MANAGER, SUPER_ADMIN)
  POST   /api/v1/tournaments/{id}/register        (require_user — user self-registers)
  PUT    /api/v1/tournaments/{id}/registrations/{rid} (TOURNAMENT_MANAGER — approve/reject)
  GET    /api/v1/tournaments/{id}/tickets         (public)
  POST   /api/v1/tournaments/{id}/tickets         (TOURNAMENT_MANAGER, SUPER_ADMIN)
  PUT    /api/v1/tournaments/{id}/tickets/{tid}   (TOURNAMENT_MANAGER, SUPER_ADMIN)
  POST   /api/v1/tickets/{id}/purchase            (require_user)
  GET    /api/v1/tournaments/{id}/revenue         (FINANCE, SUPER_ADMIN)
```

**system_configs keys to add:**
- `TOURNAMENT_COMMISSION_PCT` — platform cut on ticket sales (default "10.00")

#### Admin App (new screens)
- `TournamentScreen.kt` — list with status badges, registrations count, revenue
- `TournamentDetailScreen.kt` — registrations list, approve/reject, ticket management, revenue summary
- `TicketManagementScreen.kt` — create/edit ticket types, view sold count
- New models, repo, ViewModel
- Wire into ManageScreen for TOURNAMENT_MANAGER, SUPER_ADMIN
- Wire into bottom nav for `tournament_manager` role

---

## Architecture Decisions

### Dual taxonomy debt (cart_types vs sports)
The `cart_types` table is used by bookings, grounds, fee_configs, items. The `sports` table is used only by matchmaking queue. These are parallel naming for the same concept.  
**Decision for now:** Leave as-is. Do NOT merge. Merging requires a data migration and touches 6+ modules. Flag as Phase 03 tech debt.  
**Action:** Add a comment in both model files pointing to each other.

### Captain status vs User is_active
A captain is a user with an extra profile. We do NOT add a `captain` role to UserRole. Captains are USER-role accounts with a `captains` table entry.  
**Reason:** Mixing identity (who you are) with function (what you do) in the role field is bad. A captain might also be a user who books — they should keep `user` role.

### Society subscription paywall enforcement
Subscription check happens at the **service layer** when a user tries to book a society-locked sport or register for a society-locked tournament. The check:
1. Is the resource locked to a society? (`society_id IS NOT NULL`)
2. Is the user a member of that society? (`society_members` lookup)
3. Is the society subscription active? (`subscription_expires_at > NOW()`)

### Commission tracking
Commission is **snapshotted** at purchase time (same pattern as `cancellation_fee_pct_snapshot` in bookings). We never rely on current config value for past transactions.

---

## DB Migrations Required (in order)

```sql
-- P01-A3: timeslot is_active
ALTER TABLE timeslots ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- P01-B: captains
CREATE TABLE IF NOT EXISTS captains (
  id          SERIAL PRIMARY KEY,
  user_id     INT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  region_id   INT REFERENCES locations(id) ON DELETE SET NULL,
  status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
  rating      FLOAT NOT NULL DEFAULT 0.0,
  total_trips INT NOT NULL DEFAULT 0,
  bio         TEXT,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- P02-A: societies
CREATE TABLE IF NOT EXISTS societies ( ... );
CREATE TABLE IF NOT EXISTS society_members ( ... );
CREATE TABLE IF NOT EXISTS society_sport_access ( ... );

-- P02-B: tournaments
CREATE TABLE IF NOT EXISTS tournaments ( ... );
CREATE TABLE IF NOT EXISTS tournament_registrations ( ... );
CREATE TABLE IF NOT EXISTS tickets ( ... );
CREATE TABLE IF NOT EXISTS ticket_purchases ( ... );

-- P02-B: system config seed
INSERT INTO system_configs (key, value) VALUES ('TOURNAMENT_COMMISSION_PCT', '10.00')
ON CONFLICT (key) DO NOTHING;
```

---

## API Changes Summary

| Change | Type | Endpoint | Auth |
|--------|------|----------|------|
| Add captain CRUD | New | `/api/v1/captains/*` | OPS_MANAGER, SUPER_ADMIN |
| User search by phone | New | `GET /api/v1/users/search?phone=` | SUPPORT, SUPER_ADMIN |
| Admin create match | New | `POST /api/v1/admin/matches` | TOURNAMENT_MANAGER, OPS_MANAGER, SUPER_ADMIN |
| Ground region filter | Modify | `GET /api/v1/grounds?region_id=` | existing auth |
| Society CRUD | New | `/api/v1/societies/*` | OPS_MANAGER, SUPER_ADMIN |
| Tournament CRUD | New | `/api/v1/tournaments/*` | TOURNAMENT_MANAGER, SUPER_ADMIN |
| Ticket CRUD + purchase | New | `/api/v1/tickets/*` | mixed |
| Tournament revenue | New | `GET /api/v1/tournaments/{id}/revenue` | FINANCE, SUPER_ADMIN |

---

## Admin App Changes Summary

| Change | Type | File(s) |
|--------|------|---------|
| Wire SystemConfig + Queue to nav | Fix | `MainScreen.kt`, `PlaceholderScreens.kt`, `AppNavigation.kt` |
| Logout / role header in top bar | Fix | `MainScreen.kt` |
| Captain screen | New | `CaptainScreen.kt`, `CaptainViewModel.kt`, `CaptainRepository.kt` |
| Finance extended payments | Extend | `PaymentsScreen.kt`, `PaymentViewModel.kt` |
| Ground Owner "My Grounds" tab | New | `GroundOwnerScreen.kt`, `MainScreen.kt` |
| Support panel + user search | New | `SupportScreen.kt`, `MainScreen.kt` |
| CSR panel | New | `CsrScreen.kt`, `MainScreen.kt` |
| Tournament Manager view | New | wire `MatchesScreen` + new `TournamentScreen.kt` |
| Society management | New | `SocietyScreen.kt`, `SocietyDetailScreen.kt` |
| Tournament + tickets admin | New | `TournamentScreen.kt`, `TournamentDetailScreen.kt` |
| Add new models | Extend | `Models.kt` |
| Add new API endpoints | Extend | `ApiService.kt` |

---

## Implementation Order (dependencies first)

```
Phase 01 Fixes (no DB):
  1. P01-A1: Wire SystemConfig + Queue to nav         [30 min]
  2. P01-A2: Logout + role header                     [30 min]

Phase 01 Captain (needs DB):
  3. Run DB migration for captains table
  4. P01-B backend: captain model → repo → service → controller → register
  5. P01-B admin: models → ApiService → repo → ViewModel → Screen → nav

Phase 01 Role Panels (no new DB, mostly admin app):
  6. P01-C Finance: extended PaymentsScreen (filter tabs + refund)
  7. P01-C Ground Owner: GroundOwnerScreen + conditional bottom tab
  8. P01-C Support: SupportScreen + user search backend endpoint
  9. P01-C Tournament Manager: wire MatchesScreen + admin match creation
  10. P01-C CSR: CsrScreen (read-only, no new endpoints needed)

Phase 02 Society (needs DB):
  11. Run DB migration for societies tables
  12. P02-A backend: society module (model → repo → service → controller)
  13. P02-A admin: Society screens + nav wiring

Phase 02 Tournament Tickets (needs DB):
  14. Run DB migration for tournaments + tickets tables
  15. Insert TOURNAMENT_COMMISSION_PCT into system_configs
  16. P02-B backend: tournament module
  17. P02-B admin: Tournament screens + nav wiring
```

---

## Test Cases to Write

### Backend
- Captain: create, update status, delete, duplicate prevention (user already captain)
- Captain: GROUND_OWNER cannot create captain (forbidden)
- Society: subscription check (expired = 403), member approval flow
- Tournament: commission snapshot on ticket purchase, society-locked tournament blocks non-members
- User search: SUPPORT can search, USER role cannot

### Admin App
- Navigation: each role sees only their permitted tabs
- Ground Owner screen: only shows their region's data
- Support: user search returns result, empty state on no match
- Tournament Manager: can create match, cannot access Users screen

---

*Review this plan and annotate it. Reply "implement" when ready.*
