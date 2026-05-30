from datetime import datetime, timedelta
from decimal import Decimal

from core.base.base_service import BaseService
from modules.booking.repository.booking_repository import (
    booking_repository as _default_booking_repo,
)
from modules.user.repository.user_repository import (
    user_repository as _default_user_repo,
)
from modules.location.repository.location_repository import (
    location_repository as _default_location_repo,
)
from modules.cart_type.repository.cart_type_repository import (
    cart_type_repository as _default_cart_type_repo,
)
from modules.timeslot.repository.timeslot_repository import (
    timeslot_repository as _default_timeslot_repo,
)
from modules.cart.repository.cart_repository import (
    cart_repository as _default_cart_repo,
)
from modules.booking_item.service.booking_item_service import BookingItemService
from modules.payment.repository.payment_repository import (
    payment_repository as _default_payment_repo,
)
from modules.payment.service.payment_service import (
    PaymentService as _DefaultPaymentService,
)
from modules.fee_config.repository.fee_config_repository import (
    fee_config_repository as _default_fee_config_repo,
)


# ── Constants ─────────────────────────────────────────────────────────

MAX_BOOKINGS_PER_USER_PER_DAY = 3
BOOKING_EXPIRY_MINUTES = 10


class BookingService(BaseService):
    """
    Business logic layer for Booking operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to BookingRepository and cross-module repositories.
    - Enforces slot capacity, daily user limits, and cart availability.
    - Manages booking state machine (PENDING_PAYMENT -> CONFIRMED -> etc).
    - Manages cart lifecycle (assign on confirm, release on cancel/complete).
    - Returns formatted results to the controller.

    BookingService NEVER directly mutates booking.payment_status.
    That field is owned exclusively by PaymentService.
    """

    def __init__(
        self,
        booking_repository=None,
        user_repository=None,
        location_repository=None,
        cart_type_repository=None,
        timeslot_repository=None,
        cart_repository=None,
        booking_item_service=None,
        payment_repository=None,
        payment_service=None,
        fee_config_repository=None,
    ):
        super().__init__()
        self.booking_repository = booking_repository or _default_booking_repo
        self.user_repository = user_repository or _default_user_repo
        self.location_repository = location_repository or _default_location_repo
        self.cart_type_repository = cart_type_repository or _default_cart_type_repo
        self.timeslot_repository = timeslot_repository or _default_timeslot_repo
        self.cart_repository = cart_repository or _default_cart_repo
        self.booking_item_service = booking_item_service or BookingItemService()
        self.payment_repository = payment_repository or _default_payment_repo
        self.payment_service = payment_service or _DefaultPaymentService()
        self.fee_config_repository = fee_config_repository or _default_fee_config_repo

    # ── FK Validation Helpers ─────────────────────────────────────────

    def _validate_user(self, user_id: int) -> None:
        """Ensure the referenced user exists."""
        user = self.user_repository.find_by_id(user_id)
        if not user:
            raise ValueError("Referenced user does not exist.")

    def _validate_region(self, region_id: int) -> None:
        """Ensure the referenced region (location) exists."""
        region = self.location_repository.find_by_id(region_id)
        if not region:
            raise ValueError("Referenced region does not exist.")

    def _validate_cart_type(self, cart_type_id: int) -> None:
        """Ensure the referenced cart type exists."""
        cart_type = self.cart_type_repository.find_by_id(cart_type_id)
        if not cart_type:
            raise ValueError("Referenced cart type does not exist.")

    def _validate_timeslot(self, timeslot_id: int) -> dict:
        """Ensure the referenced timeslot exists. Returns the timeslot record."""
        timeslot = self.timeslot_repository.find_by_id(timeslot_id)
        if not timeslot:
            raise ValueError("Referenced timeslot does not exist.")
        return timeslot

    # ── Business Rule Helpers ─────────────────────────────────────────

    def _enforce_slot_capacity(
        self, timeslot_id: int, capacity: int, session=None
    ) -> None:
        """Ensure the timeslot has not reached its booking capacity.

        Only CONFIRMED and IN_PROGRESS bookings count toward capacity.
        """
        current_count = self.booking_repository.count_by_timeslot(
            timeslot_id, session=session
        )
        if current_count >= capacity:
            raise ValueError("Timeslot is fully booked.")

    def _enforce_daily_user_limit(self, user_id: int, date: str, session=None) -> None:
        """Ensure the user has not exceeded the daily booking limit.

        Counts only CONFIRMED and IN_PROGRESS bookings.
        PENDING_PAYMENT, CANCELLED, and EXPIRED do NOT count.
        Also runs lazy expiry check on stale PENDING_PAYMENT bookings.
        """
        user_bookings = self.booking_repository.find_by_user_and_date(
            user_id, date, session=session
        )
        # Lazy expiry — expire any stale PENDING_PAYMENT before counting
        for b in user_bookings:
            if b["status"] == "PENDING_PAYMENT":
                self._check_and_expire(b)

        # Re-fetch after potential expiry updates
        user_bookings = self.booking_repository.find_by_user_and_date(
            user_id, date, session=session
        )
        active_bookings = [
            b for b in user_bookings if b["status"] in ("CONFIRMED", "IN_PROGRESS")
        ]
        if len(active_bookings) >= MAX_BOOKINGS_PER_USER_PER_DAY:
            raise ValueError(
                f"User has reached the maximum of {MAX_BOOKINGS_PER_USER_PER_DAY} "
                f"bookings per day."
            )

    def _find_available_cart(
        self, region_id: int, cart_type_id: int, session=None
    ) -> dict:
        """Find an available cart matching the region and cart type.

        Raises ValueError if no cart is available.
        """
        all_carts = self.cart_repository.find_all(session=session)
        for cart in all_carts:
            if (
                cart["region_id"] == region_id
                and cart["cart_type_id"] == cart_type_id
                and cart["status"] == "AVAILABLE"
                and cart.get("is_active", True)
            ):
                return cart
        raise ValueError(
            "No available cart found for the selected region and cart type."
        )

    def _check_and_expire(self, booking: dict) -> dict:
        """Check if a PENDING_PAYMENT booking should be expired.

        Returns the updated booking if expired, otherwise returns the original.
        """
        if booking["status"] != "PENDING_PAYMENT":
            return booking

        created_at = booking["created_at"]
        if isinstance(created_at, str):
            created_at = datetime.fromisoformat(created_at)

        if datetime.utcnow() - created_at > timedelta(minutes=BOOKING_EXPIRY_MINUTES):
            return self.booking_repository.update(
                booking["id"],
                {
                    "status": "EXPIRED",
                },
            )

        return booking

    # ── Display Name Enrichment ──────────────────────────────────────

    def _enrich_booking(self, booking: dict) -> dict:
        """Attach human-readable display names to a single booking dict.

        Used for get_booking() where only one booking needs enrichment.
        For lists, use _enrich_bookings_batch() to avoid N+1 queries.
        """
        region = self.location_repository.find_by_id(booking.get("region_id"))
        booking["region_name"] = region.get("name") if region else None

        cart_type = self.cart_type_repository.find_by_id(booking.get("cart_type_id"))
        booking["cart_type_name"] = cart_type.get("name") if cart_type else None

        timeslot = self.timeslot_repository.find_by_id(booking.get("timeslot_id"))
        if timeslot:
            booking["timeslot_label"] = (
                f"{timeslot.get('start_time', '?')} - {timeslot.get('end_time', '?')}"
            )
        else:
            booking["timeslot_label"] = None

        assigned = booking.get("assigned_cart_id")
        if assigned is not None:
            cart = self.cart_repository.find_by_id(assigned)
            booking["cart_label"] = (
                cart.get("label") if cart and cart.get("label") else f"Ground {assigned}"
            )
        else:
            booking["cart_label"] = None

        return booking

    def _enrich_bookings_batch(self, bookings: list[dict]) -> list[dict]:
        """Batch-enrich a list of bookings with display names.

        Fetches all locations, cart types, timeslots, and carts in 4 queries
        total (instead of 4 per booking) to avoid N+1 round-trips to Neon.
        """
        if not bookings:
            return bookings

        locations = {r["id"]: r for r in self.location_repository.find_all()}
        cart_types = {ct["id"]: ct for ct in self.cart_type_repository.find_all()}
        timeslots = {ts["id"]: ts for ts in self.timeslot_repository.find_all()}
        carts = {c["id"]: c for c in self.cart_repository.find_all()}

        for booking in bookings:
            region = locations.get(booking.get("region_id"))
            booking["region_name"] = region.get("name") if region else None

            cart_type = cart_types.get(booking.get("cart_type_id"))
            booking["cart_type_name"] = cart_type.get("name") if cart_type else None

            timeslot = timeslots.get(booking.get("timeslot_id"))
            if timeslot:
                booking["timeslot_label"] = (
                    f"{timeslot.get('start_time', '?')} - {timeslot.get('end_time', '?')}"
                )
            else:
                booking["timeslot_label"] = None

            assigned = booking.get("assigned_cart_id")
            if assigned is not None:
                cart = carts.get(assigned)
                booking["cart_label"] = (
                    cart.get("label") if cart and cart.get("label") else f"Ground {assigned}"
                )
            else:
                booking["cart_label"] = None

        return bookings

    # ── CRUD ──────────────────────────────────────────────────────────

    def create_booking(self, booking_data: dict) -> dict:
        """
        Create a new booking in PENDING_PAYMENT status.

        Flow:
        1. Validate FK existence (user, region, cart_type, timeslot)
        2. Fetch fee config and apply booking_fee + snapshot pcts
        3. Validate and snapshot items (if provided) -- before any side effects
        4. Enforce daily user limit
        5. Create booking as PENDING_PAYMENT (no cart assignment, no payment)
        6. Persist BookingItem records (after booking exists)

        Cart assignment is deferred to confirm_booking() after payment approval.
        """
        # 1. Validate all foreign keys
        self._validate_user(booking_data["user_id"])
        self._validate_region(booking_data["region_id"])
        self._validate_cart_type(booking_data["cart_type_id"])
        timeslot = self._validate_timeslot(booking_data["timeslot_id"])

        # 2. Fetch fee config — server-side booking fee enforcement
        fee_config = self.fee_config_repository.find_by_region_and_cart_type(
            booking_data["region_id"], booking_data["cart_type_id"]
        )
        if not fee_config or not fee_config.get("is_active", False):
            raise ValueError(
                "No active fee configuration found for this region and cart type."
            )

        # Apply server-side values (ignore any client-sent booking_fee)
        booking_data["booking_fee"] = fee_config["booking_fee"]
        booking_data["cancellation_fee_pct_snapshot"] = fee_config[
            "cancellation_fee_pct"
        ]
        booking_data["platform_fee_pct_snapshot"] = fee_config["platform_fee_pct"]

        # 3. Validate items and compute estimated_total (before side effects)
        items_input = booking_data.pop("items", None) or []
        if items_input:
            validated_snapshots = self.booking_item_service.validate_items(
                booking_data["cart_type_id"],
                items_input,
            )
            estimated_total = self.booking_item_service.calculate_estimated_total(
                validated_snapshots,
            )
        else:
            validated_snapshots = []
            estimated_total = Decimal("0.00")

        booking_data["estimated_total"] = float(estimated_total)

        tx_session = self.booking_repository._session_factory()
        try:
            # 4. Enforce daily user limit
            self._enforce_daily_user_limit(
                booking_data["user_id"],
                timeslot["date"],
                session=tx_session,
            )

            # 5. Create booking as PENDING_PAYMENT -- no cart, no payment
            booking_data["assigned_cart_id"] = None
            booking_data["status"] = "PENDING_PAYMENT"
            booking_data["payment_status"] = "PENDING"
            booking_data["refund_status"] = "NONE"
            booking_data["refund_amount"] = 0.0
            booking_data["date"] = timeslot["date"]

            booking = self.booking_repository.create(booking_data, session=tx_session)

            # 6. Persist BookingItem records after booking is created
            if validated_snapshots:
                booking["items"] = self.booking_item_service.create_booking_items(
                    booking["id"], validated_snapshots, session=tx_session
                )

            tx_session.commit()
            return booking
        except Exception:
            tx_session.rollback()
            raise
        finally:
            tx_session.close()

    def create_instant_match_booking(self, booking_data: dict) -> dict:
        """
        Create a CONFIRMED booking with a cart claimed atomically — for matchmaking engine use only.

        Unlike create_booking(), this method:
        - Skips the payment-approval gate (the match itself is the authorization).
        - Immediately claims an AVAILABLE cart (same lock strategy as confirm_booking).
        - Sets status = CONFIRMED and assigned_cart_id in one transaction.

        Returns the confirmed booking dict including assigned_cart_id and cart_id fields.

        Raises ValueError if:
        - Any FK validation fails
        - No active fee config exists
        - No available cart can be found for the region/cart_type
        """
        # 1. Validate all foreign keys
        self._validate_user(booking_data["user_id"])
        self._validate_region(booking_data["region_id"])
        self._validate_cart_type(booking_data["cart_type_id"])
        timeslot = self._validate_timeslot(booking_data["timeslot_id"])

        # 2. Fetch fee config
        fee_config = self.fee_config_repository.find_by_region_and_cart_type(
            booking_data["region_id"], booking_data["cart_type_id"]
        )
        if not fee_config or not fee_config.get("is_active", False):
            raise ValueError(
                "No active fee configuration found for this region and cart type."
            )

        booking_data["booking_fee"] = fee_config["booking_fee"]
        booking_data["cancellation_fee_pct_snapshot"] = fee_config[
            "cancellation_fee_pct"
        ]
        booking_data["platform_fee_pct_snapshot"] = fee_config["platform_fee_pct"]
        booking_data["estimated_total"] = 0.0
        booking_data["date"] = timeslot["date"]
        booking_data["payment_status"] = "PENDING"
        booking_data["refund_status"] = "NONE"
        booking_data["refund_amount"] = 0.0
        booking_data.pop("items", None)  # no items for instant match bookings

        tx_session = self.booking_repository._session_factory()
        try:
            # 3. Atomically claim a cart (row-level lock)
            cart = self.cart_repository.claim_available_cart(
                booking_data["region_id"],
                booking_data["cart_type_id"],
                session=tx_session,
            )
            if cart is None:
                raise ValueError(
                    "No available cart found for the selected region and cart type."
                )

            # 4. Create booking directly as CONFIRMED with the claimed cart
            booking_data["assigned_cart_id"] = cart.id
            booking_data["status"] = "CONFIRMED"

            booking = self.booking_repository.create(booking_data, session=tx_session)
            # Expose cart_id at top level for convenience (match engine reads this)
            booking["cart_id"] = cart.id

            tx_session.commit()
            return booking
        except Exception:
            tx_session.rollback()
            raise
        finally:
            tx_session.close()

    def confirm_booking(self, booking_id: int) -> dict:
        """
        Admin confirms a booking after payment approval.

        All validation and state changes happen inside a single DB
        transaction so that cart claiming and booking confirmation are
        atomic.  The cart row is locked with FOR UPDATE SKIP LOCKED to
        prevent two concurrent confirmations from grabbing the same cart.

        Order of operations:
        1. Load booking, run expiry check
        2. Validate booking.status == PENDING_PAYMENT
        3. Validate payment.status == SUCCESS
        4. Atomically claim an AVAILABLE cart (row-lock + set BUSY)
        5. Assign cart to booking and set status = CONFIRMED
        6. Commit — or rollback on any failure
        """
        tx_session = self.booking_repository._session_factory()
        try:
            # 1. Load booking
            booking = self.booking_repository.find_by_id(booking_id, session=tx_session)
            if not booking:
                raise ValueError("Booking not found.")

            booking = self._check_and_expire(booking)

            # 2. Validate booking status
            if booking["status"] != "PENDING_PAYMENT":
                raise ValueError(
                    f"Cannot confirm booking in {booking['status']} status. "
                    f"Only PENDING_PAYMENT bookings can be confirmed."
                )

            # 3. Validate payment status
            payment = self.payment_repository.find_by_booking_id(
                booking_id, session=tx_session
            )
            if not payment:
                raise ValueError("No payment found for this booking.")
            if payment["status"] != "SUCCESS":
                raise ValueError(
                    f"Payment is in {payment['status']} status. "
                    f"Only bookings with SUCCESS payment can be confirmed."
                )

            # 4. Atomically claim a cart (row-level lock prevents races)
            cart = self.cart_repository.claim_available_cart(
                booking["region_id"], booking["cart_type_id"], session=tx_session
            )
            if cart is None:
                raise ValueError("No cart available at confirmation time.")

            # 5. Assign cart and confirm booking in the same transaction
            confirmed = self.booking_repository.update(
                booking_id,
                {
                    "assigned_cart_id": cart.id,
                    "status": "CONFIRMED",
                },
                session=tx_session,
            )

            tx_session.commit()
            return confirmed
        except Exception:
            tx_session.rollback()
            raise
        finally:
            tx_session.close()

    def get_booking(self, booking_id: int) -> dict | None:
        """Retrieve a single booking by ID. Runs lazy expiry check."""
        booking = self.booking_repository.find_by_id(booking_id)
        if booking:
            booking = self._check_and_expire(booking)
            booking = self._enrich_booking(booking)
        return booking

    def list_bookings(self) -> list[dict]:
        """Retrieve all bookings. Runs lazy expiry check on each."""
        bookings = self.booking_repository.find_all()
        bookings = [self._check_and_expire(b) for b in bookings]
        return self._enrich_bookings_batch(bookings)

    def list_bookings_by_user(self, user_id: int) -> list[dict]:
        """Retrieve all bookings belonging to a specific user. Runs lazy expiry check."""
        bookings = self.booking_repository.find_by_user_id(user_id)
        bookings = [self._check_and_expire(b) for b in bookings]
        return self._enrich_bookings_batch(bookings)

    def list_bookings_by_region(self, region_id: int) -> list[dict]:
        """Retrieve all bookings for a specific region (ground_owner isolation)."""
        bookings = self.booking_repository.find_by_region_id(region_id)
        bookings = [self._check_and_expire(b) for b in bookings]
        return self._enrich_bookings_batch(bookings)

    def cancel_booking(self, booking_id: int) -> dict:
        """
        Cancel an existing booking.

        Allowed from: PENDING_PAYMENT, CONFIRMED
        Blocked for: IN_PROGRESS, COMPLETED, EXPIRED, CANCELLED

        If PENDING_PAYMENT: just cancel (no cart to release, no refund).
        If CONFIRMED: release cart, initiate refund via PaymentService if payment SUCCESS.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")

        if booking["status"] not in ("PENDING_PAYMENT", "CONFIRMED"):
            raise ValueError(
                f"Cannot cancel booking in {booking['status']} status. "
                f"Only PENDING_PAYMENT or CONFIRMED bookings can be cancelled."
            )

        if booking["status"] == "CONFIRMED":
            # Release the assigned cart
            if booking["assigned_cart_id"]:
                self.cart_repository.update(
                    booking["assigned_cart_id"], {"status": "AVAILABLE"}
                )

            # Refund via PaymentService if payment was successful
            payment = self.payment_repository.find_by_booking_id(booking_id)
            if payment and payment["status"] == "SUCCESS":
                self.payment_service.process_refund(payment["id"], booking)

        return self.booking_repository.update(
            booking_id,
            {
                "status": "CANCELLED",
            },
        )

    def expire_booking(self, booking_id: int) -> dict:
        """
        Expire a booking that has been in PENDING_PAYMENT too long.

        Only allowed if status == PENDING_PAYMENT.
        Checks if created_at is older than BOOKING_EXPIRY_MINUTES.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")

        if booking["status"] != "PENDING_PAYMENT":
            raise ValueError("Only PENDING_PAYMENT bookings can be expired.")

        created_at = booking["created_at"]
        if isinstance(created_at, str):
            created_at = datetime.fromisoformat(created_at)

        if datetime.utcnow() - created_at <= timedelta(minutes=BOOKING_EXPIRY_MINUTES):
            raise ValueError("Booking has not yet exceeded the expiry window.")

        return self.booking_repository.update(
            booking_id,
            {
                "status": "EXPIRED",
            },
        )

    def start_booking(self, booking_id: int) -> dict:
        """
        Start a confirmed booking (CONFIRMED → IN_PROGRESS). Admin only.

        Flow:
        1. Validate booking exists and status is CONFIRMED
        2. Set booking status to IN_PROGRESS
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")

        if booking["status"] != "CONFIRMED":
            raise ValueError(
                f"Cannot start booking in {booking['status']} status. "
                f"Only CONFIRMED bookings can be started."
            )

        updated = self.booking_repository.update(
            booking_id,
            {
                "status": "IN_PROGRESS",
            },
        )
        # TODO: send notification to user that service has started
        return updated

    def complete_booking(self, booking_id: int) -> dict:
        """
        Mark a booking as completed.

        Flow:
        1. Validate booking exists and status is IN_PROGRESS
        2. Release assigned cart (set status to AVAILABLE)
        3. Set booking status to COMPLETED
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")

        if booking["status"] != "IN_PROGRESS":
            raise ValueError(
                f"Cannot complete booking in {booking['status']} status. "
                f"Only IN_PROGRESS bookings can be completed."
            )

        # Release the assigned cart
        if booking["assigned_cart_id"]:
            self.cart_repository.update(
                booking["assigned_cart_id"], {"status": "AVAILABLE"}
            )

        return self.booking_repository.update(
            booking_id,
            {
                "status": "COMPLETED",
            },
        )

    def start_session(self, booking_id: int) -> dict:
        """Captain/admin starts the session timer.

        Allowed only from CONFIRMED. Sets session_started_at and moves to IN_PROGRESS.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "CONFIRMED":
            raise ValueError(
                f"Cannot start session for a booking in {booking['status']} status. "
                "Only CONFIRMED bookings can start."
            )
        return self.booking_repository.update(
            booking_id,
            {
                "status": "IN_PROGRESS",
                "session_started_at": datetime.utcnow(),
            },
        )

    def end_session(self, booking_id: int) -> dict:
        """Captain/admin ends the session. Computes the time bill and moves to
        AWAITING_TIME_PAYMENT.
        """
        from modules.billing.billing_calculator import compute_session_bill

        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "IN_PROGRESS":
            raise ValueError(
                f"Cannot end session for a booking in {booking['status']} status. "
                "Only IN_PROGRESS bookings can end."
            )
        if not booking.get("session_started_at"):
            raise ValueError("Booking has no session start time.")

        config = self.fee_config_repository.find_by_region_and_cart_type(
            booking["region_id"], booking["cart_type_id"]
        )
        if not config:
            raise ValueError("No pricing config found for this region + sport.")

        started_at = booking["session_started_at"]
        if isinstance(started_at, str):
            started_at = datetime.fromisoformat(started_at)

        surge = (
            float(config["surge_multiplier"])
            if config.get("surge_enabled")
            else 1.0
        )
        breakdown = compute_session_bill(
            started_at=started_at,
            ended_at=datetime.utcnow(),
            rate_per_block=float(config["rate_per_block"]),
            block_duration_minutes=int(config["block_duration_minutes"]),
            max_duration_minutes=int(config["max_duration_minutes"]),
            surge_multiplier=surge,
        )

        update = {
            "status": "AWAITING_TIME_PAYMENT",
            "session_ended_at": datetime.utcnow(),
            **breakdown,
        }
        updated = self.booking_repository.update(booking_id, update)

        # Create the TIME_BILL payment record for the user to pay.
        # NOTE: create_time_bill_payment is added in Task 7 (forward reference).
        self.payment_service.create_time_bill_payment(
            booking_id, breakdown["time_bill_amount"]
        )
        return updated

    def session_status(self, booking_id: int) -> dict:
        """Return a live billing estimate for an in-progress session."""
        from modules.billing.billing_calculator import compute_session_bill

        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")
        if booking["status"] != "IN_PROGRESS" or not booking.get("session_started_at"):
            return {
                "booking_id": booking_id,
                "status": booking["status"],
                "running": False,
                "elapsed_minutes": 0,
                "current_blocks": 0,
                "estimated_time_bill": 0.0,
            }

        config = self.fee_config_repository.find_by_region_and_cart_type(
            booking["region_id"], booking["cart_type_id"]
        )
        started_at = booking["session_started_at"]
        if isinstance(started_at, str):
            started_at = datetime.fromisoformat(started_at)
        surge = (
            float(config["surge_multiplier"])
            if config and config.get("surge_enabled")
            else 1.0
        )
        breakdown = compute_session_bill(
            started_at=started_at,
            ended_at=datetime.utcnow(),
            rate_per_block=float(config["rate_per_block"]) if config else 0.0,
            block_duration_minutes=int(config["block_duration_minutes"]) if config else 45,
            max_duration_minutes=int(config["max_duration_minutes"]) if config else 180,
            surge_multiplier=surge,
        )
        return {
            "booking_id": booking_id,
            "status": booking["status"],
            "running": True,
            "elapsed_minutes": breakdown["session_minutes"],
            "current_blocks": breakdown["session_blocks"],
            "estimated_time_bill": breakdown["time_bill_amount"],
        }
