from typing import Optional

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, Request, UploadFile
from fastapi.responses import Response
from pydantic import BaseModel, Field

from modules.audit.service.audit_service import audit_service
from modules.auth.dependencies.auth_dependencies import (
    get_current_user,
    require_role,
    require_user,
)
from modules.captain.schemas.captain_schema import CreateCaptainSchema, UpdateCaptainSchema
from modules.captain.service.captain_service import CaptainService
from modules.user.model.user_model import UserRole
from core.storage.kyc_storage import read_kyc_document


router = APIRouter(prefix="/api/v1/captains", tags=["Captains"])

captain_service = CaptainService()

_ADMIN_ROLES_FOR_REVIEW = (UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN)


class ReviewCaptainRequest(BaseModel):
    approve: bool
    reason: Optional[str] = Field(default=None)


class ApplyCaptainRequest(BaseModel):
    bio: Optional[str] = Field(default=None)


class UpdatePayoutUpiRequest(BaseModel):
    upi_id: str


class PayoutRequest(BaseModel):
    reference: Optional[str] = Field(default=None)


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Self-service onboarding (any authenticated user) ────────────────────


@router.post("/apply", status_code=201)
def apply_to_be_captain(
    request: ApplyCaptainRequest = ApplyCaptainRequest(),
    current_user: dict = Depends(require_user),
):
    """Start a captain application. Status begins at PENDING_REVIEW."""
    result = captain_service.apply(current_user["id"], bio=request.bio)
    return _success(result, "Application started. Upload your ID to continue.")


@router.post("/me/kyc")
async def upload_my_kyc(
    document_type: str = Form(...),
    file: UploadFile = File(...),
    current_user: dict = Depends(require_user),
):
    """Upload the KYC document for the caller's own captain application."""
    content = await file.read()
    result = captain_service.upload_kyc(
        current_user["id"], document_type.upper(), file.filename or "document.jpg", content
    )
    return _success(result, "Document submitted for review.")


@router.get("/me/application-status")
def get_my_application_status(current_user: dict = Depends(require_user)):
    """Poll the caller's own captain application status."""
    result = captain_service.get_my_application(current_user["id"])
    return _success(result)


@router.put("/me/payout-upi")
def update_my_payout_upi(
    request: UpdatePayoutUpiRequest,
    current_user: dict = Depends(require_user),
):
    """Set/update the UPI ID the captain's manual payouts should be sent to."""
    result = captain_service.update_payout_upi(current_user["id"], request.upi_id)
    return _success(result, "Payout UPI ID updated.")


@router.get("/payouts/pending")
def list_pending_payouts(
    current_user: dict = require_role(*_ADMIN_ROLES_FOR_REVIEW),
):
    """Admin: captains with an outstanding (unpaid) wallet balance."""
    return _success(captain_service.list_pending_payouts())


@router.post("/{captain_id}/payout")
def mark_captain_paid(
    captain_id: int,
    request: PayoutRequest,
    current_user: dict = require_role(*_ADMIN_ROLES_FOR_REVIEW),
):
    """Admin: mark a captain's outstanding balance as paid (manual UPI/bank transfer sent)."""
    result = captain_service.mark_paid(captain_id, request.reference)

    from modules.notification.service.notification_service import notification_service
    captain = captain_service.get_captain(captain_id)
    notification_service.send(
        user_id=captain["user_id"],
        title="Payout sent",
        body="Your captain earnings have been paid out.",
        type_="CAPTAIN_PAYOUT",
    )

    audit_service.log(
        action="CAPTAIN_PAYOUT",
        actor_user_id=current_user["id"],
        target_resource_type="captain",
        target_resource_id=captain_id,
        details={"reference": request.reference, "entries_paid": result["entries_paid"]},
    )
    return _success(result, "Payout recorded.")


# ── Endpoints ─────────────────────────────────────────────────────────


@router.get("/me/stats")
def get_my_captain_stats(current_user: dict = Depends(require_user)):
    """Return stats for the authenticated captain."""
    result = captain_service.get_my_stats(current_user["id"])
    return _success(result)


@router.get("")
def list_captains(
    region_id: Optional[int] = Query(None, description="Filter by region ID"),
    status: Optional[str] = Query(None, description="Filter by captain status"),
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """List all captains. Optionally filter by region_id and/or status."""
    captains = captain_service.list_captains(region_id=region_id, status_filter=status)
    return _success(captains)


@router.post("", status_code=201)
def create_captain(
    request_data: dict,
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """Register a user as a captain."""
    schema = CreateCaptainSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        captain = captain_service.create_captain(schema.validated_data)
        return _success(captain, "Captain created successfully.")
    except HTTPException:
        raise
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{captain_id}")
def get_captain(
    captain_id: int,
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """Retrieve a single captain by ID."""
    captain = captain_service.get_captain(captain_id)
    return _success(captain)


@router.put("/{captain_id}/review")
def review_captain(
    captain_id: int,
    request: ReviewCaptainRequest,
    current_user: dict = require_role(*_ADMIN_ROLES_FOR_REVIEW),
):
    """Approve or reject a pending captain application."""
    result = captain_service.review(captain_id, request.approve, request.reason)

    from modules.notification.service.notification_service import notification_service
    if request.approve:
        notification_service.send(
            user_id=result["user_id"],
            title="You're a Plixo Captain!",
            body="Your application was approved. You can now lead matches.",
            type_="CAPTAIN_APPROVED",
        )
    else:
        notification_service.send(
            user_id=result["user_id"],
            title="Captain application update",
            body=request.reason or "Your application was not approved this time.",
            type_="CAPTAIN_REJECTED",
        )

    audit_service.log(
        action="CAPTAIN_REVIEWED",
        actor_user_id=current_user["id"],
        target_resource_type="captain",
        target_resource_id=captain_id,
        details={"approved": request.approve, "reason": request.reason},
    )
    return _success(result, "Review recorded.")


def _decode_token_or_query(request: Request, token: Optional[str]) -> dict:
    """Accept either a Bearer header or a `?token=` query param — the latter is
    needed because <img>/AsyncImage requests can't attach custom headers."""
    from jose import JWTError, jwt
    from modules.auth.service.auth_service import SECRET_KEY, ALGORITHM
    from modules.user.repository.user_repository import user_repository

    auth_header = request.headers.get("Authorization", "")
    raw_token = auth_header.removeprefix("Bearer ").strip() if auth_header else token
    if not raw_token:
        raise HTTPException(status_code=401, detail="Missing authentication token.")
    try:
        payload = jwt.decode(raw_token, SECRET_KEY, algorithms=[ALGORITHM])
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token.")
    user = user_repository.find_by_id(int(payload.get("sub", 0)))
    if not user or user.get("role") not in {r.value for r in _ADMIN_ROLES_FOR_REVIEW}:
        raise HTTPException(status_code=403, detail="Admin privileges required.")
    return user


@router.get("/{captain_id}/kyc-document")
def get_kyc_document(
    captain_id: int,
    request: Request,
    token: Optional[str] = Query(None),
):
    """Stream a captain's uploaded KYC document. Admin-only."""
    _decode_token_or_query(request, token)
    captain = captain_service.get_captain(captain_id)
    if not captain.get("kyc_document_url"):
        raise HTTPException(status_code=404, detail="No KYC document uploaded.")
    content = read_kyc_document(captain["kyc_document_url"])
    if content is None:
        raise HTTPException(status_code=404, detail="Document file missing on server.")
    return Response(content=content, media_type="image/jpeg")


@router.put("/{captain_id}")
def update_captain(
    captain_id: int,
    request_data: dict,
    current_user: dict = require_role(UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    """Update an existing captain record."""
    schema = UpdateCaptainSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        captain = captain_service.update_captain(captain_id, schema.validated_data)
        if "status" in request_data:
            audit_service.log(
                action="CAPTAIN_STATUS_CHANGE",
                actor_user_id=current_user["id"],
                target_resource_type="captain",
                target_resource_id=captain_id,
                details={"new_status": request_data["status"]},
            )
        return _success(captain, "Captain updated successfully.")
    except HTTPException:
        raise
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{captain_id}")
def delete_captain(
    captain_id: int,
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    """Delete a captain. Restricted to SUPER_ADMIN."""
    try:
        captain_service.delete_captain(captain_id)
        return _success(None, "Captain deleted successfully.")
    except HTTPException:
        raise
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
