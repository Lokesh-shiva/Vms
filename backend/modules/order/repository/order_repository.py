from core.database.db_connection import SessionLocal
from modules.order.model.order_item_model import OrderItem
from modules.order.model.order_model import Order


class OrderRepository:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or SessionLocal

    def create(self, user_id: int, reference_code: str, total_amount: float, items: list[dict]) -> dict:
        """items: list of {item_id, name_snapshot, unit_price_snapshot, quantity}."""
        session = self._session_factory()
        try:
            order = Order(
                user_id=user_id,
                status="PENDING_PAYMENT",
                total_amount=total_amount,
                reference_code=reference_code,
            )
            session.add(order)
            session.flush()

            for line in items:
                session.add(OrderItem(order_id=order.id, **line))

            session.commit()
            session.refresh(order)
            return self._to_full_dict(session, order)
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def find_by_id(self, order_id: int) -> dict | None:
        session = self._session_factory()
        try:
            order = session.query(Order).filter(Order.id == order_id).first()
            return self._to_full_dict(session, order) if order else None
        finally:
            session.close()

    def find_by_user(self, user_id: int) -> list[dict]:
        session = self._session_factory()
        try:
            orders = (
                session.query(Order)
                .filter(Order.user_id == user_id)
                .order_by(Order.created_at.desc())
                .all()
            )
            return [self._to_full_dict(session, o) for o in orders]
        finally:
            session.close()

    def find_all(self, status: str | None = None) -> list[dict]:
        session = self._session_factory()
        try:
            query = session.query(Order)
            if status:
                query = query.filter(Order.status == status)
            orders = query.order_by(Order.created_at.desc()).all()
            return [self._to_full_dict(session, o) for o in orders]
        finally:
            session.close()

    def update(self, order_id: int, update_data: dict) -> dict | None:
        session = self._session_factory()
        try:
            order = session.query(Order).filter(Order.id == order_id).first()
            if not order:
                return None
            for key, value in update_data.items():
                setattr(order, key, value)
            session.commit()
            session.refresh(order)
            return self._to_full_dict(session, order)
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def _to_full_dict(self, session, order: Order) -> dict:
        data = order.to_dict()
        line_items = session.query(OrderItem).filter(OrderItem.order_id == order.id).all()
        data["items"] = [li.to_dict() for li in line_items]
        return data


order_repository = OrderRepository()
