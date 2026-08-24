# Plan: Trainers/Coaches — trimmed v1

## Scope (per user decision — time-pressured, client already asked for this before)
Admin adds trainer profiles directly (no self-signup, no KYC/approval workflow). Users browse and
book a session with a free-text date/time (no calendar/availability-slot system). Payment via the
same manual-UPI-reference + admin-approval flow as the shop (`order` module) — no automated payout,
admin settles trainers manually same as captains today. No image upload in v1 (plain `image_url`
text field, matching the shop items' pattern before upload was added) — noted as a fast follow-up,
not blocking.

## Backend — new `trainer` module (mirrors `order` module's shape closely)
- `model/trainer_model.py` — `Trainer`: name, bio, specialties (comma-separated string, kept
  simple — no multi-select taxonomy for v1), rate_per_session, image_url (nullable), is_active.
- `model/trainer_booking_model.py` — `TrainerBooking`: trainer_id, user_id, session_date,
  session_time (both plain strings — no calendar validation against trainer availability in v1),
  status (`PENDING_PAYMENT → UNDER_REVIEW → CONFIRMED/REJECTED`, same transition shape as
  `Order`/`Payment`), amount (server-computed from `trainer.rate_per_session`, never trusted from
  client), reference_code, transaction_id.
- `repository/trainer_repository.py` — CRUD, mirrors `cart_type_repository.py`.
- `repository/trainer_booking_repository.py` — create/find/update, mirrors `order_repository.py`.
- `service/trainer_service.py` — admin CRUD validation (name/rate required, rate > 0).
- `service/trainer_booking_service.py` — `create_booking` (trainer must exist + be active, date not
  in the past, amount = trainer's current rate), `submit_payment` (ownership-checked),
  `approve_booking`/`reject_booking` (UNDER_REVIEW-gated).
- `controller/trainer_routes.py` — `GET /api/v1/trainers` (public browse, active only),
  `GET /{id}`, admin `POST`/`PUT`/`DELETE` gated to `OPS_MANAGER`/`SUPER_ADMIN` (same admin-catalog
  pattern as sports/grounds/items).
- `controller/trainer_booking_routes.py` — `POST /api/v1/trainer-bookings`,
  `GET /trainer-bookings/mine` (declared before `/{id}` — same ordering care as `orders`),
  `GET /{id}`, `POST /{id}/submit-payment`; admin `GET /api/v1/admin/trainer-bookings`,
  `POST /{id}/approve`, `POST /{id}/reject` — same `_ORDER_ADMIN_ROLES`-equivalent set.
- Migration: `trainers` + `trainer_bookings` tables.
- Tests mirroring `test_order_service.py`/`test_order_routes.py` coverage.

## App — Vmsuserapp (mirrors the shop's nested-graph pattern)
- `TrainersScreen` (browse/list) → `TrainerDetailScreen` (bio, rate, date/time fields, book button)
  → `TrainerBookingPaymentScreen` (manual UPI, mirrors `OrderPaymentScreen`), all inside a nested
  `"trainer_graph"` nav graph sharing one `TrainerViewModel` (booking state must survive
  navigation, same reason `shop_graph` exists).
- `TrainerBookingsScreen` — booking history, mirrors shop `OrdersScreen`, tappable to resume a
  `PENDING_PAYMENT` booking.
- Entry points: Home quick-tile ("Coaches") + Profile menu ("My trainer bookings") — both required
  up front this time, learned from the shop/standings discoverability misses.

## App — Vmsadminapp
- `TrainersScreen` — CRUD list, mirrors `CartTypesScreen`/`ItemsScreen`.
- `TrainerBookingsScreen` — approval queue, mirrors the shop `OrdersScreen` (including the
  reload-on-entry + pull-to-refresh fix already learned from that bug).
- Both under Manage, gated to the same roles as Shop Orders.

## Explicitly not doing in v1
Trainer self-signup/KYC/admin-approval-to-become-a-trainer, real scheduling calendar/availability
slots, automated payout tracking, image upload for trainer photos.
