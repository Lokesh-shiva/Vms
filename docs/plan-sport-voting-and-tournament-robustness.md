# Plan: real sport-voting poll + tournament registration robustness

## Problem
1. The Vote tab in `TournamentsScreen.kt` is 100% fake — hardcoded `VOTE_DATA`, a local-only
   `myVote` variable, no backend at all. Copy claims "the most-voted sport becomes the next
   city-wide tournament" but nothing persists or is counted for real.
2. `TournamentsViewModel.register()` marks a tournament as registered in local state
   **unconditionally**, even when the API call throws — so a failed/duplicate registration still
   shows "You're registered!" to the user. No error is ever surfaced. No withdraw capability in the
   app despite the backend already supporting `DELETE /tournaments/{id}/register`.

## Backend — real voting
- New table `sport_votes`: `id, user_id FK users(id), region_id FK locations(id) NOT NULL,
  sport_name VARCHAR NOT NULL, created_at, updated_at`. Unique `(user_id, region_id)` — one active
  vote per user per region; re-voting updates the existing row (no double counting).
  Sport stored as a plain name string (not FK'd) — matches how `Tournament.sport` is already
  serialized as a string, and votes against the same admin-managed "active sports" list already
  built this session (`cart_types`, live via `/api/v1/sports`) rather than the legacy/duplicate
  `sports` lookup table tournaments' `sport_id` inconsistently joins against.
- `backend/modules/tournament/model/sport_vote_model.py`,
  `repository/sport_vote_repository.py` (`upsert_vote`, `get_results`, `get_my_vote`),
  `service/sport_vote_service.py` (validates sport is active in `cart_types`, resolves
  `region_id` from the caller's own `users.region_id`, 400 if the user has none).
- `controller/tournament_vote_routes.py`, prefix `/api/v1/tournaments`:
  - `GET /votes` → `{results: [{sport, votes}], my_vote, total_votes}` for the caller's region.
  - `POST /votes` body `{"sport": "..."}` → same shape after upsert.
  Registered in `main.py`.
- `run_migrations.py` — new migration creating `sport_votes`.
- Tests: new vote appears in results; re-vote moves the count instead of duplicating; no-region
  user gets 400; inactive/unknown sport gets 400; empty state returns `total_votes: 0`.

## App — wire Vote tab for real + registration robustness
- `Models.kt` — `SportVoteResult(sport, votes)`, `SportVotesResponse(results, myVote, totalVotes)`.
- `ApiService.kt` — `getSportVotes()`, `castSportVote(body)`, `withdrawTournament(id)` (new —
  backend route already existed, app never called it).
- `TournamentRepository.kt` — same `HttpException`/`toUserMessage` pattern used everywhere else
  this session (was using a bare swallowing `catch`); add `getVotes()`, `castVote()`, `withdraw()`.
- `TournamentsViewModel.kt` — `register()` fixed to only add to `_registered` on actual success,
  plus `registerError` state; add `withdraw()`; add `votes`/`myVote`/`votesLoading` state +
  `loadVotes()`/`castVote()`.
- `TournamentsScreen.kt` — `VoteTab()` rewritten against real data: loading/empty/error states,
  same visual treatment (progress bars, pills) but real counts and a real "cast vote" network call;
  fake countdown line removed (no real deadline concept), replaced with "your area" + total-vote
  copy.
- `TournamentDetailScreen.kt` — register CTA gets a loading spinner + error banner (reusing the
  `updateError`-banner pattern from `EditProfileScreen`); registered state gets a "Withdraw" text
  action calling the new `withdraw()`.

## Not doing
- No team-name/roster input UI (backend already accepts an empty `team_data` body for individual
  registration; team creation flow is a separate, larger feature not asked for here).
- No tournament standings/leaderboard UI on the detail screen — out of scope for this ask, backend
  route already exists separately and can be its own slice later if wanted.
- No full re-skin of either screen — both already follow the app's design system (PlixoInk hero
  cards, gradient overlays, pill badges) reasonably well; "visual" effort here goes into making the
  states (loading/error/empty) look intentional rather than inventing a new look.
