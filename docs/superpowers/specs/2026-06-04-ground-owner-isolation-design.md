# Ground Owner Data Isolation — Design Spec

**Date:** 2026-06-04
**Phase:** 02
**Feature:** Repository-level ground ownership scoping + owner assignment UI

---

## Goal

A GROUND_OWNER user sees only the grounds they own and only the bookings for those grounds. Isolation is enforced at the SQL query layer, not in Python or at the route layer. A SUPER_ADMIN can assign grounds to owners via phone-number search in the admin app.

---

## Current state (what's wrong)

- No `owner_user_id` column on the `carts` (grounds) table — ownership doesn't exist in DB
- `GET /api/v1/grounds` filters by `current_user["region_id"]` in Python — region-scoped, not owner-scoped
- `GET /api/v1/bookings` calls `list_bookings_by_region(region_id)` — same problem
- Filtering happens at the route layer, violating the hard rule: *"Ground Owner data isolation enforced at repository/query level, not just route filtering"*

---

## Architecture

```
GROUND_OWNER login
      ↓
GET /api/v1/grounds
      ↓ passes owner_user_id=current_user["id"]
CartRepository.find_by_owner(owner_user_id)
      ↓ WHERE carts.owner_user_id = :uid
DB → only owned grounds returned

GET /api/v1/bookings
      ↓ passes owner_user_id=current_user["id"]
BookingRepository.find_by_owner(owner_user_id)
      ↓ WHERE cart_id IN (SELECT id FROM carts WHERE owner_user_id = :uid)
DB → only bookings for owned grounds returned

SUPER_ADMIN assigns owner:
Phone search → GET /api/v1/users/search?phone=
Confirm user → PUT /api/v1/grounds/{id} with {"owner_user_id": user_id}
```

---

## Backend changes

### Migration 7: `run_migrations.py`
```python
cur.execute("""
    ALTER TABLE carts
        ADD COLUMN IF NOT EXISTS owner_user_id INT REFERENCES users(id) ON DELETE SET NULL;
""")
```

### Cart model: `backend/modules/cart/model/cart_model.py`
Add column:
```python
owner_user_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
```

Add to `to_dict()`:
```python
"owner_user_id": self.owner_user_id,
```

### CartRepository: `backend/modules/cart/repository/cart_repository.py`
Add method after `find_all()`:
```python
def find_by_owner(self, owner_user_id: int, session=None) -> list[dict]:
    """Retrieve all carts owned by a specific user. Used for GROUND_OWNER isolation."""
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

Also update `create()` to accept `owner_user_id`:
```python
cart = Cart(
    label=cart_data.get("label", ""),
    region_id=cart_data.get("region_id"),
    cart_type_id=cart_data.get("cart_type_id"),
    status=cart_data.get("status", "AVAILABLE"),
    is_active=cart_data.get("is_active", True),
    owner_user_id=cart_data.get("owner_user_id"),  # add this line
)
```

### BookingRepository: `backend/modules/booking/repository/booking_repository.py`
Add method after `find_by_region_id()`:
```python
def find_by_owner(self, owner_user_id: int, session=None) -> list[dict]:
    """Retrieve all bookings for grounds owned by a specific user.

    SQL-level subquery — enforces isolation at the DB layer.
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

### BookingService: `backend/modules/booking/service/booking_service.py`
Add method:
```python
def list_bookings_by_owner(self, owner_user_id: int) -> list[dict]:
    """Retrieve bookings for all grounds owned by the given user."""
    return self.booking_repository.find_by_owner(owner_user_id)
```

### CartService: `backend/modules/cart/service/cart_service.py`
Add method:
```python
def list_carts_by_owner(self, owner_user_id: int) -> list[dict]:
    """Retrieve all carts/grounds owned by a specific user."""
    return self.cart_repository.find_by_owner(owner_user_id)
```

### ground_routes.py: `backend/modules/cart/controller/ground_routes.py`
Replace the `list_grounds` Python-side filter with repository-level call:

Current (wrong):
```python
if current_user["role"] == "ground_owner":
    effective_region = current_user.get("region_id")
    ...
carts = _cart_service.list_carts()
grounds = [_to_ground(c) for c in carts]
if effective_region is not None:
    grounds = [g for g in grounds if g.get("location_id") == effective_region]
```

New (correct):
```python
if current_user["role"] == "ground_owner":
    carts = _cart_service.list_carts_by_owner(current_user["id"])
    return _success([_to_ground(c) for c in carts])

# Other roles: optional region_id filter
carts = _cart_service.list_carts()
grounds = [_to_ground(c) for c in carts]
if region_id is not None:
    grounds = [g for g in grounds if g.get("location_id") == region_id]
return _success(grounds)
```

Also update `update_ground` to allow `owner_user_id` in request (SUPER_ADMIN only — already guarded by `require_admin`):
- `UpdateGroundSchema` should pass through `owner_user_id` if present.

### booking_routes.py: `backend/modules/booking/controller/booking_routes.py`
Replace region-based filter with owner-based:

Current (wrong):
```python
if role == "ground_owner":
    region_id = current_user.get("region_id")
    if region_id is None:
        return _success([])
    bookings = booking_service.list_bookings_by_region(region_id)
```

New (correct):
```python
if role == "ground_owner":
    bookings = booking_service.list_bookings_by_owner(current_user["id"])
```

### UpdateGroundSchema: `backend/modules/cart/schemas/ground_schema.py`
Allow `owner_user_id` (int, optional) in the update schema. Pass it through to `validated_data`.

---

## Admin app changes

### Models.kt
Add `owner_user_id: Int? = null` to `Ground` data class.

### GroundsScreen.kt — owner assignment (SUPER_ADMIN only)
Add to the ground edit/update dialog:
- "Assign Owner" section (visible only when `currentUserRole == "super_admin"`)
- Phone search field → calls existing `GET /api/v1/users/search?phone=`
- Shows found user's name + phone on success
- On confirm → PUT includes `owner_user_id`

The phone search reuses the existing `UserManagementRepository.searchByPhone()` call pattern.

**State in the dialog:**
```kotlin
var ownerPhone by remember { mutableStateOf("") }
var foundOwner by remember { mutableStateOf<AppUser?>(null) }
var ownerSearchError by remember { mutableStateOf<String?>(null) }
```

**Confirm button** sends `owner_user_id = foundOwner?.id` alongside other update fields.

### ApiService.kt / GroundRepository
`updateGround()` already exists — just ensure `owner_user_id: Int?` is included in the update request body (`UpdateGroundRequest` model in `Models.kt`).

---

## Data isolation guarantee

| Scenario | Isolation mechanism |
|----------|-------------------|
| GROUND_OWNER lists grounds | `CartRepository.find_by_owner(uid)` — SQL `WHERE owner_user_id = :uid` |
| GROUND_OWNER lists bookings | `BookingRepository.find_by_owner(uid)` — SQL `WHERE cart_id IN (SELECT id FROM carts WHERE owner_user_id = :uid)` |
| GROUND_OWNER fetches booking by ID | Existing route guard: `booking["cart_id"]` must belong to owned ground (add check) |
| SUPER_ADMIN assigns owner | `PUT /api/v1/grounds/{id}` with `owner_user_id` |
| Non-owner tries to guess ground ID | Route returns 403 for GROUND_OWNER accessing unowned ground by ID |

---

## Testing

**Backend:**
- `CartRepository.find_by_owner(uid)` returns only owned carts
- `BookingRepository.find_by_owner(uid)` returns only bookings for owned carts
- `GET /api/v1/grounds` as GROUND_OWNER → only their grounds
- `GET /api/v1/bookings` as GROUND_OWNER → only their bookings
- `PUT /api/v1/grounds/{id}` with `owner_user_id` → assigns owner

**App (manual):**
- SUPER_ADMIN: edits a ground → phone search → assign owner
- GROUND_OWNER login: Grounds tab shows only assigned grounds
- GROUND_OWNER login: Bookings tab shows only bookings for their grounds

---

## Files changed

**Backend:**
- `backend/run_migrations.py` — Migration 7
- `backend/modules/cart/model/cart_model.py` — `owner_user_id` column
- `backend/modules/cart/repository/cart_repository.py` — `find_by_owner()`, update `create()`
- `backend/modules/cart/service/cart_service.py` — `list_carts_by_owner()`
- `backend/modules/cart/schemas/ground_schema.py` — allow `owner_user_id` in UpdateGroundSchema
- `backend/modules/cart/controller/ground_routes.py` — owner-based filtering
- `backend/modules/booking/repository/booking_repository.py` — `find_by_owner()`
- `backend/modules/booking/service/booking_service.py` — `list_bookings_by_owner()`
- `backend/modules/booking/controller/booking_routes.py` — use owner-based method

**Admin app:**
- `models/Models.kt` — `owner_user_id` on `Ground`, update `UpdateGroundRequest`
- `ui/screens/GroundsScreen.kt` — owner assignment UI in edit dialog (SUPER_ADMIN only)

---

## Constraints

- Isolation at SQL level only — no Python-side filtering for GROUND_OWNER
- `owner_user_id` is nullable — existing grounds without an owner return nothing for GROUND_OWNER login (safe default)
- `ON DELETE SET NULL` — deleting a user clears ground ownership rather than deleting the ground
- Parameterised queries only — no f-string SQL
- `require_admin` already guards `PUT /api/v1/grounds/{id}` — no new auth layer needed
