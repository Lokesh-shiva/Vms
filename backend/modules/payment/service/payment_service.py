import os
import random

from core.base.base_service import BaseService
from modules.payment.repository.payment_repository import (
    payment_repository as _default_payment_repo,
)
from modules.booking.repository.booking_repository import (
    booking_repository as _default_booking_repo,
)
from modules.payment.repository.system_config_repository import (
    system_config_repository as _default_config_repo,
)
from modules.fee_config.service.fee_config_service import FeeConfigService


# Fallback UPI ID from environment variable (used when no DB override exists)
_ENV_UPI_ID = os.getenv("UPI_ID", "vms@upi")
_ENV_MERCHANT_NAME = os.getenv("MERCHANT_NAME", "VMS")

# Key used in the system_configs table
UPI_ID_CONFIG_KEY = "UPI_ID"
MERCHANT_NAME_CONFIG_KEY = "MERCHANT_NAME"

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

    def __init__(
        self,
        payment_repository=None,
        booking_repository=None,
        system_config_repository=None,
        fee_config_service=None,
    ):
        super().__init__()
        self.payment_repository = payment_repository or _default_payment_repo
        self.booking_repository = booking_repository or _default_booking_repo
        self.config_repository = system_config_repository or _default_config_repo
        self.fee_config_service = fee_config_service or FeeConfigService()

    # ── Split Payments ─────────────────────────────────────────────────

    def create_split_payments(self, match_id: int) -> list[dict]:
        """
        Create two PENDING payment records — one per player — upon match completion.

        Flow:
        1. Fetch Match by ID with participants (MatchPlayer).
        2. Fetch Booking associated with the match via match.booking_id.
        3. Calculate amount = (booking.estimated_total + booking.booking_fee) / len(players).
        4. Create one Payment per participant with:
           - status: PENDING
           - provider: MANUAL_UPI
           - amount: split amount
           - reference_code: VMS-{booking_id}-P{user_id}-{rand}
           - user_id: participant user_id
           - match_id: match ID

        Returns list of created payment dicts.
        Raises ValueError if match or booking is not found.
        """
        from modules.match.model.match_model import Match as MatchORM, MatchPlayer
        from modules.payment.model.payment_model import Payment as PaymentORM
        from core.database.db_connection import SessionLocal

        session = SessionLocal()
        try:
            match = session.query(MatchORM).filter(MatchORM.id == match_id).first()
            if not match:
                raise ValueError(f"Match {match_id} not found.")

            if not match.booking_id:
                raise ValueError(
                    f"Match {match_id} has no associated booking. "
                    "Cannot create split payments."
                )

            # ── Idempotency guard ────────────────────────────────────────
            # If payments were already created for this match (e.g. duplicate
            # finish call or retry after a partial failure), return them as-is.
            existing_payments = (
                session.query(PaymentORM).filter(PaymentORM.match_id == match_id).all()
            )
            if existing_payments:
                return [p.to_dict() for p in existing_payments]
            # ─────────────────────────────────────────────────────────────

            booking = self.booking_repository.find_by_id(match.booking_id)
            if not booking:
                raise ValueError(
                    f"Booking {match.booking_id} not found for match {match_id}."
                )

            players = (
                session.query(MatchPlayer)
                .filter(MatchPlayer.match_id == match_id)
                .all()
            )
            if not players:
                raise ValueError(f"No players found for match {match_id}.")

            total = float(booking.get("estimated_total", 0)) + float(
                booking.get("booking_fee", 0)
            )
            split_amount = round(total / len(players), 2)

            created_payments = []
            for player in players:
                reference_code = self._generate_split_reference_code(
                    booking["id"], player.user_id
                )
                payment = self.payment_repository.create(
                    {
                        "booking_id": booking["id"],
                        "user_id": player.user_id,
                        "match_id": match_id,
                        "provider": "MANUAL_UPI",
                        "amount": split_amount,
                        "reference_code": reference_code,
                        "status": "PENDING",
                    }
                )
                created_payments.append(payment)

            return created_payments
        finally:
            session.close()

    # ── Listing ─────────────────────────────────────────────────────────

    def get_all_payments(self, status: str = None) -> list[dict]:
        """Return all payments, optionally filtered by status."""
        return self.payment_repository.find_all(status=status)

    def get_summary(self) -> dict:
        """Return aggregate payment statistics."""
        return self.payment_repository.get_summary()

    # ── Reference Code Generator ───────────────────────────────────────

    def _generate_reference_code(self, booking_id: int) -> str:
        """Generate VMS-{booking_id}-{4 random digits}."""
        suffix = f"{random.randint(0, 9999):04d}"
        return f"VMS-{booking_id}-{suffix}"

    def _generate_split_reference_code(self, booking_id: int, user_id: int) -> str:
        """Generate VMS-{booking_id}-P{user_id}-{4 random digits} for split payments."""
        suffix = f"{random.randint(0, 9999):04d}"
        return f"VMS-{booking_id}-P{user_id}-{suffix}"

    def _get_active_upi_id(self) -> str:
        """Return UPI ID from DB config, falling back to env var."""
        db_value = self.config_repository.get(UPI_ID_CONFIG_KEY)
        return db_value if db_value else _ENV_UPI_ID

    def _get_active_merchant_name(self) -> str:
        """Return Merchant Name from DB config, falling back to env var."""
        db_value = self.config_repository.get(MERCHANT_NAME_CONFIG_KEY)
        return db_value if db_value else _ENV_MERCHANT_NAME

    # ── Public Methods ─────────────────────────────────────────────────

    def initiate_payment(self, booking_id: int) -> dict:
        """
        Create a new payment record for a booking.

        - Validates booking exists and is in PENDING_PAYMENT status.
        - Allows retry if the latest payment for this booking is FAILED.
        - Blocks if a PENDING or UNDER_REVIEW payment already exists.
        - Generates a unique reference code with retry (max 5 attempts).
        - Creates payment with status=PENDING.

        Returns dict with booking_id, amount, reference_code, upi_id.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "PENDING_PAYMENT":
            raise ValueError(
                "Payment can only be initiated for bookings in PENDING_PAYMENT status."
            )

        existing_payment = self.payment_repository.find_by_booking_id(booking_id)
        if existing_payment:
            if existing_payment["status"] in ("PENDING", "UNDER_REVIEW"):
                raise ValueError("A payment is already in progress for this booking.")
            if existing_payment["status"] == "SUCCESS":
                raise ValueError("Payment already completed for this booking.")
            # FAILED or REFUNDED → allow retry (new payment record, old stays)

        pricing = self.fee_config_service.get_config_by_region_and_cart_type(
            booking["region_id"], booking["cart_type_id"]
        )
        amount = float(pricing["matching_fee"]) if pricing else 0.0

        for attempt in range(MAX_REFCODE_ATTEMPTS):
            reference_code = self._generate_reference_code(booking_id)
            try:
                payment = self.payment_repository.create(
                    {
                        "booking_id": booking_id,
                        "provider": "MANUAL_UPI",
                        "payment_type": "MATCHING_FEE",
                        "amount": amount,
                        "reference_code": reference_code,
                        "status": "PENDING",
                    }
                )

                # Build UPI deep link for mobile redirect
                upi_id = self._get_active_upi_id()
                merchant_name = self._get_active_merchant_name()
                formatted_amount = f"{float(payment['amount']):.2f}"
                upi_link = (
                    f"upi://pay?"
                    f"pa={upi_id}"
                    f"&pn={merchant_name}"
                    f"&am={formatted_amount}"
                    f"&cu=INR"
                    f"&tn={payment['reference_code']}"
                )

                return {
                    "booking_id": booking_id,
                    "amount": payment["amount"],
                    "reference_code": payment["reference_code"],
                    "upi_id": upi_id,
                    "upi_link": upi_link,
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

        return self.payment_repository.update(
            payment["id"],
            {
                "transaction_id": transaction_id.strip(),
                "status": "UNDER_REVIEW",
            },
        )

    def approve_payment(self, payment_id: int) -> dict:
        """
        Admin approves a payment.

        - Validates payment is in UNDER_REVIEW status.
        - Moves payment to SUCCESS.
        - Mirrors status to booking.payment_status (sole writer).
        - Auto-confirms the booking (assigns cart, sets CONFIRMED).
        """
        payment = self.payment_repository.find_by_id(payment_id)
        if not payment:
            raise ValueError("Payment not found.")
        if payment["status"] != "UNDER_REVIEW":
            raise ValueError("Only payments under review can be approved.")

        updated_payment = self.payment_repository.update(
            payment_id,
            {
                "status": "SUCCESS",
            },
        )

        self.booking_repository.update(
            payment["booking_id"],
            {
                "payment_status": "SUCCESS",
            },
        )

        # Type-aware booking transition
        booking = self.booking_repository.find_by_id(payment["booking_id"])
        if booking:
            if payment.get("payment_type") == "TIME_BILL":
                # Final payment — complete the booking and release the ground
                if booking["status"] == "AWAITING_TIME_PAYMENT":
                    self.booking_repository.update(
                        payment["booking_id"], {"status": "COMPLETED"}
                    )
                    if booking.get("assigned_cart_id"):
                        from modules.cart.repository.cart_repository import (
                            cart_repository,
                        )
                        cart_repository.update(
                            booking["assigned_cart_id"], {"status": "AVAILABLE"}
                        )
            else:
                # MATCHING_FEE — auto-confirm (existing behaviour)
                if booking["status"] == "PENDING_PAYMENT":
                    try:
                        from modules.booking.service.booking_service import BookingService
                        BookingService().confirm_booking(payment["booking_id"])
                    except ValueError:
                        pass

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

        updated_payment = self.payment_repository.update(
            payment_id,
            {
                "status": "FAILED",
            },
        )

        self.booking_repository.update(
            payment["booking_id"],
            {
                "payment_status": "FAILED",
            },
        )

        return updated_payment

    def process_refund(self, payment_id: int, booking: dict = None) -> dict:
        """
        Process a refund for a successful payment using percentage-based deductions.

        Uses the booking's snapshotted cancellation_fee_pct and platform_fee_pct
        (captured at booking creation time) to ensure admin config changes don't
        retroactively affect old bookings.

        Refund formula:
            total_pct = cancellation_fee_pct_snapshot + platform_fee_pct_snapshot
            refund_amount = total_paid × (1 - total_pct / 100)

        - Validates payment is in SUCCESS status.
        - Moves payment to REFUNDED.
        - Updates booking refund fields.
        """
        payment = self.payment_repository.find_by_id(payment_id)
        if not payment:
            raise ValueError("Payment not found.")
        if payment["status"] != "SUCCESS":
            raise ValueError("Only successful payments can be refunded.")

        total_paid = float(payment["amount"])

        # Use snapshot percentages from booking if provided
        if booking:
            cancellation_pct = float(booking.get("cancellation_fee_pct_snapshot", 0))
            platform_pct = float(booking.get("platform_fee_pct_snapshot", 0))
        else:
            # Fallback: fetch booking to get snapshot values
            fetched_booking = self.booking_repository.find_by_id(payment["booking_id"])
            cancellation_pct = float(
                fetched_booking.get("cancellation_fee_pct_snapshot", 0)
            )
            platform_pct = float(fetched_booking.get("platform_fee_pct_snapshot", 0))

        total_pct = cancellation_pct + platform_pct
        if total_pct > 100:
            raise ValueError(
                "Snapshot percentage sum exceeds 100. Cannot process refund."
            )

        refund_amount = round(total_paid * (1 - total_pct / 100), 2)

        updated_payment = self.payment_repository.update(
            payment_id,
            {
                "status": "REFUNDED",
            },
        )

        self.booking_repository.update(
            payment["booking_id"],
            {
                "refund_status": "REFUNDED",
                "refund_amount": refund_amount,
            },
        )

        return updated_payment

    def get_payment_by_booking_id(self, booking_id: int) -> dict | None:
        """Retrieve payment details for admin inspection."""
        return self.payment_repository.find_by_booking_id(booking_id)

    def create_time_bill_payment(self, booking_id: int, amount: float) -> dict:
        """Create the post-session TIME_BILL payment for the user to pay.

        Idempotent: if a TIME_BILL payment already exists for this booking,
        return it instead of creating a duplicate.
        """
        existing = self.payment_repository.find_by_booking_id_all(booking_id)
        for p in existing:
            if p.get("payment_type") == "TIME_BILL":
                return p

        booking = self.booking_repository.find_by_id(booking_id)
        user_id = booking["user_id"] if booking else None

        for attempt in range(MAX_REFCODE_ATTEMPTS):
            reference_code = f"{self._generate_reference_code(booking_id)}-T"
            try:
                return self.payment_repository.create(
                    {
                        "booking_id": booking_id,
                        "user_id": user_id,
                        "provider": "MANUAL_UPI",
                        "payment_type": "TIME_BILL",
                        "amount": amount,
                        "reference_code": reference_code,
                        "status": "UNDER_REVIEW",
                    }
                )
            except Exception:
                if attempt == MAX_REFCODE_ATTEMPTS - 1:
                    raise RuntimeError("Failed to generate unique TIME_BILL reference code.")

    # ── Admin Config ───────────────────────────────────────────────────

    def get_admin_payment_config(self) -> dict:
        """Retrieve current active payment configuration (UPI ID, Merchant Name)."""
        return {
            "upi_id": self._get_active_upi_id(),
            "merchant_name": self._get_active_merchant_name(),
        }

    def update_admin_payment_config(
        self, upi_id: str | None, merchant_name: str | None
    ) -> dict:
        """Update payment configuration in DB."""
        if upi_id is not None:
            if not upi_id.strip() or "@" not in upi_id:
                raise ValueError("Valid UPI ID is required (must contain '@').")
            self.config_repository.set(UPI_ID_CONFIG_KEY, upi_id.strip())

        if merchant_name is not None:
            if not merchant_name.strip():
                raise ValueError("Merchant name cannot be empty.")
            self.config_repository.set(MERCHANT_NAME_CONFIG_KEY, merchant_name.strip())

        return self.get_admin_payment_config()
