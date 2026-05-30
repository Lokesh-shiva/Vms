import json
import logging

from core.database.db_connection import SessionLocal
from modules.match.model.match_event_model import MatchEvent

logger = logging.getLogger(__name__)


class MatchEventRepository:
    """
    Append-only repository for match lifecycle events.

    log() is fire-and-forget — it never raises or propagates exceptions so that
    audit logging never disrupts the main business transaction.
    """

    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def log(
        self,
        match_id: int,
        event_type: str,
        user_id: int = None,
        meta: dict = None,
    ) -> None:
        """Write a single event row. Silently swallows all exceptions."""
        session = self._session_factory()
        try:
            event = MatchEvent(
                match_id=match_id,
                event_type=event_type,
                user_id=user_id,
                meta=json.dumps(meta) if meta else None,
            )
            session.add(event)
            session.commit()
        except Exception:
            session.rollback()
            logger.warning(
                "MatchEventRepository: failed to log %s for match %d",
                event_type,
                match_id,
            )
        finally:
            session.close()

    def find_by_match_id(self, match_id: int) -> list[dict]:
        """Return all events for a match, oldest first."""
        session = self._session_factory()
        try:
            events = (
                session.query(MatchEvent)
                .filter(MatchEvent.match_id == match_id)
                .order_by(MatchEvent.created_at.asc())
                .all()
            )
            return [e.to_dict() for e in events]
        finally:
            session.close()


match_event_repository = MatchEventRepository()
