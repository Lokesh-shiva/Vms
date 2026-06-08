from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.society.model.society_model import Society


class SocietyRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            s = Society(
                name=data["name"],
                description=data.get("description"),
                owner_user_id=data["owner_user_id"],
                region_id=data["region_id"],
                sport_id=data["sport_id"],
                is_public=data.get("is_public", True),
                max_members=data.get("max_members", 50),
            )
            session.add(s)
            session.commit()
            session.refresh(s)
            return s.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, society_id: int) -> dict | None:
        session = self._session_factory()
        try:
            s = session.query(Society).filter(Society.id == society_id).first()
            return s.to_dict() if s else None
        finally:
            session.close()

    def find_all(
        self,
        region_id: int | None = None,
        sport_id: int | None = None,
        active_only: bool = True,
    ) -> list[dict]:
        session = self._session_factory()
        try:
            query = session.query(Society)
            if active_only:
                query = query.filter(Society.is_active == True)  # noqa: E712
            if region_id is not None:
                query = query.filter(Society.region_id == region_id)
            if sport_id is not None:
                query = query.filter(Society.sport_id == sport_id)
            query = query.order_by(Society.created_at.desc())
            return [s.to_dict() for s in query.all()]
        finally:
            session.close()

    def update(self, society_id: int, data: dict) -> dict | None:
        session = self._session_factory()
        _IMMUTABLE = {"id", "owner_user_id", "region_id", "sport_id", "created_at"}
        try:
            s = session.query(Society).filter(Society.id == society_id).first()
            if not s:
                return None
            for key, value in data.items():
                if key not in _IMMUTABLE and hasattr(s, key):
                    setattr(s, key, value)
            s.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(s)
            return s.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, society_id: int) -> bool:
        session = self._session_factory()
        try:
            s = session.query(Society).filter(Society.id == society_id).first()
            if not s:
                return False
            session.delete(s)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def count_by_owner(self, owner_user_id: int) -> int:
        """Return the number of societies owned by this user."""
        session = self._session_factory()
        try:
            return session.query(Society).filter(Society.owner_user_id == owner_user_id).count()
        finally:
            session.close()


society_repository = SocietyRepository()
