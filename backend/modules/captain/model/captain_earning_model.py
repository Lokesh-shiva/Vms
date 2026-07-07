from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text
from core.database.db_connection import Base


class EarningStatus:
    PENDING = "PENDING"
    PAID = "PAID"

    ALL: frozenset[str] = frozenset({PENDING, PAID})


class CaptainEarning(Base):
    """
    One row per completed match a captain led — the source of truth for
    captain payouts.

    amount is snapshotted at creation time (the CAPTAIN_FEE_PER_MATCH value
    when the match completed), so later fee changes never retroactively
    alter past earnings. status flips PENDING -> PAID when an admin marks
    a manual UPI/bank payout as sent.
    """

    __tablename__ = "captain_earnings"

    id = Column(Integer, primary_key=True, autoincrement=True)
    captain_id = Column(Integer, ForeignKey("captains.id", ondelete="CASCADE"), nullable=False, index=True)
    match_id = Column(Integer, ForeignKey("matches.id", ondelete="SET NULL"), nullable=True, index=True)
    amount = Column(Integer, nullable=False)
    status = Column(String(20), nullable=False, default=EarningStatus.PENDING)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    paid_at = Column(DateTime, nullable=True)
    payout_reference = Column(Text, nullable=True)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "captain_id": self.captain_id,
            "match_id": self.match_id,
            "amount": self.amount,
            "status": self.status,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "paid_at": self.paid_at.isoformat() if self.paid_at else None,
            "payout_reference": self.payout_reference,
        }

    def __repr__(self) -> str:
        return f"<CaptainEarning id={self.id} captain_id={self.captain_id} amount={self.amount} status={self.status}>"
