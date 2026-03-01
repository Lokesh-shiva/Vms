from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import (
    get_current_user,
    require_admin,
    require_user,
)
from modules.booking.service.booking_service import BookingService
from modules.booking.schemas.booking_schema import CreateBookingSchema


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
    """Retrieve bookings. Admins see all; users see only their own."""
    if current_user["role"] == "admin":
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

    if current_user["role"] != "admin" and booking["user_id"] != current_user["id"]:
        raise HTTPException(status_code=403, detail="You can only view your own bookings.")

    return _success(booking)


@router.post("/{booking_id}/cancel")
def cancel_booking(booking_id: int, current_user: dict = Depends(get_current_user)):
    """Cancel an existing booking. Users can cancel own; admins can cancel any."""
    booking = booking_service.get_booking(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    if current_user["role"] != "admin" and booking["user_id"] != current_user["id"]:
        raise HTTPException(status_code=403, detail="You can only cancel your own bookings.")

    try:
        updated = booking_service.cancel_booking(booking_id)
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


@router.post("/{booking_id}/complete", dependencies=[Depends(require_admin)])
def complete_booking(booking_id: int):
    """Mark a booking as completed. Admin only."""
    try:
        booking = booking_service.complete_booking(booking_id)
        return _success(booking, "Booking completed successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
