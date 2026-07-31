"""
/api/v1/sports — domain-named API for sport types (e.g. Tennis, Badminton).

These routes are a clean-named facade over the existing CartTypeService.
No business logic lives here; all calls delegate to cart_type_service.
CartType fields are already domain-neutral (name, description, is_active)
so no field renaming is required — only the route prefix and response
wrapper differ from /api/v1/cart-types.

NOTE: The IDs returned here are cart_type IDs (used by bookings, grounds,
and fee configs).  The matchmaking queue uses a separate sport_id that
references the legacy `sports` table.  These are currently two parallel
sport taxonomies; they share the same names but different integer IDs.
"""

from fastapi import APIRouter, Depends, File, HTTPException, Response, UploadFile

from core.storage.media_storage import read_media, save_media
from modules.auth.dependencies.auth_dependencies import require_admin
from modules.cart_type.service.cart_type_service import CartTypeService
from modules.cart_type.schemas.sport_schema import (
    CreateSportSchema,
    UpdateSportSchema,
    _to_sport,
)


router = APIRouter(prefix="/api/v1/sports", tags=["Sports"])

_cart_type_service = CartTypeService()
_IMAGE_NAMESPACE = "sports"
_MAX_IMAGE_BYTES = 5 * 1024 * 1024


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_sport(request_data: dict):
    """
    Create a new sport type (e.g. Tennis, Badminton, Basketball).

    Field names:
      - name        : sport name (unique)
      - description : optional description
      - is_active   : whether this sport is available for booking (default true)
    """
    schema = CreateSportSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart_type = _cart_type_service.create_cart_type(schema.validated_data)
        return _success(_to_sport(cart_type), "Sport created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_sports():
    """List all sport types."""
    cart_types = _cart_type_service.list_cart_types()
    return _success([_to_sport(ct) for ct in cart_types])


@router.get("/{sport_id}")
def get_sport(sport_id: int):
    """Get a single sport type by ID."""
    cart_type = _cart_type_service.get_cart_type(sport_id)
    if not cart_type:
        raise HTTPException(status_code=404, detail="Sport not found.")
    return _success(_to_sport(cart_type))


@router.put("/{sport_id}", dependencies=[Depends(require_admin)])
def update_sport(sport_id: int, request_data: dict):
    """Update an existing sport type."""
    schema = UpdateSportSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        cart_type = _cart_type_service.update_cart_type(sport_id, schema.validated_data)
        if not cart_type:
            raise HTTPException(status_code=404, detail="Sport not found.")
        return _success(_to_sport(cart_type), "Sport updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/{sport_id}/image", dependencies=[Depends(require_admin)])
async def upload_sport_image(sport_id: int, file: UploadFile = File(...)):
    """Upload (or replace) a sport's photo. Admin only."""
    existing = _cart_type_service.get_cart_type(sport_id)
    if not existing:
        raise HTTPException(status_code=404, detail="Sport not found.")

    if not (file.content_type or "").startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image.")
    content = await file.read()
    if len(content) > _MAX_IMAGE_BYTES:
        raise HTTPException(status_code=400, detail="Image must be under 5MB.")

    save_media(_IMAGE_NAMESPACE, sport_id, file.filename or "photo.jpg", content)
    image_url = f"/api/v1/sports/{sport_id}/image"
    cart_type = _cart_type_service.update_cart_type(sport_id, {"image_url": image_url})
    return _success(_to_sport(cart_type), "Sport photo updated.")


@router.get("/{sport_id}/image")
def get_sport_image(sport_id: int):
    """Public sport photo — not sensitive, servable without auth for AsyncImage/Coil."""
    result = read_media(_IMAGE_NAMESPACE, sport_id)
    if result is None:
        raise HTTPException(status_code=404, detail="No image uploaded for this sport.")
    content, media_type = result
    return Response(content=content, media_type=media_type)


@router.delete("/{sport_id}", dependencies=[Depends(require_admin)])
def delete_sport(sport_id: int):
    """Delete a sport type by ID."""
    deleted = _cart_type_service.delete_cart_type(sport_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Sport not found.")
    return _success(None, "Sport deleted successfully.")
