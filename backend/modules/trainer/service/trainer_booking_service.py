import os
import random
import re
from datetime import date, datetime

from modules.payment.repository.system_config_repository import (
    system_config_repository as _default_config_repo,
)
from modules.trainer.repository.trainer_booking_repository import (
    trainer_booking_repository as _default_booking_repo,
)
from modules.trainer.repository.trainer_repository import trainer_repository as _default_trainer_repo

_ENV_UPI_ID = os.getenv("UPI_ID", "vms@upi")
_ENV_MERCHANT_NAME = os.getenv("MERCHANT_NAME", "VMS")
UPI_ID_CONFIG_KEY = "UPI_ID"
MERCHANT_NAME_CONFIG_KEY = "MERCHANT_NAME"

MAX_REFCODE_ATTEMPTS = 5
_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
_TIME_RE = re.compile(r"^\d{2}:\d{2}$")


class TrainerBookingService:
    """Session booking via the same manual-UPI-reference + admin-approval
    workflow used for shop orders."""

    def __init__(self, booking_repository=None, trainer_repository=None, system_config_repository=None, now_fn=None):
        self._bookings = booking_repository or _default_booking_repo
        self._trainers = trainer_repository or _default_trainer_repo
        self._config = system_config_repository or _default_config_repo
        self._today = now_fn or date.today

    def _get_active_upi_id(self) -> str:
        return self._config.get(UPI_ID_CONFIG_KEY) or _ENV_UPI_ID

    def _get_active_merchant_name(self) -> str:
        return self._config.get(MERCHANT_NAME_CONFIG_KEY) or _ENV_MERCHANT_NAME

    def _generate_reference_code(self, user_id: int) -> str:
        suffix = f"{random.randint(0, 9999):04d}"
        return f"TRN-{user_id}-{suffix}"

    def create_booking(self, user_id: int, trainer_id: int, session_date: str, session_time: str) -> dict:
        if not _DATE_RE.match(str(session_date or "")):
            raise ValueError("'session_date' must be in YYYY-MM-DD format.")
        if not _TIME_RE.match(str(session_time or "")):
            raise ValueError("'session_time' must be in HH:MM format.")
        try:
            parsed_date = datetime.strptime(session_date, "%Y-%m-%d").date()
        except ValueError:
            raise ValueError("'session_date' is not a valid date.")
        if parsed_date < self._today():
            raise ValueError("'session_date' can't be in the past.")

        trainer = self._trainers.find_by_id(trainer_id)
        if not trainer or not trainer.get("is_active", False):
            raise ValueError("This trainer isn't currently available.")

        amount = float(trainer["rate_per_session"])

        booking = None
        for attempt in range(MAX_REFCODE_ATTEMPTS):
            reference_code = self._generate_reference_code(user_id)
            try:
                booking = self._bookings.create({
                    "trainer_id": trainer_id,
                    "user_id": user_id,
                    "session_date": session_date,
                    "session_time": session_time,
                    "amount": amount,
                    "reference_code": reference_code,
                })
                break
            except Exception:
                if attempt == MAX_REFCODE_ATTEMPTS - 1:
                    raise RuntimeError(
                        f"Failed to generate a unique booking reference after {MAX_REFCODE_ATTEMPTS} attempts."
                    )

        upi_id = self._get_active_upi_id()
        merchant_name = self._get_active_merchant_name()
        formatted_amount = f"{amount:.2f}"
        upi_link = (
            f"upi://pay?pa={upi_id}&pn={merchant_name}"
            f"&am={formatted_amount}&cu=INR&tn={booking['reference_code']}"
        )
        return {**booking, "upi_id": upi_id, "upi_link": upi_link}

    def submit_payment(self, booking_id: int, user_id: int, transaction_id: str) -> dict:
        booking = self._bookings.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["user_id"] != user_id:
            raise ValueError("You can only submit payment for your own booking.")
        if booking["status"] != "PENDING_PAYMENT":
            raise ValueError("Booking is not awaiting payment.")
        if not transaction_id or not transaction_id.strip():
            raise ValueError("Transaction ID is required.")

        return self._bookings.update(booking_id, {
            "transaction_id": transaction_id.strip(),
            "status": "UNDER_REVIEW",
        })

    def get_booking(self, booking_id: int, user_id: int, is_admin: bool = False) -> dict:
        booking = self._bookings.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if not is_admin and booking["user_id"] != user_id:
            raise ValueError("You can only view your own bookings.")
        return booking

    def list_my_bookings(self, user_id: int) -> list[dict]:
        return self._bookings.find_by_user(user_id)

    def list_all_bookings(self, status: str | None = None) -> list[dict]:
        return self._bookings.find_all(status)

    def approve_booking(self, booking_id: int) -> dict:
        booking = self._bookings.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "UNDER_REVIEW":
            raise ValueError("Only bookings under review can be approved.")
        return self._bookings.update(booking_id, {"status": "CONFIRMED"})

    def reject_booking(self, booking_id: int) -> dict:
        booking = self._bookings.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "UNDER_REVIEW":
            raise ValueError("Only bookings under review can be rejected.")
        return self._bookings.update(booking_id, {"status": "REJECTED"})


trainer_booking_service = TrainerBookingService()
