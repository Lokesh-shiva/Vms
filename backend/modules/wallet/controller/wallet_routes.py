from fastapi import APIRouter, Depends

from modules.auth.dependencies.auth_dependencies import require_user

router = APIRouter(prefix="/api/v1/wallet", tags=["Wallet"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/transactions")
def get_wallet_transactions(current_user: dict = Depends(require_user)):
    """Return wallet transaction history for the authenticated user."""
    # Wallet ledger table not yet implemented — return empty list.
    return _success([])


@router.get("/balance")
def get_wallet_balance(current_user: dict = Depends(require_user)):
    """Return wallet coin balance for the authenticated user."""
    return _success({"balance": 0})
