# Development Log

## 13 Mar 2026 — Day 25: Admin Fee Configuration Panel

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

