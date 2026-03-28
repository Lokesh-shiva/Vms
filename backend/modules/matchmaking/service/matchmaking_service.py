from core.database.db_connection import SessionLocal
from modules.matchmaking.repository.queue_entry_repository import queue_entry_repository
from modules.pricing.service.pricing_service import PricingService

# Estimated wait time per player already in queue (seconds)
_WAIT_PER_PLAYER_SECONDS = 120  # 2 minutes per person ahead


class MatchmakingService:
    """
    Business logic for the matchmaking queue.

    Responsibilities:
    - Enforce one-active-queue-per-user rule.
    - Call PricingService for dynamic price on join.
    - Estimate wait time from queue depth.
    - Handle leave-queue status transition.
    - Return queue status on-demand.
    """

    def join_queue(self, user_id: int, region_id: int, sport_id: int, skill_level: str) -> dict:
        """
        Place user into the WAITING queue.

        Raises:
            ValueError: if user already has a WAITING entry, or skill_level invalid.

        Returns dict with:
            entry: the created QueueEntry dict
            pricing: {base_price, time_factor, demand_factor, queue_count, final_price}
            estimated_wait_seconds: int
            players_searching: int
        """
        from modules.matchmaking.model.queue_entry_model import QueueEntry

        if skill_level not in QueueEntry.VALID_SKILL_LEVELS:
            raise ValueError(
                f"Invalid skill_level '{skill_level}'. "
                f"Must be one of: {sorted(QueueEntry.VALID_SKILL_LEVELS)}"
            )

        # Duplicate guard: one active WAITING entry per user
        existing = queue_entry_repository.find_waiting_by_user(user_id)
        if existing:
            raise ValueError(
                f"User {user_id} is already in the queue (entry_id={existing['id']}). "
                "Leave the current queue before joining again."
            )

        db = SessionLocal()
        try:
            pricing = PricingService(db).calculate_price(region_id, sport_id)
        finally:
            db.close()

        players_searching = pricing["queue_count"]
        estimated_wait = players_searching * _WAIT_PER_PLAYER_SECONDS

        entry = queue_entry_repository.create({
            "user_id": user_id,
            "region_id": region_id,
            "sport_id": sport_id,
            "skill_level": skill_level,
            "status": "WAITING",
        })

        return {
            "entry": entry,
            "pricing": pricing,
            "estimated_wait_seconds": estimated_wait,
            "players_searching": players_searching,
        }

    def leave_queue(self, user_id: int) -> dict:
        """
        Cancel (WAITING → CANCELLED) the user's active queue entry.

        Raises:
            ValueError: if no WAITING entry found for user.

        Returns the updated QueueEntry dict.
        """
        existing = queue_entry_repository.find_waiting_by_user(user_id)
        if not existing:
            raise ValueError(f"User {user_id} has no active queue entry to leave.")

        updated = queue_entry_repository.update_status(existing["id"], "CANCELLED")
        return updated

    def get_queue_status(self, user_id: int) -> dict:
        """
        Return the user's current queue position and updated wait time.

        Raises:
            ValueError: if user has no active WAITING entry.

        Returns dict with:
            entry: QueueEntry dict
            players_searching: int
            estimated_wait_seconds: int
            pricing: pricing breakdown dict
        """
        entry = queue_entry_repository.find_waiting_by_user(user_id)
        if not entry:
            raise ValueError(f"User {user_id} has no active queue entry.")

        db = SessionLocal()
        try:
            pricing = PricingService(db).calculate_price(
                entry["region_id"], entry["sport_id"]
            )
        finally:
            db.close()

        players_searching = pricing["queue_count"]
        estimated_wait = players_searching * _WAIT_PER_PLAYER_SECONDS

        return {
            "entry": entry,
            "players_searching": players_searching,
            "estimated_wait_seconds": estimated_wait,
            "pricing": pricing,
        }


matchmaking_service = MatchmakingService()
