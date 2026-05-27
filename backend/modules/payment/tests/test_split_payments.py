"""
Unit tests for PaymentService.create_split_payments().

Verifies:
- Two PENDING payment records are created (one per player).
- Amount is split equally: (estimated_total + booking_fee) / 2.
- Reference codes follow VMS-{booking_id}-P{user_id}-{rand} format.
- Payments are linked to the correct match_id and booking_id.
- Raises ValueError when match or booking is missing.
"""

import re
import unittest

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.database.db_connection import Base
from modules.user.model.user_model import User  # noqa: F401
from modules.location.model.location_model import Location  # noqa: F401
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.item.model.item_model import Item  # noqa: F401
from modules.booking.model.booking_model import Booking  # noqa: F401
from modules.booking_item.model.booking_item_model import BookingItem  # noqa: F401
from modules.sport.model.sport_model import Sport  # noqa: F401
from modules.match.model.match_model import Match, MatchPlayer  # noqa: F401
from modules.payment.model.payment_model import Payment  # noqa: F401
from modules.payment.model.system_config_model import SystemConfig  # noqa: F401
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig  # noqa: F401

from modules.booking.repository.booking_repository import BookingRepository
from modules.user.repository.user_repository import UserRepository
from modules.location.repository.location_repository import LocationRepository
from modules.cart_type.repository.cart_type_repository import CartTypeRepository
from modules.timeslot.repository.timeslot_repository import TimeslotRepository
from modules.cart.repository.cart_repository import CartRepository
from modules.payment.repository.payment_repository import PaymentRepository
from modules.payment.repository.system_config_repository import SystemConfigRepository
from modules.payment.service.payment_service import PaymentService


def _make_session_factory():
    engine = create_engine(
        "sqlite:///:memory:", connect_args={"check_same_thread": False}
    )
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestCreateSplitPayments(unittest.TestCase):
    """Unit tests for PaymentService.create_split_payments()."""

    def setUp(self):
        self.sf = _make_session_factory()

        # Create fixture users
        user_repo = UserRepository(session_factory=self.sf)
        self.user1 = user_repo.create({"name": "Alice", "phone": "9000000001"})
        self.user2 = user_repo.create({"name": "Bob", "phone": "9000000002"})

        # Create location / cart type
        loc_repo = LocationRepository(session_factory=self.sf)
        loc_repo.create({"name": "Downtown", "is_serviceable": True})

        ct_repo = CartTypeRepository(session_factory=self.sf)
        ct_repo.create({"name": "Badminton"})

        ts_repo = TimeslotRepository(session_factory=self.sf)
        ts_repo.create(
            {
                "location_id": 1,
                "date": "2026-04-01",
                "start_time": "09:00",
                "end_time": "10:00",
                "capacity": 2,
            }
        )

        cart_repo = CartRepository(session_factory=self.sf)
        cart_repo.create({"region_id": 1, "cart_type_id": 1, "status": "BUSY"})

        # Create a CONFIRMED booking with booking_fee=10, estimated_total=90 → total=100
        self.booking_repo = BookingRepository(session_factory=self.sf)
        self.booking = self.booking_repo.create(
            {
                "user_id": self.user1["id"],
                "region_id": 1,
                "cart_type_id": 1,
                "timeslot_id": 1,
                "assigned_cart_id": 1,
                "address": "Ground A",
                "booking_fee": 10.0,
                "estimated_total": 90.0,
                "status": "CONFIRMED",
                "payment_status": "PENDING",
                "refund_status": "NONE",
                "refund_amount": 0.0,
                "cancellation_fee_pct_snapshot": 0.0,
                "platform_fee_pct_snapshot": 0.0,
                "date": "2026-04-01",
            }
        )

        # Create a match linked to the booking
        session = self.sf()
        try:
            match = Match(
                created_by=self.user1["id"],
                region_id=1,
                cart_type_id=1,
                cart_id=1,
                booking_id=self.booking["id"],
                max_players=2,
                joined_players=2,
                status="COMPLETED",
            )
            session.add(match)
            session.flush()

            session.add(MatchPlayer(match_id=match.id, user_id=self.user1["id"]))
            session.add(MatchPlayer(match_id=match.id, user_id=self.user2["id"]))
            session.commit()
            self.match_id = match.id
        finally:
            session.close()

        # Build PaymentService wired to same in-memory DB
        self.payment_repo = PaymentRepository(session_factory=self.sf)
        config_repo = SystemConfigRepository(session_factory=self.sf)
        self.service = PaymentService(
            payment_repository=self.payment_repo,
            booking_repository=self.booking_repo,
            system_config_repository=config_repo,
        )
        # Override the internal SessionLocal used by create_split_payments
        import modules.payment.service.payment_service as ps_module

        self._orig_session_local = (
            ps_module.SessionLocal if hasattr(ps_module, "SessionLocal") else None
        )

    def _patch_session_factory(self):
        """Patch core.database.db_connection.SessionLocal to use our test DB."""
        import core.database.db_connection as db_mod

        self._orig_sl = db_mod.SessionLocal
        db_mod.SessionLocal = self.sf

    def _restore_session_factory(self):
        import core.database.db_connection as db_mod

        db_mod.SessionLocal = self._orig_sl

    def test_creates_two_payments(self):
        """create_split_payments returns exactly 2 payments for a 2-player match."""
        self._patch_session_factory()
        try:
            payments = self.service.create_split_payments(self.match_id)
        finally:
            self._restore_session_factory()

        self.assertEqual(len(payments), 2)

    def test_split_amount_correct(self):
        """Each payment amount equals (estimated_total + booking_fee) / 2 = 50.0."""
        self._patch_session_factory()
        try:
            payments = self.service.create_split_payments(self.match_id)
        finally:
            self._restore_session_factory()

        for p in payments:
            self.assertAlmostEqual(float(p["amount"]), 50.0)

    def test_payments_status_pending(self):
        """All created split payments must have status PENDING."""
        self._patch_session_factory()
        try:
            payments = self.service.create_split_payments(self.match_id)
        finally:
            self._restore_session_factory()

        for p in payments:
            self.assertEqual(p["status"], "PENDING")

    def test_payments_linked_to_match_and_booking(self):
        """Payments carry correct match_id and booking_id."""
        self._patch_session_factory()
        try:
            payments = self.service.create_split_payments(self.match_id)
        finally:
            self._restore_session_factory()

        for p in payments:
            self.assertEqual(p["match_id"], self.match_id)
            self.assertEqual(p["booking_id"], self.booking["id"])

    def test_reference_code_format(self):
        """Reference codes follow VMS-{booking_id}-P{user_id}-{4digits} format."""
        self._patch_session_factory()
        try:
            payments = self.service.create_split_payments(self.match_id)
        finally:
            self._restore_session_factory()

        pattern = re.compile(r"^VMS-\d+-P\d+-\d{4}$")
        for p in payments:
            self.assertRegex(p["reference_code"], pattern)

    def test_one_payment_per_player(self):
        """Each player gets their own payment (user_ids are distinct)."""
        self._patch_session_factory()
        try:
            payments = self.service.create_split_payments(self.match_id)
        finally:
            self._restore_session_factory()

        user_ids = [p["user_id"] for p in payments]
        self.assertEqual(
            len(user_ids), len(set(user_ids)), "Duplicate user_id found in payments"
        )

    def test_raises_if_match_not_found(self):
        """ValueError is raised when match_id does not exist."""
        self._patch_session_factory()
        try:
            with self.assertRaises(ValueError):
                self.service.create_split_payments(99999)
        finally:
            self._restore_session_factory()

    def test_raises_if_match_has_no_booking(self):
        """ValueError is raised when match has no associated booking_id."""
        session = self.sf()
        try:
            match = Match(
                created_by=self.user1["id"],
                region_id=1,
                cart_type_id=1,
                max_players=2,
                joined_players=1,
                status="COMPLETED",
                booking_id=None,
            )
            session.add(match)
            session.commit()
            orphan_match_id = match.id
        finally:
            session.close()

        self._patch_session_factory()
        try:
            with self.assertRaises(ValueError):
                self.service.create_split_payments(orphan_match_id)
        finally:
            self._restore_session_factory()


if __name__ == "__main__":
    unittest.main()
