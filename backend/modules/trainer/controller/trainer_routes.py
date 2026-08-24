from fastapi import APIRouter, Depends, HTTPException
from modules.auth.dependencies.auth_dependencies import require_admin
from modules.trainer.service.trainer_service import trainer_service

router = APIRouter(prefix="/api/v1/trainers", tags=["Trainers"])


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_trainer(request_data: dict):
    try:
        trainer = trainer_service.create_trainer(request_data)
        return _success(trainer, "Trainer added.")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("")
def list_trainers():
    """List all trainers (active and inactive) — same convention as /api/v1/sports.
    The app filters to active-only for the public browse screen; admin needs to see
    inactive ones too to manage them."""
    return _success(trainer_service.list_trainers())


@router.get("/{trainer_id}")
def get_trainer(trainer_id: int):
    try:
        return _success(trainer_service.get_trainer(trainer_id))
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.put("/{trainer_id}", dependencies=[Depends(require_admin)])
def update_trainer(trainer_id: int, request_data: dict):
    try:
        trainer = trainer_service.update_trainer(trainer_id, request_data)
        return _success(trainer, "Trainer updated.")
    except ValueError as e:
        status_code = 404 if "not found" in str(e).lower() else 400
        raise HTTPException(status_code=status_code, detail=str(e))


@router.delete("/{trainer_id}", dependencies=[Depends(require_admin)])
def delete_trainer(trainer_id: int):
    try:
        trainer_service.delete_trainer(trainer_id)
        return _success(None, "Trainer removed.")
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
