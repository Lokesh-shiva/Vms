import requests

BASE_URL = "http://localhost:8000"

def test_join_queue_without_region_should_fail():
    # Credentials for a user whose profile has no region_id set
    login_payload = {
        "phone": "+10000000002",
        "password": "TestPass123!"
    }
    timeout = 30
    # Step 1: Login to get JWT token
    login_url = f"{BASE_URL}/api/v1/auth/login"
    try:
        login_resp = requests.post(login_url, json=login_payload, timeout=timeout)
        assert login_resp.status_code == 200, f"Login failed with status {login_resp.status_code}: {login_resp.text}"
        token = login_resp.json().get("access_token")
        assert token, "No access_token found in login response"
    except requests.RequestException as e:
        assert False, f"Login request failed: {str(e)}"

    # Step 2: Attempt to join matchmaking queue (play-now) without region set in user profile
    join_url = f"{BASE_URL}/api/v1/matchmaking/play-now"
    headers = {
        "Authorization": f"Bearer {token}"
    }
    join_payload = {
        "sport_id": 1,
        "skill_level": "BEGINNER"
    }
    try:
        join_resp = requests.post(join_url, json=join_payload, headers=headers, timeout=timeout)
    except requests.RequestException as e:
        assert False, f"Join queue request failed: {str(e)}"

    assert join_resp.status_code == 400, f"Expected status 400 but got {join_resp.status_code}"
    # Validate the error message matches exactly expected string
    try:
        data = join_resp.json()
    except ValueError:
        assert False, "Response is not a valid JSON"

    expected_message = "Your account has no region set. Please update your profile."
    actual_message = data.get("message") or data.get("detail") or ""
    assert expected_message == actual_message, f"Expected error message '{expected_message}' but got '{actual_message}'"

test_join_queue_without_region_should_fail()
