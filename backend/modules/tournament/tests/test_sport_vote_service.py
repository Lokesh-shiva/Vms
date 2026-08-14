import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.tournament.model.sport_vote_model import SportVote  # noqa: F401
from modules.tournament.repository.sport_vote_repository import SportVoteRepository
from modules.tournament.service.sport_vote_service import SportVoteService
import modules.location.model.location_model  # noqa: F401
import modules.user.model.user_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class _FakeCartTypeRepo:
    def __init__(self, active_sports: set[str]):
        self._active = active_sports

    def find_by_name(self, name: str):
        if name in self._active:
            return {"name": name, "is_active": True}
        return None


class _FakeUserRepo:
    def __init__(self, region_by_user: dict[int, int | None]):
        self._regions = region_by_user

    def find_by_id(self, user_id: int):
        if user_id not in self._regions:
            return None
        return {"id": user_id, "region_id": self._regions[user_id]}


class TestSportVoteService(unittest.TestCase):
    def setUp(self):
        vote_repo = SportVoteRepository(session_factory=_factory())
        cart_type_repo = _FakeCartTypeRepo({"Badminton", "Tennis"})
        user_repo = _FakeUserRepo({1: 10, 2: 10, 3: 10, 4: None})
        self.service = SportVoteService(
            vote_repository=vote_repo, cart_type_repo=cart_type_repo, user_repo=user_repo,
        )

    def test_cast_vote_appears_in_results(self):
        state = self.service.cast_vote(1, "Badminton")
        self.assertEqual(state["results"], [{"sport": "Badminton", "votes": 1}])
        self.assertEqual(state["my_vote"], "Badminton")
        self.assertEqual(state["total_votes"], 1)

    def test_revote_moves_count_instead_of_duplicating(self):
        self.service.cast_vote(1, "Badminton")
        state = self.service.cast_vote(1, "Tennis")
        self.assertEqual(state["my_vote"], "Tennis")
        self.assertEqual(state["total_votes"], 1)
        sports_voted = {r["sport"] for r in state["results"]}
        self.assertEqual(sports_voted, {"Tennis"})

    def test_multiple_users_aggregate_correctly(self):
        self.service.cast_vote(1, "Badminton")
        self.service.cast_vote(2, "Badminton")
        state = self.service.cast_vote(3, "Tennis")
        results_by_sport = {r["sport"]: r["votes"] for r in state["results"]}
        self.assertEqual(results_by_sport["Badminton"], 2)
        self.assertEqual(results_by_sport["Tennis"], 1)
        self.assertEqual(state["total_votes"], 3)

    def test_user_with_no_region_rejected(self):
        with self.assertRaises(ValueError):
            self.service.cast_vote(4, "Badminton")

    def test_inactive_or_unknown_sport_rejected(self):
        with self.assertRaises(ValueError):
            self.service.cast_vote(1, "Underwater Basket Weaving")

    def test_empty_state_before_any_votes(self):
        state = self.service.get_state(1)
        self.assertEqual(state["results"], [])
        self.assertIsNone(state["my_vote"])
        self.assertEqual(state["total_votes"], 0)


if __name__ == "__main__":
    unittest.main()
