# Development Log

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

