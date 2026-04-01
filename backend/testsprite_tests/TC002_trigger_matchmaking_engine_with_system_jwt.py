import requests

BASE_URL = "http://localhost:8000"
TIMEOUT = 30
SYSTEM_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMiIsInJvbGUiOiJhZG1pbiIsImV4cCI6MTc3NTA0MzE5MX0.6IORBsS-zG3iCRLXqi0B-ACjmE7uZBzJbLIX-0Q0Beo"

def test_trigger_matchmaking_engine_with_system_jwt():
    # Use system JWT to trigger the matchmaking engine
    url = f"{BASE_URL}/api/v1/matchmaking/engine/trigger"
    headers = {
        "Authorization": f"Bearer {SYSTEM_JWT}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(url, headers=headers, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request to matchmaking engine trigger failed: {e}"

    # Assert for success status code
    assert response.status_code == 200, f"Expected status 200, got {response.status_code}"

    try:
        data = response.json()
    except ValueError:
        assert False, "Response was not valid JSON"

    # Validate response contains expected keys typical for engine run result
    # Should include number of matches created, entries updated, and list of matched entry_ids
    assert isinstance(data, dict), "Response JSON is not an object"
    assert "matches_created" in data or "matchesCreated" in data or "matches" in data, "Response missing matches_created key"
    assert "entries_updated" in data or "entriesUpdated" in data or "entries" in data, "Response missing entries_updated key"
    # The list of matched entry_ids might be under 'matched_entry_ids' or similar
    matched_keys = [k for k in data.keys() if "match" in k.lower() and ("entry_ids" in k.lower() or "entries" in k.lower())]
    assert matched_keys, "Response missing list of matched entry_ids"

test_trigger_matchmaking_engine_with_system_jwt()
