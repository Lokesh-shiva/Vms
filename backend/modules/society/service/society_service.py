from modules.society.model.society_member_model import SocietyRole
from modules.society.repository.society_member_repository import (
    SocietyMemberRepository,
    society_member_repository,
)
from modules.society.repository.society_repository import (
    SocietyRepository,
    society_repository,
)
from modules.user.model.user_model import UserRole


class SocietyService:
    def __init__(
        self,
        repository: SocietyRepository | None = None,
        member_repository: SocietyMemberRepository | None = None,
    ) -> None:
        self.repository: SocietyRepository = repository or society_repository
        self.member_repository: SocietyMemberRepository = (
            member_repository or society_member_repository
        )

    def create(self, data: dict, owner_user_id: int) -> dict:
        name: str = data.get("name", "")
        if not isinstance(name, str) or not name.strip():
            raise ValueError("Society name must be a non-empty string.")

        if data.get("region_id") is None:
            raise ValueError("region_id is required.")

        if data.get("sport_id") is None:
            raise ValueError("sport_id is required.")

        payload = dict(data)
        payload["owner_user_id"] = owner_user_id
        payload["name"] = name.strip()

        society: dict = self.repository.create(payload)
        self.member_repository.add_member(
            society["id"], owner_user_id, SocietyRole.OWNER
        )
        return society

    def get_by_id(self, society_id: int) -> dict:
        society: dict | None = self.repository.find_by_id(society_id)
        if society is None:
            raise ValueError("Society not found.")
        return society

    def list(
        self,
        region_id: int | None = None,
        sport_id: int | None = None,
    ) -> list[dict]:
        return self.repository.find_all(
            region_id=region_id,
            sport_id=sport_id,
            active_only=True,
        )

    def update(
        self,
        society_id: int,
        data: dict,
        requester_id: int,
        requester_role: str,
    ) -> dict:
        allowed_admin_roles: tuple[str, ...] = (
            UserRole.SUPER_ADMIN.value,
            UserRole.OPS_MANAGER.value,
        )
        is_admin = requester_role in allowed_admin_roles

        if not is_admin:
            membership: dict | None = self.member_repository.find_member(
                society_id, requester_id
            )
            if membership is None or membership.get("role") != SocietyRole.OWNER:
                raise ValueError("Permission denied.")

        if "max_members" in data:
            current_count: int = self.member_repository.count_members(society_id)
            if data["max_members"] < current_count:
                raise ValueError(
                    "max_members cannot be less than current member count."
                )

        result: dict | None = self.repository.update(society_id, data)
        if result is None:
            raise ValueError("Society not found.")
        return result

    def deactivate(self, society_id: int, requester_role: str) -> dict:
        allowed_roles: tuple[str, ...] = (
            UserRole.SUPER_ADMIN.value,
            UserRole.OPS_MANAGER.value,
        )
        if requester_role not in allowed_roles:
            raise ValueError("Permission denied.")

        result: dict | None = self.repository.update(society_id, {"is_active": False})
        if result is None:
            raise ValueError("Society not found.")
        return result

    def delete(self, society_id: int, requester_role: str) -> bool:
        if requester_role != UserRole.SUPER_ADMIN.value:
            raise ValueError("Permission denied.")

        deleted: bool = self.repository.delete(society_id)
        if not deleted:
            raise ValueError("Society not found.")
        return deleted


society_service = SocietyService()
