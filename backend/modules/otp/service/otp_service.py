import logging
import os
import random
import string
from datetime import datetime, timedelta

from modules.otp.repository.otp_repository import otp_repository

logger = logging.getLogger(__name__)

_DEV_MODE = os.getenv("OTP_DEV_MODE", "true").lower() == "true"
_EXPIRY_MINUTES = int(os.getenv("OTP_EXPIRY_MINUTES", "10"))
_DEV_CODE = "123456"


class OtpService:
    def send_otp(self, phone: str) -> None:
        """Generate and store an OTP. In dev mode, logs the code instead of sending SMS."""
        code = _DEV_CODE if _DEV_MODE else self._generate_code()
        expires_at = datetime.utcnow() + timedelta(minutes=_EXPIRY_MINUTES)
        otp_repository.create(phone=phone, code=code, expires_at=expires_at)

        if _DEV_MODE:
            logger.info("[DEV] OTP for %s: %s", phone, code)
        else:
            # TODO: integrate MSG91 here when OTP_DEV_MODE=false
            raise NotImplementedError("SMS provider not yet configured.")

    def verify_otp(self, phone: str, code: str) -> bool:
        """Return True if the code is valid. In dev mode, always accept '123456'."""
        if _DEV_MODE and code == _DEV_CODE:
            return True
        entry = otp_repository.find_active(phone=phone, code=code)
        if not entry:
            return False
        otp_repository.mark_used(entry.id)
        return True

    @staticmethod
    def _generate_code() -> str:
        return "".join(random.choices(string.digits, k=6))


otp_service = OtpService()
