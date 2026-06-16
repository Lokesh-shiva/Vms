# Plan: Play-Now Core Model Fix

## Problem
`POST /play-now` creates a `QueueEntry` (wrong model — blind queue, no notifications, no browsing).
Real model: player creates an OPEN session → nearby users notified → others browse and join → captain auto-assigned when full.

## What's already correct (do not touch)
- `matches` + `match_players` tables — perfect for the model
- `find_active_by_user` already includes WAITING status
- `_enrich()` handles null cart/timeslot gracefully
- `QueueTrackerScreen` already polls `/status` and navigates on match_found
- `POST /api/v1/matches/{id}/join` — exists, just needs status fix
- Captain assignment via `captain_repository.find_available_captain`
- Old VMS booking flow (`POST /api/v1/matches` + OPEN + timeslot) — do NOT break

## Changes

### Backend

**match_repository.py** — Add:
- `create_play_now(user_id, region_id, cart_type_id, max_players=2)` → Match(WAITING) + MatchPlayer in one tx
- `find_waiting_in_region(region_id, sport_id=None)` → WAITING matches, newest first

**match_service.py** — Fix `join_match`:
- Accept `WAITING` in addition to `OPEN`
- When full + match was WAITING → auto-assign captain → status=MATCHED
- When full + match was OPEN → FULL as before (old VMS flow preserved)

**matchmaking_routes.py** — Rewrite three endpoints:
- `POST /play-now` → create_play_now, check for existing active match, return QueueStatus shape
- `GET /status` → find_active_by_user, shape as QueueStatus
- `DELETE /leave` → match_service.leave_match
- Keep `GET /price` from previous commit

**match_routes.py** — Add:
- `GET /open` → find_waiting_in_region for user's region, enriched

### App

**Models.kt** — Add `OpenMatch` model
**ApiService.kt** — Add `getOpenMatches()`
**OpenMatchesScreen.kt** — Wire real API, real join
**FeatureFlags.kt** — `OPEN_MATCHES = true`

## Status shapes (unchanged, app already handles these)
Join + status responses must return QueueStatus shape:
`{ in_queue, players_searching, estimated_wait_seconds, sport, match_found, match_id, price }`

## FCM
Stubbed for now — when user creates a match, nearby users will see it in OpenMatchesScreen.
Full push notification integration is a follow-up task.
