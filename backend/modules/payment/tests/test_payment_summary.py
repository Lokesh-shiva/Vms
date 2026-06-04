"""Tests for PaymentRepository.get_summary()."""
import unittest
import random
import string
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.payment.model.payment_model import Payment  # noqa: F401
from modules.payment.repository.payment_repository import PaymentRepository
import modules.user.model.user_model  # noqa: F401
import modules.booking.model.booking_model  # noqa: F401
import modules.cart.model.cart_model  # noqa: F401
import modules.location.model.location_model  # noqa: F401
import modules.cart_type.model.cart_type_model  # noqa: F401
import modules.timeslot.model.timeslot_model  # noqa: F401
import modules.match.model.match_model  # noqa: F401
import modules.sport.model.sport_model  # noqa: F401


def _make_factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


def _make_payment(repo, status: str, amount: float):
    ref = "TEST-" + "".join(random.choices(string.ascii_uppercase, k=6))
    return repo.create({
        "booking_id": 1,
        "provider": "MANUAL_UPI",
        "payment_type": "MATCHING_FEE",
        "amount": amount,
        "reference_code": ref,
        "status": status,
    })


class TestPaymentSummary(unittest.TestCase):

    def setUp(self):
        self.repo = PaymentRepository(session_factory=_make_factory())

    def test_summary_counts_correctly(self):
        _make_payment(self.repo, "SUCCESS", 100.0)
        _make_payment(self.repo, "SUCCESS", 200.0)
        _make_payment(self.repo, "REFUNDED", 50.0)
        _make_payment(self.repo, "UNDER_REVIEW", 75.0)

        summary = self.repo.get_summary()
        self.assertAlmostEqual(summary["total_revenue"], 300.0)
        self.assertAlmostEqual(summary["total_refunded"], 50.0)
        self.assertEqual(summary["pending_count"], 1)
        self.assertEqual(summary["refunded_count"], 1)

    def test_summary_returns_zeros_when_empty(self):
        summary = self.repo.get_summary()
        self.assertEqual(summary["total_revenue"], 0.0)
        self.assertEqual(summary["total_refunded"], 0.0)
        self.assertEqual(summary["pending_count"], 0)
        self.assertEqual(summary["refunded_count"], 0)
