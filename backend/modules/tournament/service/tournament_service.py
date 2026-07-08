from modules.tournament.repository.tournament_repository import tournament_repository as _default_repo
from modules.tournament.repository.tournament_team_repository import tournament_team_repository as _default_team_repo
from modules.tournament.repository.tournament_participant_repository import tournament_participant_repository as _default_participant_repo
from modules.tournament.model.tournament_model import (
    TournamentStatus, TournamentFormat, TournamentParticipantType, RULES_JSON_DEFAULTS
)
from modules.tournament.model.tournament_participant_model import ParticipantStatus


class TournamentService:
    def __init__(self, repository=None, team_repository=None, participant_repository=None, user_repository=None):
        self.repository = repository or _default_repo
        self.team_repository = team_repository or _default_team_repo
        self.participant_repository = participant_repository or _default_participant_repo
        if user_repository is not None:
            self.user_repository = user_repository
        else:
            from modules.user.repository.user_repository import user_repository as _default_user_repo
            self.user_repository = _default_user_repo

    def _merge_rules(self, rules_input: dict | None) -> dict:
        merged = dict(RULES_JSON_DEFAULTS)
        if rules_input:
            merged.update(rules_input)
        return merged

    def list_tournaments(self) -> list[dict]:
        return self.repository.find_all()

    def get_tournament(self, tournament_id: int) -> dict | None:
        return self.repository.find_by_id(tournament_id)

    def create_tournament(self, data: dict) -> dict:
        if not data.get("name", "").strip():
            raise ValueError("Tournament name is required.")
        if not data.get("organizer", "").strip():
            raise ValueError("Organizer is required.")
        if not data.get("start_date"):
            raise ValueError("start_date is required.")
        if not data.get("end_date"):
            raise ValueError("end_date is required.")
        if data["start_date"] > data["end_date"]:
            raise ValueError("start_date must be before end_date.")
        fmt = data.get("format_type", TournamentFormat.LEAGUE)
        if fmt not in TournamentFormat.ALL:
            raise ValueError(f"format_type must be one of {TournamentFormat.ALL}.")
        pt = data.get("participant_type", TournamentParticipantType.INDIVIDUAL)
        if pt not in TournamentParticipantType.ALL:
            raise ValueError(f"participant_type must be one of {TournamentParticipantType.ALL}.")
        data["format_type"] = fmt
        data["participant_type"] = pt
        data["rules_json"] = self._merge_rules(data.get("rules_json"))
        return self.repository.create(data)

    def update_tournament(self, tournament_id: int, data: dict) -> dict:
        existing = self.repository.find_by_id(tournament_id)
        if not existing:
            raise ValueError(f"Tournament {tournament_id} not found.")
        if "status" in data and data["status"] not in TournamentStatus.ALL:
            raise ValueError(f"Invalid status. Must be one of: {TournamentStatus.ALL}")
        if "format_type" in data and data["format_type"] not in TournamentFormat.ALL:
            raise ValueError(f"format_type must be one of {TournamentFormat.ALL}.")
        if "participant_type" in data and data["participant_type"] not in TournamentParticipantType.ALL:
            raise ValueError(f"participant_type must be one of {TournamentParticipantType.ALL}.")
        if "rules_json" in data and data["rules_json"]:
            current_rules = existing.get("rules_json") or {}
            current_rules.update(data["rules_json"])
            data["rules_json"] = current_rules
        if "sponsor_user_id" in data and data["sponsor_user_id"] is not None:
            sponsor = self.user_repository.find_by_id(data["sponsor_user_id"])
            if not sponsor:
                raise ValueError("Sponsor user not found.")
            if sponsor.get("role") != "csr_partner":
                raise ValueError("sponsor_user_id must belong to a CSR_PARTNER user.")
        return self.repository.update(tournament_id, data)

    def delete_tournament(self, tournament_id: int) -> bool:
        existing = self.repository.find_by_id(tournament_id)
        if not existing:
            raise ValueError(f"Tournament {tournament_id} not found.")
        return self.repository.delete(tournament_id)

    def register(self, tournament_id: int, user_id: int, team_data: dict | None = None) -> dict:
        tournament = self.repository.find_by_id(tournament_id)
        if not tournament:
            raise ValueError("Tournament not found.")
        if tournament["status"] in (TournamentStatus.ONGOING, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED):
            raise ValueError("Cannot register: tournament is not in UPCOMING status.")

        if tournament["participant_type"] == TournamentParticipantType.INDIVIDUAL:
            registered = self.participant_repository.count_registered(tournament_id)
            if registered >= tournament["max_teams"]:
                raise ValueError("Tournament is at full capacity.")
            existing = self.participant_repository.find_by_tournament_and_user(tournament_id, user_id)
            if existing and existing["status"] == ParticipantStatus.REGISTERED:
                raise ValueError("User is already registered in this tournament.")
            return self.participant_repository.create({
                "tournament_id": tournament_id,
                "user_id": user_id,
            })
        else:
            if not team_data:
                raise ValueError("team_data required for TEAM tournaments.")
            team_name = team_data.get("team_name", "").strip()
            if not team_name:
                raise ValueError("team_name is required.")
            member_ids: list[int] = team_data.get("member_user_ids", [])
            if len(member_ids) < 1:
                raise ValueError("At least one member_user_id is required.")
            team_size = tournament.get("team_size", 1)
            if len(member_ids) > team_size:
                raise ValueError(f"Team size cannot exceed {team_size}.")
            current_teams = self.team_repository.count_by_tournament(tournament_id)
            if current_teams >= tournament["max_teams"]:
                raise ValueError("Tournament is at full capacity.")
            team = self.team_repository.create({
                "tournament_id": tournament_id,
                "name": team_name,
                "captain_user_id": user_id,
            })
            all_user_ids = list({user_id} | set(member_ids))
            for uid in all_user_ids:
                self.participant_repository.create({
                    "tournament_id": tournament_id,
                    "user_id": uid,
                    "team_id": team["id"],
                })
            return team

    def withdraw(self, tournament_id: int, user_id: int) -> dict:
        existing = self.participant_repository.find_by_tournament_and_user(tournament_id, user_id)
        if not existing or existing["status"] != ParticipantStatus.REGISTERED:
            raise ValueError("User is not registered in this tournament.")
        return self.participant_repository.update_status(tournament_id, user_id, ParticipantStatus.WITHDRAWN)

    def list_sponsored(self, sponsor_user_id: int) -> list[dict]:
        """Tournaments sponsored by this CSR partner, enriched with registration counts."""
        return [
            t for t in self.repository.find_all_enriched()
            if t.get("sponsor_user_id") == sponsor_user_id
        ]

    def list_registrations(self, tournament_id: int) -> list[dict]:
        """
        Admin-facing view of who's registered for a tournament.

        For TEAM tournaments, groups participants under their team with
        member names. For INDIVIDUAL tournaments, returns a flat list of
        registered players. Enriches every entry with the user's name.
        """
        tournament = self.repository.find_by_id(tournament_id)
        if not tournament:
            raise ValueError("Tournament not found.")

        participants = self.participant_repository.find_by_tournament(tournament_id)
        name_by_user_id: dict[int, str] = {}
        for p in participants:
            user = self.user_repository.find_by_id(p["user_id"])
            name_by_user_id[p["user_id"]] = user["name"] if user else "Unknown"

        if tournament["participant_type"] != TournamentParticipantType.TEAM:
            return [
                {
                    "user_id": p["user_id"],
                    "name": name_by_user_id[p["user_id"]],
                    "team_id": None,
                    "team_name": None,
                    "joined_at": p["joined_at"],
                }
                for p in participants
            ]

        teams = self.team_repository.find_by_tournament(tournament_id)
        result: list[dict] = []
        for team in teams:
            members = [p for p in participants if p["team_id"] == team["id"]]
            result.append({
                "team_id": team["id"],
                "team_name": team["name"],
                "captain_user_id": team["captain_user_id"],
                "captain_name": name_by_user_id.get(team["captain_user_id"], "Unknown"),
                "members": [
                    {"user_id": m["user_id"], "name": name_by_user_id[m["user_id"]]}
                    for m in members
                ],
            })
        return result
