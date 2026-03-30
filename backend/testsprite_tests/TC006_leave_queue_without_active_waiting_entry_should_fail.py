import requests

BASE_URL = "http://localhost:8000"
LOGIN_URL = f"{BASE_URL}/api/v1/auth/login"
LEAVE_QUEUE_URL = f"{BASE_URL}/api/v1/matchmaking/leave"
STATUS_URL = f"{BASE_URL}/api/v1/matchmaking/status"
JOIN_QUEUE_URL = f"{BASE_URL}/api/v1/matchmaking/play-now"

# Credentials for a test user assumed without active queue entry
TEST_USER_PHONE = "+10000000004"
TEST_USER_PASSWORD = "StrongPassw0rd!"


def leave_queue_without_active_waiting_entry_should_fail():
    """
    Test DELETE /api/v1/matchmaking/leave for an authenticated user with no active WAITING queue entry.
    Expect 400 response with message 'User X has no active queue entry to leave.'
    """
    try:
        # Step 1: Login to get JWT token
        login_payload = {"phone": TEST_USER_PHONE, "password": TEST_USER_PASSWORD}
        login_resp = requests.post(LOGIN_URL, json=login_payload, timeout=30)
        assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
        token = login_resp.json().get("access_token")
        assert token, "No access_token in login response"

        headers = {"Authorization": f"Bearer {token}"}

        # Step 2: Ensure no active WAITING queue entry by checking status first
        status_resp = requests.get(STATUS_URL, headers=headers, timeout=30)
        if status_resp.status_code == 200:
            # If user has an active entry, leave it first to comply with test scenario
            leave_resp_cleanup = requests.delete(LEAVE_QUEUE_URL, headers=headers, timeout=30)
            assert leave_resp_cleanup.status_code == 200, f"Cleanup leave failed: {leave_resp_cleanup.text}"
        elif status_resp.status_code != 400:
            # If any other unexpected status code, fail test
            assert False, f"Unexpected status response: {status_resp.status_code} {status_resp.text}"

        # Step 3: Attempt to leave queue with no active WAITING entry
        leave_resp = requests.delete(LEAVE_QUEUE_URL, headers=headers, timeout=30)
        assert leave_resp.status_code == 400, f"Expected 400 but got {leave_resp.status_code}: {leave_resp.text}"

        resp_json = leave_resp.json()
        message = resp_json.get("message", "")

        assert "no active queue entry to leave" in message.lower(), f"Unexpected error message: {message}"

    except requests.RequestException as e:
        assert False, f"Request failed: {e}"


leave_queue_without_active_waiting_entry_should_fail()
