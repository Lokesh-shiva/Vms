import requests
import time

BASE_URL = "http://localhost:8000"
REGISTER_URL = f"{BASE_URL}/api/v1/auth/register"
LOGIN_URL = f"{BASE_URL}/api/v1/auth/login"
PLAY_NOW_URL = f"{BASE_URL}/api/v1/matchmaking/play-now"
ENGINE_TRIGGER_URL = f"{BASE_URL}/api/v1/matchmaking/engine/trigger"
ARRIVE_URL_TEMPLATE = f"{BASE_URL}/api/v1/matches/{{match_id}}/arrive"
LEAVE_URL = f"{BASE_URL}/api/v1/matchmaking/leave"

TIMEOUT = 30


def test_player_arrival_with_valid_gps():
    session = requests.Session()

    # Step 1: Register new user
    unique_username = f"user_{int(time.time()*1000)}"
    unique_phone = f"123456{int(time.time()*1000) % 10000000}"
    register_payload = {
        "username": unique_username,
        "password": "TestPass123!",
        "email": f"{unique_username}@example.com",
        "name": "Test User",
        "phone": unique_phone
    }
    r = session.post(REGISTER_URL, json=register_payload, timeout=TIMEOUT)
    assert r.status_code == 201, f"Registration failed: {r.text}"

    # Step 2: Login to get JWT token
    login_payload = {
        "username": unique_username,
        "password": "TestPass123!"
    }
    r = session.post(LOGIN_URL, json=login_payload, timeout=TIMEOUT)
    assert r.status_code == 200, f"Login failed: {r.text}"
    token = r.json().get("access_token")
    assert token, "No access_token in login response"

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    match_id = None
    try:
        # Step 3: Join matchmaking queue to create a WAITING entry
        play_now_payload = {
            "sport_id": 1,
            "skill_level": "BEGINNER"
        }
        r = session.post(PLAY_NOW_URL, headers=headers, json=play_now_payload, timeout=TIMEOUT)
        assert r.status_code == 201, f"Join matchmaking queue failed: {r.text}"
        queue_entry = r.json()
        entry_id = queue_entry.get("entry_id")
        assert entry_id, "No entry_id in matchmaking join response"

        # Step 4: Trigger matchmaking engine (simulate system user)
        # The test instruction requires system JWT - we reuse provided system token.
        system_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMiIsInJvbGUiOiJhZG1pbiIsImV4cCI6MTc3NTA0MzE5MX0.6IORBsS-zG3iCRLXqi0B-ACjmE7uZBzJbLIX-0Q0Beo"
        system_headers = {
            "Authorization": f"Bearer {system_token}",
            "Content-Type": "application/json"
        }

        r = session.post(ENGINE_TRIGGER_URL, headers=system_headers, timeout=TIMEOUT)
        assert r.status_code == 200, f"Matchmaking engine trigger failed: {r.text}"
        engine_response = r.json()
        matched_ids = engine_response.get("matched_entry_ids") or engine_response.get("entry_ids") or []
        assert isinstance(matched_ids, list), "matched_entry_ids missing or invalid"

        # Confirm we have a matched match_id for the user
        # Since we only have entry_id, find match from matched entries
        # The response might not include direct match_id, so we find by polling status
        match_id = None
        max_poll_attempts = 10
        poll_url = f"{BASE_URL}/api/v1/matchmaking/status"
        for _ in range(max_poll_attempts):
            r = session.get(poll_url, headers=headers, timeout=TIMEOUT)
            if r.status_code == 200:
                status_info = r.json()
                current_match_id = status_info.get("match_id")
                current_status = status_info.get("status")
                if current_match_id and current_status in ["MATCHED", "ARRIVED"]:
                    match_id = current_match_id
                    break
            time.sleep(1)
        assert match_id, "Failed to get match_id with status MATCHED or ARRIVED after matchmaking engine trigger"

        # Step 5: Call POST /api/v1/matches/{match_id}/arrive with GPS lat 12.0, lng 77.0
        arrive_url = ARRIVE_URL_TEMPLATE.format(match_id=match_id)
        arrive_payload = {
            "latitude": 12.0,
            "longitude": 77.0
        }
        r = session.post(arrive_url, headers=headers, json=arrive_payload, timeout=TIMEOUT)
        assert r.status_code == 200, f"Arrival failed: {r.text}"
        match_obj = r.json()
        # Validate state transitioned to ARRIVED or IN_PROGRESS
        state = match_obj.get("state") or match_obj.get("status") or match_obj.get("match_status")
        assert state in ["ARRIVED", "IN_PROGRESS"], f"Unexpected match state after arrival: {state}"

    finally:
        # Step 6: Cleanup user queue entry and leave
        try:
            r = session.delete(LEAVE_URL, headers=headers, timeout=TIMEOUT)
            # 200 or 400 (if no active queue) acceptable for cleanup
            assert r.status_code in [200, 400]
        except Exception:
            pass


test_player_arrival_with_valid_gps()
