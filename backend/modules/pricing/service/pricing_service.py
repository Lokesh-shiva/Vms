import os
from datetime import datetime

from sqlalchemy.orm import Session

from modules.matchmaking.model.queue_entry_model import QueueEntry


# Default base price if not configured in SystemConfig or env
_DEFAULT_BASE_PRICE = 200.0

# Peak-hour window (24h format)
_PEAK_START = 17  # 5 PM
_PEAK_END = 21    # 9 PM
_PEAK_MULTIPLIER = 1.5

# Demand thresholds → multipliers
_DEMAND_TIERS = [
    (10, 1.5),   # 10+ queued → 1.5x
    (5, 1.25),   # 5-9 queued → 1.25x
    (0, 1.0),    # 0-4 queued → 1.0x (no surge)
]


class PricingService:
    """
    Dynamic pricing engine.

    Formula:  price = base_price × time_factor × demand_factor

    - base_price:    from SystemConfig table key 'BASE_MATCH_PRICE', env var, or default
    - time_factor:   peak-hour multiplier (5 PM – 9 PM)
    - demand_factor: based on active queue count for the given region + sport
    """

    def __init__(self, db: Session):
        self.db = db

    def get_base_price(self) -> float:
        """Retrieve base price from SystemConfig, env, or fallback default."""
        from modules.payment.model.system_config_model import SystemConfig

        config = (
            self.db.query(SystemConfig)
            .filter(SystemConfig.key == "BASE_MATCH_PRICE")
            .first()
        )
        if config:
            return float(config.value)

        env_price = os.getenv("BASE_MATCH_PRICE")
        if env_price:
            return float(env_price)

        return _DEFAULT_BASE_PRICE

    def get_time_factor(self, now: datetime | None = None) -> float:
        """Return peak-hour multiplier based on current hour."""
        now = now or datetime.utcnow()
        if _PEAK_START <= now.hour < _PEAK_END:
            return _PEAK_MULTIPLIER
        return 1.0

    def get_active_queue_count(self, region_id: int, sport_id: int) -> int:
        """Count currently WAITING queue entries for a region + sport."""
        return (
            self.db.query(QueueEntry)
            .filter(
                QueueEntry.region_id == region_id,
                QueueEntry.sport_id == sport_id,
                QueueEntry.status == "WAITING",
            )
            .count()
        )

    def get_demand_factor(self, region_id: int, sport_id: int) -> float:
        """Return demand multiplier based on active queue count."""
        count = self.get_active_queue_count(region_id, sport_id)
        for threshold, multiplier in _DEMAND_TIERS:
            if count >= threshold:
                return multiplier
        return 1.0

    def calculate_price(
        self,
        region_id: int,
        sport_id: int,
        now: datetime | None = None,
    ) -> dict:
        """
        Compute dynamic price and return breakdown.

        Returns dict with: base_price, time_factor, demand_factor,
        queue_count, final_price
        """
        base = self.get_base_price()
        time_f = self.get_time_factor(now)
        demand_f = self.get_demand_factor(region_id, sport_id)
        queue_count = self.get_active_queue_count(region_id, sport_id)

        return {
            "base_price": base,
            "time_factor": time_f,
            "demand_factor": demand_f,
            "queue_count": queue_count,
            "final_price": round(base * time_f * demand_f, 2),
        }
