from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, DateTime, UniqueConstraint

from core.database.db_connection import Base


class Match(Base):
    """
    SQLAlchemy model for the 'matches' table.

    A Match represents a sports game session — either user-created (legacy)
    or system-created via the matchmaking queue.

    Status lifecycle (matchmaking):
        WAITING            → queued players grouped, awaiting ground
        MATCHED            → ground secured, players notified
        ARRIVED            → at least one player arrived
        IN_PROGRESS        → both players arrived, game underway
        COMPLETED          → game finished
        CANCELLED          → match cancelled before completion
        CANCELLED_NO_SHOW  → match cancelled because players failed to arrive
    """

    __tablename__ = "matches"

    VALID_STATUSES = {"WAITING", "MATCHED", "ARRIVED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "CANCELLED_NO_SHOW"}
    VALID_SKILL_LEVELS = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}

    id = Column(Integer, primary_key=True, autoincrement=True)
    created_by = Column(Integer, ForeignKey("users.id"), nullable=True, index=True)
    sport_id = Column(Integer, ForeignKey("sports.id"), nullable=True, index=True)
    region_id = Column(Integer, ForeignKey("locations.id"), nullable=False, index=True)
    cart_type_id = Column(Integer, ForeignKey("cart_types.id"), nullable=False, index=True)
    cart_id = Column(Integer, ForeignKey("carts.id"), nullable=True, index=True)
    booking_id = Column(Integer, ForeignKey("bookings.id"), nullable=True, index=True)
    skill_level = Column(String, nullable=True)
    max_players = Column(Integer, nullable=False, default=2)
    joined_players = Column(Integer, nullable=False, default=0)
    status = Column(String, nullable=False, default="WAITING")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "created_by": self.created_by,
            "sport_id": self.sport_id,
            "region_id": self.region_id,
            "cart_type_id": self.cart_type_id,
            "cart_id": self.cart_id,
            "booking_id": self.booking_id,
            "skill_level": self.skill_level,
            "max_players": self.max_players,
            "joined_players": self.joined_players,
            "status": self.status,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<Match id={self.id} status={self.status} players={self.joined_players}/{self.max_players}>"


class MatchPlayer(Base):
    """
    Tracks which users have joined a match.

    Unique constraint on (match_id, user_id) prevents double-join at DB level.
    has_arrived tracks whether the player arrived at the ground.
    """

    __tablename__ = "match_players"

    __table_args__ = (
        UniqueConstraint("match_id", "user_id", name="uq_match_player"),
    )

    id = Column(Integer, primary_key=True, autoincrement=True)
    match_id = Column(Integer, ForeignKey("matches.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    has_arrived = Column(Boolean, nullable=False, default=False)
    joined_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "match_id": self.match_id,
            "user_id": self.user_id,
            "has_arrived": self.has_arrived,
            "joined_at": self.joined_at.isoformat() if self.joined_at else None,
        }

    def __repr__(self):
        return f"<MatchPlayer match_id={self.match_id} user_id={self.user_id}>"


class MatchPenalty(Base):
    """
    Records a temporary matchmaking ban issued to a player who no-showed.

    A penalty is considered active while expires_at > now(). The matchmaking
    service checks for active penalties before allowing a user to join the queue.

    Reason values:
        "Ghosting" — player was in a MATCHED match but never arrived within 20 min.
    """

    __tablename__ = "match_penalties"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    match_id = Column(Integer, ForeignKey("matches.id"), nullable=False, index=True)
    reason = Column(String, nullable=False)
    expires_at = Column(DateTime, nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "match_id": self.match_id,
            "reason": self.reason,
            "expires_at": self.expires_at.isoformat() if self.expires_at else None,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self):
        return f"<MatchPenalty user_id={self.user_id} match_id={self.match_id} expires_at={self.expires_at}>"
