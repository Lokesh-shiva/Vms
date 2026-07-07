from core.database.db_connection import SessionLocal
from modules.chat.repository.message_repository import (
    MessageRepository,
    message_repository as _default_message_repo,
)
from modules.match.model.match_model import Match, MatchPlayer
from modules.user.model.user_model import User


class ChatService:
    """
    Chat is scoped to match participants — a "thread" is just a match's
    message history. Polling-based, no websockets.
    """

    def __init__(self, message_repository: MessageRepository | None = None, session_factory=None):
        self.message_repository = message_repository or _default_message_repo
        self._session_factory = session_factory or SessionLocal

    def _is_participant(self, match_id: int, user_id: int) -> bool:
        session = self._session_factory()
        try:
            match = session.query(Match).filter(Match.id == match_id).first()
            if match is None:
                return False
            if match.created_by == user_id:
                return True
            return (
                session.query(MatchPlayer)
                .filter(MatchPlayer.match_id == match_id, MatchPlayer.user_id == user_id)
                .first()
                is not None
            )
        finally:
            session.close()

    def list_threads(self, user_id: int) -> list[dict]:
        """One thread per match the user participates in, newest activity first."""
        session = self._session_factory()
        try:
            matches = (
                session.query(Match)
                .join(MatchPlayer, MatchPlayer.match_id == Match.id)
                .filter(MatchPlayer.user_id == user_id)
                .order_by(Match.created_at.desc())
                .all()
            )
            threads: list[dict] = []
            for match in matches:
                other_player = (
                    session.query(MatchPlayer)
                    .filter(MatchPlayer.match_id == match.id, MatchPlayer.user_id != user_id)
                    .first()
                )
                other_name = "Match participant"
                if other_player:
                    other_user = session.query(User).filter(User.id == other_player.user_id).first()
                    if other_user:
                        other_name = other_user.name

                last = self.message_repository.find_last_by_match(match.id)
                threads.append({
                    "id": str(match.id),
                    "name": other_name,
                    "last_message": last["body"] if last else "",
                    "last_message_time": last["created_at"] if last else "",
                    "unread_count": 0,
                    "avatar_url": None,
                    "type": "direct",
                })
            return threads
        finally:
            session.close()

    def get_messages(self, match_id: int, user_id: int) -> list[dict]:
        if not self._is_participant(match_id, user_id):
            raise PermissionError("You are not a participant in this match's chat.")
        return self.message_repository.find_by_match(match_id)

    def send_message(self, match_id: int, sender_id: int, body: str) -> dict:
        if not body or not body.strip():
            raise ValueError("Message body is required.")
        if not self._is_participant(match_id, sender_id):
            raise PermissionError("You are not a participant in this match's chat.")

        message = self.message_repository.create(match_id, sender_id, body.strip())

        session = self._session_factory()
        try:
            recipients = (
                session.query(MatchPlayer.user_id)
                .filter(MatchPlayer.match_id == match_id, MatchPlayer.user_id != sender_id)
                .all()
            )
            sender = session.query(User).filter(User.id == sender_id).first()
        finally:
            session.close()

        from modules.notification.service.notification_service import notification_service
        for (recipient_id,) in recipients:
            notification_service.send(
                user_id=recipient_id,
                title=sender.name if sender else "New message",
                body=body.strip(),
                type_="CHAT_MESSAGE",
                data={"match_id": match_id},
            )

        return message


chat_service = ChatService()
