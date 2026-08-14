from modules.cart_type.repository.cart_type_repository import cart_type_repository
from modules.tournament.repository.sport_vote_repository import sport_vote_repository
from modules.user.repository.user_repository import user_repository


class SportVoteService:
    def __init__(self, vote_repository=None, cart_type_repo=None, user_repo=None):
        self._votes = vote_repository or sport_vote_repository
        self._cart_types = cart_type_repo or cart_type_repository
        self._users = user_repo or user_repository

    def _resolve_region(self, user_id: int) -> int:
        user = self._users.find_by_id(user_id)
        region_id = user.get("region_id") if user else None
        if not region_id:
            raise ValueError("Set your area in your profile before voting.")
        return region_id

    def get_state(self, user_id: int) -> dict:
        region_id = self._resolve_region(user_id)
        results = self._votes.get_results(region_id)
        my_vote = self._votes.get_my_vote(user_id, region_id)
        return {
            "results": results,
            "my_vote": my_vote,
            "total_votes": sum(r["votes"] for r in results),
        }

    def cast_vote(self, user_id: int, sport_name: str) -> dict:
        sport_name = (sport_name or "").strip()
        if not sport_name:
            raise ValueError("'sport' is required.")

        sport = self._cart_types.find_by_name(sport_name)
        if not sport or not sport.get("is_active", False):
            raise ValueError(f"'{sport_name}' is not a currently active sport.")

        region_id = self._resolve_region(user_id)
        self._votes.upsert_vote(user_id, region_id, sport_name)
        return self.get_state(user_id)


sport_vote_service = SportVoteService()
