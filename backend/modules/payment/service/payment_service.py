import random

from core.base.base_service import BaseService
from modules.payment.repository.payment_repository import payment_repository as _default_payment_repo
from modules.booking.repository.booking_repository import booking_repository as _default_booking_repo


# UPI ID used for manual payment instructions (MVP placeholder)
MANUAL_UPI_ID = "vms@upi"

MAX_REFCODE_ATTEMPTS = 5


class PaymentService(BaseService):
    """
    Manual Payment Approval Workflow.

    This service tracks admin approval decisions for payments.
    It does NOT verify UPI transactions, connect to any bank,
    or validate actual money movement.

    PaymentService is the sole writer of bookings.payment_status
    (single source of truth rule).
    """

    def __init__(self, payment_repository=None, booking_repository=None):
        super().__init__()
        self.payment_repository = payment_repository or _default_payment_repo
        self.booking_repository = booking_repository or _default_booking_repo

    # ── Reference Code Generator ───────────────────────────────────────

    def _generate_reference_code(self, booking_id: int) -> str:
        """Generate VMS-{booking_id}-{4 random digits}."""
        suffix = f"{random.randint(0, 9999):04d}"
        return f"VMS-{booking_id}-{suffix}"

    # ── Public Methods ─────────────────────────────────────────────────

    def initiate_payment(self, booking_id: int) -> dict:
        """
        Create a new payment record for a booking.

        - Validates booking exists and is in PENDING_PAYMENT status.
        - Generates a unique reference code with retry (max 5 attempts).
        - Creates payment with status=PENDING.

        Returns dict with booking_id, amount, reference_code, upi_id.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "PENDING_PAYMENT":
            raise ValueError("Payment can only be initiated for bookings in PENDING_PAYMENT status.")

        existing_payment = self.payment_repository.find_by_booking_id(booking_id)
        if existing_payment:
            raise ValueError("Payment already exists for this booking.")

        amount = booking["estimated_total"] + booking["booking_fee"]

        for attempt in range(MAX_REFCODE_ATTEMPTS):
            reference_code = self._generate_reference_code(booking_id)
            try:
                payment = self.payment_repository.create({
                    "booking_id": booking_id,
                    "provider": "MANUAL_UPI",
                    "amount": amount,
                    "reference_code": reference_code,
                    "status": "PENDING",
                })
                return {
                    "booking_id": booking_id,
                    "amount": payment["amount"],
                    "reference_code": payment["reference_code"],
                    "upi_id": MANUAL_UPI_ID,
                    "payment": payment,
                }
            except Exception:
                if attempt == MAX_REFCODE_ATTEMPTS - 1:
                    raise RuntimeError(
                        "Failed to generate unique reference code after "
                        f"{MAX_REFCODE_ATTEMPTS} attempts."
                    )

    def submit_manual_confirmation(self, booking_id: int, transaction_id: str) -> dict:
        """
        User submits their UPI transaction ID for admin review.

        - Validates booking is in PENDING_PAYMENT status.
        - Validates payment is in PENDING status.
        - Stores transaction_id and moves payment to UNDER_REVIEW.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "PENDING_PAYMENT":
            raise ValueError("Booking is not in PENDING_PAYMENT status.")

        payment = self.payment_repository.find_by_booking_id(booking_id)
        if not payment:
            raise ValueError("No payment found for this booking.")
        if payment["status"] != "PENDING":
            raise ValueError("Payment is not in PENDING status.")

        if not transaction_id or not transaction_id.strip():
            raise ValueError("Transaction ID is required.")

        return self.payment_repository.update(payment["id"], {
            "transaction_id": transaction_id.strip(),
            "status": "UNDER_REVIEW",
        })

    def approve_payment(self, payment_id: int) -> dict:
        """
        Admin approves a payment.

        - Validates payment is in UNDER_REVIEW status.
        - Moves payment to SUCCESS.
        - Mirrors status to booking.payment_status (sole writer).
        """
        payment = self.payment_repository.find_by_id(payment_id)
        if not payment:
            raise ValueError("Payment not found.")
        if payment["status"] != "UNDER_REVIEW":
            raise ValueError("Only payments under review can be approved.")

        updated_payment = self.payment_repository.update(payment_id, {
            "status": "SUCCESS",
        })

        self.booking_repository.update(payment["booking_id"], {
            "payment_status": "SUCCESS",
        })

        return updated_payment

    def reject_payment(self, payment_id: int) -> dict:
        """
        Admin rejects a payment.

        - Validates payment is in UNDER_REVIEW status.
        - Moves payment to FAILED.
        - Mirrors status to booking.payment_status (sole writer).
        """
        payment = self.payment_repository.find_by_id(payment_id)
        if not payment:
            raise ValueError("Payment not found.")
        if payment["status"] != "UNDER_REVIEW":
            raise ValueError("Only payments under review can be rejected.")

        updated_payment = self.payment_repository.update(payment_id, {
            "status": "FAILED",
        })

        self.booking_repository.update(payment["booking_id"], {
            "payment_status": "FAILED",
        })

        return updated_payment

    def process_refund(self, payment_id: int, amount: float = None) -> dict:
        """
        Admin initiates a refund for a successful payment.

        - Validates payment is in SUCCESS status.
        - Moves payment to REFUNDED.
        - Updates booking refund fields.
        """
        payment = self.payment_repository.find_by_id(payment_id)
        if not payment:
            raise ValueError("Payment not found.")
        if payment["status"] != "SUCCESS":
            raise ValueError("Only successful payments can be refunded.")

        refund_amount = amount if amount is not None else payment["amount"]

        updated_payment = self.payment_repository.update(payment_id, {
            "status": "REFUNDED",
        })

        self.booking_repository.update(payment["booking_id"], {
            "refund_status": "REFUNDED",
            "refund_amount": refund_amount,
        })

        return updated_payment

    def get_payment_by_booking_id(self, booking_id: int) -> dict | None:
        """Retrieve payment details for admin inspection."""
        return self.payment_repository.find_by_booking_id(booking_id)
