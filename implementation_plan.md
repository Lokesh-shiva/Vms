# Phase 1: DB Models & Pricing

Lay the DB schema foundation for the Uber-style matchmaking platform.
Updates existing Match/MatchPlayer models and creates the new `QueueEntry`, `Sport`, and `Pricing` module.

## Proposed Changes

---

### Match Module — DB Model Update

#### [MODIFY] [match_model.py](file:///d:/Vms%20project/backend/modules/match/model/match_model.py)

Update the [Match](file:///d:/Vms%20project/backend/modules/match/model/match_model.py#8-62) model:
- Replace statuses `OPEN/FULL/COMPLETED/CANCELLED` with `WAITING/MATCHED/ARRIVED/IN_PROGRESS/COMPLETED/CANCELLED`
- Replace `created_by` FK with nullable (system-created matches won't have a user creator); add `sport_id` FK
- Make `cart_id` (ground) nullable until match is formed
- Keep `region_id`, `skill_level`, `max_players` (hardcode to 2 for MVP)
- Remove `timeslot_id` (matchmaking is demand-driven, not timeslot-based)

Update [MatchPlayer](file:///d:/Vms%20project/backend/modules/match/model/match_model.py#64-95) model:
- Add `has_arrived` boolean field (replaces `has_paid`, which moves to the payment module)
- Keep `joined_at`

---

### New Module: `modules/matchmaking/`

#### [NEW] `modules/matchmaking/model/queue_entry_model.py`

New `QueueEntry` ORM model:
```
- id (PK)
- user_id (FK → users.id)
- region_id (FK → locations.id)
- sport_id (FK → sports.id)
- skill_level (String: BEGINNER/INTERMEDIATE/ADVANCED)
- status (String: WAITING/MATCHED/CANCELLED)
- created_at (DateTime)
```

#### [NEW] `modules/matchmaking/__init__.py`
#### [NEW] `modules/matchmaking/model/__init__.py`
#### [NEW] `modules/matchmaking/repository/__init__.py`
#### [NEW] `modules/matchmaking/service/__init__.py`
#### [NEW] `modules/matchmaking/controller/__init__.py`
#### [NEW] `modules/matchmaking/schemas/__init__.py`

---

### New Module: `modules/pricing/`

#### [NEW] `modules/pricing/service/pricing_service.py`

Dynamic pricing logic:
```
price = base_price × time_factor × demand_factor
```
- `base_price` from config (env var or `SystemConfig` table)
- `time_factor`: peak hour multiplier (e.g. 1.5x between 5pm–9pm)
- `demand_factor`: based on `get_active_queue_count(region_id, sport_id)`

#### [NEW] `modules/pricing/__init__.py`
#### [NEW] `modules/pricing/service/__init__.py`

---

### New Model: Sport

#### [NEW] `modules/sport/model/sport_model.py`

New `Sport` ORM model:
```
- id (PK)
- name (String, unique)
- is_active (Boolean, default True)
```

#### [NEW] `modules/sport/__init__.py`
#### [NEW] `modules/sport/model/__init__.py`

---

### App Entry Point

#### [MODIFY] [main.py](file:///d:/Vms%20project/backend/main.py)

Register new model imports so SQLAlchemy creates the tables on startup:
- `from modules.matchmaking.model.queue_entry_model import QueueEntry`
- `from modules.sport.model.sport_model import Sport`

---

## Verification Plan

### Automated Tests
- Run all existing tests to verify no regressions:
  ```powershell
  cd "d:\Vms project\backend"
  python -m pytest --tb=short -q
  ```

### Manual Verification
1. Start the server:
   ```powershell
   cd "d:\Vms project\backend"
   uvicorn main:app --reload --port 8000
   ```
2. Hit `GET http://localhost:8000/health` → should return `{"success": true}`.
3. Check the Neon PostgreSQL console to confirm new tables created: `queue_entries`, `sports`.
4. Verify `matches` table columns include the new `sport_id` column and updated status values.
