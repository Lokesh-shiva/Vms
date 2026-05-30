"""
Centralised runtime configuration reader.

Priority order (highest wins):
  1. system_configs table (admin-editable at runtime, no restart needed)
  2. Environment variables
  3. Hardcoded defaults below

Add new keys to DEFAULTS to document them and provide a safe fallback.
"""

import os

# Key name → string default
DEFAULTS: dict[str, str] = {
    # Match engine
    "MATCH_ARRIVAL_TIMEOUT_MINUTES": "20",  # cancel MATCHED match if no arrival within N min
    "MATCH_IN_PROGRESS_TIMEOUT_HOURS": "3",  # cancel IN_PROGRESS match after N hours (safety net)
    "MATCH_MAX_PLAYERS": "2",
    # Ghost / no-show penalty
    "GHOST_STRIKES_BEFORE_PENALTY": "2",  # excuses before issuing a MatchPenalty
    "GHOST_PENALTY_HOURS": "4",  # duration of a matchmaking ban
    # Arrival GPS proximity
    "MATCH_PROXIMITY_DEGREES": "0.005",  # ~500 m latitude/longitude delta
}


def _raw(key: str) -> str:
    """Return raw string value, checking DB → env → default."""
    # Late import to avoid circular dependency at module load time
    try:
        from modules.payment.repository.system_config_repository import (
            system_config_repository,
        )

        db_val = system_config_repository.get(key)
        if db_val is not None:
            return db_val
    except Exception:
        pass

    env_val = os.getenv(key)
    if env_val is not None:
        return env_val

    return DEFAULTS.get(key, "")


def get_int(key: str) -> int:
    return int(_raw(key) or "0")


def get_float(key: str) -> float:
    return float(_raw(key) or "0.0")


def get_str(key: str) -> str:
    return _raw(key)
