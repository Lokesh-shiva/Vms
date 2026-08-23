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
        """Standings enriched with a display name — resolved server-side so any
        authenticated caller gets usable names, not just admins (who separately
        have access to the registrations list to resolve this themselves)."""
        from modules.tournament.model.tournament_team_model import TournamentTeam
        from modules.user.model.user_model import User

        session = self._session_factory()
        try:
            rows = session.query(TournamentStanding).filter(
                TournamentStanding.tournament_id == tournament_id,
            ).order_by(TournamentStanding.points.desc()).all()

            user_ids = {r.user_id for r in rows if r.user_id}
            team_ids = {r.team_id for r in rows if r.team_id}
            names_by_user = {}
            if user_ids:
                names_by_user = {
                    u.id: u.name
                    for u in session.query(User).filter(User.id.in_(user_ids)).all()
                }
            names_by_team = {}
            if team_ids:
                names_by_team = {
                    t.id: t.name
                    for t in session.query(TournamentTeam).filter(TournamentTeam.id.in_(team_ids)).all()
                }

            result = []
            for r in rows:
                data = r.to_dict()
                if r.team_id:
                    data["name"] = names_by_team.get(r.team_id, f"Team #{r.team_id}")
                elif r.user_id:
                    data["name"] = names_by_user.get(r.user_id, f"Player #{r.user_id}")
                else:
                    data["name"] = "TBD"
                result.append(data)
            return result
        finally:
            session.close()


tournament_standing_repository = TournamentStandingRepository()
