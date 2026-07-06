# Captain-Created Matches — Design

## Problem
`CaptainDashboardScreen.kt`'s `CreateMatchTab` renders 4 options (Open match, Society match, Tournament, Private) as no-op clickable rows. There is no backend support for a captain-initiated match creation flow. This spec defines that flow for 3 of the 4 options; the 4th (Tournament) is explicitly out of scope here.

## Scope decision: Tournament option
"Tournament" does not fit the `Match` model — tournaments are separate bracket entities (`Tournament`/`TournamentMatch`/`TournamentTeam`, organizer-created, with start/end dates and registration via `SocietyTournamentService`). Tapping "Tournament" in `CreateMatchTab` navigates to the existing tournament browse/registration flow. **No new backend work for this option.**

## Data model changes

Add to `backend/modules/match/model/match_model.py` `Match`:
- `visibility: str` — `OPEN | SOCIETY | PRIVATE`, default `OPEN`
- `society_id: int | None` — FK → `societies.id`, set only when `visibility == SOCIETY`
- `invite_code: str(8) | None` — unique, set only when `visibility == PRIVATE`; auto-generated 6-char alphanumeric, collision-checked on insert

Requires an Alembic-style migration (see `backend/run_migrations.py` pattern) adding these 3 columns + a unique index on `invite_code`.

## Captain's role in a captain-created match
The creating captain is set as `Match.captain_id` immediately at creation — no auto-assign-when-full logic (that stays specific to the play-now flow). The captain does **not** occupy a `MatchPlayer` slot; they organize/manage, players fill all `max_players` slots via join.

## Endpoints

### `POST /api/v1/matches/captain-create` (role: CAPTAIN)
```
Request: {
  sport_id: int,
  cart_type_id: int,
  region_id: int,
  max_players: int (2-22),
  skill_level: str | None,
  visibility: "OPEN" | "SOCIETY" | "PRIVATE",
  society_id: int | None   # required iff visibility == SOCIETY
}
Response: Match (includes invite_code if PRIVATE)
```
Validation:
- Caller must have an active `Captain` profile (403 otherwise).
- If `visibility == SOCIETY`: `society_id` required, and caller must be a `SocietyMember` of that society (403 otherwise).
- If `visibility == PRIVATE`: server generates `invite_code`, retrying on unique-constraint collision (409 surfaced only if retries exhausted — expected to never happen in practice with 6-char alphanumeric space).

Creates `Match(status=WAITING, captain_id=<caller's captain id>, visibility=..., society_id=..., invite_code=...)`. No `MatchPlayer` row created for the captain.

### `GET /api/v1/matches/open` (existing, modified)
Query now explicitly filters `visibility == OPEN` (today it implicitly returns all `WAITING` matches in region; this must be tightened so Society/Private matches never leak into the public browse list).

### `GET /api/v1/societies/{id}/matches` (new)
Lists `WAITING`/`MATCHED` matches with `society_id == id`. Controller enforces caller is a `SocietyMember` of `{id}` (403 otherwise).

### `POST /api/v1/matches/join-by-code` (new)
```
Request: { invite_code: str }
Response: Match
```
Looks up `Match` by `invite_code`; 404 if not found. Applies the same join logic as the existing `join_match` service method (capacity check via `max_players`/`joined_players`, penalty check via `MatchPenalty`, WAITING→MATCHED transition when full). No region/society restriction beyond possession of the code.

### `GET /api/v1/societies/mine` (new)
Returns societies where the current user has a `SocietyMember` row (any role). Used by the app's society picker.

## App-side wiring (Vmsuserapp)

- `ApiService.kt`: add `captainCreateMatch()`, `joinMatchByCode()`, `getSocietyMatches(societyId)`, `getMySocieties()`. Extend `Models.kt`: new `CaptainCreateMatchRequest`; extend `Match` with `visibility`, `societyId`, `inviteCode`.
- New `CaptainCreateMatchViewModel` (or extension of the existing captain dashboard ViewModel): holds selected visibility, society-picker state (lazy-loads `getMySocieties()` only when "Society match" is tapped), sport/region/max_players/skill_level form fields, submit state (idle/loading/success/error), and the created match result (needed to surface the invite code for Private).
- `CreateMatchTab` (`Vmsuserapp/.../ui/screens/captain/CaptainDashboardScreen.kt`) changes:
  - **Open match** → confirm sheet (sport/region/max_players/skill_level) → `captainCreateMatch(visibility=OPEN)` → navigate to match lobby/detail screen.
  - **Society match** → society picker (from `getMySocieties()`) shown before the confirm sheet → `captainCreateMatch(visibility=SOCIETY, society_id=...)`.
  - **Tournament** → navigates to existing tournament browse/registration screen. No new ViewModel state.
  - **Private** → same confirm sheet as Open → on success, prominently display the returned `invite_code` with a share action, since it's the only way for others to join.
- RBAC: tab and all four actions remain gated to `CAPTAIN` role at all four layers (backend endpoint, ViewModel, navigation, UI) per `CLAUDE.md`.

## Error handling
- 403: caller has no active Captain profile; or (Society) caller isn't a member of the chosen society; or (society matches list) caller isn't a member of the society being browsed.
- 404: join-by-code with an invite_code that doesn't exist.
- 409: join-by-code / join-open-match on a match that's already full or cancelled (reuses existing `join_match` error paths — no new error semantics).

## Out of scope
- Tournament creation/registration logic (already exists via `SocietyTournamentService` + `tournament` module; only the navigation target changes).
- Ground/timeslot/booking assignment for captain-created matches — follows the same later-assignment pattern as play-now matches (`cart_id`/`timeslot_id`/`booking_id` remain nullable at creation).
- Changes to the auto-assign-captain-when-full logic used by the play-now flow.
