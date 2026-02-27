from fastapi import APIRouter, HTTPException
from modules.booking.service.booking_service import BookingService
from modules.booking.schemas.booking_schema import CreateBookingSchema


router = APIRouter(prefix="/api/v1/bookings", tags=["Bookings"])

booking_service = BookingService()


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.post("", status_code=201)
def create_booking(request_data: dict):
    """Create a new booking."""
    schema = CreateBookingSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        booking = booking_service.create_booking(schema.validated_data)
        return _success(booking, "Booking created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_bookings():
    """Retrieve all bookings."""
    bookings = booking_service.list_bookings()
    return _success(bookings)


@router.get("/{booking_id}")
def get_booking(booking_id: int):
    """Retrieve a booking by ID."""
    booking = booking_service.get_booking(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")
    return _success(booking)


@router.post("/{booking_id}/cancel")
def cancel_booking(booking_id: int):
    """Cancel an existing booking."""
    try:
        booking = booking_service.cancel_booking(booking_id)
        return _success(booking, "Booking cancelled successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{booking_id}/complete")
def complete_booking(booking_id: int):
    """Mark a booking as completed."""
    try:
        booking = booking_service.complete_booking(booking_id)
        return _success(booking, "Booking completed successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
