"""
Background job — auto-cancels WAITING sessions that never found a second
player. Runs on an interval via APScheduler (wired in main.py's lifespan).
"""
import logging
from datetime import datetime, timedelta

from modules.match.repository.match_repository import match_repository
from modules.match.service.match_service import MatchService

logger = logging.getLogger(__name__)

ABANDONED_SESSION_TIMEOUT_MINUTES = 15


def reap_abandoned_sessions() -> None:
    cutoff = datetime.utcnow() - timedelta(minutes=ABANDONED_SESSION_TIMEOUT_MINUTES)
    abandoned = match_repository.find_abandoned_waiting(cutoff)
    if not abandoned:
        return

    service = MatchService()
    for match in abandoned:
        try:
            service.system_cancel_abandoned(match["id"])
            logger.info("session_reaper: auto-cancelled abandoned match %d", match["id"])
        except ValueError:
            # Already left WAITING (raced with a player joining) — skip.
            continue
        except Exception:
            logger.exception("session_reaper: failed to cancel match %d", match["id"])
