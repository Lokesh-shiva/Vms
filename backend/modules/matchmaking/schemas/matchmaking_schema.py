from pydantic import BaseModel, Field
from typing import Optional


class JoinQueueRequest(BaseModel):
    """Request body for POST /matchmaking/play-now"""

    sport_id: int = Field(..., gt=0, description="ID of the sport to match for")
    skill_level: str = Field(..., description="BEGINNER | INTERMEDIATE | ADVANCED")
    # Set to True to skip the stranger-wait and get a captain assigned immediately.
    # If no captain is available the request returns 503 so the client can retry
    # or fall back to the normal queue.
    instant_captain: bool = Field(
        default=False,
        description="Skip queue, assign an available captain right now.",
    )
    # region_id is injected server-side from user profile; not client-supplied


class QueueStatusResponse(BaseModel):
    """Unified shape returned by join, status, and leave endpoints."""

    entry_id: int
    user_id: int
    region_id: int
    sport_id: int
    skill_level: str
    status: str
    players_searching: int
    estimated_wait_seconds: int
    pricing: Optional[dict] = None
    created_at: Optional[str] = None
