import unittest
from datetime import date, timedelta
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.trainer.model.trainer_model import Trainer  # noqa: F401
from modules.trainer.model.trainer_booking_model import TrainerBooking  # noqa: F401
from modules.trainer.repository.trainer_repository import TrainerRepository
from modules.trainer.repository.trainer_booking_repository import TrainerBookingRepository
from modules.trainer.service.trainer_service import TrainerService
from modules.trainer.service.trainer_booking_service import TrainerBookingService
import modules.user.model.user_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class _FakeConfigRepo:
    def get(self, key: str):
        return None


class TestTrainerBookingService(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        self.trainer_repo = TrainerRepository(session_factory=factory)
        self.booking_repo = TrainerBookingRepository(session_factory=factory)
        self.trainer_service = TrainerService(trainer_repository=self.trainer_repo)
        self.today = date(2026, 8, 1)
        self.service = TrainerBookingService(
            booking_repository=self.booking_repo,
            trainer_repository=self.trainer_repo,
            system_config_repository=_FakeConfigRepo(),
            now_fn=lambda: self.today,
        )
        self.trainer = self.trainer_service.create_trainer({"name": "Coach Ravi", "rate_per_session": 500})

    def test_create_booking_computes_amount_from_trainer_rate(self):
        booking = self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")
        self.assertEqual(booking["amount"], 500.0)
        self.assertEqual(booking["status"], "PENDING_PAYMENT")
        self.assertIn("upi_link", booking)

    def test_create_booking_rejects_past_date(self):
        with self.assertRaises(ValueError):
            self.service.create_booking(1, self.trainer["id"], "2026-07-01", "18:00")

    def test_create_booking_rejects_bad_date_format(self):
        with self.assertRaises(ValueError):
            self.service.create_booking(1, self.trainer["id"], "05-08-2026", "18:00")

    def test_create_booking_rejects_bad_time_format(self):
        with self.assertRaises(ValueError):
            self.service.create_booking(1, self.trainer["id"], "2026-08-05", "6pm")

    def test_create_booking_rejects_unknown_trainer(self):
        with self.assertRaises(ValueError):
            self.service.create_booking(1, 999, "2026-08-05", "18:00")

    def test_create_booking_rejects_inactive_trainer(self):
        self.trainer_service.update_trainer(self.trainer["id"], {"is_active": False})
        with self.assertRaises(ValueError):
            self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")

    def test_submit_payment_moves_to_under_review(self):
        booking = self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")
        updated = self.service.submit_payment(booking["id"], 1, "TXN123")
        self.assertEqual(updated["status"], "UNDER_REVIEW")

    def test_submit_payment_rejects_wrong_user(self):
        booking = self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")
        with self.assertRaises(ValueError):
            self.service.submit_payment(booking["id"], 2, "TXN123")

    def test_approve_booking_requires_under_review(self):
        booking = self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")
        with self.assertRaises(ValueError):
            self.service.approve_booking(booking["id"])
        self.service.submit_payment(booking["id"], 1, "TXN123")
        approved = self.service.approve_booking(booking["id"])
        self.assertEqual(approved["status"], "CONFIRMED")

    def test_reject_booking(self):
        booking = self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")
        self.service.submit_payment(booking["id"], 1, "TXN123")
        rejected = self.service.reject_booking(booking["id"])
        self.assertEqual(rejected["status"], "REJECTED")

    def test_get_booking_ownership_check(self):
        booking = self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")
        self.service.get_booking(booking["id"], 1)
        with self.assertRaises(ValueError):
            self.service.get_booking(booking["id"], 2)
        self.service.get_booking(booking["id"], 2, is_admin=True)

    def test_list_my_bookings(self):
        self.service.create_booking(1, self.trainer["id"], "2026-08-05", "18:00")
        self.service.create_booking(1, self.trainer["id"], "2026-08-06", "18:00")
        self.service.create_booking(2, self.trainer["id"], "2026-08-05", "18:00")
        self.assertEqual(len(self.service.list_my_bookings(1)), 2)
        self.assertEqual(len(self.service.list_my_bookings(2)), 1)


if __name__ == "__main__":
    unittest.main()
