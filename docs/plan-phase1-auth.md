# Plan: Phase 1 — Real Auth (OTP + Registration)

Date: 2026-06-16

## Goal
Replace the mocked auth flow (fake OTP → fake profile save) with a real end-to-end flow:
Phone → OTP (dev bypass: always `123456`) → JWT → ProfileSetup saves to DB → Home

## What changes

### Backend (implement first)

**1. Extend User model** (`backend/modules/user/model/user_model.py`)
New columns:
- `date_of_birth` (String, nullable) — stored as ISO date string
- `city` (String, nullable)
- `sport_preferences` (JSON, nullable) — list of sport names
- `profile_photo_url` (String, nullable)
- `is_profile_complete` (Boolean, default False)
Add all new fields to `to_dict()`

**2. Migration** (`backend/run_migrations.py` / alembic or raw ALTER TABLE)
Add the 5 new columns to `users` table.

**3. New OTP module** (`backend/modules/otp/`)
- `model/otp_model.py` — table: `otp_codes(id, phone, code, expires_at, used)`
- `repository/otp_repository.py` — create, find_active, mark_used
- `service/otp_service.py` — generate 6-digit code, store, verify; reads `OTP_DEV_MODE` from env
- No controller — service called directly from auth routes

**4. New auth endpoints** (`backend/modules/auth/controller/auth_routes.py`)

`POST /api/v1/auth/send-otp`
- Body: `{ phone: string }`
- Creates OTP record (expires in 10 min), logs code to console in dev mode
- Returns: `{ success: true, message: "OTP sent" }`

`POST /api/v1/auth/verify-otp`
- Body: `{ phone: string, otp: string }`
- Validates OTP (or accepts `123456` if `OTP_DEV_MODE=true`)
- If phone exists → issue JWT, `is_new_user: false`
- If phone new → create stub user (phone only, name=""), issue JWT, `is_new_user: true`
- Returns: `{ token: string, is_new_user: bool }`

`POST /api/v1/auth/complete-profile`  ← requires JWT
- Body: `{ name, date_of_birth, city, sport_preferences: [], profile_photo_url? }`
- Sets `is_profile_complete = true`
- Returns updated user

**5. Update `/api/v1/auth/me`**
Return all new fields: `date_of_birth`, `city`, `sport_preferences`, `profile_photo_url`, `is_profile_complete`

**6. `.env` additions**
```
OTP_DEV_MODE=true
OTP_EXPIRY_MINUTES=10
```

**7. Register OTP table in `main.py`**
Import otp model so `Base.metadata.create_all` picks it up.

---

### App (implement after backend)

**8. `ApiService.kt`**
Remove `firebaseVerify`. Add:
- `POST api/v1/auth/send-otp` → `ApiResponse<Unit>`
- `POST api/v1/auth/verify-otp` → `ApiResponse<OtpVerifyResponse>`
- `POST api/v1/auth/complete-profile` → `ApiResponse<User>`
Update `getMe` return type to include new fields.

**9. `Models.kt`**
Add `OtpVerifyResponse(token: String, isNewUser: Boolean)`
Add new fields to `User`: `dateOfBirth`, `city`, `sportPreferences`, `profilePhotoUrl`, `isProfileComplete`

**10. `AuthRepository.kt`** (new file `data/AuthRepository.kt`)
- `sendOtp(phone)`, `verifyOtp(phone, otp)`, `completeProfile(...)` 
- On `verifyOtp` success: save token via `AuthTokenManager`, set `UserSession`

**11. `SplashScreen.kt`**
- Check for saved token via `AuthTokenManager`
- If token exists → call `getMe` → if success set `UserSession` → navigate to Home
- If no token / `getMe` fails → navigate to Phone

**12. `PhoneInputScreen.kt`**
- On "Send OTP" click → call `AuthRepository.sendOtp("+91$phone")`
- On success → navigate to OTP screen
- Show error snackbar on failure

**13. `OtpScreen.kt`**
- On 6 digits entered → call `AuthRepository.verifyOtp(phone, otp)`
- On success + `is_new_user=true` → navigate to ProfileSetup (clear back stack to Phone)
- On success + `is_new_user=false` → navigate to Home (clear full back stack)
- On error → show "Invalid OTP" message, clear boxes

**14. `ProfileSetupScreen.kt`**
- Step 1: name + city (free text, not dropdown) + date of birth picker
- Step 2: sport picker (existing chips, works fine)
- On "Start playing" → call `AuthRepository.completeProfile(...)` → navigate to Home
- On success refresh `UserSession` with updated user

## Order of implementation
1. User model migration
2. OTP module (model + repo + service)  
3. Auth routes (3 new endpoints + update /me)
4. Register in main.py + run migration
5. Models.kt (new fields + OtpVerifyResponse)
6. ApiService.kt (new endpoints)
7. AuthRepository.kt
8. SplashScreen (token check)
9. PhoneInputScreen (real send-otp)
10. OtpScreen (real verify-otp)
11. ProfileSetupScreen (real complete-profile, add DOB field)
