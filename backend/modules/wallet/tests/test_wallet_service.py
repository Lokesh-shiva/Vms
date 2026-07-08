import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.wallet.model.wallet_transaction_model import WalletTransaction  # noqa: F401
from modules.wallet.repository.wallet_transaction_repository import WalletTransactionRepository
from modules.wallet.service.wallet_service import WalletService, DEFAULT_MATCH_COMPLETION_BONUS
from modules.payment.model.system_config_model import SystemConfig  # noqa: F401
from modules.payment.repository.system_config_repository import SystemConfigRepository
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


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestWalletService(unittest.TestCase):
    def setUp(self):
        factory = _factory()
        self.repo = WalletTransactionRepository(session_factory=factory)
        self.config_repo = SystemConfigRepository(session_factory=factory)
        self.service = WalletService(repository=self.repo, system_config_repository=self.config_repo)

    def test_balance_zero_with_no_transactions(self):
        self.assertEqual(self.service.get_balance(1), 0)

    def test_award_match_completion_bonus_credits_default_amount(self):
        tx = self.service.award_match_completion_bonus(user_id=1, match_id=10)
        self.assertIsNotNone(tx)
        self.assertEqual(tx["amount"], DEFAULT_MATCH_COMPLETION_BONUS)
        self.assertEqual(self.service.get_balance(1), DEFAULT_MATCH_COMPLETION_BONUS)

    def test_award_match_completion_bonus_respects_system_config(self):
        self.config_repo.set("WALLET_MATCH_COMPLETION_BONUS", "50")
        self.service.award_match_completion_bonus(user_id=1, match_id=10)
        self.assertEqual(self.service.get_balance(1), 50)

    def test_award_match_completion_bonus_is_idempotent_per_match(self):
        self.service.award_match_completion_bonus(user_id=1, match_id=10)
        second = self.service.award_match_completion_bonus(user_id=1, match_id=10)
        self.assertIsNone(second)
        self.assertEqual(self.service.get_balance(1), DEFAULT_MATCH_COMPLETION_BONUS)

    def test_same_user_earns_separately_for_different_matches(self):
        self.service.award_match_completion_bonus(user_id=1, match_id=10)
        self.service.award_match_completion_bonus(user_id=1, match_id=11)
        self.assertEqual(self.service.get_balance(1), DEFAULT_MATCH_COMPLETION_BONUS * 2)

    def test_get_transactions_returns_signed_amount_and_lowercase_type(self):
        self.service.award_match_completion_bonus(user_id=1, match_id=10)
        transactions = self.service.get_transactions(1)
        self.assertEqual(len(transactions), 1)
        self.assertEqual(transactions[0]["type"], "credit")
        self.assertEqual(transactions[0]["amount"], DEFAULT_MATCH_COMPLETION_BONUS)
        self.assertEqual(transactions[0]["description"], "Match completion bonus")

    def test_balances_are_isolated_per_user(self):
        self.service.award_match_completion_bonus(user_id=1, match_id=10)
        self.service.award_match_completion_bonus(user_id=2, match_id=11)
        self.assertEqual(self.service.get_balance(1), DEFAULT_MATCH_COMPLETION_BONUS)
        self.assertEqual(self.service.get_balance(2), DEFAULT_MATCH_COMPLETION_BONUS)
