import requests

BASE_URL = "http://localhost:8000"
LOGIN_URL = f"{BASE_URL}/api/v1/auth/login"
PLAY_NOW_URL = f"{BASE_URL}/api/v1/matchmaking/play-now"
LEAVE_URL = f"{BASE_URL}/api/v1/matchmaking/leave"
TIMEOUT = 30


def test_join_queue_with_duplicate_waiting_entry_should_fail():
    # Replace these with valid test user credentials present in the system.
    test_user_credentials = {"phone": "+10000000001", "password": "testpassword"}
    headers = {"Content-Type": "application/json"}

    # Login to get JWT token
    login_resp = requests.post(
        LOGIN_URL, json=test_user_credentials, headers=headers, timeout=TIMEOUT
    )
    assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
    token = login_resp.json().get("access_token") or login_resp.json().get("token")
    assert token, "No token received on login"

    auth_headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }

    # Prepare join queue request body
    join_data = {"sport_id": 1, "skill_level": "BEGINNER"}

    entry_id = None

    try:
        # First join attempt should succeed with 201
        first_resp = requests.post(
            PLAY_NOW_URL, json=join_data, headers=auth_headers, timeout=TIMEOUT
        )
        assert first_resp.status_code == 201, (
            f"First join_queue failed: {first_resp.text}"
        )
        first_data = first_resp.json()
        entry_id = first_data.get("entry_id")
        user_id = first_data.get("user_id")
        assert entry_id is not None, "No entry_id in first join response"
        assert user_id is not None, "No user_id in first join response"
        assert first_data.get("status") == "WAITING", "First join status is not WAITING"

        # Second join attempt while already waiting should fail with 400
        second_resp = requests.post(
            PLAY_NOW_URL, json=join_data, headers=auth_headers, timeout=TIMEOUT
        )
        assert second_resp.status_code == 400, (
            f"Duplicate join did not fail as expected: {second_resp.text}"
        )
        err_message = second_resp.json().get("message") or ""
        expected_message = f"User {user_id} is already in the queue."
        assert expected_message == err_message, (
            f"Unexpected error message: {err_message}"
        )

    finally:
        # Clean up: leave the queue to cancel the WAITING entry if exists
        leave_resp = requests.delete(LEAVE_URL, headers=auth_headers, timeout=TIMEOUT)
        # Accept both success (200) or 400 if already not waiting
        if leave_resp.status_code == 200:
            leave_message = leave_resp.json().get("message") or ""
            assert "You have left the queue." in leave_message
        elif leave_resp.status_code == 400:
            pass
        else:
            # Unexpected status code on cleanup
            raise AssertionError(
                f"Failed to clean up queue entry: {leave_resp.status_code} {leave_resp.text}"
            )


test_join_queue_with_duplicate_waiting_entry_should_fail()
