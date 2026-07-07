import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.society.model.society_model import Society  # noqa: F401
from modules.society.model.society_member_model import SocietyMember, SocietyRole  # noqa: F401
from modules.tournament.model.player_score_model import PlayerScore  # noqa: F401
import modules.user.model.user_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401

from modules.society.repository.society_repository import SocietyRepository
from modules.society.repository.society_member_repository import SocietyMemberRepository
from modules.tournament.repository.player_score_repository import PlayerScoreRepository
from modules.user.repository.user_repository import UserRepository
from modules.society.service.society_member_service import SocietyMemberService


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestSocietyMemberService(unittest.TestCase):

    # ------------------------------------------------------------------
    # Setup
    # ------------------------------------------------------------------

    def setUp(self) -> None:
        factory = _factory()
        self.s_repo = SocietyRepository(session_factory=factory)
        self.m_repo = SocietyMemberRepository(session_factory=factory)
        self.ps_repo = PlayerScoreRepository(session_factory=factory)
        self.u_repo = UserRepository(session_factory=factory)
        self.service = SocietyMemberService(
            society_repository=self.s_repo,
            member_repository=self.m_repo,
            player_score_repository=self.ps_repo,
            user_repository=self.u_repo,
        )

        # Create a default public, active society with owner_user_id=1
        self.society: dict = self.s_repo.create({
            "name": "Test Society",
            "owner_user_id": 1,
            "region_id": 1,
            "sport_id": 1,
            "is_public": True,
            "max_members": 50,
        })
        self.m_repo.add_member(self.society["id"], 1, SocietyRole.OWNER)

    def _add_member(self, user_id: int) -> dict:
        """Helper: add a plain MEMBER to the default society."""
        return self.m_repo.add_member(self.society["id"], user_id, SocietyRole.MEMBER)

    # ------------------------------------------------------------------
    # join
    # ------------------------------------------------------------------

    def test_join_success(self) -> None:
        """user_id=2 joins the public society → returns member dict with MEMBER role."""
        result = self.service.join(self.society["id"], user_id=2)
        self.assertEqual(result["user_id"], 2)
        self.assertEqual(result["role"], SocietyRole.MEMBER)

    def test_join_already_member_raises(self) -> None:
        """Joining a second time raises ValueError('Already a member.')."""
        self.service.join(self.society["id"], user_id=2)
        with self.assertRaises(ValueError) as ctx:
            self.service.join(self.society["id"], user_id=2)
        self.assertIn("Already a member", str(ctx.exception))

    def test_join_private_society_raises(self) -> None:
        """Trying to join a private society raises ValueError."""
        private_society = self.s_repo.create({
            "name": "Private Society",
            "owner_user_id": 1,
            "region_id": 1,
            "sport_id": 1,
            "is_public": False,
            "max_members": 50,
        })
        with self.assertRaises(ValueError) as ctx:
            self.service.join(private_society["id"], user_id=2)
        self.assertIn("private", str(ctx.exception))

    def test_join_over_capacity_raises(self) -> None:
        """With max_members=1 and owner already there, joining raises ValueError."""
        small_society = self.s_repo.create({
            "name": "Small Society",
            "owner_user_id": 1,
            "region_id": 1,
            "sport_id": 1,
            "is_public": True,
            "max_members": 1,
        })
        self.m_repo.add_member(small_society["id"], 1, SocietyRole.OWNER)
        with self.assertRaises(ValueError) as ctx:
            self.service.join(small_society["id"], user_id=2)
        self.assertIn("full capacity", str(ctx.exception))

    # ------------------------------------------------------------------
    # leave
    # ------------------------------------------------------------------

    def test_leave_success(self) -> None:
        """A MEMBER who joined can leave → returns True."""
        self.service.join(self.society["id"], user_id=2)
        result = self.service.leave(self.society["id"], user_id=2)
        self.assertTrue(result)

    def test_leave_owner_raises(self) -> None:
        """Society OWNER cannot leave without transferring ownership first."""
        with self.assertRaises(ValueError) as ctx:
            self.service.leave(self.society["id"], user_id=1)
        self.assertIn("Transfer ownership", str(ctx.exception))

    def test_leave_not_member_raises(self) -> None:
        """Non-member trying to leave raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.leave(self.society["id"], user_id=99)
        self.assertIn("Not a member", str(ctx.exception))

    # ------------------------------------------------------------------
    # kick
    # ------------------------------------------------------------------

    def test_kick_by_owner(self) -> None:
        """OWNER can kick a MEMBER → returns True."""
        self._add_member(2)
        result = self.service.kick(
            self.society["id"],
            target_user_id=2,
            requester_id=1,
            requester_role="user",
        )
        self.assertTrue(result)

    def test_kick_owner_raises(self) -> None:
        """Attempting to kick the OWNER raises ValueError('Cannot kick the society owner.')."""
        # Add user 2 as a member and try to kick user 1 (OWNER) via super_admin
        self._add_member(2)
        with self.assertRaises(ValueError) as ctx:
            self.service.kick(
                self.society["id"],
                target_user_id=1,
                requester_id=2,
                requester_role="super_admin",
            )
        self.assertIn("Cannot kick the society owner", str(ctx.exception))

    def test_kick_by_non_owner_raises(self) -> None:
        """A plain MEMBER (not OWNER, not SUPER_ADMIN) cannot kick → ValueError."""
        self._add_member(2)
        self._add_member(3)
        with self.assertRaises(ValueError) as ctx:
            self.service.kick(
                self.society["id"],
                target_user_id=3,
                requester_id=2,
                requester_role="user",
            )
        self.assertIn("Permission denied", str(ctx.exception))

    def test_kick_by_super_admin(self) -> None:
        """SUPER_ADMIN can kick even without being a society member."""
        self._add_member(2)
        result = self.service.kick(
            self.society["id"],
            target_user_id=2,
            requester_id=999,  # not a society member
            requester_role="super_admin",
        )
        self.assertTrue(result)

    # ------------------------------------------------------------------
    # transfer_ownership
    # ------------------------------------------------------------------

    def test_transfer_ownership_success(self) -> None:
        """Owner transfers to existing member → new member gets OWNER, old gets MEMBER."""
        self._add_member(2)
        result = self.service.transfer_ownership(
            self.society["id"], new_owner_id=2, requester_id=1
        )
        self.assertEqual(result["role"], SocietyRole.OWNER)
        self.assertEqual(result["user_id"], 2)

        # Confirm old owner is now MEMBER
        old_owner = self.m_repo.find_member(self.society["id"], 1)
        self.assertIsNotNone(old_owner)
        self.assertEqual(old_owner["role"], SocietyRole.MEMBER)  # type: ignore[index]

    def test_transfer_ownership_not_owner_raises(self) -> None:
        """A plain MEMBER trying to transfer raises ValueError."""
        self._add_member(2)
        with self.assertRaises(ValueError) as ctx:
            self.service.transfer_ownership(
                self.society["id"], new_owner_id=1, requester_id=2
            )
        self.assertIn("Only the current owner", str(ctx.exception))

    def test_transfer_ownership_non_member_target_raises(self) -> None:
        """Transferring to a user who is not a member raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            self.service.transfer_ownership(
                self.society["id"], new_owner_id=99, requester_id=1
            )
        self.assertIn("must already be a member", str(ctx.exception))

    # ------------------------------------------------------------------
    # get_leaderboard
    # ------------------------------------------------------------------

    def test_get_leaderboard_returns_sorted(self) -> None:
        """Leaderboard is sorted by total_points DESC; member with 0 pts comes last."""
        self._add_member(2)
        self._add_member(3)

        # Give user 3 more points than user 2
        self.ps_repo.add_points(user_id=3, region_id=1, sport_id=1, points=100)
        self.ps_repo.add_points(user_id=2, region_id=1, sport_id=1, points=50)

        board = self.service.get_leaderboard(self.society["id"])

        self.assertIsInstance(board, list)
        # All 3 members (owner + 2) should appear
        self.assertEqual(len(board), 3)

        # First entry must have highest points
        self.assertEqual(board[0]["user_id"], 3)
        self.assertEqual(board[0]["total_points"], 100)

        # Second entry
        self.assertEqual(board[1]["user_id"], 2)
        self.assertEqual(board[1]["total_points"], 50)

        # Owner has no scores → 0 points, last
        self.assertEqual(board[2]["total_points"], 0)

        # All entries have the society_member_role key
        for entry in board:
            self.assertIn("society_member_role", entry)


if __name__ == "__main__":
    unittest.main()
