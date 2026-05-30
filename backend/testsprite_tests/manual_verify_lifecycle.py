import requests
import time

BASE_URL = "http://localhost:8000"
CRON_SECRET = "dev-secret"

USERS = [
    {"username": "testerA", "password": "Password123!", "phone": "9999990001"},
    {"username": "testerB", "password": "Password123!", "phone": "9999990002"},
]


def login(user):
    r = requests.post(f"{BASE_URL}/api/v1/auth/login", json=user)
    if r.status_code != 200:
        print(f"FAILED: Login {user['username']} -> {r.status_code} {r.text}")
        return None
    return r.json().get("access_token")


def run_test():
    print("--- Starting Phase 04 Match Lifecycle Manual Verification ---")

    tokens = [login(u) for u in USERS]
    if None in tokens:
        return

    headers = [
        {"Authorization": f"Bearer {t}", "Content-Type": "application/json"}
        for t in tokens
    ]

    print("\n[Step 1] Users joining queue...")
    payload = {"sport_id": 1, "skill_level": "BEGINNER"}

    for i, h in enumerate(headers):
        r = requests.post(
            f"{BASE_URL}/api/v1/matchmaking/play-now", headers=h, json=payload
        )
        if r.status_code != 201:
            if "already in the queue" in r.text:
                print(f"User {i} already in queue.")
            else:
                print(f"FAILED: User {i} join -> {r.status_code} {r.text}")
                return
        else:
            print(f"SUCCESS: User {i} joined queue.")

    print("\n[Step 2] Triggering matching engine...")
    r = requests.post(
        f"{BASE_URL}/engine/trigger", headers={"X-Cron-Secret": CRON_SECRET}
    )
    print(f"Engine result: {r.json()}")

    print("\n[Step 3] Polling matchmaking status for User 0...")
    match_id = None
    for i in range(10):
        r = requests.get(f"{BASE_URL}/api/v1/matchmaking/status", headers=headers[0])
        if r.status_code == 200:
            d = r.json().get("data", {})
            match_id = d.get("match_id")
            if match_id:
                print(f"SUCCESS: Match found! match_id={match_id}")
                break
        time.sleep(1)

    if not match_id:
        print("FAILED: Match not found.")
        return

    print(f"\n[Step 4] Arriving at match {match_id} (GPS Check)...")
    # Cart 1 is at 12.9716, 77.5946
    r = requests.post(
        f"{BASE_URL}/api/v1/matches/{match_id}/arrive",
        headers=headers[0],
        json={"latitude": 12.9716, "longitude": 77.5946},
    )
    if r.status_code != 200:
        print(f"FAILED: Arrival failed. {r.text}")
        return
    print(f"SUCCESS: Arrived. Status: {r.json().get('status')}")

    print(f"\n[Step 5] Finishing match {match_id}...")
    r = requests.post(
        f"{BASE_URL}/api/v1/matches/{match_id}/finish", headers=headers[0]
    )
    if r.status_code != 200:
        print(f"FAILED: Finish failed. {r.text}")
        return
    print(f"SUCCESS: Match finished. Status: {r.json().get('status')}")
    print("\n--- Phase 04 Match Lifecycle Verification PASSED ---")


if __name__ == "__main__":
    run_test()
