# Finance Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend payment summary endpoint (DB-level aggregates) and a REFUNDED filter tab to the admin app Payments screen.

**Architecture:** New `GET /api/v1/payments/summary` endpoint runs SQL aggregates via the existing `PaymentRepository`. Admin app fetches summary on load and derives the revenue card from the backend response instead of a local sum. A new `REFUNDED` enum value in `PaymentFilter` adds refund history visibility.

**Tech Stack:** Python 3.12 / FastAPI / SQLAlchemy (backend); Kotlin / Jetpack Compose / Retrofit / StateFlow (app)

---

## Task 1: Backend — GET /api/v1/payments/summary

**Files:**
- Modify: `backend/modules/payment/repository/payment_repository.py`
- Modify: `backend/modules/payment/service/payment_service.py`
- Modify: `backend/modules/payment/controller/payment_routes.py`

### Context
- `PaymentRepository` uses SQLAlchemy. `Payment` model has `status` (PENDING/UNDER_REVIEW/SUCCESS/FAILED/REFUNDED) and `amount` (Numeric).
- `PaymentService` already has `get_all_payments()`. Add `get_summary()`.
- `payment_routes.py` uses `router = APIRouter(prefix="/api/v1/payments", ...)`. Add new route.
- Use `sqlalchemy.func.sum`, `sqlalchemy.func.count` for DB-level aggregates.
- Route must be restricted to FINANCE and SUPER_ADMIN roles (already pattern exists via `require_role`).

- [ ] **Step 1: Add `get_summary()` to PaymentRepository**

Read `backend/modules/payment/repository/payment_repository.py`. Add after `find_all()`:

```python
def get_summary(self, session=None) -> dict:
    """Return aggregate payment statistics computed at DB level."""
    own_session = session is None
    session = session or self._session_factory()
    try:
        from sqlalchemy import func as sql_func
        total_revenue = session.query(
            sql_func.coalesce(sql_func.sum(Payment.amount), 0)
        ).filter(Payment.status == "SUCCESS").scalar()

        total_refunded = session.query(
            sql_func.coalesce(sql_func.sum(Payment.amount), 0)
        ).filter(Payment.status == "REFUNDED").scalar()

        pending_count = session.query(
            sql_func.count(Payment.id)
        ).filter(Payment.status == "UNDER_REVIEW").scalar()

        refunded_count = session.query(
            sql_func.count(Payment.id)
        ).filter(Payment.status == "REFUNDED").scalar()

        return {
            "total_revenue": float(total_revenue),
            "total_refunded": float(total_refunded),
            "pending_count": int(pending_count),
            "refunded_count": int(refunded_count),
        }
    finally:
        if own_session:
            session.close()
```

- [ ] **Step 2: Add `get_summary()` to PaymentService**

Read `backend/modules/payment/service/payment_service.py`. Add after `get_all_payments()`:

```python
def get_summary(self) -> dict:
    """Return aggregate payment statistics."""
    return self.payment_repository.get_summary()
```

- [ ] **Step 3: Add route to payment_routes.py**

Read `backend/modules/payment/controller/payment_routes.py`. Add a new GET route for `/summary`. Use `require_role(UserRole.FINANCE, UserRole.SUPER_ADMIN)` for access control. Follow existing route patterns in the file.

```python
@router.get("/summary")
def get_payment_summary(
    current_user: dict = require_role(UserRole.FINANCE, UserRole.SUPER_ADMIN),
):
    """Return aggregate payment statistics. Restricted to FINANCE and SUPER_ADMIN."""
    try:
        summary = payment_service.get_summary()
        return _success(summary)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

Check what imports exist in the file — `UserRole`, `require_role`, `_success`, `HTTPException` should already be there. Add any that are missing.

- [ ] **Step 4: Write tests**

Create `backend/modules/payment/tests/test_payment_summary.py`:

```python
"""Tests for PaymentRepository.get_summary()."""
import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.payment.model.payment_model import Payment  # noqa: F401
from modules.payment.repository.payment_repository import PaymentRepository
import modules.user.model.user_model  # noqa: F401
import modules.booking.model.booking_model  # noqa: F401
import modules.cart.model.cart_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.cart_type.model.cart_type_model  # noqa: F401
import modules.timeslot.model.timeslot_model  # noqa: F401
import modules.match.model.match_model  # noqa: F401


def _make_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


def _make_payment(repo, status: str, amount: float):
    import random, string
    ref = "TEST-" + "".join(random.choices(string.ascii_uppercase, k=6))
    return repo.create({
        "booking_id": 1,
        "provider": "MANUAL_UPI",
        "payment_type": "MATCHING_FEE",
        "amount": amount,
        "reference_code": ref,
        "status": status,
    })


class TestPaymentSummary(unittest.TestCase):

    def setUp(self):
        self.repo = PaymentRepository(session_factory=_make_factory())

    def test_summary_counts_correctly(self):
        _make_payment(self.repo, "SUCCESS", 100.0)
        _make_payment(self.repo, "SUCCESS", 200.0)
        _make_payment(self.repo, "REFUNDED", 50.0)
        _make_payment(self.repo, "UNDER_REVIEW", 75.0)

        summary = self.repo.get_summary()
        self.assertAlmostEqual(summary["total_revenue"], 300.0)
        self.assertAlmostEqual(summary["total_refunded"], 50.0)
        self.assertEqual(summary["pending_count"], 1)
        self.assertEqual(summary["refunded_count"], 1)

    def test_summary_returns_zeros_when_empty(self):
        summary = self.repo.get_summary()
        self.assertEqual(summary["total_revenue"], 0.0)
        self.assertEqual(summary["total_refunded"], 0.0)
        self.assertEqual(summary["pending_count"], 0)
        self.assertEqual(summary["refunded_count"], 0)
```

Run: `venv\Scripts\python.exe -m pytest backend/modules/payment/tests/test_payment_summary.py -v` (from project root — if collection error, run from `backend/` dir).

- [ ] **Step 5: Commit**

```
git add backend/modules/payment/repository/payment_repository.py backend/modules/payment/service/payment_service.py backend/modules/payment/controller/payment_routes.py backend/modules/payment/tests/test_payment_summary.py
git commit -m "feat(backend): GET /payments/summary — DB-level revenue + refund aggregates"
```

---

## Task 2: Admin app — PaymentSummary model + REFUNDED tab + backend summary fetch

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/PaymentRepository.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/PaymentViewModel.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/PaymentsScreen.kt`

### Context
- `PaymentViewModel` currently computes `totalRevenue` locally via `allPayments.filter { status == "SUCCESS" }.sumOf { amount }`. Replace with backend fetch.
- `PaymentFilter` enum has `PENDING_REVIEW, ALL`. Add `REFUNDED`.
- `RevenueSummaryCard` in `PaymentsScreen.kt` currently takes `totalRevenue: Double, pendingReviewCount: Int`. Extend to also show `totalRefunded` and `refundedCount`.
- `PaymentFilterTabs` renders tabs for each filter — it will automatically pick up the new `REFUNDED` variant if it iterates over enum values. Check the implementation.

- [ ] **Step 1: Add `PaymentSummary` to Models.kt**

Find the payment-related models section. Add:

```kotlin
@Serializable
data class PaymentSummary(
    val total_revenue: Double,
    val total_refunded: Double,
    val pending_count: Int,
    val refunded_count: Int
)
```

- [ ] **Step 2: Add `getPaymentSummary()` to ApiService.kt**

After `getPayments()`:

```kotlin
@GET("payments/summary")
suspend fun getPaymentSummary(): ApiResponse<PaymentSummary>
```

- [ ] **Step 3: Add `fetchSummary()` to PaymentRepository.kt**

Read the file. Add after `fetchPayments()`:

```kotlin
suspend fun fetchSummary(): PaymentSummary {
    val response = apiService.getPaymentSummary()
    if (response.success && response.data != null) {
        return response.data
    }
    throw Exception(response.message ?: "Failed to fetch summary")
}
```

- [ ] **Step 4: Update PaymentViewModel.kt**

**a) Add `REFUNDED` to `PaymentFilter` enum:**
```kotlin
enum class PaymentFilter { PENDING_REVIEW, ALL, REFUNDED }
```

**b) Add `totalRefunded: Double = 0.0` and `refundedCount: Int = 0` to `PaymentUiState`:**
```kotlin
data class PaymentUiState(
    val payments: List<Payment> = emptyList(),
    val filter: PaymentFilter = PaymentFilter.PENDING_REVIEW,
    val totalRevenue: Double = 0.0,
    val totalRefunded: Double = 0.0,
    val pendingReviewCount: Int = 0,
    val refundedCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**c) Add `loadSummary()` private function and call it from `loadPayments()`:**
```kotlin
private fun loadSummary() {
    viewModelScope.launch {
        try {
            val summary = paymentRepository.fetchSummary()
            _uiState.value = _uiState.value.copy(
                totalRevenue = summary.total_revenue,
                totalRefunded = summary.total_refunded,
                pendingReviewCount = summary.pending_count,
                refundedCount = summary.refunded_count,
            )
        } catch (e: Exception) {
            // Non-fatal — summary card will show 0s
        }
    }
}
```

In `loadPayments()`, add `loadSummary()` call after `allPayments = ...`:
```kotlin
allPayments = paymentRepository.fetchPayments()
loadSummary()
_uiState.value = applyFilter(_uiState.value.filter).copy(isLoading = false)
```

**d) Update `applyFilter()` to handle REFUNDED:**
```kotlin
private fun applyFilter(filter: PaymentFilter): PaymentUiState {
    val visible = when (filter) {
        PaymentFilter.PENDING_REVIEW -> allPayments.filter { it.status == "UNDER_REVIEW" }
        PaymentFilter.REFUNDED -> allPayments.filter { it.status == "REFUNDED" }
        PaymentFilter.ALL -> allPayments
    }
    return _uiState.value.copy(
        payments = visible,
        filter = filter,
        error = null
    )
}
```

Note: remove the local revenue/count computation from `applyFilter` — those now come from backend via `loadSummary()`.

- [ ] **Step 5: Update RevenueSummaryCard in PaymentsScreen.kt**

Read the file. Find `RevenueSummaryCard`. Update its signature and body to show `totalRefunded` and `refundedCount`:

```kotlin
@Composable
private fun RevenueSummaryCard(
    totalRevenue: Double,
    totalRefunded: Double,
    pendingReviewCount: Int,
    refundedCount: Int
) {
    AppCard {
        Text("Revenue Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total Revenue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${String.format("%.2f", totalRevenue)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Refunded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${String.format("%.2f", totalRefunded)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pending Review: $pendingReviewCount", style = MaterialTheme.typography.bodySmall)
            Text("Refunded: $refundedCount", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

Update the call site to pass `totalRefunded = uiState.totalRefunded` and `refundedCount = uiState.refundedCount`.

- [ ] **Step 6: Update PaymentFilterTabs in PaymentsScreen.kt**

Find `PaymentFilterTabs`. Check if it iterates over `PaymentFilter.entries` (or `values()`). If so, it will automatically show the new REFUNDED tab. If it has a hardcoded list, add REFUNDED. The tab label should display "Refunded" for `PaymentFilter.REFUNDED`.

- [ ] **Step 7: Commit**

```
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/PaymentRepository.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/PaymentViewModel.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/PaymentsScreen.kt
git commit -m "feat(app): REFUNDED filter tab + backend-driven revenue summary"
```

---

## Task 3: DEV_LOG + push

- [ ] **Step 1: Run backend tests**

From `backend/` directory:
```
cd backend && ..\venv\Scripts\python.exe -m pytest modules/payment/tests/test_payment_summary.py -v
```

Expected: 2 PASSes.

- [ ] **Step 2: Prepend to `backend/DEV_LOG.md`**

```markdown
---
## [2026-06-04] Phase 02 — Finance reporting

### Backend
**Added:**
- `GET /api/v1/payments/summary` — FINANCE + SUPER_ADMIN only; returns total_revenue, total_refunded, pending_count, refunded_count via DB-level SQL aggregates
- `PaymentRepository.get_summary()` — uses sqlalchemy.func.sum/count with COALESCE
- `PaymentService.get_summary()`
- `backend/modules/payment/tests/test_payment_summary.py` — 2 tests

### Admin App
**Modified:**
- `Models.kt` — added `PaymentSummary` data class
- `ApiService.kt` — added `getPaymentSummary()`
- `PaymentRepository.kt` — added `fetchSummary()`
- `PaymentViewModel.kt` — added REFUNDED to PaymentFilter, totalRefunded/refundedCount to state, loadSummary() fetches backend aggregates
- `PaymentsScreen.kt` — RevenueSummaryCard shows total refunded + count; REFUNDED filter tab added

### Architecture decisions
- Revenue computed at DB level (not client-side sum) — scales regardless of payment volume
- loadSummary() is non-fatal: summary card shows 0s if endpoint fails, payments list still loads
---
```

- [ ] **Step 3: Commit and push**

```
git add backend/DEV_LOG.md
git commit -m "chore: DEV_LOG Phase 02 finance reporting"
git push
```
