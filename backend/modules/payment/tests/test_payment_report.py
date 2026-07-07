"""Tests for PaymentService.get_report() — daily revenue/refund bucketing."""
import unittest
import random
import string
from datetime import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.payment.model.payment_model import Payment  # noqa: F401
from modules.payment.repository.payment_repository import PaymentRepository
from modules.payment.service.payment_service import PaymentService
import modules.user.model.user_model  # noqa: F401
import modules.booking.model.booking_model  # noqa: F401
import modules.cart.model.cart_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.cart_type.model.cart_type_model  # noqa: F401
import modules.timeslot.model.timeslot_model  # noqa: F401
import modules.match.model.match_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401
import modules.captain.model.captain_model  # noqa: F401
import modules.society.model.society_model  # noqa: F401


def _make_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


def _make_payment(session_factory, status: str, amount: float, created_at: datetime):
    session = session_factory()
    ref = "TEST-" + "".join(random.choices(string.ascii_uppercase, k=6))
    p = Payment(
        booking_id=1,
        provider="MANUAL_UPI",
        payment_type="MATCHING_FEE",
        amount=amount,
        reference_code=ref,
        status=status,
        created_at=created_at,
    )
    session.add(p)
    session.commit()
    session.close()


class TestPaymentReport(unittest.TestCase):

    def setUp(self):
        self.factory = _make_factory()
        self.repo = PaymentRepository(session_factory=self.factory)
        self.service = PaymentService(payment_repository=self.repo)

    def test_report_buckets_by_day(self):
        _make_payment(self.factory, "SUCCESS", 100.0, datetime(2026, 6, 1, 9, 0))
        _make_payment(self.factory, "SUCCESS", 50.0, datetime(2026, 6, 1, 15, 0))
        _make_payment(self.factory, "REFUNDED", 20.0, datetime(2026, 6, 1, 16, 0))
        _make_payment(self.factory, "SUCCESS", 200.0, datetime(2026, 6, 2, 10, 0))

        report = self.service.get_report(datetime(2026, 6, 1), datetime(2026, 6, 2, 23, 59))

        self.assertEqual(len(report), 2)
        self.assertEqual(report[0]["period"], "2026-06-01")
        self.assertAlmostEqual(report[0]["revenue"], 150.0)
        self.assertAlmostEqual(report[0]["refunded"], 20.0)
        self.assertEqual(report[0]["count"], 3)
        self.assertEqual(report[1]["period"], "2026-06-02")
        self.assertAlmostEqual(report[1]["revenue"], 200.0)

    def test_report_excludes_pending_and_out_of_range(self):
        _make_payment(self.factory, "UNDER_REVIEW", 999.0, datetime(2026, 6, 1, 9, 0))
        _make_payment(self.factory, "SUCCESS", 100.0, datetime(2026, 5, 1, 9, 0))

        report = self.service.get_report(datetime(2026, 6, 1), datetime(2026, 6, 30))
        self.assertEqual(report, [])

    def test_report_empty_range_returns_empty_list(self):
        report = self.service.get_report(datetime(2026, 1, 1), datetime(2026, 1, 31))
        self.assertEqual(report, [])
