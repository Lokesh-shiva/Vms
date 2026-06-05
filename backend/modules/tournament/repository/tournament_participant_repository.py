from core.database.db_connection import SessionLocal


class TournamentParticipantRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        raise NotImplementedError

    def find_by_tournament_and_user(self, tournament_id: int, user_id: int) -> dict | None:
        raise NotImplementedError

    def update_status(self, tournament_id: int, user_id: int, status: str) -> dict | None:
        raise NotImplementedError

    def count_registered(self, tournament_id: int) -> int:
        raise NotImplementedError

    def find_by_team(self, team_id: int) -> list[dict]:
        raise NotImplementedError


tournament_participant_repository = TournamentParticipantRepository()
