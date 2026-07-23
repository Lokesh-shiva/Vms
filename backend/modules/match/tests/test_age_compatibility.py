import unittest

from modules.match.utils.age_compatibility import is_age_compatible


class TestAgeCompatibility(unittest.TestCase):
    def test_within_window_compatible(self):
        self.assertTrue(is_age_compatible(25, 30))
        self.assertTrue(is_age_compatible(30, 25))

    def test_exactly_at_window_boundary_compatible(self):
        self.assertTrue(is_age_compatible(25, 30))  # diff == 5

    def test_outside_window_incompatible(self):
        self.assertFalse(is_age_compatible(20, 30))  # diff == 10

    def test_unknown_age_a_always_compatible(self):
        self.assertTrue(is_age_compatible(None, 40))

    def test_unknown_age_b_always_compatible(self):
        self.assertTrue(is_age_compatible(40, None))

    def test_both_unknown_compatible(self):
        self.assertTrue(is_age_compatible(None, None))

    def test_same_age_compatible(self):
        self.assertTrue(is_age_compatible(25, 25))
