from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.matchmaking.repository.queue_entry_repository import queue_entry_repository
from modules.match.model.match_model import MatchPenalty
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
                "Invalid skill_level. Must be one of BEGINNER|INTERMEDIATE|ADVANCED."
            )

        # Penalty guard: block users with an active no-show penalty
        db = SessionLocal()
        try:
            active_penalty = (
                db.query(MatchPenalty)
                .filter(
                    MatchPenalty.user_id == user_id,
                    MatchPenalty.expires_at > datetime.utcnow(),
                )
                .first()
            )
            if active_penalty:
                raise ValueError(
                    f"You are temporarily restricted from matchmaking until "
                    f"{active_penalty.expires_at} due to a previous no-show."
                )
        finally:
            db.close()

        # Duplicate guard: one active WAITING entry per user
        existing = queue_entry_repository.find_waiting_by_user(user_id)
        if existing:
            raise ValueError(
                f"User {user_id} is already in the queue."
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

        If status is MATCHED, we find the corresponding match_id from MatchPlayer.

        Raises:
            ValueError: if user has no active WAITING or MATCHED entry.
        """
        entry = queue_entry_repository.find_waiting_by_user(user_id)
        if not entry:
            raise ValueError("User has no active queue entry.")

        db = SessionLocal()
        try:
            pricing = PricingService(db).calculate_price(
                entry["region_id"], entry["sport_id"]
            )
            
            players_searching = pricing["queue_count"]
            match_id = None
            if entry["status"] == "MATCHED":
                # Find the match_id where this user is a player
                from modules.match.model.match_model import Match, MatchPlayer
                match_p = (
                    db.query(MatchPlayer)
                    .join(Match, MatchPlayer.match_id == Match.id)
                    .filter(
                        MatchPlayer.user_id == user_id,
                        Match.status.in_(["MATCHED", "ARRIVED", "IN_PROGRESS"])
                    )
                    .order_by(Match.created_at.desc())
                    .first()
                )
                if match_p:
                    match_id = match_p.match_id

            if entry["status"] == "MATCHED":
                wait_estimation_msg = "You're matched — arrive in 20 mins or lose your spot"
            elif players_searching == 0:
                wait_estimation_msg = "No players nearby yet — hang tight"
            else:
                wait_mins = max(1, round((players_searching * _WAIT_PER_PLAYER_SECONDS) / 60))
                wait_estimation_msg = f"{players_searching} players nearby — match likely in {wait_mins} mins"

            return {
                "entry": entry,
                "players_searching": players_searching,
                "estimated_wait_seconds": 0 if match_id else (pricing["queue_count"] * _WAIT_PER_PLAYER_SECONDS),
                "pricing": pricing,
                "match_id": match_id,
                "wait_estimation_msg": wait_estimation_msg,
            }
        finally:
            db.close()


matchmaking_service = MatchmakingService()
