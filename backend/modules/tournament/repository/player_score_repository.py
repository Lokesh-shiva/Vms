from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.tournament.model.player_score_model import PlayerScore


class PlayerScoreRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def add_points(self, user_id: int, region_id: int, sport_id: int, points: int) -> None:
        """Add points to a user's global score, creating the row if needed. Always increments matches_played."""
        session = self._session_factory()
        try:
            row = session.query(PlayerScore).filter(
                PlayerScore.user_id == user_id,
                PlayerScore.region_id == region_id,
                PlayerScore.sport_id == sport_id,
            ).first()
            if not row:
                row = PlayerScore(user_id=user_id, region_id=region_id, sport_id=sport_id)
                session.add(row)
                session.flush()
            row.total_points += points
            row.matches_played += 1
            row.updated_at = datetime.utcnow()
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def get_leaderboard(self, region_id: int, sport_id: int, limit: int = 50) -> list[dict]:
        session = self._session_factory()
        try:
            rows = session.query(PlayerScore).filter(
                PlayerScore.region_id == region_id,
                PlayerScore.sport_id == sport_id,
            ).order_by(PlayerScore.total_points.desc()).limit(limit).all()
            return [r.to_dict() for r in rows]
        finally:
            session.close()


player_score_repository = PlayerScoreRepository()
