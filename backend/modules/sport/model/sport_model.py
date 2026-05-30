from sqlalchemy import Column, Integer, String, Boolean

from core.database.db_connection import Base


class Sport(Base):
    """
    Lookup table for supported sports.

    Used by QueueEntry and Match to indicate which sport is being played.
    """

    __tablename__ = "sports"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, nullable=False, unique=True, index=True)
    is_active = Column(Boolean, nullable=False, default=True)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "is_active": self.is_active,
        }

    def __repr__(self):
        return f"<Sport id={self.id} name={self.name}>"
