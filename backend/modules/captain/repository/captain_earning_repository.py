from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.captain.model.captain_earning_model import CaptainEarning, EarningStatus


class CaptainEarningRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, captain_id: int, match_id: int | None, amount: int) -> dict:
        session = self._session_factory()
        try:
            row = CaptainEarning(captain_id=captain_id, match_id=match_id, amount=amount)
            session.add(row)
            session.commit()
            session.refresh(row)
            return row.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_captain(self, captain_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = (
                session.query(CaptainEarning)
                .filter(CaptainEarning.captain_id == captain_id)
                .order_by(CaptainEarning.created_at.desc())
                .all()
            )
            return [r.to_dict() for r in rows]
        finally:
            session.close()

    def sum_since(self, captain_id: int, since: datetime) -> int:
        """Total earned (any status) since a timestamp — an activity metric, not a balance."""
        session = self._session_factory()
        try:
            rows = (
                session.query(CaptainEarning)
                .filter(CaptainEarning.captain_id == captain_id, CaptainEarning.created_at >= since)
                .all()
            )
            return sum(r.amount for r in rows)
        finally:
            session.close()

    def sum_pending(self, captain_id: int) -> int:
        """Wallet balance — total unpaid earnings, all-time."""
        session = self._session_factory()
        try:
            rows = (
                session.query(CaptainEarning)
                .filter(CaptainEarning.captain_id == captain_id, CaptainEarning.status == EarningStatus.PENDING)
                .all()
            )
            return sum(r.amount for r in rows)
        finally:
            session.close()

    def mark_captain_paid(self, captain_id: int, reference: str | None) -> int:
        """Mark all PENDING earnings for a captain as PAID. Returns the count updated."""
        session = self._session_factory()
        try:
            rows = (
                session.query(CaptainEarning)
                .filter(CaptainEarning.captain_id == captain_id, CaptainEarning.status == EarningStatus.PENDING)
                .all()
            )
            now = datetime.utcnow()
            for row in rows:
                row.status = EarningStatus.PAID
                row.paid_at = now
                row.payout_reference = reference
            session.commit()
            return len(rows)
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


captain_earning_repository = CaptainEarningRepository()
