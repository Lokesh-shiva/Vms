from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, DateTime

from core.database.db_connection import Base


class OtpCode(Base):
    __tablename__ = "otp_codes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    phone = Column(String, nullable=False, index=True)
    code = Column(String(6), nullable=False)
    expires_at = Column(DateTime, nullable=False)
    used = Column(Boolean, nullable=False, default=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
