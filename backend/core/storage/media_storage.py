"""
Local-disk storage for resource images (grounds, sports, items, ...).

Generalizes the profile_photo_storage.py pattern with a namespace param
instead of a hardcoded directory, so callers don't each reinvent the same
save/read/delete logic. Public-read by design (not sensitive like KYC docs) —
callers expose a GET route with no auth so AsyncImage/Coil can load directly.
One active image per (namespace, entity_id): re-upload replaces.
"""

from pathlib import Path

_UPLOAD_ROOT = Path(__file__).resolve().parent.parent.parent / "uploads"

_MEDIA_TYPES = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
}


def _dir_for(namespace: str) -> Path:
    return _UPLOAD_ROOT / namespace


def save_media(namespace: str, entity_id: int, filename: str, content: bytes) -> None:
    """Save the file to disk, replacing any previous image for this entity."""
    upload_dir = _dir_for(namespace)
    upload_dir.mkdir(parents=True, exist_ok=True)
    for existing in upload_dir.glob(f"{namespace}_{entity_id}.*"):
        existing.unlink()
    ext = Path(filename).suffix.lower()
    if ext not in _MEDIA_TYPES:
        ext = ".jpg"
    dest = upload_dir / f"{namespace}_{entity_id}{ext}"
    dest.write_bytes(content)


def read_media(namespace: str, entity_id: int) -> tuple[bytes, str] | None:
    """Return (content, media_type) for the entity's current image, or None if none uploaded."""
    matches = list(_dir_for(namespace).glob(f"{namespace}_{entity_id}.*"))
    if not matches:
        return None
    path = matches[0]
    media_type = _MEDIA_TYPES.get(path.suffix.lower(), "image/jpeg")
    return path.read_bytes(), media_type


def delete_media(namespace: str, entity_id: int) -> bool:
    """Delete the entity's current image, if any. Returns True if a file was removed."""
    removed = False
    for existing in _dir_for(namespace).glob(f"{namespace}_{entity_id}.*"):
        existing.unlink()
        removed = True
    return removed
