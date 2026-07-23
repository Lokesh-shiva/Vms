import unittest

from modules.location.utils.geo import haversine_km


class TestHaversine(unittest.TestCase):
    def test_same_point_zero_distance(self):
        self.assertAlmostEqual(haversine_km(17.6868, 83.2185, 17.6868, 83.2185), 0.0, places=5)

    def test_known_distance_vizag_to_hyderabad(self):
        # Visakhapatnam to Hyderabad is ~500km great-circle
        d = haversine_km(17.6868, 83.2185, 17.3850, 78.4867)
        self.assertGreater(d, 450)
        self.assertLess(d, 550)

    def test_symmetric(self):
        d1 = haversine_km(17.6868, 83.2185, 17.3850, 78.4867)
        d2 = haversine_km(17.3850, 78.4867, 17.6868, 83.2185)
        self.assertAlmostEqual(d1, d2, places=8)
