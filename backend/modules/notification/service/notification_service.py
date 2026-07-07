from core.push.firebase_client import send_push
from modules.notification.repository.notification_repository import (
    NotificationRepository,
    notification_repository as _default_notification_repo,
)
from modules.notification.repository.fcm_token_repository import (
    FcmTokenRepository,
    fcm_token_repository as _default_token_repo,
)


class NotificationService:
    def __init__(self, notification_repository=None, fcm_token_repository=None):
        self.notification_repository: NotificationRepository = notification_repository or _default_notification_repo
        self.fcm_token_repository: FcmTokenRepository = fcm_token_repository or _default_token_repo

    def send(self, user_id: int, title: str, body: str, type_: str = "GENERAL", data: dict | None = None) -> dict:
        """
        Write an in-app notification row and, if the user has a registered
        device token, push it via Firebase. Push failures never block the
        in-app notification from being recorded.
        """
        record = self.notification_repository.create(user_id, title, body, type_, data)

        token_row = self.fcm_token_repository.find_by_user(user_id)
        if token_row:
            send_push(token_row["token"], title, body, data)

        return record

    def list_for_user(self, user_id: int) -> list[dict]:
        return self.notification_repository.find_by_user(user_id)

    def mark_read(self, notification_id: int, user_id: int) -> dict | None:
        return self.notification_repository.mark_read(notification_id, user_id)

    def register_token(self, user_id: int, token: str, platform: str = "android") -> dict:
        return self.fcm_token_repository.upsert(user_id, token, platform)


notification_service = NotificationService()
