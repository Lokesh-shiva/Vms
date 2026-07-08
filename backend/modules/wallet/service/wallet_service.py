from modules.wallet.repository.wallet_transaction_repository import (
    WalletTransactionRepository,
    wallet_transaction_repository as _default_repo,
)
from modules.wallet.model.wallet_transaction_model import WalletTransactionType, WalletTransactionReason
from modules.payment.repository.system_config_repository import (
    SystemConfigRepository,
    system_config_repository as _default_config_repo,
)

DEFAULT_MATCH_COMPLETION_BONUS = 20


class WalletService:
    """
    Earn-only coin wallet: balance is entirely derived from system-awarded
    bonuses (currently: match completion). There is no purchase/top-up path.
    """

    def __init__(self, repository: WalletTransactionRepository | None = None, system_config_repository: SystemConfigRepository | None = None):
        self.repository = repository or _default_repo
        self.config_repository = system_config_repository or _default_config_repo

    def get_match_completion_bonus(self) -> int:
        raw = self.config_repository.get("WALLET_MATCH_COMPLETION_BONUS")
        try:
            return int(raw) if raw is not None else DEFAULT_MATCH_COMPLETION_BONUS
        except (TypeError, ValueError):
            return DEFAULT_MATCH_COMPLETION_BONUS

    def get_balance(self, user_id: int) -> int:
        return self.repository.get_balance(user_id)

    def get_transactions(self, user_id: int) -> list[dict]:
        """API-facing shape: signed amount, lowercase type (matches the app's Retrofit model)."""
        rows = self.repository.find_by_user(user_id)
        result = []
        for r in rows:
            signed_amount = r["amount"] if r["type"] == WalletTransactionType.CREDIT else -r["amount"]
            result.append({
                "id": r["id"],
                "type": r["type"].lower(),
                "amount": signed_amount,
                "description": r["description"],
                "created_at": r["created_at"],
            })
        return result

    def award_match_completion_bonus(self, user_id: int, match_id: int) -> dict | None:
        """
        Credit a player's wallet for completing a match. Idempotent per
        (user, match) — safe to call once per player when a match finishes.
        """
        if self.repository.has_bonus_for_match(user_id, match_id, WalletTransactionReason.MATCH_COMPLETION_BONUS):
            return None
        amount = self.get_match_completion_bonus()
        return self.repository.create(
            user_id=user_id,
            type_=WalletTransactionType.CREDIT,
            amount=amount,
            reason=WalletTransactionReason.MATCH_COMPLETION_BONUS,
            description="Match completion bonus",
            match_id=match_id,
        )


wallet_service = WalletService()
