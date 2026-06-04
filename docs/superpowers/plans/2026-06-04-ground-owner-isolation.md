# Ground Owner Data Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce repository-level data isolation so a GROUND_OWNER sees only their own grounds and bookings, and give SUPER_ADMIN a phone-search UI to assign grounds to owners.

**Architecture:** Add `owner_user_id` (nullable FK → users) to the `carts` table via migration. Add `find_by_owner()` to `CartRepository` and `BookingRepository` using parameterised SQL filters. Replace Python-side region filtering in routes with DB-level owner filtering. Admin app gains an owner-assignment dialog in GroundsScreen (SUPER_ADMIN only) using the existing phone-search API.

**Tech Stack:** Python 3.12 / FastAPI / SQLAlchemy / PostgreSQL / psycopg2 (backend); Kotlin / Jetpack Compose / Retrofit / StateFlow (admin app)

---

## File Map

| File | Change |
|------|--------|
| `backend/run_migrations.py` | Add Migration 7: `owner_user_id` column on carts |
| `backend/modules/cart/model/cart_model.py` | Add `owner_user_id` Column + `to_dict()` entry |
| `backend/modules/cart/repository/cart_repository.py` | Add `find_by_owner()`, update `create()` to accept `owner_user_id` |
| `backend/modules/cart/service/cart_service.py` | Add `list_carts_by_owner()` |
| `backend/modules/cart/schemas/ground_schema.py` | Add `owner_user_id` to `UpdateGroundSchema` + `_to_ground()` |
| `backend/modules/cart/controller/ground_routes.py` | Replace region-filter with owner-filter for GROUND_OWNER |
| `backend/modules/booking/repository/booking_repository.py` | Add `find_by_owner()` with subquery |
| `backend/modules/booking/service/booking_service.py` | Add `list_bookings_by_owner()` |
| `backend/modules/booking/controller/booking_routes.py` | Replace region-filter with owner-filter for GROUND_OWNER |
| `Vmsadminapp/.../models/Models.kt` | Add `owner_user_id: Int?` to `Ground`, add `owner_user_id: Int?` to `UpdateGroundRequest` |
| `Vmsadminapp/.../data/GroundRepository.kt` | Add `assignOwner()`, `searchUserByPhone()` |
| `Vmsadminapp/.../network/ApiService.kt` | `updateGround` already exists — verify it accepts full `UpdateGroundRequest` |
| `Vmsadminapp/.../viewmodel/GroundViewModel.kt` | Add `assignOwner()`, `searchOwnerByPhone()`, `foundOwner` state |
| `Vmsadminapp/.../ui/screens/GroundsScreen.kt` | Add owner-assignment section to `GroundCard` (SUPER_ADMIN only) |
| `backend/DEV_LOG.md` | Append Phase 02 entry |

---

## Task 1: Migration + Cart model + CartRepository

**Files:**
- Modify: `backend/run_migrations.py`
- Modify: `backend/modules/cart/model/cart_model.py`
- Modify: `backend/modules/cart/repository/cart_repository.py`

### Context
- `run_migrations.py` uses psycopg2, connects via `DATABASE_URL` from `.env`. Existing pattern: `cur.execute("ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...");`
- `cart_model.py` uses SQLAlchemy Column. Existing columns: `id, label, region_id, cart_type_id, status, is_active, latitude, longitude, created_at, updated_at`. The `to_dict()` returns a plain dict — add `owner_user_id` there too.
- `cart_repository.py`: `find_all()` is at line 58. `create()` builds `Cart(...)` at line 29. The `update()` method at line 69 already uses `setattr(cart, key, value)` for any key in `update_data` — so `owner_user_id` updates work automatically once the column exists on the model.
- Run migrations from project root: `venv\Scripts\python.exe backend/run_migrations.py`

- [ ] **Step 1: Add Migration 7 to run_migrations.py**

Open `backend/run_migrations.py`. Before `conn.commit()`, add:

```python
print("Running migration 7: add owner_user_id to carts ...")
cur.execute("""
    ALTER TABLE carts
        ADD COLUMN IF NOT EXISTS owner_user_id INT REFERENCES users(id) ON DELETE SET NULL;
""")
```

- [ ] **Step 2: Run the migration**

```
venv\Scripts\python.exe backend/run_migrations.py
```

Expected: prints all migration steps including "Running migration 7: add owner_user_id to carts ..." and ends with "All migrations completed successfully."

- [ ] **Step 3: Add `owner_user_id` column to Cart model**

In `backend/modules/cart/model/cart_model.py`, add after `longitude`:

```python
owner_user_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
```

The import block already has `Integer, ForeignKey` — no new imports needed.

Also add to `to_dict()` after `"longitude"`:

```python
"owner_user_id": self.owner_user_id,
```

- [ ] **Step 4: Update `CartRepository.create()` to accept `owner_user_id`**

In `cart_repository.py`, in the `create()` method, the `Cart(...)` constructor currently has `label, region_id, cart_type_id, status, is_active`. Add `owner_user_id`:

```python
cart = Cart(
    label=cart_data.get("label", ""),
    region_id=cart_data.get("region_id"),
    cart_type_id=cart_data.get("cart_type_id"),
    status=cart_data.get("status", "AVAILABLE"),
    is_active=cart_data.get("is_active", True),
    owner_user_id=cart_data.get("owner_user_id"),
)
```

- [ ] **Step 5: Add `CartRepository.find_by_owner()` after `find_all()`**

```python
def find_by_owner(self, owner_user_id: int, session=None) -> list[dict]:
    """Retrieve all carts owned by a specific user. Enforces GROUND_OWNER isolation at DB level."""
    own_session = session is None
    session = session or self._session_factory()
    try:
        carts = (
            session.query(Cart)
            .filter(Cart.owner_user_id == owner_user_id)
            .all()
        )
        return [c.to_dict() for c in carts]
    finally:
        if own_session:
            session.close()
```

- [ ] **Step 6: Write tests for `find_by_owner()`**

Create `backend/modules/cart/tests/test_cart_owner_isolation.py`:

```python
"""Tests for CartRepository.find_by_owner() — owner-level isolation."""
import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.cart.repository.cart_repository import CartRepository


def _make_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestCartOwnerIsolation(unittest.TestCase):

    def setUp(self):
        self.repo = CartRepository(session_factory=_make_factory())

    def _create(self, region_id=1, cart_type_id=1, owner_user_id=None):
        return self.repo.create({
            "region_id": region_id,
            "cart_type_id": cart_type_id,
            "owner_user_id": owner_user_id,
        })

    def test_find_by_owner_returns_only_owned_carts(self):
        """find_by_owner returns only carts whose owner_user_id matches."""
        self._create(owner_user_id=10)
        self._create(owner_user_id=10)
        self._create(owner_user_id=99)  # different owner

        results = self.repo.find_by_owner(10)
        self.assertEqual(len(results), 2)
        self.assertTrue(all(c["owner_user_id"] == 10 for c in results))

    def test_find_by_owner_returns_empty_when_no_match(self):
        """find_by_owner returns [] when the user owns no carts."""
        self._create(owner_user_id=10)
        results = self.repo.find_by_owner(999)
        self.assertEqual(results, [])

    def test_find_by_owner_excludes_unowned_carts(self):
        """find_by_owner excludes carts with owner_user_id=None."""
        self._create(owner_user_id=None)  # unowned
        self._create(owner_user_id=10)

        results = self.repo.find_by_owner(10)
        self.assertEqual(len(results), 1)
```

- [ ] **Step 7: Run the tests**

```
venv\Scripts\python.exe -m pytest backend/modules/cart/tests/test_cart_owner_isolation.py -v
```

Expected: 3 PASSes.

- [ ] **Step 8: Commit**

```
git add backend/run_migrations.py backend/modules/cart/model/cart_model.py backend/modules/cart/repository/cart_repository.py backend/modules/cart/tests/test_cart_owner_isolation.py
git commit -m "feat(backend): owner_user_id on carts — migration, model, repository"
```

---

## Task 2: CartService + UpdateGroundSchema + ground_routes.py

**Files:**
- Modify: `backend/modules/cart/service/cart_service.py`
- Modify: `backend/modules/cart/schemas/ground_schema.py`
- Modify: `backend/modules/cart/controller/ground_routes.py`

### Context
- `CartService` uses `self.cart_repository` — add `list_carts_by_owner()` that delegates to `find_by_owner()`.
- `UpdateGroundSchema` validates update fields. Add `owner_user_id` (optional int). Pass through to `validated_data` as-is (no name translation needed — backend Cart model uses `owner_user_id`).
- `_to_ground()` maps cart dict to ground dict. Add `"owner_user_id"` passthrough.
- `ground_routes.py` `list_grounds`: currently checks `current_user["role"] == "ground_owner"` then filters by `region_id`. Replace with owner-level repository call. Keep the rest (non-ground-owner path) the same.

- [ ] **Step 1: Add `list_carts_by_owner()` to CartService**

In `backend/modules/cart/service/cart_service.py`, add after `list_carts()`:

```python
def list_carts_by_owner(self, owner_user_id: int) -> list[dict]:
    """Return all carts owned by the given user. Used for GROUND_OWNER isolation."""
    return self.cart_repository.find_by_owner(owner_user_id)
```

- [ ] **Step 2: Add `owner_user_id` to `UpdateGroundSchema`**

In `backend/modules/cart/schemas/ground_schema.py`, in `UpdateGroundSchema.is_valid()`, add after the `latitude/longitude` block:

```python
if "owner_user_id" in self._data:
    val = self._data["owner_user_id"]
    if val is not None and (not isinstance(val, int) or val <= 0):
        self.errors.append("'owner_user_id' must be a positive integer or null.")
    else:
        self.validated_data["owner_user_id"] = val
```

- [ ] **Step 3: Add `owner_user_id` to `_to_ground()`**

In `_to_ground()`, add after `"updated_at"`:

```python
"owner_user_id": cart.get("owner_user_id"),
```

- [ ] **Step 4: Fix `list_grounds` in ground_routes.py**

Replace the entire `list_grounds` function with:

```python
@router.get("")
def list_grounds(
    region_id: Optional[int] = Query(None, description="Filter by region ID"),
    current_user: dict = Depends(get_current_user),
):
    """List grounds.

    - ground_owner: sees only their owned grounds (DB-level isolation).
    - Other roles: optional region_id filter.
    """
    if current_user["role"] == "ground_owner":
        carts = _cart_service.list_carts_by_owner(current_user["id"])
        return _success([_to_ground(c) for c in carts])

    carts = _cart_service.list_carts()
    grounds = [_to_ground(c) for c in carts]
    if region_id is not None:
        grounds = [g for g in grounds if g.get("location_id") == region_id]
    return _success(grounds)
```

- [ ] **Step 5: Commit**

```
git add backend/modules/cart/service/cart_service.py backend/modules/cart/schemas/ground_schema.py backend/modules/cart/controller/ground_routes.py
git commit -m "feat(backend): ground_owner isolation in grounds route + schema owner_user_id"
```

---

## Task 3: BookingRepository + BookingService + booking_routes.py

**Files:**
- Modify: `backend/modules/booking/repository/booking_repository.py`
- Modify: `backend/modules/booking/service/booking_service.py`
- Modify: `backend/modules/booking/controller/booking_routes.py`

### Context
- `BookingRepository` uses SQLAlchemy. `Booking` model has `cart_id` field (the FK to carts/grounds). `find_by_region_id()` at line 100 is the existing (wrong) ground-owner filter — keep it, just add the correct `find_by_owner()` alongside it.
- `BookingService.list_bookings_by_region()` is called from the route — add a new `list_bookings_by_owner()` alongside it.
- `booking_routes.py` `list_bookings`: the `ground_owner` branch at line 54 uses `list_bookings_by_region`. Replace that branch.

- [ ] **Step 1: Add `find_by_owner()` to BookingRepository**

In `backend/modules/booking/repository/booking_repository.py`, add after `find_by_region_id()`:

```python
def find_by_owner(self, owner_user_id: int, session=None) -> list[dict]:
    """Retrieve all bookings for grounds owned by a specific user.

    Uses a SQL subquery — isolation enforced at DB level, not in Python.
    """
    own_session = session is None
    session = session or self._session_factory()
    try:
        from modules.cart.model.cart_model import Cart as CartModel
        owned_cart_ids = (
            session.query(CartModel.id)
            .filter(CartModel.owner_user_id == owner_user_id)
            .subquery()
        )
        bookings = (
            session.query(Booking)
            .filter(Booking.cart_id.in_(owned_cart_ids))
            .all()
        )
        return [b.to_dict() for b in bookings]
    finally:
        if own_session:
            session.close()
```

- [ ] **Step 2: Write tests for `find_by_owner()`**

Create `backend/modules/booking/tests/test_booking_owner_isolation.py`:

```python
"""Tests for BookingRepository.find_by_owner() — owner-level booking isolation."""
import unittest
from datetime import date
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.booking.model.booking_model import Booking  # noqa: F401
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.booking.repository.booking_repository import BookingRepository
from modules.cart.repository.cart_repository import CartRepository


def _make_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestBookingOwnerIsolation(unittest.TestCase):

    def setUp(self):
        factory = _make_factory()
        self.booking_repo = BookingRepository(session_factory=factory)
        self.cart_repo = CartRepository(session_factory=factory)
        # Create two carts with different owners
        self.cart_a = self.cart_repo.create({"region_id": 1, "cart_type_id": 1, "owner_user_id": 10})
        self.cart_b = self.cart_repo.create({"region_id": 1, "cart_type_id": 1, "owner_user_id": 99})

    def _booking(self, cart_id: int, user_id: int = 1) -> dict:
        return self.booking_repo.create({
            "user_id": user_id,
            "cart_id": cart_id,
            "region_id": 1,
            "cart_type_id": 1,
            "date": str(date.today()),
            "timeslot_id": 1,
            "status": "CONFIRMED",
        })

    def test_find_by_owner_returns_only_bookings_for_owned_carts(self):
        """Only bookings for owned carts are returned."""
        self._booking(self.cart_a["id"])
        self._booking(self.cart_a["id"])
        self._booking(self.cart_b["id"])  # different owner's cart

        results = self.booking_repo.find_by_owner(10)
        self.assertEqual(len(results), 2)
        self.assertTrue(all(b["cart_id"] == self.cart_a["id"] for b in results))

    def test_find_by_owner_returns_empty_if_no_owned_carts(self):
        """Returns [] when the user owns no carts."""
        self._booking(self.cart_a["id"])
        results = self.booking_repo.find_by_owner(999)
        self.assertEqual(results, [])
```

- [ ] **Step 3: Run booking isolation tests**

```
venv\Scripts\python.exe -m pytest backend/modules/booking/tests/test_booking_owner_isolation.py -v
```

Expected: 2 PASSes. If `Booking.create()` needs more fields, check `booking_model.py` and add the minimum required nullable fields.

- [ ] **Step 4: Add `list_bookings_by_owner()` to BookingService**

In `backend/modules/booking/service/booking_service.py`, add after `list_bookings_by_region()`:

```python
def list_bookings_by_owner(self, owner_user_id: int) -> list[dict]:
    """Return all bookings for grounds owned by the given user."""
    return self.booking_repository.find_by_owner(owner_user_id)
```

- [ ] **Step 5: Fix `list_bookings` in booking_routes.py**

In `backend/modules/booking/controller/booking_routes.py`, replace the `ground_owner` branch:

Current:
```python
if role == "ground_owner":
    region_id = current_user.get("region_id")
    if region_id is None:
        return _success([])
    bookings = booking_service.list_bookings_by_region(region_id)
```

New:
```python
if role == "ground_owner":
    bookings = booking_service.list_bookings_by_owner(current_user["id"])
```

- [ ] **Step 6: Run full backend test suite**

```
venv\Scripts\python.exe -m pytest --tb=short -q
```

Expected: all previously-passing tests pass + 5 new tests pass.

- [ ] **Step 7: Commit**

```
git add backend/modules/booking/repository/booking_repository.py backend/modules/booking/service/booking_service.py backend/modules/booking/controller/booking_routes.py backend/modules/booking/tests/test_booking_owner_isolation.py
git commit -m "feat(backend): booking owner isolation — find_by_owner subquery + route fix"
```

---

## Task 4: Admin app — Models + GroundRepository + ApiService

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/GroundRepository.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt`

### Context
- `Ground` data class (line ~193 in Models.kt): add `owner_user_id: Int? = null`
- `UpdateGroundRequest` (line ~207 in Models.kt): currently only `is_active: Boolean?`. Add `owner_user_id: Int? = null`.
- `GroundRepository.kt`: has `getGrounds()` and `toggleGround()`. Add `assignOwner(id, ownerUserId)` that calls `apiService.updateGround(id, UpdateGroundRequest(owner_user_id = ownerUserId))`.
- `ApiService.kt`: `updateGround()` already exists. It takes `UpdateGroundRequest` — once we add `owner_user_id` to that model, the API call automatically includes it.
- Also add `searchUserByPhone(phone)` to `GroundRepository` — reuses `apiService.searchUserByPhone(phone)` which already exists in ApiService. This avoids cross-repository calls in the ViewModel.

- [ ] **Step 1: Update `Ground` in Models.kt**

Find `data class Ground(` and add `val owner_user_id: Int? = null` after `longitude`:

```kotlin
@Serializable
data class Ground(
    val id: Int,
    val name: String,
    val sport_id: Int,
    val location_id: Int,
    val status: String,
    val is_active: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val owner_user_id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
```

- [ ] **Step 2: Update `UpdateGroundRequest` in Models.kt**

```kotlin
@Serializable
data class UpdateGroundRequest(
    val is_active: Boolean? = null,
    val owner_user_id: Int? = null
)
```

- [ ] **Step 3: Add `assignOwner()` and `searchUserByPhone()` to GroundRepository.kt**

```kotlin
suspend fun assignOwner(id: Int, ownerUserId: Int): Ground {
    val response = apiService.updateGround(id, UpdateGroundRequest(owner_user_id = ownerUserId))
    if (response.success && response.data != null) {
        return response.data
    }
    throw Exception(response.message ?: "Failed to assign owner")
}

suspend fun searchUserByPhone(phone: String): AppUser? {
    return try {
        val response = apiService.searchUserByPhone(phone)
        if (response.success) response.data else null
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 404) null
        else throw Exception("Search failed")
    }
}
```

Add import for `AppUser` at the top:
```kotlin
import com.example.vmsadmin.models.AppUser
```

- [ ] **Step 4: Commit**

```
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/GroundRepository.kt
git commit -m "feat(app): Ground + UpdateGroundRequest owner_user_id, GroundRepository assignOwner"
```

---

## Task 5: Admin app — GroundViewModel + GroundsScreen owner assignment UI

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/GroundViewModel.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/GroundsScreen.kt`

### Context
- Read `GroundViewModel.kt` fully before editing. It has `GroundUiState`, `GroundViewModel`, `GroundViewModelFactory`.
- Read `GroundsScreen.kt` fully before editing. It has `GroundsScreen` (top-level) and `GroundCard` composable. `GroundCard` currently shows ground name, status badge, sport/location IDs, and a toggle switch.
- Owner assignment is **SUPER_ADMIN only**. The ViewModel must know the current user's role. Check how other ViewModels get the current role — look at how `currentUserId` is passed in `UserManagementViewModel`. The `GroundViewModel` is instantiated from navigation — check `AppNavigation.kt` for how to pass `currentUserRole`.
- If `currentUserRole` isn't available in the factory, add it as a constructor param (like `currentUserId` in UserManagementViewModel).

#### ViewModel additions

Add to `GroundUiState`:
```kotlin
data class GroundUiState(
    val grounds: List<Ground> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet(),
    // owner search state
    val ownerSearchResult: AppUser? = null,
    val ownerSearchLoading: Boolean = false,
    val ownerSearchError: String? = null,
)
```

Add to `GroundViewModel`:
```kotlin
fun searchOwnerByPhone(phone: String) {
    if (phone.isBlank()) return
    viewModelScope.launch {
        _uiState.update { it.copy(ownerSearchLoading = true, ownerSearchError = null, ownerSearchResult = null) }
        try {
            val user = repository.searchUserByPhone(phone)
            _uiState.update {
                it.copy(
                    ownerSearchLoading = false,
                    ownerSearchResult = user,
                    ownerSearchError = if (user == null) "User not found" else null
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(ownerSearchLoading = false, ownerSearchError = e.message ?: "Search failed") }
        }
    }
}

fun assignOwner(groundId: Int, ownerUserId: Int) {
    viewModelScope.launch {
        _uiState.update { it.copy(updatingIds = it.updatingIds + groundId) }
        try {
            val updated = repository.assignOwner(groundId, ownerUserId)
            _uiState.update { state ->
                state.copy(
                    grounds = state.grounds.map { if (it.id == groundId) updated else it },
                    updatingIds = state.updatingIds - groundId,
                    ownerSearchResult = null,
                    ownerSearchError = null,
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(updatingIds = it.updatingIds - groundId, error = e.message ?: "Failed to assign owner") }
        }
    }
}

fun clearOwnerSearch() {
    _uiState.update { it.copy(ownerSearchResult = null, ownerSearchError = null) }
}
```

#### GroundsScreen additions

Pass `currentUserRole: String` down from the top-level `GroundsScreen` composable into `GroundCard`.

Add "Assign Owner" section to `GroundCard` — visible only when `currentUserRole == "super_admin"`:

```kotlin
if (currentUserRole == "super_admin") {
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    Text("Assign Owner", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    ground.owner_user_id?.let {
        Text("Current owner ID: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = ownerPhone,
            onValueChange = { ownerPhone = it },
            label = { Text("Phone / Name") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onSearchOwner(ownerPhone) }, enabled = ownerPhone.isNotBlank() && !isUpdating) {
            Text("Search")
        }
    }
    ownerSearchError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    foundOwner?.let { user ->
        Spacer(Modifier.height(6.dp))
        Text("Found: ${user.name ?: user.phone}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { onAssignOwner(ground.id, user.id) },
            enabled = !isUpdating
        ) {
            Text("Assign as Owner")
        }
    }
}
```

State in `GroundCard`:
```kotlin
var ownerPhone by remember { mutableStateOf("") }
```

`GroundCard` signature (updated):
```kotlin
@Composable
private fun GroundCard(
    ground: Ground,
    isUpdating: Boolean,
    currentUserRole: String,
    foundOwner: AppUser?,
    ownerSearchError: String?,
    onToggle: (Boolean) -> Unit,
    onSearchOwner: (String) -> Unit,
    onAssignOwner: (Int, Int) -> Unit,
)
```

In `GroundsScreen` top-level, collect owner search state from ViewModel and pass to each `GroundCard`:
```kotlin
val uiState by viewModel.uiState.collectAsState()
// inside LazyColumn items:
GroundCard(
    ground = ground,
    isUpdating = uiState.updatingIds.contains(ground.id),
    currentUserRole = currentUserRole,
    foundOwner = uiState.ownerSearchResult,
    ownerSearchError = uiState.ownerSearchError,
    onToggle = { isActive -> viewModel.toggleGround(ground.id, isActive) },
    onSearchOwner = { phone -> viewModel.searchOwnerByPhone(phone) },
    onAssignOwner = { gId, uId -> viewModel.assignOwner(gId, uId) }
)
```

`GroundsScreen` composable must receive `currentUserRole: String` as a parameter — wire it from `AppNavigation.kt` where the screen is called (pass the logged-in user's role from `AuthViewModel`).

- [ ] **Step 1: Read GroundViewModel.kt fully**

Read `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/GroundViewModel.kt` and note the exact fields in `GroundUiState` and the factory signature.

- [ ] **Step 2: Read AppNavigation.kt to check how currentUserRole is available**

Read `Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt` and find where `GroundsScreen` is called. Note what auth state is available there.

- [ ] **Step 3: Update GroundUiState in GroundViewModel.kt**

Add owner search fields to `GroundUiState` as shown above.

- [ ] **Step 4: Add `searchOwnerByPhone()`, `assignOwner()`, `clearOwnerSearch()` to GroundViewModel**

As shown above. Add `import com.example.vmsadmin.models.AppUser` at the top.

- [ ] **Step 5: Update GroundsScreen.kt — `GroundCard` signature + owner section**

Read the full file first. Then:
1. Add `currentUserRole: String` param to the top-level `GroundsScreen` composable
2. Update `GroundCard` signature with the new params
3. Add the owner assignment section inside `GroundCard` (SUPER_ADMIN only, as shown above)
4. Update the call site in the `LazyColumn` to pass all new params
5. Add `var ownerPhone by remember { mutableStateOf("") }` inside `GroundCard`

- [ ] **Step 6: Wire `currentUserRole` in AppNavigation.kt**

Find the `GroundsScreen(...)` call in `AppNavigation.kt`. Pass `currentUserRole = authState.role` (or equivalent — check what `authState` exposes).

- [ ] **Step 7: Commit**

```
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/GroundViewModel.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/GroundsScreen.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt
git commit -m "feat(app): GroundsScreen owner assignment UI + GroundViewModel owner search"
```

---

## Task 6: Final test run + DEV_LOG

**Files:**
- Modify: `backend/DEV_LOG.md`

- [ ] **Step 1: Run full backend test suite**

```
venv\Scripts\python.exe -m pytest --tb=short -q
```

Expected: all tests pass including 5 new isolation tests.

- [ ] **Step 2: Prepend entry to `backend/DEV_LOG.md`**

Add at the top of the file:

```markdown
---
## [2026-06-04] Phase 02 — Ground Owner data isolation

### Backend
**Added:**
- Migration 7: `owner_user_id INT REFERENCES users(id) ON DELETE SET NULL` on `carts` table
- `CartRepository.find_by_owner(owner_user_id)` — SQL `WHERE owner_user_id = :uid`
- `CartService.list_carts_by_owner(owner_user_id)`
- `BookingRepository.find_by_owner(owner_user_id)` — SQL subquery `WHERE cart_id IN (SELECT id FROM carts WHERE owner_user_id = :uid)`
- `BookingService.list_bookings_by_owner(owner_user_id)`
- `backend/modules/cart/tests/test_cart_owner_isolation.py` — 3 tests
- `backend/modules/booking/tests/test_booking_owner_isolation.py` — 2 tests

**Modified:**
- `cart_model.py` — `owner_user_id` Column + `to_dict()`
- `cart_repository.py` — `create()` accepts `owner_user_id`
- `ground_schema.py` — `UpdateGroundSchema` allows `owner_user_id`; `_to_ground()` passes it through
- `ground_routes.py` — GROUND_OWNER branch uses `list_carts_by_owner()` (DB-level isolation, not Python)
- `booking_routes.py` — GROUND_OWNER branch uses `list_bookings_by_owner()` (replaces region-based filter)

### Admin App
**Modified:**
- `Models.kt` — `Ground.owner_user_id: Int?`, `UpdateGroundRequest.owner_user_id: Int?`
- `GroundRepository.kt` — `assignOwner()`, `searchUserByPhone()`
- `GroundViewModel.kt` — `ownerSearchResult/Loading/Error` in state, `searchOwnerByPhone()`, `assignOwner()`, `clearOwnerSearch()`
- `GroundsScreen.kt` — owner assignment section in `GroundCard` (SUPER_ADMIN only): phone search → found user → assign button
- `AppNavigation.kt` — `currentUserRole` wired into `GroundsScreen`

### Architecture decisions
- Isolation enforced at SQL `WHERE` clause — never in Python loops or route handlers
- `owner_user_id` is nullable: unowned grounds invisible to GROUND_OWNER by default (safe)
- `ON DELETE SET NULL`: deleting a user releases their grounds rather than cascading
- Phone search reuses existing `GET /api/v1/users/search?phone=` — no new endpoint
---
```

- [ ] **Step 3: Commit and push**

```
git add backend/DEV_LOG.md
git commit -m "chore: DEV_LOG Phase 02 ground owner isolation"
git push
```

---

## Self-Review

**Spec coverage:**
- ✅ Migration 7: `owner_user_id` on carts — Task 1
- ✅ Cart model column + `to_dict()` — Task 1
- ✅ `CartRepository.find_by_owner()` SQL filter — Task 1
- ✅ `CartRepository.create()` accepts `owner_user_id` — Task 1
- ✅ `CartService.list_carts_by_owner()` — Task 2
- ✅ `UpdateGroundSchema` allows `owner_user_id` — Task 2
- ✅ `_to_ground()` passes through `owner_user_id` — Task 2
- ✅ `ground_routes.py` uses DB-level filter for GROUND_OWNER — Task 2
- ✅ `BookingRepository.find_by_owner()` subquery — Task 3
- ✅ `BookingService.list_bookings_by_owner()` — Task 3
- ✅ `booking_routes.py` uses owner-based method for GROUND_OWNER — Task 3
- ✅ `Ground.owner_user_id: Int?` in Models.kt — Task 4
- ✅ `UpdateGroundRequest.owner_user_id: Int?` — Task 4
- ✅ `GroundRepository.assignOwner()` — Task 4
- ✅ `GroundRepository.searchUserByPhone()` — Task 4
- ✅ GroundViewModel owner search + assign methods — Task 5
- ✅ GroundsScreen owner assignment UI (SUPER_ADMIN only, phone search) — Task 5
- ✅ `currentUserRole` wired in AppNavigation — Task 5
- ✅ DEV_LOG entry — Task 6

**Placeholder scan:** None found.

**Type consistency:**
- `find_by_owner(owner_user_id: int)` — consistent across Task 1 (repo), Task 2 (service), Task 3 (repo+service)
- `list_carts_by_owner` / `list_bookings_by_owner` — consistent across service + route
- `owner_user_id: Int?` — consistent across `Ground`, `UpdateGroundRequest`, `assignOwner(id, ownerUserId: Int)`
- `AppUser` — used in `GroundRepository.searchUserByPhone()` return type and `GroundUiState.ownerSearchResult`
