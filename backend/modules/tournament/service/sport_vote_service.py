from datetime import datetime

from modules.cart_type.repository.cart_type_repository import cart_type_repository
from modules.tournament.model.sport_vote_round_model import VoteRoundStatus
from modules.tournament.repository.sport_vote_repository import sport_vote_repository
from modules.tournament.repository.sport_vote_round_repository import sport_vote_round_repository

MIN_OPTIONS = 2
MAX_OPTIONS = 8


class SportVoteService:
    def __init__(self, vote_repository=None, round_repository=None, cart_type_repo=None, now_fn=None):
        self._votes = vote_repository or sport_vote_repository
        self._rounds = round_repository or sport_vote_round_repository
        self._cart_types = cart_type_repo or cart_type_repository
        self._now = now_fn or datetime.utcnow

    def _is_closed(self, round_: dict) -> bool:
        if round_["status"] == VoteRoundStatus.CLOSED:
            return True
        closes_at = round_["closes_at"]
        if isinstance(closes_at, str):
            closes_at = datetime.fromisoformat(closes_at)
        return self._now() >= closes_at

    def _build_state(self, round_: dict | None, user_id: int | None) -> dict:
        if round_ is None:
            return {
                "options": [], "results": [], "closes_at": None, "status": "NONE",
                "my_vote": None, "total_votes": 0, "winner_sport": None,
            }

        counts = self._votes.get_results(round_["id"])
        results = sorted(
            ({"sport": name, "votes": counts.get(name, 0)} for name in round_["options"]),
            key=lambda r: (-r["votes"], r["sport"]),
        )
        total_votes = sum(r["votes"] for r in results)
        closed = self._is_closed(round_)

        winner_sport = None
        if closed and total_votes > 0:
            winner_sport = results[0]["sport"]

        return {
            "round_id": round_["id"],
            "options": round_["options"],
            "results": results,
            "closes_at": round_["closes_at"],
            "status": "CLOSED" if closed else "OPEN",
            "my_vote": self._votes.get_my_vote(round_["id"], user_id) if user_id else None,
            "total_votes": total_votes,
            "winner_sport": winner_sport,
        }

    def get_state(self, user_id: int) -> dict:
        return self._build_state(self._rounds.get_current(), user_id)

    def cast_vote(self, user_id: int, sport_name: str) -> dict:
        sport_name = (sport_name or "").strip()
        if not sport_name:
            raise ValueError("'sport' is required.")

        round_ = self._rounds.get_current()
        if round_ is None:
            raise ValueError("There's no active vote right now.")
        if self._is_closed(round_):
            raise ValueError("Voting is closed for this round.")
        if sport_name not in round_["options"]:
            raise ValueError(f"'{sport_name}' is not one of this round's options.")

        self._votes.upsert_vote(round_["id"], user_id, sport_name)
        return self._build_state(round_, user_id)

    # ── Admin ────────────────────────────────────────────────────────

    def get_admin_state(self) -> dict:
        return self._build_state(self._rounds.get_current(), user_id=None)

    def create_round(self, options: list, closes_at: datetime) -> dict:
        if not isinstance(options, list) or not (MIN_OPTIONS <= len(options) <= MAX_OPTIONS):
            raise ValueError(f"Pick between {MIN_OPTIONS} and {MAX_OPTIONS} sports.")
        cleaned = []
        for name in options:
            name = str(name).strip()
            if not name or name in cleaned:
                raise ValueError("Sport options must be non-empty and unique.")
            sport = self._cart_types.find_by_name(name)
            if not sport or not sport.get("is_active", False):
                raise ValueError(f"'{name}' is not a currently active sport.")
            cleaned.append(name)

        if not isinstance(closes_at, datetime):
            raise ValueError("'closes_at' must be a valid date/time.")
        if closes_at <= self._now():
            raise ValueError("'closes_at' must be in the future.")

        round_ = self._rounds.create(cleaned, closes_at)
        return self._build_state(round_, user_id=None)

    def close_round(self, round_id: int) -> dict:
        round_ = self._rounds.close(round_id)
        if round_ is None:
            raise ValueError("Vote round not found.")
        return self._build_state(round_, user_id=None)


sport_vote_service = SportVoteService()
