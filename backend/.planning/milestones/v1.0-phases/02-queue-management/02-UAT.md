---
status: complete
phase: 02-queue-management
source: TestSprite Automated Run
started: 2026-03-28T10:45:00+05:30
updated: 2026-03-28T10:55:00+05:30
---

## Current Test

[testing complete]

## Tests

### 1. TC001 join_queue_with_valid_data
expected: User can join the matchmaking queue by providing sport_id and GPS region.
result: issue
reported: "TestSprite automation failed: AssertionError: Login failed: {\"detail\":[\"'phone' is required and must be a string.\"]}"
severity: blocker

### 2. TC002 join_queue_without_region_should_fail
expected: User without region cannot join the queue.
result: issue
reported: "TestSprite automation failed: AssertionError: Login failed with status 400"
severity: blocker

### 3. TC003 join_queue_with_duplicate_waiting_entry_should_fail
expected: User already in the queue cannot join again.
result: issue
reported: "TestSprite automation failed: AssertionError: Login failed"
severity: blocker

### 4. TC004 join_queue_with_invalid_skill_level_should_fail
expected: Providing an invalid skill level fails validation.
result: issue
reported: "TestSprite automation failed: AssertionError: Login failed"
severity: blocker

### 5. TC005 leave_queue_with_active_waiting_entry
expected: User can leave the queue before a match is formed.
result: issue
reported: "TestSprite automation failed: AssertionError: Login failed"
severity: blocker

### 6. TC006 leave_queue_without_active_waiting_entry_should_fail
expected: Leaving without an active entry returns an error.
result: issue
reported: "TestSprite automation failed: AssertionError: Login failed"
severity: blocker

### 7. TC007 get_queue_status_with_active_entry
expected: System returns estimated wait time, players searching count, and dynamic price point upon querying status.
result: issue
reported: "TestSprite automation failed: AssertionError: Login failed"
severity: blocker

### 8. TC008 get_queue_status_without_active_entry_should_fail
expected: Querying status without an entry fails.
result: issue
reported: "TestSprite automation failed: HTTPError: 400 Client Error"
severity: blocker

### 9. TC009 calculate_pricing_with_valid_region_and_sport
expected: Dynamic price calculates successfully via API.
result: issue
reported: "TestSprite automation failed: Expected status code 200 but got 404"
severity: blocker

### 10. TC010 calculate_pricing_with_invalid_sport_should_fail
expected: Dynamic price calculation fails appropriately with invalid input.
result: issue
reported: "TestSprite automation failed: Expected status code 422, got 404"
severity: blocker

## Summary

total: 10
passed: 0
issues: 10
pending: 0
skipped: 0

## Gaps

- truth: "User can join the matchmaking queue by providing sport_id and GPS region."
  status: failed
  reason: "User reported: TestSprite automation failed: AssertionError: Login failed: {\"detail\":[\"'phone' is required and must be a string.\"]}"
  severity: blocker
  test: 1
  artifacts: []
  missing: []

- truth: "User without region cannot join the queue."
  status: failed
  reason: "User reported: TestSprite automation failed: AssertionError: Login failed with status 400"
  severity: blocker
  test: 2
  artifacts: []
  missing: []

- truth: "Dynamic price calculates successfully via API."
  status: failed
  reason: "User reported: TestSprite automation failed: Expected status code 200 but got 404"
  severity: blocker
  test: 9
  artifacts: []
  missing: []
