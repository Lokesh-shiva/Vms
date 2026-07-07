from modules.dispute.repository.dispute_repository import dispute_repository as _default_repo
from modules.dispute.model.dispute_model import DisputeStatus


class DisputeService:
    def __init__(self, repository=None):
        self.repository = repository or _default_repo

    def list_disputes(self) -> list[dict]:
        return self.repository.find_all()

    def list_my_disputes(self, user_id: int) -> list[dict]:
        return self.repository.find_by_raised_by(user_id)

    def get_dispute(self, dispute_id: int) -> dict | None:
        return self.repository.find_by_id(dispute_id)

    def create_dispute(self, data: dict) -> dict:
        if not data.get("title", "").strip():
            raise ValueError("Title is required.")
        if not data.get("description", "").strip():
            raise ValueError("Description is required.")
        return self.repository.create(data)

    def update_dispute(self, dispute_id: int, data: dict) -> dict:
        existing = self.repository.find_by_id(dispute_id)
        if not existing:
            raise ValueError(f"Dispute {dispute_id} not found.")
        if "status" in data and data["status"] not in DisputeStatus.ALL:
            raise ValueError(f"Invalid status. Must be one of: {DisputeStatus.ALL}")
        return self.repository.update(dispute_id, data)
