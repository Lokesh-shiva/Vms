# Dispute / Ticket System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a dispute/ticket system — backend CRUD module + admin app DisputesScreen. SUPPORT agents raise tickets linked to bookings; OPS_MANAGER and SUPER_ADMIN can view and resolve them.

**Architecture:** New `backend/modules/dispute/` module (same pattern as tournament). Migration 9 creates disputes table. Admin app gets `DisputesScreen` (list + raise dialog + resolve) accessible to SUPPORT, OPS_MANAGER, SUPER_ADMIN. SupportScreen gets a "Raise Ticket" button per booking row.

**Tech Stack:** Python 3.12 / FastAPI / SQLAlchemy; Kotlin / Jetpack Compose / Retrofit

---

## Task 1: Backend dispute module

**Model fields:**
```
id               SERIAL PRIMARY KEY
booking_id       INT REFERENCES bookings(id) ON DELETE SET NULL  (nullable)
user_id          INT REFERENCES users(id) ON DELETE SET NULL      (the user the ticket is about)
raised_by        INT REFERENCES users(id) ON DELETE SET NULL      (the agent who raised it)
title            VARCHAR NOT NULL
description      TEXT NOT NULL
status           VARCHAR(50) NOT NULL DEFAULT 'OPEN'   -- OPEN/IN_PROGRESS/RESOLVED/CLOSED
resolution_note  TEXT  (nullable)
created_at       TIMESTAMP NOT NULL DEFAULT NOW()
updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
```

**Endpoints:**
| Method | Path | Roles |
|--------|------|-------|
| POST | /api/v1/disputes | SUPPORT, OPS_MANAGER, SUPER_ADMIN |
| GET | /api/v1/disputes | SUPPORT, OPS_MANAGER, SUPER_ADMIN |
| GET | /api/v1/disputes/{id} | SUPPORT, OPS_MANAGER, SUPER_ADMIN |
| PUT | /api/v1/disputes/{id} | SUPPORT, OPS_MANAGER, SUPER_ADMIN |

- [ ] **Step 1: Create module structure** — all `__init__.py` files in `backend/modules/dispute/{model,repository,service,schemas,controller,tests}/`

- [ ] **Step 2: Create `backend/modules/dispute/model/dispute_model.py`**

```python
from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text
from core.database.db_connection import Base


class DisputeStatus:
    OPEN = "OPEN"
    IN_PROGRESS = "IN_PROGRESS"
    RESOLVED = "RESOLVED"
    CLOSED = "CLOSED"
    ALL: frozenset[str] = frozenset({OPEN, IN_PROGRESS, RESOLVED, CLOSED})


class Dispute(Base):
    __tablename__ = "disputes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    booking_id = Column(Integer, ForeignKey("bookings.id", ondelete="SET NULL"), nullable=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)
    raised_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    status = Column(String(50), nullable=False, default=DisputeStatus.OPEN)
    resolution_note = Column(Text, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "booking_id": self.booking_id,
            "user_id": self.user_id,
            "raised_by": self.raised_by,
            "title": self.title,
            "description": self.description,
            "status": self.status,
            "resolution_note": self.resolution_note,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<Dispute id={self.id} status={self.status} title={self.title!r}>"
```

- [ ] **Step 3: Create `backend/modules/dispute/repository/dispute_repository.py`**

```python
from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.dispute.model.dispute_model import Dispute


class DisputeRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            d = Dispute(
                booking_id=data.get("booking_id"),
                user_id=data.get("user_id"),
                raised_by=data.get("raised_by"),
                title=data["title"],
                description=data["description"],
                status=data.get("status", "OPEN"),
            )
            session.add(d)
            session.commit()
            session.refresh(d)
            return d.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, dispute_id: int) -> dict | None:
        session = self._session_factory()
        try:
            d = session.query(Dispute).filter(Dispute.id == dispute_id).first()
            return d.to_dict() if d else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        session = self._session_factory()
        try:
            return [d.to_dict() for d in session.query(Dispute).order_by(Dispute.id.desc()).all()]
        finally:
            session.close()

    def update(self, dispute_id: int, data: dict) -> dict | None:
        session = self._session_factory()
        try:
            d = session.query(Dispute).filter(Dispute.id == dispute_id).first()
            if not d:
                return None
            for key, value in data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(d, key):
                    setattr(d, key, value)
            d.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(d)
            return d.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


dispute_repository = DisputeRepository()
```

- [ ] **Step 4: Create service, schema, routes** — follow tournament patterns exactly.

**Service** (`backend/modules/dispute/service/dispute_service.py`):
```python
from modules.dispute.repository.dispute_repository import dispute_repository as _default_repo
from modules.dispute.model.dispute_model import DisputeStatus


class DisputeService:
    def __init__(self, repository=None):
        self.repository = repository or _default_repo

    def list_disputes(self) -> list[dict]:
        return self.repository.find_all()

    def get_dispute(self, dispute_id: int) -> dict | None:
        return self.repository.find_by_id(dispute_id)

    def create_dispute(self, data: dict) -> dict:
        if not data.get("title", "").strip():
            raise ValueError("Title is required.")
        if not data.get("description", "").strip():
            raise ValueError("Description is required.")
        return self.repository.create(data)

    def update_dispute(self, dispute_id: int, data: dict) -> dict:
        existing = self.repository.find_by_id(dispute_id)
        if not existing:
            raise ValueError(f"Dispute {dispute_id} not found.")
        if "status" in data and data["status"] not in DisputeStatus.ALL:
            raise ValueError(f"Invalid status. Must be one of: {DisputeStatus.ALL}")
        return self.repository.update(dispute_id, data)
```

**Schema** (`backend/modules/dispute/schemas/dispute_schema.py`):
```python
class CreateDisputeSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        title = self._data.get("title", "")
        if not isinstance(title, str) or not title.strip():
            self.errors.append("'title' is required.")
        else:
            self.validated_data["title"] = title.strip()

        description = self._data.get("description", "")
        if not isinstance(description, str) or not description.strip():
            self.errors.append("'description' is required.")
        else:
            self.validated_data["description"] = description.strip()

        for field in ("booking_id", "user_id", "raised_by"):
            if field in self._data and self._data[field] is not None:
                val = self._data[field]
                if not isinstance(val, int) or val <= 0:
                    self.errors.append(f"'{field}' must be a positive integer.")
                else:
                    self.validated_data[field] = val

        return len(self.errors) == 0


class UpdateDisputeSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "status" in self._data:
            self.validated_data["status"] = self._data["status"]

        if "resolution_note" in self._data:
            val = self._data["resolution_note"]
            if val is not None and not isinstance(val, str):
                self.errors.append("'resolution_note' must be a string.")
            else:
                self.validated_data["resolution_note"] = val

        return len(self.errors) == 0
```

**Routes** (`backend/modules/dispute/controller/dispute_routes.py`):
```python
from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_role
from modules.dispute.schemas.dispute_schema import CreateDisputeSchema, UpdateDisputeSchema
from modules.dispute.service.dispute_service import DisputeService
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/disputes", tags=["Disputes"])
dispute_service = DisputeService()


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_disputes(
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    return _success(dispute_service.list_disputes())


@router.post("", status_code=201)
def create_dispute(
    request_data: dict,
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    request_data["raised_by"] = current_user["id"]
    schema = CreateDisputeSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        d = dispute_service.create_dispute(schema.validated_data)
        return _success(d, "Dispute raised successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{dispute_id}")
def get_dispute(
    dispute_id: int,
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    d = dispute_service.get_dispute(dispute_id)
    if not d:
        raise HTTPException(status_code=404, detail="Dispute not found.")
    return _success(d)


@router.put("/{dispute_id}")
def update_dispute(
    dispute_id: int,
    request_data: dict,
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    schema = UpdateDisputeSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        d = dispute_service.update_dispute(dispute_id, schema.validated_data)
        return _success(d, "Dispute updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
```

- [ ] **Step 5: Write tests** (`backend/modules/dispute/tests/test_dispute_service.py`):

```python
import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.dispute.model.dispute_model import Dispute  # noqa: F401
from modules.dispute.repository.dispute_repository import DisputeRepository
from modules.dispute.service.dispute_service import DisputeService
import modules.user.model.user_model  # noqa: F401
import modules.booking.model.booking_model  # noqa: F401
import modules.cart.model.cart_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.cart_type.model.cart_type_model  # noqa: F401
import modules.timeslot.model.timeslot_model  # noqa: F401
import modules.match.model.match_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401
import modules.tournament.model.tournament_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestDisputeService(unittest.TestCase):
    def setUp(self):
        repo = DisputeRepository(session_factory=_factory())
        self.service = DisputeService(repository=repo)

    def _base(self, **kwargs):
        base = {"title": "Late booking", "description": "Ground was not available."}
        base.update(kwargs)
        return base

    def test_create_dispute(self):
        d = self.service.create_dispute(self._base())
        self.assertEqual(d["status"], "OPEN")
        self.assertEqual(d["title"], "Late booking")

    def test_list_disputes(self):
        self.service.create_dispute(self._base(title="A"))
        self.service.create_dispute(self._base(title="B"))
        self.assertEqual(len(self.service.list_disputes()), 2)

    def test_update_status(self):
        d = self.service.create_dispute(self._base())
        updated = self.service.update_dispute(d["id"], {"status": "RESOLVED", "resolution_note": "Fixed."})
        self.assertEqual(updated["status"], "RESOLVED")
        self.assertEqual(updated["resolution_note"], "Fixed.")

    def test_create_requires_title(self):
        with self.assertRaises(ValueError):
            self.service.create_dispute({"title": "", "description": "desc"})
```

- [ ] **Step 6: Migration 9 + register in main.py**

Add to `run_migrations.py`:
```python
print("Running migration 9: create disputes table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS disputes (
        id               SERIAL PRIMARY KEY,
        booking_id       INT REFERENCES bookings(id) ON DELETE SET NULL,
        user_id          INT REFERENCES users(id) ON DELETE SET NULL,
        raised_by        INT REFERENCES users(id) ON DELETE SET NULL,
        title            VARCHAR NOT NULL,
        description      TEXT NOT NULL,
        status           VARCHAR(50) NOT NULL DEFAULT 'OPEN',
        resolution_note  TEXT,
        created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")
```

Run migration. Add to `main.py`:
```python
from modules.dispute.controller.dispute_routes import router as dispute_router
from modules.dispute.model.dispute_model import Dispute  # noqa: F401
```
```python
app.include_router(dispute_router)
```

- [ ] **Step 7: Run tests + commit**

```
cd "C:\Users\Lokesh\Desktop\Pojects\Vms project\backend" && ..\venv\Scripts\python.exe -m pytest modules/dispute/tests/ -v
```

4 tests must pass. Then:
```
git add backend/modules/dispute/ backend/run_migrations.py backend/main.py
git commit -m "feat(backend): dispute/ticket module — model, repo, service, routes, migration"
```

---

## Task 2: Admin app — DisputesScreen + SupportScreen raise button

**Files to create:**
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/DisputeRepository.kt`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/DisputeViewModel.kt`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/DisputesScreen.kt`

**Files to modify:**
- `models/Models.kt` — add Dispute, CreateDisputeRequest, UpdateDisputeRequest
- `network/ApiService.kt` — add dispute endpoints
- `AppNavigation.kt` / `MainScreen.kt` — add disputes route
- `SupportScreen.kt` — add "Raise Ticket" button per booking

### Models

```kotlin
@Serializable
data class Dispute(
    val id: Int,
    val booking_id: Int? = null,
    val user_id: Int? = null,
    val raised_by: Int? = null,
    val title: String,
    val description: String,
    val status: String = "OPEN",
    val resolution_note: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateDisputeRequest(
    val title: String,
    val description: String,
    val booking_id: Int? = null,
    val user_id: Int? = null
)

@Serializable
data class UpdateDisputeRequest(
    val status: String? = null,
    val resolution_note: String? = null
)
```

### ApiService

```kotlin
@GET("disputes")
suspend fun getDisputes(): ApiResponse<List<Dispute>>

@POST("disputes")
suspend fun createDispute(@Body request: CreateDisputeRequest): ApiResponse<Dispute>

@PUT("disputes/{id}")
suspend fun updateDispute(
    @Path("id") id: Int,
    @Body request: UpdateDisputeRequest
): ApiResponse<Dispute>
```

### DisputeRepository

```kotlin
class DisputeRepository(private val apiService: ApiService) {
    suspend fun getDisputes(): List<Dispute> { ... }
    suspend fun createDispute(request: CreateDisputeRequest): Dispute { ... }
    suspend fun updateDispute(id: Int, request: UpdateDisputeRequest): Dispute { ... }
}
```

### DisputeViewModel

```kotlin
data class DisputeUiState(
    val disputes: List<Dispute> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet(),
)

class DisputeViewModel(private val repository: DisputeRepository) : ViewModel() {
    // loadDisputes(), createDispute(request), updateStatus(id, status, note)
}
```

### DisputesScreen

- TopAppBar "Disputes" + back
- LazyColumn of DisputeCards
- Each card: title, description, booking_id, status badge, resolution_note if resolved
- "Resolve" button on OPEN/IN_PROGRESS disputes → opens a small dialog for resolution_note + status change
- Shimmer skeleton + error/retry

### SupportScreen update

Read `SupportScreen.kt`. In the booking list section, add a "Raise Ticket" icon/button per booking row. On tap → opens a small dialog with title + description fields → calls `disputeViewModel.createDispute(...)`.

This requires passing `DisputeViewModel` into `SupportScreen`. Read AppNavigation to see how SupportScreen is currently called, then add `disputeViewModel` param.

### Navigation

Add `"disputes"` route accessible to SUPPORT, OPS_MANAGER, SUPER_ADMIN. Add "Disputes" entry to ManageScreen or as a separate nav item — check existing structure and add where logical (under Operations section makes sense).

- [ ] **Step 1: Add models to Models.kt**
- [ ] **Step 2: Add API methods to ApiService.kt**
- [ ] **Step 3: Create DisputeRepository.kt**
- [ ] **Step 4: Create DisputeViewModel.kt**
- [ ] **Step 5: Create DisputesScreen.kt**
- [ ] **Step 6: Update SupportScreen.kt with Raise Ticket button**
- [ ] **Step 7: Wire navigation**
- [ ] **Step 8: Commit**

```
git add Vmsadminapp/...
git commit -m "feat(app): DisputesScreen + SupportScreen raise ticket + navigation"
```

---

## Task 3: DEV_LOG + push

```markdown
---
## [2026-06-04] Phase 02 — Dispute/ticket system

### Backend
**Added:**
- Migration 9: `disputes` table
- Full CRUD module: `backend/modules/dispute/` (OPEN/IN_PROGRESS/RESOLVED/CLOSED)
- `GET/POST /api/v1/disputes`, `GET/PUT /api/v1/disputes/{id}`
- Role guards: SUPPORT, OPS_MANAGER, SUPER_ADMIN
- `raised_by` auto-populated from JWT on create
- 4 tests

**Modified:**
- `backend/main.py`, `backend/run_migrations.py`

### Admin App
**Added:**
- Dispute, CreateDisputeRequest, UpdateDisputeRequest models
- DisputeRepository, DisputeViewModel, DisputesScreen
- "Raise Ticket" button in SupportScreen per booking

**Modified:**
- ApiService.kt — dispute endpoints
- AppNavigation + MainScreen — disputes route
- SupportScreen — raise ticket button + DisputeViewModel param

### Architecture decisions
- `raised_by` set server-side from JWT, not trusted from client
- Disputes navigable from Support role panel
---
```
