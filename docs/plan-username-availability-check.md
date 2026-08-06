# Plan: live username-availability check (onboarding + edit profile)

## Problem
Username-taken conflicts are currently only discovered at final submit (`complete-profile` /
`PUT /users/me`), forcing the user to resubmit blind. Should be surfaced inline, on focus-loss of
the username field, before they proceed further.

## Backend
- `GET /api/v1/auth/check-username?username=<value>` (new route, `auth_routes.py`).
  Auth-gated (`get_current_user`) — valid during onboarding (post-OTP, pre-profile-complete) since
  the JWT already exists at that point (photo upload already relies on this).
  Reuses `validate_username()` (format) + `user_repository.find_by_username()` (uniqueness,
  excluding the caller's own id — same exclusion logic as `complete_profile`).
  Response: `{"available": bool, "reason": str | None}`.
- Test: format-invalid → available=false with reason; taken by someone else → false; taken by self
  (re-checking own current username) → true; free → true.

## App — Vmsuserapp (both onboarding and edit-profile use the same field pattern)
- `ApiService.kt` — `checkUsername(username: String): ApiResponse<UsernameAvailability>`.
- `Models.kt` — `UsernameAvailability(available: Boolean, reason: String?)`.
- `AuthRepository.kt` — `checkUsername()` wrapper (same try/catch/toUserMessage pattern as
  `completeProfile`).
- `ProfileSetupScreen.kt` (onboarding) + `EditProfileScreen.kt` (edit) — on the username field's
  `onFocusChanged` losing focus (and only if non-blank + locally regex-valid), fire the check;
  show the red `reason` text under the field, same slot pattern as the existing local-regex error.
  Local regex error takes priority over the network check (don't call the network for an
  already-locally-invalid value).

## Not doing
No debounce-while-typing (checking on every keystroke) — only on focus-loss, matches the ask and
avoids hammering the endpoint.
