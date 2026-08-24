from modules.trainer.repository.trainer_repository import trainer_repository as _default_repo


class TrainerService:
    def __init__(self, trainer_repository=None):
        self._trainers = trainer_repository or _default_repo

    def _validate(self, data: dict) -> None:
        name = str(data.get("name", "")).strip()
        if not name:
            raise ValueError("'name' is required.")
        rate = data.get("rate_per_session")
        if rate is None or not isinstance(rate, (int, float)) or rate <= 0:
            raise ValueError("'rate_per_session' must be a positive number.")

    def create_trainer(self, data: dict) -> dict:
        self._validate(data)
        return self._trainers.create(data)

    def update_trainer(self, trainer_id: int, data: dict) -> dict:
        if not self._trainers.find_by_id(trainer_id):
            raise ValueError("Trainer not found.")
        if "name" in data or "rate_per_session" in data:
            merged = {**(self._trainers.find_by_id(trainer_id) or {}), **data}
            self._validate(merged)
        updated = self._trainers.update(trainer_id, data)
        return updated

    def delete_trainer(self, trainer_id: int) -> bool:
        if not self._trainers.find_by_id(trainer_id):
            raise ValueError("Trainer not found.")
        return self._trainers.delete(trainer_id)

    def get_trainer(self, trainer_id: int) -> dict:
        trainer = self._trainers.find_by_id(trainer_id)
        if not trainer:
            raise ValueError("Trainer not found.")
        return trainer

    def list_trainers(self, active_only: bool = False) -> list[dict]:
        return self._trainers.find_all(active_only)


trainer_service = TrainerService()
