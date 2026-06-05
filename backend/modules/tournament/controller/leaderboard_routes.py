from fastapi import APIRouter, Depends, Query
from modules.auth.dependencies.auth_dependencies import require_user
from modules.tournament.service.tournament_standing_service import tournament_standing_service

router = APIRouter(prefix="/api/v1/leaderboard", tags=["Leaderboard"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("")
def get_global_leaderboard(
    region_id: int = Query(..., gt=0),
    sport_id: int = Query(..., gt=0),
    limit: int = Query(default=50, ge=1, le=200),
    current_user: dict = Depends(require_user),
):
    """Global area leaderboard — ranked by total tournament points for a region+sport."""
    board = tournament_standing_service.get_global_leaderboard(
        region_id=region_id, sport_id=sport_id, limit=limit
    )
    return _success(board)
