from modules.society.model.society_member_model import SocietyRole
from modules.society.repository.society_member_repository import (
    SocietyMemberRepository,
    society_member_repository,
)
from modules.society.repository.society_repository import (
    SocietyRepository,
    society_repository,
)
from modules.tournament.repository.player_score_repository import (
    PlayerScoreRepository,
    player_score_repository,
)
from modules.user.model.user_model import UserRole
from modules.user.repository.user_repository import (
    UserRepository,
    user_repository as _default_user_repository,
)


class SocietyMemberService:
    def __init__(
        self,
        society_repository: SocietyRepository | None = None,
        member_repository: SocietyMemberRepository | None = None,
        player_score_repository: PlayerScoreRepository | None = None,
        user_repository: UserRepository | None = None,
    ) -> None:
        self.society_repository: SocietyRepository = (
            society_repository or globals()["society_repository"]
        )
        self.member_repository: SocietyMemberRepository = (
            member_repository or globals()["society_member_repository"]
        )
        self.player_score_repository: PlayerScoreRepository = (
            player_score_repository or globals()["player_score_repository"]
        )
        self.user_repository: UserRepository = user_repository or _default_user_repository

    def join(self, society_id: int, user_id: int) -> dict:
        """Join a public, active society that is not yet at capacity."""
        society: dict | None = self.society_repository.find_by_id(society_id)
        if society is None:
            raise ValueError("Society not found.")

        if not society["is_active"]:
            raise ValueError("Society is not active.")

        if not society["is_public"]:
            raise ValueError("This society is private.")

        if self.member_repository.find_member(society_id, user_id) is not None:
            raise ValueError("Already a member.")

        count: int = self.member_repository.count_members(society_id)
        if count >= society["max_members"]:
            raise ValueError("Society is at full capacity.")

        return self.member_repository.add_member(society_id, user_id, SocietyRole.MEMBER)

    def leave(self, society_id: int, user_id: int) -> bool:
        """Leave a society. The owner must transfer ownership first."""
        member: dict | None = self.member_repository.find_member(society_id, user_id)
        if member is None:
            raise ValueError("Not a member of this society.")

        if member["role"] == SocietyRole.OWNER:
            raise ValueError("Transfer ownership before leaving.")

        return self.member_repository.remove_member(society_id, user_id)

    def kick(
        self,
        society_id: int,
        target_user_id: int,
        requester_id: int,
        requester_role: str,
    ) -> bool:
        """Remove a member from a society. Only the OWNER or a SUPER_ADMIN may do this."""
        requester_member: dict | None = self.member_repository.find_member(
            society_id, requester_id
        )
        is_owner: bool = (
            requester_member is not None
            and requester_member["role"] == SocietyRole.OWNER
        )
        is_super_admin: bool = requester_role == UserRole.SUPER_ADMIN.value

        if not (is_owner or is_super_admin):
            raise ValueError("Permission denied.")

        target_member: dict | None = self.member_repository.find_member(
            society_id, target_user_id
        )
        if target_member is None:
            raise ValueError("Target user is not a member.")

        if target_member["role"] == SocietyRole.OWNER:
            raise ValueError("Cannot kick the society owner.")

        return self.member_repository.remove_member(society_id, target_user_id)

    def transfer_ownership(
        self, society_id: int, new_owner_id: int, requester_id: int
    ) -> dict:
        """Transfer OWNER role to an existing member. Updates member roles only (v1)."""
        current: dict | None = self.member_repository.find_member(society_id, requester_id)
        if current is None or current["role"] != SocietyRole.OWNER:
            raise ValueError("Only the current owner can transfer ownership.")

        new_member: dict | None = self.member_repository.find_member(society_id, new_owner_id)
        if new_member is None:
            raise ValueError("New owner must already be a member.")

        self.member_repository.update_role(society_id, requester_id, SocietyRole.MEMBER)
        updated: dict | None = self.member_repository.update_role(
            society_id, new_owner_id, SocietyRole.OWNER
        )
        return updated  # type: ignore[return-value]

    def _attach_user_names(self, members: list[dict]) -> list[dict]:
        """Enrich member dicts with the user's display name (app-facing 'name' field)."""
        for m in members:
            user = self.user_repository.find_by_id(m["user_id"])
            m["name"] = user["name"] if user else ""
        return members

    def get_members(self, society_id: int) -> list[dict]:
        """Return all members of a society. Raises ValueError if society does not exist."""
        society: dict | None = self.society_repository.find_by_id(society_id)
        if society is None:
            raise ValueError("Society not found.")
        members = self.member_repository.get_members(society_id)
        return self._attach_user_names(members)

    def get_leaderboard(self, society_id: int) -> list[dict]:
        """Return members sorted by total_points DESC, then matches_played DESC."""
        society: dict | None = self.society_repository.find_by_id(society_id)
        if society is None:
            raise ValueError("Society not found.")

        members: list[dict] = self._attach_user_names(
            self.member_repository.get_members(society_id)
        )
        user_ids: list[int] = [m["user_id"] for m in members]

        scores: list[dict] = self.player_score_repository.get_leaderboard(
            region_id=society["region_id"],
            sport_id=society["sport_id"],
            limit=len(user_ids) + 1,
        )
        score_map: dict[int, dict] = {s["user_id"]: s for s in scores}

        result: list[dict] = [
            {
                "user_id": m["user_id"],
                "name": m["name"],
                "society_member_role": m["role"],
                "total_points": score_map.get(m["user_id"], {}).get("total_points", 0),
                "matches_played": score_map.get(m["user_id"], {}).get("matches_played", 0),
            }
            for m in members
        ]

        result.sort(key=lambda x: (x["total_points"], x["matches_played"]), reverse=True)
        return result

    def get_my_societies(self, user_id: int) -> list[dict]:
        """Return the societies the current user is a member of."""
        memberships = self.member_repository.find_by_user(user_id)
        result = []
        for m in memberships:
            society = self.society_repository.find_by_id(m["society_id"])
            if society is not None:
                result.append(society)
        return result


society_member_service = SocietyMemberService()
