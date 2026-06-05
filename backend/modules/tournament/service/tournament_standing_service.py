from modules.tournament.repository.tournament_standing_repository import tournament_standing_repository as _default_s_repo
from modules.tournament.repository.player_score_repository import player_score_repository as _default_ps_repo


class TournamentStandingService:
    def __init__(self, standing_repository=None, player_score_repository=None):
        self._s_repo = standing_repository or _default_s_repo
        self._ps_repo = player_score_repository or _default_ps_repo

    def get_standings(self, tournament_id: int) -> list[dict]:
        """Return standings for a tournament sorted by rank (points desc)."""
        return self._s_repo.find_by_tournament(tournament_id)

    def get_global_leaderboard(self, region_id: int, sport_id: int, limit: int = 50) -> list[dict]:
        """Return global area leaderboard for a region+sport combination."""
        return self._ps_repo.get_leaderboard(region_id=region_id, sport_id=sport_id, limit=limit)


tournament_standing_service = TournamentStandingService()
