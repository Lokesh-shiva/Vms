from modules.society.model.society_member_model import SocietyRole
from modules.society.repository.society_member_repository import (
    SocietyMemberRepository,
    society_member_repository as _default_member_repo,
)
from modules.society.repository.society_repository import (
    SocietyRepository,
    society_repository as _default_society_repo,
)
from modules.tournament.service.tournament_service import TournamentService

_default_tournament_service: TournamentService = TournamentService()


class SocietyTournamentService:
    def __init__(
        self,
        society_repository: SocietyRepository | None = None,
        member_repository: SocietyMemberRepository | None = None,
        tournament_service: TournamentService | None = None,
    ) -> None:
        self.society_repository: SocietyRepository = (
            society_repository or _default_society_repo
        )
        self.member_repository: SocietyMemberRepository = (
            member_repository or _default_member_repo
        )
        self.tournament_service: TournamentService = (
            tournament_service or _default_tournament_service
        )

    def register_as_team(
        self,
        society_id: int,
        tournament_id: int,
        member_ids: list[int],
        requester_id: int,
    ) -> dict:
        # 1. Get society
        society: dict | None = self.society_repository.find_by_id(society_id)
        if society is None:
            raise ValueError("Society not found.")

        # 2. Check active
        if not society["is_active"]:
            raise ValueError("Society is not active.")

        # 3. Requester must be the OWNER
        requester_member: dict | None = self.member_repository.find_member(
            society_id, requester_id
        )
        if not requester_member or requester_member["role"] != SocietyRole.OWNER:
            raise ValueError("Only the society owner can register for tournaments.")

        # 4. Validate all member_ids are current society members
        for uid in member_ids:
            if not self.member_repository.find_member(society_id, uid):
                raise ValueError(f"User {uid} is not a member of this society.")

        # 5. Delegate to tournament_service.register
        return self.tournament_service.register(
            tournament_id=tournament_id,
            user_id=requester_id,
            team_data={
                "team_name": society["name"],
                "member_user_ids": member_ids,
            },
        )


society_tournament_service = SocietyTournamentService()
