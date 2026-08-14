import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.order.model.order_model import Order  # noqa: F401
from modules.order.model.order_item_model import OrderItem  # noqa: F401
from modules.order.repository.order_repository import OrderRepository
from modules.order.service.order_service import OrderService
import modules.user.model.user_model  # noqa: F401
import modules.item.model.item_model  # noqa: F401
import modules.cart_type.model.cart_type_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class _FakeItemRepo:
    def __init__(self, items: dict[int, dict]):
        self._items = items

    def find_by_id(self, item_id: int):
        return self._items.get(item_id)


class _FakeConfigRepo:
    def get(self, key: str):
        return None


class TestOrderService(unittest.TestCase):
    def setUp(self):
        self.order_repo = OrderRepository(session_factory=_factory())
        self.items = _FakeItemRepo({
            1: {"id": 1, "name": "Samosa", "price": 20.0, "is_available": True},
            2: {"id": 2, "name": "Water Bottle", "price": 15.0, "is_available": True},
            3: {"id": 3, "name": "Out of stock racket", "price": 500.0, "is_available": False},
        })
        self.service = OrderService(
            order_repository=self.order_repo, item_repository=self.items, system_config_repository=_FakeConfigRepo(),
        )

    def test_create_order_computes_total_server_side(self):
        order = self.service.create_order(1, [{"item_id": 1, "quantity": 2}, {"item_id": 2, "quantity": 1}])
        self.assertEqual(order["total_amount"], 55.0)
        self.assertEqual(order["status"], "PENDING_PAYMENT")
        self.assertEqual(len(order["items"]), 2)
        self.assertIn("upi_link", order)

    def test_create_order_rejects_empty_cart(self):
        with self.assertRaises(ValueError):
            self.service.create_order(1, [])

    def test_create_order_rejects_unavailable_item(self):
        with self.assertRaises(ValueError):
            self.service.create_order(1, [{"item_id": 3, "quantity": 1}])

    def test_create_order_rejects_unknown_item(self):
        with self.assertRaises(ValueError):
            self.service.create_order(1, [{"item_id": 999, "quantity": 1}])

    def test_create_order_rejects_bad_quantity(self):
        with self.assertRaises(ValueError):
            self.service.create_order(1, [{"item_id": 1, "quantity": 0}])

    def test_submit_payment_moves_to_under_review(self):
        order = self.service.create_order(1, [{"item_id": 1, "quantity": 1}])
        updated = self.service.submit_payment(order["id"], 1, "TXN123")
        self.assertEqual(updated["status"], "UNDER_REVIEW")
        self.assertEqual(updated["transaction_id"], "TXN123")

    def test_submit_payment_rejects_wrong_user(self):
        order = self.service.create_order(1, [{"item_id": 1, "quantity": 1}])
        with self.assertRaises(ValueError):
            self.service.submit_payment(order["id"], 2, "TXN123")

    def test_submit_payment_rejects_missing_transaction_id(self):
        order = self.service.create_order(1, [{"item_id": 1, "quantity": 1}])
        with self.assertRaises(ValueError):
            self.service.submit_payment(order["id"], 1, "")

    def test_approve_order_requires_under_review(self):
        order = self.service.create_order(1, [{"item_id": 1, "quantity": 1}])
        with self.assertRaises(ValueError):
            self.service.approve_order(order["id"])

        self.service.submit_payment(order["id"], 1, "TXN123")
        approved = self.service.approve_order(order["id"])
        self.assertEqual(approved["status"], "PAID")

    def test_reject_order(self):
        order = self.service.create_order(1, [{"item_id": 1, "quantity": 1}])
        self.service.submit_payment(order["id"], 1, "TXN123")
        rejected = self.service.reject_order(order["id"])
        self.assertEqual(rejected["status"], "REJECTED")

    def test_get_order_ownership_check(self):
        order = self.service.create_order(1, [{"item_id": 1, "quantity": 1}])
        self.service.get_order(order["id"], 1)  # no raise
        with self.assertRaises(ValueError):
            self.service.get_order(order["id"], 2)
        self.service.get_order(order["id"], 2, is_admin=True)  # admin bypass, no raise

    def test_list_my_orders(self):
        self.service.create_order(1, [{"item_id": 1, "quantity": 1}])
        self.service.create_order(1, [{"item_id": 2, "quantity": 1}])
        self.service.create_order(2, [{"item_id": 1, "quantity": 1}])
        self.assertEqual(len(self.service.list_my_orders(1)), 2)
        self.assertEqual(len(self.service.list_my_orders(2)), 1)


if __name__ == "__main__":
    unittest.main()
