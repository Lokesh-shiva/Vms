# Plan: User registration & onboarding overhaul

## Scope (per user decisions)
1. Add **username** (required) + **email** (optional, can add later) to registration.
2. **DOB** → validated, age computed, used as a **real matchmaking filter** (not just stored).
3. **City** → GPS auto-detect nearest serviceable location, user can confirm/override.
4. **Profile photo** upload during onboarding.
5. **Admin app**: Users list shows the new fields; admin can create a user directly (backend already supports this — app-side is missing).

Registration already persists to the DB correctly (stub `User` row created at OTP-verify,
completed at `complete-profile`) — not a gap, just noting it since it was raised as a concern.

---

## Part 1 — Backend: User model fields

**Files:** `backend/modules/user/model/user_model.py`, `backend/run_migrations.py`, `backend/modules/user/schemas/user_schema.py`, `backend/modules/auth/controller/auth_routes.py`

- Add `username` (`String, unique=True, nullable=True` at DB level — nullable because existing
  users predate this; enforced as required at the *application* layer for new registrations only)
  and `email` (`String, unique=True, nullable=True`, always optional) to `User`.
- Migration: `ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR UNIQUE`, same for `email`.
  Existing users get `NULL` — need a decision later on whether to force a backfill prompt, out of
  scope for this pass.
- `complete-profile` endpoint: require `username`, validate format (alphanumeric + underscore,
  3–20 chars), check uniqueness, return a clear 400 if taken. `email` optional, validated with a
  simple regex if present, uniqueness-checked if present.
- DOB: replace the free-text validation with a real check — must parse as `YYYY-MM-DD`, must
  imply age ≥ 13, must not be in the future. Reject with 400 otherwise.
- `CreateUserSchema`/`UpdateUserSchema` (admin-direct user creation/edit): add optional
  `username`/`email` fields with the same validation, so admin-created users can set them too.

## Part 2 — Backend: age-based matchmaking

**Files:** `backend/modules/match/repository/match_repository.py`, `backend/modules/match/service/match_service.py`

The active play-now flow is `match_repository.create_play_now()` / `find_waiting_in_region()` /
`match_service.join_match()` — **not** the legacy `matchmaking_service.py` (that file still exists
but is dead code since the 2026-06-16 Match-based rewrite; confirmed by grepping for its only
caller — none).

- Compute age from `date_of_birth` server-side (helper, e.g. in `user_repository` or a small
  `age_utils.py`).
- `find_waiting_in_region()`: accept the requesting user's age, filter out matches whose creator's
  age is outside a ±5-year window (mirrors how `skill_level` is already enforced in `join_match`).
- `join_match()`: add the same age-window check alongside the existing skill-level check, so a
  direct join call can't bypass what the list view already filtered.
- Users with no DOB set: exempt from the filter (treat as compatible with everyone) rather than
  hard-blocking existing users who onboarded before this feature.

## Part 3 — Backend: GPS-based nearest location

**Files:** `backend/modules/location/model/location_model.py`, `backend/modules/location/controller/location_routes.py` (or wherever `GET /locations` lives), `backend/run_migrations.py`

- Add `latitude`/`longitude` (`Float, nullable=True`) to `Location`. Migration adds the columns;
  **existing rows will have NULL coordinates until manually backfilled** — flagging this now since
  it means GPS auto-detect won't do anything useful until someone enters real lat/long for each
  serviceable area (a one-time data-entry task, not code).
- New endpoint `GET /api/v1/locations/nearest?lat=&lng=` — haversine distance against all
  `is_serviceable=true` locations with non-null coordinates, returns the closest one (or a ranked
  list of the top N, for the "sort dropdown" fallback if closest is too far / has no coords yet).

## Part 4 — Backend: profile photo upload

**Files:** `backend/core/storage/` (new, mirrors `kyc_storage.py`), `backend/modules/auth/controller/auth_routes.py`

- Reuse the KYC upload pattern: `POST /api/v1/auth/me/profile-photo` (multipart `UploadFile`),
  saved to local disk (`backend/uploads/profile_photos/`, gitignored) — same known limitation as
  KYC: **ephemeral on Render, lost on redeploy**. Flagging again since this is the second feature
  hitting that same gap; may be worth prioritizing S3/Cloudinary migration after this.
- `complete-profile` already accepts `profile_photo_url` — just need the upload endpoint to
  populate it instead of the app always sending `null`.

## Part 5 — App (Vmsuserapp): onboarding UI

**Files:** `ProfileSetupScreen.kt`, `AuthRepository.kt`, `Models.kt`, `ApiService.kt`, new permission handling

- Step 1 gets: **username** field (required, inline availability check debounced against backend),
  **email** field (optional).
- DOB: replace free-text field with a real date picker (`DatePickerDialog`/Material3
  `DatePicker`), enforce 13+ client-side too (defense in depth, not a replacement for backend check).
- City: request `ACCESS_COARSE_LOCATION` permission → call `/locations/nearest` → pre-fill the
  dropdown with the suggestion, clearly labeled ("Detected: X — not right? pick another") — user
  can still override via the existing dropdown. Graceful fallback to manual-only if permission
  denied or no coords available yet (see Part 3 caveat).
- New step (or fold into step 1): photo picker (`ActivityResultContracts.PickVisualMedia` or
  similar) → upload → show preview. Skippable, not required (photo is about UX warmth, not gating).

## Part 6 — App (Vmsadminapp): user management

**Files:** `Models.kt`, `ApiService.kt`, `UserManagementViewModel.kt` (or equivalent), `UsersScreen.kt`

- `AppUser` model: add `username`, `email`, `date_of_birth`, `age` (computed client-side or
  returned by backend — backend is simpler, add it to `User.to_dict()`), `profile_photo_url`.
- `UsersScreen.kt`: show these in the user detail/list view (at minimum username + age + photo
  thumbnail; email/DOB can be secondary/detail-view only to avoid cluttering the list).
- Wire the **already-working** `POST /api/v1/users` — add `createUser()` to `ApiService.kt` +
  `UserRepository`/ViewModel, add a "Create user" FAB/dialog to `UsersScreen.kt` (name, phone,
  role, optionally username/email) — this is the "admin can't create a user directly" gap, and
  it's UI-only since the backend route + schema already exist and are tested.

---

## Sequencing recommendation

Parts 1 (fields) → 5/6 partial (wire the new fields into onboarding + admin list) can ship as one
slice fairly quickly. Parts 2 (age matching) and 3 (GPS) are each a separate, riskier slice — 2
touches live matchmaking logic, 3 needs real lat/long data before it does anything — so I'd suggest
doing those as their own follow-up commits rather than one giant PR, consistent with how every
other feature this session shipped as an individually-committed vertical slice.

## Decisions (confirmed)
- Age window: **±5 years**, fixed (not configurable for now).
- GPS coordinates: ship the feature now; `Location` rows keep `NULL` lat/long until backfilled
  separately. Auto-detect falls back to manual picking until then — not a blocker for this slice.

## Progress
- [x] Part 1 — Backend: username/email fields + DOB validation
- [x] Part 5a — App (Vmsuserapp): username/email fields + real DOB date picker in onboarding
- [x] Part 2 — Backend: age-based matchmaking filter (±5yr, applied to open-matches listing + join_match)
- [x] Part 3 — Backend: GPS nearest-location endpoint (haversine) + Part 5b app wiring (GPS
      auto-detect + confirm card in onboarding, no new Gradle dependency — uses
      `android.location.LocationManager` directly since play-services-location wasn't already a
      dependency)
- [ ] Part 4 — Backend: profile photo upload
- [ ] Part 5c — App (Vmsuserapp): photo picker
- [ ] Part 6 — App (Vmsadminapp): user management (show new fields, wire admin-create-user)
