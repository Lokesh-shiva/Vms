from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, DateTime, UniqueConstraint

from core.database.db_connection import Base


class Match(Base):
    """
    SQLAlchemy model for the 'matches' table.

    A Match represents a sports game session created by a user.
    The system assigns an available ground (cart) automatically.

    Status lifecycle:
        OPEN       → players can join
        FULL       → max_players reached; cart locked BUSY
        COMPLETED  → admin force-completed
        CANCELLED  → creator left or admin cancelled; cart freed

    Cart is only set to BUSY when status transitions to FULL.
    """

    __tablename__ = "matches"

    VALID_STATUSES = {"OPEN", "FULL", "COMPLETED", "CANCELLED"}
    VALID_SKILL_LEVELS = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}

    id = Column(Integer, primary_key=True, autoincrement=True)
    created_by = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    region_id = Column(Integer, ForeignKey("locations.id"), nullable=False, index=True)
    cart_type_id = Column(Integer, ForeignKey("cart_types.id"), nullable=False, index=True)
    cart_id = Column(Integer, ForeignKey("carts.id"), nullable=True, index=True)
    timeslot_id = Column(Integer, ForeignKey("timeslots.id"), nullable=False, index=True)
    skill_level = Column(String, nullable=True)
    max_players = Column(Integer, nullable=False)
    joined_players = Column(Integer, nullable=False, default=1)
    status = Column(String, nullable=False, default="OPEN")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "created_by": self.created_by,
            "region_id": self.region_id,
            "cart_type_id": self.cart_type_id,
            "cart_id": self.cart_id,
            "timeslot_id": self.timeslot_id,
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
    has_paid is a hook for future payment integration.
    """

    __tablename__ = "match_players"

    __table_args__ = (
        UniqueConstraint("match_id", "user_id", name="uq_match_player"),
    )

    id = Column(Integer, primary_key=True, autoincrement=True)
    match_id = Column(Integer, ForeignKey("matches.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    has_paid = Column(Boolean, nullable=False, default=False)
    joined_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "match_id": self.match_id,
            "user_id": self.user_id,
            "has_paid": self.has_paid,
            "joined_at": self.joined_at.isoformat() if self.joined_at else None,
        }

    def __repr__(self):
        return f"<MatchPlayer match_id={self.match_id} user_id={self.user_id}>"
