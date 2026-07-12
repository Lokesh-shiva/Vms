from modules.dispute.repository.dispute_message_repository import (
    DisputeMessageRepository,
    dispute_message_repository as _default_message_repo,
)
from modules.dispute.repository.dispute_repository import (
    DisputeRepository,
    dispute_repository as _default_dispute_repo,
)
from modules.user.repository.user_repository import (
    UserRepository,
    user_repository as _default_user_repo,
)

_STAFF_ROLES = frozenset({"support", "ops_manager", "super_admin"})


class DisputeMessageService:
    """
    Reply thread on a support ticket. Access is scoped to the person who
    raised the ticket plus support/admin staff — nobody else can read or
    post on someone else's dispute.
    """

    def __init__(
        self,
        message_repository: DisputeMessageRepository | None = None,
        dispute_repository: DisputeRepository | None = None,
        user_repository: UserRepository | None = None,
    ):
        self.message_repository = message_repository or _default_message_repo
        self.dispute_repository = dispute_repository or _default_dispute_repo
        self.user_repository = user_repository or _default_user_repo

    def _authorize(self, dispute_id: int, user_id: int, role: str) -> dict:
        dispute = self.dispute_repository.find_by_id(dispute_id)
        if not dispute:
            raise ValueError("Dispute not found.")
        if role not in _STAFF_ROLES and dispute.get("raised_by") != user_id:
            raise PermissionError("You do not have access to this ticket.")
        return dispute

    def _attach_sender_name(self, messages: list[dict]) -> list[dict]:
        for m in messages:
            sender_id = m.get("sender_id")
            user = self.user_repository.find_by_id(sender_id) if sender_id else None
            m["sender_name"] = user["name"] if user else "Unknown"
            m["sender_role"] = user["role"] if user else None
        return messages

    def list_messages(self, dispute_id: int, user_id: int, role: str) -> list[dict]:
        self._authorize(dispute_id, user_id, role)
        return self._attach_sender_name(self.message_repository.find_by_dispute(dispute_id))

    def send_message(self, dispute_id: int, sender_id: int, role: str, body: str) -> dict:
        if not body or not body.strip():
            raise ValueError("Message body is required.")
        self._authorize(dispute_id, sender_id, role)
        message = self.message_repository.create(dispute_id, sender_id, body.strip())
        return self._attach_sender_name([message])[0]


dispute_message_service = DisputeMessageService()
