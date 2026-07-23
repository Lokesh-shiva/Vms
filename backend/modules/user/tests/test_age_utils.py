import unittest
from datetime import date

from modules.user.utils.age_utils import compute_age, validate_dob


class TestAgeUtils(unittest.TestCase):
    def test_compute_age_none_when_no_dob(self):
        self.assertIsNone(compute_age(None))
        self.assertIsNone(compute_age(""))

    def test_compute_age_none_when_unparseable(self):
        self.assertIsNone(compute_age("not-a-date"))

    def test_compute_age_exact_birthday(self):
        today = date(2026, 7, 23)
        self.assertEqual(compute_age("2000-07-23", today), 26)

    def test_compute_age_before_birthday_this_year(self):
        today = date(2026, 7, 23)
        self.assertEqual(compute_age("2000-08-01", today), 25)

    def test_compute_age_after_birthday_this_year(self):
        today = date(2026, 7, 23)
        self.assertEqual(compute_age("2000-01-01", today), 26)

    def test_validate_dob_rejects_bad_format(self):
        with self.assertRaises(ValueError):
            validate_dob("23-07-2000")

    def test_validate_dob_rejects_future_date(self):
        today = date(2026, 7, 23)
        with self.assertRaises(ValueError):
            validate_dob("2030-01-01", today)

    def test_validate_dob_rejects_under_min_age(self):
        today = date(2026, 7, 23)
        with self.assertRaises(ValueError):
            validate_dob("2020-01-01", today)

    def test_validate_dob_accepts_valid_adult(self):
        today = date(2026, 7, 23)
        validate_dob("2000-01-01", today)  # should not raise
