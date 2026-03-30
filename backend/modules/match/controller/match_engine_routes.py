"""
match_engine_routes.py — Stateless engine webhook for the MatchEngineService.

Exposes POST /engine/trigger which is called by an external cron scheduler
(e.g. AWS EventBridge, Vercel Cron, systemd timer) to run one matching sweep.

Security: protected by a pre-shared secret header (X-Cron-Secret).
"""

import os

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import JSONResponse

from modules.match.service.match_engine_service import match_engine_service


router = APIRouter(prefix="/engine", tags=["Match Engine"])


# ── Endpoint ──────────────────────────────────────────────────────────

@router.post("/trigger")
async def trigger_matching_cycle(
    x_cron_secret: str = Header(default=None, alias="X-Cron-Secret"),
):
    """
    Run one full matching sweep.

    Protected by the X-Cron-Secret header.  Compare against the
    CRON_SECRET environment variable (falls back to "dev-secret" for
    local development).

    Returns:
        {"status": "ok", "matches_created": N}

    Raises:
        403 Forbidden — if the X-Cron-Secret header is missing or wrong.
    """
    expected_secret = os.getenv("CRON_SECRET", "dev-secret")
    if x_cron_secret != expected_secret:
        return JSONResponse(
            status_code=403,
            content={
                "success": False,
                "data": None,
                "message": "Forbidden: invalid or missing X-Cron-Secret header.",
            },
        )

    outcomes = match_engine_service.process_matching_cycle()
    matches_created = sum(1 for o in outcomes if o.get("result") == "matched")

    return {
        "status": "ok",
        "matches_created": matches_created,
    }
