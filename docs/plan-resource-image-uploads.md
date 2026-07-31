# Plan: image upload for grounds, sports, items

## Progress
- [x] Grounds (Cart) — shared `media_storage.py` utility, migration 30, upload/serve routes,
      GroundsScreen + GroundOwnerScreen picker UI.
- [x] Sports (CartType) — migration 31, upload/serve routes added to the non-deprecated
      `/api/v1/sports` router (admin app's other sport CRUD calls still go through the deprecated
      `/api/v1/cart-types` router — not migrated, out of scope for this slice), CartTypesScreen
      picker UI.
- [ ] Items

## Scope
Real file upload (not paste-a-URL) for three resource types, mirroring the profile-photo upload
pattern already shipped this session:
1. **Grounds** (`Cart` model) — currently has **no** image field at all. Accessible to
   `SUPER_ADMIN`/`OPS_MANAGER` (Manage → Grounds) **and** `GROUND_OWNER` (My Grounds, own grounds only).
2. **Sports** (`CartType` model) — currently has **no** image field at all. Admin-only
   (`MANAGE_ROLES`).
3. **Items/snacks** (`Item` model) — already has `image_url`/`image_urls` fields, but only a
   manual paste-a-URL flow (schema validates it starts with `http(s)://`, nothing uploads a file).
   Add real upload alongside — the manual-paste field stays as a fallback for anyone who wants to
   link an external image instead.

## Backend — shared storage utility (avoid 3x duplication)
`core/storage/media_storage.py` (new) — generalizes the `profile_photo_storage.py` pattern with a
`namespace` param instead of a hardcoded "profile_photos" directory, so grounds/sports/items share
one implementation instead of three near-identical copies:
- `save_media(namespace: str, entity_id: int, filename: str, content: bytes) -> None`
- `read_media(namespace: str, entity_id: int) -> tuple[bytes, str] | None`
- `delete_media(namespace: str, entity_id: int) -> bool`

Files land at `backend/uploads/<namespace>/<namespace>_<id>.<ext>` — one active image per entity,
re-upload replaces (same "no audit trail needed" reasoning as profile photos).

`profile_photo_storage.py` is left as-is (not refactored to wrap the new generic module) — it's
already shipped, tested, and working; touching it for a refactor isn't worth the risk for this slice.

## Backend — per resource
**Grounds (Cart)**
- Migration: `ALTER TABLE carts ADD COLUMN IF NOT EXISTS image_url VARCHAR`.
- `Cart` model: expose `image_url` in `to_dict()`.
- `POST /api/v1/grounds/{id}/image` — owner (`owner_user_id == current_user.id`) or
  `SUPER_ADMIN`/`OPS_MANAGER`. Reuses the ground-ownership check pattern already enforced elsewhere
  in `cart_service`/`ground_routes` for `GROUND_OWNER` edits.
- `GET /api/v1/grounds/{id}/image` — public (same reasoning as profile photos: not sensitive,
  avoids the `?token=` workaround for `AsyncImage`).

**Sports (CartType)**
- Migration: `ALTER TABLE cart_types ADD COLUMN IF NOT EXISTS image_url VARCHAR`.
- `CartType` model: expose `image_url` in `to_dict()`.
- `POST /api/v1/sports/{id}/image` — `MANAGE_ROLES` only.
- `GET /api/v1/sports/{id}/image` — public.

**Items**
- No migration — `image_url` already exists.
- `POST /api/v1/items/{id}/image` — `MANAGE_ROLES` only. Sets `image_url` to the new serve-route
  path (same as how grounds/sports will work), overwriting whatever was there (including a
  manually-pasted external URL, if the admin previously used that path — upload always wins once
  used).
- `GET /api/v1/items/{id}/image` — public. (The existing manual `image_url` field still works
  standalone for anyone who pastes an external `http(s)://` link and never uploads — no change to
  that existing validation/behavior.)

## App (Vmsadminapp)
- `GroundsScreen.kt` (super_admin/ops_manager) + `GroundOwnerScreen.kt` (ground_owner, own grounds
  only) — both get an image picker on their ground card/edit flow, reusing the
  `GetContent("image/*")` + temp-file-multipart pattern already established for KYC/profile-photo
  uploads in both apps this session.
- `CartTypesScreen.kt` — same picker added to the sport add/edit dialog.
- `ItemsScreen.kt` — already has a manual `imageUrl` text field in its add/edit dialog; add an
  upload option alongside it (upload button that, on success, fills the same field/preview the
  manual paste already renders into — `hasImage` check already exists and works for any http(s)
  URL, so no change needed to the display logic once `image_url` points at our own serve route).

## Sequencing
Ship as one slice per resource type (3 commits), same discipline as the registration overhaul —
shared storage utility lands with the first one (grounds), then sports and items each add their own
migration/routes/UI reusing it.

## Not building this round
- Multiple images per ground/sport (Items already supports an `image_urls` array in the model but
  the upload flow being built here only sets the single primary `image_url` — matches the simpler
  single-photo pattern used for profile photos, not a full gallery).
- Any image resizing/compression server-side — same as profile photos, raw bytes stored as-is
  (5MB cap, same limit as profile photos).
