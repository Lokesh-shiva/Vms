"""
Admin Dashboard Routes

Provides a single aggregated endpoint for admin dashboard statistics,
replacing multiple client-side API calls with efficient server-side queries.
"""

import logging

from fastapi import APIRouter, Depends
from sqlalchemy import func, case, cast, Date, literal_column
from sqlalchemy.orm import Session

from pydantic import BaseModel

from core.database.db_connection import get_db
from modules.auth.dependencies.auth_dependencies import require_role
from modules.user.model.user_model import UserRole
from modules.booking.model.booking_model import Booking
from modules.match.model.match_model import Match
from modules.matchmaking.model.queue_entry_model import QueueEntry
from modules.payment.model.payment_model import Payment
from modules.payment.repository.system_config_repository import system_config_repository


logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/admin", tags=["Admin"])


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.get("/dashboard")
def get_dashboard_stats(
    current_user: dict = require_role(
        UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER, UserRole.FINANCE
    ),
    db: Session = Depends(get_db),
):
    """
    Return aggregated dashboard statistics in a single response.

    Uses 2 efficient COUNT queries instead of fetching full rows:
      1. Bookings aggregation (active_services, completed_today, total_bookings)
      2. Payments count (payments_under_review)
    """
    logger.info("Dashboard stats requested by admin %s", current_user["id"])

    # ── Query 1: Bookings aggregation ─────────────────────────────────

    booking_stats = db.query(
        func.count().label("total_bookings"),
        func.count(
            case(
                (Booking.status == "IN_PROGRESS", 1),
            )
        ).label("active_services"),
        func.count(
            case(
                (
                    (Booking.status == "COMPLETED")
                    & (
                        cast(
                            literal_column("updated_at AT TIME ZONE 'Asia/Kolkata'"),
                            Date,
                        )
                        == func.current_date()
                    ),
                    1,
                ),
            )
        ).label("completed_today"),
    ).one()

    # ── Query 2: Payments under review ────────────────────────────────
    payments_under_review = (
        db.query(func.count()).filter(Payment.status == "UNDER_REVIEW").scalar()
    )

    return _success(
        {
            "active_services": booking_stats.active_services,
            "payments_under_review": payments_under_review,
            "completed_today": booking_stats.completed_today,
            "total_bookings": booking_stats.total_bookings,
        }
    )


@router.get("/metrics")
def get_match_metrics(
    current_user: dict = require_role(UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER),
    db: Session = Depends(get_db),
):
    """
    Match and queue operational metrics. Admin only.

    Returns:
      - match_counts: breakdown of matches by status
      - avg_match_duration_minutes: average time from started_at → completed_at
      - avg_queue_wait_minutes: average time a user spends in WAITING status
      - no_show_rate_pct: CANCELLED_NO_SHOW matches as % of all terminal matches
    """
    # ── Match counts by status ────────────────────────────────────────
    status_rows = (
        db.query(Match.status, func.count(Match.id).label("count"))
        .group_by(Match.status)
        .all()
    )
    match_counts = {row.status: row.count for row in status_rows}

    # ── Avg match duration (only matches with both timestamps) ─────────
    duration_row = (
        db.query(
            func.avg(
                func.extract("epoch", Match.completed_at - Match.started_at) / 60
            ).label("avg_minutes")
        )
        .filter(
            Match.started_at.isnot(None),
            Match.completed_at.isnot(None),
        )
        .one()
    )
    avg_match_duration = round(float(duration_row.avg_minutes or 0), 1)

    # ── Avg queue wait time (MATCHED entries: created_at → matched moment) ──
    # Proxy: time between QueueEntry.created_at and Match.created_at for the
    # same region/sport group.  Use QueueEntry rows that are in MATCHED status.
    wait_row = (
        db.query(
            func.avg(
                func.extract("epoch", func.now() - QueueEntry.created_at) / 60
            ).label("avg_minutes")
        )
        .filter(
            QueueEntry.status == "MATCHED",
        )
        .one()
    )
    avg_queue_wait = round(float(wait_row.avg_minutes or 0), 1)

    # ── No-show rate ──────────────────────────────────────────────────
    terminal_statuses = ("COMPLETED", "CANCELLED", "CANCELLED_NO_SHOW")
    total_terminal = (
        db.query(func.count(Match.id))
        .filter(Match.status.in_(terminal_statuses))
        .scalar()
        or 0
    )
    no_show_count = (
        db.query(func.count(Match.id))
        .filter(Match.status == "CANCELLED_NO_SHOW")
        .scalar()
        or 0
    )
    no_show_rate = (
        round(no_show_count / total_terminal * 100, 1) if total_terminal else 0.0
    )

    return _success(
        {
            "match_counts": match_counts,
            "avg_match_duration_minutes": avg_match_duration,
            "avg_queue_wait_minutes": avg_queue_wait,
            "no_show_rate_pct": no_show_rate,
        }
    )


@router.get("/queue-stats")
def get_queue_stats(
    current_user: dict = require_role(UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER),
    db: Session = Depends(get_db),
):
    """Return active queue count grouped by sport and region."""
    stats = (
        db.query(
            QueueEntry.sport_id,
            QueueEntry.region_id,
            func.count(QueueEntry.id).label("count"),
        )
        .filter(QueueEntry.status == "WAITING")
        .group_by(QueueEntry.sport_id, QueueEntry.region_id)
        .all()
    )
    return _success(
        [
            {
                "sport_id": row.sport_id,
                "region_id": row.region_id,
                "count": row.count,
            }
            for row in stats
        ]
    )


@router.get("/matches")
def list_all_matches(
    current_user: dict = require_role(
        UserRole.SUPER_ADMIN,
        UserRole.OPS_MANAGER,
        UserRole.SUPPORT,
        UserRole.TOURNAMENT_MANAGER,
    ),
):
    """
    Return all matches across all statuses, newest first. Admin only.

    This differs from GET /api/v1/matches which only returns OPEN future matches
    for end-users. Results are enriched with sport_name, region_name, etc.
    """
    from modules.match.service.match_service import match_service

    return _success(match_service.list_all_matches())


@router.post("/matches/reap-abandoned")
def reap_abandoned_sessions_now(
    current_user: dict = require_role(UserRole.SUPER_ADMIN, UserRole.OPS_MANAGER),
):
    """
    Manually trigger the abandoned-session cleanup (normally runs automatically
    every 5 minutes via APScheduler). Exists as a fallback for hosting tiers
    that suspend background jobs when the service goes idle — the scheduled
    job simply doesn't fire while the process is asleep, so a stuck WAITING
    session can otherwise sit forever until an admin notices and cancels it
    by hand one at a time.
    """
    from modules.match.repository.match_repository import match_repository
    from modules.match.service.session_reaper_service import reap_abandoned_sessions, ABANDONED_SESSION_TIMEOUT_MINUTES
    from datetime import datetime, timedelta

    cutoff = datetime.utcnow() - timedelta(minutes=ABANDONED_SESSION_TIMEOUT_MINUTES)
    pending = match_repository.find_abandoned_waiting(cutoff)
    reap_abandoned_sessions()
    return _success({"reaped_count": len(pending)}, f"Reaped {len(pending)} abandoned session(s).")


class UpdateConfigRequest(BaseModel):
    value: str


@router.get("/config")
def list_system_configs(current_user: dict = require_role(UserRole.SUPER_ADMIN)):
    """List all system configurations."""
    configs = system_config_repository.get_all()
    return _success(configs)


@router.put("/config/{key}")
def update_system_config(
    key: str,
    request: UpdateConfigRequest,
    current_user: dict = require_role(UserRole.SUPER_ADMIN),
):
    """Update a specific configuration value."""
    config = system_config_repository.set(key, request.value)
    return _success(config, "Configuration updated successfully.")
