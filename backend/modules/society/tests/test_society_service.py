import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base

import modules.user.model.user_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
from modules.society.model.society_model import Society  # noqa: F401
from modules.society.model.society_member_model import SocietyMember, SocietyRole  # noqa: F401
from modules.society.repository.society_repository import SocietyRepository
from modules.society.repository.society_member_repository import SocietyMemberRepository
from modules.society.service.society_service import SocietyService


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestSocietyService(unittest.TestCase):

    def setUp(self) -> None:
        factory = _factory()
        s_repo = SocietyRepository(session_factory=factory)
        m_repo = SocietyMemberRepository(session_factory=factory)
        self.service = SocietyService(repository=s_repo, member_repository=m_repo)

        self._base_data = {
            "name": "Test Society",
            "region_id": 1,
            "sport_id": 1,
        }

    # ------------------------------------------------------------------
    # create
    # ------------------------------------------------------------------

    def test_create_society(self) -> None:
        """Happy path: society returned with expected name; owner auto-added as OWNER."""
        result = self.service.create(dict(self._base_data), owner_user_id=10)
        self.assertEqual(result["name"], "Test Society")
        self.assertEqual(result["owner_user_id"], 10)

        member = self.service.member_repository.find_member(result["id"], 10)
        self.assertIsNotNone(member)
        self.assertEqual(member["role"], SocietyRole.OWNER)

    def test_create_missing_name_raises(self) -> None:
        """Empty name raises ValueError."""
        data = {"name": "   ", "region_id": 1, "sport_id": 1}
        with self.assertRaises(ValueError):
            self.service.create(data, owner_user_id=1)

    def test_create_missing_region_raises(self) -> None:
        """Missing region_id raises ValueError."""
        data = {"name": "Society", "region_id": None, "sport_id": 1}
        with self.assertRaises(ValueError):
            self.service.create(data, owner_user_id=1)

    def test_create_missing_sport_raises(self) -> None:
        """Missing sport_id raises ValueError."""
        data = {"name": "Society", "region_id": 1, "sport_id": None}
        with self.assertRaises(ValueError):
            self.service.create(data, owner_user_id=1)

    # ------------------------------------------------------------------
    # get_by_id
    # ------------------------------------------------------------------

    def test_get_by_id_success(self) -> None:
        created = self.service.create(dict(self._base_data), owner_user_id=5)
        fetched = self.service.get_by_id(created["id"])
        self.assertEqual(fetched["id"], created["id"])
        self.assertEqual(fetched["name"], "Test Society")

    def test_get_by_id_not_found_raises(self) -> None:
        with self.assertRaises(ValueError) as ctx:
            self.service.get_by_id(99999)
        self.assertIn("Society not found", str(ctx.exception))

    # ------------------------------------------------------------------
    # list
    # ------------------------------------------------------------------

    def test_list_no_filters(self) -> None:
        self.service.create(dict(self._base_data), owner_user_id=1)
        self.service.create(
            {"name": "Another Society", "region_id": 2, "sport_id": 2},
            owner_user_id=2,
        )
        results = self.service.list()
        self.assertIsInstance(results, list)
        self.assertEqual(len(results), 2)

    def test_list_with_filters(self) -> None:
        """Filter by region_id returns only matching society."""
        self.service.create(
            {"name": "Region1 Society", "region_id": 10, "sport_id": 1},
            owner_user_id=1,
        )
        self.service.create(
            {"name": "Region2 Society", "region_id": 20, "sport_id": 1},
            owner_user_id=2,
        )
        results = self.service.list(region_id=10)
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["name"], "Region1 Society")

    # ------------------------------------------------------------------
    # update
    # ------------------------------------------------------------------

    def test_update_by_owner(self) -> None:
        """Society OWNER can update the name."""
        society = self.service.create(dict(self._base_data), owner_user_id=7)
        updated = self.service.update(
            society["id"],
            {"name": "Updated Name"},
            requester_id=7,
            requester_role="user",
        )
        self.assertEqual(updated["name"], "Updated Name")

    def test_update_by_super_admin(self) -> None:
        """SUPER_ADMIN can update even without being a member."""
        society = self.service.create(dict(self._base_data), owner_user_id=7)
        updated = self.service.update(
            society["id"],
            {"name": "Admin Updated"},
            requester_id=999,
            requester_role="super_admin",
        )
        self.assertEqual(updated["name"], "Admin Updated")

    def test_update_by_non_owner_member_raises(self) -> None:
        """A MEMBER (not OWNER) cannot update → ValueError('Permission denied.')."""
        society = self.service.create(dict(self._base_data), owner_user_id=7)
        # add user 8 as a plain MEMBER
        self.service.member_repository.add_member(
            society["id"], 8, SocietyRole.MEMBER
        )
        with self.assertRaises(ValueError) as ctx:
            self.service.update(
                society["id"],
                {"name": "Should Fail"},
                requester_id=8,
                requester_role="user",
            )
        self.assertIn("Permission denied", str(ctx.exception))

    def test_update_max_members_below_count_raises(self) -> None:
        """Setting max_members below current member count raises ValueError."""
        society = self.service.create(dict(self._base_data), owner_user_id=7)
        # owner is already member (count=1); add another member to get count=2
        self.service.member_repository.add_member(
            society["id"], 8, SocietyRole.MEMBER
        )
        with self.assertRaises(ValueError) as ctx:
            self.service.update(
                society["id"],
                {"max_members": 1},  # 1 < current count of 2
                requester_id=7,
                requester_role="user",
            )
        self.assertIn("max_members cannot be less than current member count", str(ctx.exception))

    # ------------------------------------------------------------------
    # deactivate
    # ------------------------------------------------------------------

    def test_deactivate_by_ops_manager(self) -> None:
        """OPS_MANAGER can deactivate a society."""
        society = self.service.create(dict(self._base_data), owner_user_id=1)
        result = self.service.deactivate(society["id"], requester_role="ops_manager")
        self.assertFalse(result["is_active"])

    def test_deactivate_by_member_raises(self) -> None:
        """Regular member cannot deactivate → ValueError('Permission denied.')."""
        society = self.service.create(dict(self._base_data), owner_user_id=1)
        with self.assertRaises(ValueError) as ctx:
            self.service.deactivate(society["id"], requester_role="user")
        self.assertIn("Permission denied", str(ctx.exception))

    # ------------------------------------------------------------------
    # delete
    # ------------------------------------------------------------------

    def test_delete_by_super_admin(self) -> None:
        """SUPER_ADMIN can delete a society."""
        society = self.service.create(dict(self._base_data), owner_user_id=1)
        result = self.service.delete(society["id"], requester_role="super_admin")
        self.assertTrue(result)

    def test_delete_by_ops_manager_raises(self) -> None:
        """OPS_MANAGER cannot delete → ValueError('Permission denied.')."""
        society = self.service.create(dict(self._base_data), owner_user_id=1)
        with self.assertRaises(ValueError) as ctx:
            self.service.delete(society["id"], requester_role="ops_manager")
        self.assertIn("Permission denied", str(ctx.exception))

    # ------------------------------------------------------------------
    # permission gate
    # ------------------------------------------------------------------

    def test_create_without_permission_raises(self) -> None:
        """Regular user without flag cannot create."""
        with self.assertRaises(ValueError) as ctx:
            self.service.create(
                {"name": "Blocked Society", "region_id": 1, "sport_id": 1},
                owner_user_id=5,
                requester_role="user",
                can_create_society=False,
            )
        self.assertIn("permission", str(ctx.exception).lower())

    def test_create_with_permission_succeeds(self) -> None:
        """Regular user WITH flag can create."""
        result = self.service.create(
            {"name": "Permitted Society", "region_id": 1, "sport_id": 1},
            owner_user_id=5,
            requester_role="user",
            can_create_society=True,
        )
        self.assertEqual(result["name"], "Permitted Society")

    def test_create_second_society_raises(self) -> None:
        """User with flag cannot own more than 1 society."""
        self.service.create(
            {"name": "First Society", "region_id": 1, "sport_id": 1},
            owner_user_id=5,
            requester_role="user",
            can_create_society=True,
        )
        with self.assertRaises(ValueError) as ctx:
            self.service.create(
                {"name": "Second Society", "region_id": 1, "sport_id": 1},
                owner_user_id=5,
                requester_role="user",
                can_create_society=True,
            )
        self.assertIn("already own", str(ctx.exception).lower())


if __name__ == "__main__":
    unittest.main()
