# Plan: shop — cart + checkout (Phase 1 of 2; trainer marketplace is Phase 2)

## Context
`backend/modules/item/` already has `price`, `cart_type_id` (sport), `image_url`, `is_available`,
and a public `GET /api/v1/items` (filterable by sport, no auth required). No cart/order/payment
concept exists anywhere — every "cart" in this codebase means a ground (legacy naming), not a
shopping cart. `Payment.booking_id` is a required FK to `bookings`, so it can't be reused as-is for
item purchases.

## Backend — new `order` module
- `model/order_model.py` — `Order`: `user_id`, `status` (PENDING_PAYMENT/PAID/CANCELLED),
  `total_amount`, `payment_reference` (manual UPI ref, same pattern as existing payment flow),
  `created_at`/`updated_at`.
- `model/order_item_model.py` — `OrderItem`: `order_id`, `item_id`, `quantity`,
  `unit_price_snapshot` (price at time of order — items can change price later, order must not).
- Repositories + service: `create_order` (from a list of `{item_id, quantity}`, validates each item
  `is_available`, snapshots price, computes total), `submit_payment_reference` (manual UPI ref,
  mirrors `PaymentService.submit_manual_confirmation`), `approve_order` (admin/finance marks PAID,
  mirrors `approve_payment`), `list_my_orders`, `get_order`.
- Routes: `POST /api/v1/orders` (create, current user), `POST /api/v1/orders/{id}/submit-payment`,
  `GET /api/v1/orders/mine`, `GET /api/v1/orders/{id}`; admin: `GET /api/v1/admin/orders`,
  `POST /api/v1/admin/orders/{id}/approve`, `POST /api/v1/admin/orders/{id}/reject`.
- Migration: `orders`, `order_items` tables.
- Tests covering: order creation with price snapshot, unavailable-item rejection, payment
  submission, admin approve/reject, ownership checks (can't view/submit for someone else's order).

## App — Vmsuserapp
- `ShopScreen.kt` — item grid/list (reuse `SPORT_PHOTOS`-style sport filter chips), add-to-cart.
- Local cart state (ViewModel, in-memory — no persistence needed pre-checkout) + a cart badge/sheet.
- `CheckoutScreen.kt` — review cart, submit order, then a manual-UPI payment-reference submission
  screen (mirrors the existing booking payment flow's UX).
- `OrdersScreen.kt` (under Profile) — order history + status.
- Home screen: a "Shop" section (same card pattern as the Open Matches section added earlier).

## App — Vmsadminapp
- Orders queue screen (FINANCE/SUPPORT/OPS_MANAGER/SUPER_ADMIN) — approve/reject pending orders,
  mirrors the existing Payments approval screen.

## Not doing in this phase
Trainer marketplace (separate plan, Phase 2). Real payment gateway (project has no gateway by
design — manual UPI reference + admin approval, same as the existing matching-fee flow).
