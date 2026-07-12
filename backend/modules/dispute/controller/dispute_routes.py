from fastapi import APIRouter, Depends, HTTPException
from modules.audit.service.audit_service import audit_service
from modules.auth.dependencies.auth_dependencies import require_role, require_user
from modules.dispute.schemas.dispute_schema import CreateDisputeSchema, UpdateDisputeSchema
from modules.dispute.service.dispute_service import DisputeService
from modules.dispute.service.dispute_message_service import dispute_message_service
from modules.user.model.user_model import UserRole

router = APIRouter(prefix="/api/v1/disputes", tags=["Disputes"])
dispute_service = DisputeService()


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/mine")
def list_my_disputes(current_user: dict = Depends(require_user)):
    """Return the authenticated user's own support tickets."""
    return _success(dispute_service.list_my_disputes(current_user["id"]))


@router.post("/mine", status_code=201)
def create_my_dispute(
    request_data: dict,
    current_user: dict = Depends(require_user),
):
    """Raise a support ticket as the authenticated user (self-service, no role required)."""
    request_data["user_id"] = current_user["id"]
    request_data["raised_by"] = current_user["id"]
    request_data.pop("booking_id", None)
    schema = CreateDisputeSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        d = dispute_service.create_dispute(schema.validated_data)
        return _success(d, "Support ticket raised. We'll get back to you soon.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_disputes(
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    return _success(dispute_service.list_disputes())


@router.post("", status_code=201)
def create_dispute(
    request_data: dict,
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    request_data["raised_by"] = current_user["id"]
    schema = CreateDisputeSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        d = dispute_service.create_dispute(schema.validated_data)
        return _success(d, "Dispute raised successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{dispute_id}")
def get_dispute(
    dispute_id: int,
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    d = dispute_service.get_dispute(dispute_id)
    if not d:
        raise HTTPException(status_code=404, detail="Dispute not found.")
    return _success(d)


@router.put("/{dispute_id}")
def update_dispute(
    dispute_id: int,
    request_data: dict,
    current_user: dict = require_role(UserRole.SUPPORT, UserRole.OPS_MANAGER, UserRole.SUPER_ADMIN),
):
    schema = UpdateDisputeSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        d = dispute_service.update_dispute(dispute_id, schema.validated_data)
        if schema.validated_data.get("status") in ("RESOLVED", "CLOSED"):
            audit_service.log(
                action="DISPUTE_RESOLVED",
                actor_user_id=current_user["id"],
                target_resource_type="dispute",
                target_resource_id=dispute_id,
                details={"status": schema.validated_data.get("status")},
            )
        return _success(d, "Dispute updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.get("/{dispute_id}/messages")
def list_dispute_messages(dispute_id: int, current_user: dict = Depends(require_user)):
    """Reply thread on a ticket. Accessible to whoever raised it, plus support/admin staff."""
    try:
        return _success(dispute_message_service.list_messages(dispute_id, current_user["id"], current_user["role"]))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except PermissionError as e:
        raise HTTPException(status_code=403, detail=str(e))


@router.post("/{dispute_id}/messages", status_code=201)
def send_dispute_message(dispute_id: int, request_data: dict, current_user: dict = Depends(require_user)):
    try:
        message = dispute_message_service.send_message(
            dispute_id, current_user["id"], current_user["role"], request_data.get("body", "")
        )
        return _success(message, "Message sent.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except PermissionError as e:
        raise HTTPException(status_code=403, detail=str(e))
