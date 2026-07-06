from core.database.db_connection import SessionLocal
from modules.society.model.society_member_model import SocietyMember


class SocietyMemberRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def add_member(self, society_id: int, user_id: int, role: str) -> dict:
        session = self._session_factory()
        try:
            member = SocietyMember(
                society_id=society_id,
                user_id=user_id,
                role=role,
            )
            session.add(member)
            session.commit()
            session.refresh(member)
            return member.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_member(self, society_id: int, user_id: int) -> dict | None:
        session = self._session_factory()
        try:
            member = (
                session.query(SocietyMember)
                .filter(
                    SocietyMember.society_id == society_id,
                    SocietyMember.user_id == user_id,
                )
                .first()
            )
            return member.to_dict() if member else None
        finally:
            session.close()

    def get_members(self, society_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            members = (
                session.query(SocietyMember)
                .filter(SocietyMember.society_id == society_id)
                .order_by(SocietyMember.joined_at.asc())
                .all()
            )
            return [m.to_dict() for m in members]
        finally:
            session.close()

    def remove_member(self, society_id: int, user_id: int) -> bool:
        session = self._session_factory()
        try:
            member = (
                session.query(SocietyMember)
                .filter(
                    SocietyMember.society_id == society_id,
                    SocietyMember.user_id == user_id,
                )
                .first()
            )
            if not member:
                return False
            session.delete(member)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def update_role(self, society_id: int, user_id: int, new_role: str) -> dict | None:
        session = self._session_factory()
        try:
            member = (
                session.query(SocietyMember)
                .filter(
                    SocietyMember.society_id == society_id,
                    SocietyMember.user_id == user_id,
                )
                .first()
            )
            if not member:
                return None
            member.role = new_role
            session.commit()
            session.refresh(member)
            return member.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def count_members(self, society_id: int) -> int:
        session = self._session_factory()
        try:
            return (
                session.query(SocietyMember)
                .filter(SocietyMember.society_id == society_id)
                .count()
            )
        finally:
            session.close()

    def find_by_user(self, user_id: int) -> list[dict]:
        """Return all society_member rows for a user (their society memberships)."""
        session = self._session_factory()
        try:
            rows = (
                session.query(SocietyMember)
                .filter(SocietyMember.user_id == user_id)
                .all()
            )
            return [m.to_dict() for m in rows]
        finally:
            session.close()


society_member_repository = SocietyMemberRepository()
