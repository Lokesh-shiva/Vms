from fastapi import APIRouter, Depends

from modules.auth.dependencies.auth_dependencies import require_user
from modules.wallet.service.wallet_service import wallet_service

router = APIRouter(prefix="/api/v1/wallet", tags=["Wallet"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/transactions")
def get_wallet_transactions(current_user: dict = Depends(require_user)):
    """Return wallet transaction history for the authenticated user."""
    return _success(wallet_service.get_transactions(current_user["id"]))


@router.get("/balance")
def get_wallet_balance(current_user: dict = Depends(require_user)):
    """Return wallet coin balance for the authenticated user."""
    return _success({"balance": wallet_service.get_balance(current_user["id"])})
