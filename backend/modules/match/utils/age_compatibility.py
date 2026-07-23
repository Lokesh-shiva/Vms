AGE_COMPATIBILITY_WINDOW_YEARS = 5


def is_age_compatible(age_a: int | None, age_b: int | None) -> bool:
    """Two players are age-compatible if either side's age is unknown, or they're
    within AGE_COMPATIBILITY_WINDOW_YEARS of each other. Unknown-age is always
    treated as compatible so pre-existing users without a DOB aren't locked out."""
    if age_a is None or age_b is None:
        return True
    return abs(age_a - age_b) <= AGE_COMPATIBILITY_WINDOW_YEARS
