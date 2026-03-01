"""
VMS Backend — Application Entry Point

Starts the FastAPI application with:
- Centralized error handling middleware
- User module routes registered
- Interactive API documentation at /docs (Swagger UI)

Run with:
    uvicorn backend.main:app --reload --port 8000
"""

import sys
import os

# Ensure the backend package is importable when running from project root.
sys.path.insert(0, os.path.dirname(__file__))

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from core.middleware.error_handler import ErrorHandlerMiddleware
from core.database.db_connection import engine, Base
from modules.user.controller.user_routes import router as user_router
from modules.location.controller.location_routes import router as location_router
from modules.timeslot.controller.timeslot_routes import router as timeslot_router
from modules.cart_type.controller.cart_type_routes import router as cart_type_router
from modules.cart.controller.cart_routes import router as cart_router
from modules.item.controller.item_routes import router as item_router
from modules.booking.controller.booking_routes import router as booking_router
from modules.payment.controller.payment_routes import router as payment_router
from modules.auth.controller.auth_routes import router as auth_router
from modules.payment.model.payment_model import Payment  # noqa: F401 — registers model


# ── Application ───────────────────────────────────────────────────────

app = FastAPI(
    title="VMS Backend API",
    description="Modular V1 backend for the VMS project.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)


# ── Startup Event ────────────────────────────────────────────────────

@app.on_event("startup")
def startup():
    """Create all ORM tables on startup (no-op if they already exist)."""
    Base.metadata.create_all(bind=engine)


# ── Middleware ────────────────────────────────────────────────────────

app.add_middleware(ErrorHandlerMiddleware)


# ── Custom HTTPException handler (standardized format) ────────────────

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
app.include_router(cart_type_router)
app.include_router(cart_router)
app.include_router(item_router)
app.include_router(booking_router)
app.include_router(payment_router)
app.include_router(auth_router)


# ── Health Check ──────────────────────────────────────────────────────

@app.get("/health", tags=["System"])
def health_check():
    return {"success": True, "data": None, "message": "Server is running."}


# ── Run Server ────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
