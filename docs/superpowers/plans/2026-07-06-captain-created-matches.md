# Captain-Created Matches Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire up the 3 real options in `CreateMatchTab` (Open, Society, Private) to a new captain-only match-creation backend endpoint, and redirect "Tournament" to the existing tournament flow — replacing 4 no-op click handlers with working features.

**Architecture:** Extend the existing `Match` model with a `visibility` column (`OPEN|SOCIETY|PRIVATE`) plus `society_id` and `invite_code`. A captain hits a new `POST /api/v1/matches/captain-create` endpoint that creates a `Match(status=WAITING, captain_id=<their captain id>)` without adding them as a `MatchPlayer`. Society matches are discoverable only via a new society-scoped endpoint; private matches only via a new invite-code join endpoint. The Android app gets new Retrofit calls, model fields, a `CaptainCreateMatchViewModel`, and inline confirm-sheet UI in `CreateMatchTab`.

**Tech Stack:** Python 3.12 / FastAPI / SQLAlchemy / PostgreSQL (backend), Kotlin / Jetpack Compose / Retrofit (Vmsuserapp).

**Reference spec:** `docs/superpowers/specs/2026-07-06-captain-created-matches-design.md`

**Note on request shape:** The spec listed `sport_id` and `cart_type_id` as separate request fields. The existing `CreateMatchSchema` (`backend/modules/match/schemas/match_schema.py`) and `MatchRepository.create_play_now` only ever take `cart_type_id` — `sport_id` is set internally by the repository (`sport_id=cart_type_id`, "unified post-migration-14"). This plan follows that existing convention: the new endpoint takes `cart_type_id`, not a separate `sport_id`.

---

## Task 1: Add `visibility` / `society_id` / `invite_code` columns to `Match`

**Files:**
- Modify: `backend/modules/match/model/match_model.py`
- Modify: `backend/run_migrations.py`

- [ ] **Step 1: Add the migration**

Append to the end of `backend/run_migrations.py`, right before the final `conn.commit()` block (currently ends after "Running migration 16..."):

```python
print("Running migration 21: add match visibility, society_id, invite_code ...")
cur.execute("""
    ALTER TABLE matches
        ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'OPEN',
        ADD COLUMN IF NOT EXISTS society_id INTEGER REFERENCES societies(id) ON DELETE SET NULL,
        ADD COLUMN IF NOT EXISTS invite_code VARCHAR(8);
""")
cur.execute("""
    CREATE UNIQUE INDEX IF NOT EXISTS uq_matches_invite_code
        ON matches (invite_code) WHERE invite_code IS NOT NULL;
""")
```

- [ ] **Step 2: Update the `Match` model**

In `backend/modules/match/model/match_model.py`, add the three new columns right after the existing `captain_id` column (after line 59):

```python
    visibility = Column(String(20), nullable=False, default="OPEN")
    society_id = Column(Integer, ForeignKey("societies.id", ondelete="SET NULL"), nullable=True, index=True)
    invite_code = Column(String(8), nullable=True, unique=True, index=True)
```

Add `VALID_VISIBILITIES` next to the existing `VALID_STATUSES`/`VALID_SKILL_LEVELS` class attributes (after line 44):

```python
    VALID_VISIBILITIES = {"OPEN", "SOCIETY", "PRIVATE"}
```

Add the 3 fields to `to_dict()`, right after `"captain_id": self.captain_id,` (line 81):

```python
            "visibility": self.visibility,
            "society_id": self.society_id,
            "invite_code": self.invite_code,
```

- [ ] **Step 3: Run the migration against the dev database**

```bash
venv\Scripts\python.exe backend/run_migrations.py
```
Expected: prints "Running migration 21: ..." and ends with "All migrations completed successfully." No errors.

- [ ] **Step 4: Commit**

```bash
git add backend/modules/match/model/match_model.py backend/run_migrations.py
git commit -m "feat(match): add visibility, society_id, invite_code columns"
```

---

## Task 2: Repository support — captain-create, society matches, invite-code lookup, visibility filter

**Files:**
- Modify: `backend/modules/match/repository/match_repository.py`
- Test: `backend/modules/match/tests/test_captain_created_matches.py`

- [ ] **Step 1: Write the failing tests**

Create `backend/modules/match/tests/test_captain_created_matches.py`:

```python
"""
Tests for captain-created match repository methods:
create_captain_match, find_waiting_in_region (visibility filter),
find_society_matches, find_by_invite_code.
"""

import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.location.model.location_model import Location  # noqa: F401
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.match.model.match_model import Match, MatchPlayer  # noqa: F401
from modules.user.model.user_model import User  # noqa: F401
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401
from modules.sport.model.sport_model import Sport  # noqa: F401
from modules.booking.model.booking_model import Booking  # noqa: F401
from modules.booking_item.model.booking_item_model import BookingItem  # noqa: F401
from modules.item.model.item_model import Item  # noqa: F401
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig  # noqa: F401
from modules.payment.model.payment_model import Payment  # noqa: F401
from modules.payment.model.system_config_model import SystemConfig  # noqa: F401
from modules.matchmaking.model.queue_entry_model import QueueEntry  # noqa: F401
from modules.captain.model.captain_model import Captain  # noqa: F401
from modules.society.model.society_model import Society  # noqa: F401
from modules.society.model.society_member_model import SocietyMember  # noqa: F401

from modules.match.repository.match_repository import MatchRepository


def _make_test_session_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestCaptainCreatedMatches(unittest.TestCase):
    def setUp(self):
        self.session_factory = _make_test_session_factory()
        self.repo = MatchRepository(session_factory=self.session_factory)

        session = self.session_factory()
        session.add(Location(name="Downtown", is_serviceable=True))  # id=1
        session.add(CartType(name="Badminton"))  # id=1
        session.add(Captain(user_id=10, status="ACTIVE"))  # id=1
        session.add(Society(name="Weekend Warriors", owner_user_id=10, region_id=1, sport_id=1))  # id=1
        session.commit()
        session.close()

    def test_create_captain_match_open_no_player_row(self):
        """OPEN captain match: captain_id set, no MatchPlayer row for captain."""
        match = self.repo.create_captain_match(
            captain_id=1,
            region_id=1,
            cart_type_id=1,
            max_players=4,
            visibility="OPEN",
        )
        self.assertEqual(match["status"], "WAITING")
        self.assertEqual(match["captain_id"], 1)
        self.assertEqual(match["joined_players"], 0)
        self.assertEqual(match["visibility"], "OPEN")

        session = self.session_factory()
        players = session.query(MatchPlayer).filter(MatchPlayer.match_id == match["id"]).all()
        self.assertEqual(len(players), 0)
        session.close()

    def test_create_captain_match_society_sets_society_id(self):
        match = self.repo.create_captain_match(
            captain_id=1,
            region_id=1,
            cart_type_id=1,
            max_players=4,
            visibility="SOCIETY",
            society_id=1,
        )
        self.assertEqual(match["society_id"], 1)
        self.assertEqual(match["visibility"], "SOCIETY")

    def test_create_captain_match_private_generates_invite_code(self):
        match = self.repo.create_captain_match(
            captain_id=1,
            region_id=1,
            cart_type_id=1,
            max_players=4,
            visibility="PRIVATE",
        )
        self.assertEqual(match["visibility"], "PRIVATE")
        self.assertIsNotNone(match["invite_code"])
        self.assertEqual(len(match["invite_code"]), 6)

    def test_find_waiting_in_region_only_returns_open(self):
        """SOCIETY and PRIVATE matches must not appear in the public open-matches feed."""
        self.repo.create_captain_match(
            captain_id=1, region_id=1, cart_type_id=1, max_players=4, visibility="OPEN"
        )
        self.repo.create_captain_match(
            captain_id=1, region_id=1, cart_type_id=1, max_players=4,
            visibility="SOCIETY", society_id=1,
        )
        self.repo.create_captain_match(
            captain_id=1, region_id=1, cart_type_id=1, max_players=4, visibility="PRIVATE"
        )
        results = self.repo.find_waiting_in_region(region_id=1)
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["visibility"], "OPEN")

    def test_find_society_matches_returns_only_that_society(self):
        match = self.repo.create_captain_match(
            captain_id=1, region_id=1, cart_type_id=1, max_players=4,
            visibility="SOCIETY", society_id=1,
        )
        results = self.repo.find_society_matches(society_id=1)
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["id"], match["id"])

    def test_find_by_invite_code_returns_match(self):
        match = self.repo.create_captain_match(
            captain_id=1, region_id=1, cart_type_id=1, max_players=4, visibility="PRIVATE"
        )
        found = self.repo.find_by_invite_code(match["invite_code"])
        self.assertIsNotNone(found)
        self.assertEqual(found["id"], match["id"])

    def test_find_by_invite_code_unknown_returns_none(self):
        self.assertIsNone(self.repo.find_by_invite_code("ZZZZZZ"))


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
venv\Scripts\python.exe -m pytest backend/modules/match/tests/test_captain_created_matches.py -v
```
Expected: FAIL — `AttributeError: 'MatchRepository' object has no attribute 'create_captain_match'` (and similar for the other new methods).

- [ ] **Step 3: Implement the repository methods**

In `backend/modules/match/repository/match_repository.py`, add these methods after `create_play_now` (after line 125, before `find_waiting_in_region`):

```python
    def create_captain_match(
        self,
        captain_id: int,
        region_id: int,
        cart_type_id: int,
        max_players: int,
        visibility: str,
        skill_level: str | None = None,
        society_id: int | None = None,
    ) -> dict:
        """
        Create a captain-organized WAITING match. Unlike create_play_now, the
        captain is NOT added as a MatchPlayer — they organize, they don't play.

        For PRIVATE visibility, generates a unique 6-char alphanumeric invite_code.
        """
        import random
        import string

        invite_code = None
        if visibility == "PRIVATE":
            invite_code = "".join(
                random.choices(string.ascii_uppercase + string.digits, k=6)
            )

        session = self._session_factory()
        try:
            match = Match(
                region_id=region_id,
                cart_type_id=cart_type_id,
                sport_id=cart_type_id,
                max_players=max_players,
                joined_players=0,
                status="WAITING",
                captain_id=captain_id,
                skill_level=skill_level,
                visibility=visibility,
                society_id=society_id,
                invite_code=invite_code,
            )
            session.add(match)
            session.commit()
            session.refresh(match)
            return match.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_society_matches(self, society_id: int) -> list[dict]:
        """Return WAITING/MATCHED matches for a specific society, newest first."""
        session = self._session_factory()
        try:
            rows = (
                session.query(Match)
                .filter(
                    Match.society_id == society_id,
                    Match.status.in_(["WAITING", "MATCHED"]),
                )
                .order_by(Match.created_at.desc())
                .all()
            )
            return [self._enrich(m, session) for m in rows]
        finally:
            session.close()

    def find_by_invite_code(self, invite_code: str) -> dict | None:
        """Look up a match by its private invite code."""
        session = self._session_factory()
        try:
            m = session.query(Match).filter(Match.invite_code == invite_code).first()
            return m.to_dict() if m else None
        finally:
            session.close()
```

- [ ] **Step 4: Update `find_waiting_in_region` to filter by `visibility == OPEN`**

In the same file, replace the existing `find_waiting_in_region` method (lines 127–142):

```python
    def find_waiting_in_region(
        self, region_id: int, sport_id: int | None = None
    ) -> list[dict]:
        """Return OPEN-visibility WAITING matches in a region, newest first."""
        session = self._session_factory()
        try:
            query = session.query(Match).filter(
                Match.region_id == region_id,
                Match.status == "WAITING",
                Match.visibility == "OPEN",
            )
            if sport_id:
                query = query.filter(Match.cart_type_id == sport_id)
            rows = query.order_by(Match.created_at.desc()).all()
            return [self._enrich(m, session) for m in rows]
        finally:
            session.close()
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
venv\Scripts\python.exe -m pytest backend/modules/match/tests/test_captain_created_matches.py -v
```
Expected: PASS (7 tests).

- [ ] **Step 6: Run the full match test suite to check for regressions**

```bash
venv\Scripts\python.exe -m pytest backend/modules/match/tests/ -v
```
Expected: PASS (all tests, including `test_match_lifecycle.py`).

- [ ] **Step 7: Commit**

```bash
git add backend/modules/match/repository/match_repository.py backend/modules/match/tests/test_captain_created_matches.py
git commit -m "feat(match): add captain-create, society-matches, invite-code repository methods"
```

---

## Task 3: Service layer — `captain_create_match` and `join_by_code`

**Files:**
- Modify: `backend/modules/match/service/match_service.py`
- Modify: `backend/modules/match/schemas/match_schema.py`
- Test: `backend/modules/match/tests/test_captain_created_matches.py` (extend from Task 2)

- [ ] **Step 1: Write the failing tests**

Append to `backend/modules/match/tests/test_captain_created_matches.py` (add these imports at the top, next to the existing `MatchRepository` import):

```python
from modules.match.service.match_service import MatchService
from modules.captain.repository.captain_repository import CaptainRepository
from modules.cart.repository.cart_repository import CartRepository
from modules.timeslot.repository.timeslot_repository import TimeslotRepository
from modules.cart_type.repository.cart_type_repository import CartTypeRepository
from modules.location.repository.location_repository import LocationRepository
from modules.society.repository.society_member_repository import SocietyMemberRepository
```

Add a new test class at the end of the file, before `if __name__ == "__main__":`:

```python
class TestCaptainCreateMatchService(unittest.TestCase):
    def setUp(self):
        self.session_factory = _make_test_session_factory()
        self.match_repo = MatchRepository(session_factory=self.session_factory)
        self.captain_repo = CaptainRepository(session_factory=self.session_factory)
        self.society_member_repo = SocietyMemberRepository(session_factory=self.session_factory)
        self.service = MatchService(
            match_repository=self.match_repo,
            cart_repository=CartRepository(session_factory=self.session_factory),
            timeslot_repository=TimeslotRepository(session_factory=self.session_factory),
            cart_type_repository=CartTypeRepository(session_factory=self.session_factory),
            location_repository=LocationRepository(session_factory=self.session_factory),
        )

        session = self.session_factory()
        session.add(Location(name="Downtown", is_serviceable=True))  # id=1
        session.add(CartType(name="Badminton"))  # id=1
        session.add(Captain(user_id=10, status="ACTIVE"))  # id=1
        session.add(Captain(user_id=11, status="PENDING_REVIEW"))  # id=2, not active
        session.add(Society(name="Weekend Warriors", owner_user_id=10, region_id=1, sport_id=1))  # id=1
        session.commit()
        session.close()
        self.society_member_repo.add_member(society_id=1, user_id=10, role="OWNER")

    def test_captain_create_match_open(self):
        match = self.service.captain_create_match(
            user_id=10,
            data={
                "cart_type_id": 1, "region_id": 1, "max_players": 4,
                "visibility": "OPEN", "society_id": None, "skill_level": None,
            },
        )
        self.assertEqual(match["status"], "WAITING")
        self.assertEqual(match["captain_id"], 1)

    def test_captain_create_match_requires_active_captain(self):
        with self.assertRaises(ValueError) as ctx:
            self.service.captain_create_match(
                user_id=999,
                data={
                    "cart_type_id": 1, "region_id": 1, "max_players": 4,
                    "visibility": "OPEN", "society_id": None, "skill_level": None,
                },
            )
        self.assertIn("Captain profile not found", str(ctx.exception))

    def test_captain_create_match_inactive_captain_rejected(self):
        with self.assertRaises(ValueError) as ctx:
            self.service.captain_create_match(
                user_id=11,
                data={
                    "cart_type_id": 1, "region_id": 1, "max_players": 4,
                    "visibility": "OPEN", "society_id": None, "skill_level": None,
                },
            )
        self.assertIn("not active", str(ctx.exception))

    def test_captain_create_society_match_requires_membership(self):
        with self.assertRaises(ValueError) as ctx:
            self.service.captain_create_match(
                user_id=10,
                data={
                    "cart_type_id": 1, "region_id": 1, "max_players": 4,
                    "visibility": "SOCIETY", "society_id": 999, "skill_level": None,
                },
            )
        self.assertIn("Society not found", str(ctx.exception))

    def test_captain_create_society_match_non_member_rejected(self):
        session = self.session_factory()
        session.add(Captain(user_id=12, status="ACTIVE"))  # id=3
        session.commit()
        session.close()
        with self.assertRaises(ValueError) as ctx:
            self.service.captain_create_match(
                user_id=12,
                data={
                    "cart_type_id": 1, "region_id": 1, "max_players": 4,
                    "visibility": "SOCIETY", "society_id": 1, "skill_level": None,
                },
            )
        self.assertIn("not a member", str(ctx.exception))

    def test_captain_create_society_match_member_succeeds(self):
        match = self.service.captain_create_match(
            user_id=10,
            data={
                "cart_type_id": 1, "region_id": 1, "max_players": 4,
                "visibility": "SOCIETY", "society_id": 1, "skill_level": None,
            },
        )
        self.assertEqual(match["society_id"], 1)

    def test_join_by_code_success(self):
        created = self.service.captain_create_match(
            user_id=10,
            data={
                "cart_type_id": 1, "region_id": 1, "max_players": 2,
                "visibility": "PRIVATE", "society_id": None, "skill_level": None,
            },
        )
        joined = self.service.join_by_code(user_id=20, invite_code=created["invite_code"])
        self.assertEqual(joined["joined_players"], 1)

    def test_join_by_code_unknown_raises(self):
        with self.assertRaises(ValueError) as ctx:
            self.service.join_by_code(user_id=20, invite_code="ZZZZZZ")
        self.assertIn("Invalid invite code", str(ctx.exception))
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
venv\Scripts\python.exe -m pytest backend/modules/match/tests/test_captain_created_matches.py -v
```
Expected: FAIL — `AttributeError: 'MatchService' object has no attribute 'captain_create_match'`.

- [ ] **Step 3: Add `CaptainCreateMatchSchema` to `match_schema.py`**

In `backend/modules/match/schemas/match_schema.py`, add after `VALID_SKILL_LEVELS` (line 3):

```python
VALID_VISIBILITIES = {"OPEN", "SOCIETY", "PRIVATE"}
```

Add a new schema class at the end of the file:

```python
class CaptainCreateMatchSchema:
    """
    Validates input for POST /matches/captain-create.

    Required: cart_type_id, region_id, max_players, visibility
    Conditionally required: society_id (when visibility == SOCIETY)
    Optional: skill_level
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        cart_type_id = self._data.get("cart_type_id")
        if cart_type_id is None or not isinstance(cart_type_id, int) or cart_type_id <= 0:
            self.errors.append("'cart_type_id' is required and must be a positive integer.")
        else:
            self.validated_data["cart_type_id"] = cart_type_id

        region_id = self._data.get("region_id")
        if region_id is None or not isinstance(region_id, int) or region_id <= 0:
            self.errors.append("'region_id' is required and must be a positive integer.")
        else:
            self.validated_data["region_id"] = region_id

        max_players = self._data.get("max_players")
        if max_players is None or not isinstance(max_players, int) or not (2 <= max_players <= 22):
            self.errors.append("'max_players' is required and must be an integer between 2 and 22.")
        else:
            self.validated_data["max_players"] = max_players

        visibility = self._data.get("visibility")
        if visibility not in VALID_VISIBILITIES:
            self.errors.append(f"'visibility' must be one of: {', '.join(sorted(VALID_VISIBILITIES))}.")
        else:
            self.validated_data["visibility"] = visibility

        society_id = self._data.get("society_id")
        if visibility == "SOCIETY":
            if society_id is None or not isinstance(society_id, int) or society_id <= 0:
                self.errors.append("'society_id' is required when visibility is SOCIETY.")
            else:
                self.validated_data["society_id"] = society_id
        else:
            self.validated_data["society_id"] = None

        skill_level = self._data.get("skill_level")
        if skill_level is not None:
            if not isinstance(skill_level, str) or skill_level.upper() not in VALID_SKILL_LEVELS:
                self.errors.append(f"'skill_level' must be one of: {', '.join(sorted(VALID_SKILL_LEVELS))}.")
            else:
                self.validated_data["skill_level"] = skill_level.upper()
        else:
            self.validated_data["skill_level"] = None

        return len(self.errors) == 0
```

- [ ] **Step 4: Add `captain_create_match` and `join_by_code` to `MatchService`**

In `backend/modules/match/service/match_service.py`, add these imports at the top of the file, alongside the existing repository imports:

```python
from modules.captain.repository.captain_repository import (
    captain_repository as _default_captain_repo,
)
from modules.society.repository.society_member_repository import (
    society_member_repository as _default_society_member_repo,
)
```

Update `__init__` to accept and store these two repositories (modify the existing constructor):

```python
    def __init__(
        self,
        match_repository=None,
        cart_repository=None,
        timeslot_repository=None,
        cart_type_repository=None,
        location_repository=None,
        event_repository=None,
        captain_repository=None,
        society_member_repository=None,
    ):
        super().__init__()
        self.match_repo = match_repository or _default_match_repo
        self.cart_repo = cart_repository or _default_cart_repo
        self.timeslot_repo = timeslot_repository or _default_timeslot_repo
        self.cart_type_repo = cart_type_repository or _default_cart_type_repo
        self.location_repo = location_repository or _default_location_repo
        self.event_repo = event_repository or _default_event_repo
        self.captain_repo = captain_repository or _default_captain_repo
        self.society_member_repo = society_member_repository or _default_society_member_repo
```

Add these two methods after `create_match` (after the existing `create_match` method body, before the `# ── Join ──` section):

```python
    # ── Captain-created matches ───────────────────────────────────────

    def captain_create_match(self, user_id: int, data: dict) -> dict:
        """
        Create a captain-organized match (Open, Society, or Private).

        The captain is set as Match.captain_id but is NOT added as a player —
        they organize, players fill all max_players slots via join.
        """
        captain = self.captain_repo.get_by_user_id(user_id)
        if not captain:
            raise ValueError("Captain profile not found.")
        if captain["status"] != "ACTIVE":
            raise ValueError("Your captain profile is not active.")

        self._get_cart_type(data["cart_type_id"])
        self._get_region(data["region_id"])

        visibility = data["visibility"]
        society_id = data.get("society_id")
        if visibility == "SOCIETY":
            from modules.society.repository.society_repository import society_repository

            society = society_repository.find_by_id(society_id)
            if society is None:
                raise ValueError("Society not found.")
            member = self.society_member_repo.find_member(society_id, user_id)
            if member is None:
                raise ValueError("You are not a member of this society.")

        match = self.match_repo.create_captain_match(
            captain_id=captain["id"],
            region_id=data["region_id"],
            cart_type_id=data["cart_type_id"],
            max_players=data["max_players"],
            visibility=visibility,
            skill_level=data.get("skill_level"),
            society_id=society_id,
        )
        self.event_repo.log(
            match["id"], "MATCH_CREATED", user_id=user_id, meta={"visibility": visibility}
        )
        return match

    def join_by_code(self, user_id: int, invite_code: str) -> dict:
        """Join a PRIVATE match using its invite code."""
        match = self.match_repo.find_by_invite_code(invite_code)
        if match is None:
            raise ValueError("Invalid invite code.")
        return self.join_match(user_id, match["id"])
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
venv\Scripts\python.exe -m pytest backend/modules/match/tests/test_captain_created_matches.py -v
```
Expected: PASS (all tests in both test classes).

- [ ] **Step 6: Run the full match test suite to check for regressions**

```bash
venv\Scripts\python.exe -m pytest backend/modules/match/tests/ -v
```
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/modules/match/service/match_service.py backend/modules/match/schemas/match_schema.py backend/modules/match/tests/test_captain_created_matches.py
git commit -m "feat(match): add captain_create_match and join_by_code service methods"
```

---

## Task 4: Routes — `captain-create`, `join-by-code`, society matches, my societies

**Files:**
- Modify: `backend/modules/match/controller/match_routes.py`
- Modify: `backend/modules/society/controller/society_routes.py`
- Modify: `backend/modules/society/service/society_member_service.py`
- Modify: `backend/modules/society/repository/society_member_repository.py`

- [ ] **Step 1: Add `find_by_user` to `SocietyMemberRepository`**

In `backend/modules/society/repository/society_member_repository.py`, add after `count_members` (before the closing `society_member_repository = SocietyMemberRepository()` line):

```python
    def find_by_user(self, user_id: int) -> list[dict]:
        """Return all society_member rows for a user (their society memberships)."""
        session = self._session_factory()
        try:
            rows = (
                session.query(SocietyMember)
                .filter(SocietyMember.user_id == user_id)
                .all()
            )
            return [m.to_dict() for m in rows]
        finally:
            session.close()
```

- [ ] **Step 2: Add `get_my_societies` to `SocietyMemberService`**

In `backend/modules/society/service/society_member_service.py`, add after `get_leaderboard` (before the closing `society_member_service = SocietyMemberService()` line):

```python
    def get_my_societies(self, user_id: int) -> list[dict]:
        """Return the societies the current user is a member of."""
        memberships = self.member_repository.find_by_user(user_id)
        result = []
        for m in memberships:
            society = self.society_repository.find_by_id(m["society_id"])
            if society is not None:
                result.append(society)
        return result
```

- [ ] **Step 3: Add `GET /societies/mine` and `GET /societies/{society_id}/matches` routes**

In `backend/modules/society/controller/society_routes.py`, insert `GET /mine` **before** the existing `@router.get("/{society_id}")` route (before line 65) — it must be registered ahead of the `/{society_id}` path so FastAPI doesn't try to parse the literal `mine` segment as an int:

```python
@router.get("/mine")
def get_my_societies(current_user: dict = Depends(require_user)):
    """Return societies the current user belongs to — used for the captain society picker."""
    return _success(society_member_service.get_my_societies(current_user["id"]))
```

Add `GET /{society_id}/matches` right before the `@router.post("/{society_id}/tournament-register", ...)` route (before line 205):

```python
@router.get("/{society_id}/matches")
def get_society_matches(
    society_id: int,
    current_user: dict = Depends(require_user),
):
    """List WAITING/MATCHED matches for a society. Members only."""
    member = society_member_service.member_repository.find_member(society_id, current_user["id"])
    if member is None:
        raise HTTPException(status_code=403, detail="You are not a member of this society.")

    from modules.match.repository.match_repository import match_repository
    return _success(match_repository.find_society_matches(society_id))
```

- [ ] **Step 4: Add `POST /matches/captain-create` and `POST /matches/join-by-code` routes**

In `backend/modules/match/controller/match_routes.py`, update the import line (line 11) to include the new schema:

```python
from modules.match.schemas.match_schema import CreateMatchSchema, MatchArriveSchema, CaptainCreateMatchSchema
```

Add these two routes right after the existing `create_match` route (after line 40, before `@router.post("/{match_id}/join")`):

```python
@router.post("/captain-create", status_code=201)
def captain_create_match(request_data: dict, current_user: dict = Depends(require_user)):
    """Create a captain-organized match (Open, Society, or Private). Captain only."""
    schema = CaptainCreateMatchSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        match = match_service.captain_create_match(current_user["id"], schema.validated_data)
        return _success(match, "Match created successfully.")
    except ValueError as e:
        code = 403 if "not a member" in str(e) or "not active" in str(e) or "Captain profile not found" in str(e) else 400
        raise HTTPException(status_code=code, detail=str(e))


@router.post("/join-by-code")
def join_by_code(request_data: dict, current_user: dict = Depends(require_user)):
    """Join a PRIVATE match by its invite code."""
    invite_code = request_data.get("invite_code")
    if not invite_code or not isinstance(invite_code, str):
        raise HTTPException(status_code=400, detail="'invite_code' is required.")
    try:
        match = match_service.join_by_code(current_user["id"], invite_code)
        return _success(match, "Joined match successfully.")
    except ValueError as e:
        code = 404 if e.args and "Invalid invite code" in str(e) else 400
        raise HTTPException(status_code=code, detail=str(e))
```

- [ ] **Step 5: Manual verification — start the backend and hit the new endpoints**

```bash
venv\Scripts\python.exe -m uvicorn backend.main:app --reload --port 8000
```
In a separate terminal (replace `<TOKEN>` with a valid JWT for a user who has an ACTIVE captain profile):
```bash
curl -X POST http://localhost:8000/api/v1/matches/captain-create -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" -d "{\"cart_type_id\":1,\"region_id\":1,\"max_players\":4,\"visibility\":\"OPEN\"}"
```
Expected: `{"success": true, "data": {..., "status": "WAITING", "visibility": "OPEN", ...}, ...}`

- [ ] **Step 6: Commit**

```bash
git add backend/modules/match/controller/match_routes.py backend/modules/society/controller/society_routes.py backend/modules/society/service/society_member_service.py backend/modules/society/repository/society_member_repository.py
git commit -m "feat(match): add captain-create, join-by-code, society-matches, my-societies routes"
```

---

## Task 5: Android — models and Retrofit endpoints

**Files:**
- Modify: `Vmsuserapp/app/src/main/java/com/example/vmsuser/models/Models.kt`
- Modify: `Vmsuserapp/app/src/main/java/com/example/vmsuser/network/ApiService.kt`

- [ ] **Step 1: Extend `Match` and add new model classes**

In `Models.kt`, replace the existing `Match` data class (lines 90–102) with:

```kotlin
@Serializable
data class Match(
    val id: Int = 0,
    val sport: String = "",
    val status: String = "",
    @SerialName("ground_name") val groundName: String = "",
    @SerialName("ground_address") val groundAddress: String = "",
    @SerialName("scheduled_at") val scheduledAt: String = "",
    @SerialName("captain_name") val captainName: String? = null,
    @SerialName("captain_id") val captainId: Int? = null,
    @SerialName("player_ids") val playerIds: List<Int> = emptyList(),
    val price: Int = 0,
    val visibility: String = "OPEN",
    @SerialName("society_id") val societyId: Int? = null,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("max_players") val maxPlayers: Int = 2,
    @SerialName("joined_players") val joinedPlayers: Int = 0,
)
```

Add new model classes at the end of the file (after `CreateSocietyRequest`):

```kotlin
@Serializable
data class CaptainCreateMatchRequest(
    @SerialName("cart_type_id") val cartTypeId: Int,
    @SerialName("region_id") val regionId: Int,
    @SerialName("max_players") val maxPlayers: Int,
    val visibility: String,
    @SerialName("society_id") val societyId: Int? = null,
    @SerialName("skill_level") val skillLevel: String? = null,
)

@Serializable
data class JoinByCodeRequest(
    @SerialName("invite_code") val inviteCode: String,
)

@Serializable
data class MySociety(
    val id: Int = 0,
    val name: String = "",
)
```

- [ ] **Step 2: Add Retrofit endpoints**

In `ApiService.kt`, add after the existing `getMatch` endpoint (after line 63, before the `// Chat` section):

```kotlin
    @POST("api/v1/matches/captain-create")
    suspend fun captainCreateMatch(@Body request: CaptainCreateMatchRequest): ApiResponse<Match>

    @POST("api/v1/matches/join-by-code")
    suspend fun joinMatchByCode(@Body request: JoinByCodeRequest): ApiResponse<Match>

    @GET("api/v1/societies/{id}/matches")
    suspend fun getSocietyMatches(@Path("id") id: Int): ApiResponse<List<Match>>

    @GET("api/v1/societies/mine")
    suspend fun getMySocieties(): ApiResponse<List<MySociety>>
```

- [ ] **Step 3: Verify the app still compiles**

Do NOT run `gradlew assembleDebug` (per project rules — the user builds via Android Studio). Instead, visually double-check: `Match(...)` is constructed with positional/named args elsewhere (e.g., `CaptainViewModel.kt` line 108: `Match(1, "Badminton", "confirmed", "Kanteerava Annex", "Indiranagar", "Today 6 PM", price = 400)`). Confirm this still compiles by counting positional args against the new field order — the mock data uses named `price =` for the last arg, and all fields before it are unchanged in order, so the new trailing fields (`visibility`, `societyId`, `inviteCode`, `maxPlayers`, `joinedPlayers`) all have defaults and don't break this call site.

- [ ] **Step 4: Commit**

```bash
git add "Vmsuserapp/app/src/main/java/com/example/vmsuser/models/Models.kt" "Vmsuserapp/app/src/main/java/com/example/vmsuser/network/ApiService.kt"
git commit -m "feat(app): add captain-create match models and Retrofit endpoints"
```

---

## Task 6: Android — repository and ViewModel

**Files:**
- Modify: `Vmsuserapp/app/src/main/java/com/example/vmsuser/data/CaptainRepository.kt`
- Modify: `Vmsuserapp/app/src/main/java/com/example/vmsuser/viewmodel/CaptainViewModel.kt`

- [ ] **Step 1: Add repository methods**

In `CaptainRepository.kt`, add these imports at the top (alongside the existing ones):

```kotlin
import com.example.vmsuser.models.CaptainCreateMatchRequest
import com.example.vmsuser.models.Match
import com.example.vmsuser.models.MySociety
```

Add these methods at the end of the `CaptainRepository` class (after `updatePayoutUpi`, before the closing `}`):

```kotlin
    suspend fun createMatch(request: CaptainCreateMatchRequest): Result<Match> = try {
        val res = api.captainCreateMatch(request)
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Could not create match."))
    } catch (e: Exception) { Log.e("CaptainRepo", "createMatch", e); Result.failure(e) }

    suspend fun getMySocieties(): Result<List<MySociety>> = try {
        val res = api.getMySocieties()
        if (res.success && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "Failed"))
    } catch (e: Exception) { Log.e("CaptainRepo", "getMySocieties", e); Result.failure(e) }
```

- [ ] **Step 2: Add ViewModel state and actions**

In `CaptainViewModel.kt`, add these imports (alongside the existing ones):

```kotlin
import com.example.vmsuser.models.CaptainCreateMatchRequest
import com.example.vmsuser.models.MySociety
```

Add new state flows after the existing `_updatingUpi` flow (after line 36):

```kotlin
    private val _mySocieties = MutableStateFlow<List<MySociety>>(emptyList())
    val mySocieties: StateFlow<List<MySociety>> = _mySocieties

    private val _creatingMatch = MutableStateFlow(false)
    val creatingMatch: StateFlow<Boolean> = _creatingMatch

    private val _createdMatch = MutableStateFlow<com.example.vmsuser.models.Match?>(null)
    val createdMatch: StateFlow<com.example.vmsuser.models.Match?> = _createdMatch
```

Add new functions after `updatePayoutUpi` (after line 100, before `private fun mockStats()`):

```kotlin
    fun clearCreatedMatch() { _createdMatch.value = null }

    fun loadMySocieties() {
        viewModelScope.launch {
            repo.getMySocieties()
                .onSuccess { _mySocieties.value = it }
                .onFailure { Log.w("CaptainVM", "loadMySocieties: ${it.message}") }
        }
    }

    fun createMatch(
        cartTypeId: Int,
        regionId: Int,
        maxPlayers: Int,
        visibility: String,
        societyId: Int? = null,
        skillLevel: String? = null,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _creatingMatch.value = true
            _error.value = null
            repo.createMatch(
                CaptainCreateMatchRequest(
                    cartTypeId = cartTypeId,
                    regionId = regionId,
                    maxPlayers = maxPlayers,
                    visibility = visibility,
                    societyId = societyId,
                    skillLevel = skillLevel,
                )
            )
                .onSuccess { _createdMatch.value = it; onSuccess() }
                .onFailure { _error.value = it.message ?: "Could not create match." }
            _creatingMatch.value = false
        }
    }
```

- [ ] **Step 3: Commit**

```bash
git add "Vmsuserapp/app/src/main/java/com/example/vmsuser/data/CaptainRepository.kt" "Vmsuserapp/app/src/main/java/com/example/vmsuser/viewmodel/CaptainViewModel.kt"
git commit -m "feat(app): add match creation and society-picker state to CaptainViewModel"
```

---

## Task 7: Android — wire up `CreateMatchTab` UI

**Files:**
- Modify: `Vmsuserapp/app/src/main/java/com/example/vmsuser/ui/screens/captain/CaptainDashboardScreen.kt`

This task replaces the no-op `CreateMatchTab` with working UI: tapping "Open match" or "Private" opens a bottom sheet with a sport picker, region picker, and max-players stepper; tapping "Society match" opens a society-picker bottom sheet first, then the same confirm sheet; tapping "Tournament" navigates to `Screen.Tournaments`.

The app has no existing plumbing for "the logged-in user's region as an int" (`User.region` in `Models.kt` is a display `String`, not an id), so rather than plumb that through, the confirm sheet lets the captain pick their region explicitly from `ApiService.getLocations()` — the same source already used during profile setup elsewhere in the app.

- [ ] **Step 1: Add sports/regions loading to `CaptainViewModel`**

In `CaptainViewModel.kt`, add these imports:

```kotlin
import com.example.vmsuser.models.LocationOption
import com.example.vmsuser.models.SportItem
import com.example.vmsuser.network.RetrofitClient
```

Add state and a loader function after `loadMySocieties` (from Task 6):

```kotlin
    private val _sports = MutableStateFlow<List<SportItem>>(emptyList())
    val sports: StateFlow<List<SportItem>> = _sports

    private val _regions = MutableStateFlow<List<LocationOption>>(emptyList())
    val regions: StateFlow<List<LocationOption>> = _regions

    fun loadSportsAndRegions() {
        viewModelScope.launch {
            try {
                val sportsRes = RetrofitClient.api.getSports()
                if (sportsRes.success && sportsRes.data != null) _sports.value = sportsRes.data
                val locationsRes = RetrofitClient.api.getLocations()
                if (locationsRes.success && locationsRes.data != null) _regions.value = locationsRes.data
            } catch (e: Exception) { Log.e("CaptainVM", "loadSportsAndRegions", e) }
        }
    }
```

- [ ] **Step 2: Rewrite `CreateMatchTab`**

In `CaptainDashboardScreen.kt`, add these imports at the top (alongside the existing ones):

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.vmsuser.models.MySociety
```

Replace the entire `CreateMatchTab` composable (lines 171–200) with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMatchTab(navController: NavController) {
    val vm: CaptainViewModel = viewModel()
    val sports by vm.sports.collectAsState()
    val regions by vm.regions.collectAsState()
    val mySocieties by vm.mySocieties.collectAsState()
    val creating by vm.creatingMatch.collectAsState()
    val createdMatch by vm.createdMatch.collectAsState()
    val error by vm.error.collectAsState()

    var sheetVisibility by remember { mutableStateOf<String?>(null) } // "OPEN" | "SOCIETY" | "PRIVATE"
    var showSocietyPicker by remember { mutableStateOf(false) }
    var selectedSocietyId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        vm.loadSportsAndRegions()
        vm.loadMySocieties()
    }

    LaunchedEffect(createdMatch) {
        createdMatch?.let {
            if (it.visibility != "PRIVATE") {
                navController.navigate(Screen.CaptainMatch.create(it.id))
                vm.clearCreatedMatch()
                sheetVisibility = null
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        listOf(
            Triple("Open match", "Anyone can join via the app", PlixoPrimaryLight),
            Triple("Society match", "Exclusive to your society members", BlockSkyBg),
            Triple("Tournament", "Official Plixo tournament format", BlockLilacBg),
            Triple("Private", "Invite-only with a code", PlixoSurface2),
        ).forEach { (title, desc, bg) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg, PlixoShape.Card)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        when (title) {
                            "Open match" -> sheetVisibility = "OPEN"
                            "Private" -> sheetVisibility = "PRIVATE"
                            "Society match" -> showSocietyPicker = true
                            "Tournament" -> navController.navigate(Screen.Tournaments.route)
                        }
                    }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
                    Text(desc, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = PlixoText3)
            }
        }
    }

    if (showSocietyPicker) {
        ModalBottomSheet(onDismissRequest = { showSocietyPicker = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Choose a society", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlixoText)
                Spacer(Modifier.height(12.dp))
                if (mySocieties.isEmpty()) {
                    Text("You're not a member of any society yet.", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
                } else {
                    mySocieties.forEach { society: MySociety ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSocietyId = society.id
                                    showSocietyPicker = false
                                    sheetVisibility = "SOCIETY"
                                }
                                .padding(vertical = 12.dp),
                        ) {
                            Text(society.name, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PlixoText)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (sheetVisibility != null) {
        val visibility = sheetVisibility!!
        ModalBottomSheet(onDismissRequest = { sheetVisibility = null; vm.clearCreatedMatch() }) {
            if (createdMatch != null && createdMatch!!.visibility == "PRIVATE") {
                val clipboard = LocalClipboardManager.current
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Match created!", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlixoText)
                    Spacer(Modifier.height(8.dp))
                    Text("Share this code so others can join:", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        createdMatch!!.inviteCode ?: "",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = PlixoPrimary,
                    )
                    Spacer(Modifier.height(16.dp))
                    PlixoButton(
                        "Copy code",
                        onClick = { clipboard.setText(AnnotatedString(createdMatch!!.inviteCode ?: "")) },
                        variant = PlixoButtonVariant.Soft,
                    )
                    Spacer(Modifier.height(10.dp))
                    PlixoButton(
                        "Done",
                        onClick = {
                            navController.navigate(Screen.CaptainMatch.create(createdMatch!!.id))
                            vm.clearCreatedMatch()
                            sheetVisibility = null
                        },
                    )
                }
            } else {
                var selectedSportId by remember { mutableStateOf<Int?>(sports.firstOrNull()?.id) }
                var selectedRegionId by remember { mutableStateOf<Int?>(regions.firstOrNull()?.id) }
                var maxPlayers by remember { mutableStateOf(4) }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        when (visibility) { "PRIVATE" -> "Create private match"; "SOCIETY" -> "Create society match"; else -> "Create open match" },
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PlixoText,
                    )
                    Spacer(Modifier.height(16.dp))

                    Text("Sport", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sports.forEach { sport ->
                            SkillLevelChip(
                                level = sport.name,
                                selected = selectedSportId == sport.id,
                                onClick = { selectedSportId = sport.id },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("Region", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        regions.forEach { region ->
                            SkillLevelChip(
                                level = region.name,
                                selected = selectedRegionId == region.id,
                                onClick = { selectedRegionId = region.id },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Max players", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText, modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (maxPlayers > 2) maxPlayers-- }) { Icon(Icons.Filled.Remove, null) }
                        Text("$maxPlayers", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { if (maxPlayers < 22) maxPlayers++ }) { Icon(Icons.Filled.Add, null) }
                    }
                    Spacer(Modifier.height(16.dp))

                    error?.let {
                        Text(it, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                    }

                    PlixoButton(
                        if (creating) "Creating…" else "Create match",
                        onClick = {
                            val sportId = selectedSportId
                            val regionId = selectedRegionId
                            if (sportId != null && regionId != null) {
                                vm.createMatch(
                                    cartTypeId = sportId,
                                    regionId = regionId,
                                    maxPlayers = maxPlayers,
                                    visibility = visibility,
                                    societyId = if (visibility == "SOCIETY") selectedSocietyId else null,
                                ) {
                                    if (visibility != "PRIVATE") sheetVisibility = null
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Manual verification in the running app**

The user builds/installs via Android Studio (per project rules, do not run `gradlew assembleDebug`). Ask the user to:
1. Log in as a user with an ACTIVE captain profile.
2. Open Captain Mode → Create tab.
3. Tap "Open match" → pick a sport/region → Create match → confirm navigation to the match detail screen.
4. Tap "Private" → Create match → confirm the invite code is displayed and copyable.
5. Tap "Society match" → confirm the society picker shows societies the captain belongs to (or the empty state if none) → pick one → Create match.
6. Tap "Tournament" → confirm it navigates to the Tournaments screen.

- [ ] **Step 4: Commit**

```bash
git add "Vmsuserapp/app/src/main/java/com/example/vmsuser/ui/screens/captain/CaptainDashboardScreen.kt" "Vmsuserapp/app/src/main/java/com/example/vmsuser/viewmodel/CaptainViewModel.kt"
git commit -m "feat(app): wire CreateMatchTab to captain-create match flow"
```

---

## Task 8: Update DEV_LOG.md

**Files:**
- Modify: `backend/DEV_LOG.md`

- [ ] **Step 1: Append the dev log entry**

Read the end of `backend/DEV_LOG.md` first to match its existing entry format, then append (never overwrite) an entry covering:
- Date: 2026-07-06 (or the actual date this task is executed)
- Phase tag: Phase 02
- Added: `Match.visibility`/`society_id`/`invite_code` columns + migration 21; `MatchRepository.create_captain_match`/`find_society_matches`/`find_by_invite_code`; `MatchService.captain_create_match`/`join_by_code`; `POST /api/v1/matches/captain-create`; `POST /api/v1/matches/join-by-code`; `GET /api/v1/societies/{id}/matches`; `GET /api/v1/societies/mine`; `SocietyMemberRepository.find_by_user`; `SocietyMemberService.get_my_societies`
- Modified: `MatchRepository.find_waiting_in_region` (now filters `visibility == OPEN`); `Vmsuserapp` `Match` model (new fields), `ApiService.kt`, `CaptainRepository.kt`, `CaptainViewModel.kt`, `CaptainDashboardScreen.kt` (`CreateMatchTab` wired up)
- Removed: nothing
- Architectural decision: Tournament option in `CreateMatchTab` redirects to the existing tournament flow rather than building a new backend concept, since tournaments are organizer-created bracket entities, not something a captain spins up instantly like a match — reason documented in `docs/superpowers/specs/2026-07-06-captain-created-matches-design.md`.

- [ ] **Step 2: Commit**

```bash
git add backend/DEV_LOG.md
git commit -m "docs: log captain-created matches feature in DEV_LOG"
```

---

## Self-Review Notes

- **Spec coverage:** All spec sections (data model, captain role, 5 endpoints, app wiring, error handling) map to Tasks 1–7. Tournament redirect is covered in Task 7 Step 2 (`Screen.Tournaments.route` navigation).
- **Type consistency:** `visibility` is a `String` end-to-end (backend `str` column/schema, Kotlin `String`) — no enum class introduced on either side, matching the existing `status`/`skill_level` string-based convention in this codebase.
- **Known gap flagged inline:** Task 7 Step 1 discovered that the app has no existing plumbing for "the logged-in user's region as an int" — resolved by having the captain pick their region explicitly from `getLocations()` in the confirm sheet, consistent with how region selection already works during profile setup.
