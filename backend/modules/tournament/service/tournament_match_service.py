from modules.tournament.repository.tournament_repository import tournament_repository as _default_t_repo
from modules.tournament.repository.tournament_participant_repository import tournament_participant_repository as _default_p_repo
from modules.tournament.repository.tournament_match_repository import tournament_match_repository as _default_m_repo
from modules.tournament.repository.tournament_standing_repository import tournament_standing_repository as _default_s_repo
from modules.tournament.repository.player_score_repository import player_score_repository as _default_ps_repo
from modules.tournament.model.tournament_model import TournamentFormat
from modules.tournament.model.tournament_match_model import TournamentMatchStatus
from modules.tournament.model.tournament_participant_model import ParticipantStatus


class TournamentMatchService:
    def __init__(
        self,
        tournament_repository=None,
        participant_repository=None,
        match_repository=None,
        standing_repository=None,
        player_score_repository=None,
    ):
        self._t_repo = tournament_repository or _default_t_repo
        self._p_repo = participant_repository or _default_p_repo
        self._m_repo = match_repository or _default_m_repo
        self._s_repo = standing_repository or _default_s_repo
        self._ps_repo = player_score_repository or _default_ps_repo

    def list_matches(self, tournament_id: int) -> list[dict]:
        return self._m_repo.find_by_tournament(tournament_id)

    def create_match(self, tournament_id: int, data: dict) -> dict:
        """
        Schedule a match. Validates:
        - Tournament exists
        - home != away
        - Both participants are REGISTERED in the tournament (for INDIVIDUAL type)
        """
        tournament = self._t_repo.find_by_id(tournament_id)
        if not tournament:
            raise ValueError("Tournament not found.")

        is_team = tournament["participant_type"] == "TEAM"
        home_key = "home_team_id" if is_team else "home_user_id"
        away_key = "away_team_id" if is_team else "away_user_id"

        home_id = data.get(home_key)
        away_id = data.get(away_key)

        if not home_id or not away_id:
            raise ValueError(f"Both {home_key} and {away_key} are required.")
        if home_id == away_id:
            raise ValueError("Home and away participants cannot be the same.")

        if not is_team:
            for uid in (home_id, away_id):
                p = self._p_repo.find_by_tournament_and_user(tournament_id, uid)
                if not p or p["status"] != ParticipantStatus.REGISTERED:
                    raise ValueError(f"User {uid} is not a registered participant.")
        else:
            # Validate both teams belong to this tournament
            from modules.tournament.repository.tournament_team_repository import tournament_team_repository as _team_repo
            for team_id in (home_id, away_id):
                team = _team_repo.find_by_id(team_id)
                if not team or team["tournament_id"] != tournament_id:
                    raise ValueError(f"Team {team_id} is not registered in this tournament.")

        return self._m_repo.create({
            "tournament_id": tournament_id,
            "round": data.get("round", 1),
            home_key: home_id,
            away_key: away_id,
            "scheduled_at": data.get("scheduled_at"),
        })

    def record_result(
        self,
        tournament_id: int,
        match_id: int,
        home_score: int,
        away_score: int,
        overrides: dict | None = None,
    ) -> dict:
        """
        Record result, award points, update standings and global scores.

        overrides (optional): {"home_points": int, "away_points": int, "notes": str}
        Manual overrides REQUIRE a non-empty "notes" string.
        """
        tournament = self._t_repo.find_by_id(tournament_id)
        if not tournament:
            raise ValueError("Tournament not found.")

        match = self._m_repo.find_by_id(match_id)
        if not match:
            raise ValueError("Match not found.")
        if match["tournament_id"] != tournament_id:
            raise ValueError("Match does not belong to the given tournament.")
        if match["status"] == TournamentMatchStatus.COMPLETED:
            raise ValueError("Match result has already been recorded.")

        rules = tournament.get("rules_json") or {}
        fmt = tournament.get("format_type", TournamentFormat.LEAGUE)

        # Determine result
        if home_score > away_score:
            home_result, away_result = "win", "loss"
        elif away_score > home_score:
            home_result, away_result = "loss", "win"
        else:
            if fmt == TournamentFormat.KNOCKOUT:
                raise ValueError("Draws are not allowed in KNOCKOUT format. Adjust the scores.")
            home_result = away_result = "draw"

        _pts = {
            "win": rules.get("win_points", 3),
            "draw": rules.get("draw_points", 1),
            "loss": rules.get("loss_points", 0),
        }

        if overrides:
            if not overrides.get("notes", "").strip():
                raise ValueError("Manual point overrides require a non-empty 'notes' field.")
            home_pts = overrides.get("home_points", _pts[home_result])
            away_pts = overrides.get("away_points", _pts[away_result])
        else:
            home_pts = _pts[home_result]
            away_pts = _pts[away_result]

        result = self._m_repo.record_result(match_id, {
            "home_score": home_score,
            "away_score": away_score,
            "home_points_awarded": home_pts,
            "away_points_awarded": away_pts,
            "notes": (overrides or {}).get("notes"),
        })

        is_team = tournament["participant_type"] == "TEAM"
        self._update_standings(tournament_id, match, home_result, away_result, home_pts, away_pts, is_team)
        self._s_repo.rerank(tournament_id)
        self._update_global_scores(tournament, match, home_result, away_result, rules, is_team)

        return result

    # ── Internal helpers ──────────────────────────────────────────────

    def _delta(self, result: str, points: int) -> dict:
        return {
            "won": 1 if result == "win" else 0,
            "drawn": 1 if result == "draw" else 0,
            "lost": 1 if result == "loss" else 0,
            "points": points,
        }

    def _update_standings(
        self,
        tournament_id: int,
        match: dict,
        home_result: str,
        away_result: str,
        home_pts: int,
        away_pts: int,
        is_team: bool,
    ) -> None:
        if is_team:
            self._s_repo.upsert_team(tournament_id, match["home_team_id"], self._delta(home_result, home_pts))
            self._s_repo.upsert_team(tournament_id, match["away_team_id"], self._delta(away_result, away_pts))
        else:
            self._s_repo.upsert_user(tournament_id, match["home_user_id"], self._delta(home_result, home_pts))
            self._s_repo.upsert_user(tournament_id, match["away_user_id"], self._delta(away_result, away_pts))

    def _update_global_scores(
        self,
        tournament: dict,
        match: dict,
        home_result: str,
        away_result: str,
        rules: dict,
        is_team: bool,
    ) -> None:
        region_id = tournament.get("region_id")
        sport_id = tournament.get("sport_id")
        if not region_id or not sport_id:
            return  # no global scoring without region+sport context

        global_pts = rules.get("global_points_per_win", 10)

        if is_team:
            if home_result == "draw":
                for member in self._p_repo.find_by_team(match["home_team_id"]):
                    self._ps_repo.add_points(member["user_id"], region_id, sport_id, 0)
                for member in self._p_repo.find_by_team(match["away_team_id"]):
                    self._ps_repo.add_points(member["user_id"], region_id, sport_id, 0)
            else:
                winner_team_id = match["home_team_id"] if home_result == "win" else match["away_team_id"]
                loser_team_id = match["away_team_id"] if home_result == "win" else match["home_team_id"]
                for member in self._p_repo.find_by_team(winner_team_id):
                    self._ps_repo.add_points(member["user_id"], region_id, sport_id, global_pts)
                for member in self._p_repo.find_by_team(loser_team_id):
                    self._ps_repo.add_points(member["user_id"], region_id, sport_id, 0)
        else:
            if home_result == "draw":
                self._ps_repo.add_points(match["home_user_id"], region_id, sport_id, 0)
                self._ps_repo.add_points(match["away_user_id"], region_id, sport_id, 0)
            else:
                winner_uid = match["home_user_id"] if home_result == "win" else match["away_user_id"]
                loser_uid = match["away_user_id"] if home_result == "win" else match["home_user_id"]
                self._ps_repo.add_points(winner_uid, region_id, sport_id, global_pts)
                self._ps_repo.add_points(loser_uid, region_id, sport_id, 0)


tournament_match_service = TournamentMatchService()
