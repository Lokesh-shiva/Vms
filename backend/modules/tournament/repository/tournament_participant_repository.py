from core.database.db_connection import SessionLocal


class TournamentParticipantRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        # Fully implemented in Task 4
        raise NotImplementedError

    def find_by_tournament_and_user(self, tournament_id: int, user_id: int) -> dict | None:
        # Fully implemented in Task 4
        raise NotImplementedError

    def update_status(self, tournament_id: int, user_id: int, status: str) -> dict | None:
        # Fully implemented in Task 4
        raise NotImplementedError

    def count_registered(self, tournament_id: int) -> int:
        # Fully implemented in Task 4
        raise NotImplementedError

    def find_by_team(self, team_id: int) -> list[dict]:
        # Fully implemented in Task 4
        raise NotImplementedError


tournament_participant_repository = TournamentParticipantRepository()
