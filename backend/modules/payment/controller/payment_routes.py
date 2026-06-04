from fastapi import APIRouter, Body, Depends, HTTPException, Query, status
from modules.auth.dependencies.auth_dependencies import (
    require_role,
    require_user,
)
from modules.user.model.user_model import UserRole
from modules.payment.service.payment_service import PaymentService
from modules.booking.repository.booking_repository import booking_repository


router = APIRouter(prefix="/api/v1/payments", tags=["Payments"])

payment_service = PaymentService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


def _assert_booking_owner(booking_id: int, current_user: dict) -> None:
    """Raise 403 if the authenticated user does not own the booking."""
    booking = booking_repository.find_by_id(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")
    if booking.get("user_id") != current_user.get("id"):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only manage payments for your own bookings.",
        )


# ── Endpoints ─────────────────────────────────────────────────────────


@router.get("/summary")
def get_payment_summary(
    current_user: dict = require_role(UserRole.FINANCE, UserRole.SUPER_ADMIN),
):
    """Return aggregate payment statistics. Restricted to FINANCE and SUPER_ADMIN."""
    try:
        summary = payment_service.get_summary()
        return _success(summary)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/")
def list_payments(
    status: str = Query(
        None, description="Filter by payment status, e.g. UNDER_REVIEW"
    ),
    current_user: dict = require_role(
        UserRole.FINANCE, UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER, UserRole.SUPPORT
    ),
):
    """List all payments, optionally filtered by status. Admin only."""
    payments = payment_service.get_all_payments(status=status)
    return _success(payments, "Payments retrieved.")


@router.post("/initiate/{booking_id}", status_code=201)
def initiate_payment(booking_id: int, current_user: dict = Depends(require_user)):
    """Initiate a manual UPI payment for a booking. Requires user role."""
    _assert_booking_owner(booking_id, current_user)
    try:
        result = payment_service.initiate_payment(booking_id)
        return _success(
            result,
            "Payment initiated. Please pay via UPI and submit your transaction ID.",
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/confirm-manual/{booking_id}")
def confirm_manual_payment(
    booking_id: int,
    request_data: dict,
    current_user: dict = Depends(require_user),
):
    """Submit a UPI transaction ID for admin review. Requires user role."""
    _assert_booking_owner(booking_id, current_user)
    transaction_id = request_data.get("transaction_id")
    try:
        payment = payment_service.submit_manual_confirmation(booking_id, transaction_id)
        return _success(payment, "Payment submitted for admin review.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/approve/{payment_id}")
def approve_payment(
    payment_id: int,
    current_user: dict = require_role(UserRole.FINANCE, UserRole.SUPER_ADMIN),
):
    """Approve a payment under review. Admin only."""
    try:
        payment = payment_service.approve_payment(payment_id)
        return _success(payment, "Payment approved.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/reject/{payment_id}")
def reject_payment(
    payment_id: int,
    current_user: dict = require_role(UserRole.FINANCE, UserRole.SUPER_ADMIN),
):
    """Reject a payment under review. Admin only."""
    try:
        payment = payment_service.reject_payment(payment_id)
        return _success(payment, "Payment rejected.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/refund/{payment_id}")
def refund_payment(
    payment_id: int,
    request_data: dict = Body(default=None),
    current_user: dict = require_role(UserRole.FINANCE, UserRole.SUPER_ADMIN),
):
    """Initiate a refund for a successful payment. Admin only."""
    amount = request_data.get("amount") if request_data else None
    try:
        payment = payment_service.process_refund(payment_id, amount)
        return _success(payment, "Refund processed.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/booking/{booking_id}")
def get_payment_by_booking(
    booking_id: int,
    current_user: dict = require_role(
        UserRole.FINANCE, UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER, UserRole.SUPPORT
    ),
):
    """Retrieve payment details for a booking. Admin only."""
    payment = payment_service.get_payment_by_booking_id(booking_id)
    if not payment:
        raise HTTPException(
            status_code=404, detail="No payment found for this booking."
        )
    return _success(payment)


@router.get("/config")
def get_payment_config(current_user: dict = require_role(UserRole.SUPER_ADMIN)):
    """Get active UPI payment configuration (Admin only)."""
    config = payment_service.get_admin_payment_config()
    return _success(config, "Payment configuration retrieved.")


@router.put("/config")
def update_payment_config(
    request_data: dict,
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    """Update active UPI payment configuration (Admin only)."""
    upi_id = request_data.get("upi_id")
    merchant_name = request_data.get("merchant_name")

    if upi_id is None and merchant_name is None:
        raise HTTPException(
            status_code=400, detail="Must provide upi_id or merchant_name to update."
        )

    try:
        updated_config = payment_service.update_admin_payment_config(
            upi_id, merchant_name
        )
        return _success(updated_config, "Payment configuration updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
