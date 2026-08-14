from sqlalchemy import Column, ForeignKey, Integer, Numeric, String

from core.database.db_connection import Base


class OrderItem(Base):
    """A line item within an order — snapshots the item's name/price at
    order time so later price changes on the catalog item never alter a
    past order's total."""

    __tablename__ = "order_items"

    id = Column(Integer, primary_key=True, autoincrement=True)
    order_id = Column(Integer, ForeignKey("orders.id", ondelete="CASCADE"), nullable=False, index=True)
    item_id = Column(Integer, ForeignKey("items.id", ondelete="SET NULL"), nullable=True)
    name_snapshot = Column(String, nullable=False)
    unit_price_snapshot = Column(Numeric(10, 2), nullable=False)
    quantity = Column(Integer, nullable=False, default=1)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "order_id": self.order_id,
            "item_id": self.item_id,
            "name": self.name_snapshot,
            "unit_price": float(self.unit_price_snapshot) if self.unit_price_snapshot is not None else 0.0,
            "quantity": self.quantity,
            "subtotal": float(self.unit_price_snapshot) * self.quantity if self.unit_price_snapshot is not None else 0.0,
        }

    def __repr__(self) -> str:
        return f"<OrderItem id={self.id} order_id={self.order_id} item_id={self.item_id} qty={self.quantity}>"
