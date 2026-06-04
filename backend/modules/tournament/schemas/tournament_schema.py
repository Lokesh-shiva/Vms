from datetime import date


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

        return len(self.errors) == 0
