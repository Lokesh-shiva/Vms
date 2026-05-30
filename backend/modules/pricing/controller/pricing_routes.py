from fastapi import APIRouter
from pydantic import BaseModel, Field

from core.database.db_connection import SessionLocal
from modules.pricing.service.pricing_service import PricingService


router = APIRouter(prefix="/api/v1/pricing", tags=["Pricing"])


class CalculatePricingRequest(BaseModel):
    region_id: int = Field(..., gt=0, description="ID of the region")
    sport_id: int = Field(..., gt=0, description="ID of the sport (must be > 0)")


@router.post("/calculate")
def calculate_pricing(request: CalculatePricingRequest):
    """
    Calculate dynamic pricing for a region + sport combination.

    Returns: base_price, time_factor, demand_factor, queue_count, final_price
    No auth required — pricing is public.
    """
    db = SessionLocal()
    try:
        pricing = PricingService(db).calculate_price(
            region_id=request.region_id,
            sport_id=request.sport_id,
        )
    finally:
        db.close()

    return pricing
