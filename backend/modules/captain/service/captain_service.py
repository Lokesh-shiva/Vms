from datetime import datetime, timedelta

from fastapi import HTTPException, status

from core.base.base_service import BaseService
from core.database.db_connection import SessionLocal
from core.storage.kyc_storage import save_kyc_document
from modules.captain.model.captain_model import Captain, CaptainStatus, KycDocumentType, KycStatus
from modules.captain.repository.captain_repository import (
    captain_repository as _default_captain_repo,
)
from modules.captain.repository.captain_earning_repository import (
    captain_earning_repository as _default_earning_repo,
)
from modules.payment.repository.system_config_repository import (
    system_config_repository as _default_system_config_repo,
)
from modules.user.model.user_model import User

DEFAULT_CAPTAIN_FEE_PER_MATCH = 200


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

    MIN_COMPLETED_MATCHES = 3

    def __init__(
        self,
        captain_repository=None,
        session_factory=None,
        match_repository=None,
        system_config_repository=None,
        earning_repository=None,
    ):
        super().__init__()
        self.captain_repository = captain_repository or _default_captain_repo
        self._session_factory = session_factory or SessionLocal
        self.system_config_repository = system_config_repository or _default_system_config_repo
        self.earning_repository = earning_repository or _default_earning_repo
        if match_repository is not None:
            self._match_repository = match_repository
        else:
            from modules.match.repository.match_repository import (
                match_repository as _default_match_repo,
            )
            self._match_repository = _default_match_repo

    # ── Queries ──────────────────────────────────────────────────────

    def list_captains(self, region_id: int | None = None, status_filter: str | None = None) -> list[dict]:
        """
        Return all captains, optionally filtered by region and/or status.

        Enriches each record with name + phone from the users table.
        """
        session = self._session_factory()
        try:
            query = session.query(Captain)
            if region_id is not None:
                query = query.filter(Captain.region_id == region_id)
            if status_filter is not None:
                query = query.filter(Captain.status == status_filter)
            captains = query.all()
            return [_merge_with_user(c.to_dict(), session) for c in captains]
        finally:
            session.close()

    def get_captain_fee(self) -> int:
        """Current flat fee per completed match — admin-editable via SystemConfig (CAPTAIN_FEE_PER_MATCH)."""
        fee_raw = self.system_config_repository.get("CAPTAIN_FEE_PER_MATCH")
        try:
            return int(fee_raw) if fee_raw is not None else DEFAULT_CAPTAIN_FEE_PER_MATCH
        except (TypeError, ValueError):
            return DEFAULT_CAPTAIN_FEE_PER_MATCH

    def record_match_earning(self, captain_id: int, match_id: int) -> dict:
        """Create a ledger entry for a completed match. Fee is snapshotted at creation time."""
        return self.earning_repository.create(captain_id, match_id, self.get_captain_fee())

    def get_my_stats(self, user_id: int) -> dict:
        """
        Return earnings + performance stats for the authenticated captain.

        today_earnings/week_earnings are activity metrics — total earned in that
        window regardless of payout status. wallet_balance is the actual amount
        still owed (unpaid ledger entries), which is what the app should show
        as the captain's withdrawable balance.
        """
        captain = self.captain_repository.get_by_user_id(user_id)
        if not captain:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Captain profile not found")

        now = datetime.utcnow()
        today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
        week_start = now - timedelta(days=7)

        return {
            "today_earnings": self.earning_repository.sum_since(captain["id"], today_start),
            "week_earnings": self.earning_repository.sum_since(captain["id"], week_start),
            "wallet_balance": self.earning_repository.sum_pending(captain["id"]),
            "payout_upi_id": captain.get("payout_upi_id"),
            "rating": captain["rating"],
            "matches_led": captain["total_trips"],
            "active_matches": [],
        }

    def update_payout_upi(self, user_id: int, upi_id: str) -> dict:
        """Self-service: captain sets/updates where their manual payouts should be sent."""
        captain = self.captain_repository.get_by_user_id(user_id)
        if not captain:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Captain profile not found")
        upi_id = upi_id.strip()
        if not upi_id:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="upi_id cannot be empty.")
        return self.update_captain(captain["id"], {"payout_upi_id": upi_id})

    def list_pending_payouts(self) -> list[dict]:
        """Admin: captains with a non-zero wallet balance, enriched with balance + payout details."""
        session = self._session_factory()
        try:
            captains = session.query(Captain).all()
            result = []
            for c in captains:
                balance = self.earning_repository.sum_pending(c.id)
                if balance > 0:
                    enriched = _merge_with_user(c.to_dict(), session)
                    enriched["wallet_balance"] = balance
                    result.append(enriched)
            return result
        finally:
            session.close()

    def mark_paid(self, captain_id: int, reference: str | None) -> dict:
        """Admin: mark all pending earnings for a captain as paid out."""
        captain = self.captain_repository.get_by_id(captain_id)
        if not captain:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Captain not found.")
        count = self.earning_repository.mark_captain_paid(captain_id, reference)
        return {"captain_id": captain_id, "entries_paid": count}

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

    # ── Self-service onboarding (manual-review KYC) ─────────────────────

    def apply(self, user_id: int, bio: str | None = None) -> dict:
        """Create a captain application in PENDING_REVIEW for the given user."""
        completed = len([
            m for m in self._match_repository.find_by_user(user_id) if m.get("status") == "COMPLETED"
        ])
        if completed < self.MIN_COMPLETED_MATCHES:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"You need at least {self.MIN_COMPLETED_MATCHES} completed matches to apply "
                       f"(you have {completed}).",
            )

        session = self._session_factory()
        try:
            existing = session.query(Captain).filter(Captain.user_id == user_id).first()
            if existing:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="You have already applied to be a captain.",
                )
            captain = Captain(
                user_id=user_id,
                bio=bio,
                status=CaptainStatus.PENDING_REVIEW,
                kyc_status=KycStatus.PENDING,
                verification_method="MANUAL",
            )
            session.add(captain)
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

    def upload_kyc(self, user_id: int, document_type: str, filename: str, content: bytes) -> dict:
        """Attach a KYC document to the caller's own captain application."""
        if document_type not in KycDocumentType.ALL:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"document_type must be one of {sorted(KycDocumentType.ALL)}.",
            )
        session = self._session_factory()
        try:
            captain = session.query(Captain).filter(Captain.user_id == user_id).first()
            if not captain:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="No captain application found. Apply first.",
                )
            path = save_kyc_document(captain.id, filename, content)
            captain.kyc_document_url = path
            captain.kyc_document_type = document_type
            captain.kyc_status = KycStatus.PENDING
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

    def get_my_application(self, user_id: int) -> dict:
        session = self._session_factory()
        try:
            captain = session.query(Captain).filter(Captain.user_id == user_id).first()
            if not captain:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="No captain application found.",
                )
            return _merge_with_user(captain.to_dict(), session)
        finally:
            session.close()

    def review(self, captain_id: int, approve: bool, reason: str | None = None) -> dict:
        """Admin approves or rejects a pending captain application."""
        session = self._session_factory()
        try:
            captain = session.query(Captain).filter(Captain.id == captain_id).first()
            if not captain:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Captain application not found.",
                )
            if approve:
                captain.status = CaptainStatus.ACTIVE
                captain.kyc_status = KycStatus.APPROVED
                captain.rejection_reason = None
            else:
                captain.status = CaptainStatus.REJECTED
                captain.kyc_status = KycStatus.REJECTED
                captain.rejection_reason = reason
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
