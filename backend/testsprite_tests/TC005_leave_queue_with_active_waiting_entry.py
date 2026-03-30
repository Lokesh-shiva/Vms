import requests

BASE_URL = "http://localhost:8000"
LOGIN_URL = f"{BASE_URL}/api/v1/auth/login"
PLAY_NOW_URL = f"{BASE_URL}/api/v1/matchmaking/play-now"
LEAVE_URL = f"{BASE_URL}/api/v1/matchmaking/leave"

TEST_USER_PHONE = "+10000000001"
TEST_USER_PASSWORD = "testpassword"
SPORT_ID = 1
SKILL_LEVEL = "BEGINNER"
TIMEOUT = 30

def test_leave_queue_with_active_waiting_entry():
    # Step 1: Authenticate and get token
    login_payload = {
        "phone": TEST_USER_PHONE,
        "password": TEST_USER_PASSWORD
    }
    login_resp = requests.post(LOGIN_URL, json=login_payload, timeout=TIMEOUT)
    assert login_resp.status_code == 200, f"Login failed with status {login_resp.status_code}, body: {login_resp.text}"
    token = login_resp.json().get("access_token")
    assert token, "No access token returned on login"

    headers = {"Authorization": f"Bearer {token}"}

    # Step 2: Create a queue entry (join the queue)
    play_now_payload = {
        "sport_id": SPORT_ID,
        "skill_level": SKILL_LEVEL
    }
    # Try to join the queue - must succeed with 201 or fail if user already in queue
    play_now_resp = requests.post(PLAY_NOW_URL, json=play_now_payload, headers=headers, timeout=TIMEOUT)
    if play_now_resp.status_code == 400:
        msg = play_now_resp.json().get("message", "")
        if "already in the queue" in msg:
            # The user already has a waiting queue entry, proceed
            pass
        else:
            assert False, f"Unexpected 400 response joining queue: {msg}"
    else:
        assert play_now_resp.status_code == 201, f"Join queue failed with status {play_now_resp.status_code}, body: {play_now_resp.text}"

    # Step 3: Call DELETE /api/v1/matchmaking/leave to leave the queue
    leave_resp = requests.delete(LEAVE_URL, headers=headers, timeout=TIMEOUT)
    assert leave_resp.status_code == 200, f"Leave queue failed with status {leave_resp.status_code}, body: {leave_resp.text}"
    resp_json = leave_resp.json()
    data = resp_json.get("data")
    message = resp_json.get("message", "")

    assert data is not None, "Response JSON missing 'data'"
    assert data.get("status") == "CANCELLED", f"Expected status CANCELLED but got {data.get('status')}"
    assert "You have left the queue." in message, f"Expected confirmation message not found, got: {message}"

test_leave_queue_with_active_waiting_entry()
