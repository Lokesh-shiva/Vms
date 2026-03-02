from core.base.base_service import BaseService
from modules.fee_config.repository.fee_config_repository import (
    fee_config_repository as _default_repo,
)
from modules.location.repository.location_repository import (
    location_repository as _default_location_repo,
)
from modules.cart_type.repository.cart_type_repository import (
    cart_type_repository as _default_cart_type_repo,
)


class FeeConfigService(BaseService):
    """
    Business logic layer for RegionCartTypeConfig operations.

    All business validation (pct limits, negative checks, FK existence,
    uniqueness) lives here.  The controller / route layer only does
    structural / type validation via schemas.
    """

    def __init__(self, fee_config_repository=None, location_repository=None,
                 cart_type_repository=None):
        super().__init__()
        self.fee_config_repository = fee_config_repository or _default_repo
        self.location_repository = location_repository or _default_location_repo
        self.cart_type_repository = cart_type_repository or _default_cart_type_repo

    # ── Validation Helpers ────────────────────────────────────────────

    def _validate_fee_values(self, data: dict) -> None:
        """Validate booking_fee and percentages are non-negative and pct sum <= 100."""
        booking_fee = data.get("booking_fee")
        if booking_fee is not None and booking_fee < 0:
            raise ValueError("booking_fee cannot be negative.")

        cancellation_pct = data.get("cancellation_fee_pct", 0)
        platform_pct = data.get("platform_fee_pct", 0)

        if cancellation_pct < 0:
            raise ValueError("cancellation_fee_pct cannot be negative.")
        if platform_pct < 0:
            raise ValueError("platform_fee_pct cannot be negative.")
        if cancellation_pct + platform_pct > 100:
            raise ValueError(
                "cancellation_fee_pct + platform_fee_pct cannot exceed 100."
            )

    # ── CRUD ──────────────────────────────────────────────────────────

    def create_config(self, config_data: dict) -> dict:
        """
        Create a new fee config after business validation.

        Validates:
        - FK existence (region_id, cart_type_id)
        - booking_fee >= 0
        - percentages >= 0, sum <= 100
        - Uniqueness of (region_id, cart_type_id)
        """
        # FK validation
        region = self.location_repository.find_by_id(config_data["region_id"])
        if not region:
            raise ValueError("Referenced region does not exist.")

        cart_type = self.cart_type_repository.find_by_id(config_data["cart_type_id"])
        if not cart_type:
            raise ValueError("Referenced cart type does not exist.")

        # Fee / pct validation
        self._validate_fee_values(config_data)

        # Uniqueness check
        existing = self.fee_config_repository.find_by_region_and_cart_type(
            config_data["region_id"], config_data["cart_type_id"]
        )
        if existing:
            raise ValueError(
                "A config already exists for this region + cart type combination."
            )

        return self.fee_config_repository.create(config_data)

    def get_config(self, config_id: int) -> dict | None:
        """Retrieve a fee config by ID."""
        return self.fee_config_repository.find_by_id(config_id)

    def get_config_by_region_and_cart_type(
        self, region_id: int, cart_type_id: int
    ) -> dict | None:
        """Retrieve the config for a specific region + cart type."""
        return self.fee_config_repository.find_by_region_and_cart_type(
            region_id, cart_type_id
        )

    def list_configs(self) -> list[dict]:
        """Retrieve all fee configs."""
        return self.fee_config_repository.find_all()

    def update_config(self, config_id: int, update_data: dict) -> dict | None:
        """
        Update an existing fee config.

        Re-validates fee/pct constraints against the merged values.
        Changes only affect future bookings (existing bookings use snapshots).
        """
        existing = self.fee_config_repository.find_by_id(config_id)
        if not existing:
            return None

        # Merge existing values with updates for validation
        merged = {
            "booking_fee": update_data.get("booking_fee", existing["booking_fee"]),
            "cancellation_fee_pct": update_data.get(
                "cancellation_fee_pct", existing["cancellation_fee_pct"]
            ),
            "platform_fee_pct": update_data.get(
                "platform_fee_pct", existing["platform_fee_pct"]
            ),
        }
        self._validate_fee_values(merged)

        return self.fee_config_repository.update(config_id, update_data)

    def delete_config(self, config_id: int) -> bool:
        """
        Soft-delete a fee config (sets is_active = False).

        Existing bookings are unaffected because they use snapshotted values.
        """
        return self.fee_config_repository.delete(config_id)
