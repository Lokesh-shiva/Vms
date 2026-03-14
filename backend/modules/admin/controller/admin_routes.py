"""
Admin Dashboard Routes

Provides a single aggregated endpoint for admin dashboard statistics,
replacing multiple client-side API calls with efficient server-side queries.
"""

import logging

from fastapi import APIRouter, Depends
from sqlalchemy import func, case, cast, Date
from sqlalchemy.orm import Session

from core.database.db_connection import get_db
from modules.auth.dependencies.auth_dependencies import require_admin
from modules.booking.model.booking_model import Booking
from modules.payment.model.payment_model import Payment


logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/admin", tags=["Admin"])


# ── Response helper ───────────────────────────────────────────────────

def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────

@router.get("/dashboard")
def get_dashboard_stats(
    current_user: dict = Depends(require_admin),
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
        func.count(case(
            (Booking.status == "IN_PROGRESS", 1),
        )).label("active_services"),
        func.count(case(
            (
                (Booking.status == "COMPLETED") & 
                (cast(func.timezone("Asia/Kolkata", Booking.updated_at), Date) == func.current_date()),
                1,
            ),
        )).label("completed_today"),
    ).one()

    # ── Query 2: Payments under review ────────────────────────────────
    payments_under_review = (
        db.query(func.count())
        .filter(Payment.status == "UNDER_REVIEW")
        .scalar()
    )

    return _success({
        "active_services": booking_stats.active_services,
        "payments_under_review": payments_under_review,
        "completed_today": booking_stats.completed_today,
        "total_bookings": booking_stats.total_bookings,
    })
