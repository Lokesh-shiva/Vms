import requests

BASE_URL = "http://localhost:8000"
TIMEOUT = 30


def test_join_queue_with_invalid_skill_level_should_fail():
    login_url = f"{BASE_URL}/api/v1/auth/login"
    play_now_url = f"{BASE_URL}/api/v1/matchmaking/play-now"
    credentials = {"phone": "+10000000001", "password": "testpassword"}

    # Authenticate user and get JWT token
    response = requests.post(login_url, json=credentials, timeout=TIMEOUT)
    assert response.status_code == 200, f"Login failed: {response.text}"
    token = (
        response.json().get("access_token")
        or response.json().get("token")
        or response.json().get("data", {}).get("token")
    )
    assert token, "No access token returned on login"

    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    payload = {"sport_id": 1, "skill_level": "INVALID"}

    response = requests.post(
        play_now_url, headers=headers, json=payload, timeout=TIMEOUT
    )
    assert response.status_code == 400, (
        f"Expected 400 Bad Request but got {response.status_code}"
    )

    json_resp = response.json()
    message = json_resp.get("message") or json_resp.get("detail") or ""
    expected_message = (
        "Invalid skill_level. Must be one of BEGINNER|INTERMEDIATE|ADVANCED."
    )
    assert expected_message in message, (
        f"Expected message '{expected_message}', but got '{message}'"
    )


test_join_queue_with_invalid_skill_level_should_fail()
