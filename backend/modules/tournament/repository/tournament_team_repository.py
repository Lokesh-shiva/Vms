from core.database.db_connection import SessionLocal


class TournamentTeamRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        # Fully implemented in Task 4
        raise NotImplementedError

    def count_by_tournament(self, tournament_id: int) -> int:
        # Fully implemented in Task 4
        raise NotImplementedError


tournament_team_repository = TournamentTeamRepository()
