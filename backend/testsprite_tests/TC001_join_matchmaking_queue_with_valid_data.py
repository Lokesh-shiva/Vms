import requests

BASE_URL = "http://localhost:8000"
REGISTER_ENDPOINT = "/api/v1/auth/register"
LOGIN_ENDPOINT = "/api/v1/auth/login"
JOIN_QUEUE_ENDPOINT = "/api/v1/matchmaking/play-now"

TIMEOUT = 30

def test_join_matchmaking_queue_with_valid_data():
    # Step 1: Register a new user
    register_url = BASE_URL + REGISTER_ENDPOINT
    register_payload = {
        "username": "testuser_tc001",
        "password": "TestPass123!",
        "email": "testuser_tc001@example.com",
        "name": "Test User",
        "phone": "1234567890"
    }
    try:
        register_resp = requests.post(register_url, json=register_payload, timeout=TIMEOUT)
        assert register_resp.status_code == 201 or register_resp.status_code == 200, \
            f"Registration failed: {register_resp.status_code} {register_resp.text}"
    except requests.RequestException as e:
        assert False, f"Registration request failed: {e}"

    # Step 2: Log in with the registered user
    login_url = BASE_URL + LOGIN_ENDPOINT
    login_payload = {
        "username": register_payload["username"],
        "password": register_payload["password"],
        "phone": register_payload["phone"]
    }
    try:
        login_resp = requests.post(login_url, json=login_payload, timeout=TIMEOUT)
        assert login_resp.status_code == 200, f"Login failed: {login_resp.status_code} {login_resp.text}"
        login_data = login_resp.json()
        assert "access_token" in login_data, "Login response missing access_token"
        access_token = login_data["access_token"]
    except requests.RequestException as e:
        assert False, f"Login request failed: {e}"

    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

    # Step 3: Join the matchmaking queue
    join_url = BASE_URL + JOIN_QUEUE_ENDPOINT
    join_payload = {
        "sport_id": 1,
        "skill_level": "BEGINNER"
    }

    entry_id = None
    try:
        join_resp = requests.post(join_url, json=join_payload, headers=headers, timeout=TIMEOUT)
        assert join_resp.status_code == 201, f"Failed to join matchmaking queue: {join_resp.status_code} {join_resp.text}"
        join_data = join_resp.json()
        # Validate required response fields
        assert "entry_id" in join_data, "Response missing entry_id"
        assert "players_searching" in join_data, "Response missing players_searching"
        assert "estimated_wait_seconds" in join_data, "Response missing estimated_wait_seconds"
        assert "pricing" in join_data, "Response missing pricing"
        entry_id = join_data["entry_id"]
    except requests.RequestException as e:
        assert False, f"Join matchmaking request failed: {e}"

    # Cleanup: Leave the matchmaking queue
    if entry_id:
        leave_url = BASE_URL + "/api/v1/matchmaking/leave"
        try:
            leave_resp = requests.delete(leave_url, headers=headers, timeout=TIMEOUT)
            assert leave_resp.status_code == 200, f"Failed to leave matchmaking queue: {leave_resp.status_code} {leave_resp.text}"
            leave_data = leave_resp.json()
            assert "status" in leave_data and leave_data["status"] == "CANCELLED", "Queue leave did not cancel the entry"
        except requests.RequestException as e:
            # Do not fail test here but report cleanup failure
            print(f"Warning: Cleanup leave matchmaking queue request failed: {e}")

test_join_matchmaking_queue_with_valid_data()
