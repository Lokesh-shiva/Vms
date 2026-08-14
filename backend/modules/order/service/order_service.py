import os
import random

from modules.item.repository.item_repository import item_repository as _default_item_repo
from modules.order.repository.order_repository import order_repository as _default_order_repo
from modules.payment.repository.system_config_repository import (
    system_config_repository as _default_config_repo,
)

_ENV_UPI_ID = os.getenv("UPI_ID", "vms@upi")
_ENV_MERCHANT_NAME = os.getenv("MERCHANT_NAME", "VMS")
UPI_ID_CONFIG_KEY = "UPI_ID"
MERCHANT_NAME_CONFIG_KEY = "MERCHANT_NAME"

MAX_REFCODE_ATTEMPTS = 5


class OrderService:
    """Shop checkout via the same manual-UPI-reference + admin-approval
    workflow already used for booking payments — no payment gateway,
    matching this project's existing architecture."""

    def __init__(self, order_repository=None, item_repository=None, system_config_repository=None):
        self._orders = order_repository or _default_order_repo
        self._items = item_repository or _default_item_repo
        self._config = system_config_repository or _default_config_repo

    def _get_active_upi_id(self) -> str:
        return self._config.get(UPI_ID_CONFIG_KEY) or _ENV_UPI_ID

    def _get_active_merchant_name(self) -> str:
        return self._config.get(MERCHANT_NAME_CONFIG_KEY) or _ENV_MERCHANT_NAME

    def _generate_reference_code(self, user_id: int) -> str:
        suffix = f"{random.randint(0, 9999):04d}"
        return f"ORD-{user_id}-{suffix}"

    def create_order(self, user_id: int, cart: list[dict]) -> dict:
        """cart: list of {item_id, quantity}. Validates each item is available,
        snapshots its current name/price, and computes the total server-side
        (never trusts a client-supplied price)."""
        if not cart:
            raise ValueError("Cart is empty.")

        line_items = []
        total = 0.0
        for entry in cart:
            item_id = entry.get("item_id")
            quantity = entry.get("quantity", 1)
            if not isinstance(quantity, int) or quantity < 1:
                raise ValueError("Quantity must be a positive integer.")

            item = self._items.find_by_id(item_id)
            if not item:
                raise ValueError(f"Item {item_id} not found.")
            if not item["is_available"]:
                raise ValueError(f"'{item['name']}' is not currently available.")

            unit_price = float(item["price"])
            line_items.append({
                "item_id": item["id"],
                "name_snapshot": item["name"],
                "unit_price_snapshot": unit_price,
                "quantity": quantity,
            })
            total += unit_price * quantity

        order = None
        for attempt in range(MAX_REFCODE_ATTEMPTS):
            reference_code = self._generate_reference_code(user_id)
            try:
                order = self._orders.create(user_id, reference_code, total, line_items)
                break
            except Exception:
                if attempt == MAX_REFCODE_ATTEMPTS - 1:
                    raise RuntimeError(
                        f"Failed to generate a unique order reference after {MAX_REFCODE_ATTEMPTS} attempts."
                    )

        upi_id = self._get_active_upi_id()
        merchant_name = self._get_active_merchant_name()
        formatted_amount = f"{total:.2f}"
        upi_link = (
            f"upi://pay?pa={upi_id}&pn={merchant_name}"
            f"&am={formatted_amount}&cu=INR&tn={order['reference_code']}"
        )
        return {**order, "upi_id": upi_id, "upi_link": upi_link}

    def submit_payment(self, order_id: int, user_id: int, transaction_id: str) -> dict:
        order = self._orders.find_by_id(order_id)
        if not order:
            raise ValueError("Order not found.")
        if order["user_id"] != user_id:
            raise ValueError("You can only submit payment for your own order.")
        if order["status"] != "PENDING_PAYMENT":
            raise ValueError("Order is not awaiting payment.")
        if not transaction_id or not transaction_id.strip():
            raise ValueError("Transaction ID is required.")

        return self._orders.update(order_id, {
            "transaction_id": transaction_id.strip(),
            "status": "UNDER_REVIEW",
        })

    def get_order(self, order_id: int, user_id: int, is_admin: bool = False) -> dict:
        order = self._orders.find_by_id(order_id)
        if not order:
            raise ValueError("Order not found.")
        if not is_admin and order["user_id"] != user_id:
            raise ValueError("You can only view your own orders.")
        return order

    def list_my_orders(self, user_id: int) -> list[dict]:
        return self._orders.find_by_user(user_id)

    def list_all_orders(self, status: str | None = None) -> list[dict]:
        return self._orders.find_all(status)

    def approve_order(self, order_id: int) -> dict:
        order = self._orders.find_by_id(order_id)
        if not order:
            raise ValueError("Order not found.")
        if order["status"] != "UNDER_REVIEW":
            raise ValueError("Only orders under review can be approved.")
        return self._orders.update(order_id, {"status": "PAID"})

    def reject_order(self, order_id: int) -> dict:
        order = self._orders.find_by_id(order_id)
        if not order:
            raise ValueError("Order not found.")
        if order["status"] != "UNDER_REVIEW":
            raise ValueError("Only orders under review can be rejected.")
        return self._orders.update(order_id, {"status": "REJECTED"})


order_service = OrderService()
