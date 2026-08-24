from core.database.db_connection import SessionLocal
from modules.trainer.model.trainer_model import Trainer


class TrainerRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            trainer = Trainer(
                name=data["name"],
                bio=data.get("bio", ""),
                specialties=data.get("specialties", ""),
                rate_per_session=data["rate_per_session"],
                image_url=data.get("image_url"),
                is_active=data.get("is_active", True),
            )
            session.add(trainer)
            session.commit()
            session.refresh(trainer)
            return trainer.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, trainer_id: int) -> dict | None:
        session = self._session_factory()
        try:
            trainer = session.query(Trainer).filter(Trainer.id == trainer_id).first()
            return trainer.to_dict() if trainer else None
        finally:
            session.close()

    def find_all(self, active_only: bool = False) -> list[dict]:
        session = self._session_factory()
        try:
            query = session.query(Trainer)
            if active_only:
                query = query.filter(Trainer.is_active.is_(True))
            return [t.to_dict() for t in query.order_by(Trainer.name).all()]
        finally:
            session.close()

    def update(self, trainer_id: int, update_data: dict) -> dict | None:
        session = self._session_factory()
        try:
            trainer = session.query(Trainer).filter(Trainer.id == trainer_id).first()
            if not trainer:
                return None
            for key, value in update_data.items():
                setattr(trainer, key, value)
            session.commit()
            session.refresh(trainer)
            return trainer.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, trainer_id: int) -> bool:
        session = self._session_factory()
        try:
            trainer = session.query(Trainer).filter(Trainer.id == trainer_id).first()
            if not trainer:
                return False
            session.delete(trainer)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


trainer_repository = TrainerRepository()
