from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_user
from modules.trainer.service.trainer_booking_service import trainer_booking_service

router = APIRouter(prefix="/api/v1/trainer-bookings", tags=["Trainer Bookings"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.post("", status_code=201)
def create_booking(body: dict, current_user: dict = Depends(require_user)):
    """body = {"trainer_id": int, "session_date": "YYYY-MM-DD", "session_time": "HH:MM"}."""
    try:
        result = trainer_booking_service.create_booking(
            current_user["id"], body.get("trainer_id"), body.get("session_date"), body.get("session_time"),
        )
        return _success(result, "Booking created.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/mine")
def list_my_bookings(current_user: dict = Depends(require_user)):
    return _success(trainer_booking_service.list_my_bookings(current_user["id"]))


@router.get("/{booking_id}")
def get_booking(booking_id: int, current_user: dict = Depends(require_user)):
    try:
        return _success(trainer_booking_service.get_booking(booking_id, current_user["id"]))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{booking_id}/submit-payment")
def submit_payment(booking_id: int, body: dict, current_user: dict = Depends(require_user)):
    try:
        result = trainer_booking_service.submit_payment(booking_id, current_user["id"], body.get("transaction_id"))
        return _success(result, "Payment submitted for review.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
