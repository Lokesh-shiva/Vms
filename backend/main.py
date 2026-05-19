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


# ── Lifespan ──────────────────────────────────────────────────────────


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Create all ORM tables and run idempotent schema migrations on startup."""
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
        conn.commit()
    yield


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
app.include_router(booking_router)
app.include_router(payment_router)
app.include_router(auth_router)
app.include_router(fee_config_router)
app.include_router(admin_router)
app.include_router(match_router)
app.include_router(matchmaking_router)
app.include_router(pricing_router)
app.include_router(engine_router)


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
