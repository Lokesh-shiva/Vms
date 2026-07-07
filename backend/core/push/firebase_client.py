"""
Firebase Admin SDK bootstrap — lazily initialized so tests and local runs
without a service account key still work (push sends are silently skipped).
"""

import json
import logging
import os
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, messaging

logger = logging.getLogger(__name__)

_BACKEND_DIR = Path(__file__).resolve().parent.parent.parent

_initialized = False
_available = False


def _init() -> None:
    global _initialized, _available
    if _initialized:
        return
    _initialized = True

    path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
    inline_json = os.getenv("FIREBASE_SERVICE_ACCOUNT_JSON")

    try:
        resolved_path = None
        if path:
            candidate = Path(path)
            if not candidate.is_absolute():
                candidate = _BACKEND_DIR / candidate
            if candidate.exists():
                resolved_path = str(candidate)

        if resolved_path:
            cred = credentials.Certificate(resolved_path)
        elif inline_json:
            cred = credentials.Certificate(json.loads(inline_json))
        else:
            logger.warning(
                "Firebase credentials not configured (FIREBASE_SERVICE_ACCOUNT_PATH / "
                "FIREBASE_SERVICE_ACCOUNT_JSON) — push notifications disabled."
            )
            return
        firebase_admin.initialize_app(cred)
        _available = True
        logger.info("Firebase Admin SDK initialized — push notifications enabled.")
    except Exception:
        logger.exception("Failed to initialize Firebase Admin SDK — push notifications disabled.")


def is_available() -> bool:
    _init()
    return _available


def send_push(token: str, title: str, body: str, data: dict | None = None) -> bool:
    """Send a single push notification. Returns True on success, False otherwise (never raises)."""
    _init()
    if not _available:
        return False
    try:
        message = messaging.Message(
            token=token,
            notification=messaging.Notification(title=title, body=body),
            data={k: str(v) for k, v in (data or {}).items()},
        )
        messaging.send(message)
        return True
    except Exception:
        logger.exception("Failed to send push notification to token=%s", token[:12] + "…")
        return False
