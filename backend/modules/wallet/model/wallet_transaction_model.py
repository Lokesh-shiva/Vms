from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text
from core.database.db_connection import Base


class WalletTransactionType:
    CREDIT = "CREDIT"
    DEBIT = "DEBIT"
    ALL: frozenset[str] = frozenset({CREDIT, DEBIT})


class WalletTransactionReason:
    """Earn-only model: coins are awarded by the system, never purchased."""
    MATCH_COMPLETION_BONUS = "MATCH_COMPLETION_BONUS"
    ALL: frozenset[str] = frozenset({MATCH_COMPLETION_BONUS})


class WalletTransaction(Base):
    """
    Append-only ledger row for the player-facing coin wallet.

    amount is always stored as a positive magnitude; type (CREDIT/DEBIT)
    carries the direction. Balance is the sum of credits minus debits —
    never mutated directly, always derived from this ledger.
    """

    __tablename__ = "wallet_transactions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    type = Column(String(10), nullable=False)
    amount = Column(Integer, nullable=False)
    reason = Column(String(50), nullable=False)
    description = Column(Text, nullable=False)
    match_id = Column(Integer, ForeignKey("matches.id", ondelete="SET NULL"), nullable=True, index=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "type": self.type,
            "amount": self.amount,
            "reason": self.reason,
            "description": self.description,
            "match_id": self.match_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self) -> str:
        return f"<WalletTransaction id={self.id} user_id={self.user_id} type={self.type} amount={self.amount}>"
