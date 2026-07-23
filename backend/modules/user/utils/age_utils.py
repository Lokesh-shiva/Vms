from datetime import date, datetime

MIN_AGE_YEARS = 13

_DOB_FORMAT = "%Y-%m-%d"


def parse_dob(dob_str: str) -> date:
    """Parse a 'YYYY-MM-DD' date-of-birth string.

    Raises:
        ValueError: if the string doesn't parse as a valid date.
    """
    return datetime.strptime(dob_str, _DOB_FORMAT).date()


def compute_age(dob_str: str | None, today: date | None = None) -> int | None:
    """Return whole-years age from a 'YYYY-MM-DD' DOB string, or None if absent/unparseable."""
    if not dob_str:
        return None
    try:
        dob = parse_dob(dob_str)
    except ValueError:
        return None
    today = today or date.today()
    years = today.year - dob.year
    if (today.month, today.day) < (dob.month, dob.day):
        years -= 1
    return years


def validate_dob(dob_str: str, today: date | None = None) -> None:
    """Validate a DOB string is parseable, not in the future, and implies age >= MIN_AGE_YEARS.

    Raises:
        ValueError: with a user-facing message if invalid.
    """
    try:
        dob = parse_dob(dob_str)
    except ValueError:
        raise ValueError("date_of_birth must be in YYYY-MM-DD format.")

    today = today or date.today()
    if dob > today:
        raise ValueError("date_of_birth cannot be in the future.")

    age = compute_age(dob_str, today)
    if age is None or age < MIN_AGE_YEARS:
        raise ValueError(f"You must be at least {MIN_AGE_YEARS} years old to register.")
