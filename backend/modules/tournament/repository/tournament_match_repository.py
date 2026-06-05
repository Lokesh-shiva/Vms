from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.tournament.model.tournament_match_model import TournamentMatch, TournamentMatchStatus


class TournamentMatchRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            m = TournamentMatch(
                tournament_id=data["tournament_id"],
                round=data.get("round", 1),
                home_user_id=data.get("home_user_id"),
                away_user_id=data.get("away_user_id"),
                home_team_id=data.get("home_team_id"),
                away_team_id=data.get("away_team_id"),
                scheduled_at=data.get("scheduled_at"),
                status=TournamentMatchStatus.SCHEDULED,
            )
            session.add(m)
            session.commit()
            session.refresh(m)
            return m.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, match_id: int) -> dict | None:
        session = self._session_factory()
        try:
            m = session.query(TournamentMatch).filter(TournamentMatch.id == match_id).first()
            return m.to_dict() if m else None
        finally:
            session.close()

    def find_by_tournament(self, tournament_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            matches = session.query(TournamentMatch).filter(
                TournamentMatch.tournament_id == tournament_id
            ).order_by(TournamentMatch.round.asc(), TournamentMatch.id.asc()).all()
            return [m.to_dict() for m in matches]
        finally:
            session.close()

    def record_result(self, match_id: int, data: dict) -> dict | None:
        session = self._session_factory()
        try:
            m = session.query(TournamentMatch).filter(TournamentMatch.id == match_id).first()
            if not m:
                return None
            m.home_score = data["home_score"]
            m.away_score = data["away_score"]
            m.home_points_awarded = data["home_points_awarded"]
            m.away_points_awarded = data["away_points_awarded"]
            m.status = TournamentMatchStatus.COMPLETED
            m.completed_at = datetime.utcnow()
            if data.get("notes"):
                m.notes = data["notes"]
            session.commit()
            session.refresh(m)
            return m.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


tournament_match_repository = TournamentMatchRepository()
