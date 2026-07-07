"""
Local-disk storage for KYC documents.

Not served as static files — access always goes through an authenticated
route (GET /api/v1/captains/{id}/kyc-document) so raw ID documents are
never publicly reachable by URL guessing.
"""

import uuid
from pathlib import Path

_UPLOAD_DIR = Path(__file__).resolve().parent.parent.parent / "uploads" / "kyc"


def save_kyc_document(captain_id: int, filename: str, content: bytes) -> str:
    """Save the file to disk and return a relative path to store on the Captain row."""
    _UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    ext = Path(filename).suffix or ".jpg"
    stored_name = f"captain_{captain_id}_{uuid.uuid4().hex}{ext}"
    dest = _UPLOAD_DIR / stored_name
    dest.write_bytes(content)
    return str(dest)


def read_kyc_document(path: str) -> bytes | None:
    """Read a previously saved document. Returns None if missing."""
    p = Path(path)
    if not p.exists() or not p.is_file():
        return None
    return p.read_bytes()
