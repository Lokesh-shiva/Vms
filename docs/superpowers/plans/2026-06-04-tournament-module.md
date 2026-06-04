# Tournament Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete tournament module — backend CRUD + admin app screen with create/list/status-update, accessible to TOURNAMENT_MANAGER and SUPER_ADMIN.

**Architecture:** New `backend/modules/tournament/` module following the exact captain module pattern (model → repository → service → schema → routes → register in main.py). Migration 8 creates the tournaments table. Admin app gets `TournamentsScreen` (list + create dialog + status update) wired via navigation.

**Tech Stack:** Python 3.12 / FastAPI / SQLAlchemy / psycopg2; Kotlin / Jetpack Compose / Retrofit

---

## Task 1: Backend tournament module (full vertical slice)

**Files to create:**
- `backend/modules/tournament/__init__.py`
- `backend/modules/tournament/model/__init__.py`
- `backend/modules/tournament/model/tournament_model.py`
- `backend/modules/tournament/repository/__init__.py`
- `backend/modules/tournament/repository/tournament_repository.py`
- `backend/modules/tournament/service/__init__.py`
- `backend/modules/tournament/service/tournament_service.py`
- `backend/modules/tournament/schemas/__init__.py`
- `backend/modules/tournament/schemas/tournament_schema.py`
- `backend/modules/tournament/controller/__init__.py`
- `backend/modules/tournament/controller/tournament_routes.py`
- `backend/modules/tournament/tests/__init__.py`
- `backend/modules/tournament/tests/test_tournament_service.py`

**Files to modify:**
- `backend/run_migrations.py` — Migration 8
- `backend/main.py` — register tournament router + model

### Context
- Follow captain module pattern exactly. See `backend/modules/captain/` for all patterns.
- `require_role` from `modules.auth.dependencies.auth_dependencies`
- `UserRole` from `modules.user.model.user_model`
- `Base` from `core.database.db_connection`
- `SessionLocal` from `core.database.db_connection`
- Run tests from `backend/` directory: `cd backend && ..\venv\Scripts\python.exe -m pytest modules/tournament/tests/ -v`

### Tournament model fields
```
id          SERIAL PRIMARY KEY
name        VARCHAR NOT NULL
sport_id    INT FK → sports.id ON DELETE SET NULL
region_id   INT FK → locations.id ON DELETE SET NULL
organizer   VARCHAR NOT NULL
start_date  DATE NOT NULL
end_date    DATE NOT NULL
max_teams   INT NOT NULL DEFAULT 8
status      VARCHAR NOT NULL DEFAULT 'UPCOMING'  -- UPCOMING/ONGOING/COMPLETED/CANCELLED
created_at  TIMESTAMP NOT NULL DEFAULT NOW()
updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
```

### CRUD endpoints
| Method | Path | Roles |
|--------|------|-------|
| POST | /api/v1/tournaments | TOURNAMENT_MANAGER, SUPER_ADMIN |
| GET | /api/v1/tournaments | TOURNAMENT_MANAGER, OPS_MANAGER, SUPER_ADMIN |
| GET | /api/v1/tournaments/{id} | TOURNAMENT_MANAGER, OPS_MANAGER, SUPER_ADMIN |
| PUT | /api/v1/tournaments/{id} | TOURNAMENT_MANAGER, SUPER_ADMIN |
| DELETE | /api/v1/tournaments/{id} | SUPER_ADMIN only |

- [ ] **Step 1: Create all `__init__.py` files**

Create empty files:
- `backend/modules/tournament/__init__.py`
- `backend/modules/tournament/model/__init__.py`
- `backend/modules/tournament/repository/__init__.py`
- `backend/modules/tournament/service/__init__.py`
- `backend/modules/tournament/schemas/__init__.py`
- `backend/modules/tournament/controller/__init__.py`
- `backend/modules/tournament/tests/__init__.py`

- [ ] **Step 2: Create tournament_model.py**

```python
from datetime import date, datetime
from sqlalchemy import Column, Date, DateTime, ForeignKey, Integer, String
from core.database.db_connection import Base


class TournamentStatus:
    UPCOMING = "UPCOMING"
    ONGOING = "ONGOING"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    ALL: frozenset[str] = frozenset({UPCOMING, ONGOING, COMPLETED, CANCELLED})


class Tournament(Base):
    """SQLAlchemy model for the 'tournaments' table."""

    __tablename__ = "tournaments"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, nullable=False)
    sport_id = Column(Integer, ForeignKey("sports.id", ondelete="SET NULL"), nullable=True, index=True)
    region_id = Column(Integer, ForeignKey("locations.id", ondelete="SET NULL"), nullable=True, index=True)
    organizer = Column(String, nullable=False)
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False)
    max_teams = Column(Integer, nullable=False, default=8)
    status = Column(String(50), nullable=False, default=TournamentStatus.UPCOMING)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "sport_id": self.sport_id,
            "region_id": self.region_id,
            "organizer": self.organizer,
            "start_date": self.start_date.isoformat() if self.start_date else None,
            "end_date": self.end_date.isoformat() if self.end_date else None,
            "max_teams": self.max_teams,
            "status": self.status,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self) -> str:
        return f"<Tournament id={self.id} name={self.name} status={self.status}>"
```

- [ ] **Step 3: Create tournament_repository.py**

```python
from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.tournament.model.tournament_model import Tournament


class TournamentRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            t = Tournament(
                name=data["name"],
                sport_id=data.get("sport_id"),
                region_id=data.get("region_id"),
                organizer=data["organizer"],
                start_date=data["start_date"],
                end_date=data["end_date"],
                max_teams=data.get("max_teams", 8),
                status=data.get("status", "UPCOMING"),
            )
            session.add(t)
            session.commit()
            session.refresh(t)
            return t.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, tournament_id: int) -> dict | None:
        session = self._session_factory()
        try:
            t = session.query(Tournament).filter(Tournament.id == tournament_id).first()
            return t.to_dict() if t else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        session = self._session_factory()
        try:
            return [t.to_dict() for t in session.query(Tournament).order_by(Tournament.id.desc()).all()]
        finally:
            session.close()

    def update(self, tournament_id: int, data: dict) -> dict | None:
        session = self._session_factory()
        try:
            t = session.query(Tournament).filter(Tournament.id == tournament_id).first()
            if not t:
                return None
            for key, value in data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(t, key):
                    setattr(t, key, value)
            t.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(t)
            return t.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, tournament_id: int) -> bool:
        session = self._session_factory()
        try:
            t = session.query(Tournament).filter(Tournament.id == tournament_id).first()
            if not t:
                return False
            session.delete(t)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


tournament_repository = TournamentRepository()
```

- [ ] **Step 4: Create tournament_service.py**

```python
from modules.tournament.repository.tournament_repository import (
    tournament_repository as _default_repo,
)
from modules.tournament.model.tournament_model import TournamentStatus


class TournamentService:
    def __init__(self, repository=None):
        self.repository = repository or _default_repo

    def list_tournaments(self) -> list[dict]:
        return self.repository.find_all()

    def get_tournament(self, tournament_id: int) -> dict | None:
        return self.repository.find_by_id(tournament_id)

    def create_tournament(self, data: dict) -> dict:
        if not data.get("name", "").strip():
            raise ValueError("Tournament name is required.")
        if not data.get("organizer", "").strip():
            raise ValueError("Organizer is required.")
        if not data.get("start_date"):
            raise ValueError("start_date is required.")
        if not data.get("end_date"):
            raise ValueError("end_date is required.")
        if data["start_date"] > data["end_date"]:
            raise ValueError("start_date must be before end_date.")
        return self.repository.create(data)

    def update_tournament(self, tournament_id: int, data: dict) -> dict:
        existing = self.repository.find_by_id(tournament_id)
        if not existing:
            raise ValueError(f"Tournament {tournament_id} not found.")
        if "status" in data and data["status"] not in TournamentStatus.ALL:
            raise ValueError(f"Invalid status. Must be one of: {TournamentStatus.ALL}")
        return self.repository.update(tournament_id, data)

    def delete_tournament(self, tournament_id: int) -> bool:
        existing = self.repository.find_by_id(tournament_id)
        if not existing:
            raise ValueError(f"Tournament {tournament_id} not found.")
        return self.repository.delete(tournament_id)
```

- [ ] **Step 5: Create tournament_schema.py**

```python
from datetime import date


class CreateTournamentSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        name = self._data.get("name", "")
        if not isinstance(name, str) or not name.strip():
            self.errors.append("'name' is required.")
        else:
            self.validated_data["name"] = name.strip()

        organizer = self._data.get("organizer", "")
        if not isinstance(organizer, str) or not organizer.strip():
            self.errors.append("'organizer' is required.")
        else:
            self.validated_data["organizer"] = organizer.strip()

        for field in ("start_date", "end_date"):
            val = self._data.get(field)
            if not val:
                self.errors.append(f"'{field}' is required.")
            else:
                try:
                    self.validated_data[field] = date.fromisoformat(str(val))
                except (ValueError, TypeError):
                    self.errors.append(f"'{field}' must be a valid date (YYYY-MM-DD).")

        for field in ("sport_id", "region_id"):
            if field in self._data and self._data[field] is not None:
                val = self._data[field]
                if not isinstance(val, int) or val <= 0:
                    self.errors.append(f"'{field}' must be a positive integer.")
                else:
                    self.validated_data[field] = val

        max_teams = self._data.get("max_teams", 8)
        if not isinstance(max_teams, int) or max_teams < 2:
            self.errors.append("'max_teams' must be an integer ≥ 2.")
        else:
            self.validated_data["max_teams"] = max_teams

        return len(self.errors) == 0


class UpdateTournamentSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "name" in self._data:
            if not isinstance(self._data["name"], str) or not self._data["name"].strip():
                self.errors.append("'name' must be a non-empty string.")
            else:
                self.validated_data["name"] = self._data["name"].strip()

        if "organizer" in self._data:
            if not isinstance(self._data["organizer"], str) or not self._data["organizer"].strip():
                self.errors.append("'organizer' must be a non-empty string.")
            else:
                self.validated_data["organizer"] = self._data["organizer"].strip()

        for field in ("start_date", "end_date"):
            if field in self._data:
                try:
                    self.validated_data[field] = date.fromisoformat(str(self._data[field]))
                except (ValueError, TypeError):
                    self.errors.append(f"'{field}' must be a valid date (YYYY-MM-DD).")

        if "status" in self._data:
            self.validated_data["status"] = self._data["status"]

        if "max_teams" in self._data:
            val = self._data["max_teams"]
            if not isinstance(val, int) or val < 2:
                self.errors.append("'max_teams' must be an integer ≥ 2.")
            else:
                self.validated_data["max_teams"] = val

        return len(self.errors) == 0
```

- [ ] **Step 6: Create tournament_routes.py**

```python
from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_role
from modules.tournament.schemas.tournament_schema import CreateTournamentSchema, UpdateTournamentSchema
from modules.tournament.service.tournament_service import TournamentService
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/tournaments", tags=["Tournaments"])
tournament_service = TournamentService()


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def list_tournaments(
    current_user: dict = require_role(
        UserRole.TOURNAMENT_MANAGER, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN
    ),
):
    """List all tournaments."""
    return _success(tournament_service.list_tournaments())


@router.post("", status_code=201)
def create_tournament(
    request_data: dict,
    current_user: dict = require_role(UserRole.TOURNAMENT_MANAGER, UserRole.SUPER_ADMIN),
):
    """Create a new tournament."""
    schema = CreateTournamentSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        tournament = tournament_service.create_tournament(schema.validated_data)
        return _success(tournament, "Tournament created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{tournament_id}")
def get_tournament(
    tournament_id: int,
    current_user: dict = require_role(
        UserRole.TOURNAMENT_MANAGER, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN
    ),
):
    """Get a tournament by ID."""
    tournament = tournament_service.get_tournament(tournament_id)
    if not tournament:
        raise HTTPException(status_code=404, detail="Tournament not found.")
    return _success(tournament)


@router.put("/{tournament_id}")
def update_tournament(
    tournament_id: int,
    request_data: dict,
    current_user: dict = require_role(UserRole.TOURNAMENT_MANAGER, UserRole.SUPER_ADMIN),
):
    """Update a tournament."""
    schema = UpdateTournamentSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        tournament = tournament_service.update_tournament(tournament_id, schema.validated_data)
        return _success(tournament, "Tournament updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.delete("/{tournament_id}")
def delete_tournament(
    tournament_id: int,
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    """Delete a tournament. SUPER_ADMIN only."""
    try:
        tournament_service.delete_tournament(tournament_id)
        return _success(None, "Tournament deleted successfully.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
```

- [ ] **Step 7: Write tests**

```python
"""Tests for TournamentService (SQLite-backed)."""
import unittest
from datetime import date
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.tournament.model.tournament_model import Tournament  # noqa: F401
from modules.tournament.repository.tournament_repository import TournamentRepository
from modules.tournament.service.tournament_service import TournamentService
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


def _valid_data(**kwargs):
    base = {
        "name": "Test Cup",
        "organizer": "Plixo",
        "start_date": date(2026, 7, 1),
        "end_date": date(2026, 7, 10),
        "max_teams": 8,
    }
    base.update(kwargs)
    return base


class TestTournamentService(unittest.TestCase):
    def setUp(self):
        repo = TournamentRepository(session_factory=_factory())
        self.service = TournamentService(repository=repo)

    def test_create_tournament(self):
        t = self.service.create_tournament(_valid_data())
        self.assertEqual(t["name"], "Test Cup")
        self.assertEqual(t["status"], "UPCOMING")

    def test_list_tournaments(self):
        self.service.create_tournament(_valid_data(name="A"))
        self.service.create_tournament(_valid_data(name="B"))
        results = self.service.list_tournaments()
        self.assertEqual(len(results), 2)

    def test_update_status(self):
        t = self.service.create_tournament(_valid_data())
        updated = self.service.update_tournament(t["id"], {"status": "ONGOING"})
        self.assertEqual(updated["status"], "ONGOING")

    def test_delete_tournament(self):
        t = self.service.create_tournament(_valid_data())
        self.assertTrue(self.service.delete_tournament(t["id"]))
        self.assertIsNone(self.service.get_tournament(t["id"]))

    def test_create_validates_dates(self):
        with self.assertRaises(ValueError):
            self.service.create_tournament(_valid_data(
                start_date=date(2026, 7, 10),
                end_date=date(2026, 7, 1),
            ))
```

- [ ] **Step 8: Add Migration 8 to run_migrations.py**

Before `conn.commit()`:
```python
print("Running migration 8: create tournaments table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS tournaments (
        id          SERIAL PRIMARY KEY,
        name        VARCHAR NOT NULL,
        sport_id    INT REFERENCES sports(id) ON DELETE SET NULL,
        region_id   INT REFERENCES locations(id) ON DELETE SET NULL,
        organizer   VARCHAR NOT NULL,
        start_date  DATE NOT NULL,
        end_date    DATE NOT NULL,
        max_teams   INT NOT NULL DEFAULT 8,
        status      VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
        created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")
```

Run: `venv\Scripts\python.exe backend/run_migrations.py`

- [ ] **Step 9: Register in main.py**

Add import:
```python
from modules.tournament.controller.tournament_routes import router as tournament_router
from modules.tournament.model.tournament_model import Tournament  # noqa: F401
```

Add to `app.include_router(...)` calls (follow existing pattern):
```python
app.include_router(tournament_router)
```

- [ ] **Step 10: Run tests**

```
cd "C:\Users\Lokesh\Desktop\Pojects\Vms project\backend" && ..\venv\Scripts\python.exe -m pytest modules/tournament/tests/test_tournament_service.py -v
```

Expected: 5 PASSes.

- [ ] **Step 11: Commit**

```
git add backend/modules/tournament/ backend/run_migrations.py backend/main.py
git commit -m "feat(backend): tournament module — model, repo, service, routes, migration"
```

---

## Task 2: Admin app — Tournament screen

**Files to create:**
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/TournamentRepository.kt`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/TournamentViewModel.kt`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/TournamentsScreen.kt`

**Files to modify:**
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt`
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt`

### Context
- Follow CaptainScreen / CaptainViewModel / CaptainRepository patterns
- `ApiResponse<T>` wrapper used throughout
- Navigation adds `TournamentsScreen` accessible to TOURNAMENT_MANAGER and SUPER_ADMIN
- Read AppNavigation.kt to find where to add the route and how other screens are wired

### Models.kt additions

Add near other domain models:
```kotlin
@Serializable
data class Tournament(
    val id: Int,
    val name: String,
    val sport_id: Int? = null,
    val region_id: Int? = null,
    val organizer: String,
    val start_date: String,
    val end_date: String,
    val max_teams: Int = 8,
    val status: String = "UPCOMING",
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateTournamentRequest(
    val name: String,
    val organizer: String,
    val start_date: String,
    val end_date: String,
    val max_teams: Int = 8,
    val sport_id: Int? = null,
    val region_id: Int? = null
)

@Serializable
data class UpdateTournamentRequest(
    val status: String? = null,
    val name: String? = null,
    val organizer: String? = null
)
```

### ApiService.kt additions

```kotlin
@GET("tournaments")
suspend fun getTournaments(): ApiResponse<List<Tournament>>

@POST("tournaments")
suspend fun createTournament(@Body request: CreateTournamentRequest): ApiResponse<Tournament>

@PUT("tournaments/{id}")
suspend fun updateTournament(
    @Path("id") id: Int,
    @Body request: UpdateTournamentRequest
): ApiResponse<Tournament>

@DELETE("tournaments/{id}")
suspend fun deleteTournament(@Path("id") id: Int): ApiResponse<JsonElement>
```

### TournamentRepository.kt

```kotlin
package com.example.vmsadmin.data

import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.UpdateTournamentRequest
import com.example.vmsadmin.network.ApiService

class TournamentRepository(private val apiService: ApiService) {

    suspend fun getTournaments(): List<Tournament> {
        val response = apiService.getTournaments()
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to fetch tournaments")
    }

    suspend fun createTournament(request: CreateTournamentRequest): Tournament {
        val response = apiService.createTournament(request)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to create tournament")
    }

    suspend fun updateTournament(id: Int, request: UpdateTournamentRequest): Tournament {
        val response = apiService.updateTournament(id, request)
        if (response.success && response.data != null) return response.data
        throw Exception(response.message ?: "Failed to update tournament")
    }

    suspend fun deleteTournament(id: Int) {
        val response = apiService.deleteTournament(id)
        if (!response.success) throw Exception(response.message ?: "Failed to delete tournament")
    }
}
```

### TournamentViewModel.kt

```kotlin
package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TournamentRepository
import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.models.UpdateTournamentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TournamentUiState(
    val tournaments: List<Tournament> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet(),
)

class TournamentViewModel(private val repository: TournamentRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentUiState())
    val uiState: StateFlow<TournamentUiState> = _uiState.asStateFlow()

    init { loadTournaments() }

    fun loadTournaments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(
                    tournaments = repository.getTournaments(), isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun refreshTournaments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                _uiState.value = _uiState.value.copy(
                    tournaments = repository.getTournaments(), isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, error = e.message)
            }
        }
    }

    fun createTournament(request: CreateTournamentRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.createTournament(request)
                loadTournaments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to create tournament")
            }
        }
    }

    fun updateStatus(id: Int, status: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds + id)
            try {
                val updated = repository.updateTournament(id, UpdateTournamentRequest(status = status))
                _uiState.value = _uiState.value.copy(
                    tournaments = _uiState.value.tournaments.map { if (it.id == id) updated else it },
                    updatingIds = _uiState.value.updatingIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to update tournament"
                )
            }
        }
    }
}

class TournamentViewModelFactory(
    private val repository: TournamentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TournamentViewModel(repository) as T
    }
}
```

### TournamentsScreen.kt

Build a screen similar to CaptainScreen/GroundsScreen:
- TopAppBar "Tournaments" with back button
- FAB to open create dialog (TOURNAMENT_MANAGER + SUPER_ADMIN)
- LazyColumn of TournamentCard composables
- Each card shows: name, organizer, dates, status badge, max_teams
- Status dropdown chip on each card to change status (UPCOMING/ONGOING/COMPLETED/CANCELLED)
- Create dialog: fields for name, organizer, start_date (YYYY-MM-DD), end_date (YYYY-MM-DD), max_teams
- Shimmer skeleton while loading
- Pull-to-refresh
- Error + retry state

Key composables:
```kotlin
@Composable
fun TournamentsScreen(viewModel: TournamentViewModel, onBack: () -> Unit)

@Composable
private fun TournamentCard(
    tournament: Tournament,
    isUpdating: Boolean,
    onStatusChange: (String) -> Unit
)

@Composable
private fun CreateTournamentDialog(
    onConfirm: (CreateTournamentRequest) -> Unit,
    onDismiss: () -> Unit
)
```

- [ ] **Step 1: Add models to Models.kt**
- [ ] **Step 2: Add API methods to ApiService.kt**
- [ ] **Step 3: Create TournamentRepository.kt**
- [ ] **Step 4: Create TournamentViewModel.kt**
- [ ] **Step 5: Create TournamentsScreen.kt**
- [ ] **Step 6: Wire in AppNavigation.kt**

Read AppNavigation.kt. Find how other screens (CaptainScreen, GroundOwnerScreen) are added to navigation. Add a `"tournaments"` route accessible to TOURNAMENT_MANAGER and SUPER_ADMIN. Use the same ViewModel factory pattern.

- [ ] **Step 7: Commit**

```
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/TournamentRepository.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/TournamentViewModel.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/TournamentsScreen.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt
git commit -m "feat(app): TournamentsScreen — list, create, status update + navigation"
```

---

## Task 3: DEV_LOG + push

- [ ] **Run all tournament tests:**
```
cd "C:\Users\Lokesh\Desktop\Pojects\Vms project\backend" && ..\venv\Scripts\python.exe -m pytest modules/tournament/tests/ -v
```

- [ ] **Prepend to `backend/DEV_LOG.md`:**

```markdown
---
## [2026-06-04] Phase 02 — Tournament module

### Backend
**Added:**
- Migration 8: `tournaments` table (name, sport_id, region_id, organizer, start_date, end_date, max_teams, status)
- Full CRUD module: `backend/modules/tournament/` (model, repository, service, schemas, routes)
- `GET/POST /api/v1/tournaments`, `GET/PUT/DELETE /api/v1/tournaments/{id}`
- Role guards: list (TOURNAMENT_MANAGER/OPS_MANAGER/SUPER_ADMIN), create/update (TOURNAMENT_MANAGER/SUPER_ADMIN), delete (SUPER_ADMIN)
- `backend/modules/tournament/tests/test_tournament_service.py` — 5 tests

**Modified:**
- `backend/main.py` — registered tournament router + model
- `backend/run_migrations.py` — Migration 8

### Admin App
**Added:**
- `Tournament`, `CreateTournamentRequest`, `UpdateTournamentRequest` in Models.kt
- `TournamentRepository.kt`
- `TournamentViewModel.kt` — list, create, updateStatus
- `TournamentsScreen.kt` — list + create dialog + status change

**Modified:**
- `ApiService.kt` — tournament CRUD endpoints
- `AppNavigation.kt` — tournaments route (TOURNAMENT_MANAGER + SUPER_ADMIN)

### Architecture decisions
- Minimal CRUD only — no brackets, no automation, no scheduling
- Status transitions: UPCOMING → ONGOING → COMPLETED / CANCELLED (enforced at service layer)
- start_date > end_date rejected at schema validation layer
---
```

- [ ] **Commit and push:**
```
git add backend/DEV_LOG.md
git commit -m "chore: DEV_LOG Phase 02 tournament module"
git push
```
