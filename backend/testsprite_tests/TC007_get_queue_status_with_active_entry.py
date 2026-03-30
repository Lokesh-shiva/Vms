import requests

BASE_URL = "http://localhost:8000"
TIMEOUT = 30

def test_get_queue_status_with_active_entry():
    phone = "+10000000001"
    password = "testpassword"
    login_url = f"{BASE_URL}/api/v1/auth/login"
    play_now_url = f"{BASE_URL}/api/v1/matchmaking/play-now"
    status_url = f"{BASE_URL}/api/v1/matchmaking/status"
    leave_url = f"{BASE_URL}/api/v1/matchmaking/leave"

    token = None
    entry_id = None

    try:
        # Login to get JWT token
        login_resp = requests.post(login_url, json={"phone": phone, "password": password}, timeout=TIMEOUT)
        assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
        token = login_resp.json().get("access_token")
        assert token, "No token received on login"

        headers = {"Authorization": f"Bearer {token}"}

        # Join queue with valid sport_id and skill_level
        join_payload = {"sport_id": 1, "skill_level": "BEGINNER"}
        join_resp = requests.post(play_now_url, headers=headers, json=join_payload, timeout=TIMEOUT)

        # If user already in queue, we can proceed; else expect 201 Created
        if join_resp.status_code == 201:
            data = join_resp.json()
            entry_id = data.get("entry_id")
            assert data.get("status") == "WAITING", "Status is not WAITING on join"
            assert "players_searching" in data, "players_searching missing in join response"
            assert "estimated_wait_seconds" in data, "estimated_wait_seconds missing in join response"
            assert "pricing" in data and isinstance(data["pricing"], dict), "pricing missing or invalid in join response"
        elif join_resp.status_code == 400:
            message = join_resp.json().get("message", "")
            assert "already in the queue" in message, "Unexpected message when joining with active entry"
        else:
            assert False, f"Unexpected status code from play-now: {join_resp.status_code} Body: {join_resp.text}"

        # Get queue status
        status_resp = requests.get(status_url, headers=headers, timeout=TIMEOUT)
        assert status_resp.status_code == 200, f"Queue status retrieval failed: {status_resp.text}"
        status_data = status_resp.json()
        data = status_data.get("data") or status_data  # accomodate if data is nested or flat
        assert data.get("status") == "WAITING", "Queue status is not WAITING"
        assert isinstance(data.get("players_searching"), int), "players_searching missing or not int"
        assert isinstance(data.get("estimated_wait_seconds"), int), "estimated_wait_seconds missing or not int"
        pricing = data.get("pricing")
        assert pricing is not None and isinstance(pricing, dict), "pricing missing or invalid in status response"
        # Additional optional fields
        assert "entry_id" in data, "entry_id missing in status response"
        assert "user_id" in data, "user_id missing in status response"
        assert "region_id" in data, "region_id missing in status response"
        assert "sport_id" in data, "sport_id missing in status response"
        assert "skill_level" in data, "skill_level missing in status response"
        assert "created_at" in data, "created_at missing in status response"

    finally:
        # Cleanup: leave queue if joined
        if token:
            headers = {"Authorization": f"Bearer {token}"}
            leave_resp = requests.delete(leave_url, headers=headers, timeout=TIMEOUT)
            # We accept 200 if there was an active entry, or 400 if none active
            assert leave_resp.status_code in (200, 400)

test_get_queue_status_with_active_entry()
