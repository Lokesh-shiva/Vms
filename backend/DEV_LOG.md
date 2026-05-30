# Development Log

---

## 2026-05-30 — Phase 02: Admin App — Time-Based Billing UI

### Summary
Wired the admin app to the time-based billing backend. Session start/end now uses the `/start-session` and `/end-session` endpoints (metered billing flow). `IN_PROGRESS` booking cards show a live elapsed-time timer (`LiveSessionTimer` composable, 1-second tick, `isActive`-guarded loop, API-24-safe `SimpleDateFormat`). `AWAITING_TIME_PAYMENT` cards display session duration, block count, and time bill amount with a note to approve in the Payments tab. Pricing (FeeConfig) edit dialog gains matching_fee, rate_per_block, block_duration_minutes, max_duration_minutes fields plus a surge enabled toggle and multiplier (edit-only). Duration fields only required when rate_per_block > 0.

### Admin App — Modified Files

| File | Change |
|------|--------|
| `models/Models.kt` | Added session fields to `Booking` (session_started_at, session_ended_at, session_minutes, session_blocks, time_bill_amount, surge_multiplier_snapshot); `payment_type` to `Payment`; time-rate + surge fields to `FeeConfig`; extended `CreateFeeConfigRequest` + `UpdateFeeConfigRequest`; added `SessionStatus` model |
| `network/ApiService.kt` | Added `startSession`, `endSession`, `getSessionStatus` endpoints |
| `data/BookingRepository.kt` | Added `startSession()`, `endSession()` |
| `data/FeeConfigRepository.kt` | Extended `createFeeConfig()` + `updateFeeConfig()` with time-rate + surge params |
| `viewmodel/BookingViewModel.kt` | Added `startSession()`, `endSession()` methods |
| `viewmodel/FeeConfigViewModel.kt` | Extended `addConfig()` + `updateConfig()` signatures for all time-rate + surge params |
| `ui/screens/BookingsScreen.kt` | Live `LiveSessionTimer` composable on IN_PROGRESS cards; AWAITING_TIME_PAYMENT info section; wired to `startSession`/`endSession` ViewModel methods |
| `ui/screens/FeeConfigScreen.kt` | `FeeConfigFormDialog` extended with 6 new fields + surge toggle; card shows time-rate info; both Add + Edit call sites updated; `toBigDecimal().stripTrailingZeros().toPlainString()` for numeric pre-fills |

### Architectural Decisions
- **API-24-safe timer**: Uses `java.text.SimpleDateFormat` (not `java.time`) since `minSdk = 24` and `coreLibraryDesugaring` is not configured.
- **`isActive`-guarded coroutine loop**: `LiveSessionTimer` uses `while (isActive)` inside `LaunchedEffect` — loop respects coroutine cancellation when composable leaves composition.
- **Surge edit-only**: Surge is an operational control (flip live), not a config-time setting; Add dialog does not show surge fields.
- **AWAITING_TIME_PAYMENT is display-only**: No action button from BookingsScreen — the TIME_BILL payment appears in Payments tab where Finance/Admin approves it.
- **Conditional duration validation**: Block/max duration fields only required when rate_per_block > 0, allowing basic configs without time-billing.
- **Old `/start` + `/complete` endpoints kept**: Backward compatibility; new metered-billing session endpoints are `/start-session` and `/end-session`.

## 2026-05-29 — Phase 02: Time-based billing backend (Tasks 1-9)

### Summary
End-to-end time-based ("metered") billing for cart sessions. A booking now starts with a **matching fee** (paid up front from the pricing config), then runs a live **session** whose final bill is computed from elapsed time in fixed blocks (`blocks = ceil(minutes / block_duration)`, `bill = blocks * rate_per_block * surge_multiplier`). The session-cost portion is collected as a **second payment** after the session ends. Surge pricing is configurable per region/cart-type and snapshotted at session end. Billing math lives in a pure, dependency-free calculator for testability.

### Backend — New Files

| File | Description |
|------|-------------|
| `modules/billing/__init__.py` | Billing module package |
| `modules/billing/calculator.py` | Pure billing calculator + `compute_session_bill` — no DB/I-O, deterministic math (blocks = ceil(minutes/block), bill = blocks × rate × surge) |

### Backend — Modified Files

| File | Change |
|------|--------|
| `modules/fee_config/model/fee_config_model.py` | Added time-rate + surge columns: `matching_fee`, `rate_per_block`, `block_duration_minutes`, `max_duration_minutes`, `surge_enabled`, `surge_multiplier` |
| `modules/fee_config/schemas/fee_config_schema.py` | Schema fields for the new time-rate + surge config columns; surge update payload |
| `modules/fee_config/service/fee_config_service.py` | Surge toggle/update logic; exposes time-rate config to billing |
| `modules/booking/model/booking_model.py` | Session lifecycle columns (session start/end, status) + `AWAITING_TIME_PAYMENT` status |
| `modules/booking/service/booking_service.py` | Session lifecycle: start-session / end-session; transitions booking into/out of `AWAITING_TIME_PAYMENT`; invokes billing calculator at session end |
| `modules/booking/controller/booking_routes.py` | New session routes: start-session, end-session, session-status |
| `modules/payment/model/payment_model.py` | `payment_type` discriminator (matching-fee vs time-bill) for the two-payment flow |
| `modules/payment/service/payment_service.py` | Two-payment flow: matching-fee payment up front + time-bill payment after session end |
| `modules/fee_config/controller/fee_config_routes.py` | Surge configuration endpoint (fee-config surge) |
| `run_migrations.py` | Migrations 3-5 — fee_config time-rate + surge columns, booking session columns + status, payment `payment_type` |
| `db_seed.py` | Pricing seed updated with `matching_fee`, `rate_per_block`, `block_duration_minutes` (45), `max_duration_minutes` (180), `surge_enabled` (FALSE), `surge_multiplier` (1.0) |

### Backend Changes
- New endpoints: `start-session`, `end-session`, `session-status` (booking); fee-config **surge** configuration.
- Payment model now carries a `payment_type` so the **matching fee** and the **time-bill** are distinct payment rows under one booking.
- Billing formula (pure calculator): `blocks = ceil(elapsed_minutes / block_duration_minutes)`, `bill = blocks * rate_per_block * surge_multiplier`, capped by `max_duration_minutes`.
- Matching fee is read from the region/cart-type pricing config and charged at booking start.
- Re-seeded successfully; full backend suite: **311 passed**. The two booking test files (`modules/booking/tests/test_booking_service.py`, `modules/booking_item/tests/test_booking_item_service.py`) already import the `Match`/`Sport` ORM models, so no FK-registration fix was required this cycle.

### Architectural Decisions
- **Pure calculator** for billing math: `modules/billing/calculator.py` has no DB or service dependencies, so it is unit-testable in isolation and reusable from any caller. `compute_session_bill` is the single source of truth for the formula.
- **Matching fee sourced from pricing config** (`region_cart_type_configs.matching_fee`) rather than hardcoded — keeps per-region/per-cart-type pricing in one place.
- **`AWAITING_TIME_PAYMENT` state**: a booking sits in this status between session end and the time-bill payment, making the two-payment flow explicit and recoverable.
- **Surge snapshot at session end**: the `surge_multiplier` in effect is captured when the session ends so the final bill is reproducible even if config changes later.
- **GROUND_OWNER as interim captain guard**: session start/end is gated on GROUND_OWNER until a dedicated captain role/guard lands.
- **10-minute grace auto-start deferred to a scheduler (Phase 03)**: sessions are started explicitly for now; the automatic grace-period auto-start needs a background scheduler.
- **Known follow-ups**: inject `FeeConfigService` into `PaymentService` (currently constructed internally — hurts testability); narrow the bare-except in the refcode retry path to the specific integrity error.

---

## 2026-05-29 — Phase 01A-2: Ground Owner panel + RBAC test fixes

### Summary
Backend region isolation for ground_owner role: bookings and grounds are now filtered at the **repository/query level** by the user's `region_id`. New `GroundOwnerScreen` with dedicated "My Grounds" bottom tab. Fixed 2 pre-existing RBAC test failures (cart create schema rejection + admin booking creation).

### Backend — Modified Files

| File | Change |
|------|--------|
| `modules/booking/repository/booking_repository.py` | Added `find_by_region_id(region_id)` — SQL query filtered by `Booking.region_id` |
| `modules/booking/service/booking_service.py` | Added `list_bookings_by_region(region_id)` — delegates to new repo method + lazy expiry + batch enrichment |
| `modules/booking/controller/booking_routes.py` | `list_bookings` now branches: `ground_owner` → `list_bookings_by_region(user.region_id)`; other admins → `list_bookings()`; users → `list_bookings_by_user()` |
| `modules/cart/controller/ground_routes.py` | `list_grounds` now accepts optional `region_id` query param; `ground_owner` is forced to their own `region_id` (param ignored) |
| `modules/auth/tests/test_rbac_routes.py` | **Fix 1:** `test_admin_can_create_cart` — removed `status` from JSON body (CreateCartSchema rejects it). **Fix 2:** renamed `test_admin_cannot_create_booking` → `test_admin_can_also_create_booking` — super_admin is in `_ALL_AUTHENTICATED_ROLES` and correctly passes `require_user` |

### Admin App — New Files

| File | Description |
|------|-------------|
| `ui/screens/GroundOwnerScreen.kt` | Dedicated ground_owner panel: region grounds (status cards) + region bookings list, both auto-filtered server-side |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/screens/MainScreen.kt` | Added `MyGrounds` bottom nav item (ground_owner only), NavHost route with RBAC guard |
| `network/ApiService.kt` | Added `getGroundsByRegion(regionId)` for region-filtered grounds fetch |
| `test/.../UserManagementViewModelTest.kt` | Added `getGroundsByRegion` no-op override |

### Backend Changes
- `GET /api/v1/bookings` — ground_owner now sees only their region's bookings (data isolation at repo level)
- `GET /api/v1/grounds?region_id=` — optional region filter; ground_owner forced to their region
- All 291 backend tests passing (previously 289/291 due to the 2 RBAC test bugs)

### Architectural Decisions
- **Region isolation at repository level** (CLAUDE.md hard rule): `find_by_region_id` queries `WHERE region_id = :id` in SQL, not post-hoc filtering. Ground routes do post-filter on the in-memory list since CartService.list_carts has no region param — acceptable for the current ground count but should be pushed to SQL in Phase 03 if scale demands it.
- **ground_owner without region_id returns empty lists**: defensive default; a ground_owner who hasn't been assigned a region sees nothing rather than everything.
- **Admin booking creation allowed**: `require_user` permits any authenticated role. The old test expected admins to be blocked, but current `_ALL_AUTHENTICATED_ROLES` includes all admin roles. The test was wrong, not the code.

---

## 2026-05-29 — Phase 01A: Role panels (Finance/Tournament/CSR) + User screen filters

### Summary
Frontend-only cycle. Added role-specific experiences for Finance (payment filter tabs + revenue summary + refund), Tournament Manager (Matches as a dedicated bottom tab), and CSR Partner (read-only matches panel + tournaments placeholder). Extended the SUPER_ADMIN Users screen with role filter chips and grouped-by-role sections. Ground Owner panel deferred (needs backend region isolation). Fixed a latent test-compile gap (captain ApiService overrides lacked imports).

### Backend — No Changes
(Refund endpoint `POST /api/v1/payments/refund/{payment_id}` already existed and is now consumed by the app.)

### Admin App — New Files

| File | Description |
|------|-------------|
| `ui/screens/CsrScreen.kt` | Read-only CSR_PARTNER panel: live matches list (reuses `MatchViewModel`) + tournaments "coming soon" placeholder |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/screens/UsersScreen.kt` | Added `RoleFilterRow` (horizontal chips: All + each role present, with counts), `RoleSectionHeader`, `ROLE_ORDER`, `roleLabel()`. Default view groups users by role; selecting a chip shows a flat filtered list. `rememberSaveable` filter state |
| `viewmodel/PaymentViewModel.kt` | Keeps full payment list in memory; added `PaymentFilter` enum (PENDING_REVIEW/ALL), `setFilter()`, `totalRevenue` (sum of SUCCESS amounts), `pendingReviewCount`, and `refundPayment()` |
| `ui/screens/PaymentsScreen.kt` | Added `RevenueSummaryCard` + `PaymentFilterTabs`; `PaymentCard` now renders status-aware actions (Approve/Reject for UNDER_REVIEW, Refund for SUCCESS, none otherwise). Removed per-item `AnimatedVisibility` (scope clash with new outer Column) |
| `data/PaymentRepository.kt` | Added `refundPayment(paymentId)` |
| `network/ApiService.kt` | Added `refundPayment` → `POST /api/v1/payments/refund/{payment_id}` |
| `ui/screens/MatchesScreen.kt` | `onBack` made nullable; back arrow hidden when null so the screen works as a bottom tab |
| `ui/screens/MainScreen.kt` | Added `Matches` (tournament_manager) and `Csr` (csr_partner) bottom-nav items + NavHost routes with role guards |
| `test/.../UserManagementViewModelTest.kt` | Added missing imports for `CreateCaptainRequest`/`UpdateCaptainRequest`; added `refundPayment` no-op override |

### App Changes
- Finance role: Payments screen now has Pending Review / All tabs, a total-collected revenue card, and per-payment Refund on SUCCESS records.
- Tournament Manager role: gets a dedicated **Matches** bottom tab (ops/super still reach Matches via Manage).
- CSR Partner role: gets a dedicated **CSR** bottom tab (read-only matches; tournaments placeholder for Phase 02).
- Users screen: filter chips + grouped sections for all 8 roles.

### Architectural Decisions
- **Matches tab limited to `tournament_manager`**: ops_manager/super_admin already reach Matches via the Manage screen, so a duplicate tab would clutter their nav.
- **Refund only surfaced on SUCCESS payments**: mirrors backend state machine; avoids invalid transitions from the UI.
- **Four-layer RBAC kept**: new `matches`/`csr` routes guarded in NavHost (`!in` role set → `onForbidden`), tab visibility filtered in the bottom-nav list, and backend role guards already enforce on every endpoint.
- **Ground Owner deferred**: proper region isolation must live at the repository/query level (CLAUDE.md hard rule), so it is its own backend+app slice — not bundled into this frontend-only cycle.

### Plan
- `docs/plan-role-panels.md` — scope + deferred Ground Owner notes.



### Summary
Wired all orphaned ViewModels (SystemConfig, QueueOverview) into navigation. Added Support and Captain stubs. Backend: captain module from scratch, timeslot `is_active`, user phone-search endpoint, DB migration script.

### Backend — New Files

| File | Description |
|------|-------------|
| `backend/modules/captain/__init__.py` | Package marker |
| `backend/modules/captain/controller/__init__.py` | Package marker |
| `backend/modules/captain/model/captain_model.py` | `CaptainStatus` constants + `Captain` ORM model linked to `users` table |
| `backend/modules/captain/schemas/captain_schema.py` | `CreateCaptainSchema` + `UpdateCaptainSchema` (dict-validation pattern) |
| `backend/modules/captain/repository/captain_repository.py` | `CaptainRepository`: get_all, get_by_id, get_by_user_id, create, update, delete; module-level singleton |
| `backend/modules/captain/service/captain_service.py` | `CaptainService` with user-join enrichment; raises `HTTPException` directly (404/400) |
| `backend/modules/captain/controller/captain_routes.py` | `/api/v1/captains` CRUD routes using `require_role()` (SUPER_ADMIN + OPS_MANAGER) |
| `backend/run_migrations.py` | Idempotent migration script: adds `timeslots.is_active`, creates `captains` table |

### Backend — Modified Files

| File | Change |
|------|--------|
| `backend/modules/timeslot/model/timeslot_model.py` | Added `is_active = Column(Boolean, nullable=False, default=True)` |
| `backend/modules/timeslot/schemas/timeslot_schema.py` | Added optional `is_active` bool validation in `UpdateTimeslotSchema.is_valid()` |
| `backend/modules/timeslot/repository/timeslot_repository.py` | `create()` passes `is_active` to ORM constructor |
| `backend/modules/user/controller/user_routes.py` | Added `GET /api/v1/users/search?phone=` (SUPER_ADMIN + SUPPORT only); placed before `/{user_id}` to avoid path shadowing |
| `backend/main.py` | Registered captain router; imported `Captain` model for `Base.metadata.create_all` |

### Admin App — New Files

| File | Description |
|------|-------------|
| `ui/screens/SupportScreen.kt` | Stub screen with back button and "coming soon" body |
| `ui/screens/CaptainScreen.kt` | Stub screen with back button and "coming soon" body |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/screens/MainScreen.kt` | Full rewrite: TopAppBar (Plixo title + role chip + logout icon), Support bottom tab, new role sets (SUPPORT_ROLES, SYSTEM_CONFIG_ROLES, QUEUE_ROLES, CAPTAIN_ROLES, TOURNAMENT_ROLES, CSR_ROLES, GROUND_OWNER_ROLES), new params `systemConfigViewModel` + `queueOverviewViewModel` + `onLogout`, new routes `manage/system-config`, `manage/queue`, `manage/captains`, DebugRoleSwitcher with all 8 roles |
| `ui/screens/PlaceholderScreens.kt` | Added `onNavigateToSystemConfig`, `onNavigateToQueue`, `onNavigateToCaptains` params; System Config tile (super_admin), Queue Overview + Captains tiles (super_admin/ops_manager) |
| `navigation/AppNavigation.kt` | Added `systemConfigViewModel: SystemConfigViewModel`, `queueOverviewViewModel: QueueOverviewViewModel` params; wired `onLogout` → `authViewModel.logout()` + navigate("login") |
| `MainActivity.kt` | Instantiated `SystemConfigRepository`, `QueueRepository`, `SystemConfigViewModel`, `QueueOverviewViewModel`; all passed to `AppNavigation` |

### Backend Changes
- `POST /api/v1/captains` — create captain profile (links user_id to ground)
- `GET /api/v1/captains` — list all captains with user enrichment
- `GET /api/v1/captains/{id}` — get single captain
- `PUT /api/v1/captains/{id}` — update captain status/ground
- `DELETE /api/v1/captains/{id}` — remove captain
- `GET /api/v1/users/search?phone=` — find user by phone (SUPER_ADMIN + SUPPORT)
- `timeslots.is_active` column added (DEFAULT TRUE, non-breaking)
- Migration: run `cd backend && python run_migrations.py`

### Architectural Decisions
- **Captain as profile table** (not a new UserRole enum): user identity vs operational function kept separate; a user can have both `super_admin` role and a captain profile if needed
- **`is_active` default TRUE**: non-breaking migration; existing timeslot records get TRUE backfilled by `DEFAULT TRUE`
- **SupportScreen/CaptainScreen as stubs**: referenced by MainScreen.kt routing, so must exist to compile; real content comes in Phase 01-B
- **All four RBAC layers applied to new routes**: backend role guard → ViewModel (will have role check in Phase 01-B) → navigation LaunchedEffect guard → UI tile visibility

---

## 2026-05-27 — Phase 01-B: UI Overhaul (Clean Professional Style)

### Summary
Admin app visual overhaul: replaced glassmorphism cards and gradient background with clean white cards, flat bottom nav, grouped ManageScreen with labelled sections, renamed confusing menu items, and unified icon/weight styling throughout.

### Backend — No Changes

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/components/AppCard.kt` | Replaced glassmorphism (gradient brush, border, 18dp corners, 20dp padding, dark-mode glass tint) with plain white card: 12dp corners, 1dp elevation, 16dp padding, no border |
| `ui/theme/Color.kt` | `BackgroundLight` tweaked from `#F5F6FA` to `#F8F9FC` (cooler, less yellow) |
| `ui/screens/MainScreen.kt` | Removed `Box` radial gradient background; Scaffold `containerColor` → `MaterialTheme.colorScheme.background`; bottom nav: removed floating pill (padding + clip RoundedCornerShape 24dp), `tonalElevation` 8dp → 0dp, `containerColor` → plain surface |
| `ui/screens/PlaceholderScreens.kt` | ManageScreen rewritten: grouped into 4 labelled sections (Operations, Catalogue, Venues & Matches, Admin); renamed tiles (Sports→Sport Types, Fee Config→Pricing, Items→Menu Items, Queue Overview→Live Queue, System Config→System Settings); ManageCard simplified (no coloured icon box, plain grey icon + title/subtitle) |
| `ui/screens/DashboardScreen.kt` | `StatCard`: removed per-card `accentColor` param, icon box uses neutral `surfaceVariant` background + `onSurfaceVariant` tint; value text `FontWeight.Bold` → `FontWeight.SemiBold`; header `FontWeight.Bold` → `FontWeight.SemiBold` |

### Architectural Decisions
- **No gradient in production UI**: gradient was purely decorative and made text contrast hard to predict across devices; flat background is more readable and accessible
- **Grouped ManageScreen**: all 10+ items in a flat list overwhelmed non-technical admins; section headers make intent clear without extra navigation
- **Neutral icon tint on StatCards**: per-card accent colours (blue/orange/green) created a "traffic light" perception mismatch — numbers don't inherently carry colour semantics here
- **`AppCard` simplified aggressively**: removing the `isDark` branch reduced ~60 lines to ~15; dark mode now relies entirely on Material3 theme surface token

---

## 2026-05-20 — Phase 01: SUPER_ADMIN User Management

### Summary
Implemented full user management feature for SUPER_ADMIN role. Backend RBAC hardened: only `super_admin` can change roles or deactivate users; self-lockout enforced. Admin app wired end-to-end with four-layer RBAC enforcement.

### Backend — Modified Files

| File | Change |
|------|--------|
| `backend/modules/user/controller/user_routes.py` | Replaced loose `_ADMIN_ROLES` guard on role-change with `SUPER_ADMIN`-only check; added self-lockout (caller cannot change own role or deactivate self); added `is_active=False` deactivation guard; added `# TODO(phase01-audit)` hooks on both privileged paths; imported `UserRole` enum for consistency |
| `backend/modules/user/service/user_service.py` | Mirrored same guards at service layer (defense-in-depth): `SUPER_ADMIN`-only for role-change and deactivation, self-lockout raised as `ValueError`; imported `UserRole` |
| `backend/modules/auth/tests/test_user_rbac_routes.py` | Added `OPS_MANAGER_USER` fixture + 7 new RBAC tests: super_admin can change role, ops_manager blocked, self role-change blocked, self-deactivation blocked, super_admin can deactivate/reactivate others, invalid role value → 400 |
| `backend/modules/user/tests/test_user_service.py` | Added 2 service-layer tests: non-super-admin role change raises ValueError, super_admin self role-change raises ValueError |

### Admin App — New Files

| File | Description |
|------|-------------|
| `data/UserManagementRepository.kt` | `getUsers()`, `updateRole(id, role)`, `setActive(id, active)` — mirrors RegionRepository pattern with `parseErrorDetail()` |
| `viewmodel/UserManagementViewModel.kt` | `UserManagementState` sealed class, `loadUsers()`, `changeRole()`, `toggleActive()`, per-row `pendingIds` StateFlow, ViewModel-layer self-mutation guard, `UserManagementViewModelFactory` |
| `ui/screens/UsersScreen.kt` | Full users screen: shimmer skeleton, pull-to-refresh, per-row overflow menu, role change AlertDialog (8 roles), active/inactive badges, self-row "(you)" with hidden menu (fourth RBAC layer) |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `models/Models.kt` | Added `AppUser`, `UpdateUserRequest(role?, is_active?)` (single merged DTO); added `user_id: Int? = null` to `LoginResponse` |
| `network/ApiService.kt` | Added `getUsers()` and single `updateUser(id, UpdateUserRequest)` endpoints |
| `data/TokenManager.kt` | Added `USER_ID_KEY`, `userIdFlow: Flow<Int?>`, `saveUserId(id)`, removed USER_ID_KEY in `clearSession()` |
| `viewmodel/AuthViewModel.kt` | Exposed `currentUserId: StateFlow<Int?>` (SharingStarted.Lazily); saves `user_id` from login response |
| `ui/screens/PlaceholderScreens.kt` | `ManageScreen` adds `role` and `onNavigateToUsers` params; Users tile rendered only when `role == "super_admin"` (UI-hide layer) |
| `ui/screens/MainScreen.kt` | Added `USERS_ROLES = setOf("super_admin")`; `manage/users` composable route with role guard → `ForbiddenScreen`; passes `userManagementViewModel` and `currentUserId` to `UsersScreen` |
| `navigation/AppNavigation.kt` | Added `userManagementViewModel` param; collects `currentUserId` from `authViewModel.currentUserId`; passes both to `MainScreen` |
| `MainActivity.kt` | Instantiates `UserManagementRepository` + `UserManagementViewModel`; passes to `AppNavigation` |

### Backend Changes
- `PUT /api/v1/users/{id}`: role-change guard tightened from any `_ADMIN_ROLES` to `SUPER_ADMIN` only
- New guard: deactivation (`is_active=False`) restricted to `SUPER_ADMIN`
- New guard: self-lockout — any caller blocked from changing own role or deactivating self
- Service layer mirrors all three guards (defense-in-depth)
- Audit TODO hooks left at both privileged paths for phase01-audit integration

### Architectural Decisions
- **Single `UpdateUserRequest(role?, is_active?)` DTO** instead of two separate DTOs — avoids Retrofit interface duplication for the same URL; keeps the model layer clean
- **`AppUser` naming** (not `User`) avoids future naming collision with `User` imports in Compose
- **Four-layer RBAC enforcement** for user management: backend endpoint → service (defense-in-depth) → ViewModel (refuses to call backend) → navigation guard → UI-hide (tile + self-row menu)
- **`user_id` persisted in DataStore** via `TokenManager` so `UserManagementViewModel` can block self-mutation without a round-trip

### Test Results
- Backend: 46/46 pass (all new RBAC tests green)
- Android: `./gradlew assembleDebug` BUILD SUCCESSFUL (0 errors, 1 pre-existing deprecation warning in PlaceholderScreens.kt unrelated to this change)

---

## 19 Mar 2026 — Day 29: Cart Screen + Checkout + Booking Creation

### Summary
- Implemented `CartScreen` with item list, quantity controls, address section, and checkout flow.
- Added `AddressDialog` with field validation (name, phone 10-digit, address required).
- Added `AddressManager` integration for local address storage via DataStore.
- Implemented booking creation flow with full validation:
  - **Mixed cart type prevention**: rejects orders spanning multiple cart types.
  - **Safe region/timeslot fallback**: uses first available, errors if none.
  - **Address validation**: blocks checkout if address is blank.
  - **Double-submit prevention**: `isSubmitting` guard + button disabled state.
  - **Cart clears only after successful API response** (not before).
- Connected cart → backend booking API (`POST /api/v1/bookings`).
- Updated `BookingStatusScreen` with booking ID, "Pending Payment" status, and navigation CTAs.
- Added cart FAB with badge on `HomeScreen` for quick cart access.
- Loaded regions and timeslots in `HomeViewModel.loadHome()` for checkout data.

### Android — New Files

| File | Description |
|------|-------------|
| `ui/screens/CartScreen.kt` | Cart item list, quantity controls, address section, checkout with all validations, Snackbar errors |
| `ui/components/AddressDialog.kt` | Address form dialog with name/phone/address validation, saves via AddressManager |

### Android — Modified Files

| File | Change |
|------|--------|
| `viewmodel/HomeViewModel.kt` | Added `getCartItems()`, `getTotalAmount()`, `getCartTypeIds()`, `clearCart()`, `getCartCount()`; added `regions`/`timeslots` to `HomeUiState`; loads regions + timeslots in `loadHome()` |
| `ui/screens/HomeScreen.kt` | Added `onNavigateToCart` callback; FAB with `BadgedBox` showing cart count |
| `ui/screens/BookingStatusScreen.kt` | Full UI: booking ID card, "Pending Payment" status, "Proceed to Payment" + "Back to Home" buttons |
| `navigation/AppNavigation.kt` | Added `cart` route; added `addressManager` parameter; wired `CartScreen` |
| `MainActivity.kt` | Passes `addressManager` to `AppNavigation` |

### Key Design Decisions
- **Single cart type enforcement**: `getCartTypeIds().size > 1` → error. Prevents mixed category orders.
- **Safe fallbacks**: `regions.firstOrNull()?.id` and `timeslots.firstOrNull { it.is_active }?.id` with null checks.
- **Cart clear timing**: `clearCart()` called inside `LaunchedEffect` only when `createState is UiState.Success`.
- **Double-submit**: `BookingViewModel.createBooking()` has `if (_createState.value is UiState.Loading) return` guard; button also disabled via `enabled = !isSubmitting`.
- **Address full format**: concatenates name, address, pincode, phone into single string for API.
- **Snackbar + Retry**: errors shown with Snackbar including "Retry" action label.
- **Empty state UX**: Shopping cart icon + "Your cart is empty 🛒" + "Add items to get started" + "Browse Items" button.

**Status**:
Cart screen + checkout flow operational.
Booking creation connected to backend API.
System stable.

---

## 18 Mar 2026 — Day 28: User App Home Screen + Item Browsing + Local Cart State

### Summary
- Replaced placeholder `HomeScreen` with a real, functional browsing experience.
- Items fetched from backend, filtered by `is_available`, sorted by name, and grouped by cart type.
- Cart type categories shown as horizontal chips (`LazyRow`).
- Items displayed in a flat `LazyColumn` (no nesting) using `forEach { } + items { }` pattern.
- Empty cart type sections are hidden (only groups with ≥ 1 available item shown).
- Local `cart: Map<Int, Int>` (itemId → quantity) for add/remove state — no backend yet.
- Coil `AsyncImage` used for images; placeholder icon shown when `image_url` is null/missing.
- Image rendered with `aspectRatio(1.6f)` for consistent sizing across all item cards.
- Add/remove controls use `+ Add` button (qty=0) or `- qty +` stepper (qty>0) with vertical alignment.

### Android — New Files

| File | Description |
|------|-------------|
| `repository/ItemRepository.kt` | Fetches items via `GET /api/v1/items`; uses `parseErrorDetail` for HTTP error parsing |
| `ui/screens/ItemCard.kt` | Reusable item card: Coil image, optional description, price, add/stepper controls |

### Android — Modified Files

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Added `coil = "2.7.0"` version and `coil-compose` library entry |
| `app/build.gradle.kts` | Added `implementation(libs.coil.compose)` |
| `network/ApiService.kt` | Added `GET /api/v1/items` endpoint (`getItems()`) |
| `viewmodel/HomeViewModel.kt` | Full rewrite: flat `HomeUiState` with `items`, `groupedItems`, `cartTypes`, `cart`, `isLoading`, `error`; concurrent fetch via `async/await`; cart add/remove logic; `HomeViewModelFactory` updated |
| `ui/screens/HomeScreen.kt` | Full rewrite: single flat `LazyColumn`, loading/error/empty states, cart type chips, grouped items |
| `MainActivity.kt` | Instantiated `ItemRepository`, updated `HomeViewModelFactory` call to pass both repos |

### Key Design Decisions
- **No nested LazyColumn**: used `LazyColumn { forEach { item { } + items { } } }` to avoid scroll conflicts.
- **Availability filter**: `items.filter { it.is_available }` before grouping prevents hidden items appearing.
- **Sorted display**: both `cartTypes` and `items` sorted by `name` for consistent, intentional ordering.
- **Empty section pruning**: `groupedItems.filter { it.value.isNotEmpty() }` ensures only non-empty sections render.
- **`aspectRatio(1.6f)`**: cleaner than fixed `height(150.dp)` — handles all image sizes naturally.

**Status**:
Home screen item browsing operational.
Local cart state (no backend) ready for future checkout flow.
System stable.

---

## 17 Mar 2026 — Day 27: Items Management + Category Grouping Module

### Summary
- Implemented full Items Management module in the VMS Admin Android app.
- Backend has no `item-categories` endpoint — `CartType` is used as the item grouping "category" (Part 10 fallback).
- Items are grouped under cart types in the UI; toggling a cart type header mass-activates/deactivates all items in that group (optimistic update with rollback).
- Full CRUD: add, edit, toggle availability, delete items.
- `updatingCartTypeIds: Set<Int>` prevents spam taps on category-level toggle switch.
- Items and cart types loaded in parallel via `async`/`await`; items enriched with `cart_type_name` and sorted alphabetically within each group.
- Empty cart type sections show a header with "0 items" label.

### Android — New Files

| File | Description |
|------|-------------|
| `data/ItemRepository.kt` | CRUD + `parseErrorDetail` pattern; wraps `GET /api/v1/items`, `POST`, `PUT`, `DELETE` |
| `viewmodel/ItemViewModel.kt` | `ItemUiState` with `items`, `cartTypes`, `updatingIds`, `updatingCartTypeIds`; `toggleItemsByCartType()` for mass toggle; `ItemViewModelFactory` |
| `ui/screens/ItemsScreen.kt` | Grouped list by cart type; `CategoryHeader` with mass-toggle `Switch`; `ItemCard` with price, availability badge, edit/delete buttons; `ItemDialog` with name/price/cart-type dropdown; shimmer skeleton; pull-to-refresh; snackbars |

### Android — Modified Files

| File | Change |
|------|--------|
| `models/Models.kt` | Added `Item`, `CreateItemRequest`, `UpdateItemRequest` data classes |
| `network/ApiService.kt` | Added `getItems()`, `getItemsByCartType(@Query)`, `createItem()`, `updateItem()`, `deleteItem()` endpoints |
| `ui/screens/PlaceholderScreens.kt` | Added `onNavigateToItems` callback + Items `ManageCard` entry |
| `ui/screens/MainScreen.kt` | Added `itemViewModel` param + `composable("manage/items")` route |
| `navigation/AppNavigation.kt` | Added `itemViewModel: ItemViewModel` param, wired to `MainScreen` |
| `MainActivity.kt` | Instantiated `ItemRepository`, `ItemViewModelFactory`, `itemViewModel`; passed to `AppNavigation` |

### Key Design Decisions
- `CartType` acts as "category" — no backend changes needed.
- Mass toggle fires sequential `toggleItem()` calls per item; any failure rolls back all items in the group.
- `updatingCartTypeIds` disables the group-level switch during pending backend calls to prevent duplicate requests.
- Items sorted by `(cart_type_name, item_name)` for consistent display order.

---

## 13 Mar 2026 — Day 24: Admin App UX Hardening + UI Polish

### Summary
- Added `isSubmitting` state to all 5 admin ViewModels to prevent duplicate form submissions.
- Replaced static Save buttons with loading-aware buttons (animated spinner + disabled state) across all add/edit dialogs.
- Added success snackbar feedback after every create/update/delete operation.
- Improved keyboard usability with IME actions (`Next`/`Done`) and `onDone` submit in all form dialogs.
- Fixed light-theme glassmorphism in `AppCard` — dark mode keeps glass effect, light mode uses clean solid surface.
- Fixed login screen text field visibility with explicit theme-aware colors.
- Added `LocalSoftwareKeyboardController` to hide keyboard on form submit.

### Android — Modified Files

| File | Change |
|------|--------|
| `RegionViewModel.kt` | Added `isSubmitting`, `successMessage` to `RegionUiState`; refactored `addRegion`, `updateRegion`, `deleteRegion` with submit guard, `delay(200)`, `finally` block, success messages |
| `CartTypeViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `TimeslotViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `CartViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `FeeConfigViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `RegionsScreen.kt` | Success snackbar, `isSubmitting` passed to `RegionNameDialog`, `AnimatedContent` Save button, IME Done action, keyboard hide |
| `CartTypesScreen.kt` | Success snackbar, `isSubmitting` passed to `CartTypeNameDialog`, `AnimatedContent` Save button, IME Done action, keyboard hide |
| `TimeslotsScreen.kt` | Success snackbar, `isSubmitting` passed to `TimeslotFormDialog`, `AnimatedContent` Save button, IME Next/Done actions, keyboard hide |
| `CartsScreen.kt` | Success snackbar, `isSubmitting` passed to `CartFormDialog`, `AnimatedContent` Save button, IME Done action, keyboard hide |
| `FeeConfigScreen.kt` | Success snackbar, `isSubmitting` passed to `FeeConfigFormDialog`, `AnimatedContent` Save button, IME Next/Done actions, keyboard hide |
| `AppCard.kt` | Conditional glass effect: dark theme keeps translucent gradient + glass border; light theme uses solid `MaterialTheme.colorScheme.surface` with transparent border |
| `LoginScreen.kt` | Explicit `OutlinedTextFieldDefaults.colors()` for strong text contrast in both themes; IME Next/Done with keyboard hide on login; horizontal padding increased to 24dp |

### Key Patterns
- **ViewModel submit guard**: `if (_uiState.value.isSubmitting) return` at top of every action prevents race conditions even if UI guard is bypassed.
- **Submit flow**: `isSubmitting = true` → repo call → `delay(200)` → reload list → set `successMessage` + dismiss dialog → `finally { isSubmitting = false }`.
- **Dialog UX**: `onDismissRequest` blocked while submitting; Cancel button disabled during submit.
- **AnimatedContent**: Smooth transition between Save text and spinner in button.
- **Keyboard**: `LocalSoftwareKeyboardController.current?.hide()` called before every submit to prevent flicker.

**Status**:
Admin UX hardened — no duplicate requests, visible saving feedback, smoother form interactions, improved theme consistency, better keyboard usability.
System stable.

---

## 13 Mar 2026 — Day 24: Admin Fee Configuration Panel

### Summary
- Implemented **Fee Configuration** management module in the VMS Admin Android app.
- Added full fee config CRUD integration with backend `/api/v1/fee-config` endpoints.
- Enabled Fee Configuration navigation from the Manage screen and wired module end-to-end through app DI/navigation.
- Form dialog includes region/cart-type dropdowns with duplicate-combo prevention and numeric fee validation.

### Android — New Files

| File | Purpose |
|------|---------|
| `FeeConfigRepository.kt` | CRUD for fee configs with backend error parsing (`detail`) |
| `FeeConfigViewModel.kt` | `FeeConfigUiState`, dialog state management, sorted config list |
| `FeeConfigScreen.kt` | Fee config UI with pull-to-refresh, shimmer loading, add/edit/delete dialogs, region/cart-type dropdowns |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `FeeConfig`, `CreateFeeConfigRequest`, `UpdateFeeConfigRequest` |
| `ApiService.kt` | Added 5 fee config endpoints (`getAll`, `getByRegionAndCartType`, `create`, `update`, `delete`) |
| `MainActivity.kt` | Instantiated `FeeConfigRepository` + `FeeConfigViewModel` and passed into navigation |
| `AppNavigation.kt` | Added `feeConfigViewModel` in navigation wiring |
| `MainScreen.kt` | Added `feeConfigViewModel` param, registered `manage/fee-config` route |
| `PlaceholderScreens.kt` | Enabled Fee Configuration card with click navigation callback |

### Key Features
- **Fee Config Screen UX**: `Scaffold`, FAB, `PullToRefreshBox`, `LazyColumn`, shimmer, empty/error states, snackbar errors.
- **Fee Config Card**: shows region name, cart type name, booking fee (₹), cancellation fee (%), platform fee (%), active status badge, edit/delete actions.
- **Add Dialog**: region/cart-type dropdowns with smart filtering (already-configured combos hidden), three numeric fee fields with validation.
- **Edit Dialog**: read-only region/cart-type display, editable fee fields.
- **Validation**: booking_fee >= 0, 0 <= percentages <= 100, cancellation + platform <= 100, client-side duplicate region+cartType prevention.
- **Sorted List**: configs sorted by region name then cart type name for readability.

**Status**:
Fee Configuration module fully operational in Android admin app.
Manage navigation updated with live Fee Config route.
System stable.

---

## 13 Mar 2026 — Day 24: Admin System Management Panel (Carts)

### Summary
- Implemented **Carts** management module in the VMS Admin Android app.
- Added full cart CRUD integration with backend `/api/v1/carts` endpoints.
- Enabled Carts navigation from the Manage screen and wired module end-to-end through app DI/navigation.
- Added optimistic toggle behavior for cart active state using `ACTIVE` / `INACTIVE`.

### Android — New Files

| File | Purpose |
|------|---------|
| `CartRepository.kt` | CRUD for carts with backend error parsing (`detail`) |
| `CartViewModel.kt` | `CartUiState`, optimistic toggle handling, dialog state management |
| `CartsScreen.kt` | Carts UI with pull-to-refresh, shimmer loading, add/edit/delete dialogs, and status controls |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `Cart`, `CreateCartRequest`, `UpdateCartRequest` |
| `ApiService.kt` | Added 5 cart endpoints (`get`, `getById`, `create`, `update`, `delete`) |
| `MainActivity.kt` | Instantiated `CartRepository` + `CartViewModel` and passed into navigation |
| `AppNavigation.kt` | Added `cartViewModel` in navigation wiring |
| `MainScreen.kt` | Registered `manage/carts` route and connected `CartsScreen` |
| `PlaceholderScreens.kt` | Enabled Carts card and added click navigation callback |

### Key Features
- **Carts Screen UX**: `Scaffold`, FAB, `PullToRefreshBox`, `LazyColumn`, shimmer, empty/error states, snackbar errors.
- **Cart Card**: shows label, region name, cart type name, `StatusBadge`, active switch, edit/delete actions.
- **Dialogs**: add/edit dialog with cart label + region/cart-type dropdown selectors; delete confirmation dialog.
- **Optimistic Toggle**: immediate status update in UI, rollback on failure, per-item disable via `updatingCartIds`.

**Status**:
Carts module fully operational in Android admin app.
Manage navigation updated with live Carts route.
System stable.

---

## 12 Mar 2026 — Day 23: Admin System Management Panel (Cart Types & Timeslots)

### Summary
- Implemented **Cart Types** and **Timeslots** management modules in the VMS Admin Android app.
- Backend fix for Timeslots deletion: added proper error handling for `ForeignKeyViolation` (prevents 500 errors when deleting timeslots referenced by bookings).
- Android app now supports full CRUD for Timeslots with optimistic UI for active/inactive toggle.
- Enabled Timeslots and Cart Types cards in the Manage screen.

### Backend Changes

#### `timeslot_service.py` & `timeslot_routes.py` — Error Handling
- Added `try-except IntegrityError` handling in `delete_timeslot` service method.
- Surfacing user-friendly message for foreign key constraints: *"Cannot delete timeslot because it is still referenced by existing bookings."*
- Updated router to catch `ValueError` and return `400 Bad Request`.

### Android — New Files

| File | Purpose |
|------|---------|
| `CartTypeRepository.kt` | CRUD for cart categories with backend error parsing |
| `CartTypeViewModel.kt` | UI state, add/edit/delete dialog management for Cart Types |
| `CartTypesScreen.kt` | UI for Cart Types with pull-to-refresh, shimmer, and status toggles |
| `TimeslotRepository.kt` | CRUD for timeslots + toggle logic |
| `TimeslotViewModel.kt` | Optimistic UI updates for toggling active status, sorting by `start_time` |
| `TimeslotsScreen.kt` | UI for Timeslots with time format validation and range checks |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `CartType`, `Timeslot` and their respective Request data classes |
| `ApiService.kt` | Added 10 new endpoints (5 for Cart Types, 5 for Timeslots) |
| `PlaceholderScreens.kt` | Enabled Cart Types and Timeslots cards; removed "Soon" badges |
| `MainScreen.kt` | Added ViewModels to params and registered new manage routes |
| `AppNavigation.kt` | Wired ViewModels through the navigation graph |
| `MainActivity.kt` | Instantiated repositories and ViewModels for the new modules |

### Key Features
- **Optimistic Toggle**: Switch updates instantly on click; ID added to `updatingTimeslotIds` to disable further interaction while request is in flight.
- **Sorting**: Timeslots automatically sorted by `start_time` in ascending order.
- **Validation**: Client-side checks for `end_time > start_time` and valid `HH:mm` format.
- **Robust Errors**: Backend validation errors (overlapping timeslots) are parsed and displayed via Snackbar.

**Status**:
Cart Types and Timeslots modules fully operational.
Backend error handling improved.
System stable.

---

## 11 Mar 2026 — Day 22: Admin System Management Panel (Regions)

### Summary
- Implemented the first configuration module (Regions) inside the Manage tab of the VMS Admin Android app.
- Backend already had CRUD endpoints at `/api/v1/locations`; Android app now fully consumes them.
- Fixed case-insensitive duplicate region name detection in the backend.
- Added region delete support with confirmation dialog.
- Added proper HTTP error parsing for user-facing error messages (Snackbar).

### Backend Changes

#### `location_repository.py` — Case-Insensitive Duplicate Check
- `find_by_name()` now uses `func.lower()` for case-insensitive comparison.
- "Delhi" and "delhi" are now correctly treated as duplicates.

### Android — New Files

| File | Purpose |
|------|---------|
| `RegionRepository.kt` | Data layer — get/create/update/toggle/delete with `HttpException` error parsing |
| `RegionViewModel.kt` | State management — `RegionUiState`, CRUD, dialog state, delete confirmation |
| `RegionsScreen.kt` | UI — LazyColumn, AppCard items, toggle switch, Edit/Delete buttons, FAB, Snackbar errors |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `Region`, `CreateRegionRequest`, `UpdateRegionRequest` data classes |
| `ApiService.kt` | Added `getRegions()`, `createRegion()`, `updateRegion()`, `deleteRegion()` endpoints |
| `PlaceholderScreens.kt` | Rewrote `ManageScreen` with card-based menu (Regions active, 4 others "Soon") |
| `MainScreen.kt` | Added `regionViewModel` param, `manage/regions` nested route |
| `AppNavigation.kt` | Passes `regionViewModel` to `MainScreen` |
| `MainActivity.kt` | Creates `RegionRepository` + `RegionViewModel`, passes to nav |
| `ApiClient.kt` | Updated `BASE_URL` to `192.168.1.3` |
| `network_security_config.xml` | Added `192.168.1.3` to cleartext traffic policy |

### Key Features
- **Manage Screen**: 5 config cards — Regions (active), Cart Types / Timeslots / Carts / Fee Config ("Soon")
- **Regions Screen**: Pull-to-refresh, shimmer loading, animated list entry, empty state
- **Region Cards**: Name + Active/Inactive toggle + Edit button + Delete button (red)
- **Add/Edit Dialog**: Name input with blank validation
- **Delete Confirmation**: "Are you sure?" dialog with red Delete button
- **Error Handling**: `HttpException` body parsed for FastAPI `detail` field → shown in Snackbar
- **Case-Insensitive**: Backend rejects "delhi" if "Delhi" already exists

### Architecture
- Repository pattern with `HttpException` error parsing via `parseErrorDetail()`
- ViewModel manages dialog state (add/edit/delete confirmation) with `StateFlow`
- Snackbar + `LaunchedEffect` for transient error display
- Backend `find_by_name` uses `func.lower()` for case-insensitive SQL comparison

**Status**:
Regions management panel fully implemented.
System stable.

---

## 07 Mar 2026 — Day 21: Admin App UI System Upgrade (Dark/Light Dashboard)

### Summary
- Upgraded the visual design of the VMS Admin Android app to a modern dashboard UI.
- Implemented a full Light/Dark Theme switch that respects the system theme.
- Enhanced core UI components with glassmorphism effects (translucency, subtle gradients, and rounded corners).

### Changes
- **Theme & Colors** (`Color.kt`, `Theme.kt`): Added distinct palettes for Light (Soft White/Gold) and Dark (Deep Space Purple/Dark Grey) modes. Disabled Material You dynamic colors to enforce the premium dashboard look.
- **Glassmorphism Components** (`AppCard.kt`, `MainScreen.kt`): Applied 18dp rounded corners, translucent surfaces, and a dynamic radial gradient background (`Scaffold`) to make the glass effect pop.
- **Status Badges** (`StatusBadge.kt`): Updated colors (Orange, Green, Blue, Red, Gray) with translucent backgrounds (`alpha = 0.2f`) for distinct, readable status pills.
- **Screen Layout Refactors**:
  - `DashboardScreen.kt`: Converted to a 2x2 grid using `LazyVerticalGrid`. Fixed deprecated icon usage.
  - `BookingsScreen.kt` & `PaymentsScreen.kt`: Increased spacing and refined typography for better readability.
  - `MainScreen.kt`: Updated bottom navigation to a floating, rounded bar with outlined Material icons.

---
## 05 Mar 2026 — Day 19: Auth Stability + Payment Approval Automation

### Summary
- Payment approval now auto-confirms bookings (one-click admin workflow).
- Added `/auth/me` endpoint for token validation.
- Improved Android AuthInterceptor logging.
- Added automatic logout on 401 responses.
- Improved token persistence handling.

### Backend Changes

#### `payment_service.py` — Auto-Confirm on Approve
- `approve_payment()` now calls `BookingService.confirm_booking()` after setting payment to SUCCESS.
- Guard: only confirms if booking status is `PENDING_PAYMENT` (prevents double-confirm).
- Uses lazy import to avoid circular dependency (BookingService ↔ PaymentService).
- If confirm fails (e.g. no cart available), payment approval still succeeds.

#### `auth_routes.py` — `GET /api/v1/auth/me`
- New endpoint protected by `Depends(get_current_user)`.
- Returns `{id, name, phone, role}` — useful for token validation and debugging.

### Android Changes

#### `AuthInterceptor.kt` — Debug Logging
- Logs token attachment: `Log.d("AUTH", "Attaching token: ...")`.
- Logs missing token: `Log.e("AUTH", "No JWT token found")`.

#### `ApiClient.kt` — Global 401 Handler
- Added response interceptor: detects `401` responses (excluding `/auth/login`).
- On 401: clears token from DataStore, emits logout event via `SharedFlow`.

#### `AppNavigation.kt` — Auto-Logout Redirect
- Collects `ApiClient.logoutEvent` in `LaunchedEffect`.
- Navigates to login with `popUpTo(0)` — clears entire back stack.

### Admin Workflow (After)
```
User pays → submits UTR → Admin presses Approve
→ payment SUCCESS → booking CONFIRMED → cart assigned
```
**One click operation.** No separate confirm step needed.

---

## 02 Mar 2026 — Day 16: Admin-Configurable UPI & Merchant Settings

### Summary
Added runtime-configurable payment settings so admins can change the UPI ID and merchant/company name without redeploying.

### Changes
- **New model**: `system_config_model.py` — key-value `system_configs` table for runtime settings.
- **New repository**: `system_config_repository.py` — `get(key)` / `set(key, value)` with upsert logic.
- **Updated `payment_service.py`**:
  - Reads `UPI_ID` and `MERCHANT_NAME` from DB at runtime, falls back to `.env` values.
  - `_get_active_upi_id()` / `_get_active_merchant_name()` helpers.
  - `get_admin_payment_config()` — returns current active config.
  - `update_admin_payment_config(upi_id, merchant_name)` — validates and persists.
  - UPI link `pn=` now uses dynamic merchant name instead of hardcoded "VMS".
- **Updated `payment_routes.py`**:
  - `GET /api/v1/payments/config` — admin-only, returns current UPI ID + merchant name.
  - `PUT /api/v1/payments/config` — admin-only, updates UPI ID and/or merchant name.
- **Registered** `SystemConfig` model in `main.py` for auto table creation.
- **Updated tests**: 3 new tests (default config, update + deep link verification, validation), fixed 4 existing UPI link tests that referenced the renamed `UPI_ID` variable.

### Safety
- DB config is optional; env var fallback ensures zero-downtime if table is empty.
- UPI ID validated to contain `@`; merchant name validated to be non-blank.
- Config changes take effect immediately on next payment initiation (no restart needed).

### Test Coverage
- 38 payment tests pass (3 new + 35 existing, 0 regressions).

---

## 02 Mar 2026 — Day 16: UPI Deep Link Redirect Integration

### Summary
- Enhanced manual UPI payment flow with UPI deep link generation.
- Mobile apps can now open the deep link directly via intent/redirect — no QR code needed.
- `UPI_ID` loaded from environment variable (`UPI_ID`), not hardcoded.
- Amount formatted to 2 decimal places in the UPI link.
- Reference code used as transaction note (`tn` parameter).
- Existing workflow (initiate → confirm-manual → admin approve) unchanged.

### Changes

#### `payment_service.py`
- `MANUAL_UPI_ID` replaced with `UPI_ID = os.getenv("UPI_ID", "vms@upi")`.
- `initiate_payment()` now constructs a `upi://pay?` deep link with `pa`, `pn`, `am`, `cu`, `tn` params.
- Response includes new `upi_link` field alongside existing `booking_id`, `amount`, `reference_code`, `upi_id`.

#### `.env`
- Added `UPI_ID=vms@okicici`.

#### `payment_routes.py`
- No changes needed — `upi_link` flows through the existing `_success(result)` wrapper.

### Safety Guarantees
- Amount formatted to 2 decimal places (no floating-point noise in link).
- No spaces in UPI link.
- Booking must be `PENDING_PAYMENT` before initiation.
- Retry logic unchanged — deep link generated on retry too.
- No QR code generation. No external QR libraries.
- No payment gateway integration. Admin approval workflow intact.

### Test Coverage
- 6 new UPI deep link assertions added to `test_payment_service.py`:
  - `upi_link` starts with `upi://pay?`
  - Contains correctly formatted amount
  - Contains reference code in `tn` param
  - Contains `UPI_ID` in `pa` param
  - No spaces in link
  - Deep link present on retry after rejection
- **240/240 tests passing** — zero regressions.

**Status**:
UPI deep link integration complete.
Mobile-first redirect design operational.
System stable.

---

## 01 Mar 2026 — Day 15.2: Region + CartType Fee Config & Refund Deduction Engine

### Summary
- New `fee_config` module: admin-configurable booking fees and refund deduction percentages per region + cart type.
- Booking fee enforced server-side — client-provided `booking_fee` ignored.
- Refund uses **snapshot** percentages captured at booking creation time (not live config).
- Soft-delete only — `DELETE` sets `is_active = False`, preserving historical data.
- All percentage validation at service layer. No business rules in routes.

### Fee Config Module Structure
```
modules/fee_config/
├── model/fee_config_model.py           — ORM model (region_cart_type_configs table)
├── repository/fee_config_repository.py — CRUD + find_by_region_and_cart_type, soft-delete
├── service/fee_config_service.py       — Business validation (FK, pct sum ≤ 100, negative guards)
├── schemas/fee_config_schema.py        — Create + Update structural validation
├── controller/fee_config_routes.py     — 5 admin-only endpoints
└── tests/test_fee_config_service.py    — 18 unit tests
```

### Database Changes
- New table: `region_cart_type_configs` with `UniqueConstraint(region_id, cart_type_id)`.
- New columns on `bookings`: `cancellation_fee_pct_snapshot`, `platform_fee_pct_snapshot` (Numeric 5,2).

### Key Architecture Decisions
- **Snapshot design**: At booking creation, `cancellation_fee_pct` and `platform_fee_pct` are copied from config into the booking record. Refund logic uses these snapshot values, never the live config. This prevents admin config changes from retroactively affecting old bookings.
- **Refund formula**: `refund_amount = total_paid × (1 - (cancellation_pct + platform_pct) / 100)`, rounded to 2 decimal places.
- **No fee_config_repository in PaymentService**: Refund reads snapshot fields directly from the booking, eliminating dependency on live config.
- **Soft-delete**: `DELETE /fee-config/{id}` sets `is_active = False`. No hard deletion.

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/fee-config/create` | POST | `require_admin` |
| `/api/v1/fee-config/{id}` | PUT | `require_admin` |
| `/api/v1/fee-config/region/{id}/cart-type/{id}` | GET | `require_admin` |
| `/api/v1/fee-config/all` | GET | `require_admin` |
| `/api/v1/fee-config/{id}` | DELETE | `require_admin` |

### Booking Flow Changes
1. User creates booking → fee config fetched by (region_id, cart_type_id)
2. `booking_fee`, `cancellation_fee_pct_snapshot`, `platform_fee_pct_snapshot` set from config
3. Client-provided `booking_fee` ignored
4. If config missing/inactive → booking creation blocked (400)

### Payment Retry & Lazy Expiry (Refinements)
- **Payment Retry:** Admin rejecting a manual payment moves it to `FAILED`, but the `booking` remains `PENDING_PAYMENT`. Users can safely `initiate_payment` again, generating a new, unique reference code and pending payment record.
- **Lazy Expiry:** PENDING_PAYMENT bindings automatically expire if older than 10 minutes when accessed (e.g., via `get_booking`, `list_bookings`, or limit checks). No background workers required.
- **Daily Booking Limit:** Strict daily checks now *only* count `CONFIRMED` and `IN_PROGRESS` bookings, fully ignoring `EXPIRED` and `PENDING_PAYMENT` bookings to prevent blocking legitimate attempts.

### Refund Flow Changes
1. `cancel_booking()` passes full booking dict to `process_refund()`
2. `process_refund()` reads snapshot pcts from booking
3. Deduction = `cancellation_fee_pct_snapshot + platform_fee_pct_snapshot`
4. `refund_amount = total_paid × (1 - deduction / 100)`

### Test Coverage
- Fee config: 18 tests (CRUD, uniqueness, pct validation, soft-delete)
- Booking: 25 tests (fee from config, snapshot capture, config deactivation edge case)
- Payment: 22 tests (refund formula, snapshot-not-live, fallback fetch)
- BookingItem: 11 tests (updated for fee config DI)
- **231/231 tests passing** — zero regressions.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC enforced on all routes
- **Fee Config** → Admin-configurable, snapshot-based, soft-delete only
- **Booking** → Server-side fee enforcement, snapshot pcts at creation time
- **Payment** → Percentage-based refund using snapshot values

**Status**:
Fee config engine implemented.
Snapshot-based refund logic operational.
System stable.

---

## 01 Mar 2026 — Day 15 + 15.1: Booking & Payment State Machine (Manual UPI MVP)

### Summary
- Implemented proper Booking and Payment state machines with controlled transitions.
- Created new Payment module (model, repository, service, controller, tests).
- Booking creation now starts as `PENDING_PAYMENT` — no auto-confirmation, no cart assignment.
- Cart assignment deferred to admin-controlled `confirm_booking()` after payment approval.
- Manual UPI provider with reference code generation (`VMS-{booking_id}-{4 digits}`).
- Separate admin workflows: payment approval and booking confirmation.

### Day 15.1 Refinements Applied
- Removed `INITIATED` payment state — 5 states only: PENDING, UNDER_REVIEW, SUCCESS, FAILED, REFUNDED.
- Payment table is single source of truth; `bookings.payment_status` is a mirror field updated exclusively by PaymentService.
- BookingService never directly mutates `payment_status`.
- Slot capacity counts only CONFIRMED + IN_PROGRESS (not PENDING_PAYMENT).
- Daily limit ignores CANCELLED and EXPIRED bookings.
- Cancellation hardened: blocked for IN_PROGRESS, COMPLETED, EXPIRED. Refund routed through PaymentService.
- `confirm_booking()` runs expiry check first, then validates status, payment, and cart availability.
- Reference code: unique DB constraint + 5-attempt retry + system error on persistent collision.
- Admin payment inspection endpoint added (`GET /api/v1/payments/booking/{booking_id}`).

### Booking Status Refactor

| Old Statuses | New Statuses |
|---|---|
| PENDING_PAYMENT, CONFIRMED, CANCELLED, COMPLETED, PAYMENT_FAILED | PENDING_PAYMENT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, EXPIRED |

### Payment Module Structure
```
modules/payment/
├── model/payment_model.py          — ORM model (payments table)
├── repository/payment_repository.py — Session-based CRUD
├── service/payment_service.py       — Admin approval workflow
├── controller/payment_routes.py     — 6 endpoints
└── tests/test_payment_service.py    — 24 unit tests
```

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/payments/initiate/{booking_id}` | POST | `require_user` |
| `/api/v1/payments/confirm-manual/{booking_id}` | POST | `require_user` |
| `/api/v1/payments/approve/{payment_id}` | POST | `require_admin` |
| `/api/v1/payments/reject/{payment_id}` | POST | `require_admin` |
| `/api/v1/payments/refund/{payment_id}` | POST | `require_admin` |
| `/api/v1/payments/booking/{booking_id}` | GET | `require_admin` |
| `/api/v1/bookings/{id}/confirm` | POST | `require_admin` |

### Booking Flow (New)
1. User creates booking → status = PENDING_PAYMENT (no cart assigned)
2. User initiates payment → payment = PENDING
3. User submits UPI transaction ID → payment = UNDER_REVIEW
4. Admin approves payment → payment = SUCCESS, booking.payment_status mirrored
5. Admin confirms booking → cart assigned, status = CONFIRMED
6. Admin completes booking → cart released, status = COMPLETED

### Key Architecture Decisions
- PaymentService is a manual admin approval workflow — no bank connection, no UPI verification.
- Cart not locked until confirmation — prevents holding carts for unpaid bookings.
- Expiry check runs inside `confirm_booking()` to prevent race conditions.
- No background workers — expiry is on-demand.

### Test Coverage
- Booking tests: 27 tests (rewritten for new state machine)
- Payment tests: 24 tests (new module)
- BookingItem tests: 11 tests (updated status assertions)
- RBAC tests: updated for service-level cancellation checks
- **202/202 tests passing** — zero regressions.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC enforced on all routes
- **Booking** → State machine with PENDING_PAYMENT → CONFIRMED → COMPLETED
- **Payment** → Manual UPI approval workflow, source of truth for payment state
- **No auto-confirmation. No simulated payment. No Razorpay.**

**Status**:
Booking & Payment state machines implemented.
Manual UPI MVP operational.
System stable.

---

## 28 Feb 2026 — Day 14.1: User Management Secured

### Summary
- Locked down all user management routes under role-based access control.
- PUT route uses `get_current_user` with ownership logic: users can update their own non-role fields, admins can update anyone.
- Role mutation blocked for non-admins at both route and service layers (defense-in-depth).
- Self-deletion blocked at both route and service layers to prevent admin lockout.
- GET routes enforce ownership: admins see all users, regular users see only their own profile.
- POST (create) and DELETE restricted to admin role.

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/users` | POST (create) | `require_admin` |
| `/api/v1/users` | GET (list) | `get_current_user` + role filter |
| `/api/v1/users/{id}` | GET (detail) | `get_current_user` + ownership check |
| `/api/v1/users/{id}` | PUT (update) | `get_current_user` + ownership + role-mutation guard |
| `/api/v1/users/{id}` | DELETE | `require_admin` + self-deletion guard |

### Service-Layer Guards (Defense-in-Depth)
- `UserService.update_user()` independently rejects role mutation from non-admins, even if route-level auth is misconfigured.
- `UserService.delete_user()` independently blocks self-deletion, preventing accidental admin lockout.

### Admin Bootstrap Policy
- No dev backdoor routes or environment variable overrides.
- Role switching must be done manually via Neon DB: `UPDATE users SET role='admin' WHERE id=<id>;`
- System relies strictly on DB truth for role assignments.

### Test Coverage
- New test file: `modules/auth/tests/test_user_rbac_routes.py` — 11 route-level tests using FastAPI `TestClient` with dependency overrides.
- Tests cover: user creation (admin only), profile update ownership, role mutation guard, self-deletion prevention, admin delete/update, GET ownership enforcement.
- **172/172 tests passing** (161 existing + 11 user RBAC tests) — zero regressions.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC dependencies enforced on all routes
- **User management** → Fully secured; privilege escalation surface closed
- **No unprotected write endpoints remain.**

**Status**:
User management secured.
Privilege escalation vulnerability patched.
System stable.

---

## 28 Feb 2026 — Day 14: RBAC Activation (Operational Lifecycle Controlled)

### Summary
- Activated RBAC across all system routes and booking lifecycle endpoints.
- Admin-only protection applied to create/update/delete on locations, cart types, items, and carts.
- Booking creation restricted to authenticated users (`require_user`); server overrides `user_id` from JWT token.
- Booking cancellation enforces ownership: users can cancel only their own bookings, admins can cancel any.
- Booking completion restricted to admin role only.
- List bookings filters by role: admins see all, users see only their own.
- All state transitions remain strict (only CONFIRMED bookings can be cancelled or completed).

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/locations` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/cart-types` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/items` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/carts` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/bookings` | POST (create) | `require_user` |
| `/api/v1/bookings` | GET (list) | `get_current_user` + role filter |
| `/api/v1/bookings/{id}` | GET (detail) | `get_current_user` + ownership check |
| `/api/v1/bookings/{id}/cancel` | POST | `get_current_user` + ownership check |
| `/api/v1/bookings/{id}/complete` | POST | `require_admin` |

Read-only endpoints (GET list/detail) on locations, cart types, items, carts remain publicly accessible.
Booking detail (`GET /bookings/{id}`) requires authentication with ownership enforcement (users see own only, admins see any).

### Data Layer Changes
- `BookingRepository.find_by_user_id()` added for user-scoped booking queries.
- `BookingService.list_bookings_by_user()` added to support role-based list filtering.

### Test Coverage
- New test file: `modules/auth/tests/test_rbac_routes.py` — 28 route-level tests using FastAPI `TestClient` with dependency overrides.
- Tests cover: admin route protection (16 tests), booking creation user_id override, booking cancellation ownership, booking completion admin-only, role-based list filtering.
- **161/161 tests passing** (130 existing + 31 RBAC tests) — zero regressions.

### Test Architecture Note
- First route-level (HTTP) tests in the codebase — all prior tests were service-layer unit tests.
- Uses `app.dependency_overrides[get_current_user]` to simulate authenticated users without JWT.
- Service methods mocked via `unittest.mock.patch` to isolate route-layer RBAC logic from DB.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC dependencies now enforced on all relevant routes
- **Booking lifecycle** → Operationally controlled (completion = admin, cancellation = owner/admin)
- **Backend is now role-secured.** No unprotected write endpoints remain.

**Status**:
RBAC activation complete.
All routes protected according to role requirements.
System stable.

---

## 27 Feb 2026 — Day 13: Auth Module Implementation (JWT + Bcrypt)

### Summary
- Implemented full authentication module with JWT access tokens and bcrypt password hashing.
- Added `password_hash` column to existing `users` table via Neon MCP (ALTER TABLE, no table recreation).
- DB default dropped after backfill — future inserts must supply a real bcrypt hash.
- Auth routes at `/api/v1/auth` (register + login) are unprotected and publicly accessible.
- RBAC dependencies created and ready for future route protection.

### Database Changes
- `password_hash VARCHAR NOT NULL` added to `users` table via Neon MCP `run_sql`.
- Existing rows backfilled with empty string; default then dropped (`ALTER COLUMN password_hash DROP DEFAULT`).
- User model updated; `password_hash` explicitly excluded from `to_dict()`.

### Auth Module Structure
```
modules/auth/
├── __init__.py
├── schemas/auth_schema.py         — RegisterSchema, LoginSchema
├── service/auth_service.py        — register_user(), login_user()
├── dependencies/auth_dependencies.py — get_current_user(), require_admin(), require_user()
├── controller/auth_routes.py      — POST /register, POST /login
└── tests/test_auth_service.py     — 5 unit tests
```

### Security Implementation
- **Password hashing**: passlib CryptContext with bcrypt scheme.
- **JWT (HS256)**: python-jose, 60-minute expiry, timezone-aware (`datetime.now(timezone.utc)`).
- **Token payload**: `{ "sub": user_id, "role": user_role, "exp": expiry }`.
- **SECRET_KEY**: stored in `.env`, loaded at startup.
- **Phone normalization**: spaces stripped before uniqueness checks and lookups.
- **`find_by_phone_with_hash()`**: explicit field serialization (does not reuse `to_dict()`) to prevent accidental hash leakage.
- **`get_current_user()`**: validates user existence and `is_active` status from DB on every request — does not trust token payload alone.

### Dependencies Added
- `passlib[bcrypt]`, `python-jose[cryptography]` added to `requirements.txt`.
- `bcrypt` pinned to 4.0.1 for passlib compatibility.

### Test Isolation
- Auth tests use SQLite in-memory with injected `session_factory`.
- Zero Neon connections during automated testing.

### Verification
- **130/130 tests passing** (125 existing + 5 new auth tests) — zero regressions.
- Manual API test confirmed:
  - `POST /api/v1/auth/register` → 201, user created with hashed password, no `password_hash` in response.
  - `POST /api/v1/auth/login` → 200, JWT access token returned.
  - Invalid password → 401 rejected.
  - Test user cleaned up via `DELETE /api/v1/users`.

### Current Architecture State
- **User, Location, CartType, Timeslot, Cart, Booking, BookingItem, Item** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, routes unprotected, RBAC dependencies ready
- **All modules now fully DB-backed.** No in-memory repositories remain.

**Status**:
Auth module successfully implemented.
Backend secured and ready for route protection.
System stable.

---

## 27 Feb 2026 — Booking & BookingItem Post-Migration Validation

### Summary
- Completed full API + DB validation for Booking and BookingItem in the existing Neon project (`VMS-Backend`, `neondb`).
- Confirmed service-layer business rules and DB-backed repository behavior are consistent after ORM migration.
- Verified booking lifecycle side effects on cart status and refund fields.

### Validation Coverage
- Full booking creation with items:
  - Booking created as `CONFIRMED` with `payment_status = SUCCESS`.
  - `estimated_total` computed correctly from item snapshots.
  - `assigned_cart_id` populated.
  - `booking_items` rows inserted.
  - Assigned cart moved to `BUSY`.
- Cancel booking flow:
  - Booking moved to `CANCELLED`.
  - `refund_status = REFUNDED`.
  - `refund_amount = booking_fee`.
  - Cart released back to `AVAILABLE`.
- Complete booking flow:
  - Booking moved to `COMPLETED`.
  - Cart released back to `AVAILABLE`.
- Slot capacity enforcement:
  - Capacity=1 timeslot accepted first booking, rejected second with HTTP 400.
- Daily booking limit:
  - Same user + same date allowed 3 active bookings; 4th booking rejected with HTTP 400.
- Atomic rollback safety:
  - With item temporarily set unavailable, booking request failed.
  - No booking row created, no booking_item row created, cart status unchanged.

### DB Verification
- Verified via Neon SQL:
  - `SELECT * FROM bookings;`
  - `SELECT * FROM booking_items;`
  - `SELECT * FROM carts;`
- Confirmed expected persisted state after each scenario.

### Notes
- Decimal-safe monetary handling remains consistent:
  - DB stores monetary values as `Numeric(10,2)`.
  - API responses serialize `booking_fee`, `estimated_total`, and `refund_amount` as `float`.
- No ORM `relationship()` usage introduced; ForeignKey-only architecture preserved.

## 26 Feb 2026 — Item Migration to Neon PostgreSQL

### Summary
- Migrated Item module from in-memory repository to Neon PostgreSQL using SQLAlchemy ORM.
- Followed the established migration pattern from User, Location, CartType, Timeslot, and Cart modules.
- Table `items` successfully created in the existing Neon database (`neondb`).
- No changes to service layer, controller, schemas, or business logic.

### Model Changes
- `item_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "items"`
  - `cart_type_id` as an indexed `ForeignKey` to `cart_types.id`.
  - `price` stored as `Numeric(10, 2)` for monetary precision.
  - `image_urls` stored as `JSON` column (native JSONB in PostgreSQL, JSON text in SQLite).
  - `is_available` boolean with default `True`.
  - `created_at` / `updated_at` timestamps with `onupdate` trigger.
  - `to_dict()` converts `Decimal` price to `float` for JSON serialization.
  - No ORM `relationship()` declarations — ForeignKey only.

### Repository Refactor
- `item_repository.py` rewritten to session-based CRUD (commit/rollback/finally pattern).
- Removed in-memory `_store` dictionary and `_next_id` counter.
- Removed `BaseRepository` inheritance.
- All 7 methods migrated: `create`, `find_by_id`, `find_all`, `find_by_cart_type_id`, `find_by_name_and_cart_type`, `update`, `delete`.
- Optional `session_factory` injection for test isolation (production defaults to Neon).
- Singleton export preserved: `item_repository`.

### Test Isolation
- 3 test files updated to inject the SQLite in-memory `session_factory` into `ItemRepository`:
  - `test_item_service.py` — added `Item` model import for schema registration.
  - `test_booking_item_service.py` — changed `ItemRepository()` to `ItemRepository(session_factory=test_session_factory)`, added `Item` model import.
  - `test_booking_service.py` — same changes as above.
- Maintained zero connections to the Neon DB during automated testing.

### Verification
- **All 125 tests passing** across 8 independent feature modules without regressions.
- Confirmed table mapping via FastAPI startup event (`Base.metadata.create_all`).
- Manual persistence test confirmed:
  - Item created via `POST /api/v1/items` (201, correct data with Decimal-safe price).
  - Server restarted — item retrieved with identical data.
  - Direct DB query (`SELECT * FROM items`) confirmed row in Neon.
  - Test item cleaned up via `DELETE /api/v1/items/1`.

### Current Architecture State
- **User, Location, CartType, Timeslot, Cart, Booking, BookingItem, Item** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **All modules now fully DB-backed.** No in-memory repositories remain.

**Status**:
Item module successfully migrated to PostgreSQL.
Backend is now 100% database-backed — zero in-memory repositories remain.
System stable.

---

## 25 Feb 2026 — Timeslot & Cart Migration to Neon PostgreSQL

### Summary
- Migrated Timeslot and Cart modules from in-memory repositories to Neon PostgreSQL using SQLAlchemy ORM.
- Followed the established migration pattern from the User, Location, and CartType modules.
- Tables `timeslots` and `carts` successfully created in the existing Neon database (`neondb`).
- No changes to business logic or downstream service features.

### Model Changes
- `timeslot_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "timeslots"`
  - `location_id` as an indexed `ForeignKey` to `locations.id`.
  - Enforced `nullable=False` on data columns and initialized `DateTime` attributes.
- `cart_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "carts"`
  - `region_id` as an indexed `ForeignKey` to `locations.id`.
  - `cart_type_id` as an indexed `ForeignKey` to `cart_types.id`.
  - Enforced `nullable=False` on data columns and initialized `DateTime` attributes.

### Repository Refactor
- `timeslot_repository.py` and `cart_repository.py` rewritten to use session-based CRUD.
- Implemented robust `commit/rollback/finally` transaction management.
- Replaced `_store` dictionary logic with SQLAlchemy query filters.
- Preserved singletons and optional `session_factory` injection for test isolation.

### Test Isolation
- 4 test suites updated to inject the SQLite in-memory `session_factory`:
  - `test_timeslot_service.py`
  - `test_cart_service.py`
  - `test_booking_service.py`
  - `test_booking_item_service.py`
- Required importing ORM models to properly build SQLite schema metadata.
- Maintained zero connections to the Neon DB during automated testing.

### Verification
- **All 125 tests passing** across 8 independent feature modules without regressions.
- Confirmed table mapping via FastAPI startup event (`Base.metadata.create_all`).
- Manual tests validated via standard POST requests and direct Neon DB queries verifying data persistence.

### Current Architecture State
- **User, Location, CartType, Timeslot, Cart** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Item, Booking, BookingItem** → in-memory repositories (pending migration)

**Status**:
Timeslot & Cart modules successfully migrated to PostgreSQL.
Cross-table relationships successfully modeled with `ForeignKey`.
System stable.

---

## 24 Feb 2026 — Location & CartType Migration to Neon PostgreSQL

### Summary
- Migrated Location and CartType modules from in-memory repositories to Neon PostgreSQL using SQLAlchemy ORM.
- Followed the exact migration pattern established by the User module (23 Feb 2026).
- Tables created in existing Neon project: **VMS-Backend** (`still-darkness-99863466`), database: `neondb`.
- No new Neon project or database created.

### Model Changes
- `location_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "locations"`
  - DB-level unique constraint on `name` column (indexed).
  - `is_serviceable` boolean with default True.
  - `created_at` / `updated_at` timestamps.
  - `to_dict()` preserved for backward compatibility.
- `cart_type_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "cart_types"`
  - DB-level unique constraint on `name` column (indexed).
  - `description` nullable, `is_active` boolean default True.
  - `created_at` / `updated_at` timestamps.
  - `to_dict()` preserved for backward compatibility.

### Repository Refactor
- `location_repository.py` rewritten to session-based CRUD (commit/rollback/finally pattern).
- `cart_type_repository.py` rewritten to session-based CRUD (same pattern).
- Optional `session_factory` injection for test isolation (production defaults to Neon).
- Singleton exports preserved: `location_repository`, `cart_type_repository`.

### Test Isolation
- 7 test files updated to use SQLite in-memory session injection:
  - `test_location_service.py` — inject Location session
  - `test_cart_type_service.py` — inject CartType session
  - `test_timeslot_service.py` — inject Location session
  - `test_cart_service.py` — inject Location + CartType sessions
  - `test_item_service.py` — inject CartType session
  - `test_booking_service.py` — extended SQLite setup for Location + CartType
  - `test_booking_item_service.py` — extended SQLite setup for Location + CartType
- Zero Neon connections during tests.

### Verification
- **All 125 tests passing** — zero regressions.
- Tables auto-created via `Base.metadata.create_all` on server startup.

### Current Architecture State
- **User** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Location** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **CartType** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **All other modules** → in-memory repositories

**Status**:
Location & CartType modules successfully migrated to PostgreSQL.
Test isolation enforced across all modules.
System stable.

---

## 23 Feb 2026 — Day 9: User Module Migration to Neon PostgreSQL

### Summary
- Migrated User module from in-memory repository to Neon PostgreSQL using SQLAlchemy ORM.
- All other modules remain in-memory.
- Hybrid architecture temporarily in place.

### Infrastructure Changes
- Added SQLAlchemy engine + `SessionLocal` session factory in `core/database/db_connection.py`.
- Added `Base` declarative model for ORM inheritance.
- `DATABASE_URL` stored in `.env` (not committed to version control).
- Table creation via `Base.metadata.create_all()` on FastAPI startup event.
- New Neon project: **VMS-Backend** (`still-darkness-99863466`).
- Dependencies added: `sqlalchemy`, `psycopg2-binary`, `python-dotenv`.

### Repository Refactor
- `user_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
- `user_repository.py` rewritten to session-based CRUD (commit/rollback/finally pattern).
- Optional `session_factory` injection added for test isolation (production defaults to Neon).
- DB-level unique constraint on `phone` column (`ix_users_phone` index).
- `to_dict()` method preserved for backward compatibility with service layer.

### Test Isolation Fix
- Booking and BookingItem tests updated to inject SQLite in-memory `UserRepository`.
- Prevented any Neon usage during tests — zero production DB connections.
- Full test suite restored to green.

### Verification
- Manual persistence test confirmed:
  - Users persist after server restart.
  - Auto-increment IDs working correctly.
  - Unique constraint enforced at DB level.
- Neon table verified via MCP: columns, indexes, constraints all correct.

### Current Architecture State
- **User** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **All other modules** → in-memory repositories
- **All 125 tests passing** — zero regressions.

### Next Steps
- Migrate Location module to Neon.
- Continue module-by-module migration.
- Auth implementation postponed until full DB migration complete.

**Status**:
User module successfully migrated to PostgreSQL.
Test isolation enforced across all modules.
System stable.

---

## 23 Feb 2026
- **BookingItem Module Implementation**: New module under `modules/booking_item/` with full layered architecture.
- **Model Fields**: id, booking_id, item_id, quantity, unit_price (Decimal), created_at.
- **Architecture — Validate-then-Persist**:
  - `BookingItemService.validate_items()` — validation only, returns sanitized snapshots.
  - `BookingItemService.create_booking_items()` — persists only after parent booking exists.
  - `BookingItemService.calculate_estimated_total()` — operates on validated snapshot data.
- **Validation Rules**:
  - Item must exist (authoritative lookup from ItemRepository).
  - Item must belong to the booking's cart_type.
  - Item must be available (`is_available == True`).
  - Quantity must be a positive integer.
- **Monetary Precision**: All price/total calculations use `Decimal` to avoid float rounding errors.
- **Booking Integration**:
  - `BookingService.create_booking()` accepts optional `items` list.
  - `estimated_total` is always server-computed: `sum(qty * unit_price)`. Client values ignored.
  - Items validated before any side effects (cart assignment, payment simulation).
  - BookingItem records created after booking record — no orphaned records.
  - `estimated_total` removed from required schema fields.
  - Backward compatible: bookings without items still work (`estimated_total = 0.00`).
- **Payment simulation logic not modified**.
- **Unit Tests**:
  - BookingItem module: 11/11 passed (valid items, no items, invalid ID, wrong cart_type, invalid qty, total calculation, unavailable item, no side effects on failure).
  - Booking module: 17/17 passed (no regressions).
  - All other modules: no regressions.
  - **Total: 125/125 passed.**

**Status**:
Eighth feature module successfully integrated.
No core refactors required.
System stable.

## 22 Feb 2026
- **Booking Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, user_id, region_id, cart_type_id, timeslot_id, assigned_cart_id (nullable), address, booking_fee, estimated_total, status, payment_status, refund_status, refund_amount, created_at, updated_at.
- **Status Enums**:
  - Booking status: PENDING_PAYMENT, CONFIRMED, CANCELLED, COMPLETED, PAYMENT_FAILED.
  - Payment status: PENDING, SUCCESS, FAILED.
  - Refund status: NONE, PENDING, REFUNDED.
- **Validation & Constraints**:
  - Cross-module FK validation (user, region, cart_type, timeslot).
  - Slot capacity enforcement (count active bookings vs timeslot capacity).
  - Daily user booking limit (max 3 per user per day).
  - Cart availability check — booking fails if no AVAILABLE cart found.
- **Booking Flow**: Validate FKs → Slot capacity → Daily limit → Cart availability → Simulate payment → Assign cart (BUSY) → Confirm booking.
- **Cart Lifecycle**: Cart set to BUSY on assignment; released to AVAILABLE on cancel/complete.
- **Simulated Payment**: Always returns SUCCESS (no real gateway integration yet).
- **Integration**:
  - Integrated routes at `/api/v1/bookings`.
  - Registered booking router in `main.py`.
  - Endpoints: POST (create), GET (list/detail), POST /{id}/cancel, POST /{id}/complete.
- **Unit Tests**:
  - Booking module: 17/17 passed.
  - Item module: no regressions.
  - Cart module: no regressions.
  - Cart type module: no regressions.
  - Timeslot module: no regressions.
  - Location module: no regressions.
  - User module: no regressions.
  - **Total: 114/114 passed.**

**Status**:
Seventh feature module successfully integrated.
No core refactors required.
System stable.

## 20 Feb 2026
- **Item Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, cart_type_id, name, description, price, image_urls, is_available, created_at, updated_at.
- **Validation & Constraints**:
  - Cross-module cart type validation (existence check via CartTypeRepository).
  - Price must be >= 0 (schema-level enforcement).
  - image_urls optional; validated as list of http/https URL strings.
  - is_available optional (defaults to True).
  - Unique item name enforced per cart_type.
- **Integration**:
  - Integrated routes at `/api/v1/items`.
  - Registered item router in `main.py`.
  - Optional `?cart_type_id=` query param on list endpoint.
- **Unit Tests**:
  - Item module: 16/16 passed.
  - Cart module: no regressions.
  - Cart type module: no regressions.
  - Timeslot module: no regressions.
  - Location module: no regressions.
  - User module: no regressions.
  - **Total: 97/97 passed.**

**Status**:
Sixth feature module successfully integrated.
No core refactors required.
System stable.

## 19 Feb 2026
- **Cart Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, region_id, cart_type_id, status, is_active, created_at, updated_at.
- **Validation & Constraints**:
  - Cross-module region validation (existence check via LocationRepository).
  - Cross-module cart type validation (existence check via CartTypeRepository).
  - Status enum enforcement: AVAILABLE, BUSY, BUFFER, OFFLINE.
  - Status input normalized to uppercase.
  - is_active optional (defaults to True).
- **Integration**:
  - Integrated routes at `/api/v1/carts`.
  - Registered cart router in `main.py`.
- **Unit Tests**:
  - Cart module: all passed.
  - Cart type module: no regressions.
  - Timeslot module: no regressions.
  - Location module: no regressions.
  - User module: no regressions.

**Status**:
Fifth feature module successfully integrated.
No core refactors required.
System stable.

## 18 Feb 2026
- **Cart Type Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, name, description, is_active, created_at, updated_at.
- **Validation & Constraints**:
  - Enforced unique cart type name validation in the service layer.
  - Description optional (defaults to empty string).
  - is_active optional (defaults to True).
- **Integration**:
  - Integrated routes at `/api/v1/cart-types`.
  - Registered cart type router in `main.py`.
- **Unit Tests**:
  - Cart type module: 13/13 passed.
  - Timeslot module: 14/14 passed (no regressions).
  - Location module: 13/13 passed (no regressions).
  - User module: 26/26 passed (no regressions).

**Status**:
Fourth feature module successfully integrated.
No core refactors required.
System stable.

## 17 Feb 2026
- **Timeslot Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, location_id, date, start_time, end_time, capacity, created_at, updated_at.
- **Validation & Constraints**:
  - Cross-module location validation (existence + is_serviceable).
  - Time-range validation (end_time > start_time).
  - Unique constraint on (location_id, date, start_time).
  - Capacity must be > 0.
- **Integration**:
  - Integrated routes at `/api/v1/timeslots`.
  - Registered timeslot router in `main.py`.
- **Unit Tests**:
  - Timeslot module: 14/14 passed.
  - Location module: 13/13 passed (no regressions).
  - User module: 26/26 passed (no regressions).
- **Bug Fix — Shared Repository Instance**:
  - **Issue**: `LocationService` and `TimeslotService` each instantiated their own `LocationRepository()`, creating separate in-memory stores. Timeslot validation could not see locations created via the location API.
  - **Fix**: Exported a module-level singleton (`location_repository`) from `location_repository.py`. Both services now import and default to this shared instance. Optional injection preserved for test isolation.
  - **Guideline added**: PROJECT_GUIDELINES §2 updated — all in-memory repositories must be shared instances across modules.

**Status**:
Third feature module successfully integrated.
No core refactors required.
System stable.

## 16 Feb 2026
- **Location Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Validation & Constraints**:
  - Enforced unique location name validation in the service layer.
- **Integration**:
  - Integrated routes at `/api/v1/locations`.
  - Registered location router in `main.py`.
- **API Testing**: Manual verification completed in Swagger UI for:
  - Create location
  - Duplicate name validation
  - Update location (timestamp verification)
  - Delete location
  - Fetch after delete (404 validation)
- **Unit Tests**:
  - Location module: 13/13 passed.
  - User module: 26/26 passed (no regressions).

**Status**:
Backend architecture successfully validated for multi-module scalability.
No core refactors required.
System stable.

## 15 Feb 2026
- **User Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Validation & Constraints**:
  - Enforced unique phone number validation in the service layer.
  - Implemented role normalization (case-insensitive input, normalized to lowercase).
- **Infrastructure**:
  - Integrated centralized error handling middleware for consistent API responses.
  - Created and verified the FastAPI entry point (`main.py`).
  - Enabled interactive Swagger documentation at `/docs`.
- **Manual API Testing**: Successfully verified the following flows:
  - User creation with valid data.
  - Prevention of duplicate phone numbers.
  - User updates with automatic `updated_at` timestamp refresh.
  - Deletion of user records and subsequent "Not Found" validation.

## 17 Mar 2026 — Day 27.5: Item Visual Support

- **Backend**: Added `image_url` (String, nullable) field to `Item` model alongside existing `image_urls` (JSON list).
- **to_dict() fallback**: `image_url` resolves to explicit `image_url` value, or falls back to first entry in `image_urls` if present, ensuring backward compatibility.
- **Schema validation**: Added `image_url` (optional string) to both `CreateItemSchema` and `UpdateItemSchema` with non-empty string enforcement when provided.
- **Repository**: `create()` now persists `image_url`; `update()` handles it generically via existing `setattr` loop.
- **Tests**: Added 4 new tests — `test_create_item_with_image_url`, `test_update_item_image_url`, `test_item_without_image_still_valid`, `test_to_dict_fallback_image`. All 20 tests pass.
- **Admin App**: Added `image_url: String? = null` to `Item`, `CreateItemRequest`, `UpdateItemRequest` models.
- **Admin App**: `ItemRepository`, `ItemViewModel` updated to pass `description` and `imageUrl` through to the API.
- **Admin App**: `ItemCard` now shows `AsyncImage` (Coil) when a valid URL is present, or a placeholder icon otherwise. Description shown below name with 2-line ellipsis.
- **Admin App**: `ItemDialog` adds Description and Image URL fields with UX hint "Paste image link (e.g., from Imgur)".
- **Admin App**: Coil 2.7.0 added to `libs.versions.toml` and `build.gradle.kts`.
- **User App**: Added `Item` data class with `description` and `image_url` fields for future UI rendering.
- **No breaking changes**: Existing items without images work correctly with placeholder fallback.


## 2026-05-19 — Phase 01: RBAC Foundation (Tasks 1–6) — Backend middleware + Android auth layer

### Added
- `backend/core/security/auth_manager.py` — `AuthManager` class with `_ALL_PERMISSIONS` frozenset (23 permissions), `_ROLE_PERMISSIONS` map for all 7 Plixo roles + `user`, `has_permission(role, permission)` and `get_permissions(role)` methods, module-level `auth_manager` singleton.
- `require_role(*allowed_roles)` factory in `auth_dependencies.py` — returns a FastAPI `Depends` that enforces role membership; role values compared against `UserRole` enum.
- `require_permission(permission)` factory in `auth_dependencies.py` — delegates to `auth_manager.has_permission`; returns 403 with named permission in detail.

### Modified
- `backend/modules/user/model/user_model.py` — `UserRole` enum extended from 2 values (`USER`, `ADMIN`) to 8 (`USER`, `SUPER_ADMIN`, `OPS_MANAGER`, `GROUND_OWNER`, `TOURNAMENT_MANAGER`, `SUPPORT`, `FINANCE`, `CSR_PARTNER`). `ADMIN` removed.
- `backend/main.py` — `startup()` function: added idempotent `UPDATE users SET role = 'super_admin' WHERE role = 'admin'` migration shim so existing admin rows are renamed on next boot.
- `backend/modules/auth/dependencies/auth_dependencies.py` — added `from typing import Callable`, `UserRole` and `auth_manager` imports; added `require_role()` and `require_permission()` factories. Existing `require_admin`, `require_user`, `get_current_user` left intact for backwards compatibility.
- `backend/modules/payment/controller/payment_routes.py` — all 7 endpoints migrated from `require_admin` to granular `require_role` guards: list/booking/config-read allow FINANCE+SUPER_ADMIN+OPS_MANAGER+SUPPORT; approve/reject/refund/config-write restricted to FINANCE+SUPER_ADMIN only.
- `backend/modules/admin/controller/admin_routes.py` — all 6 endpoints migrated: dashboard allows SUPER_ADMIN+OPS_MANAGER+FINANCE; metrics/queue-stats allow SUPER_ADMIN+OPS_MANAGER; matches allows +SUPPORT+TOURNAMENT_MANAGER; config endpoints restricted to SUPER_ADMIN only.
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/TokenManager.kt` — added `ROLE_KEY`, `roleFlow`, `saveRole()`, `clearRole()` via new `clearSession()` which atomically clears both token and role. `clearToken()` now delegates to `clearSession()`.
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/AuthViewModel.kt` — added `currentRole: StateFlow<String?>` backed by `tokenManager.roleFlow` via `stateIn`; replaced hard-coded `role != "admin"` gate with `adminRoles` set of all 7 Plixo roles; now calls `tokenManager.saveRole(role)` on successful login; `logout()` uses `clearSession()`.

### Backend changes
- New permission middleware in `core/security/auth_manager.py` — pure Python, no DB queries
- `require_role()` / `require_permission()` usable on any FastAPI route as `current_user: dict = require_role(UserRole.X, ...)`
- Existing `require_admin` still wired on routes not touched this session — will be migrated in follow-on phases

### Architectural decisions
- Kept `require_admin()` in `auth_dependencies.py` during transition; removing it now would break untouched controllers. It will be eliminated after all controllers are migrated to `require_role`.
- Role stored in JWT payload (already existed) and mirrored to DataStore on login — avoids re-decoding JWT on every screen load.
- `UserRole` uses `str, Enum` so enum values compare equal to raw JWT role strings without extra coercion.
- Startup migration shim is idempotent — safe to leave permanently; no Alembic dependency.


## 2026-05-19 — Phase 01: RBAC Tasks 7 & 8 — Role-filtered navigation + ForbiddenScreen

### Added
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/ForbiddenScreen.kt` — new Compose screen shown when a user lacks access; displays "Access Denied" message with a Logout button.

### Modified
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt`
  - Added imports: `collectAsState`, `getValue`, `ForbiddenScreen`
  - Collect `role` from `authViewModel.currentRole` as state
  - Pass `role = role ?: ""` to `MainScreen` in the `composable("main")` block
  - Added `composable("forbidden")` route that renders `ForbiddenScreen` with logout + navigate-to-login logic

- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/MainScreen.kt`
  - Added `role: String = ""` parameter to `MainScreen` composable
  - Replaced hardcoded `val items = listOf(...)` with role-filtered list: `Payments` visible only to `super_admin`/`finance`; `Manage` visible only to `super_admin`/`ops_manager`; `Dashboard` and `Bookings` visible to all

### Architectural decisions
- Role filtering is applied at the composable level using the `role` StateFlow collected in `AppNavigation`, following the four-layer RBAC enforcement pattern (backend, ViewModel, navigation, UI).
- `ForbiddenScreen` is wired into the NavHost so any future guard logic can `navigate("forbidden")` without additional boilerplate.
- No changes to `BottomNavItem` sealed class definitions.


## 2026-05-27 — Phase 01: Captain module + timeslot is_active + user phone search

### Added
- `backend/modules/captain/__init__.py` — package marker
- `backend/modules/captain/controller/__init__.py` — package marker
- `backend/modules/captain/model/captain_model.py` — `CaptainStatus` constants class + `Captain` SQLAlchemy ORM model mapping to `captains` table; FK to `users` (CASCADE) and `locations` (SET NULL); fields: id, user_id, region_id, status, rating, total_trips, bio, created_at, updated_at; `to_dict()` serializer.
- `backend/modules/captain/schemas/captain_schema.py` — `CreateCaptainSchema` (required: user_id; optional: region_id, bio) and `UpdateCaptainSchema` (all optional: region_id, status, bio); status validates against `CaptainStatus.ALL` frozenset.
- `backend/modules/captain/repository/captain_repository.py` — `CaptainRepository` with `get_all(region_id)`, `get_by_id`, `get_by_user_id`, `create`, `update`, `delete`; module-level `captain_repository` singleton.
- `backend/modules/captain/service/captain_service.py` — `CaptainService` with `list_captains(region_id)`, `get_captain`, `create_captain`, `update_captain`, `delete_captain`; all list/get methods join with `users` table to enrich dicts with `name` + `phone`; `create_captain` validates user existence and duplicate guard; raises HTTPException 404/400 directly.
- `backend/modules/captain/controller/captain_routes.py` — FastAPI router at `/api/v1/captains`; GET/POST guarded by `OPS_MANAGER|SUPER_ADMIN`; GET/{id}/PUT/{id} same; DELETE/{id} restricted to `SUPER_ADMIN`; uses `require_role()` factory pattern.
- `backend/run_migrations.py` — standalone psycopg2 migration script (run once from backend/ directory).

### Modified
- `backend/modules/timeslot/model/timeslot_model.py` — added `is_active` column (`Boolean, nullable=False, default=True, server_default="true"`); updated `to_dict()` to include `is_active`.
- `backend/modules/timeslot/schemas/timeslot_schema.py` — added `is_active` validation to `UpdateTimeslotSchema` (optional bool field).
- `backend/modules/timeslot/repository/timeslot_repository.py` — `create()` now explicitly passes `is_active=timeslot_data.get("is_active", True)` to the ORM constructor; `update()` already handles via generic setattr loop.
- `backend/modules/user/controller/user_routes.py` — added `GET /api/v1/users/search?phone=` endpoint (SUPER_ADMIN + SUPPORT only); direct DB query via `get_db` dependency; returns 404 if not found.
- `backend/main.py` — registered `captain_router` and `Captain` model (for `Base.metadata.create_all`).

### Backend changes (schema / routes)
- `captains` table: `CREATE TABLE IF NOT EXISTS captains (id SERIAL PK, user_id INT UNIQUE NOT NULL FK users, region_id INT FK locations, status VARCHAR(50) DEFAULT 'ACTIVE', rating FLOAT DEFAULT 0.0, total_trips INT DEFAULT 0, bio TEXT, created_at TIMESTAMP, updated_at TIMESTAMP)`
- `timeslots` table: `ALTER TABLE timeslots ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE`
- New routes: `GET/POST /api/v1/captains`, `GET/PUT/DELETE /api/v1/captains/{id}`, `GET /api/v1/users/search?phone=`

### Architectural decisions
- Captain service owns the user-join logic internally (not in the repository) to keep repository pure data-access; service opens its own session for join queries rather than accepting one from caller — consistent with existing LocationService/TimeslotService pattern.
- `is_active` on timeslots follows the same `server_default="true"` pattern as `Boolean` columns elsewhere to ensure DB-level default for rows inserted outside the ORM.
- User phone search route placed at `/users/search` (before `/{user_id}`) so FastAPI's path matching doesn't shadow it with the int-param route.
- `run_migrations.py` created as a standalone script (not Alembic) consistent with the no-Alembic project constraint; migrations are idempotent (`IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`).
