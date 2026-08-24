from core.database.db_connection import SessionLocal
from modules.trainer.model.trainer_booking_model import TrainerBooking


class TrainerBookingRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, data: dict) -> dict:
        session = self._session_factory()
        try:
            booking = TrainerBooking(
                trainer_id=data["trainer_id"],
                user_id=data["user_id"],
                session_date=data["session_date"],
                session_time=data["session_time"],
                status="PENDING_PAYMENT",
                amount=data["amount"],
                reference_code=data["reference_code"],
            )
            session.add(booking)
            session.commit()
            session.refresh(booking)
            return booking.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, booking_id: int) -> dict | None:
        session = self._session_factory()
        try:
            booking = session.query(TrainerBooking).filter(TrainerBooking.id == booking_id).first()
            return booking.to_dict() if booking else None
        finally:
            session.close()

    def find_by_user(self, user_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            rows = (
                session.query(TrainerBooking)
                .filter(TrainerBooking.user_id == user_id)
                .order_by(TrainerBooking.created_at.desc())
                .all()
            )
            return [r.to_dict() for r in rows]
        finally:
            session.close()

    def find_all(self, status: str | None = None) -> list[dict]:
        session = self._session_factory()
        try:
            query = session.query(TrainerBooking)
            if status:
                query = query.filter(TrainerBooking.status == status)
            rows = query.order_by(TrainerBooking.created_at.desc()).all()
            return [r.to_dict() for r in rows]
        finally:
            session.close()

    def update(self, booking_id: int, update_data: dict) -> dict | None:
        session = self._session_factory()
        try:
            booking = session.query(TrainerBooking).filter(TrainerBooking.id == booking_id).first()
            if not booking:
                return None
            for key, value in update_data.items():
                setattr(booking, key, value)
            session.commit()
            session.refresh(booking)
            return booking.to_dict()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()


trainer_booking_repository = TrainerBookingRepository()
