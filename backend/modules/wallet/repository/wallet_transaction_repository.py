from core.database.db_connection import SessionLocal
from modules.wallet.model.wallet_transaction_model import WalletTransaction, WalletTransactionType


class WalletTransactionRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(
        self,
        user_id: int,
        type_: str,
        amount: int,
        reason: str,
        description: str,
        match_id: int | None = None,
    ) -> dict:
        session = self._session_factory()
        try:
            tx = WalletTransaction(
                user_id=user_id,
                type=type_,
                amount=amount,
                reason=reason,
                description=description,
                match_id=match_id,
            )
            session.add(tx)
            session.commit()
            session.refresh(tx)
            return tx.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_user(self, user_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = (
                session.query(WalletTransaction)
                .filter(WalletTransaction.user_id == user_id)
                .order_by(WalletTransaction.created_at.desc())
                .all()
            )
            return [r.to_dict() for r in rows]
        finally:
            session.close()

    def get_balance(self, user_id: int) -> int:
        session = self._session_factory()
        try:
            rows = (
                session.query(WalletTransaction)
                .filter(WalletTransaction.user_id == user_id)
                .all()
            )
            balance = 0
            for r in rows:
                if r.type == WalletTransactionType.CREDIT:
                    balance += r.amount
                else:
                    balance -= r.amount
            return balance
        finally:
            session.close()

    def has_bonus_for_match(self, user_id: int, match_id: int, reason: str) -> bool:
        """Idempotency guard — a player should only earn a given bonus once per match."""
        session = self._session_factory()
        try:
            existing = (
                session.query(WalletTransaction)
                .filter(
                    WalletTransaction.user_id == user_id,
                    WalletTransaction.match_id == match_id,
                    WalletTransaction.reason == reason,
                )
                .first()
            )
            return existing is not None
        finally:
            session.close()


wallet_transaction_repository = WalletTransactionRepository()
