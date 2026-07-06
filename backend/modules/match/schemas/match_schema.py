from pydantic import BaseModel, Field

VALID_SKILL_LEVELS = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}
VALID_VISIBILITIES = {"OPEN", "SOCIETY", "PRIVATE"}


class MatchArriveSchema(BaseModel):
    """Request body for POST /matches/{match_id}/arrive"""

    latitude: float = Field(..., description="Player's current GPS latitude")
    longitude: float = Field(..., description="Player's current GPS longitude")


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
        if (
            cart_type_id is None
            or not isinstance(cart_type_id, int)
            or cart_type_id <= 0
        ):
            self.errors.append(
                "'cart_type_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["cart_type_id"] = cart_type_id

        # timeslot_id — required, positive int
        timeslot_id = self._data.get("timeslot_id")
        if timeslot_id is None or not isinstance(timeslot_id, int) or timeslot_id <= 0:
            self.errors.append(
                "'timeslot_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["timeslot_id"] = timeslot_id

        # region_id — required, positive int
        region_id = self._data.get("region_id")
        if region_id is None or not isinstance(region_id, int) or region_id <= 0:
            self.errors.append(
                "'region_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["region_id"] = region_id

        # max_players — required, int > 1
        max_players = self._data.get("max_players")
        if max_players is None or not isinstance(max_players, int) or max_players <= 1:
            self.errors.append(
                "'max_players' is required and must be an integer greater than 1."
            )
        else:
            self.validated_data["max_players"] = max_players

        # skill_level — optional, must be valid enum if present
        skill_level = self._data.get("skill_level")
        if skill_level is not None:
            if (
                not isinstance(skill_level, str)
                or skill_level.upper() not in VALID_SKILL_LEVELS
            ):
                self.errors.append(
                    f"'skill_level' must be one of: {', '.join(sorted(VALID_SKILL_LEVELS))}."
                )
            else:
                self.validated_data["skill_level"] = skill_level.upper()

        return len(self.errors) == 0


class CaptainCreateMatchSchema:
    """
    Validates input for POST /matches/captain-create.

    Required: cart_type_id, region_id, max_players, visibility
    Conditionally required: society_id (when visibility == SOCIETY)
    Optional: skill_level
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        cart_type_id = self._data.get("cart_type_id")
        if cart_type_id is None or not isinstance(cart_type_id, int) or cart_type_id <= 0:
            self.errors.append("'cart_type_id' is required and must be a positive integer.")
        else:
            self.validated_data["cart_type_id"] = cart_type_id

        region_id = self._data.get("region_id")
        if region_id is None or not isinstance(region_id, int) or region_id <= 0:
            self.errors.append("'region_id' is required and must be a positive integer.")
        else:
            self.validated_data["region_id"] = region_id

        max_players = self._data.get("max_players")
        if max_players is None or not isinstance(max_players, int) or not (2 <= max_players <= 22):
            self.errors.append("'max_players' is required and must be an integer between 2 and 22.")
        else:
            self.validated_data["max_players"] = max_players

        visibility = self._data.get("visibility")
        if visibility not in VALID_VISIBILITIES:
            self.errors.append(f"'visibility' must be one of: {', '.join(sorted(VALID_VISIBILITIES))}.")
        else:
            self.validated_data["visibility"] = visibility

        society_id = self._data.get("society_id")
        if visibility == "SOCIETY":
            if society_id is None or not isinstance(society_id, int) or society_id <= 0:
                self.errors.append("'society_id' is required when visibility is SOCIETY.")
            else:
                self.validated_data["society_id"] = society_id
        else:
            self.validated_data["society_id"] = None

        skill_level = self._data.get("skill_level")
        if skill_level is not None:
            if not isinstance(skill_level, str) or skill_level.upper() not in VALID_SKILL_LEVELS:
                self.errors.append(f"'skill_level' must be one of: {', '.join(sorted(VALID_SKILL_LEVELS))}.")
            else:
                self.validated_data["skill_level"] = skill_level.upper()
        else:
            self.validated_data["skill_level"] = None

        return len(self.errors) == 0
