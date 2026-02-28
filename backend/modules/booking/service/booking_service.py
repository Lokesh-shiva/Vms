from decimal import Decimal

from core.base.base_service import BaseService
from modules.booking.repository.booking_repository import booking_repository as _default_booking_repo
from modules.user.repository.user_repository import user_repository as _default_user_repo
from modules.location.repository.location_repository import location_repository as _default_location_repo
from modules.cart_type.repository.cart_type_repository import cart_type_repository as _default_cart_type_repo
from modules.timeslot.repository.timeslot_repository import timeslot_repository as _default_timeslot_repo
from modules.cart.repository.cart_repository import cart_repository as _default_cart_repo
from modules.booking_item.service.booking_item_service import BookingItemService


# ── Constants ─────────────────────────────────────────────────────────

MAX_BOOKINGS_PER_USER_PER_DAY = 3


class BookingService(BaseService):
    """
    Business logic layer for Booking operations.

    Responsibilities:
    - Validates business rules before data access.
    - Orchestrates calls to BookingRepository and cross-module repositories.
    - Enforces slot capacity, daily user limits, and cart availability.
    - Simulates payment processing.
    - Manages cart lifecycle (assign on confirm, release on cancel/complete).
    - Returns formatted results to the controller.
    """

    def __init__(self, booking_repository=None, user_repository=None,
                 location_repository=None, cart_type_repository=None,
                 timeslot_repository=None, cart_repository=None,
                 booking_item_service=None):
        super().__init__()
        self.booking_repository = booking_repository or _default_booking_repo
        self.user_repository = user_repository or _default_user_repo
        self.location_repository = location_repository or _default_location_repo
        self.cart_type_repository = cart_type_repository or _default_cart_type_repo
        self.timeslot_repository = timeslot_repository or _default_timeslot_repo
        self.cart_repository = cart_repository or _default_cart_repo
        self.booking_item_service = booking_item_service or BookingItemService()

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

    def _enforce_slot_capacity(self, timeslot_id: int, capacity: int, session=None) -> None:
        """Ensure the timeslot has not reached its booking capacity."""
        current_count = self.booking_repository.count_by_timeslot(
            timeslot_id, session=session
        )
        if current_count >= capacity:
            raise ValueError("Timeslot is fully booked.")

    def _enforce_daily_user_limit(self, user_id: int, date: str, session=None) -> None:
        """Ensure the user has not exceeded the daily booking limit."""
        user_bookings = self.booking_repository.find_by_user_and_date(
            user_id, date, session=session
        )
        active_bookings = [
            b for b in user_bookings
            if b["status"] in ("PENDING_PAYMENT", "CONFIRMED")
        ]
        if len(active_bookings) >= MAX_BOOKINGS_PER_USER_PER_DAY:
            raise ValueError(
                f"User has reached the maximum of {MAX_BOOKINGS_PER_USER_PER_DAY} "
                f"bookings per day."
            )

    def _find_available_cart(self, region_id: int, cart_type_id: int, session=None) -> dict:
        """Find an available cart matching the region and cart type.

        Raises ValueError if no cart is available.
        """
        all_carts = self.cart_repository.find_all(session=session)
        for cart in all_carts:
            if (cart["region_id"] == region_id
                    and cart["cart_type_id"] == cart_type_id
                    and cart["status"] == "AVAILABLE"
                    and cart.get("is_active", True)):
                return cart
        raise ValueError("No available cart found for the selected region and cart type.")

    def _simulate_payment(self) -> str:
        """Simulate payment processing. Always returns SUCCESS for V1."""
        return "SUCCESS"

    # ── CRUD ──────────────────────────────────────────────────────────

    def create_booking(self, booking_data: dict) -> dict:
        """
        Create a new booking after applying all business rules.

        Flow:
        1. Validate FK existence (user, region, cart_type, timeslot)
        2. Validate and snapshot items (if provided) — before any side effects
        3. Enforce slot capacity
        4. Enforce daily user limit
        5. Check cart availability (fail if none)
        6. Simulate payment
        7. Assign cart (set cart status to BUSY)
        8. Create booking as CONFIRMED
        9. Persist BookingItem records (after booking exists)

        Args:
            booking_data: Validated booking input.

        Returns:
            The created booking record as a dict.

        Raises:
            ValueError: If any business validation fails.
        """
        # 1. Validate all foreign keys
        self._validate_user(booking_data["user_id"])
        self._validate_region(booking_data["region_id"])
        self._validate_cart_type(booking_data["cart_type_id"])
        timeslot = self._validate_timeslot(booking_data["timeslot_id"])

        # 2. Validate items and compute estimated_total (before side effects)
        items_input = booking_data.pop("items", None) or []
        if items_input:
            validated_snapshots = self.booking_item_service.validate_items(
                booking_data["cart_type_id"], items_input,
            )
            estimated_total = self.booking_item_service.calculate_estimated_total(
                validated_snapshots,
            )
        else:
            validated_snapshots = []
            estimated_total = Decimal("0.00")

        # Server-computed — never trust client value
        booking_data["estimated_total"] = float(estimated_total)

        tx_session = self.booking_repository._session_factory()
        try:
            # 3. Enforce slot capacity
            self._enforce_slot_capacity(
                booking_data["timeslot_id"],
                timeslot["capacity"],
                session=tx_session,
            )

            # 4. Enforce daily user limit
            self._enforce_daily_user_limit(
                booking_data["user_id"],
                timeslot["date"],
                session=tx_session,
            )

            # 5. Check cart availability — fail if none found
            cart = self._find_available_cart(
                booking_data["region_id"],
                booking_data["cart_type_id"],
                session=tx_session,
            )

            # 6. Simulate payment
            payment_status = self._simulate_payment()

            # 7. Assign cart → set cart status to BUSY
            self.cart_repository.update(
                cart["id"], {"status": "BUSY"}, session=tx_session
            )

            # 8. Create booking as CONFIRMED
            booking_data["assigned_cart_id"] = cart["id"]
            booking_data["status"] = "CONFIRMED"
            booking_data["payment_status"] = payment_status
            booking_data["refund_status"] = "NONE"
            booking_data["refund_amount"] = 0.0
            booking_data["date"] = timeslot["date"]  # Store date for daily limit queries

            booking = self.booking_repository.create(booking_data, session=tx_session)

            # 9. Persist BookingItem records after booking is created
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

    def get_booking(self, booking_id: int) -> dict | None:
        """Retrieve a single booking by ID."""
        return self.booking_repository.find_by_id(booking_id)

    def list_bookings(self) -> list[dict]:
        """Retrieve all bookings."""
        return self.booking_repository.find_all()

    def list_bookings_by_user(self, user_id: int) -> list[dict]:
        """Retrieve all bookings belonging to a specific user."""
        return self.booking_repository.find_by_user_id(user_id)

    def cancel_booking(self, booking_id: int) -> dict:
        """
        Cancel an existing booking.

        Flow:
        1. Validate booking exists and status is CONFIRMED
        2. Release assigned cart (set status to AVAILABLE)
        3. Set booking status to CANCELLED, refund_status to REFUNDED

        Args:
            booking_id: Target booking ID.

        Returns:
            The updated booking record.

        Raises:
            ValueError: If booking not found or not in CONFIRMED status.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")

        if booking["status"] != "CONFIRMED":
            raise ValueError("Only confirmed bookings can be cancelled.")

        # Release the assigned cart
        if booking["assigned_cart_id"]:
            self.cart_repository.update(
                booking["assigned_cart_id"], {"status": "AVAILABLE"}
            )

        # Update booking status
        return self.booking_repository.update(booking_id, {
            "status": "CANCELLED",
            "refund_status": "REFUNDED",
            "refund_amount": booking["booking_fee"],
        })

    def complete_booking(self, booking_id: int) -> dict:
        """
        Mark a booking as completed.

        Flow:
        1. Validate booking exists and status is CONFIRMED
        2. Release assigned cart (set status to AVAILABLE)
        3. Set booking status to COMPLETED

        Args:
            booking_id: Target booking ID.

        Returns:
            The updated booking record.

        Raises:
            ValueError: If booking not found or not in CONFIRMED status.
        """
        booking = self.booking_repository.find_by_id(booking_id)
        if not booking:
            raise ValueError("Booking not found.")

        if booking["status"] != "CONFIRMED":
            raise ValueError("Only confirmed bookings can be completed.")

        # Release the assigned cart
        if booking["assigned_cart_id"]:
            self.cart_repository.update(
                booking["assigned_cart_id"], {"status": "AVAILABLE"}
            )

        # Update booking status
        return self.booking_repository.update(booking_id, {
            "status": "COMPLETED",
        })
