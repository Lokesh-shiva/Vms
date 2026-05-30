from modules.captain.model.captain_model import CaptainStatus

VALID_STATUSES: frozenset[str] = CaptainStatus.ALL


class CreateCaptainSchema:
    """
    Validates input for captain creation.

    Required fields: user_id
    Optional fields: region_id, bio
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # user_id — required, positive int
        user_id = self._data.get("user_id")
        if user_id is None or not isinstance(user_id, int) or user_id <= 0:
            self.errors.append("'user_id' is required and must be a positive integer.")
        else:
            self.validated_data["user_id"] = user_id

        # region_id — optional, positive int or None
        if "region_id" in self._data:
            region_id = self._data["region_id"]
            if region_id is not None and (
                not isinstance(region_id, int) or region_id <= 0
            ):
                self.errors.append("'region_id' must be a positive integer or null.")
            else:
                self.validated_data["region_id"] = region_id

        # bio — optional, string or None
        if "bio" in self._data:
            bio = self._data["bio"]
            if bio is not None and not isinstance(bio, str):
                self.errors.append("'bio' must be a string or null.")
            else:
                self.validated_data["bio"] = bio.strip() if isinstance(bio, str) else None

        return len(self.errors) == 0


class UpdateCaptainSchema:
    """
    Validates input for captain updates.

    All fields are optional. Only provided fields are validated and returned.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors: list[str] = []
        self.validated_data: dict = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # region_id — optional, positive int or None
        if "region_id" in self._data:
            region_id = self._data["region_id"]
            if region_id is not None and (
                not isinstance(region_id, int) or region_id <= 0
            ):
                self.errors.append("'region_id' must be a positive integer or null.")
            else:
                self.validated_data["region_id"] = region_id

        # status — optional, one of ACTIVE/INACTIVE/SUSPENDED
        if "status" in self._data:
            status = self._data["status"]
            if not isinstance(status, str) or status.upper() not in VALID_STATUSES:
                self.errors.append(
                    f"'status' must be one of {sorted(VALID_STATUSES)}."
                )
            else:
                self.validated_data["status"] = status.upper()

        # bio — optional, string or None
        if "bio" in self._data:
            bio = self._data["bio"]
            if bio is not None and not isinstance(bio, str):
                self.errors.append("'bio' must be a string or null.")
            else:
                self.validated_data["bio"] = bio.strip() if isinstance(bio, str) else None

        return len(self.errors) == 0
