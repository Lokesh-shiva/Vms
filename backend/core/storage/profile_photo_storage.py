"""
Local-disk storage for user profile photos.

Unlike KYC documents, profile photos are not sensitive — they're served
publicly (GET /api/v1/users/{id}/profile-photo, no auth) so AsyncImage/Coil
can load them directly without any header trickery. One active photo per
user: re-uploading replaces the previous file rather than accumulating.
"""

from pathlib import Path

_UPLOAD_DIR = Path(__file__).resolve().parent.parent.parent / "uploads" / "profile_photos"

_MEDIA_TYPES = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
}


def save_profile_photo(user_id: int, filename: str, content: bytes) -> None:
    """Save the file to disk, replacing any previous photo for this user."""
    _UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    for existing in _UPLOAD_DIR.glob(f"user_{user_id}.*"):
        existing.unlink()
    ext = Path(filename).suffix.lower()
    if ext not in _MEDIA_TYPES:
        ext = ".jpg"
    dest = _UPLOAD_DIR / f"user_{user_id}{ext}"
    dest.write_bytes(content)


def read_profile_photo(user_id: int) -> tuple[bytes, str] | None:
    """Return (content, media_type) for the user's current photo, or None if none uploaded."""
    matches = list(_UPLOAD_DIR.glob(f"user_{user_id}.*"))
    if not matches:
        return None
    path = matches[0]
    media_type = _MEDIA_TYPES.get(path.suffix.lower(), "image/jpeg")
    return path.read_bytes(), media_type


def delete_profile_photo(user_id: int) -> bool:
    """Delete the user's current photo, if any. Returns True if a file was removed."""
    removed = False
    for existing in _UPLOAD_DIR.glob(f"user_{user_id}.*"):
        existing.unlink()
        removed = True
    return removed
