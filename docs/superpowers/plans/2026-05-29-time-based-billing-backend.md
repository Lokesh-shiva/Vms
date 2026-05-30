# Time-Based Billing — Backend Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the fixed-fee payment model with a two-payment, time-based billing model: a flat matching fee paid upfront, then a session-duration bill computed in 45-minute blocks with optional surge multiplier.

**Architecture:** A pure `billing_calculator` module (no DB, fully unit-testable) computes blocks and bill. `BookingService` gains session start/end lifecycle methods that snapshot pricing and call the calculator. `PaymentService` creates two payment types (MATCHING_FEE, TIME_BILL) and branches booking state on approval. New routes expose captain start/end and a live session-status estimate. Pricing config gains time-rate + surge columns.

**Tech Stack:** Python 3.12, FastAPI, SQLAlchemy, PostgreSQL (Neon), pytest. Existing dict-based repository/service pattern.

**Scope note:** This is the backend half. The admin-app timer UI, pricing-form fields, and captain active-sessions panel are covered in a separate plan (`2026-05-29-time-based-billing-app.md`) written after this lands.

---

## File Structure

| File | Responsibility | New/Modify |
|------|---------------|------------|
| `backend/modules/billing/__init__.py` | Package marker | Create |
| `backend/modules/billing/billing_calculator.py` | Pure billing math (blocks, bill) | Create |
| `backend/modules/billing/tests/test_billing_calculator.py` | Unit tests for calculator | Create |
| `backend/modules/fee_config/model/fee_config_model.py` | Add time-rate + surge columns | Modify |
| `backend/modules/fee_config/schemas/fee_config_schema.py` | Validate new fields | Modify |
| `backend/modules/fee_config/service/fee_config_service.py` | Validate + surge update | Modify |
| `backend/modules/booking/model/booking_model.py` | Add session + bill columns, new status | Modify |
| `backend/modules/payment/model/payment_model.py` | Add `payment_type` column | Modify |
| `backend/modules/booking/repository/booking_repository.py` | Persist new columns | Modify |
| `backend/modules/booking/service/booking_service.py` | `start_session`, `end_session` | Modify |
| `backend/modules/booking/controller/booking_routes.py` | captain-start/end, session-status | Modify |
| `backend/modules/payment/service/payment_service.py` | matching-fee + time-bill flow | Modify |
| `backend/modules/fee_config/controller/fee_config_routes.py` | surge update endpoint | Modify |
| `backend/run_migrations.py` | Add migrations 3–5 | Modify |

---

## Conventions (read before starting)

- **Run tests from `backend/`:** `cd backend && ..\venv\Scripts\python.exe -m pytest <path> -v`
- **Pure-calculator tests** don't touch the DB, so they avoid the known SQLite-FK issue in `test_booking_service.py`.
- **Numeric fields** serialize to `float` in `to_dict()` (follow existing pattern).
- **Commit** after each task with the message shown.

---

### Task 1: Billing calculator (pure functions)

**Files:**
- Create: `backend/modules/billing/__init__.py`
- Create: `backend/modules/billing/billing_calculator.py`
- Create: `backend/modules/billing/tests/__init__.py`
- Test: `backend/modules/billing/tests/test_billing_calculator.py`

- [ ] **Step 1: Write the failing tests**

```python
# backend/modules/billing/tests/test_billing_calculator.py
import math
import pytest

from modules.billing.billing_calculator import (
    calculate_blocks,
    calculate_time_bill,
    BillingError,
)


class TestCalculateBlocks:
    def test_zero_minutes_is_one_block(self):
        # Any started session bills at least one block
        assert calculate_blocks(0, block_duration_minutes=45) == 1

    def test_under_one_block(self):
        assert calculate_blocks(30, block_duration_minutes=45) == 1

    def test_exactly_one_block(self):
        assert calculate_blocks(45, block_duration_minutes=45) == 1

    def test_one_minute_into_second_block(self):
        assert calculate_blocks(46, block_duration_minutes=45) == 2

    def test_exactly_two_blocks(self):
        assert calculate_blocks(90, block_duration_minutes=45) == 2

    def test_into_third_block(self):
        assert calculate_blocks(91, block_duration_minutes=45) == 3

    def test_custom_block_size(self):
        assert calculate_blocks(60, block_duration_minutes=30) == 2

    def test_negative_minutes_raises(self):
        with pytest.raises(BillingError):
            calculate_blocks(-5, block_duration_minutes=45)

    def test_zero_block_duration_raises(self):
        with pytest.raises(BillingError):
            calculate_blocks(45, block_duration_minutes=0)


class TestCalculateTimeBill:
    def test_one_block_no_surge(self):
        bill = calculate_time_bill(
            session_minutes=30, rate_per_block=60.0,
            block_duration_minutes=45, surge_multiplier=1.0,
        )
        assert bill == 60.0

    def test_two_blocks_no_surge(self):
        bill = calculate_time_bill(
            session_minutes=90, rate_per_block=60.0,
            block_duration_minutes=45, surge_multiplier=1.0,
        )
        assert bill == 120.0

    def test_surge_one_point_five(self):
        bill = calculate_time_bill(
            session_minutes=90, rate_per_block=60.0,
            block_duration_minutes=45, surge_multiplier=1.5,
        )
        assert bill == 180.0

    def test_rounds_to_two_decimals(self):
        bill = calculate_time_bill(
            session_minutes=45, rate_per_block=33.33,
            block_duration_minutes=45, surge_multiplier=1.1,
        )
        # 1 * 33.33 * 1.1 = 36.663 -> 36.66
        assert bill == 36.66

    def test_negative_rate_raises(self):
        with pytest.raises(BillingError):
            calculate_time_bill(
                session_minutes=45, rate_per_block=-1.0,
                block_duration_minutes=45, surge_multiplier=1.0,
            )

    def test_surge_below_one_raises(self):
        with pytest.raises(BillingError):
            calculate_time_bill(
                session_minutes=45, rate_per_block=60.0,
                block_duration_minutes=45, surge_multiplier=0.5,
            )
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && ..\venv\Scripts\python.exe -m pytest modules/billing/tests/test_billing_calculator.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'modules.billing.billing_calculator'`

- [ ] **Step 3: Create package markers**

```python
# backend/modules/billing/__init__.py
```
```python
# backend/modules/billing/tests/__init__.py
```
(both empty files)

- [ ] **Step 4: Implement the calculator**

```python
# backend/modules/billing/billing_calculator.py
"""Pure billing math for time-based sessions. No DB, no side effects."""

import math


class BillingError(ValueError):
    """Raised on invalid billing inputs."""


def calculate_blocks(session_minutes: int, block_duration_minutes: int) -> int:
    """Return the number of billable blocks for a session.

    A started session always bills at least one block (minimum charge).
    Blocks round UP: 46 minutes over a 45-minute block = 2 blocks.
    """
    if block_duration_minutes <= 0:
        raise BillingError("block_duration_minutes must be positive.")
    if session_minutes < 0:
        raise BillingError("session_minutes cannot be negative.")
    if session_minutes == 0:
        return 1
    return math.ceil(session_minutes / block_duration_minutes)


def calculate_time_bill(
    session_minutes: int,
    rate_per_block: float,
    block_duration_minutes: int,
    surge_multiplier: float = 1.0,
) -> float:
    """Compute the time-portion of the bill (excludes matching fee).

    time_bill = blocks * rate_per_block * surge_multiplier, rounded to 2dp.
    """
    if rate_per_block < 0:
        raise BillingError("rate_per_block cannot be negative.")
    if surge_multiplier < 1.0:
        raise BillingError("surge_multiplier cannot be below 1.0.")
    blocks = calculate_blocks(session_minutes, block_duration_minutes)
    return round(blocks * rate_per_block * surge_multiplier, 2)
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd backend && ..\venv\Scripts\python.exe -m pytest modules/billing/tests/test_billing_calculator.py -v`
Expected: PASS (15 passed)

- [ ] **Step 6: Commit**

```bash
git add backend/modules/billing/
git commit -m "feat(billing): pure block + time-bill calculator with tests"
```

---

### Task 2: Pricing config — new columns (model)

**Files:**
- Modify: `backend/modules/fee_config/model/fee_config_model.py`

- [ ] **Step 1: Add columns to the model**

In `fee_config_model.py`, add these columns after `platform_fee_pct` (line ~51):

```python
    platform_fee_pct = Column(Numeric(5, 2), nullable=False, default=0)
    # ── Time-based billing ──────────────────────────────────────────
    matching_fee = Column(Numeric(10, 2), nullable=False, default=0)
    rate_per_block = Column(Numeric(10, 2), nullable=False, default=0)
    block_duration_minutes = Column(Integer, nullable=False, default=45)
    max_duration_minutes = Column(Integer, nullable=False, default=180)
    surge_enabled = Column(Boolean, nullable=False, default=False)
    surge_multiplier = Column(Numeric(4, 2), nullable=False, default=1.0)
```

- [ ] **Step 2: Add fields to `to_dict()`**

In the `to_dict()` return dict, add after `platform_fee_pct`:

```python
            "matching_fee": float(self.matching_fee) if self.matching_fee is not None else 0.0,
            "rate_per_block": float(self.rate_per_block) if self.rate_per_block is not None else 0.0,
            "block_duration_minutes": self.block_duration_minutes,
            "max_duration_minutes": self.max_duration_minutes,
            "surge_enabled": self.surge_enabled,
            "surge_multiplier": float(self.surge_multiplier) if self.surge_multiplier is not None else 1.0,
```

- [ ] **Step 3: Verify import exists**

Confirm the import line at top includes `Integer` (it does not currently — the model only imports Boolean, Column, DateTime, ForeignKey, Integer, Numeric, UniqueConstraint). Open the file and ensure `Integer` is in the `from sqlalchemy import (...)` block. If missing, add it.

- [ ] **Step 4: Commit**

```bash
git add backend/modules/fee_config/model/fee_config_model.py
git commit -m "feat(pricing): add time-rate + surge columns to config model"
```

---

### Task 3: Booking + Payment models — session & bill columns

**Files:**
- Modify: `backend/modules/booking/model/booking_model.py`
- Modify: `backend/modules/payment/model/payment_model.py`

- [ ] **Step 1: Add `AWAITING_TIME_PAYMENT` to booking statuses**

In `booking_model.py`, update `VALID_STATUSES`:

```python
    VALID_STATUSES = [
        "PENDING_PAYMENT",
        "CONFIRMED",
        "IN_PROGRESS",
        "AWAITING_TIME_PAYMENT",
        "COMPLETED",
        "CANCELLED",
        "EXPIRED",
    ]
```

- [ ] **Step 2: Add session columns to Booking model**

Add `DateTime` is already imported. Add after `platform_fee_pct_snapshot` (line ~64):

```python
    platform_fee_pct_snapshot = Column(Numeric(5, 2), nullable=False, default=0)
    # ── Time-based session tracking ─────────────────────────────────
    session_started_at = Column(DateTime, nullable=True)
    session_ended_at = Column(DateTime, nullable=True)
    session_minutes = Column(Integer, nullable=True)
    session_blocks = Column(Integer, nullable=True)
    time_bill_amount = Column(Numeric(10, 2), nullable=True)
    surge_multiplier_snapshot = Column(Numeric(4, 2), nullable=True)
```

- [ ] **Step 3: Add session fields to Booking `to_dict()`**

Add before `"date":` in the return dict:

```python
            "session_started_at": self.session_started_at.isoformat() if self.session_started_at else None,
            "session_ended_at": self.session_ended_at.isoformat() if self.session_ended_at else None,
            "session_minutes": self.session_minutes,
            "session_blocks": self.session_blocks,
            "time_bill_amount": float(self.time_bill_amount) if self.time_bill_amount is not None else None,
            "surge_multiplier_snapshot": float(self.surge_multiplier_snapshot) if self.surge_multiplier_snapshot is not None else None,
```

- [ ] **Step 4: Add `payment_type` to Payment model**

In `payment_model.py`, add after `provider` column (line ~52):

```python
    provider = Column(String, nullable=False, default="MANUAL_UPI")
    payment_type = Column(String, nullable=False, default="MATCHING_FEE")
```

And in `to_dict()` after `"provider":`:

```python
            "payment_type": self.payment_type,
```

- [ ] **Step 5: Commit**

```bash
git add backend/modules/booking/model/booking_model.py backend/modules/payment/model/payment_model.py
git commit -m "feat(billing): add session + payment_type columns to booking/payment models"
```

---

### Task 4: DB migration for new columns

**Files:**
- Modify: `backend/run_migrations.py`

- [ ] **Step 1: Append migrations after the captains table block**

Add before the final `conn.commit()`:

```python
print("Running migration 3: add time-rate + surge columns to region_cart_type_configs ...")
cur.execute("""
    ALTER TABLE region_cart_type_configs
        ADD COLUMN IF NOT EXISTS matching_fee NUMERIC(10,2) NOT NULL DEFAULT 0,
        ADD COLUMN IF NOT EXISTS rate_per_block NUMERIC(10,2) NOT NULL DEFAULT 0,
        ADD COLUMN IF NOT EXISTS block_duration_minutes INT NOT NULL DEFAULT 45,
        ADD COLUMN IF NOT EXISTS max_duration_minutes INT NOT NULL DEFAULT 180,
        ADD COLUMN IF NOT EXISTS surge_enabled BOOLEAN NOT NULL DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS surge_multiplier NUMERIC(4,2) NOT NULL DEFAULT 1.0;
""")

print("Running migration 4: add session columns to bookings ...")
cur.execute("""
    ALTER TABLE bookings
        ADD COLUMN IF NOT EXISTS session_started_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS session_ended_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS session_minutes INT,
        ADD COLUMN IF NOT EXISTS session_blocks INT,
        ADD COLUMN IF NOT EXISTS time_bill_amount NUMERIC(10,2),
        ADD COLUMN IF NOT EXISTS surge_multiplier_snapshot NUMERIC(4,2);
""")

print("Running migration 5: add payment_type to payments ...")
cur.execute("""
    ALTER TABLE payments
        ADD COLUMN IF NOT EXISTS payment_type VARCHAR(50) NOT NULL DEFAULT 'MATCHING_FEE';
""")
```

- [ ] **Step 2: Run the migration**

Run: `cd backend && ..\venv\Scripts\python.exe run_migrations.py`
Expected output ends with: `All migrations completed successfully.`

- [ ] **Step 3: Verify columns exist**

Run:
```bash
cd backend && ..\venv\Scripts\python.exe -c "import os; from pathlib import Path; from dotenv import load_dotenv; import psycopg2; load_dotenv(Path('.')/'.env'); c=psycopg2.connect(os.environ['DATABASE_URL']).cursor(); c.execute(\"SELECT column_name FROM information_schema.columns WHERE table_name='bookings' AND column_name LIKE 'session%'\"); print(c.fetchall())"
```
Expected: list including `session_started_at`, `session_ended_at`, `session_minutes`.

- [ ] **Step 4: Commit**

```bash
git add backend/run_migrations.py
git commit -m "feat(billing): DB migration for time-rate, session, payment_type columns"
```

---

### Task 5: Pricing config schema + surge validation

**Files:**
- Modify: `backend/modules/fee_config/schemas/fee_config_schema.py`
- Test: `backend/modules/fee_config/tests/test_fee_config_service.py`

- [ ] **Step 1: Read the existing schema file**

Open `backend/modules/fee_config/schemas/fee_config_schema.py` and locate the Create and Update schema classes. They follow the dict-validation pattern (`is_valid()` + `validated_data`). Add validation for the new optional fields in BOTH create and update schemas.

- [ ] **Step 2: Add field validation**

For each new field, in the schema's `is_valid()` method, add (using the create schema's existing style; mirror in update as optional):

```python
        # ── Time-based billing fields (all optional, defaulted in DB) ──
        for num_field in ("matching_fee", "rate_per_block"):
            if num_field in self._data:
                val = self._data[num_field]
                if not isinstance(val, (int, float)) or val < 0:
                    self.errors.append(f"'{num_field}' must be a non-negative number.")
                else:
                    self.validated_data[num_field] = val

        for int_field in ("block_duration_minutes", "max_duration_minutes"):
            if int_field in self._data:
                val = self._data[int_field]
                if not isinstance(val, int) or val <= 0:
                    self.errors.append(f"'{int_field}' must be a positive integer.")
                else:
                    self.validated_data[int_field] = val

        if "surge_enabled" in self._data:
            val = self._data["surge_enabled"]
            if not isinstance(val, bool):
                self.errors.append("'surge_enabled' must be a boolean.")
            else:
                self.validated_data["surge_enabled"] = val

        if "surge_multiplier" in self._data:
            val = self._data["surge_multiplier"]
            if not isinstance(val, (int, float)) or val < 1.0 or val > 3.0:
                self.errors.append("'surge_multiplier' must be between 1.0 and 3.0.")
            else:
                self.validated_data["surge_multiplier"] = val
```

- [ ] **Step 3: Add a service method for surge update + test**

Write the failing test first:

```python
# append to backend/modules/fee_config/tests/test_fee_config_service.py
def test_set_surge_updates_multiplier(fee_config_service_with_seed):
    # fixture provides a service + a known config_id (id=1) — see existing fixtures
    svc, config_id = fee_config_service_with_seed
    updated = svc.set_surge(config_id, enabled=True, multiplier=1.5)
    assert updated["surge_enabled"] is True
    assert updated["surge_multiplier"] == 1.5


def test_set_surge_rejects_out_of_range(fee_config_service_with_seed):
    svc, config_id = fee_config_service_with_seed
    import pytest
    with pytest.raises(ValueError):
        svc.set_surge(config_id, enabled=True, multiplier=5.0)
```

> **Note:** Check the existing test file for the actual fixture name/shape. If there is no reusable fixture, construct the service with an in-memory SQLite `session_factory` exactly as the other tests in that file do, seed one config row, and use its id. Match the existing file's setup verbatim — do not invent a new pattern.

- [ ] **Step 4: Implement `set_surge` in the service**

In `fee_config_service.py`, add:

```python
    def set_surge(self, config_id: int, enabled: bool, multiplier: float) -> dict | None:
        """Set surge state + multiplier for a pricing config.

        multiplier must be within [1.0, 3.0].
        """
        if multiplier < 1.0 or multiplier > 3.0:
            raise ValueError("surge_multiplier must be between 1.0 and 3.0.")
        existing = self.fee_config_repository.find_by_id(config_id)
        if not existing:
            return None
        return self.fee_config_repository.update(
            config_id,
            {"surge_enabled": enabled, "surge_multiplier": multiplier},
        )
```

- [ ] **Step 5: Run the fee-config tests**

Run: `cd backend && ..\venv\Scripts\python.exe -m pytest modules/fee_config/tests/ -v`
Expected: PASS (existing tests + 2 new)

- [ ] **Step 6: Commit**

```bash
git add backend/modules/fee_config/
git commit -m "feat(pricing): validate time-rate fields + set_surge service method"
```

---

### Task 6: Booking session lifecycle (start/end)

**Files:**
- Modify: `backend/modules/booking/service/booking_service.py`
- Test: `backend/modules/billing/tests/test_session_billing.py` (pure-logic test, no DB)

**Design:** `start_session` and `end_session` operate on a booking. To keep them unit-testable without the SQLite-FK problem, factor the bill computation into a pure helper `compute_session_bill` that takes primitives and returns a dict. The service methods do DB I/O around it.

- [ ] **Step 1: Write the failing pure-helper test**

```python
# backend/modules/billing/tests/test_session_billing.py
from datetime import datetime
from modules.billing.billing_calculator import compute_session_bill


def test_compute_session_bill_basic():
    start = datetime(2026, 7, 1, 6, 0, 0)
    end = datetime(2026, 7, 1, 7, 30, 0)  # 90 minutes
    result = compute_session_bill(
        started_at=start, ended_at=end,
        rate_per_block=60.0, block_duration_minutes=45,
        max_duration_minutes=180, surge_multiplier=1.0,
    )
    assert result["session_minutes"] == 90
    assert result["session_blocks"] == 2
    assert result["time_bill_amount"] == 120.0
    assert result["surge_multiplier_snapshot"] == 1.0


def test_compute_session_bill_caps_at_max_duration():
    start = datetime(2026, 7, 1, 6, 0, 0)
    end = datetime(2026, 7, 1, 12, 0, 0)  # 360 minutes, capped to 180
    result = compute_session_bill(
        started_at=start, ended_at=end,
        rate_per_block=60.0, block_duration_minutes=45,
        max_duration_minutes=180, surge_multiplier=1.0,
    )
    assert result["session_minutes"] == 180
    assert result["session_blocks"] == 4  # ceil(180/45)
    assert result["time_bill_amount"] == 240.0


def test_compute_session_bill_with_surge():
    start = datetime(2026, 7, 1, 6, 0, 0)
    end = datetime(2026, 7, 1, 6, 30, 0)  # 30 min -> 1 block
    result = compute_session_bill(
        started_at=start, ended_at=end,
        rate_per_block=60.0, block_duration_minutes=45,
        max_duration_minutes=180, surge_multiplier=2.0,
    )
    assert result["session_minutes"] == 30
    assert result["session_blocks"] == 1
    assert result["time_bill_amount"] == 120.0
    assert result["surge_multiplier_snapshot"] == 2.0
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend && ..\venv\Scripts\python.exe -m pytest modules/billing/tests/test_session_billing.py -v`
Expected: FAIL — `ImportError: cannot import name 'compute_session_bill'`

- [ ] **Step 3: Implement `compute_session_bill` in the calculator**

Add to `backend/modules/billing/billing_calculator.py`:

```python
from datetime import datetime


def compute_session_bill(
    started_at: datetime,
    ended_at: datetime,
    rate_per_block: float,
    block_duration_minutes: int,
    max_duration_minutes: int,
    surge_multiplier: float = 1.0,
) -> dict:
    """Compute full session billing breakdown from start/end timestamps.

    Caps session_minutes at max_duration_minutes. Returns a dict ready to
    merge into a booking update.
    """
    if ended_at < started_at:
        raise BillingError("ended_at cannot be before started_at.")

    raw_minutes = int((ended_at - started_at).total_seconds() // 60)
    session_minutes = min(raw_minutes, max_duration_minutes)
    blocks = calculate_blocks(session_minutes, block_duration_minutes)
    time_bill = calculate_time_bill(
        session_minutes, rate_per_block, block_duration_minutes, surge_multiplier
    )
    return {
        "session_minutes": session_minutes,
        "session_blocks": blocks,
        "time_bill_amount": time_bill,
        "surge_multiplier_snapshot": surge_multiplier,
    }
```

- [ ] **Step 4: Run to verify pass**

Run: `cd backend && ..\venv\Scripts\python.exe -m pytest modules/billing/tests/test_session_billing.py -v`
Expected: PASS (3 passed)

- [ ] **Step 5: Add `start_session` + `end_session` to BookingService**

In `booking_service.py`, add these methods (after `complete_booking`). Note `from datetime import datetime, timedelta` is already imported at top:

```python
    def start_session(self, booking_id: int) -> dict:
        """Captain/admin starts the session timer.

        Allowed only from CONFIRMED. Sets session_started_at and moves to IN_PROGRESS.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "CONFIRMED":
            raise ValueError(
                f"Cannot start session for a booking in {booking['status']} status. "
                "Only CONFIRMED bookings can start."
            )
        return self.booking_repository.update(
            booking_id,
            {
                "status": "IN_PROGRESS",
                "session_started_at": datetime.utcnow(),
            },
        )

    def end_session(self, booking_id: int) -> dict:
        """Captain/admin ends the session. Computes the time bill and moves to
        AWAITING_TIME_PAYMENT.

        Looks up the pricing config for the booking's region+cart_type to get
        rate_per_block, block_duration, max_duration, and surge.
        """
        from modules.billing.billing_calculator import compute_session_bill

        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "IN_PROGRESS":
            raise ValueError(
                f"Cannot end session for a booking in {booking['status']} status. "
                "Only IN_PROGRESS bookings can end."
            )
        if not booking.get("session_started_at"):
            raise ValueError("Booking has no session start time.")

        config = self.fee_config_repository.find_by_region_and_cart_type(
            booking["region_id"], booking["cart_type_id"]
        )
        if not config:
            raise ValueError("No pricing config found for this region + sport.")

        started_at = booking["session_started_at"]
        if isinstance(started_at, str):
            started_at = datetime.fromisoformat(started_at)

        surge = (
            float(config["surge_multiplier"])
            if config.get("surge_enabled")
            else 1.0
        )
        breakdown = compute_session_bill(
            started_at=started_at,
            ended_at=datetime.utcnow(),
            rate_per_block=float(config["rate_per_block"]),
            block_duration_minutes=int(config["block_duration_minutes"]),
            max_duration_minutes=int(config["max_duration_minutes"]),
            surge_multiplier=surge,
        )

        update = {
            "status": "AWAITING_TIME_PAYMENT",
            "session_ended_at": datetime.utcnow(),
            **breakdown,
        }
        updated = self.booking_repository.update(booking_id, update)

        # Create the TIME_BILL payment record for the user to pay
        self.payment_service.create_time_bill_payment(
            booking_id, breakdown["time_bill_amount"]
        )
        return updated

    def session_status(self, booking_id: int) -> dict:
        """Return a live billing estimate for an in-progress session."""
        from modules.billing.billing_calculator import compute_session_bill

        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "IN_PROGRESS" or not booking.get("session_started_at"):
            return {
                "booking_id": booking_id,
                "status": booking["status"],
                "running": False,
                "elapsed_minutes": 0,
                "current_blocks": 0,
                "estimated_time_bill": 0.0,
            }

        config = self.fee_config_repository.find_by_region_and_cart_type(
            booking["region_id"], booking["cart_type_id"]
        )
        started_at = booking["session_started_at"]
        if isinstance(started_at, str):
            started_at = datetime.fromisoformat(started_at)
        surge = (
            float(config["surge_multiplier"])
            if config and config.get("surge_enabled")
            else 1.0
        )
        breakdown = compute_session_bill(
            started_at=started_at,
            ended_at=datetime.utcnow(),
            rate_per_block=float(config["rate_per_block"]) if config else 0.0,
            block_duration_minutes=int(config["block_duration_minutes"]) if config else 45,
            max_duration_minutes=int(config["max_duration_minutes"]) if config else 180,
            surge_multiplier=surge,
        )
        return {
            "booking_id": booking_id,
            "status": booking["status"],
            "running": True,
            "elapsed_minutes": breakdown["session_minutes"],
            "current_blocks": breakdown["session_blocks"],
            "estimated_time_bill": breakdown["time_bill_amount"],
        }
```

- [ ] **Step 6: Ensure booking repository persists new fields**

The repository `update()` uses `setattr` for any key matching a model attribute, so new columns are handled automatically. The `create()` method uses an explicit constructor — no change needed since sessions start NULL. **Verify** `update()` in `booking_repository.py` does NOT have a column allow-list that would drop the new keys (it only excludes `id`, `created_at`, `updated_at`). No change required.

- [ ] **Step 7: Commit**

```bash
git add backend/modules/billing/ backend/modules/booking/service/booking_service.py
git commit -m "feat(billing): session start/end lifecycle + live status on BookingService"
```

---

### Task 7: Payment service — matching fee + time bill

**Files:**
- Modify: `backend/modules/payment/service/payment_service.py`

- [ ] **Step 1: Matching-fee amount on `initiate_payment`**

In `initiate_payment`, replace the amount calculation:

```python
        amount = booking["estimated_total"] + booking["booking_fee"]
```
with a matching-fee lookup:

```python
        config = self.config_repository  # NOTE: this is system_config_repo, not pricing
        # Pricing config lives in fee_config; fetch via a fresh FeeConfigService
        from modules.fee_config.service.fee_config_service import FeeConfigService
        pricing = FeeConfigService().get_config_by_region_and_cart_type(
            booking["region_id"], booking["cart_type_id"]
        )
        matching_fee = float(pricing["matching_fee"]) if pricing else 0.0
        amount = matching_fee
```

And set `payment_type` in the `payment_repository.create({...})` call inside `initiate_payment`:

```python
                    {
                        "booking_id": booking_id,
                        "provider": "MANUAL_UPI",
                        "payment_type": "MATCHING_FEE",
                        "amount": amount,
                        "reference_code": reference_code,
                        "status": "PENDING",
                    }
```

- [ ] **Step 2: Add `create_time_bill_payment`**

Add a method to `PaymentService`:

```python
    def create_time_bill_payment(self, booking_id: int, amount: float) -> dict:
        """Create the post-session TIME_BILL payment for the user to pay.

        Idempotent: if a TIME_BILL payment already exists for this booking,
        return it instead of creating a duplicate.
        """
        existing = self.payment_repository.find_all(status=None)
        for p in existing:
            if p.get("booking_id") == booking_id and p.get("payment_type") == "TIME_BILL":
                return p

        booking = self.booking_repository.find_by_id(booking_id)
        user_id = booking["user_id"] if booking else None

        for attempt in range(MAX_REFCODE_ATTEMPTS):
            reference_code = f"{self._generate_reference_code(booking_id)}-T"
            try:
                return self.payment_repository.create(
                    {
                        "booking_id": booking_id,
                        "user_id": user_id,
                        "provider": "MANUAL_UPI",
                        "payment_type": "TIME_BILL",
                        "amount": amount,
                        "reference_code": reference_code,
                        "status": "PENDING",
                    }
                )
            except Exception:
                if attempt == MAX_REFCODE_ATTEMPTS - 1:
                    raise RuntimeError("Failed to generate unique TIME_BILL reference code.")
```

> **Note on the unique constraint:** payments has `UniqueConstraint(booking_id, user_id)`. A booking now has up to TWO payments (matching fee with `user_id=NULL`, time bill with `user_id=set`). Since the matching-fee payment created by `initiate_payment` does not set `user_id` (stays NULL) and the time-bill sets `user_id`, the pair `(booking_id, NULL)` vs `(booking_id, user_id)` differ — no constraint violation. **Verify** this holds: if `initiate_payment` ever sets user_id, this breaks. It currently does not.

- [ ] **Step 3: Branch `approve_payment` on payment_type**

In `approve_payment`, after moving payment to SUCCESS and mirroring `booking.payment_status`, replace the auto-confirm block with a type-aware branch:

```python
        # Type-aware booking transition
        booking = self.booking_repository.find_by_id(payment["booking_id"])
        if booking:
            if payment.get("payment_type") == "TIME_BILL":
                # Final payment — complete the booking and release the ground
                if booking["status"] == "AWAITING_TIME_PAYMENT":
                    self.booking_repository.update(
                        payment["booking_id"], {"status": "COMPLETED"}
                    )
                    if booking.get("assigned_cart_id"):
                        from modules.cart.repository.cart_repository import (
                            cart_repository,
                        )
                        cart_repository.update(
                            booking["assigned_cart_id"], {"status": "AVAILABLE"}
                        )
            else:
                # MATCHING_FEE — auto-confirm (existing behaviour)
                if booking["status"] == "PENDING_PAYMENT":
                    try:
                        from modules.booking.service.booking_service import BookingService
                        BookingService().confirm_booking(payment["booking_id"])
                    except ValueError:
                        pass
```

- [ ] **Step 4: Run payment tests**

Run: `cd backend && ..\venv\Scripts\python.exe -m pytest modules/payment/tests/ -v`
Expected: Existing tests still pass. (Some tests assert `amount == estimated_total + booking_fee`; if any now fail because of the matching-fee change, update those tests to set a pricing config with a known `matching_fee` and assert that value. Show the diff in the commit.)

- [ ] **Step 5: Commit**

```bash
git add backend/modules/payment/service/payment_service.py backend/modules/payment/tests/
git commit -m "feat(billing): matching-fee initiate + time-bill payment + type-aware approve"
```

---

### Task 8: Routes — captain start/end, session status, surge

**Files:**
- Modify: `backend/modules/booking/controller/booking_routes.py`
- Modify: `backend/modules/fee_config/controller/fee_config_routes.py`

- [ ] **Step 1: Add captain/admin session routes to booking_routes.py**

Add the `require_role` import and `UserRole`:

```python
from modules.auth.dependencies.auth_dependencies import (
    _ADMIN_ROLES,
    get_current_user,
    require_admin,
    require_role,
    require_user,
)
from modules.user.model.user_model import UserRole
```

Add endpoints:

```python
@router.post("/{booking_id}/start-session")
def start_session(
    booking_id: int,
    current_user: dict = require_role(
        UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER, UserRole.GROUND_OWNER
    ),
):
    """Captain/admin starts the session timer (CONFIRMED -> IN_PROGRESS)."""
    try:
        booking = booking_service.start_session(booking_id)
        return _success(booking, "Session started.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/end-session")
def end_session(
    booking_id: int,
    current_user: dict = require_role(
        UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER, UserRole.GROUND_OWNER
    ),
):
    """Captain/admin ends the session, computes the time bill
    (IN_PROGRESS -> AWAITING_TIME_PAYMENT)."""
    try:
        booking = booking_service.end_session(booking_id)
        return _success(booking, "Session ended. Time bill generated.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{booking_id}/session-status")
def session_status(booking_id: int, current_user: dict = Depends(get_current_user)):
    """Live elapsed time + running bill estimate for an in-progress session."""
    try:
        return _success(booking_service.session_status(booking_id))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
```

> **Note:** GROUND_OWNER stands in for the captain operationally until a dedicated CAPTAIN role exists. (Per design, captains are USER-role accounts with a captain profile; a follow-up can switch this guard to a captain-profile check.)

- [ ] **Step 2: Add surge endpoint to fee_config_routes.py**

Read the file first to match its router prefix and response helper. Add:

```python
@router.put("/{config_id}/surge")
def set_surge(
    config_id: int,
    request_data: dict,
    current_user: dict = require_role(UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER),
):
    """Set surge state + multiplier for a pricing config (OPS_MANAGER, SUPER_ADMIN)."""
    enabled = request_data.get("surge_enabled", False)
    multiplier = request_data.get("surge_multiplier", 1.0)
    try:
        updated = fee_config_service.set_surge(config_id, enabled, multiplier)
        if not updated:
            raise HTTPException(status_code=404, detail="Config not found.")
        return _success(updated, "Surge updated.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
```

Ensure `require_role` and `UserRole` are imported in that file (add if missing).

- [ ] **Step 3: Manual smoke test (server running)**

Start the server, then with your super_admin token:
```
POST /api/v1/bookings/{id}/start-session   → 200, status IN_PROGRESS
GET  /api/v1/bookings/{id}/session-status  → running:true, elapsed_minutes ~0
POST /api/v1/bookings/{id}/end-session      → 200, status AWAITING_TIME_PAYMENT
GET  /api/v1/payments/?status=PENDING       → a TIME_BILL payment appears
```

- [ ] **Step 4: Commit**

```bash
git add backend/modules/booking/controller/booking_routes.py backend/modules/fee_config/controller/fee_config_routes.py
git commit -m "feat(billing): session start/end/status + surge config endpoints"
```

---

### Task 9: Update seed data + full test run

**Files:**
- Modify: `backend/db_seed.py`

- [ ] **Step 1: Add time-rate values to seeded pricing configs**

In `db_seed.py`, update the `region_cart_type_configs` INSERT to include the new columns:

```python
run("""
INSERT INTO region_cart_type_configs
  (region_id, cart_type_id, booking_fee, cancellation_fee_pct, platform_fee_pct,
   matching_fee, rate_per_block, block_duration_minutes, max_duration_minutes,
   surge_enabled, surge_multiplier, is_active, created_at, updated_at)
VALUES
  (1, 1, 150.00, 10.00, 5.00, 150.00, 60.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (1, 2, 120.00, 10.00, 5.00, 120.00, 50.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (1, 3,  80.00, 10.00, 5.00,  80.00, 40.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (3, 1, 100.00, 10.00, 5.00, 100.00, 55.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (3, 2,  90.00, 10.00, 5.00,  90.00, 45.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (5, 3,  70.00, 10.00, 5.00,  70.00, 35.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW());
""")
```

- [ ] **Step 2: Re-seed**

Run: `cd backend && ..\venv\Scripts\python.exe db_seed.py`
Expected: `[OK] Database seeded successfully.`

- [ ] **Step 3: Run the full backend suite**

Run: `cd backend && ..\venv\Scripts\python.exe -m pytest . -q`
Expected: all pass EXCEPT the known pre-existing `test_booking_service.py` / `test_booking_item_service.py` SQLite-FK collection errors (document these as pre-existing if still failing; they are unrelated to this work). New billing + fee-config + payment tests pass.

- [ ] **Step 4: Append DEV_LOG entry**

Add an entry to `backend/DEV_LOG.md` (append, never overwrite) documenting: new billing module, model/migration changes, session lifecycle, two-payment flow, new routes, architectural decisions (pure calculator, matching-fee snapshot, AWAITING_TIME_PAYMENT state, GROUND_OWNER-as-captain interim guard).

- [ ] **Step 5: Commit**

```bash
git add backend/db_seed.py backend/DEV_LOG.md
git commit -m "chore(billing): seed time-rate pricing + DEV_LOG entry"
```

---

## Self-Review Notes

- **Spec coverage:** matching fee (Task 7) ✓, 45-min blocks (Task 1) ✓, ceil rounding (Task 1) ✓, session timer start/end (Task 6/8) ✓, captain trigger (Task 8 guard) ✓, max-duration cap (Task 6) ✓, surge per region+sport (Task 2/5/8) ✓, AWAITING_TIME_PAYMENT state (Task 3/6/7) ✓, auto time-bill on end (Task 6) ✓, type-aware approve→COMPLETED (Task 7) ✓, live status estimate (Task 6/8) ✓.
- **Deferred (Phase 03, per spec):** GPS check-in, auto-surge from demand, in-app UPI, dispute resolution, 10-minute grace auto-start (noted but not built — captain/admin manual start covers MVP; add as a scheduled job later).
- **Grace-period auto-start** from the spec is intentionally NOT in this plan — it needs a scheduler/cron. Flag for a follow-up task; manual captain/admin start is the MVP path.
- **Type consistency:** `compute_session_bill` returns keys `session_minutes`, `session_blocks`, `time_bill_amount`, `surge_multiplier_snapshot` — same names used in booking columns and `to_dict()`. `payment_type` values `MATCHING_FEE`/`TIME_BILL` consistent across Task 3/7.
