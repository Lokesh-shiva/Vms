from core.database.db_connection import SessionLocal
from modules.tournament.model.tournament_participant_model import TournamentParticipant, ParticipantStatus


class TournamentParticipantRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict, session=None) -> dict:
        own = session is None
        session = session or self._session_factory()
        try:
            p = TournamentParticipant(
                tournament_id=data["tournament_id"],
                user_id=data["user_id"],
                team_id=data.get("team_id"),
                status=data.get("status", ParticipantStatus.REGISTERED),
            )
            session.add(p)
            if own:
                session.commit()
                session.refresh(p)
            else:
                session.flush()
                session.refresh(p)
            return p.to_dict()
        except Exception:
            if own:
                session.rollback()
            raise
        finally:
            if own:
                session.close()

    def find_by_tournament_and_user(self, tournament_id: int, user_id: int) -> dict | None:
        session = self._session_factory()
        try:
            p = session.query(TournamentParticipant).filter(
                TournamentParticipant.tournament_id == tournament_id,
                TournamentParticipant.user_id == user_id,
            ).first()
            return p.to_dict() if p else None
        finally:
            session.close()

    def find_by_tournament(self, tournament_id: int, status: str | None = ParticipantStatus.REGISTERED) -> list[dict]:
        session = self._session_factory()
        try:
            query = session.query(TournamentParticipant).filter(
                TournamentParticipant.tournament_id == tournament_id,
            )
            if status is not None:
                query = query.filter(TournamentParticipant.status == status)
            return [p.to_dict() for p in query.all()]
        finally:
            session.close()

    def find_by_team(self, team_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            members = session.query(TournamentParticipant).filter(
                TournamentParticipant.team_id == team_id,
                TournamentParticipant.status == ParticipantStatus.REGISTERED,
            ).all()
            return [m.to_dict() for m in members]
        finally:
            session.close()

    def update_status(self, tournament_id: int, user_id: int, status: str) -> dict | None:
        session = self._session_factory()
        try:
            p = session.query(TournamentParticipant).filter(
                TournamentParticipant.tournament_id == tournament_id,
                TournamentParticipant.user_id == user_id,
            ).first()
            if not p:
                return None
            p.status = status
            session.commit()
            session.refresh(p)
            return p.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def count_registered(self, tournament_id: int) -> int:
        """Count registered individual participants (no team_id) for capacity check."""
        session = self._session_factory()
        try:
            return session.query(TournamentParticipant).filter(
                TournamentParticipant.tournament_id == tournament_id,
                TournamentParticipant.status == ParticipantStatus.REGISTERED,
                TournamentParticipant.team_id.is_(None),
            ).count()
        finally:
            session.close()


tournament_participant_repository = TournamentParticipantRepository()
