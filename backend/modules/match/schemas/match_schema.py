VALID_SKILL_LEVELS = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}


class CreateMatchSchema:
    """
    Validates input for match creation.

    Required: cart_type_id, timeslot_id, region_id, max_players
    Optional: skill_level (BEGINNER | INTERMEDIATE | ADVANCED)

    max_players must be > 1.
    skill_level, if present, must be a recognised enum value.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # cart_type_id — required, positive int
        cart_type_id = self._data.get("cart_type_id")
        if cart_type_id is None or not isinstance(cart_type_id, int) or cart_type_id <= 0:
            self.errors.append("'cart_type_id' is required and must be a positive integer.")
        else:
            self.validated_data["cart_type_id"] = cart_type_id

        # timeslot_id — required, positive int
        timeslot_id = self._data.get("timeslot_id")
        if timeslot_id is None or not isinstance(timeslot_id, int) or timeslot_id <= 0:
            self.errors.append("'timeslot_id' is required and must be a positive integer.")
        else:
            self.validated_data["timeslot_id"] = timeslot_id

        # region_id — required, positive int
        region_id = self._data.get("region_id")
        if region_id is None or not isinstance(region_id, int) or region_id <= 0:
            self.errors.append("'region_id' is required and must be a positive integer.")
        else:
            self.validated_data["region_id"] = region_id

        # max_players — required, int > 1
        max_players = self._data.get("max_players")
        if max_players is None or not isinstance(max_players, int) or max_players <= 1:
            self.errors.append("'max_players' is required and must be an integer greater than 1.")
        else:
            self.validated_data["max_players"] = max_players

        # skill_level — optional, must be valid enum if present
        skill_level = self._data.get("skill_level")
        if skill_level is not None:
            if not isinstance(skill_level, str) or skill_level.upper() not in VALID_SKILL_LEVELS:
                self.errors.append(
                    f"'skill_level' must be one of: {', '.join(sorted(VALID_SKILL_LEVELS))}."
                )
            else:
                self.validated_data["skill_level"] = skill_level.upper()

        return len(self.errors) == 0
