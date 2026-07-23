import re

from fastapi import APIRouter, Depends, HTTPException

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.auth.service.auth_service import AuthService
from modules.otp.service.otp_service import otp_service, OtpDeliveryError
from modules.user.repository.user_repository import user_repository

router = APIRouter(prefix="/api/v1/auth", tags=["Auth"])

auth_service = AuthService()

_PHONE_RE = re.compile(r"^\+?[\d\-]{7,15}$")


def _success(data, message: str = "Success") -> dict:
    return {"success": True, "data": data, "message": message}


def _normalise_phone(phone: str) -> str:
    return phone.strip().replace(" ", "")


# ── OTP flow ──────────────────────────────────────────────────────────


@router.post("/send-otp", status_code=200)
def send_otp(body: dict):
    """Send a 6-digit OTP to the given phone number.

    In dev mode (OTP_DEV_MODE=true) the code is always 123456 and is
    only logged to the console — no SMS is sent.
    """
    phone = _normalise_phone(body.get("phone", ""))
    if not phone or not _PHONE_RE.match(phone):
        raise HTTPException(status_code=400, detail="A valid phone number is required.")
    try:
        otp_service.send_otp(phone)
    except NotImplementedError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except OtpDeliveryError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    return _success(None, "OTP sent.")


@router.post("/verify-otp", status_code=200)
def verify_otp(body: dict):
    """Verify OTP and return a JWT.

    If the phone is new, a stub user is created and is_new_user=true is
    returned so the app can redirect to ProfileSetup.
    """
    phone = _normalise_phone(body.get("phone", ""))
    code = str(body.get("otp", "")).strip()

    if not phone or not code:
        raise HTTPException(status_code=400, detail="phone and otp are required.")

    if not otp_service.verify_otp(phone=phone, code=code):
        raise HTTPException(status_code=401, detail="Invalid or expired OTP.")

    existing = user_repository.find_by_phone(phone)
    if existing:
        is_new_user = not existing.get("is_profile_complete", False)
        user_id = existing["id"]
        role = existing["role"]
    else:
        # First-time login — create a stub user (name filled in at ProfileSetup)
        new_user = user_repository.create({
            "name": "",
            "phone": phone,
            "password_hash": "",
            "role": "user",
        })
        is_new_user = True
        user_id = new_user["id"]
        role = new_user["role"]

    token_data = auth_service.issue_token(user_id=user_id, role=role)
    return _success(
        {"token": token_data["access_token"], "is_new_user": is_new_user},
        "OTP verified.",
    )


@router.post("/complete-profile", status_code=200)
def complete_profile(
    body: dict,
    current_user: dict = Depends(get_current_user),
):
    """Save profile details collected during onboarding (name, username, DOB, city, sports, photo)."""
    from core.database.db_connection import SessionLocal
    from modules.location.model.location_model import Location
    from modules.user.schemas.user_schema import validate_username, validate_email
    from modules.user.utils.age_utils import validate_dob

    name = str(body.get("name", "")).strip()
    if not name:
        raise HTTPException(status_code=400, detail="name is required.")

    username, username_err = validate_username(body.get("username"))
    if username_err:
        raise HTTPException(status_code=400, detail=username_err)
    existing_username = user_repository.find_by_username(username)
    if existing_username and existing_username["id"] != current_user["id"]:
        raise HTTPException(status_code=400, detail="This username is already taken.")

    email = None
    if body.get("email"):
        email, email_err = validate_email(body["email"])
        if email_err:
            raise HTTPException(status_code=400, detail=email_err)
        existing_email = user_repository.find_by_email(email)
        if existing_email and existing_email["id"] != current_user["id"]:
            raise HTTPException(status_code=400, detail="A user with this email already exists.")

    date_of_birth = body.get("date_of_birth")
    if date_of_birth:
        try:
            validate_dob(str(date_of_birth))
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc

    city = str(body.get("city", "")).strip() or None

    # Try to resolve region_id from the city string so matchmaking works immediately
    region_id = None
    if city:
        db = SessionLocal()
        try:
            loc = db.query(Location).filter(Location.name.ilike(city)).first()
            if loc:
                region_id = loc.id
        finally:
            db.close()

    update_data: dict = {
        "name": name,
        "username": username,
        "email": email,
        "date_of_birth": date_of_birth,
        "city": city,
        "sport_preferences": body.get("sport_preferences") or [],
        "profile_photo_url": body.get("profile_photo_url"),
        "is_profile_complete": True,
    }
    if region_id is not None:
        update_data["region_id"] = region_id

    updated = user_repository.update(current_user["id"], update_data)
    if not updated:
        raise HTTPException(status_code=404, detail="User not found.")
    return _success(updated, "Profile saved.")


# ── Existing endpoints ────────────────────────────────────────────────


@router.get("/me")
def get_me(current_user: dict = Depends(get_current_user)):
    """Return the full profile of the currently authenticated user."""
    return _success(current_user, "Authenticated user retrieved.")


@router.post("/register", status_code=201)
def register(request_data: dict):
    """Legacy password-based registration (admin tooling only)."""
    from modules.auth.schemas.auth_schema import RegisterSchema
    schema = RegisterSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        user = auth_service.register_user(schema.validated_data)
        return _success(user, "User registered successfully.")
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.post("/login")
def login(request_data: dict):
    """Legacy password-based login (admin tooling only)."""
    from modules.auth.schemas.auth_schema import LoginSchema
    schema = LoginSchema(request_data)
    if not schema.is_valid():
        raise HTTPException(status_code=400, detail=schema.errors)
    try:
        token_data = auth_service.login_user(schema.validated_data)
        return _success(
            {
                "access_token": token_data["access_token"],
                "token_type": token_data["token_type"],
                "role": token_data["role"],
            },
            "Login successful.",
        )
    except ValueError as exc:
        raise HTTPException(status_code=401, detail=str(exc)) from exc
