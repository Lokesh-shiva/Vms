# Plan — Tournament Backend Wiring (Admin) + Audit Log Expansion

**Date:** 2026-07-05
**Scope:** Per user request, only these two Phase 02 items (not Finance reporting or CSR_PARTNER screens).

## Context
Tournament backend is mostly already built (CRUD, match/fixture creation, result recording, standings, registration) — the gap is entirely on the admin app side, which only supports list/create/status-change. One backend gap: no endpoint to list who's registered for a tournament.

Audit log backend/app plumbing works but: no filtering, no pagination, and only 4 actions are logged (ROLE_CHANGE, DISPUTE_RESOLVED, REFUND, CAPTAIN_STATUS_CHANGE) — missing tournament CRUD, ground edits, user creation/deactivation, payment approval, booking cancellation.

## Part 1 — Tournament admin management

### Backend (one new endpoint)
1. `TournamentParticipantRepository.find_by_tournament(tournament_id)` — mirrors the existing `find_by_team`
2. `TournamentService` (or a small new method) — `list_registrations(tournament_id)`: combines individual participants + teams (with members), enriched with user names
3. `GET /api/v1/tournaments/{id}/registrations` route (tournament_registration_routes.py)

### Admin app
4. `Models.kt` — `TournamentMatch`, `TournamentStanding`, `TournamentRegistration` (participant/team + members)
5. `ApiService.kt` — matches (list/create/record-result), standings, registrations; wire the already-existing-but-unused `deleteTournament`
6. `TournamentRepository.kt` — wire the 5 new calls
7. New `TournamentDetailViewModel.kt` — holds selected tournament + matches/standings/registrations state
8. New `TournamentDetailScreen.kt` — tabs: Matches (schedule + enter result), Standings (table), Registrations (list). Delete action here.
9. `TournamentsScreen.kt` — tapping a card navigates to the detail screen; add "Delete" to the card's dropdown menu (uses the now-wired endpoint)
10. Navigation wiring wherever `TournamentsScreen` is routed (MainScreen.kt)

## Part 2 — Audit log expansion

### Backend
11. `AuditRepository.find_all()` — add optional filters: `action`, `actor_user_id`, `target_resource_type`, `start_date`, `end_date`; add `offset` for pagination
12. `audit_routes.py` — expose as query params
13. New `audit_service.log()` calls:
    - Tournament create/update/delete (`tournament_routes.py`)
    - Tournament match result entry (`tournament_match_routes.py`)
    - Ground create/update/delete — admin-initiated only, not owner's own is_active toggle (`ground_routes.py`)
    - User creation (`user_routes.py` `create_user`) and deactivation (currently only role changes are logged, not `is_active=False` alone)
    - Payment approval (`payment_routes.py` `/approve/{payment_id}`)
    - Booking cancellation (`booking_routes.py` `/{booking_id}/cancel`)
14. Tests for the new filter params

### Admin app
15. `ApiService.kt` — `getAuditLogs()` gains filter params
16. `AuditLogRepository.kt` / `AuditLogViewModel.kt` — filter state + params, "load more" pagination
17. `AuditLogScreen.kt` — action-type filter dropdown, date range inputs, load-more button

## Order of execution
Part 1 backend → Part 1 admin app → Part 2 backend → Part 2 admin app → pytest + review
