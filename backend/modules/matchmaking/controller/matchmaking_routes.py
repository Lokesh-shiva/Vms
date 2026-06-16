from fastapi import APIRouter, Depends, Query
from fastapi.responses import JSONResponse
from core.database.db_connection import SessionLocal
from modules.auth.dependencies.auth_dependencies import require_user
from modules.match.repository.match_repository import match_repository
from modules.match.service.match_service import match_service
from modules.cart_type.model.cart_type_model import CartType


router = APIRouter(prefix="/api/v1/matchmaking", tags=["Matchmaking"])


# -- Response helpers ────────────────────────────────────────────────────


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


def _error(message: str, status_code: int = 400):
    return JSONResponse(
        status_code=status_code,
        content={"success": False, "data": None, "message": message},
    )


def _match_to_queue_status(match: dict, price: int = 200) -> dict:
    """Shape an enriched match dict into the QueueStatus response the app expects."""
    status = match.get("status", "WAITING")
    match_found = status in ("MATCHED", "ARRIVED", "IN_PROGRESS")
    return {
        "in_queue": True,
        "queue_position": 0,
        "players_searching": match.get("joined_players", 1),
        "estimated_wait_seconds": 0,
        "sport": match.get("sport", ""),
        "match_found": match_found,
        "match_id": match["id"] if match_found else None,
        "price": price,
    }


# -- Endpoints ──────────────────────────────────────────────────────────


@router.get("/price")
def get_price(sport: str, current_user: dict = Depends(require_user)):
    """
    Return the current dynamic price for a sport in the user's region.
    Used by the mobile app to display the price before joining the queue.
    """
    region_id = current_user.get("region_id")
    if not region_id:
        return _error("No region set on your account. Update your profile first.")

    db = SessionLocal()
    try:
        cart_type = db.query(CartType).filter(CartType.name.ilike(sport)).first()
        if not cart_type:
            return _error(f"Unknown sport: {sport}")
        from modules.pricing.service.pricing_service import PricingService
        pricing = PricingService(db).calculate_price(region_id, cart_type.id)
    finally:
        db.close()

    return _success({
        "sport": sport,
        "final_price": int(pricing["final_price"]),
        "base_price": int(pricing["base_price"]),
        "reason": pricing["reason"],
        "players_searching": pricing["queue_count"],
    })


@router.post("/play-now", status_code=201)
def play_now(request: dict, current_user: dict = Depends(require_user)):
    """
    Create an open play-now session for a sport.

    Flow:
    1. Resolve region from user's profile.
    2. Resolve sport (cart_type) from name.
    3. Check user has no existing active match.
    4. Create Match(WAITING) with creator as first MatchPlayer.
    5. Return QueueStatus shape so the app's existing polling loop works.

    Other users in the same region will see this match in GET /api/v1/matches/open
    and can join via POST /api/v1/matches/{id}/join.
    """
    region_id = current_user.get("region_id")
    if not region_id:
        return _error("No region set on your account. Please update your profile first.")

    sport_name: str = request.get("sport", "")
    if not sport_name:
        return _error("sport is required.")

    db = SessionLocal()
    try:
        cart_type = db.query(CartType).filter(CartType.name.ilike(sport_name)).first()
        if not cart_type:
            return _error(f"Unknown sport: {sport_name}. Check the sports list.")
        sport_id = cart_type.id

        from modules.pricing.service.pricing_service import PricingService
        pricing = PricingService(db).calculate_price(region_id, sport_id)
        price = int(pricing["final_price"])
    finally:
        db.close()

    # Guard: one active match at a time
    active = match_repository.find_active_by_user(current_user["id"])
    if active:
        return _error(
            "You already have an active match. Leave it before starting a new one."
        )

    try:
        match = match_repository.create_play_now(
            user_id=current_user["id"],
            region_id=region_id,
            cart_type_id=sport_id,
            max_players=2,
        )
    except Exception as e:
        return _error(str(e))

    enriched = match_repository.find_by_id_enriched(match["id"])
    data = _match_to_queue_status(enriched or match, price=price)

    return _success(data, "Session created! Looking for players in your area…")


@router.get("/status")
def queue_status(current_user: dict = Depends(require_user)):
    """
    Return the current user's active match as a QueueStatus.
    The app polls this every few seconds while on the QueueTrackerScreen.
    """
    active = match_repository.find_active_by_user(current_user["id"])
    if not active:
        return _error("No active match.", status_code=404)

    # Compute current price for the match sport + user's region
    price = 200
    region_id = current_user.get("region_id")
    sport_id = active.get("cart_type_id")
    if region_id and sport_id:
        db = SessionLocal()
        try:
            from modules.pricing.service.pricing_service import PricingService
            p = PricingService(db).calculate_price(region_id, sport_id)
            price = int(p["final_price"])
        except Exception:
            pass
        finally:
            db.close()

    data = _match_to_queue_status(active, price=price)
    return _success(data, "Status retrieved.")


@router.delete("/leave")
def leave_queue(current_user: dict = Depends(require_user)):
    """
    Leave the user's current WAITING match.
    If the creator leaves, the match is cancelled. Otherwise the player is removed.
    """
    active = match_repository.find_active_by_user(current_user["id"])
    if not active:
        return _error("No active match to leave.")

    try:
        result = match_service.leave_match(current_user["id"], active["id"])
    except ValueError as e:
        return _error(str(e))

    return _success(result, "Left the match.")
