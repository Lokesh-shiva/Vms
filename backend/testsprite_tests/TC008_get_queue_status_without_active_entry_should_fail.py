import requests

BASE_URL = "http://localhost:8000"
LOGIN_URL = f"{BASE_URL}/api/v1/auth/login"
STATUS_URL = f"{BASE_URL}/api/v1/matchmaking/status"
LEAVE_URL = f"{BASE_URL}/api/v1/matchmaking/leave"

TEST_USER_PHONE = "+10000000003"
TEST_USER_PASSWORD = "TestPassword123!"


def login(phone: str, password: str) -> str:
    resp = requests.post(
        LOGIN_URL,
        json={"phone": phone, "password": password},
        timeout=30,
    )
    resp.raise_for_status()
    data = resp.json()
    token = data.get("access_token") or data.get("token")
    if not token:
        raise ValueError("Login response missing access token")
    return token


def cancel_active_queue_entry(token: str):
    headers = {"Authorization": f"Bearer {token}"}
    resp = requests.delete(LEAVE_URL, headers=headers, timeout=30)
    # It's okay if no active entry, ignore error here as leave might fail
    # This is to ensure no active entries remain for the user before test


def test_get_queue_status_without_active_entry_should_fail():
    # 1. Login to get token
    token = login(TEST_USER_PHONE, TEST_USER_PASSWORD)
    headers = {"Authorization": f"Bearer {token}"}

    # 2. Ensure user has no active WAITING queue entry by trying to leave queue, ignore result
    try:
        cancel_active_queue_entry(token)
    except requests.HTTPError:
        pass

    # 3. Call GET /api/v1/matchmaking/status
    resp = requests.get(STATUS_URL, headers=headers, timeout=30)

    # 4. Assert that status code is 400
    assert resp.status_code == 400, (
        f"Expected status code 400, got {resp.status_code} with body {resp.text}"
    )

    # 5. Assert response message is 'User has no active queue entry.'
    resp_json = resp.json()
    message = resp_json.get("message")
    assert message == "User has no active queue entry.", (
        f"Expected message 'User has no active queue entry.', got '{message}'"
    )


test_get_queue_status_without_active_entry_should_fail()
