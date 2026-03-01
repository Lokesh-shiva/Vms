from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import (
    require_admin,
    require_user,
)
from modules.payment.service.payment_service import PaymentService


router = APIRouter(prefix="/api/v1/payments", tags=["Payments"])

payment_service = PaymentService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.post("/initiate/{booking_id}", status_code=201)
def initiate_payment(booking_id: int, current_user: dict = Depends(require_user)):
    """Initiate a manual UPI payment for a booking. Requires user role."""
    try:
        result = payment_service.initiate_payment(booking_id)
        return _success(result, "Payment initiated. Please pay via UPI and submit your transaction ID.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/confirm-manual/{booking_id}")
def confirm_manual_payment(
    booking_id: int,
    request_data: dict,
    current_user: dict = Depends(require_user),
):
    """Submit a UPI transaction ID for admin review. Requires user role."""
    transaction_id = request_data.get("transaction_id")
    try:
        payment = payment_service.submit_manual_confirmation(booking_id, transaction_id)
        return _success(payment, "Payment submitted for admin review.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/approve/{payment_id}")
def approve_payment(payment_id: int, current_user: dict = Depends(require_admin)):
    """Approve a payment under review. Admin only."""
    try:
        payment = payment_service.approve_payment(payment_id)
        return _success(payment, "Payment approved.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/reject/{payment_id}")
def reject_payment(payment_id: int, current_user: dict = Depends(require_admin)):
    """Reject a payment under review. Admin only."""
    try:
        payment = payment_service.reject_payment(payment_id)
        return _success(payment, "Payment rejected.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/refund/{payment_id}")
def refund_payment(
    payment_id: int,
    request_data: dict = None,
    current_user: dict = Depends(require_admin),
):
    """Initiate a refund for a successful payment. Admin only."""
    amount = None
    if request_data:
        amount = request_data.get("amount")
    try:
        payment = payment_service.process_refund(payment_id, amount)
        return _success(payment, "Refund processed.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/booking/{booking_id}")
def get_payment_by_booking(booking_id: int, current_user: dict = Depends(require_admin)):
    """Retrieve payment details for a booking. Admin only."""
    payment = payment_service.get_payment_by_booking_id(booking_id)
    if not payment:
        raise HTTPException(status_code=404, detail="No payment found for this booking.")
    return _success(payment)
