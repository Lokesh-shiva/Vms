from sqlalchemy import func

from core.database.db_connection import SessionLocal
from modules.tournament.model.sport_vote_model import SportVote


class SportVoteRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def upsert_vote(self, user_id: int, region_id: int, sport_name: str) -> dict:
        session = self._session_factory()
        try:
            existing = session.query(SportVote).filter(
                SportVote.user_id == user_id,
                SportVote.region_id == region_id,
            ).first()
            if existing:
                existing.sport_name = sport_name
                session.commit()
                session.refresh(existing)
                return existing.to_dict()

            vote = SportVote(user_id=user_id, region_id=region_id, sport_name=sport_name)
            session.add(vote)
            session.commit()
            session.refresh(vote)
            return vote.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def get_my_vote(self, user_id: int, region_id: int) -> str | None:
        session = self._session_factory()
        try:
            vote = session.query(SportVote).filter(
                SportVote.user_id == user_id,
                SportVote.region_id == region_id,
            ).first()
            return vote.sport_name if vote else None
        finally:
            session.close()

    def get_results(self, region_id: int) -> list[dict]:
        """Sport -> vote count for a region, ordered most-voted first."""
        session = self._session_factory()
        try:
            rows = (
                session.query(SportVote.sport_name, func.count(SportVote.id))
                .filter(SportVote.region_id == region_id)
                .group_by(SportVote.sport_name)
                .order_by(func.count(SportVote.id).desc())
                .all()
            )
            return [{"sport": name, "votes": count} for name, count in rows]
        finally:
            session.close()


sport_vote_repository = SportVoteRepository()
