from datetime import datetime

from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, UniqueConstraint

from core.database.db_connection import Base


class SocietyRole:
    OWNER = "OWNER"
    MEMBER = "MEMBER"
    ALL: frozenset[str] = frozenset({OWNER, MEMBER})


class SocietyMember(Base):
    __tablename__ = "society_members"

    id = Column(Integer, primary_key=True, autoincrement=True)
    society_id = Column(
        Integer,
        ForeignKey("societies.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    role = Column(String(20), nullable=False)
    joined_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("society_id", "user_id", name="uq_society_member"),
    )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "society_id": self.society_id,
            "user_id": self.user_id,
            "role": self.role,
            "joined_at": self.joined_at.isoformat() if self.joined_at else None,
        }

    def __repr__(self) -> str:
        return (
            f"<SocietyMember id={self.id} society_id={self.society_id} "
            f"user_id={self.user_id} role={self.role!r}>"
        )
