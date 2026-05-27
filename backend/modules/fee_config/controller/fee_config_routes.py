from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_admin
from modules.fee_config.service.fee_config_service import FeeConfigService
from modules.fee_config.schemas.fee_config_schema import (
    CreateFeeConfigSchema,
    UpdateFeeConfigSchema,
)


router = APIRouter(prefix="/api/v1/fee-config", tags=["Fee Config"])

fee_config_service = FeeConfigService()


# ── Response helper ───────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


# ── Endpoints ─────────────────────────────────────────────────────────


@router.post("/create", status_code=201, dependencies=[Depends(require_admin)])
def create_fee_config(request_data: dict):
    """Create a new fee config. Admin only."""
    schema = CreateFeeConfigSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        config = fee_config_service.create_config(schema.validated_data)
        return _success(config, "Fee config created successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/{config_id}", dependencies=[Depends(require_admin)])
def update_fee_config(config_id: int, request_data: dict):
    """Update an existing fee config. Admin only."""
    schema = UpdateFeeConfigSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)

    try:
        config = fee_config_service.update_config(config_id, schema.validated_data)
        if not config:
            raise HTTPException(status_code=404, detail="Fee config not found.")
        return _success(config, "Fee config updated successfully.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get(
    "/region/{region_id}/cart-type/{cart_type_id}",
    dependencies=[Depends(require_admin)],
)
def get_fee_config_by_region_and_cart_type(region_id: int, cart_type_id: int):
    """Retrieve fee config for a region + cart type. Admin only."""
    config = fee_config_service.get_config_by_region_and_cart_type(
        region_id, cart_type_id
    )
    if not config:
        raise HTTPException(status_code=404, detail="Fee config not found.")
    return _success(config)


@router.get("/all", dependencies=[Depends(require_admin)])
def list_fee_configs():
    """Retrieve all fee configs. Admin only."""
    configs = fee_config_service.list_configs()
    return _success(configs)


@router.delete("/{config_id}", dependencies=[Depends(require_admin)])
def delete_fee_config(config_id: int):
    """Soft-delete a fee config (sets is_active = False). Admin only."""
    deleted = fee_config_service.delete_config(config_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Fee config not found.")
    return _success(None, "Fee config deactivated successfully.")
