import unittest
from datetime import datetime, timedelta
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.tournament.model.sport_vote_model import SportVote  # noqa: F401
from modules.tournament.model.sport_vote_round_model import SportVoteRound  # noqa: F401
from modules.tournament.repository.sport_vote_repository import SportVoteRepository
from modules.tournament.repository.sport_vote_round_repository import SportVoteRoundRepository
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


class TestSportVoteService(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        self.vote_repo = SportVoteRepository(session_factory=factory)
        self.round_repo = SportVoteRoundRepository(session_factory=factory)
        self.cart_types = _FakeCartTypeRepo({"Badminton", "Tennis", "Cricket"})
        self.now = datetime(2026, 8, 1, 12, 0, 0)
        self.service = SportVoteService(
            vote_repository=self.vote_repo,
            round_repository=self.round_repo,
            cart_type_repo=self.cart_types,
            now_fn=lambda: self.now,
        )

    def _start_round(self, options=("Badminton", "Tennis"), hours_from_now=24):
        return self.service.create_round(list(options), self.now + timedelta(hours=hours_from_now))

    def test_no_active_round_state(self):
        state = self.service.get_state(1)
        self.assertEqual(state["status"], "NONE")
        self.assertEqual(state["options"], [])
        self.assertIsNone(state["my_vote"])

    def test_create_round_requires_active_sports(self):
        with self.assertRaises(ValueError):
            self.service.create_round(["Badminton", "Chess"], self.now + timedelta(hours=1))

    def test_create_round_requires_future_deadline(self):
        with self.assertRaises(ValueError):
            self.service.create_round(["Badminton", "Tennis"], self.now - timedelta(hours=1))

    def test_create_round_requires_valid_option_count(self):
        with self.assertRaises(ValueError):
            self.service.create_round(["Badminton"], self.now + timedelta(hours=1))

    def test_cast_vote_appears_in_results_including_zero_vote_options(self):
        self._start_round(("Badminton", "Tennis"))
        state = self.service.cast_vote(1, "Badminton")
        results_by_sport = {r["sport"]: r["votes"] for r in state["results"]}
        self.assertEqual(results_by_sport, {"Badminton": 1, "Tennis": 0})
        self.assertEqual(state["my_vote"], "Badminton")
        self.assertEqual(state["status"], "OPEN")

    def test_can_switch_to_a_zero_vote_option(self):
        self._start_round(("Badminton", "Tennis"))
        self.service.cast_vote(1, "Badminton")
        state = self.service.cast_vote(1, "Tennis")
        self.assertEqual(state["my_vote"], "Tennis")
        self.assertEqual(state["total_votes"], 1)

    def test_vote_rejected_outside_options(self):
        self._start_round(("Badminton", "Tennis"))
        with self.assertRaises(ValueError):
            self.service.cast_vote(1, "Cricket")

    def test_vote_rejected_when_no_round(self):
        with self.assertRaises(ValueError):
            self.service.cast_vote(1, "Badminton")

    def test_vote_rejected_after_deadline(self):
        self._start_round(("Badminton", "Tennis"), hours_from_now=1)
        self.now += timedelta(hours=2)
        with self.assertRaises(ValueError):
            self.service.cast_vote(1, "Badminton")

    def test_vote_rejected_after_admin_force_close(self):
        round_ = self._start_round(("Badminton", "Tennis"))
        self.service.close_round(round_["round_id"])
        with self.assertRaises(ValueError):
            self.service.cast_vote(1, "Badminton")

    def test_winner_only_shown_when_closed(self):
        round_ = self._start_round(("Badminton", "Tennis"))
        self.service.cast_vote(1, "Badminton")
        open_state = self.service.get_state(1)
        self.assertIsNone(open_state["winner_sport"])

        self.service.close_round(round_["round_id"])
        closed_state = self.service.get_state(1)
        self.assertEqual(closed_state["status"], "CLOSED")
        self.assertEqual(closed_state["winner_sport"], "Badminton")

    def test_starting_new_round_replaces_current(self):
        self._start_round(("Badminton", "Tennis"))
        self.service.cast_vote(1, "Badminton")
        self._start_round(("Cricket", "Tennis"))
        state = self.service.get_state(1)
        self.assertEqual(state["options"], ["Cricket", "Tennis"])
        self.assertIsNone(state["my_vote"])  # fresh round, no vote yet
        self.assertEqual(state["total_votes"], 0)


if __name__ == "__main__":
    unittest.main()
