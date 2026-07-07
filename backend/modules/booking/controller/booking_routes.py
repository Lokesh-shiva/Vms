from fastapi import APIRouter, Depends, HTTPException
from modules.audit.service.audit_service import audit_service
from modules.auth.dependencies.auth_dependencies import (
    _ADMIN_ROLES,
    get_current_user,
    require_admin,
    require_role,
    require_user,
)
from modules.booking.service.booking_service import BookingService
from modules.booking.schemas.booking_schema import CreateBookingSchema
from modules.user.model.user_model import UserRole


router = APIRouter(prefix="/api/v1/bookings", tags=["Bookings"])

booking_service = BookingService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("", status_code=201)
def create_booking(request_data: dict, current_user: dict = Depends(require_user)):
    """Create a new booking. Requires authenticated user role."""
    request_data["user_id"] = current_user["id"]

    schema = CreateBookingSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        booking = booking_service.create_booking(schema.validated_data)
        return _success(booking, "Booking created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_bookings(current_user: dict = Depends(get_current_user)):
    """Retrieve bookings.

    - ground_owner: sees only bookings in their assigned region.
    - Other admins: see all bookings.
    - Regular users: see only their own bookings.
    """
    role = current_user["role"]
    if role.lower() == "ground_owner":
        bookings = booking_service.list_bookings_by_owner(current_user["id"])
    elif role in _ADMIN_ROLES:
        bookings = booking_service.list_bookings()
    else:
        bookings = booking_service.list_bookings_by_user(current_user["id"])
    return _success(bookings)


@router.get("/{booking_id}")
def get_booking(booking_id: int, current_user: dict = Depends(get_current_user)):
    """Retrieve a booking by ID. Users can only view their own."""
    booking = booking_service.get_booking(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    if (
        current_user["role"] not in _ADMIN_ROLES
        and booking["user_id"] != current_user["id"]
    ):
        raise HTTPException(
            status_code=403, detail="You can only view your own bookings."
        )

    return _success(booking)


@router.post("/{booking_id}/cancel")
def cancel_booking(booking_id: int, current_user: dict = Depends(get_current_user)):
    """Cancel an existing booking. Users can cancel own; admins can cancel any."""
    booking = booking_service.get_booking(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    if (
        current_user["role"] not in _ADMIN_ROLES
        and booking["user_id"] != current_user["id"]
    ):
        raise HTTPException(
            status_code=403, detail="You can only cancel your own bookings."
        )

    try:
        updated = booking_service.cancel_booking(booking_id)
        audit_service.log(
            action="BOOKING_CANCELLED",
            actor_user_id=current_user["id"],
            target_resource_type="booking",
            target_resource_id=booking_id,
            details={"by_admin": current_user["role"] in _ADMIN_ROLES},
        )
        return _success(updated, "Booking cancelled successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/confirm", dependencies=[Depends(require_admin)])
def confirm_booking(booking_id: int):
    """Confirm a booking after payment approval. Admin only."""
    try:
        booking = booking_service.confirm_booking(booking_id)
        return _success(booking, "Booking confirmed successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/start", dependencies=[Depends(require_admin)])
def start_booking(booking_id: int):
    """Start a confirmed booking (CONFIRMED → IN_PROGRESS). Admin only."""
    try:
        booking = booking_service.start_booking(booking_id)
        return _success(booking, "Booking started successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/complete", dependencies=[Depends(require_admin)])
def complete_booking(booking_id: int):
    """Mark a booking as completed. Admin only."""
    try:
        booking = booking_service.complete_booking(booking_id)
        return _success(booking, "Booking completed successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/start-session")
def start_session(
    booking_id: int,
    current_user: dict = require_role(
        UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER, UserRole.GROUND_OWNER
    ),
):
    """Captain/admin starts the session timer (CONFIRMED -> IN_PROGRESS)."""
    try:
        booking = booking_service.start_session(booking_id)
        return _success(booking, "Session started.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/end-session")
def end_session(
    booking_id: int,
    current_user: dict = require_role(
        UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER, UserRole.GROUND_OWNER
    ),
):
    """Captain/admin ends the session, computes the time bill
    (IN_PROGRESS -> AWAITING_TIME_PAYMENT)."""
    try:
        booking = booking_service.end_session(booking_id)
        return _success(booking, "Session ended. Time bill generated.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{booking_id}/session-status")
def session_status(booking_id: int, current_user: dict = Depends(get_current_user)):
    """Live elapsed time + running bill estimate for an in-progress session."""
    try:
        return _success(booking_service.session_status(booking_id))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
