import json
from datetime import datetime

from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text

from core.database.db_connection import Base


class MatchEvent(Base):
    """
    Immutable audit log for match lifecycle events.

    Every state transition and player action writes one row here.
    Never updated — append-only.

    Event types:
        MATCH_CREATED           — match record was created
        PLAYER_JOINED           — a player joined the match
        PLAYER_LEFT             — a player left the match
        PLAYER_ARRIVED          — a player marked their arrival
        MATCH_STARTED           — all players arrived → IN_PROGRESS
        MATCH_COMPLETED         — match finished normally
        MATCH_CANCELLED         — match cancelled by creator or admin
        MATCH_CANCELLED_NO_SHOW — match cancelled by engine (no-show timeout)
        PAYMENT_CREATED         — split payment records generated
    """

    __tablename__ = "match_events"

    id = Column(Integer, primary_key=True, autoincrement=True)
    match_id = Column(Integer, ForeignKey("matches.id"), nullable=False, index=True)
    event_type = Column(String, nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
    meta = Column(Text, nullable=True)  # JSON-encoded extra context
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "match_id": self.match_id,
            "event_type": self.event_type,
            "user_id": self.user_id,
            "meta": json.loads(self.meta) if self.meta else None,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self):
        return f"<MatchEvent match_id={self.match_id} type={self.event_type} user={self.user_id}>"
