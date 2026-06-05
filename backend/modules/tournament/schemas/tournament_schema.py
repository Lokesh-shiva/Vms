from datetime import date

_VALID_FORMAT_TYPES = frozenset({"KNOCKOUT", "ROUND_ROBIN", "LEAGUE"})
_VALID_PARTICIPANT_TYPES = frozenset({"INDIVIDUAL", "TEAM"})


class CreateTournamentSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        name = self._data.get("name", "")
        if not isinstance(name, str) or not name.strip():
            self.errors.append("'name' is required.")
        else:
            self.validated_data["name"] = name.strip()

        organizer = self._data.get("organizer", "")
        if not isinstance(organizer, str) or not organizer.strip():
            self.errors.append("'organizer' is required.")
        else:
            self.validated_data["organizer"] = organizer.strip()

        for field in ("start_date", "end_date"):
            val = self._data.get(field)
            if not val:
                self.errors.append(f"'{field}' is required.")
            else:
                try:
                    self.validated_data[field] = date.fromisoformat(str(val))
                except (ValueError, TypeError):
                    self.errors.append(f"'{field}' must be a valid date (YYYY-MM-DD).")

        for field in ("sport_id", "region_id"):
            if field in self._data and self._data[field] is not None:
                val = self._data[field]
                if not isinstance(val, int) or val <= 0:
                    self.errors.append(f"'{field}' must be a positive integer.")
                else:
                    self.validated_data[field] = val

        max_teams = self._data.get("max_teams", 8)
        if not isinstance(max_teams, int) or max_teams < 2:
            self.errors.append("'max_teams' must be an integer >= 2.")
        else:
            self.validated_data["max_teams"] = max_teams

        format_type = self._data.get("format_type", "LEAGUE")
        if format_type not in _VALID_FORMAT_TYPES:
            self.errors.append(f"'format_type' must be one of {sorted(_VALID_FORMAT_TYPES)}.")
        else:
            self.validated_data["format_type"] = format_type

        participant_type = self._data.get("participant_type", "INDIVIDUAL")
        if participant_type not in _VALID_PARTICIPANT_TYPES:
            self.errors.append(f"'participant_type' must be one of {sorted(_VALID_PARTICIPANT_TYPES)}.")
        else:
            self.validated_data["participant_type"] = participant_type

        if "team_size" in self._data:
            ts = self._data["team_size"]
            if not isinstance(ts, int) or ts < 1:
                self.errors.append("'team_size' must be a positive integer.")
            else:
                self.validated_data["team_size"] = ts

        if "rules_json" in self._data:
            rj = self._data["rules_json"]
            if rj is not None and not isinstance(rj, dict):
                self.errors.append("'rules_json' must be a dict or null.")
            else:
                self.validated_data["rules_json"] = rj

        return len(self.errors) == 0


class UpdateTournamentSchema:
    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "name" in self._data:
            if not isinstance(self._data["name"], str) or not self._data["name"].strip():
                self.errors.append("'name' must be a non-empty string.")
            else:
                self.validated_data["name"] = self._data["name"].strip()

        if "organizer" in self._data:
            if not isinstance(self._data["organizer"], str) or not self._data["organizer"].strip():
                self.errors.append("'organizer' must be a non-empty string.")
            else:
                self.validated_data["organizer"] = self._data["organizer"].strip()

        for field in ("start_date", "end_date"):
            if field in self._data:
                try:
                    self.validated_data[field] = date.fromisoformat(str(self._data[field]))
                except (ValueError, TypeError):
                    self.errors.append(f"'{field}' must be a valid date (YYYY-MM-DD).")

        if "status" in self._data:
            self.validated_data["status"] = self._data["status"]

        if "max_teams" in self._data:
            val = self._data["max_teams"]
            if not isinstance(val, int) or val < 2:
                self.errors.append("'max_teams' must be an integer >= 2.")
            else:
                self.validated_data["max_teams"] = val

        if "format_type" in self._data:
            fmt = self._data["format_type"]
            if fmt not in _VALID_FORMAT_TYPES:
                self.errors.append(f"'format_type' must be one of {sorted(_VALID_FORMAT_TYPES)}.")
            else:
                self.validated_data["format_type"] = fmt

        if "participant_type" in self._data:
            pt = self._data["participant_type"]
            if pt not in _VALID_PARTICIPANT_TYPES:
                self.errors.append(f"'participant_type' must be one of {sorted(_VALID_PARTICIPANT_TYPES)}.")
            else:
                self.validated_data["participant_type"] = pt

        if "team_size" in self._data:
            ts = self._data["team_size"]
            if not isinstance(ts, int) or ts < 1:
                self.errors.append("'team_size' must be a positive integer.")
            else:
                self.validated_data["team_size"] = ts

        if "rules_json" in self._data:
            rj = self._data["rules_json"]
            if rj is not None and not isinstance(rj, dict):
                self.errors.append("'rules_json' must be a dict or null.")
            else:
                self.validated_data["rules_json"] = rj

        return len(self.errors) == 0
