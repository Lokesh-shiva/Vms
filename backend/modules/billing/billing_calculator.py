"""Pure billing math for time-based sessions. No DB, no side effects."""

import math
from datetime import datetime


class BillingError(ValueError):
    """Raised on invalid billing inputs."""


def calculate_blocks(session_minutes: int, block_duration_minutes: int) -> int:
    """Return the number of billable blocks for a session.

    A started session always bills at least one block (minimum charge).
    Blocks round UP: 46 minutes over a 45-minute block = 2 blocks.
    """
    if block_duration_minutes <= 0:
        raise BillingError("block_duration_minutes must be positive.")
    if session_minutes < 0:
        raise BillingError("session_minutes cannot be negative.")
    if session_minutes == 0:
        return 1
    return math.ceil(session_minutes / block_duration_minutes)


def calculate_time_bill(
    session_minutes: int,
    rate_per_block: float,
    block_duration_minutes: int,
    surge_multiplier: float = 1.0,
) -> float:
    """Compute the time-portion of the bill (excludes matching fee).

    time_bill = blocks * rate_per_block * surge_multiplier, rounded to 2dp.
    """
    if rate_per_block < 0:
        raise BillingError("rate_per_block cannot be negative.")
    if surge_multiplier < 1.0:
        raise BillingError("surge_multiplier cannot be below 1.0.")
    blocks = calculate_blocks(session_minutes, block_duration_minutes)
    return round(blocks * rate_per_block * surge_multiplier, 2)


def compute_session_bill(
    started_at: datetime,
    ended_at: datetime,
    rate_per_block: float,
    block_duration_minutes: int,
    max_duration_minutes: int,
    surge_multiplier: float = 1.0,
) -> dict:
    """Compute full session billing breakdown from start/end timestamps.

    Caps session_minutes at max_duration_minutes. Returns a dict ready to
    merge into a booking update.
    """
    if ended_at < started_at:
        raise BillingError("ended_at cannot be before started_at.")

    raw_minutes = int((ended_at - started_at).total_seconds() // 60)
    session_minutes = min(raw_minutes, max_duration_minutes)
    blocks = calculate_blocks(session_minutes, block_duration_minutes)
    time_bill = calculate_time_bill(
        session_minutes, rate_per_block, block_duration_minutes, surge_multiplier
    )
    return {
        "session_minutes": session_minutes,
        "session_blocks": blocks,
        "time_bill_amount": time_bill,
        "surge_multiplier_snapshot": surge_multiplier,
    }
