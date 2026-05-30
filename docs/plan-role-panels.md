# Plan — Phase 01A Role Panels + User Screen Filters

**Date:** 2026-05-29
**Status:** In progress

## Scope (this cycle — frontend only, backend already supports)

### 1. User screen (UsersScreen.kt)
- Add a horizontal filter chip row: "All" + each role present in the list.
- When "All" is selected → group users by role with section headers.
- When a specific role is selected → flat list of just that role.
- Show per-group counts.
- Pure UI/state; no ViewModel or backend change.

### 2. Finance panel (Payments)
- `ApiService.kt`: add `refundPayment(paymentId)` → `POST /api/v1/payments/refund/{payment_id}` (endpoint exists).
- `PaymentRepository.kt`: add `refundPayment`.
- `PaymentViewModel.kt`: keep ALL payments; add `filter` (PENDING_REVIEW | ALL); expose filtered list + `todaysRevenue` (sum of SUCCESS amounts); add `refundPayment`.
- `PaymentsScreen.kt`: tab row (Pending Review | All), revenue summary card, refund button on SUCCESS rows.

### 3. Tournament Manager
- `MatchesScreen.kt`: make `onBack` nullable; hide back arrow when null (so it works as a bottom tab).
- `MainScreen.kt`: add `Matches` bottom nav item visible to `tournament_manager`; route `matches` → `MatchesScreen(onBack = null)`.

### 4. CSR Partner
- New `CsrScreen.kt`: read-only matches list (reuse `MatchViewModel`), tournaments "coming soon" placeholder (Phase 02).
- `MainScreen.kt`: add `CSR` bottom nav item for `csr_partner`; route `csr` → `CsrScreen`.

## Deferred to next cycle (needs backend work)

### Ground Owner panel
Requires data isolation at repository/query level (CLAUDE.md hard rule):
- `booking_service.list_bookings_by_region(region_id)` + route branch for `ground_owner`.
- Grounds filtered by region for `ground_owner`.
- `GroundOwnerScreen.kt` + bottom tab.
Tracked separately; not in this cycle.

## Files touched
- App: UsersScreen.kt, PaymentsScreen.kt, PaymentViewModel.kt, PaymentRepository.kt, ApiService.kt, MatchesScreen.kt, MainScreen.kt, CsrScreen.kt (new), UserManagementViewModelTest.kt (mock)
- Backend: none this cycle
