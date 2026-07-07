from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_user
from modules.chat.service.chat_service import chat_service

router = APIRouter(prefix="/api/v1", tags=["Chat"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.get("/chat/threads")
def list_threads(current_user: dict = Depends(require_user)):
    return _success(chat_service.list_threads(current_user["id"]))


@router.get("/matches/{match_id}/messages")
def get_messages(match_id: int, current_user: dict = Depends(require_user)):
    try:
        return _success(chat_service.get_messages(match_id, current_user["id"]))
    except PermissionError as e:
        raise HTTPException(status_code=403, detail=str(e))


@router.post("/matches/{match_id}/messages", status_code=201)
def send_message(match_id: int, request_data: dict, current_user: dict = Depends(require_user)):
    try:
        message = chat_service.send_message(match_id, current_user["id"], request_data.get("body", ""))
        return _success(message, "Message sent.")
    except PermissionError as e:
        raise HTTPException(status_code=403, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
