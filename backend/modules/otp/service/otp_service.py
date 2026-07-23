import logging
import os
import random
import string
from datetime import datetime, timedelta

import requests

from modules.otp.repository.otp_repository import otp_repository

logger = logging.getLogger(__name__)

_DEV_MODE = os.getenv("OTP_DEV_MODE", "true").lower() == "true"
_EXPIRY_MINUTES = int(os.getenv("OTP_EXPIRY_MINUTES", "10"))
_DEV_CODE = "123456"

# MSG91 Flow API (template-based SMS) — required for real OTP delivery to Indian
# numbers. template_id must be a DLT-registered transactional/OTP template;
# MSG91_OTP_VAR must match the variable name used inside that template
# (MSG91 shows this when you create the template, e.g. "OTP" or "VAR1").
_MSG91_AUTH_KEY = os.getenv("MSG91_AUTH_KEY")
_MSG91_TEMPLATE_ID = os.getenv("MSG91_TEMPLATE_ID")
_MSG91_OTP_VAR = os.getenv("MSG91_OTP_VAR", "OTP")
_MSG91_FLOW_URL = "https://control.msg91.com/api/v5/flow/"
_MSG91_TIMEOUT_SECONDS = 10


class OtpDeliveryError(RuntimeError):
    """Raised when the SMS provider is configured but a send attempt fails."""


class OtpService:
    def send_otp(self, phone: str) -> None:
        """Generate and store an OTP. In dev mode, logs the code instead of sending SMS."""
        code = _DEV_CODE if _DEV_MODE else self._generate_code()
        expires_at = datetime.utcnow() + timedelta(minutes=_EXPIRY_MINUTES)
        otp_repository.create(phone=phone, code=code, expires_at=expires_at)

        if _DEV_MODE:
            logger.info("[DEV] OTP for %s: %s", phone, code)
        else:
            self._send_via_msg91(phone, code)

    def _send_via_msg91(self, phone: str, code: str) -> None:
        if not _MSG91_AUTH_KEY or not _MSG91_TEMPLATE_ID:
            raise NotImplementedError(
                "MSG91_AUTH_KEY and MSG91_TEMPLATE_ID must be set in .env to send real OTPs "
                "(see backend/.env.example). Requires a DLT-registered template for India."
            )
        payload = {
            "template_id": _MSG91_TEMPLATE_ID,
            "short_url": "0",
            "recipients": [
                {
                    "mobiles": phone.lstrip("+"),
                    _MSG91_OTP_VAR: code,
                }
            ],
        }
        headers = {"authkey": _MSG91_AUTH_KEY, "Content-Type": "application/json"}
        try:
            response = requests.post(
                _MSG91_FLOW_URL, json=payload, headers=headers, timeout=_MSG91_TIMEOUT_SECONDS
            )
            response.raise_for_status()
            body = response.json()
            if body.get("type") != "success":
                raise OtpDeliveryError(f"MSG91 rejected the request: {body}")
        except requests.RequestException as exc:
            logger.error("MSG91 send failed for %s: %s", phone, exc)
            raise OtpDeliveryError("Failed to send OTP SMS. Please try again.") from exc

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
