---
status: testing
phase: 08-pricing-queue-ux-perception
source: [08-01-SUMMARY.md]
started: 2026-04-02T00:00:00Z
updated: 2026-04-02T00:00:00Z
---

## Current Test

number: 1
name: Pricing reason — standard pricing
expected: |
  Call GET /api/v1/pricing/calculate with a region/sport that has low queue count
  and during off-peak hours. The response JSON contains a "reason" key with value
  "Standard pricing".
awaiting: user response

## Tests

### 1. Pricing reason — standard pricing
expected: Call GET /api/v1/pricing/calculate with low demand (queue < 5) during off-peak hours. Response JSON contains `"reason": "Standard pricing"`.
result: [pending]

### 2. Pricing reason — demand surge
expected: Call GET /api/v1/pricing/calculate when 5+ players are queued in that region/sport. Response JSON contains `"reason": "High demand in your area"`.
result: [pending]

### 3. Pricing reason — peak hours
expected: Call GET /api/v1/pricing/calculate during 5–9 PM with low queue count. Response JSON contains `"reason": "Peak hours (5–9 PM)"`.
result: [pending]

### 4. Queue status — wait message with players nearby
expected: Call GET /api/v1/matchmaking/status while WAITING with other players in queue. Response JSON contains a top-level `"wait_estimation_msg"` field like `"2 players nearby — match likely in 2 mins"`.
result: [pending]

### 5. Queue status — wait message when no players
expected: Call GET /api/v1/matchmaking/status while WAITING but queue is otherwise empty. Response JSON contains `"wait_estimation_msg": "No players nearby yet — hang tight"`.
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps
