from core.database.db_connection import SessionLocal
from modules.tournament.model.tournament_team_model import TournamentTeam


class TournamentTeamRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict, session=None) -> dict:
        own = session is None
        session = session or self._session_factory()
        try:
            team = TournamentTeam(
                tournament_id=data["tournament_id"],
                name=data["name"],
                captain_user_id=data["captain_user_id"],
            )
            session.add(team)
            if own:
                session.commit()
                session.refresh(team)
            else:
                session.flush()
                session.refresh(team)
            return team.to_dict()
        except Exception:
            if own:
                session.rollback()
            raise
        finally:
            if own:
                session.close()

    def find_by_tournament(self, tournament_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            teams = session.query(TournamentTeam).filter(
                TournamentTeam.tournament_id == tournament_id
            ).all()
            return [t.to_dict() for t in teams]
        finally:
            session.close()

    def find_by_id(self, team_id: int) -> dict | None:
        session = self._session_factory()
        try:
            t = session.query(TournamentTeam).filter(TournamentTeam.id == team_id).first()
            return t.to_dict() if t else None
        finally:
            session.close()

    def count_by_tournament(self, tournament_id: int) -> int:
        session = self._session_factory()
        try:
            return session.query(TournamentTeam).filter(
                TournamentTeam.tournament_id == tournament_id
            ).count()
        finally:
            session.close()


tournament_team_repository = TournamentTeamRepository()
