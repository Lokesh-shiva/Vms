from fastapi import APIRouter, Depends, HTTPException, Query
from modules.auth.dependencies.auth_dependencies import require_admin
from modules.item.service.item_service import ItemService
from modules.item.schemas.item_schema import CreateItemSchema, UpdateItemSchema


router = APIRouter(prefix="/api/v1/items", tags=["Items"])

item_service = ItemService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_item(request_data: dict):
    """Create a new item."""
    schema = CreateItemSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        item = item_service.create_item(schema.validated_data)
        return _success(item, "Item created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_items(cart_type_id: int | None = Query(default=None)):
    """Retrieve all items, optionally filtered by cart_type_id."""
    if cart_type_id is not None:
        items = item_service.list_items_by_cart_type(cart_type_id)
    else:
        items = item_service.list_items()
    return _success(items)


@router.get("/{item_id}")
def get_item(item_id: int):
    """Retrieve an item by ID."""
    item = item_service.get_item(item_id)
    if not item:
        raise HTTPException(status_code=404, detail="Item not found.")
    return _success(item)


@router.put("/{item_id}", dependencies=[Depends(require_admin)])
def update_item(item_id: int, request_data: dict):
    """Update an existing item."""
    schema = UpdateItemSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        item = item_service.update_item(item_id, schema.validated_data)
        if not item:
            raise HTTPException(status_code=404, detail="Item not found.")
        return _success(item, "Item updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{item_id}", dependencies=[Depends(require_admin)])
def delete_item(item_id: int):
    """Delete an item by ID."""
    deleted = item_service.delete_item(item_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Item not found.")
    return _success(None, "Item deleted successfully.")
