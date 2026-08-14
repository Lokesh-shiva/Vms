from core.database.db_connection import SessionLocal
from modules.tournament.model.sport_vote_round_model import SportVoteRound, VoteRoundStatus


class SportVoteRoundRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def get_current(self) -> dict | None:
        session = self._session_factory()
        try:
            round_ = session.query(SportVoteRound).filter(
                SportVoteRound.is_current.is_(True)
            ).first()
            return round_.to_dict() if round_ else None
        finally:
            session.close()

    def find_by_id(self, round_id: int) -> dict | None:
        session = self._session_factory()
        try:
            round_ = session.query(SportVoteRound).filter(SportVoteRound.id == round_id).first()
            return round_.to_dict() if round_ else None
        finally:
            session.close()

    def create(self, options: list[str], closes_at) -> dict:
        session = self._session_factory()
        try:
            session.query(SportVoteRound).filter(
                SportVoteRound.is_current.is_(True)
            ).update({"is_current": False})

            round_ = SportVoteRound(options=options, closes_at=closes_at, status=VoteRoundStatus.OPEN, is_current=True)
            session.add(round_)
            session.commit()
            session.refresh(round_)
            return round_.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def close(self, round_id: int) -> dict | None:
        session = self._session_factory()
        try:
            round_ = session.query(SportVoteRound).filter(SportVoteRound.id == round_id).first()
            if not round_:
                return None
            round_.status = VoteRoundStatus.CLOSED
            session.commit()
            session.refresh(round_)
            return round_.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


sport_vote_round_repository = SportVoteRoundRepository()
