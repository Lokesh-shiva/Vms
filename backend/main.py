"""
VMS Backend — Application Entry Point

Starts the FastAPI application with:
- Centralized error handling middleware
- All module routers registered
- Interactive API documentation at /docs (Swagger UI)

Run with:
    uvicorn backend.main:app --reload --port 8000
"""

import os
import sys
from contextlib import asynccontextmanager

from apscheduler.schedulers.background import BackgroundScheduler

# Ensure the backend package is importable when running from project root.
sys.path.insert(0, os.path.dirname(__file__))

from fastapi import FastAPI, Request, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy import text

from core.middleware.error_handler import ErrorHandlerMiddleware
from core.database.db_connection import engine, Base
from modules.user.controller.user_routes import router as user_router
from modules.location.controller.location_routes import router as location_router
from modules.timeslot.controller.timeslot_routes import router as timeslot_router
from modules.cart_type.controller.cart_type_routes import router as cart_type_router
from modules.cart_type.controller.sport_routes import router as sport_router
from modules.cart.controller.cart_routes import router as cart_router
from modules.cart.controller.ground_routes import router as ground_router
from modules.item.controller.item_routes import router as item_router
from modules.order.controller.order_routes import router as order_router
from modules.order.controller.admin_order_routes import router as admin_order_router
from modules.booking.controller.booking_routes import router as booking_router
from modules.payment.controller.payment_routes import router as payment_router
from modules.auth.controller.auth_routes import router as auth_router
from modules.fee_config.controller.fee_config_routes import router as fee_config_router
from modules.admin.controller.admin_routes import router as admin_router
from modules.match.controller.match_routes import router as match_router
from modules.matchmaking.controller.matchmaking_routes import (
    router as matchmaking_router,
)
from modules.pricing.controller.pricing_routes import router as pricing_router
from modules.match.controller.match_engine_routes import router as engine_router
from modules.match.model.match_model import Match, MatchPlayer  # noqa: F401 — registers models
from modules.match.model.match_event_model import MatchEvent  # noqa: F401 — registers model
from modules.payment.model.payment_model import Payment  # noqa: F401 — registers model
from modules.payment.model.system_config_model import SystemConfig  # noqa: F401 — registers model
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig  # noqa: F401 — registers model
from modules.sport.model.sport_model import Sport  # noqa: F401 — registers model
from modules.matchmaking.model.queue_entry_model import QueueEntry  # noqa: F401 — registers model
from modules.captain.controller.captain_routes import router as captain_router
from modules.captain.model.captain_model import Captain  # noqa: F401 — registers model
from modules.captain.model.captain_earning_model import CaptainEarning  # noqa: F401 — registers model
from modules.notification.controller.notification_routes import router as notification_router
from modules.notification.model.notification_model import Notification  # noqa: F401 — registers model
from modules.notification.model.fcm_token_model import FcmToken  # noqa: F401 — registers model
from modules.chat.controller.chat_routes import router as chat_router
from modules.chat.model.message_model import Message  # noqa: F401 — registers model
from modules.tournament.controller.tournament_routes import router as tournament_router
from modules.tournament.controller.tournament_registration_routes import router as tournament_registration_router
from modules.tournament.controller.tournament_vote_routes import router as tournament_vote_router
from modules.tournament.controller.admin_vote_round_routes import router as admin_vote_round_router
from modules.tournament.controller.tournament_match_routes import router as tournament_match_router
from modules.tournament.controller.leaderboard_routes import router as leaderboard_router
from modules.tournament.model.tournament_model import Tournament  # noqa: F401 — registers model
from modules.tournament.model.tournament_team_model import TournamentTeam  # noqa: F401 — registers model
from modules.tournament.model.tournament_participant_model import TournamentParticipant  # noqa: F401 — registers model
from modules.tournament.model.tournament_match_model import TournamentMatch  # noqa: F401 — registers model
from modules.tournament.model.tournament_standing_model import TournamentStanding  # noqa: F401 — registers model
from modules.tournament.model.player_score_model import PlayerScore  # noqa: F401 — registers model
from modules.dispute.controller.dispute_routes import router as dispute_router
from modules.dispute.model.dispute_model import Dispute  # noqa: F401 — registers model
from modules.dispute.model.dispute_message_model import DisputeMessage  # noqa: F401 — registers model
from modules.audit.controller.audit_routes import router as audit_router
from modules.audit.model.audit_model import AuditLog  # noqa: F401 — registers model
from modules.society.controller.society_routes import router as society_router
from modules.wallet.controller.wallet_routes import router as wallet_router
from modules.wallet.model.wallet_transaction_model import WalletTransaction  # noqa: F401 — registers model
from modules.society.model.society_model import Society  # noqa: F401 — registers model
from modules.society.model.society_member_model import SocietyMember  # noqa: F401 — registers model
from modules.otp.model.otp_model import OtpCode  # noqa: F401 — registers model


# ── Lifespan ──────────────────────────────────────────────────────────

_scheduler = BackgroundScheduler(daemon=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Create all ORM tables, run idempotent schema migrations, start background jobs."""
    Base.metadata.create_all(bind=engine)
    with engine.connect() as conn:
        # Add columns that may be missing from pre-existing tables (no Alembic)
        conn.execute(
            text("ALTER TABLE matches ADD COLUMN IF NOT EXISTS started_at TIMESTAMP")
        )
        conn.execute(
            text("ALTER TABLE matches ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP")
        )
        # Idempotent migration: rename legacy 'admin' role to 'super_admin'
        conn.execute(text("UPDATE users SET role = 'super_admin' WHERE role = 'admin'"))
        conn.execute(
            text("ALTER TABLE users ADD COLUMN IF NOT EXISTS can_create_society BOOLEAN NOT NULL DEFAULT FALSE")
        )
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth VARCHAR"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS sport_preferences JSON"))
        conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR"))
        conn.execute(
            text("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_profile_complete BOOLEAN NOT NULL DEFAULT FALSE")
        )
        conn.commit()

    from core.push.firebase_client import is_available as firebase_is_available
    firebase_is_available()  # trigger lazy init now so failures surface at boot, not on first push

    from modules.match.service.session_reaper_service import reap_abandoned_sessions
    _scheduler.add_job(reap_abandoned_sessions, "interval", minutes=5, id="session_reaper", replace_existing=True)
    _scheduler.start()
    yield
    _scheduler.shutdown(wait=False)


# ── Application ───────────────────────────────────────────────────────

app = FastAPI(
    title="VMS Backend API",
    description="Modular V1 backend for the VMS project.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)


# ── Middleware ────────────────────────────────────────────────────────

app.add_middleware(ErrorHandlerMiddleware)


# ── Custom HTTPException handler (standardized format) ────────────────


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """Return HTTP errors with 'message' key for TestSprite test compatibility."""
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "data": None,
            "message": exc.detail if isinstance(exc.detail, str) else str(exc.detail),
            "detail": exc.detail,
        },
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Return FastAPI 422 validation errors in the standard {success, data, message} envelope."""
    return JSONResponse(
        status_code=422,
        content={
            "success": False,
            "data": None,
            "message": "Validation error.",
            "detail": exc.errors(),
        },
    )


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Fallback handler for any unhandled exception."""
    return JSONResponse(
        status_code=500,
        content={"success": False, "data": None, "message": "Internal server error."},
    )


# ── Route Registration ───────────────────────────────────────────────

app.include_router(user_router)
app.include_router(location_router)
app.include_router(timeslot_router)
app.include_router(sport_router)  # /api/v1/sports  (domain name)
app.include_router(ground_router)  # /api/v1/grounds (domain name)
app.include_router(cart_type_router)  # /api/v1/cart-types (deprecated)
app.include_router(cart_router)  # /api/v1/carts      (deprecated)
app.include_router(item_router)
app.include_router(order_router)
app.include_router(admin_order_router)
app.include_router(booking_router)
app.include_router(payment_router)
app.include_router(auth_router)
app.include_router(fee_config_router)
app.include_router(admin_router)
app.include_router(match_router)
app.include_router(matchmaking_router)
app.include_router(pricing_router)
app.include_router(engine_router)
app.include_router(captain_router)
app.include_router(tournament_vote_router)  # static /votes route must precede tournament_router's /{tournament_id}
app.include_router(admin_vote_round_router)
app.include_router(tournament_router)
app.include_router(tournament_registration_router)
app.include_router(tournament_match_router)
app.include_router(leaderboard_router)
app.include_router(dispute_router)
app.include_router(audit_router)
app.include_router(society_router)
app.include_router(wallet_router)
app.include_router(notification_router)
app.include_router(chat_router)


# ── Health Check ──────────────────────────────────────────────────────


@app.get("/", tags=["System"])
@app.head("/", tags=["System"])
def root():
    """Root health check — required by Render's deploy probe."""
    return {"success": True, "data": None, "message": "VMS API is running."}


@app.get("/health", tags=["System"])
def health_check():
    return {"success": True, "data": None, "message": "Server is running."}


# ── Run Server ────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn

    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
