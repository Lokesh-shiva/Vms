from modules.tournament.repository.tournament_repository import tournament_repository as _default_repo
from modules.tournament.model.tournament_model import TournamentStatus


class TournamentService:
    def __init__(self, repository=None):
        self.repository = repository or _default_repo

    def list_tournaments(self) -> list[dict]:
        return self.repository.find_all()

    def get_tournament(self, tournament_id: int) -> dict | None:
        return self.repository.find_by_id(tournament_id)

    def create_tournament(self, data: dict) -> dict:
        if not data.get("name", "").strip():
            raise ValueError("Tournament name is required.")
        if not data.get("organizer", "").strip():
            raise ValueError("Organizer is required.")
        if not data.get("start_date"):
            raise ValueError("start_date is required.")
        if not data.get("end_date"):
            raise ValueError("end_date is required.")
        if data["start_date"] > data["end_date"]:
            raise ValueError("start_date must be before end_date.")
        return self.repository.create(data)

    def update_tournament(self, tournament_id: int, data: dict) -> dict:
        existing = self.repository.find_by_id(tournament_id)
        if not existing:
            raise ValueError(f"Tournament {tournament_id} not found.")
        if "status" in data and data["status"] not in TournamentStatus.ALL:
            raise ValueError(f"Invalid status. Must be one of: {TournamentStatus.ALL}")
        return self.repository.update(tournament_id, data)

    def delete_tournament(self, tournament_id: int) -> bool:
        existing = self.repository.find_by_id(tournament_id)
        if not existing:
            raise ValueError(f"Tournament {tournament_id} not found.")
        return self.repository.delete(tournament_id)
