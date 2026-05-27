from datetime import datetime

from fastapi import HTTPException, status

from core.base.base_service import BaseService
from core.database.db_connection import SessionLocal
from modules.captain.model.captain_model import Captain, CaptainStatus
from modules.captain.repository.captain_repository import (
    captain_repository as _default_captain_repo,
)
from modules.user.model.user_model import User


def _merge_with_user(captain_dict: dict, session) -> dict:
    """Fetch name + phone from users table and merge into captain dict."""
    user = session.query(User).filter(User.id == captain_dict["user_id"]).first()
    result = dict(captain_dict)
    result["name"] = user.name if user else None
    result["phone"] = user.phone if user else None
    return result


class CaptainService(BaseService):
    """
    Business logic layer for Captain operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to the CaptainRepository.
    - Joins with the users table to enrich responses.
    - Returns formatted results to the controller.
    """

    def __init__(self, captain_repository=None, session_factory=None):
        super().__init__()
        self.captain_repository = captain_repository or _default_captain_repo
        self._session_factory = session_factory or SessionLocal

    # ── Queries ──────────────────────────────────────────────────────

    def list_captains(self, region_id: int | None = None) -> list[dict]:
        """
        Return all captains, optionally filtered by region.

        Enriches each record with name + phone from the users table.
        """
        session = self._session_factory()
        try:
            query = session.query(Captain)
            if region_id is not None:
                query = query.filter(Captain.region_id == region_id)
            captains = query.all()
            return [_merge_with_user(c.to_dict(), session) for c in captains]
        finally:
            session.close()

    def get_captain(self, captain_id: int) -> dict:
        """
        Retrieve a single captain by ID.

        Raises:
            HTTPException 404: If captain not found.
        """
        session = self._session_factory()
        try:
            captain = session.query(Captain).filter(Captain.id == captain_id).first()
            if not captain:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Captain not found.",
                )
            return _merge_with_user(captain.to_dict(), session)
        finally:
            session.close()

    # ── Mutations ────────────────────────────────────────────────────

    def create_captain(self, data: dict) -> dict:
        """
        Create a new captain record.

        Validates that the referenced user exists and is not already a captain.

        Raises:
            HTTPException 400: If user not found or already a captain.
        """
        session = self._session_factory()
        try:
            user_id: int = data["user_id"]

            # Validate user exists
            user = session.query(User).filter(User.id == user_id).first()
            if not user:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="User not found.",
                )

            # Prevent duplicate captain entries
            existing = (
                session.query(Captain).filter(Captain.user_id == user_id).first()
            )
            if existing:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="This user is already registered as a captain.",
                )

            captain = Captain(
                user_id=user_id,
                region_id=data.get("region_id"),
                status=CaptainStatus.ACTIVE,
                rating=0.0,
                total_trips=0,
                bio=data.get("bio"),
            )
            session.add(captain)
            session.commit()
            session.refresh(captain)
            result = _merge_with_user(captain.to_dict(), session)
            return result
        except HTTPException:
            session.rollback()
            raise
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def update_captain(self, captain_id: int, data: dict) -> dict:
        """
        Update an existing captain.

        Raises:
            HTTPException 404: If captain not found.
        """
        session = self._session_factory()
        try:
            captain = session.query(Captain).filter(Captain.id == captain_id).first()
            if not captain:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Captain not found.",
                )

            for key, value in data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(
                    captain, key
                ):
                    setattr(captain, key, value)

            captain.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(captain)
            return _merge_with_user(captain.to_dict(), session)
        except HTTPException:
            session.rollback()
            raise
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete_captain(self, captain_id: int) -> None:
        """
        Delete a captain by ID.

        Raises:
            HTTPException 404: If captain not found.
        """
        session = self._session_factory()
        try:
            captain = session.query(Captain).filter(Captain.id == captain_id).first()
            if not captain:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Captain not found.",
                )
            session.delete(captain)
            session.commit()
        except HTTPException:
            session.rollback()
            raise
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()
