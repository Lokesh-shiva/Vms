from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.tournament.model.tournament_standing_model import TournamentStanding


class TournamentStandingRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def upsert_user(self, tournament_id: int, user_id: int, delta: dict) -> None:
        """Add delta values to a user's standing row, creating it if missing."""
        session = self._session_factory()
        try:
            row = session.query(TournamentStanding).filter(
                TournamentStanding.tournament_id == tournament_id,
                TournamentStanding.user_id == user_id,
            ).first()
            if not row:
                row = TournamentStanding(tournament_id=tournament_id, user_id=user_id)
                session.add(row)
                session.flush()
            row.played += 1
            row.won += delta.get("won", 0)
            row.drawn += delta.get("drawn", 0)
            row.lost += delta.get("lost", 0)
            row.points += delta.get("points", 0)
            row.updated_at = datetime.utcnow()
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def upsert_team(self, tournament_id: int, team_id: int, delta: dict) -> None:
        """Add delta values to a team's standing row, creating it if missing."""
        session = self._session_factory()
        try:
            row = session.query(TournamentStanding).filter(
                TournamentStanding.tournament_id == tournament_id,
                TournamentStanding.team_id == team_id,
            ).first()
            if not row:
                row = TournamentStanding(tournament_id=tournament_id, team_id=team_id)
                session.add(row)
                session.flush()
            row.played += 1
            row.won += delta.get("won", 0)
            row.drawn += delta.get("drawn", 0)
            row.lost += delta.get("lost", 0)
            row.points += delta.get("points", 0)
            row.updated_at = datetime.utcnow()
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def rerank(self, tournament_id: int) -> None:
        """Recompute rank for all standings ordered by points desc."""
        session = self._session_factory()
        try:
            rows = session.query(TournamentStanding).filter(
                TournamentStanding.tournament_id == tournament_id,
            ).order_by(TournamentStanding.points.desc()).all()
            for i, row in enumerate(rows, start=1):
                row.rank = i
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_tournament(self, tournament_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = session.query(TournamentStanding).filter(
                TournamentStanding.tournament_id == tournament_id,
            ).order_by(TournamentStanding.points.desc()).all()
            return [r.to_dict() for r in rows]
        finally:
            session.close()


tournament_standing_repository = TournamentStandingRepository()
