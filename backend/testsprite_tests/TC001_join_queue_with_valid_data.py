import requests

BASE_URL = "http://localhost:8000"
LOGIN_ENDPOINT = "/api/v1/auth/login"
JOIN_QUEUE_ENDPOINT = "/api/v1/matchmaking/play-now"
LEAVE_QUEUE_ENDPOINT = "/api/v1/matchmaking/leave"

# Replace with valid test user credentials that have region_id set in profile
TEST_USER_PHONE = "+10000000001"
TEST_USER_PASSWORD = "testpassword"


def test_join_queue_with_valid_data():
    session = requests.Session()
    timeout = 30
    # Step 1: Login to get JWT token
    login_payload = {"phone": TEST_USER_PHONE, "password": TEST_USER_PASSWORD}
    login_resp = session.post(
        BASE_URL + LOGIN_ENDPOINT, json=login_payload, timeout=timeout
    )
    assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
    login_data = login_resp.json()
    token = (
        login_data.get("access_token")
        or login_data.get("token")
        or login_data.get("data", {}).get("access_token")
    )
    assert token, "JWT token not found in login response"

    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    # Helper function to leave queue to clean up
    def leave_queue():
        leave_resp = session.delete(
            BASE_URL + LEAVE_QUEUE_ENDPOINT, headers=headers, timeout=timeout
        )
        # Leave may fail if no active queue entry, ignore in cleanup
        if leave_resp.status_code not in (200, 400):
            raise Exception(
                f"Error during cleanup leave_queue: {leave_resp.status_code} {leave_resp.text}"
            )

    # Ensure no active queue entry before test
    try:
        leave_queue()
    except:
        pass  # ignore errors on initial cleanup

    join_payload = {"sport_id": 1, "skill_level": "BEGINNER"}

    try:
        # Step 2: Join matchmaking queue with valid payload
        join_resp = session.post(
            BASE_URL + JOIN_QUEUE_ENDPOINT,
            headers=headers,
            json=join_payload,
            timeout=timeout,
        )
        assert join_resp.status_code == 201, (
            f"Expected 201 Created but got {join_resp.status_code}: {join_resp.text}"
        )

        resp_data = join_resp.json()
        # Validate required fields in response
        expected_fields = [
            "entry_id",
            "user_id",
            "region_id",
            "sport_id",
            "skill_level",
            "status",
            "players_searching",
            "estimated_wait_seconds",
            "pricing",
            "created_at",
        ]
        for field in expected_fields:
            assert field in resp_data, f"Response JSON missing field '{field}'"

        # Validate values
        assert resp_data["sport_id"] == join_payload["sport_id"], (
            "sport_id mismatch in response"
        )
        assert resp_data["skill_level"] == join_payload["skill_level"], (
            "skill_level mismatch in response"
        )
        assert resp_data["status"] == "WAITING", (
            f"Expected status 'WAITING' but got '{resp_data['status']}'"
        )
        assert (
            isinstance(resp_data["players_searching"], int)
            and resp_data["players_searching"] >= 0
        ), "Invalid players_searching value"
        assert (
            isinstance(resp_data["estimated_wait_seconds"], int)
            and resp_data["estimated_wait_seconds"] >= 0
        ), "Invalid estimated_wait_seconds value"
        pricing = resp_data["pricing"]
        assert isinstance(pricing, dict), "Pricing should be a dictionary"
        # Pricing dictionary should include components: base_price, time_factor, demand_factor, queue_count, final_price
        pricing_fields = {
            "base_price",
            "time_factor",
            "demand_factor",
            "queue_count",
            "final_price",
        }
        missing_pricing_fields = pricing_fields - pricing.keys()
        assert not missing_pricing_fields, (
            f"Pricing missing fields: {missing_pricing_fields}"
        )

    finally:
        # Cleanup: leave the queue to remove the entry
        leave_queue()


test_join_queue_with_valid_data()
