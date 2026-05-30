import requests

BASE_URL = "http://localhost:8000"
TIMEOUT = 30


def test_calculate_pricing_with_valid_region_and_sport():
    url = f"{BASE_URL}/api/v1/pricing/calculate"
    payload = {"region_id": 1, "sport_id": 1}
    headers = {"Content-Type": "application/json"}
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=TIMEOUT)
        assert response.status_code == 200, (
            f"Expected status code 200 but got {response.status_code}"
        )
        data = response.json()
        required_fields = [
            "base_price",
            "time_factor",
            "demand_factor",
            "queue_count",
            "final_price",
        ]
        for field in required_fields:
            assert field in data, f"Response JSON missing required field: {field}"
    except requests.RequestException as e:
        assert False, f"Request exception occurred: {e}"


test_calculate_pricing_with_valid_region_and_sport()
