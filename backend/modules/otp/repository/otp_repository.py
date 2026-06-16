from datetime import datetime

from core.database.db_connection import SessionLocal
from modules.otp.model.otp_model import OtpCode


class OtpRepository:
    def create(self, phone: str, code: str, expires_at: datetime) -> OtpCode:
        with SessionLocal() as db:
            entry = OtpCode(phone=phone, code=code, expires_at=expires_at)
            db.add(entry)
            db.commit()
            db.refresh(entry)
            return entry

    def find_active(self, phone: str, code: str) -> OtpCode | None:
        """Return the most recent unused, unexpired OTP for this phone+code pair."""
        with SessionLocal() as db:
            return (
                db.query(OtpCode)
                .filter(
                    OtpCode.phone == phone,
                    OtpCode.code == code,
                    OtpCode.used == False,  # noqa: E712
                    OtpCode.expires_at > datetime.utcnow(),
                )
                .order_by(OtpCode.created_at.desc())
                .first()
            )

    def mark_used(self, otp_id: int) -> None:
        with SessionLocal() as db:
            db.query(OtpCode).filter(OtpCode.id == otp_id).update({"used": True})
            db.commit()


otp_repository = OtpRepository()
