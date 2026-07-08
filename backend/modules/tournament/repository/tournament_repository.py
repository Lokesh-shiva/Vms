from datetime import datetime
from core.database.db_connection import SessionLocal
from modules.tournament.model.tournament_model import Tournament
from modules.tournament.model.tournament_participant_model import TournamentParticipant


class TournamentRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            t = Tournament(
                name=data["name"],
                sport_id=data.get("sport_id"),
                region_id=data.get("region_id"),
                organizer=data["organizer"],
                start_date=data["start_date"],
                end_date=data["end_date"],
                max_teams=data.get("max_teams", 8),
                status=data.get("status", "UPCOMING"),
                format_type=data.get("format_type", "LEAGUE"),
                participant_type=data.get("participant_type", "INDIVIDUAL"),
                team_size=data.get("team_size", 1),
                entry_fee=data.get("entry_fee", 0),
                prize_pool=data.get("prize_pool", ""),
                banner_url=data.get("banner_url"),
                description=data.get("description"),
                sponsor_user_id=data.get("sponsor_user_id"),
                rules_json=data.get("rules_json", {}),
            )
            session.add(t)
            session.commit()
            session.refresh(t)
            return t.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, tournament_id: int) -> dict | None:
        session = self._session_factory()
        try:
            t = session.query(Tournament).filter(Tournament.id == tournament_id).first()
            return t.to_dict() if t else None
        finally:
            session.close()

    def find_all(self) -> list[dict]:
        session = self._session_factory()
        try:
            return [t.to_dict() for t in session.query(Tournament).order_by(Tournament.id.desc()).all()]
        finally:
            session.close()

    def update(self, tournament_id: int, data: dict) -> dict | None:
        session = self._session_factory()
        try:
            t = session.query(Tournament).filter(Tournament.id == tournament_id).first()
            if not t:
                return None
            for key, value in data.items():
                if key not in ("id", "created_at", "updated_at") and hasattr(t, key):
                    setattr(t, key, value)
            t.updated_at = datetime.utcnow()
            session.commit()
            session.refresh(t)
            return t.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def delete(self, tournament_id: int) -> bool:
        session = self._session_factory()
        try:
            t = session.query(Tournament).filter(Tournament.id == tournament_id).first()
            if not t:
                return False
            session.delete(t)
            session.commit()
            return True
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def _enrich(self, t: Tournament, session) -> dict:
        """Augment a tournament dict with sport name, location name, and registered count."""
        from modules.tournament.model.tournament_participant_model import ParticipantStatus
        base = t.to_dict()

        if t.sport_id:
            from modules.cart_type.model.cart_type_model import CartType
            sport = session.query(CartType).filter(CartType.id == t.sport_id).first()
            if not sport:
                # fall back to the sports table (tournament FK points there)
                from sqlalchemy import text
                row = session.execute(
                    text("SELECT name FROM sports WHERE id = :sid"),
                    {"sid": t.sport_id},
                ).fetchone()
                base["sport"] = row[0] if row else ""
            else:
                base["sport"] = sport.name
        else:
            base["sport"] = ""

        if t.region_id:
            from modules.location.model.location_model import Location
            loc = session.query(Location).filter(Location.id == t.region_id).first()
            base["location"] = loc.name if loc else ""
        else:
            base["location"] = ""

        registered = (
            session.query(TournamentParticipant)
            .filter(
                TournamentParticipant.tournament_id == t.id,
                TournamentParticipant.status == ParticipantStatus.REGISTERED,
            )
            .count()
        )
        base["registered_teams"] = registered
        return base

    def find_all_enriched(self) -> list[dict]:
        session = self._session_factory()
        try:
            rows = session.query(Tournament).order_by(Tournament.id.desc()).all()
            return [self._enrich(t, session) for t in rows]
        finally:
            session.close()

    def find_by_id_enriched(self, tournament_id: int) -> dict | None:
        session = self._session_factory()
        try:
            t = session.query(Tournament).filter(Tournament.id == tournament_id).first()
            return self._enrich(t, session) if t else None
        finally:
            session.close()


tournament_repository = TournamentRepository()
