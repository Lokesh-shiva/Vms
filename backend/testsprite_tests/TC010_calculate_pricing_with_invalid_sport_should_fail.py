import requests

BASE_URL = "http://localhost:8000"
TIMEOUT = 30


def test_calculate_pricing_with_invalid_sport_should_fail():
    url = f"{BASE_URL}/api/v1/pricing/calculate"
    payload = {"region_id": 1, "sport_id": 0}
    headers = {"Content-Type": "application/json"}
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed: {e}"

    assert response.status_code == 422, (
        f"Expected status code 422, got {response.status_code}"
    )
    json_response = response.json()
    # Validate error response contains validation error for sport_id
    errors = json_response.get("detail", [])
    assert isinstance(errors, list), "Expected 'detail' to be a list of errors"
    sport_id_error_found = any(
        ("loc" in err and "sport_id" in err["loc"]) and ("msg" in err) for err in errors
    )
    assert sport_id_error_found, "Validation error for 'sport_id' not found in response"


test_calculate_pricing_with_invalid_sport_should_fail()
