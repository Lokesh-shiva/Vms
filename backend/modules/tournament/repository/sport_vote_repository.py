from sqlalchemy import func

from core.database.db_connection import SessionLocal
from modules.tournament.model.sport_vote_model import SportVote


class SportVoteRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def upsert_vote(self, round_id: int, user_id: int, sport_name: str) -> dict:
        session = self._session_factory()
        try:
            existing = session.query(SportVote).filter(
                SportVote.round_id == round_id,
                SportVote.user_id == user_id,
            ).first()
            if existing:
                existing.sport_name = sport_name
                session.commit()
                session.refresh(existing)
                return existing.to_dict()

            vote = SportVote(round_id=round_id, user_id=user_id, sport_name=sport_name)
            session.add(vote)
            session.commit()
            session.refresh(vote)
            return vote.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def get_my_vote(self, round_id: int, user_id: int) -> str | None:
        session = self._session_factory()
        try:
            vote = session.query(SportVote).filter(
                SportVote.round_id == round_id,
                SportVote.user_id == user_id,
            ).first()
            return vote.sport_name if vote else None
        finally:
            session.close()

    def get_results(self, round_id: int) -> dict[str, int]:
        """sport_name -> vote count for a round."""
        session = self._session_factory()
        try:
            rows = (
                session.query(SportVote.sport_name, func.count(SportVote.id))
                .filter(SportVote.round_id == round_id)
                .group_by(SportVote.sport_name)
                .all()
            )
            return {name: count for name, count in rows}
        finally:
            session.close()


sport_vote_repository = SportVoteRepository()
