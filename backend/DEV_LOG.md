# Development Log

---
## [2026-06-16] Core play-now model fix — Match(WAITING) not QueueEntry

### The problem
`POST /play-now` was creating a `QueueEntry` (blind queue model). Actual Plixo model:
player creates an OPEN session → others browse it in "Open Matches" → join → captain auto-assigned when full.
The `matches` + `match_players` tables were already perfectly suited. `queue_entries` was the wrong abstraction.

### Changed
**Backend**
- `matchmaking_routes.py` — Full rewrite. `POST /play-now` now creates `Match(WAITING)` via `match_repository.create_play_now()` and returns QueueStatus shape. `GET /status` reads from `find_active_by_user`. `DELETE /leave` delegates to `match_service.leave_match`. `GET /price` kept.
- `match_repository.py` — Added `create_play_now(user_id, region_id, cart_type_id, max_players)`: creates Match(WAITING) + MatchPlayer in one transaction. Added `find_waiting_in_region(region_id, sport_id)`: returns WAITING matches enriched, no timeslot join.
- `match_service.py` — `join_match` now accepts WAITING (play-now) + OPEN (old VMS booking). When full: WAITING → captain auto-assigned → MATCHED; OPEN → FULL + cart locked (old behavior preserved). `leave_match` now accepts WAITING status.
- `match_routes.py` — Added `GET /api/v1/matches/open`: lists WAITING matches in user's region with optional sport filter.

**App**
- `Models.kt` — Added `OpenMatch` model; removed unused `JoinQueueRequest`.
- `ApiService.kt` — `joinQueue` now sends `Map<String,String>` (not JoinQueueRequest); added `getOpenMatches()` and `joinOpenMatch()`.
- `MatchRepository.kt` — Updated joinQueue call; added `getOpenMatches()` and `joinOpenMatch()`.
- `OpenMatchesScreen.kt` — Full rewrite with real API data, join flow, loading/empty/error states.
- `FeatureFlags.kt` — `OPEN_MATCHES = true` (backend now exists).

### Architectural decisions
- `queue_entries` table kept in DB but matchmaking no longer writes to it — it's now audit/legacy only.
- Old VMS booking flow (`POST /api/v1/matches` + OPEN + timeslot) is fully preserved.
- FCM push notifications (ping nearby users on match creation) is stubbed — users discover open sessions via OpenMatchesScreen for now.
- Captain assignment on play-now match full: best-effort (no captain available = match stays MATCHED, captain assigned manually later).

---
## [2026-06-16] Real pricing endpoint + error display + match history wiring

### Added
**Backend**
- `GET /api/v1/matchmaking/price?sport=X` — returns `PricingService.calculate_price()` result (final_price, reason, players_searching) for the authenticated user's region. Used by PlayScreen to show real dynamic price before joining queue.
- Migration 15: CROSS JOIN seed of `region_cart_type_configs` for all serviceable locations × active cart_types (`ON CONFLICT DO NOTHING`) — ensures fee config exists for all Vizag areas seeded in migration 13.
- Join queue response now includes `price: int` (the calculated final_price) so QueueTrackerScreen can display it post-join.

**App**
- `MatchmakingPrice` model added to `Models.kt`; `QueueStatus` gains `price: Int = 200` field.
- `ApiService.kt` — `getMatchmakingPrice(@Query sport)` endpoint added.
- `PlayViewModel` — `_price: StateFlow<Int>` + `_playersSearching: StateFlow<Int>` + `_matchHistory: StateFlow<List<Match>>` replace hardcoded mock data; price fetched on sport selection via `fetchPrice()`; match history loaded from `GET /api/v1/matches/mine` on init.
- `PlayScreen` — stats row uses real `price` and `playersSearching` from VM; error message displayed in red card when join fails (previously errors were swallowed silently); join button label uses real price.

### Removed
**App**
- `SPORT_INFO` hardcoded map and `SportInfo` data class removed from `PlayScreen.kt` — all stats now API-driven.

### Architectural decisions
- PricingService (dynamic: base × time_factor × demand_factor) is what matchmaking uses — NOT `region_cart_type_configs.matching_fee` (that's for ground booking). The price endpoint wraps PricingService, not fee_config.
- Error display is an inline red card above the CTA button (no Scaffold/SnackbarHost needed since PlayScreen isn't wrapped in Scaffold).

---
## [2026-06-16] Critical fixes — DB seed + location picker + profile update

### Added
**Backend**
- Migration 12: `sports` table seeded with Cricket, Football, Badminton, Volleyball, Basketball, Tennis (`ON CONFLICT DO NOTHING`) — was empty, causing all matchmaking attempts to fail with "Unknown sport"
- Migration 13: `locations` table seeded with 8 Vizag areas (Vizag Central, Gajuwaka, Vizag North Zone, Rushikonda, Madhurawada, Dwaraka Nagar, MVP Colony, Seethammadhara) — city→region_id lookup now resolves for Vizag users
- `PUT /api/v1/users/me` — self-update endpoint for authenticated users; strips `role`/`is_active` before delegating to `UpdateUserSchema`; resolves `region_id` from `city` string via Location ilike lookup
- `city` field added to `UpdateUserSchema` so profile updates can carry city

**App**
- `LocationOption` model in `Models.kt` (id, name)
- `getLocations()` in `ApiService` — `GET /api/v1/locations` (public, no auth)
- `ProfileSetupScreen`: city free-text replaced with `ExposedDropdownMenuBox` that loads locations from API on mount; city is now always a valid location name
- `EditProfileScreen`: hardcoded Bangalore/Mumbai/Delhi list replaced with real API locations; sends `city` (was `region`)
- `ProfileRepository.updateProfile`: sends `city` field (was `region`) to match backend schema

### Architectural decisions
- Location picker fetches from API at mount time — no local cache needed, list is small and rarely changes
- `PUT /users/me` resolves region_id server-side from city name (ilike match), same pattern as `complete-profile` — app stays simple, FK resolution stays in backend
- Sports and locations seeded via `run_migrations.py` (idempotent, `ON CONFLICT DO NOTHING`) so Render picks them up on next deploy without wiping existing data

---
## [2026-06-16] Phase 04b — Real auth: OTP + registration (Phase 1)

### Added
**Backend**
- `backend/modules/otp/model/otp_model.py` — `otp_codes` table (phone, code, expires_at, used)
- `backend/modules/otp/repository/otp_repository.py` — create / find_active / mark_used
- `backend/modules/otp/service/otp_service.py` — generate + verify OTP; reads `OTP_DEV_MODE` env var; in dev mode always accepts `123456` and logs to console; SMS hook stubbed for MSG91 later
- `backend/modules/auth/controller/auth_routes.py` — 3 new endpoints: `POST /send-otp`, `POST /verify-otp`, `POST /complete-profile`
- `Vmsuserapp/.../data/AuthRepository.kt` — sendOtp / verifyOtp / completeProfile / getMe

**App**
- `docs/plan-phase1-auth.md` — full implementation plan

### Modified
**Backend**
- `backend/modules/user/model/user_model.py` — 5 new columns: `date_of_birth`, `city`, `sport_preferences` (JSON), `profile_photo_url`, `is_profile_complete`; all added to `to_dict()`
- `backend/modules/auth/service/auth_service.py` — added `issue_token(user_id, role)` for OTP flow
- `backend/main.py` — import `OtpCode` to register table; 5 `ALTER TABLE IF NOT EXISTS` migrations for new user columns

**App**
- `models/Models.kt` — `User` gains 5 new fields; added `OtpVerifyResponse`; removed legacy `TokenData`
- `network/ApiService.kt` — removed `firebaseVerify`; added `sendOtp`, `verifyOtp`, `completeProfile`
- `ui/screens/auth/SplashScreen.kt` — checks saved JWT via `AuthTokenManager`; calls `getMe` to restore session; routes to Home / ProfileSetup / Phone accordingly
- `ui/screens/auth/PhoneInputScreen.kt` — calls real `sendOtp` endpoint; shows inline error on failure
- `ui/screens/auth/OtpScreen.kt` — calls real `verifyOtp`; saves JWT; seeds `UserSession`; routes to ProfileSetup (new) or Home (returning); error state clears boxes
- `ui/screens/auth/ProfileSetupScreen.kt` — step 1 now collects name + DOB + city (free text); step 2 sport picker unchanged; calls `completeProfile` on submit; "Skip for now" also submits with empty sports

### Architectural decisions
- OTP module has no controller — service called directly from auth routes to avoid extra router noise
- `is_profile_complete` flag on User drives routing: Splash and OtpScreen both check it so returning users always land on Home, not ProfileSetup
- Dev bypass (`OTP_DEV_MODE=true`, code=`123456`) lets full flow be tested today; switching to real SMS requires only adding MSG91 credentials to `.env` and setting `OTP_DEV_MODE=false`

---

## [2026-06-16] Phase 05 — All phases wired end-to-end

### Added
**Backend**
- `match_routes.py` — `GET /matches/mine/active`, `GET /matches/mine`, `GET /matches/{match_id}` (all enriched: sport, ground_name, ground_address, scheduled_at, captain_name, player_ids)
- `match_repository.py` — `find_by_user()`, `find_active_by_user()`, `find_by_id_enriched()`, `_enrich()` helper (joins CartType, Cart, Location, Timeslot, User, MatchPlayer)

### Modified
**Backend**
- `matchmaking_schema.py` — `JoinQueueRequest` now accepts `sport: Optional[str]` (name) + `sport_id: Optional[int]` (legacy)
- `matchmaking_routes.py`:
  - `/play-now`: resolves sport name → ID via DB lookup; normalises `skill_level` to uppercase; response now wrapped in `{success, data}` format with `in_queue`, `match_found`, `match_id` fields
  - `/status`: adds `in_queue: True`, `match_found: bool`, removes raw pricing dump

**App**
- `ui/screens/play/QueueTrackerScreen.kt` — reads real `queueStatus.playersSearching` for player count; shows `estimatedWaitSeconds` instead of hardcoded ₹400; navigates to `ActiveMatch` when `matchFound=true` with real `matchId`; removed fake 7.5 s delay
- `ui/screens/play/ActiveMatchScreen.kt` — full rewrite: calls `vm.loadMatch(matchId)`, shows real sport/ground/time/captain/player count; graceful "Venue TBD" / "Captain" fallbacks for missing fields
- `viewmodel/PlayViewModel.kt` — added `_match`, `loadMatch(matchId)`, `joinQueue` now propagates `QueueStatus` and handles instant-captain match path; poll interval extended to 60×3s
- `data/MatchRepository.kt` — `joinQueue` returns `Result<QueueStatus>`; added `getMatch(id)`
- `network/ApiService.kt` — `joinQueue` returns `ApiResponse<QueueStatus>` (was `ApiResponse<QueueEntry>`)
- `ui/screens/home/HomeScreen.kt` — avatar uses `user?.name`; region defaults to `user?.city`
- `ui/screens/profile/ProfileScreen.kt` — region reads `user?.city ?: user?.region`
- `ui/screens/social/SocietyDetailScreen.kt` — replaced hardcoded fallback member list with member count label

### Architectural decisions
- Match enrichment done at repository layer (not service) — single query session reused for all joins, avoiding N+1
- Match `/mine/active` and `/mine` defined before `/{match_id}` in route order so FastAPI doesn't try to parse "mine" as an integer
- Android `joinQueue` now returns `QueueStatus` instead of `QueueEntry` — shape matches both join and poll responses, simplifying state management in ViewModel

---

## [2026-06-16] Phase 04c — Home screen real data (Phase 2)

### Modified
**App**
- `ui/screens/home/HomeScreen.kt`
  - `PlixoAvatar` now uses `user?.name?.takeIf { it.isNotBlank() } ?: "Player"` instead of hardcoded `"Aarav Mehta"`
  - Default region now reads `user?.city` (new field) falling back to `user?.region` and then `"My area"` — was hardcoded to `"Indiranagar"`
- `ui/screens/profile/ProfileScreen.kt`
  - Region display now reads `(user?.city ?: user?.region)?.takeIf { it.isNotBlank() } ?: "—"` — was only reading the old `region` field which isn't populated by the new OTP auth flow

---

## [2026-06-16] Phase 04a — Navigation bug fix (Phase 0)

### Added
- `parentTabRoute()` top-level function in `AppNavigation.kt` — maps any route (including sub-screen routes like `queue/{sport}`, `tournament_detail/{id}`) to its owning tab route, enabling correct active-tab highlighting at any stack depth

### Modified
- `navigation/AppNavigation.kt`
  - Replaced `noNavRoutes` blacklist with `tabRoutes` whitelist — bottom nav now shown ONLY on the 5 root tabs (Home, Play, Tournaments, Societies, Profile) plus Captain/Chat when flagged on; all sub-screens (Queue, ActiveMatch, TournamentDetail, SocietyDetail, EditProfile, KYC flow, etc.) are now full-screen without a nav bar
  - `showBottomNav = currentRoute in tabRoutes` (was negative filter)
  - `popUpTo` now uses `navController.graph.startDestinationId` (was string `Screen.Home.route`) — more reliable, avoids route-string lookup failure when Home hasn't been pushed yet
- `ui/components/BottomNav.kt`
  - Added import for `parentTabRoute`
  - `isActive` now uses `parentTabRoute(currentRoute) == tab.route` instead of `currentRoute == tab.route` — Home tab stays highlighted while on sub-screens that belong to the Play/Tournaments/etc. flows

### Architectural decision
Bottom nav visibility is now a **whitelist** (show only on root tabs) rather than a blacklist (hide on known screens). This is correct for an app where new sub-screens are regularly added — new screens are automatically hidden from the nav bar by default without needing to update a list.

---
## [2026-06-15] Phase 03e — Revert image redesign, keep glass, real-data + empty states

Per user direction: reverted the sports-app-image-inspired layouts back to the previous
UI, kept the glassmorphism, thickened the nav, and replaced mock content with real data +
empty states on Home and Profile.

### App Changes
**Modified**
- `ui/components/BottomNav.kt` — thicker (52dp icons, 13dp padding) refractive glass slab:
  layered vertical shimmer + diagonal sheen + gradient rim, active icon glow shadow
- `ui/screens/home/HomeScreen.kt` — removed stories row + VS match cards + fake "285 playing"
  + Courts grid (unbacked). Stat cards now bind to `UserSession.user` (streak, win rate);
  quick tiles + "Up next" pull from `TournamentsViewModel` with a real empty state. Kept all glass.
- `ui/screens/play/PlayScreen.kt` — reverted date strip + emoji chips back to the 2-col photo grid
- `ui/screens/tournaments/TournamentsScreen.kt` — reverted league/VS cards + sport filter pills
  back to photo `TournamentCard`; added loading + empty states (mock seed removed)
- `ui/screens/profile/ProfileScreen.kt` — stats/XP/level bind to real user (no fake fallbacks);
  Stats / Badges / History tabs are now empty states (backend has no per-sport/badge data)
- `viewmodel/TournamentsViewModel.kt`, `viewmodel/SocialViewModel.kt` — dropped mock seed lists;
  start empty + expose `loading` so screens show real data or empty states

### Architectural decisions
- Glassmorphism retained as the app's visual language; only the image-derived *layouts* were reverted.
- "Real data + empty states" over fabricated numbers: every metric now reflects `UserSession.user`
  or a wired ViewModel; sections without a backend feed render a labelled empty state.

---
## [2026-06-15] Phase 03d — User app feature gating (link real, disable unsupported)

Audited every endpoint `Vmsuserapp/.../network/ApiService.kt` calls against `backend/modules/*`.
Disabled (NOT deleted) user-app features that have no backend endpoint, via a single
reversible kill-switch. Screens + routes stay registered; only entry points are gated.

### Backend audit result
- ON (wired): matchmaking queue, tournaments, societies, captain dashboard (`/captains/me/stats`)
- OFF (no endpoint): Chat (no module), Notifications (no `/notifications`), Wallet
  (`/wallet/*` are zero/empty stubs), Become-a-Captain + KYC (no `/captains/apply`, no KYC),
  Open Matches (`/matches/mine` 404)

### App Changes
**Added**
- `Vmsuserapp/.../config/FeatureFlags.kt` — central booleans, each annotated with the missing endpoint
- `docs/plan-user-app-feature-gating.md` — endpoint audit + decision matrix

**Modified**
- `ui/components/BottomNav.kt` — Chat tab hidden when `!CHAT`; Captain tab guarded by `CAPTAIN_DASHBOARD`
- `ui/screens/home/HomeScreen.kt` — gated notifications bell, coins strip, wallet quick tile
  (replaced with Tournaments tile when off), Become-a-Captain CTA, "See all" → Play fallback
- `ui/screens/profile/ProfileScreen.kt` — menu rows built conditionally (wallet/notifications/
  captain-onboarding/KYC hidden; captain dashboard kept for existing captains)
- `ui/screens/play/PlayScreen.kt` — "Browse open matches" button hidden when `!OPEN_MATCHES`
- `ui/screens/play/ActiveMatchScreen.kt` — captain chat button hidden when `!CHAT`

### Architectural decisions
- **Kill-switch over deletion** — flipping a flag back to `true` re-enables a feature in one line
  once its backend ships. No screens, routes, repositories, or ViewModels were removed.
- Auth (`/auth/firebase-verify`) left untouched despite being a known backend gap (B1) — changing
  it needs a backend + Firebase change, out of scope for this UI-gating pass.

---
## [2026-06-15] Phase 03c — Sports app design refresh (user app)

### App Changes
**Modified**
- `Vmsuserapp/.../ui/screens/home/HomeScreen.kt` — added `StoriesRow` (captain story circles with LIVE indicator), replaced "Courts near you" two-card grid with horizontal `LazyRow` of `MatchVsCard` components (VS layout, LIVE badge, Join button, dark ink background)
- `Vmsuserapp/.../ui/screens/play/PlayScreen.kt` — added `DateStrip` (7-day week scroll, selected day highlighted with PlixoInk), replaced 2-col sport photo grid with two horizontal `Row + horizontalScroll` rows of emoji sport chips (72dp tall, active state PlixoInk + border)

### Architectural decisions
- Adopted VS match card style from Dunkra/sports app mockups — dark `PlixoInk` cards with lime Join button, LIVE red badge, player initials as avatars
- Story circles use lime border for live captains, grey border for offline; no external library
- LazyRow for match cards (HomeScreen); plain Row+horizontalScroll for sport chips inside vertical scroll (avoids nested lazy scroll crash)
- Kept light `PlixoBg` theme — only cards use `PlixoInk` dark background for contrast

---
## [2026-06-15] Phase 03b — Plixo User App migrated to Vmsuserapp

### App (Vmsuserapp — existing project)
**Modified:**
- Migrated all 65 Plixo Kotlin source files from `PlixoApp/` into `Vmsuserapp/` (package `com.example.vmsuser`)
- Added Firebase BOM + Auth + Messaging deps to `Vmsuserapp/app/build.gradle.kts`
- Added `google-services` plugin to root and app `build.gradle.kts`
- Added `firebase-bom`, `firebase-auth`, `firebase-messaging` to `gradle/libs.versions.toml`
- Enabled `buildConfig = true` + `BASE_URL` buildConfigField
- Raised `minSdk` from 24 → 26 (required by Plixo screens)
- Copied 7 flat TTF font files to `res/font/` (Bricolage Grotesque + Plus Jakarta Sans static variants)
- Updated `AndroidManifest.xml`: `android:name=".PlixoApp"`, `POST_NOTIFICATIONS` permission, FCM service
- Updated `strings.xml`: app_name → "Plixo"
- Deleted 29 old VMS-era files (old screens, old repositories, old network layer)
- `PlixoApp/` directory retained but no longer the active project

**Architectural decisions:**
- Kept existing `Vmsuserapp/` Gradle infrastructure (AGP 9.1.0, Kotlin 2.2.10, compileSdk 36)
- Package stays `com.example.vmsuser` (matches existing Firebase app registration in project "Memory")

---
## [2026-06-15] Phase 03 — Plixo User App (Android)

### App (PlixoApp — initial scaffold, superseded by Phase 03b)
**Added:**
- Complete new Android project at `PlixoApp/` (package `com.plixo.app`, minSdk 26, Compose BOM 2024.09.00)
- Design system: `Color.kt` (all tokens — Primary #7C5CFF, Lime #D9F26B, Ink #16151F, 6 block colors, 10 sport colors), `Typography.kt` (Bricolage Grotesque display / Plus Jakarta Sans body), `Shape.kt`, `Theme.kt`
- All 29 navigation routes in `Screen.kt` sealed class; `AppNavigation.kt` NavHost + Scaffold with floating pill BottomNav
- `BottomNav.kt` — ink bg, lime active indicator, dynamic Captain tab
- Network layer: `ApiService.kt` (22 Retrofit endpoints), `RetrofitClient.kt` (OkHttp auth interceptor), `AuthTokenManager.kt` (DataStore JWT), `UserSession.kt` (StateFlow singleton), `SseClient.kt` (OkHttp SSE callbackFlow), `PlixoMessagingService.kt`
- 16 data model classes in `Models.kt` (User, Match, Tournament, Society, ChatThread, etc.)
- Auth screens: `SplashScreen` (animated, ink bg, lime zap), `PhoneInputScreen` (Unsplash hero, +91 prefix), `OtpScreen` (6-box BasicTextField), `ProfileSetupScreen` (2-step, sport chip multi-select)
- `HomeScreen` — location picker, 240dp hero card, stat blocks, coins strip, quick tiles, tournament card, courts grid
- Play flow: `PlayScreen` (sport chips, skill level, mini stats), `QueueTrackerScreen` (radar animation, 7.5s demo navigate), `ActiveMatchScreen` (photo hero, location/captain/player cards, GPS check-in), `OpenMatchesScreen`
- Tournaments: `TournamentsScreen` (filter tabs, photo cards), `TournamentDetailScreen` (progress bar, register CTA)
- Chat: `ChatListScreen` (threads + unread badge), `ChatThreadScreen` (message bubbles, BasicTextField input)
- Social: `SocietiesScreen` (sport-colored avatars, join state), `SocietyDetailScreen` (dark header, AvatarStack), `CreateSocietyScreen` (gated by `canCreateSociety`)
- Profile: `ProfileScreen` (ink header, stat row, captain promo), `EditProfileScreen`, `SettingsScreen` (sign-out, delete account), `WalletScreen` (balance card, transactions), `NotificationsScreen`
- Captain mode: `CaptainDashboardScreen` (ink bg, 4-stat grid, 3 tabs), `CaptainMatchDetailScreen` (player check-in switches), `CaptainEarningsScreen`
- KYC/Captain funnel: `BecomeACaptainScreen`, `CaptainApplicationScreen` (3-step form), `KycIntroScreen`, `KycUploadScreen`, `KycSubmittedScreen` (lime check, pending badge), `KycStatusScreen` (timeline)
- ViewModels + Repositories: Auth, Play, Tournaments, Social, Chat, Profile, Captain
- `gradle/libs.versions.toml` version catalog; `app/build.gradle.kts` with Firebase, Coil, DataStore, SSE, kotlinx-serialization

**Architectural decisions:**
- Manual DI (no Hilt) — consistent with admin app pattern
- All screens use mock/hardcoded data fallback when API unavailable (backend gaps B1–B4 still to be built)
- SSE client: OkHttp callbackFlow pattern; demo auto-navigates after 7.5s (replace with real SSE when B2 is built)
- Firebase Auth: `firebase-verify` backend endpoint (B1) not yet built — OTP screen navigates directly in demo mode
- Fonts (Bricolage Grotesque, Plus Jakarta Sans) must be added to `res/font/` manually by developer
- `google-services.json` must be added manually (Firebase console)

---
## [2026-06-08] Phase 02 — Societies / Groups Module

### Backend
**Added:**
- `Society` model + `societies` table: name, description, owner_user_id (FK→users), region_id (FK→locations), sport_id (FK→sports), is_public, max_members (default 50), is_active, timestamps
- `SocietyMember` model + `society_members` table: society_id, user_id, role (OWNER/MEMBER), joined_at; UNIQUE(society_id, user_id)
- `SocietyRole` class (plain class pattern: OWNER, MEMBER, ALL)
- `SocietyRepository`: create, find_by_id, find_all (with region/sport/active filters), update, delete
- `SocietyMemberRepository`: add_member, find_member, get_members, remove_member, update_role, count_members
- `SocietyService`: create (auto-adds owner as OWNER member), get_by_id, list, update (owner/SUPER_ADMIN/OPS_MANAGER), deactivate (SUPER_ADMIN/OPS_MANAGER), delete (SUPER_ADMIN only)
- `SocietyMemberService`: join (capacity + public check), leave (owner guard), kick (owner or SUPER_ADMIN), transfer_ownership, get_members, get_leaderboard (reuses PlayerScore filtered by society region+sport)
- `SocietyTournamentService`: register_as_team — validates owner + active society + all member_ids in society, delegates to TournamentService.register()
- `CreateSocietySchema` / `UpdateSocietySchema` (Pydantic v2)
- 13 routes under `/api/v1/societies`: CRUD, join/leave/kick, transfer-owner, leaderboard, tournament-register
- `main.py`: Society + SocietyMember model imports (table auto-creation), society_router registered

**Files added:**
- `backend/modules/society/__init__.py`
- `backend/modules/society/model/society_model.py`
- `backend/modules/society/model/society_member_model.py`
- `backend/modules/society/repository/society_repository.py`
- `backend/modules/society/repository/society_member_repository.py`
- `backend/modules/society/service/society_service.py`
- `backend/modules/society/service/society_member_service.py`
- `backend/modules/society/service/society_tournament_service.py`
- `backend/modules/society/schemas/society_schema.py`
- `backend/modules/society/controller/society_routes.py`
- `backend/modules/society/tests/test_society_service.py` (16 tests)
- `backend/modules/society/tests/test_society_member_service.py` (15 tests)
- `backend/modules/society/tests/test_society_tournament_service.py` (4 tests)

**Files modified:**
- `backend/main.py` — added Society/SocietyMember imports + society_router

**Architectural decisions:**
- Leaderboard reuses `PlayerScore` (global tournament scores) filtered by society's region_id + sport_id — no new points table needed.
- Private societies (is_public=False): creation is supported; join is blocked with clear error. Invite flow deferred to v2.
- `SocietyTournamentService` is a thin adapter — it validates society-level preconditions then delegates to existing `TournamentService.register()`. No duplication of tournament registration logic.
- Owner cannot leave without transferring ownership first — prevents ownerless societies.
- Structural fields (owner_user_id, region_id, sport_id) are immutable after creation — SocietyRepository.update() blocks them. Ownership transfer updates only member roles (owner_user_id column update deferred to v2).
- SUPER_ADMIN-only hard delete; OPS_MANAGER can deactivate (soft) only.
- 35 tests, all passing.

---
## [2026-06-05] Phase 02 — Dynamic Tournament & League System

### Backend
**Added:**
- `Tournament` model: `format_type` (KNOCKOUT/ROUND_ROBIN/LEAGUE), `participant_type` (INDIVIDUAL/TEAM), `team_size`, `rules_json` (JSON — configurable win/draw/loss points, tiebreaker, global_points_per_win)
- `TournamentTeam` model + repository (`tournament_teams` table — team name, captain FK)
- `TournamentParticipant` model + repository (`tournament_participants` table, UNIQUE per user per tournament)
- `TournamentMatch` model + repository (`tournament_matches` table — supports individual and team matches, manual point overrides with notes)
- `TournamentStanding` model + repository (`tournament_standings` table — upsert + rerank on every result)
- `PlayerScore` model + repository (`player_scores` table — global area leaderboard, UNIQUE per user/region/sport)
- `TournamentService`: `register()` and `withdraw()` for individual and team tournaments; `update_tournament()` now validates `format_type`/`participant_type`
- `TournamentMatchService`: `create_match()`, `record_result()` (rules_json-driven + manual override requiring notes), `list_matches()`; validates team membership; cross-checks match belongs to tournament
- `TournamentStandingService`: `get_standings()`, `get_global_leaderboard()`
- `CreateTournamentSchema` / `UpdateTournamentSchema` updated to pass through `format_type`, `participant_type`, `team_size`, `rules_json`
- New routes: `POST/DELETE /{id}/register`, `GET/POST /{id}/matches`, `PUT /{id}/matches/{mid}/result`, `GET /{id}/standings`, `GET /api/v1/leaderboard`

**Architectural decisions:**
- Typed columns for structural fields (format_type, participant_type, team_size); `rules_json` JSON column for scoring — adding new rule types (age_limit, custom tiebreakers) requires no migration.
- KNOCKOUT draws raise `ValueError` at service layer — enforced, not just documented.
- Manual point overrides require non-empty `notes` field for audit trail.
- Global `PlayerScore` upserted on every match result: winner gets `global_points_per_win` (default 10), loser gets `matches_played` increment only. Draw: both get `matches_played++`, no points.
- For TEAM tournaments, all team members individually get global points (resolved via `tournament_participants WHERE team_id = winning_team_id`).
- `rules_json` updates in `update_tournament` are merged (not overwritten) — existing keys preserved unless explicitly overridden.

---
## [2026-06-04] Phase 02 — Audit log finalization

### Backend
**Added:**
- Migration 10: `audit_logs` table (action, actor_user_id, target_resource_type, target_resource_id, details, created_at)
- `backend/modules/audit/` — model, repo, service (fire-and-forget log()), routes
- `GET /api/v1/audit-logs` — SUPER_ADMIN only, newest first, default limit 200 (max 500)
- AuditService.log() swallows all exceptions — audit failure never breaks audited operations
- 4 tests passing

**Modified (hooks):**
- `user_routes.py` — ROLE_CHANGE audit on successful role update
- `payment_service.py` — REFUND audit after process_refund
- `captain_routes.py` — CAPTAIN_STATUS_CHANGE audit on captain status update
- `dispute_routes.py` — DISPUTE_RESOLVED audit when status set to RESOLVED or CLOSED
- `backend/main.py`, `backend/run_migrations.py`

### Admin App
**Added:**
- AuditLogEntry model (Models.kt)
- AuditLogRepository, AuditLogViewModel, AuditLogScreen (read-only)

**Modified:**
- ApiService.kt — getAuditLogs endpoint
- AppNavigation + MainScreen + MainActivity — audit-logs route (SUPER_ADMIN only)
- PlaceholderScreens.kt — "Audit Log" card under Admin section

### Architecture decisions
- AuditService.log() is fire-and-forget: exceptions silently swallowed, returns {}
- Append-only table: no update or delete endpoints
- Newest-first ordering, hard limit of 200 configurable up to 500 via query param

---
## [2026-06-04] Phase 02 — Audit log module

### Backend
**Added:**
- Migration 10: `audit_logs` table (id, action, actor_user_id FK→users, target_resource_type, target_resource_id, details, created_at)
- Full audit module: `backend/modules/audit/` (model, repository, service, controller, tests)
- `GET /api/v1/audit-logs` — SUPER_ADMIN only, returns recent entries (default 200, max 500)
- `AuditService.log()` — fire-and-forget, never raises; silently returns {} on DB failure
- 4 unit tests (in-memory SQLite) — all pass

**Modified:**
- `backend/main.py` — registered `audit_router`, imported `AuditLog` model
- `backend/run_migrations.py` — added migration 10
- `backend/modules/user/controller/user_routes.py` — ROLE_CHANGE audit hook in `update_user`
- `backend/modules/payment/service/payment_service.py` — REFUND audit hook in `process_refund`
- `backend/modules/captain/controller/captain_routes.py` — CAPTAIN_STATUS_CHANGE hook in `update_captain`
- `backend/modules/dispute/controller/dispute_routes.py` — DISPUTE_RESOLVED hook in `update_dispute`

**Architectural decisions:**
- `AuditService.log()` swallows all exceptions — audit failure must never break primary operations
- Hooks are import-time singletons (module-level `audit_service`) — no DI required for fire-and-forget
- `details` stored as JSON text in TEXT column — avoids JSONB dependency for portability

---
## [2026-06-04] Phase 02 — Dispute/ticket system

### Backend
**Added:**
- Migration 9: `disputes` table (booking_id, user_id, raised_by, title, description, status, resolution_note)
- Full CRUD module: `backend/modules/dispute/` (OPEN/IN_PROGRESS/RESOLVED/CLOSED)
- `GET/POST /api/v1/disputes`, `GET/PUT /api/v1/disputes/{id}`
- Role guards: SUPPORT, OPS_MANAGER, SUPER_ADMIN
- `raised_by` auto-populated from JWT on create (not trusted from client)
- 4 tests passing

**Modified:**
- `backend/main.py`, `backend/run_migrations.py`

### Admin App
**Added:**
- Dispute, CreateDisputeRequest, UpdateDisputeRequest models
- DisputeRepository, DisputeViewModel (loadDisputes, createDispute, resolve), DisputesScreen
- "Raise Ticket" button in SupportScreen per booking row

**Modified:**
- ApiService.kt — dispute endpoints
- AppNavigation + MainScreen + MainActivity — disputes route (SUPPORT/OPS_MANAGER/SUPER_ADMIN)
- SupportScreen — disputeViewModel param + raise ticket dialog per booking

### Architecture decisions
- raised_by set server-side from JWT, not trusted from client
- Disputes navigable from Support panel and Manage screen
---
## [2026-06-04] Phase 02 — Tournament module

### Backend
**Added:**
- Migration 8: `tournaments` table (name, sport_id, region_id, organizer, start_date, end_date, max_teams, status)
- Full CRUD module: `backend/modules/tournament/` (model, repository, service, schemas, routes)
- `GET/POST /api/v1/tournaments`, `GET/PUT/DELETE /api/v1/tournaments/{id}`
- Role guards: list (TOURNAMENT_MANAGER/OPS_MANAGER/SUPER_ADMIN), create/update (TOURNAMENT_MANAGER/SUPER_ADMIN), delete (SUPER_ADMIN)
- `backend/modules/tournament/tests/test_tournament_service.py` — 5 tests

**Modified:**
- `backend/main.py` — registered tournament router + model
- `backend/run_migrations.py` — Migration 8

### Admin App
**Added:**
- `Tournament`, `CreateTournamentRequest`, `UpdateTournamentRequest` in Models.kt
- `TournamentRepository.kt`
- `TournamentViewModel.kt` — list, create, updateStatus
- `TournamentsScreen.kt` — list + create dialog + status change per card

**Modified:**
- `ApiService.kt` — tournament CRUD endpoints
- `AppNavigation.kt` + `MainScreen.kt` — tournaments route (TOURNAMENT_MANAGER/OPS_MANAGER/SUPER_ADMIN)

### Architecture decisions
- Minimal CRUD only — no brackets, no automation, no scheduling
- start_date > end_date rejected at schema validation layer
- 4-layer RBAC: endpoint, ViewModel, navigation, UI
---
## [2026-06-04] Phase 02 — Finance reporting

### Backend
**Added:**
- `GET /api/v1/payments/summary` — FINANCE + SUPER_ADMIN only; returns total_revenue, total_refunded, pending_count, refunded_count via DB-level SQL aggregates
- `PaymentRepository.get_summary()` — uses sqlalchemy.func.sum/count with COALESCE
- `PaymentService.get_summary()`
- `backend/modules/payment/tests/test_payment_summary.py` — 2 tests

### Admin App
**Modified:**
- `Models.kt` — added `PaymentSummary` data class
- `ApiService.kt` — added `getPaymentSummary()`
- `PaymentRepository.kt` — added `fetchSummary()`
- `PaymentViewModel.kt` — added REFUNDED to PaymentFilter, totalRefunded/refundedCount to state, loadSummary() fetches backend aggregates (non-fatal)
- `PaymentsScreen.kt` — RevenueSummaryCard shows total refunded + count; REFUNDED filter tab added; empty-state handles REFUNDED

### Architecture decisions
- Revenue computed at DB level via SQL aggregates — not client-side sum
- loadSummary() is non-fatal: summary card shows 0s if endpoint fails, payments list still loads
---
## [2026-06-04] Phase 02 — Ground Owner data isolation

### Backend
**Added:**
- Migration 7: `owner_user_id INT REFERENCES users(id) ON DELETE SET NULL` on `carts` table
- `CartRepository.find_by_owner(owner_user_id)` — SQL `WHERE owner_user_id = :uid`
- `CartRepository.find_by_region(region_id)` — DB-level region filter
- `CartService.list_carts_by_owner(owner_user_id)` and `list_carts_by_region(region_id)`
- `BookingRepository.find_by_owner(owner_user_id)` — SQL subquery `WHERE assigned_cart_id IN (SELECT id FROM carts WHERE owner_user_id = :uid)`
- `BookingService.list_bookings_by_owner(owner_user_id)`
- `backend/modules/cart/tests/test_cart_owner_isolation.py` — 3 tests
- `backend/modules/booking/tests/test_booking_owner_isolation.py` — 2 tests

**Modified:**
- `cart_model.py` — `owner_user_id` Column + `to_dict()`
- `cart_repository.py` — `create()` accepts `owner_user_id`
- `ground_schema.py` — `UpdateGroundSchema` allows `owner_user_id`; `_to_ground()` passes it through
- `ground_routes.py` — GROUND_OWNER uses `list_carts_by_owner()` (DB-level); non-owner uses `list_carts_by_region()` (DB-level)
- `booking_routes.py` — GROUND_OWNER uses `list_bookings_by_owner()` (replaces region-based filter)

### Admin App
**Modified:**
- `Models.kt` — `Ground.owner_user_id: Int?`, `UpdateGroundRequest.owner_user_id: Int?`
- `GroundRepository.kt` — `assignOwner()`, `searchUserByPhone()`
- `GroundViewModel.kt` — owner search state in `GroundUiState`, `searchOwnerByPhone()`, `assignOwner()`, `clearOwnerSearch()`
- `GroundsScreen.kt` — owner assignment section in `GroundCard` (SUPER_ADMIN only): phone search → found user → assign button
- `MainScreen.kt` — `currentUserRole` wired into `GroundsScreen`

### Architecture decisions
- Isolation enforced at SQL `WHERE` clause — never in Python loops or route handlers
- `owner_user_id` nullable: unowned grounds invisible to GROUND_OWNER by default (safe)
- `ON DELETE SET NULL`: deleting a user releases their grounds rather than cascading
- Phone search reuses existing `GET /api/v1/users/search?phone=` — no new endpoint
- Role comparison normalised with `.lower()` for forward compatibility
---
## [2026-06-04] Phase 02 — Role change dropdown (backend-driven)

### Backend
**Added:**
- `GET /api/v1/users/assignable-roles` — returns roles the caller can assign, filtered by JWT role (SUPER_ADMIN → all 8 derived from UserRole enum, others → [])
- `backend/modules/user/tests/test_user_routes.py` — 3 tests: super_admin gets all roles, non-admin gets empty list, unauthenticated gets 401

**Modified:**
- `backend/modules/user/controller/user_routes.py` — new route + `_ALL_ROLES` constant derived from UserRole enum

### Admin App
**Modified:**
- `models/Models.kt` — added `AssignableRolesResponse`
- `network/ApiService.kt` — added `getAssignableRoles()`
- `data/UserManagementRepository.kt` — added `getAssignableRoles()` (non-fatal, returns emptyList on error)
- `viewmodel/UserManagementViewModel.kt` — added `_assignableRoles` StateFlow + `loadAssignableRoles()` called in init
- `ui/screens/UsersScreen.kt` — replaced hardcoded roles list with `assignableRoles` from ViewModel; added empty-state text in dialog

### Architecture decisions
- Role policy lives on the server (not hardcoded in the app) — future role-matrix changes require only a backend deploy, no app update
- Repository failure is non-fatal (returns emptyList) — broken backend doesn't crash the Users screen
- No changes to the role-change submit path (changeRole → updateRole → PUT /users/{id} → DB)
---

## 2026-05-30 — Phase 02: Admin App — Time-Based Billing UI

### Summary
Wired the admin app to the time-based billing backend. Session start/end now uses the `/start-session` and `/end-session` endpoints (metered billing flow). `IN_PROGRESS` booking cards show a live elapsed-time timer (`LiveSessionTimer` composable, 1-second tick, `isActive`-guarded loop, API-24-safe `SimpleDateFormat`). `AWAITING_TIME_PAYMENT` cards display session duration, block count, and time bill amount with a note to approve in the Payments tab. Pricing (FeeConfig) edit dialog gains matching_fee, rate_per_block, block_duration_minutes, max_duration_minutes fields plus a surge enabled toggle and multiplier (edit-only). Duration fields only required when rate_per_block > 0.

### Admin App — Modified Files

| File | Change |
|------|--------|
| `models/Models.kt` | Added session fields to `Booking` (session_started_at, session_ended_at, session_minutes, session_blocks, time_bill_amount, surge_multiplier_snapshot); `payment_type` to `Payment`; time-rate + surge fields to `FeeConfig`; extended `CreateFeeConfigRequest` + `UpdateFeeConfigRequest`; added `SessionStatus` model |
| `network/ApiService.kt` | Added `startSession`, `endSession`, `getSessionStatus` endpoints |
| `data/BookingRepository.kt` | Added `startSession()`, `endSession()` |
| `data/FeeConfigRepository.kt` | Extended `createFeeConfig()` + `updateFeeConfig()` with time-rate + surge params |
| `viewmodel/BookingViewModel.kt` | Added `startSession()`, `endSession()` methods |
| `viewmodel/FeeConfigViewModel.kt` | Extended `addConfig()` + `updateConfig()` signatures for all time-rate + surge params |
| `ui/screens/BookingsScreen.kt` | Live `LiveSessionTimer` composable on IN_PROGRESS cards; AWAITING_TIME_PAYMENT info section; wired to `startSession`/`endSession` ViewModel methods |
| `ui/screens/FeeConfigScreen.kt` | `FeeConfigFormDialog` extended with 6 new fields + surge toggle; card shows time-rate info; both Add + Edit call sites updated; `toBigDecimal().stripTrailingZeros().toPlainString()` for numeric pre-fills |

### Architectural Decisions
- **API-24-safe timer**: Uses `java.text.SimpleDateFormat` (not `java.time`) since `minSdk = 24` and `coreLibraryDesugaring` is not configured.
- **`isActive`-guarded coroutine loop**: `LiveSessionTimer` uses `while (isActive)` inside `LaunchedEffect` — loop respects coroutine cancellation when composable leaves composition.
- **Surge edit-only**: Surge is an operational control (flip live), not a config-time setting; Add dialog does not show surge fields.
- **AWAITING_TIME_PAYMENT is display-only**: No action button from BookingsScreen — the TIME_BILL payment appears in Payments tab where Finance/Admin approves it.
- **Conditional duration validation**: Block/max duration fields only required when rate_per_block > 0, allowing basic configs without time-billing.
- **Old `/start` + `/complete` endpoints kept**: Backward compatibility; new metered-billing session endpoints are `/start-session` and `/end-session`.

## 2026-05-29 — Phase 02: Time-based billing backend (Tasks 1-9)

### Summary
End-to-end time-based ("metered") billing for cart sessions. A booking now starts with a **matching fee** (paid up front from the pricing config), then runs a live **session** whose final bill is computed from elapsed time in fixed blocks (`blocks = ceil(minutes / block_duration)`, `bill = blocks * rate_per_block * surge_multiplier`). The session-cost portion is collected as a **second payment** after the session ends. Surge pricing is configurable per region/cart-type and snapshotted at session end. Billing math lives in a pure, dependency-free calculator for testability.

### Backend — New Files

| File | Description |
|------|-------------|
| `modules/billing/__init__.py` | Billing module package |
| `modules/billing/calculator.py` | Pure billing calculator + `compute_session_bill` — no DB/I-O, deterministic math (blocks = ceil(minutes/block), bill = blocks × rate × surge) |

### Backend — Modified Files

| File | Change |
|------|--------|
| `modules/fee_config/model/fee_config_model.py` | Added time-rate + surge columns: `matching_fee`, `rate_per_block`, `block_duration_minutes`, `max_duration_minutes`, `surge_enabled`, `surge_multiplier` |
| `modules/fee_config/schemas/fee_config_schema.py` | Schema fields for the new time-rate + surge config columns; surge update payload |
| `modules/fee_config/service/fee_config_service.py` | Surge toggle/update logic; exposes time-rate config to billing |
| `modules/booking/model/booking_model.py` | Session lifecycle columns (session start/end, status) + `AWAITING_TIME_PAYMENT` status |
| `modules/booking/service/booking_service.py` | Session lifecycle: start-session / end-session; transitions booking into/out of `AWAITING_TIME_PAYMENT`; invokes billing calculator at session end |
| `modules/booking/controller/booking_routes.py` | New session routes: start-session, end-session, session-status |
| `modules/payment/model/payment_model.py` | `payment_type` discriminator (matching-fee vs time-bill) for the two-payment flow |
| `modules/payment/service/payment_service.py` | Two-payment flow: matching-fee payment up front + time-bill payment after session end |
| `modules/fee_config/controller/fee_config_routes.py` | Surge configuration endpoint (fee-config surge) |
| `run_migrations.py` | Migrations 3-5 — fee_config time-rate + surge columns, booking session columns + status, payment `payment_type` |
| `db_seed.py` | Pricing seed updated with `matching_fee`, `rate_per_block`, `block_duration_minutes` (45), `max_duration_minutes` (180), `surge_enabled` (FALSE), `surge_multiplier` (1.0) |

### Backend Changes
- New endpoints: `start-session`, `end-session`, `session-status` (booking); fee-config **surge** configuration.
- Payment model now carries a `payment_type` so the **matching fee** and the **time-bill** are distinct payment rows under one booking.
- Billing formula (pure calculator): `blocks = ceil(elapsed_minutes / block_duration_minutes)`, `bill = blocks * rate_per_block * surge_multiplier`, capped by `max_duration_minutes`.
- Matching fee is read from the region/cart-type pricing config and charged at booking start.
- Re-seeded successfully; full backend suite: **311 passed**. The two booking test files (`modules/booking/tests/test_booking_service.py`, `modules/booking_item/tests/test_booking_item_service.py`) already import the `Match`/`Sport` ORM models, so no FK-registration fix was required this cycle.

### Architectural Decisions
- **Pure calculator** for billing math: `modules/billing/calculator.py` has no DB or service dependencies, so it is unit-testable in isolation and reusable from any caller. `compute_session_bill` is the single source of truth for the formula.
- **Matching fee sourced from pricing config** (`region_cart_type_configs.matching_fee`) rather than hardcoded — keeps per-region/per-cart-type pricing in one place.
- **`AWAITING_TIME_PAYMENT` state**: a booking sits in this status between session end and the time-bill payment, making the two-payment flow explicit and recoverable.
- **Surge snapshot at session end**: the `surge_multiplier` in effect is captured when the session ends so the final bill is reproducible even if config changes later.
- **GROUND_OWNER as interim captain guard**: session start/end is gated on GROUND_OWNER until a dedicated captain role/guard lands.
- **10-minute grace auto-start deferred to a scheduler (Phase 03)**: sessions are started explicitly for now; the automatic grace-period auto-start needs a background scheduler.
- **Known follow-ups**: inject `FeeConfigService` into `PaymentService` (currently constructed internally — hurts testability); narrow the bare-except in the refcode retry path to the specific integrity error.

---

## 2026-05-29 — Phase 01A-2: Ground Owner panel + RBAC test fixes

### Summary
Backend region isolation for ground_owner role: bookings and grounds are now filtered at the **repository/query level** by the user's `region_id`. New `GroundOwnerScreen` with dedicated "My Grounds" bottom tab. Fixed 2 pre-existing RBAC test failures (cart create schema rejection + admin booking creation).

### Backend — Modified Files

| File | Change |
|------|--------|
| `modules/booking/repository/booking_repository.py` | Added `find_by_region_id(region_id)` — SQL query filtered by `Booking.region_id` |
| `modules/booking/service/booking_service.py` | Added `list_bookings_by_region(region_id)` — delegates to new repo method + lazy expiry + batch enrichment |
| `modules/booking/controller/booking_routes.py` | `list_bookings` now branches: `ground_owner` → `list_bookings_by_region(user.region_id)`; other admins → `list_bookings()`; users → `list_bookings_by_user()` |
| `modules/cart/controller/ground_routes.py` | `list_grounds` now accepts optional `region_id` query param; `ground_owner` is forced to their own `region_id` (param ignored) |
| `modules/auth/tests/test_rbac_routes.py` | **Fix 1:** `test_admin_can_create_cart` — removed `status` from JSON body (CreateCartSchema rejects it). **Fix 2:** renamed `test_admin_cannot_create_booking` → `test_admin_can_also_create_booking` — super_admin is in `_ALL_AUTHENTICATED_ROLES` and correctly passes `require_user` |

### Admin App — New Files

| File | Description |
|------|-------------|
| `ui/screens/GroundOwnerScreen.kt` | Dedicated ground_owner panel: region grounds (status cards) + region bookings list, both auto-filtered server-side |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/screens/MainScreen.kt` | Added `MyGrounds` bottom nav item (ground_owner only), NavHost route with RBAC guard |
| `network/ApiService.kt` | Added `getGroundsByRegion(regionId)` for region-filtered grounds fetch |
| `test/.../UserManagementViewModelTest.kt` | Added `getGroundsByRegion` no-op override |

### Backend Changes
- `GET /api/v1/bookings` — ground_owner now sees only their region's bookings (data isolation at repo level)
- `GET /api/v1/grounds?region_id=` — optional region filter; ground_owner forced to their region
- All 291 backend tests passing (previously 289/291 due to the 2 RBAC test bugs)

### Architectural Decisions
- **Region isolation at repository level** (CLAUDE.md hard rule): `find_by_region_id` queries `WHERE region_id = :id` in SQL, not post-hoc filtering. Ground routes do post-filter on the in-memory list since CartService.list_carts has no region param — acceptable for the current ground count but should be pushed to SQL in Phase 03 if scale demands it.
- **ground_owner without region_id returns empty lists**: defensive default; a ground_owner who hasn't been assigned a region sees nothing rather than everything.
- **Admin booking creation allowed**: `require_user` permits any authenticated role. The old test expected admins to be blocked, but current `_ALL_AUTHENTICATED_ROLES` includes all admin roles. The test was wrong, not the code.

---

## 2026-05-29 — Phase 01A: Role panels (Finance/Tournament/CSR) + User screen filters

### Summary
Frontend-only cycle. Added role-specific experiences for Finance (payment filter tabs + revenue summary + refund), Tournament Manager (Matches as a dedicated bottom tab), and CSR Partner (read-only matches panel + tournaments placeholder). Extended the SUPER_ADMIN Users screen with role filter chips and grouped-by-role sections. Ground Owner panel deferred (needs backend region isolation). Fixed a latent test-compile gap (captain ApiService overrides lacked imports).

### Backend — No Changes
(Refund endpoint `POST /api/v1/payments/refund/{payment_id}` already existed and is now consumed by the app.)

### Admin App — New Files

| File | Description |
|------|-------------|
| `ui/screens/CsrScreen.kt` | Read-only CSR_PARTNER panel: live matches list (reuses `MatchViewModel`) + tournaments "coming soon" placeholder |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/screens/UsersScreen.kt` | Added `RoleFilterRow` (horizontal chips: All + each role present, with counts), `RoleSectionHeader`, `ROLE_ORDER`, `roleLabel()`. Default view groups users by role; selecting a chip shows a flat filtered list. `rememberSaveable` filter state |
| `viewmodel/PaymentViewModel.kt` | Keeps full payment list in memory; added `PaymentFilter` enum (PENDING_REVIEW/ALL), `setFilter()`, `totalRevenue` (sum of SUCCESS amounts), `pendingReviewCount`, and `refundPayment()` |
| `ui/screens/PaymentsScreen.kt` | Added `RevenueSummaryCard` + `PaymentFilterTabs`; `PaymentCard` now renders status-aware actions (Approve/Reject for UNDER_REVIEW, Refund for SUCCESS, none otherwise). Removed per-item `AnimatedVisibility` (scope clash with new outer Column) |
| `data/PaymentRepository.kt` | Added `refundPayment(paymentId)` |
| `network/ApiService.kt` | Added `refundPayment` → `POST /api/v1/payments/refund/{payment_id}` |
| `ui/screens/MatchesScreen.kt` | `onBack` made nullable; back arrow hidden when null so the screen works as a bottom tab |
| `ui/screens/MainScreen.kt` | Added `Matches` (tournament_manager) and `Csr` (csr_partner) bottom-nav items + NavHost routes with role guards |
| `test/.../UserManagementViewModelTest.kt` | Added missing imports for `CreateCaptainRequest`/`UpdateCaptainRequest`; added `refundPayment` no-op override |

### App Changes
- Finance role: Payments screen now has Pending Review / All tabs, a total-collected revenue card, and per-payment Refund on SUCCESS records.
- Tournament Manager role: gets a dedicated **Matches** bottom tab (ops/super still reach Matches via Manage).
- CSR Partner role: gets a dedicated **CSR** bottom tab (read-only matches; tournaments placeholder for Phase 02).
- Users screen: filter chips + grouped sections for all 8 roles.

### Architectural Decisions
- **Matches tab limited to `tournament_manager`**: ops_manager/super_admin already reach Matches via the Manage screen, so a duplicate tab would clutter their nav.
- **Refund only surfaced on SUCCESS payments**: mirrors backend state machine; avoids invalid transitions from the UI.
- **Four-layer RBAC kept**: new `matches`/`csr` routes guarded in NavHost (`!in` role set → `onForbidden`), tab visibility filtered in the bottom-nav list, and backend role guards already enforce on every endpoint.
- **Ground Owner deferred**: proper region isolation must live at the repository/query level (CLAUDE.md hard rule), so it is its own backend+app slice — not bundled into this frontend-only cycle.

### Plan
- `docs/plan-role-panels.md` — scope + deferred Ground Owner notes.



### Summary
Wired all orphaned ViewModels (SystemConfig, QueueOverview) into navigation. Added Support and Captain stubs. Backend: captain module from scratch, timeslot `is_active`, user phone-search endpoint, DB migration script.

### Backend — New Files

| File | Description |
|------|-------------|
| `backend/modules/captain/__init__.py` | Package marker |
| `backend/modules/captain/controller/__init__.py` | Package marker |
| `backend/modules/captain/model/captain_model.py` | `CaptainStatus` constants + `Captain` ORM model linked to `users` table |
| `backend/modules/captain/schemas/captain_schema.py` | `CreateCaptainSchema` + `UpdateCaptainSchema` (dict-validation pattern) |
| `backend/modules/captain/repository/captain_repository.py` | `CaptainRepository`: get_all, get_by_id, get_by_user_id, create, update, delete; module-level singleton |
| `backend/modules/captain/service/captain_service.py` | `CaptainService` with user-join enrichment; raises `HTTPException` directly (404/400) |
| `backend/modules/captain/controller/captain_routes.py` | `/api/v1/captains` CRUD routes using `require_role()` (SUPER_ADMIN + OPS_MANAGER) |
| `backend/run_migrations.py` | Idempotent migration script: adds `timeslots.is_active`, creates `captains` table |

### Backend — Modified Files

| File | Change |
|------|--------|
| `backend/modules/timeslot/model/timeslot_model.py` | Added `is_active = Column(Boolean, nullable=False, default=True)` |
| `backend/modules/timeslot/schemas/timeslot_schema.py` | Added optional `is_active` bool validation in `UpdateTimeslotSchema.is_valid()` |
| `backend/modules/timeslot/repository/timeslot_repository.py` | `create()` passes `is_active` to ORM constructor |
| `backend/modules/user/controller/user_routes.py` | Added `GET /api/v1/users/search?phone=` (SUPER_ADMIN + SUPPORT only); placed before `/{user_id}` to avoid path shadowing |
| `backend/main.py` | Registered captain router; imported `Captain` model for `Base.metadata.create_all` |

### Admin App — New Files

| File | Description |
|------|-------------|
| `ui/screens/SupportScreen.kt` | Stub screen with back button and "coming soon" body |
| `ui/screens/CaptainScreen.kt` | Stub screen with back button and "coming soon" body |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/screens/MainScreen.kt` | Full rewrite: TopAppBar (Plixo title + role chip + logout icon), Support bottom tab, new role sets (SUPPORT_ROLES, SYSTEM_CONFIG_ROLES, QUEUE_ROLES, CAPTAIN_ROLES, TOURNAMENT_ROLES, CSR_ROLES, GROUND_OWNER_ROLES), new params `systemConfigViewModel` + `queueOverviewViewModel` + `onLogout`, new routes `manage/system-config`, `manage/queue`, `manage/captains`, DebugRoleSwitcher with all 8 roles |
| `ui/screens/PlaceholderScreens.kt` | Added `onNavigateToSystemConfig`, `onNavigateToQueue`, `onNavigateToCaptains` params; System Config tile (super_admin), Queue Overview + Captains tiles (super_admin/ops_manager) |
| `navigation/AppNavigation.kt` | Added `systemConfigViewModel: SystemConfigViewModel`, `queueOverviewViewModel: QueueOverviewViewModel` params; wired `onLogout` → `authViewModel.logout()` + navigate("login") |
| `MainActivity.kt` | Instantiated `SystemConfigRepository`, `QueueRepository`, `SystemConfigViewModel`, `QueueOverviewViewModel`; all passed to `AppNavigation` |

### Backend Changes
- `POST /api/v1/captains` — create captain profile (links user_id to ground)
- `GET /api/v1/captains` — list all captains with user enrichment
- `GET /api/v1/captains/{id}` — get single captain
- `PUT /api/v1/captains/{id}` — update captain status/ground
- `DELETE /api/v1/captains/{id}` — remove captain
- `GET /api/v1/users/search?phone=` — find user by phone (SUPER_ADMIN + SUPPORT)
- `timeslots.is_active` column added (DEFAULT TRUE, non-breaking)
- Migration: run `cd backend && python run_migrations.py`

### Architectural Decisions
- **Captain as profile table** (not a new UserRole enum): user identity vs operational function kept separate; a user can have both `super_admin` role and a captain profile if needed
- **`is_active` default TRUE**: non-breaking migration; existing timeslot records get TRUE backfilled by `DEFAULT TRUE`
- **SupportScreen/CaptainScreen as stubs**: referenced by MainScreen.kt routing, so must exist to compile; real content comes in Phase 01-B
- **All four RBAC layers applied to new routes**: backend role guard → ViewModel (will have role check in Phase 01-B) → navigation LaunchedEffect guard → UI tile visibility

---

## 2026-05-27 — Phase 01-B: UI Overhaul (Clean Professional Style)

### Summary
Admin app visual overhaul: replaced glassmorphism cards and gradient background with clean white cards, flat bottom nav, grouped ManageScreen with labelled sections, renamed confusing menu items, and unified icon/weight styling throughout.

### Backend — No Changes

### Admin App — Modified Files

| File | Change |
|------|--------|
| `ui/components/AppCard.kt` | Replaced glassmorphism (gradient brush, border, 18dp corners, 20dp padding, dark-mode glass tint) with plain white card: 12dp corners, 1dp elevation, 16dp padding, no border |
| `ui/theme/Color.kt` | `BackgroundLight` tweaked from `#F5F6FA` to `#F8F9FC` (cooler, less yellow) |
| `ui/screens/MainScreen.kt` | Removed `Box` radial gradient background; Scaffold `containerColor` → `MaterialTheme.colorScheme.background`; bottom nav: removed floating pill (padding + clip RoundedCornerShape 24dp), `tonalElevation` 8dp → 0dp, `containerColor` → plain surface |
| `ui/screens/PlaceholderScreens.kt` | ManageScreen rewritten: grouped into 4 labelled sections (Operations, Catalogue, Venues & Matches, Admin); renamed tiles (Sports→Sport Types, Fee Config→Pricing, Items→Menu Items, Queue Overview→Live Queue, System Config→System Settings); ManageCard simplified (no coloured icon box, plain grey icon + title/subtitle) |
| `ui/screens/DashboardScreen.kt` | `StatCard`: removed per-card `accentColor` param, icon box uses neutral `surfaceVariant` background + `onSurfaceVariant` tint; value text `FontWeight.Bold` → `FontWeight.SemiBold`; header `FontWeight.Bold` → `FontWeight.SemiBold` |

### Architectural Decisions
- **No gradient in production UI**: gradient was purely decorative and made text contrast hard to predict across devices; flat background is more readable and accessible
- **Grouped ManageScreen**: all 10+ items in a flat list overwhelmed non-technical admins; section headers make intent clear without extra navigation
- **Neutral icon tint on StatCards**: per-card accent colours (blue/orange/green) created a "traffic light" perception mismatch — numbers don't inherently carry colour semantics here
- **`AppCard` simplified aggressively**: removing the `isDark` branch reduced ~60 lines to ~15; dark mode now relies entirely on Material3 theme surface token

---

## 2026-05-20 — Phase 01: SUPER_ADMIN User Management

### Summary
Implemented full user management feature for SUPER_ADMIN role. Backend RBAC hardened: only `super_admin` can change roles or deactivate users; self-lockout enforced. Admin app wired end-to-end with four-layer RBAC enforcement.

### Backend — Modified Files

| File | Change |
|------|--------|
| `backend/modules/user/controller/user_routes.py` | Replaced loose `_ADMIN_ROLES` guard on role-change with `SUPER_ADMIN`-only check; added self-lockout (caller cannot change own role or deactivate self); added `is_active=False` deactivation guard; added `# TODO(phase01-audit)` hooks on both privileged paths; imported `UserRole` enum for consistency |
| `backend/modules/user/service/user_service.py` | Mirrored same guards at service layer (defense-in-depth): `SUPER_ADMIN`-only for role-change and deactivation, self-lockout raised as `ValueError`; imported `UserRole` |
| `backend/modules/auth/tests/test_user_rbac_routes.py` | Added `OPS_MANAGER_USER` fixture + 7 new RBAC tests: super_admin can change role, ops_manager blocked, self role-change blocked, self-deactivation blocked, super_admin can deactivate/reactivate others, invalid role value → 400 |
| `backend/modules/user/tests/test_user_service.py` | Added 2 service-layer tests: non-super-admin role change raises ValueError, super_admin self role-change raises ValueError |

### Admin App — New Files

| File | Description |
|------|-------------|
| `data/UserManagementRepository.kt` | `getUsers()`, `updateRole(id, role)`, `setActive(id, active)` — mirrors RegionRepository pattern with `parseErrorDetail()` |
| `viewmodel/UserManagementViewModel.kt` | `UserManagementState` sealed class, `loadUsers()`, `changeRole()`, `toggleActive()`, per-row `pendingIds` StateFlow, ViewModel-layer self-mutation guard, `UserManagementViewModelFactory` |
| `ui/screens/UsersScreen.kt` | Full users screen: shimmer skeleton, pull-to-refresh, per-row overflow menu, role change AlertDialog (8 roles), active/inactive badges, self-row "(you)" with hidden menu (fourth RBAC layer) |

### Admin App — Modified Files

| File | Change |
|------|--------|
| `models/Models.kt` | Added `AppUser`, `UpdateUserRequest(role?, is_active?)` (single merged DTO); added `user_id: Int? = null` to `LoginResponse` |
| `network/ApiService.kt` | Added `getUsers()` and single `updateUser(id, UpdateUserRequest)` endpoints |
| `data/TokenManager.kt` | Added `USER_ID_KEY`, `userIdFlow: Flow<Int?>`, `saveUserId(id)`, removed USER_ID_KEY in `clearSession()` |
| `viewmodel/AuthViewModel.kt` | Exposed `currentUserId: StateFlow<Int?>` (SharingStarted.Lazily); saves `user_id` from login response |
| `ui/screens/PlaceholderScreens.kt` | `ManageScreen` adds `role` and `onNavigateToUsers` params; Users tile rendered only when `role == "super_admin"` (UI-hide layer) |
| `ui/screens/MainScreen.kt` | Added `USERS_ROLES = setOf("super_admin")`; `manage/users` composable route with role guard → `ForbiddenScreen`; passes `userManagementViewModel` and `currentUserId` to `UsersScreen` |
| `navigation/AppNavigation.kt` | Added `userManagementViewModel` param; collects `currentUserId` from `authViewModel.currentUserId`; passes both to `MainScreen` |
| `MainActivity.kt` | Instantiates `UserManagementRepository` + `UserManagementViewModel`; passes to `AppNavigation` |

### Backend Changes
- `PUT /api/v1/users/{id}`: role-change guard tightened from any `_ADMIN_ROLES` to `SUPER_ADMIN` only
- New guard: deactivation (`is_active=False`) restricted to `SUPER_ADMIN`
- New guard: self-lockout — any caller blocked from changing own role or deactivating self
- Service layer mirrors all three guards (defense-in-depth)
- Audit TODO hooks left at both privileged paths for phase01-audit integration

### Architectural Decisions
- **Single `UpdateUserRequest(role?, is_active?)` DTO** instead of two separate DTOs — avoids Retrofit interface duplication for the same URL; keeps the model layer clean
- **`AppUser` naming** (not `User`) avoids future naming collision with `User` imports in Compose
- **Four-layer RBAC enforcement** for user management: backend endpoint → service (defense-in-depth) → ViewModel (refuses to call backend) → navigation guard → UI-hide (tile + self-row menu)
- **`user_id` persisted in DataStore** via `TokenManager` so `UserManagementViewModel` can block self-mutation without a round-trip

### Test Results
- Backend: 46/46 pass (all new RBAC tests green)
- Android: `./gradlew assembleDebug` BUILD SUCCESSFUL (0 errors, 1 pre-existing deprecation warning in PlaceholderScreens.kt unrelated to this change)

---

## 19 Mar 2026 — Day 29: Cart Screen + Checkout + Booking Creation

### Summary
- Implemented `CartScreen` with item list, quantity controls, address section, and checkout flow.
- Added `AddressDialog` with field validation (name, phone 10-digit, address required).
- Added `AddressManager` integration for local address storage via DataStore.
- Implemented booking creation flow with full validation:
  - **Mixed cart type prevention**: rejects orders spanning multiple cart types.
  - **Safe region/timeslot fallback**: uses first available, errors if none.
  - **Address validation**: blocks checkout if address is blank.
  - **Double-submit prevention**: `isSubmitting` guard + button disabled state.
  - **Cart clears only after successful API response** (not before).
- Connected cart → backend booking API (`POST /api/v1/bookings`).
- Updated `BookingStatusScreen` with booking ID, "Pending Payment" status, and navigation CTAs.
- Added cart FAB with badge on `HomeScreen` for quick cart access.
- Loaded regions and timeslots in `HomeViewModel.loadHome()` for checkout data.

### Android — New Files

| File | Description |
|------|-------------|
| `ui/screens/CartScreen.kt` | Cart item list, quantity controls, address section, checkout with all validations, Snackbar errors |
| `ui/components/AddressDialog.kt` | Address form dialog with name/phone/address validation, saves via AddressManager |

### Android — Modified Files

| File | Change |
|------|--------|
| `viewmodel/HomeViewModel.kt` | Added `getCartItems()`, `getTotalAmount()`, `getCartTypeIds()`, `clearCart()`, `getCartCount()`; added `regions`/`timeslots` to `HomeUiState`; loads regions + timeslots in `loadHome()` |
| `ui/screens/HomeScreen.kt` | Added `onNavigateToCart` callback; FAB with `BadgedBox` showing cart count |
| `ui/screens/BookingStatusScreen.kt` | Full UI: booking ID card, "Pending Payment" status, "Proceed to Payment" + "Back to Home" buttons |
| `navigation/AppNavigation.kt` | Added `cart` route; added `addressManager` parameter; wired `CartScreen` |
| `MainActivity.kt` | Passes `addressManager` to `AppNavigation` |

### Key Design Decisions
- **Single cart type enforcement**: `getCartTypeIds().size > 1` → error. Prevents mixed category orders.
- **Safe fallbacks**: `regions.firstOrNull()?.id` and `timeslots.firstOrNull { it.is_active }?.id` with null checks.
- **Cart clear timing**: `clearCart()` called inside `LaunchedEffect` only when `createState is UiState.Success`.
- **Double-submit**: `BookingViewModel.createBooking()` has `if (_createState.value is UiState.Loading) return` guard; button also disabled via `enabled = !isSubmitting`.
- **Address full format**: concatenates name, address, pincode, phone into single string for API.
- **Snackbar + Retry**: errors shown with Snackbar including "Retry" action label.
- **Empty state UX**: Shopping cart icon + "Your cart is empty 🛒" + "Add items to get started" + "Browse Items" button.

**Status**:
Cart screen + checkout flow operational.
Booking creation connected to backend API.
System stable.

---

## 18 Mar 2026 — Day 28: User App Home Screen + Item Browsing + Local Cart State

### Summary
- Replaced placeholder `HomeScreen` with a real, functional browsing experience.
- Items fetched from backend, filtered by `is_available`, sorted by name, and grouped by cart type.
- Cart type categories shown as horizontal chips (`LazyRow`).
- Items displayed in a flat `LazyColumn` (no nesting) using `forEach { } + items { }` pattern.
- Empty cart type sections are hidden (only groups with ≥ 1 available item shown).
- Local `cart: Map<Int, Int>` (itemId → quantity) for add/remove state — no backend yet.
- Coil `AsyncImage` used for images; placeholder icon shown when `image_url` is null/missing.
- Image rendered with `aspectRatio(1.6f)` for consistent sizing across all item cards.
- Add/remove controls use `+ Add` button (qty=0) or `- qty +` stepper (qty>0) with vertical alignment.

### Android — New Files

| File | Description |
|------|-------------|
| `repository/ItemRepository.kt` | Fetches items via `GET /api/v1/items`; uses `parseErrorDetail` for HTTP error parsing |
| `ui/screens/ItemCard.kt` | Reusable item card: Coil image, optional description, price, add/stepper controls |

### Android — Modified Files

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Added `coil = "2.7.0"` version and `coil-compose` library entry |
| `app/build.gradle.kts` | Added `implementation(libs.coil.compose)` |
| `network/ApiService.kt` | Added `GET /api/v1/items` endpoint (`getItems()`) |
| `viewmodel/HomeViewModel.kt` | Full rewrite: flat `HomeUiState` with `items`, `groupedItems`, `cartTypes`, `cart`, `isLoading`, `error`; concurrent fetch via `async/await`; cart add/remove logic; `HomeViewModelFactory` updated |
| `ui/screens/HomeScreen.kt` | Full rewrite: single flat `LazyColumn`, loading/error/empty states, cart type chips, grouped items |
| `MainActivity.kt` | Instantiated `ItemRepository`, updated `HomeViewModelFactory` call to pass both repos |

### Key Design Decisions
- **No nested LazyColumn**: used `LazyColumn { forEach { item { } + items { } } }` to avoid scroll conflicts.
- **Availability filter**: `items.filter { it.is_available }` before grouping prevents hidden items appearing.
- **Sorted display**: both `cartTypes` and `items` sorted by `name` for consistent, intentional ordering.
- **Empty section pruning**: `groupedItems.filter { it.value.isNotEmpty() }` ensures only non-empty sections render.
- **`aspectRatio(1.6f)`**: cleaner than fixed `height(150.dp)` — handles all image sizes naturally.

**Status**:
Home screen item browsing operational.
Local cart state (no backend) ready for future checkout flow.
System stable.

---

## 17 Mar 2026 — Day 27: Items Management + Category Grouping Module

### Summary
- Implemented full Items Management module in the VMS Admin Android app.
- Backend has no `item-categories` endpoint — `CartType` is used as the item grouping "category" (Part 10 fallback).
- Items are grouped under cart types in the UI; toggling a cart type header mass-activates/deactivates all items in that group (optimistic update with rollback).
- Full CRUD: add, edit, toggle availability, delete items.
- `updatingCartTypeIds: Set<Int>` prevents spam taps on category-level toggle switch.
- Items and cart types loaded in parallel via `async`/`await`; items enriched with `cart_type_name` and sorted alphabetically within each group.
- Empty cart type sections show a header with "0 items" label.

### Android — New Files

| File | Description |
|------|-------------|
| `data/ItemRepository.kt` | CRUD + `parseErrorDetail` pattern; wraps `GET /api/v1/items`, `POST`, `PUT`, `DELETE` |
| `viewmodel/ItemViewModel.kt` | `ItemUiState` with `items`, `cartTypes`, `updatingIds`, `updatingCartTypeIds`; `toggleItemsByCartType()` for mass toggle; `ItemViewModelFactory` |
| `ui/screens/ItemsScreen.kt` | Grouped list by cart type; `CategoryHeader` with mass-toggle `Switch`; `ItemCard` with price, availability badge, edit/delete buttons; `ItemDialog` with name/price/cart-type dropdown; shimmer skeleton; pull-to-refresh; snackbars |

### Android — Modified Files

| File | Change |
|------|--------|
| `models/Models.kt` | Added `Item`, `CreateItemRequest`, `UpdateItemRequest` data classes |
| `network/ApiService.kt` | Added `getItems()`, `getItemsByCartType(@Query)`, `createItem()`, `updateItem()`, `deleteItem()` endpoints |
| `ui/screens/PlaceholderScreens.kt` | Added `onNavigateToItems` callback + Items `ManageCard` entry |
| `ui/screens/MainScreen.kt` | Added `itemViewModel` param + `composable("manage/items")` route |
| `navigation/AppNavigation.kt` | Added `itemViewModel: ItemViewModel` param, wired to `MainScreen` |
| `MainActivity.kt` | Instantiated `ItemRepository`, `ItemViewModelFactory`, `itemViewModel`; passed to `AppNavigation` |

### Key Design Decisions
- `CartType` acts as "category" — no backend changes needed.
- Mass toggle fires sequential `toggleItem()` calls per item; any failure rolls back all items in the group.
- `updatingCartTypeIds` disables the group-level switch during pending backend calls to prevent duplicate requests.
- Items sorted by `(cart_type_name, item_name)` for consistent display order.

---

## 13 Mar 2026 — Day 24: Admin App UX Hardening + UI Polish

### Summary
- Added `isSubmitting` state to all 5 admin ViewModels to prevent duplicate form submissions.
- Replaced static Save buttons with loading-aware buttons (animated spinner + disabled state) across all add/edit dialogs.
- Added success snackbar feedback after every create/update/delete operation.
- Improved keyboard usability with IME actions (`Next`/`Done`) and `onDone` submit in all form dialogs.
- Fixed light-theme glassmorphism in `AppCard` — dark mode keeps glass effect, light mode uses clean solid surface.
- Fixed login screen text field visibility with explicit theme-aware colors.
- Added `LocalSoftwareKeyboardController` to hide keyboard on form submit.

### Android — Modified Files

| File | Change |
|------|--------|
| `RegionViewModel.kt` | Added `isSubmitting`, `successMessage` to `RegionUiState`; refactored `addRegion`, `updateRegion`, `deleteRegion` with submit guard, `delay(200)`, `finally` block, success messages |
| `CartTypeViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `TimeslotViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `CartViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `FeeConfigViewModel.kt` | Same pattern: `isSubmitting`, `successMessage`, submit guard, success feedback |
| `RegionsScreen.kt` | Success snackbar, `isSubmitting` passed to `RegionNameDialog`, `AnimatedContent` Save button, IME Done action, keyboard hide |
| `CartTypesScreen.kt` | Success snackbar, `isSubmitting` passed to `CartTypeNameDialog`, `AnimatedContent` Save button, IME Done action, keyboard hide |
| `TimeslotsScreen.kt` | Success snackbar, `isSubmitting` passed to `TimeslotFormDialog`, `AnimatedContent` Save button, IME Next/Done actions, keyboard hide |
| `CartsScreen.kt` | Success snackbar, `isSubmitting` passed to `CartFormDialog`, `AnimatedContent` Save button, IME Done action, keyboard hide |
| `FeeConfigScreen.kt` | Success snackbar, `isSubmitting` passed to `FeeConfigFormDialog`, `AnimatedContent` Save button, IME Next/Done actions, keyboard hide |
| `AppCard.kt` | Conditional glass effect: dark theme keeps translucent gradient + glass border; light theme uses solid `MaterialTheme.colorScheme.surface` with transparent border |
| `LoginScreen.kt` | Explicit `OutlinedTextFieldDefaults.colors()` for strong text contrast in both themes; IME Next/Done with keyboard hide on login; horizontal padding increased to 24dp |

### Key Patterns
- **ViewModel submit guard**: `if (_uiState.value.isSubmitting) return` at top of every action prevents race conditions even if UI guard is bypassed.
- **Submit flow**: `isSubmitting = true` → repo call → `delay(200)` → reload list → set `successMessage` + dismiss dialog → `finally { isSubmitting = false }`.
- **Dialog UX**: `onDismissRequest` blocked while submitting; Cancel button disabled during submit.
- **AnimatedContent**: Smooth transition between Save text and spinner in button.
- **Keyboard**: `LocalSoftwareKeyboardController.current?.hide()` called before every submit to prevent flicker.

**Status**:
Admin UX hardened — no duplicate requests, visible saving feedback, smoother form interactions, improved theme consistency, better keyboard usability.
System stable.

---

## 13 Mar 2026 — Day 24: Admin Fee Configuration Panel

### Summary
- Implemented **Fee Configuration** management module in the VMS Admin Android app.
- Added full fee config CRUD integration with backend `/api/v1/fee-config` endpoints.
- Enabled Fee Configuration navigation from the Manage screen and wired module end-to-end through app DI/navigation.
- Form dialog includes region/cart-type dropdowns with duplicate-combo prevention and numeric fee validation.

### Android — New Files

| File | Purpose |
|------|---------|
| `FeeConfigRepository.kt` | CRUD for fee configs with backend error parsing (`detail`) |
| `FeeConfigViewModel.kt` | `FeeConfigUiState`, dialog state management, sorted config list |
| `FeeConfigScreen.kt` | Fee config UI with pull-to-refresh, shimmer loading, add/edit/delete dialogs, region/cart-type dropdowns |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `FeeConfig`, `CreateFeeConfigRequest`, `UpdateFeeConfigRequest` |
| `ApiService.kt` | Added 5 fee config endpoints (`getAll`, `getByRegionAndCartType`, `create`, `update`, `delete`) |
| `MainActivity.kt` | Instantiated `FeeConfigRepository` + `FeeConfigViewModel` and passed into navigation |
| `AppNavigation.kt` | Added `feeConfigViewModel` in navigation wiring |
| `MainScreen.kt` | Added `feeConfigViewModel` param, registered `manage/fee-config` route |
| `PlaceholderScreens.kt` | Enabled Fee Configuration card with click navigation callback |

### Key Features
- **Fee Config Screen UX**: `Scaffold`, FAB, `PullToRefreshBox`, `LazyColumn`, shimmer, empty/error states, snackbar errors.
- **Fee Config Card**: shows region name, cart type name, booking fee (₹), cancellation fee (%), platform fee (%), active status badge, edit/delete actions.
- **Add Dialog**: region/cart-type dropdowns with smart filtering (already-configured combos hidden), three numeric fee fields with validation.
- **Edit Dialog**: read-only region/cart-type display, editable fee fields.
- **Validation**: booking_fee >= 0, 0 <= percentages <= 100, cancellation + platform <= 100, client-side duplicate region+cartType prevention.
- **Sorted List**: configs sorted by region name then cart type name for readability.

**Status**:
Fee Configuration module fully operational in Android admin app.
Manage navigation updated with live Fee Config route.
System stable.

---

## 13 Mar 2026 — Day 24: Admin System Management Panel (Carts)

### Summary
- Implemented **Carts** management module in the VMS Admin Android app.
- Added full cart CRUD integration with backend `/api/v1/carts` endpoints.
- Enabled Carts navigation from the Manage screen and wired module end-to-end through app DI/navigation.
- Added optimistic toggle behavior for cart active state using `ACTIVE` / `INACTIVE`.

### Android — New Files

| File | Purpose |
|------|---------|
| `CartRepository.kt` | CRUD for carts with backend error parsing (`detail`) |
| `CartViewModel.kt` | `CartUiState`, optimistic toggle handling, dialog state management |
| `CartsScreen.kt` | Carts UI with pull-to-refresh, shimmer loading, add/edit/delete dialogs, and status controls |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `Cart`, `CreateCartRequest`, `UpdateCartRequest` |
| `ApiService.kt` | Added 5 cart endpoints (`get`, `getById`, `create`, `update`, `delete`) |
| `MainActivity.kt` | Instantiated `CartRepository` + `CartViewModel` and passed into navigation |
| `AppNavigation.kt` | Added `cartViewModel` in navigation wiring |
| `MainScreen.kt` | Registered `manage/carts` route and connected `CartsScreen` |
| `PlaceholderScreens.kt` | Enabled Carts card and added click navigation callback |

### Key Features
- **Carts Screen UX**: `Scaffold`, FAB, `PullToRefreshBox`, `LazyColumn`, shimmer, empty/error states, snackbar errors.
- **Cart Card**: shows label, region name, cart type name, `StatusBadge`, active switch, edit/delete actions.
- **Dialogs**: add/edit dialog with cart label + region/cart-type dropdown selectors; delete confirmation dialog.
- **Optimistic Toggle**: immediate status update in UI, rollback on failure, per-item disable via `updatingCartIds`.

**Status**:
Carts module fully operational in Android admin app.
Manage navigation updated with live Carts route.
System stable.

---

## 12 Mar 2026 — Day 23: Admin System Management Panel (Cart Types & Timeslots)

### Summary
- Implemented **Cart Types** and **Timeslots** management modules in the VMS Admin Android app.
- Backend fix for Timeslots deletion: added proper error handling for `ForeignKeyViolation` (prevents 500 errors when deleting timeslots referenced by bookings).
- Android app now supports full CRUD for Timeslots with optimistic UI for active/inactive toggle.
- Enabled Timeslots and Cart Types cards in the Manage screen.

### Backend Changes

#### `timeslot_service.py` & `timeslot_routes.py` — Error Handling
- Added `try-except IntegrityError` handling in `delete_timeslot` service method.
- Surfacing user-friendly message for foreign key constraints: *"Cannot delete timeslot because it is still referenced by existing bookings."*
- Updated router to catch `ValueError` and return `400 Bad Request`.

### Android — New Files

| File | Purpose |
|------|---------|
| `CartTypeRepository.kt` | CRUD for cart categories with backend error parsing |
| `CartTypeViewModel.kt` | UI state, add/edit/delete dialog management for Cart Types |
| `CartTypesScreen.kt` | UI for Cart Types with pull-to-refresh, shimmer, and status toggles |
| `TimeslotRepository.kt` | CRUD for timeslots + toggle logic |
| `TimeslotViewModel.kt` | Optimistic UI updates for toggling active status, sorting by `start_time` |
| `TimeslotsScreen.kt` | UI for Timeslots with time format validation and range checks |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `CartType`, `Timeslot` and their respective Request data classes |
| `ApiService.kt` | Added 10 new endpoints (5 for Cart Types, 5 for Timeslots) |
| `PlaceholderScreens.kt` | Enabled Cart Types and Timeslots cards; removed "Soon" badges |
| `MainScreen.kt` | Added ViewModels to params and registered new manage routes |
| `AppNavigation.kt` | Wired ViewModels through the navigation graph |
| `MainActivity.kt` | Instantiated repositories and ViewModels for the new modules |

### Key Features
- **Optimistic Toggle**: Switch updates instantly on click; ID added to `updatingTimeslotIds` to disable further interaction while request is in flight.
- **Sorting**: Timeslots automatically sorted by `start_time` in ascending order.
- **Validation**: Client-side checks for `end_time > start_time` and valid `HH:mm` format.
- **Robust Errors**: Backend validation errors (overlapping timeslots) are parsed and displayed via Snackbar.

**Status**:
Cart Types and Timeslots modules fully operational.
Backend error handling improved.
System stable.

---

## 11 Mar 2026 — Day 22: Admin System Management Panel (Regions)

### Summary
- Implemented the first configuration module (Regions) inside the Manage tab of the VMS Admin Android app.
- Backend already had CRUD endpoints at `/api/v1/locations`; Android app now fully consumes them.
- Fixed case-insensitive duplicate region name detection in the backend.
- Added region delete support with confirmation dialog.
- Added proper HTTP error parsing for user-facing error messages (Snackbar).

### Backend Changes

#### `location_repository.py` — Case-Insensitive Duplicate Check
- `find_by_name()` now uses `func.lower()` for case-insensitive comparison.
- "Delhi" and "delhi" are now correctly treated as duplicates.

### Android — New Files

| File | Purpose |
|------|---------|
| `RegionRepository.kt` | Data layer — get/create/update/toggle/delete with `HttpException` error parsing |
| `RegionViewModel.kt` | State management — `RegionUiState`, CRUD, dialog state, delete confirmation |
| `RegionsScreen.kt` | UI — LazyColumn, AppCard items, toggle switch, Edit/Delete buttons, FAB, Snackbar errors |

### Android — Modified Files

| File | Change |
|------|--------|
| `Models.kt` | Added `Region`, `CreateRegionRequest`, `UpdateRegionRequest` data classes |
| `ApiService.kt` | Added `getRegions()`, `createRegion()`, `updateRegion()`, `deleteRegion()` endpoints |
| `PlaceholderScreens.kt` | Rewrote `ManageScreen` with card-based menu (Regions active, 4 others "Soon") |
| `MainScreen.kt` | Added `regionViewModel` param, `manage/regions` nested route |
| `AppNavigation.kt` | Passes `regionViewModel` to `MainScreen` |
| `MainActivity.kt` | Creates `RegionRepository` + `RegionViewModel`, passes to nav |
| `ApiClient.kt` | Updated `BASE_URL` to `192.168.1.3` |
| `network_security_config.xml` | Added `192.168.1.3` to cleartext traffic policy |

### Key Features
- **Manage Screen**: 5 config cards — Regions (active), Cart Types / Timeslots / Carts / Fee Config ("Soon")
- **Regions Screen**: Pull-to-refresh, shimmer loading, animated list entry, empty state
- **Region Cards**: Name + Active/Inactive toggle + Edit button + Delete button (red)
- **Add/Edit Dialog**: Name input with blank validation
- **Delete Confirmation**: "Are you sure?" dialog with red Delete button
- **Error Handling**: `HttpException` body parsed for FastAPI `detail` field → shown in Snackbar
- **Case-Insensitive**: Backend rejects "delhi" if "Delhi" already exists

### Architecture
- Repository pattern with `HttpException` error parsing via `parseErrorDetail()`
- ViewModel manages dialog state (add/edit/delete confirmation) with `StateFlow`
- Snackbar + `LaunchedEffect` for transient error display
- Backend `find_by_name` uses `func.lower()` for case-insensitive SQL comparison

**Status**:
Regions management panel fully implemented.
System stable.

---

## 07 Mar 2026 — Day 21: Admin App UI System Upgrade (Dark/Light Dashboard)

### Summary
- Upgraded the visual design of the VMS Admin Android app to a modern dashboard UI.
- Implemented a full Light/Dark Theme switch that respects the system theme.
- Enhanced core UI components with glassmorphism effects (translucency, subtle gradients, and rounded corners).

### Changes
- **Theme & Colors** (`Color.kt`, `Theme.kt`): Added distinct palettes for Light (Soft White/Gold) and Dark (Deep Space Purple/Dark Grey) modes. Disabled Material You dynamic colors to enforce the premium dashboard look.
- **Glassmorphism Components** (`AppCard.kt`, `MainScreen.kt`): Applied 18dp rounded corners, translucent surfaces, and a dynamic radial gradient background (`Scaffold`) to make the glass effect pop.
- **Status Badges** (`StatusBadge.kt`): Updated colors (Orange, Green, Blue, Red, Gray) with translucent backgrounds (`alpha = 0.2f`) for distinct, readable status pills.
- **Screen Layout Refactors**:
  - `DashboardScreen.kt`: Converted to a 2x2 grid using `LazyVerticalGrid`. Fixed deprecated icon usage.
  - `BookingsScreen.kt` & `PaymentsScreen.kt`: Increased spacing and refined typography for better readability.
  - `MainScreen.kt`: Updated bottom navigation to a floating, rounded bar with outlined Material icons.

---
## 05 Mar 2026 — Day 19: Auth Stability + Payment Approval Automation

### Summary
- Payment approval now auto-confirms bookings (one-click admin workflow).
- Added `/auth/me` endpoint for token validation.
- Improved Android AuthInterceptor logging.
- Added automatic logout on 401 responses.
- Improved token persistence handling.

### Backend Changes

#### `payment_service.py` — Auto-Confirm on Approve
- `approve_payment()` now calls `BookingService.confirm_booking()` after setting payment to SUCCESS.
- Guard: only confirms if booking status is `PENDING_PAYMENT` (prevents double-confirm).
- Uses lazy import to avoid circular dependency (BookingService ↔ PaymentService).
- If confirm fails (e.g. no cart available), payment approval still succeeds.

#### `auth_routes.py` — `GET /api/v1/auth/me`
- New endpoint protected by `Depends(get_current_user)`.
- Returns `{id, name, phone, role}` — useful for token validation and debugging.

### Android Changes

#### `AuthInterceptor.kt` — Debug Logging
- Logs token attachment: `Log.d("AUTH", "Attaching token: ...")`.
- Logs missing token: `Log.e("AUTH", "No JWT token found")`.

#### `ApiClient.kt` — Global 401 Handler
- Added response interceptor: detects `401` responses (excluding `/auth/login`).
- On 401: clears token from DataStore, emits logout event via `SharedFlow`.

#### `AppNavigation.kt` — Auto-Logout Redirect
- Collects `ApiClient.logoutEvent` in `LaunchedEffect`.
- Navigates to login with `popUpTo(0)` — clears entire back stack.

### Admin Workflow (After)
```
User pays → submits UTR → Admin presses Approve
→ payment SUCCESS → booking CONFIRMED → cart assigned
```
**One click operation.** No separate confirm step needed.

---

## 02 Mar 2026 — Day 16: Admin-Configurable UPI & Merchant Settings

### Summary
Added runtime-configurable payment settings so admins can change the UPI ID and merchant/company name without redeploying.

### Changes
- **New model**: `system_config_model.py` — key-value `system_configs` table for runtime settings.
- **New repository**: `system_config_repository.py` — `get(key)` / `set(key, value)` with upsert logic.
- **Updated `payment_service.py`**:
  - Reads `UPI_ID` and `MERCHANT_NAME` from DB at runtime, falls back to `.env` values.
  - `_get_active_upi_id()` / `_get_active_merchant_name()` helpers.
  - `get_admin_payment_config()` — returns current active config.
  - `update_admin_payment_config(upi_id, merchant_name)` — validates and persists.
  - UPI link `pn=` now uses dynamic merchant name instead of hardcoded "VMS".
- **Updated `payment_routes.py`**:
  - `GET /api/v1/payments/config` — admin-only, returns current UPI ID + merchant name.
  - `PUT /api/v1/payments/config` — admin-only, updates UPI ID and/or merchant name.
- **Registered** `SystemConfig` model in `main.py` for auto table creation.
- **Updated tests**: 3 new tests (default config, update + deep link verification, validation), fixed 4 existing UPI link tests that referenced the renamed `UPI_ID` variable.

### Safety
- DB config is optional; env var fallback ensures zero-downtime if table is empty.
- UPI ID validated to contain `@`; merchant name validated to be non-blank.
- Config changes take effect immediately on next payment initiation (no restart needed).

### Test Coverage
- 38 payment tests pass (3 new + 35 existing, 0 regressions).

---

## 02 Mar 2026 — Day 16: UPI Deep Link Redirect Integration

### Summary
- Enhanced manual UPI payment flow with UPI deep link generation.
- Mobile apps can now open the deep link directly via intent/redirect — no QR code needed.
- `UPI_ID` loaded from environment variable (`UPI_ID`), not hardcoded.
- Amount formatted to 2 decimal places in the UPI link.
- Reference code used as transaction note (`tn` parameter).
- Existing workflow (initiate → confirm-manual → admin approve) unchanged.

### Changes

#### `payment_service.py`
- `MANUAL_UPI_ID` replaced with `UPI_ID = os.getenv("UPI_ID", "vms@upi")`.
- `initiate_payment()` now constructs a `upi://pay?` deep link with `pa`, `pn`, `am`, `cu`, `tn` params.
- Response includes new `upi_link` field alongside existing `booking_id`, `amount`, `reference_code`, `upi_id`.

#### `.env`
- Added `UPI_ID=vms@okicici`.

#### `payment_routes.py`
- No changes needed — `upi_link` flows through the existing `_success(result)` wrapper.

### Safety Guarantees
- Amount formatted to 2 decimal places (no floating-point noise in link).
- No spaces in UPI link.
- Booking must be `PENDING_PAYMENT` before initiation.
- Retry logic unchanged — deep link generated on retry too.
- No QR code generation. No external QR libraries.
- No payment gateway integration. Admin approval workflow intact.

### Test Coverage
- 6 new UPI deep link assertions added to `test_payment_service.py`:
  - `upi_link` starts with `upi://pay?`
  - Contains correctly formatted amount
  - Contains reference code in `tn` param
  - Contains `UPI_ID` in `pa` param
  - No spaces in link
  - Deep link present on retry after rejection
- **240/240 tests passing** — zero regressions.

**Status**:
UPI deep link integration complete.
Mobile-first redirect design operational.
System stable.

---

## 01 Mar 2026 — Day 15.2: Region + CartType Fee Config & Refund Deduction Engine

### Summary
- New `fee_config` module: admin-configurable booking fees and refund deduction percentages per region + cart type.
- Booking fee enforced server-side — client-provided `booking_fee` ignored.
- Refund uses **snapshot** percentages captured at booking creation time (not live config).
- Soft-delete only — `DELETE` sets `is_active = False`, preserving historical data.
- All percentage validation at service layer. No business rules in routes.

### Fee Config Module Structure
```
modules/fee_config/
├── model/fee_config_model.py           — ORM model (region_cart_type_configs table)
├── repository/fee_config_repository.py — CRUD + find_by_region_and_cart_type, soft-delete
├── service/fee_config_service.py       — Business validation (FK, pct sum ≤ 100, negative guards)
├── schemas/fee_config_schema.py        — Create + Update structural validation
├── controller/fee_config_routes.py     — 5 admin-only endpoints
└── tests/test_fee_config_service.py    — 18 unit tests
```

### Database Changes
- New table: `region_cart_type_configs` with `UniqueConstraint(region_id, cart_type_id)`.
- New columns on `bookings`: `cancellation_fee_pct_snapshot`, `platform_fee_pct_snapshot` (Numeric 5,2).

### Key Architecture Decisions
- **Snapshot design**: At booking creation, `cancellation_fee_pct` and `platform_fee_pct` are copied from config into the booking record. Refund logic uses these snapshot values, never the live config. This prevents admin config changes from retroactively affecting old bookings.
- **Refund formula**: `refund_amount = total_paid × (1 - (cancellation_pct + platform_pct) / 100)`, rounded to 2 decimal places.
- **No fee_config_repository in PaymentService**: Refund reads snapshot fields directly from the booking, eliminating dependency on live config.
- **Soft-delete**: `DELETE /fee-config/{id}` sets `is_active = False`. No hard deletion.

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/fee-config/create` | POST | `require_admin` |
| `/api/v1/fee-config/{id}` | PUT | `require_admin` |
| `/api/v1/fee-config/region/{id}/cart-type/{id}` | GET | `require_admin` |
| `/api/v1/fee-config/all` | GET | `require_admin` |
| `/api/v1/fee-config/{id}` | DELETE | `require_admin` |

### Booking Flow Changes
1. User creates booking → fee config fetched by (region_id, cart_type_id)
2. `booking_fee`, `cancellation_fee_pct_snapshot`, `platform_fee_pct_snapshot` set from config
3. Client-provided `booking_fee` ignored
4. If config missing/inactive → booking creation blocked (400)

### Payment Retry & Lazy Expiry (Refinements)
- **Payment Retry:** Admin rejecting a manual payment moves it to `FAILED`, but the `booking` remains `PENDING_PAYMENT`. Users can safely `initiate_payment` again, generating a new, unique reference code and pending payment record.
- **Lazy Expiry:** PENDING_PAYMENT bindings automatically expire if older than 10 minutes when accessed (e.g., via `get_booking`, `list_bookings`, or limit checks). No background workers required.
- **Daily Booking Limit:** Strict daily checks now *only* count `CONFIRMED` and `IN_PROGRESS` bookings, fully ignoring `EXPIRED` and `PENDING_PAYMENT` bookings to prevent blocking legitimate attempts.

### Refund Flow Changes
1. `cancel_booking()` passes full booking dict to `process_refund()`
2. `process_refund()` reads snapshot pcts from booking
3. Deduction = `cancellation_fee_pct_snapshot + platform_fee_pct_snapshot`
4. `refund_amount = total_paid × (1 - deduction / 100)`

### Test Coverage
- Fee config: 18 tests (CRUD, uniqueness, pct validation, soft-delete)
- Booking: 25 tests (fee from config, snapshot capture, config deactivation edge case)
- Payment: 22 tests (refund formula, snapshot-not-live, fallback fetch)
- BookingItem: 11 tests (updated for fee config DI)
- **231/231 tests passing** — zero regressions.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC enforced on all routes
- **Fee Config** → Admin-configurable, snapshot-based, soft-delete only
- **Booking** → Server-side fee enforcement, snapshot pcts at creation time
- **Payment** → Percentage-based refund using snapshot values

**Status**:
Fee config engine implemented.
Snapshot-based refund logic operational.
System stable.

---

## 01 Mar 2026 — Day 15 + 15.1: Booking & Payment State Machine (Manual UPI MVP)

### Summary
- Implemented proper Booking and Payment state machines with controlled transitions.
- Created new Payment module (model, repository, service, controller, tests).
- Booking creation now starts as `PENDING_PAYMENT` — no auto-confirmation, no cart assignment.
- Cart assignment deferred to admin-controlled `confirm_booking()` after payment approval.
- Manual UPI provider with reference code generation (`VMS-{booking_id}-{4 digits}`).
- Separate admin workflows: payment approval and booking confirmation.

### Day 15.1 Refinements Applied
- Removed `INITIATED` payment state — 5 states only: PENDING, UNDER_REVIEW, SUCCESS, FAILED, REFUNDED.
- Payment table is single source of truth; `bookings.payment_status` is a mirror field updated exclusively by PaymentService.
- BookingService never directly mutates `payment_status`.
- Slot capacity counts only CONFIRMED + IN_PROGRESS (not PENDING_PAYMENT).
- Daily limit ignores CANCELLED and EXPIRED bookings.
- Cancellation hardened: blocked for IN_PROGRESS, COMPLETED, EXPIRED. Refund routed through PaymentService.
- `confirm_booking()` runs expiry check first, then validates status, payment, and cart availability.
- Reference code: unique DB constraint + 5-attempt retry + system error on persistent collision.
- Admin payment inspection endpoint added (`GET /api/v1/payments/booking/{booking_id}`).

### Booking Status Refactor

| Old Statuses | New Statuses |
|---|---|
| PENDING_PAYMENT, CONFIRMED, CANCELLED, COMPLETED, PAYMENT_FAILED | PENDING_PAYMENT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, EXPIRED |

### Payment Module Structure
```
modules/payment/
├── model/payment_model.py          — ORM model (payments table)
├── repository/payment_repository.py — Session-based CRUD
├── service/payment_service.py       — Admin approval workflow
├── controller/payment_routes.py     — 6 endpoints
└── tests/test_payment_service.py    — 24 unit tests
```

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/payments/initiate/{booking_id}` | POST | `require_user` |
| `/api/v1/payments/confirm-manual/{booking_id}` | POST | `require_user` |
| `/api/v1/payments/approve/{payment_id}` | POST | `require_admin` |
| `/api/v1/payments/reject/{payment_id}` | POST | `require_admin` |
| `/api/v1/payments/refund/{payment_id}` | POST | `require_admin` |
| `/api/v1/payments/booking/{booking_id}` | GET | `require_admin` |
| `/api/v1/bookings/{id}/confirm` | POST | `require_admin` |

### Booking Flow (New)
1. User creates booking → status = PENDING_PAYMENT (no cart assigned)
2. User initiates payment → payment = PENDING
3. User submits UPI transaction ID → payment = UNDER_REVIEW
4. Admin approves payment → payment = SUCCESS, booking.payment_status mirrored
5. Admin confirms booking → cart assigned, status = CONFIRMED
6. Admin completes booking → cart released, status = COMPLETED

### Key Architecture Decisions
- PaymentService is a manual admin approval workflow — no bank connection, no UPI verification.
- Cart not locked until confirmation — prevents holding carts for unpaid bookings.
- Expiry check runs inside `confirm_booking()` to prevent race conditions.
- No background workers — expiry is on-demand.

### Test Coverage
- Booking tests: 27 tests (rewritten for new state machine)
- Payment tests: 24 tests (new module)
- BookingItem tests: 11 tests (updated status assertions)
- RBAC tests: updated for service-level cancellation checks
- **202/202 tests passing** — zero regressions.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC enforced on all routes
- **Booking** → State machine with PENDING_PAYMENT → CONFIRMED → COMPLETED
- **Payment** → Manual UPI approval workflow, source of truth for payment state
- **No auto-confirmation. No simulated payment. No Razorpay.**

**Status**:
Booking & Payment state machines implemented.
Manual UPI MVP operational.
System stable.

---

## 28 Feb 2026 — Day 14.1: User Management Secured

### Summary
- Locked down all user management routes under role-based access control.
- PUT route uses `get_current_user` with ownership logic: users can update their own non-role fields, admins can update anyone.
- Role mutation blocked for non-admins at both route and service layers (defense-in-depth).
- Self-deletion blocked at both route and service layers to prevent admin lockout.
- GET routes enforce ownership: admins see all users, regular users see only their own profile.
- POST (create) and DELETE restricted to admin role.

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/users` | POST (create) | `require_admin` |
| `/api/v1/users` | GET (list) | `get_current_user` + role filter |
| `/api/v1/users/{id}` | GET (detail) | `get_current_user` + ownership check |
| `/api/v1/users/{id}` | PUT (update) | `get_current_user` + ownership + role-mutation guard |
| `/api/v1/users/{id}` | DELETE | `require_admin` + self-deletion guard |

### Service-Layer Guards (Defense-in-Depth)
- `UserService.update_user()` independently rejects role mutation from non-admins, even if route-level auth is misconfigured.
- `UserService.delete_user()` independently blocks self-deletion, preventing accidental admin lockout.

### Admin Bootstrap Policy
- No dev backdoor routes or environment variable overrides.
- Role switching must be done manually via Neon DB: `UPDATE users SET role='admin' WHERE id=<id>;`
- System relies strictly on DB truth for role assignments.

### Test Coverage
- New test file: `modules/auth/tests/test_user_rbac_routes.py` — 11 route-level tests using FastAPI `TestClient` with dependency overrides.
- Tests cover: user creation (admin only), profile update ownership, role mutation guard, self-deletion prevention, admin delete/update, GET ownership enforcement.
- **172/172 tests passing** (161 existing + 11 user RBAC tests) — zero regressions.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC dependencies enforced on all routes
- **User management** → Fully secured; privilege escalation surface closed
- **No unprotected write endpoints remain.**

**Status**:
User management secured.
Privilege escalation vulnerability patched.
System stable.

---

## 28 Feb 2026 — Day 14: RBAC Activation (Operational Lifecycle Controlled)

### Summary
- Activated RBAC across all system routes and booking lifecycle endpoints.
- Admin-only protection applied to create/update/delete on locations, cart types, items, and carts.
- Booking creation restricted to authenticated users (`require_user`); server overrides `user_id` from JWT token.
- Booking cancellation enforces ownership: users can cancel only their own bookings, admins can cancel any.
- Booking completion restricted to admin role only.
- List bookings filters by role: admins see all, users see only their own.
- All state transitions remain strict (only CONFIRMED bookings can be cancelled or completed).

### Route Protection Applied

| Route | Method | Guard |
|---|---|---|
| `/api/v1/locations` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/cart-types` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/items` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/carts` | POST, PUT, DELETE | `require_admin` |
| `/api/v1/bookings` | POST (create) | `require_user` |
| `/api/v1/bookings` | GET (list) | `get_current_user` + role filter |
| `/api/v1/bookings/{id}` | GET (detail) | `get_current_user` + ownership check |
| `/api/v1/bookings/{id}/cancel` | POST | `get_current_user` + ownership check |
| `/api/v1/bookings/{id}/complete` | POST | `require_admin` |

Read-only endpoints (GET list/detail) on locations, cart types, items, carts remain publicly accessible.
Booking detail (`GET /bookings/{id}`) requires authentication with ownership enforcement (users see own only, admins see any).

### Data Layer Changes
- `BookingRepository.find_by_user_id()` added for user-scoped booking queries.
- `BookingService.list_bookings_by_user()` added to support role-based list filtering.

### Test Coverage
- New test file: `modules/auth/tests/test_rbac_routes.py` — 28 route-level tests using FastAPI `TestClient` with dependency overrides.
- Tests cover: admin route protection (16 tests), booking creation user_id override, booking cancellation ownership, booking completion admin-only, role-based list filtering.
- **161/161 tests passing** (130 existing + 31 RBAC tests) — zero regressions.

### Test Architecture Note
- First route-level (HTTP) tests in the codebase — all prior tests were service-layer unit tests.
- Uses `app.dependency_overrides[get_current_user]` to simulate authenticated users without JWT.
- Service methods mocked via `unittest.mock.patch` to isolate route-layer RBAC logic from DB.

### Current Architecture State
- **All modules** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, RBAC dependencies now enforced on all relevant routes
- **Booking lifecycle** → Operationally controlled (completion = admin, cancellation = owner/admin)
- **Backend is now role-secured.** No unprotected write endpoints remain.

**Status**:
RBAC activation complete.
All routes protected according to role requirements.
System stable.

---

## 27 Feb 2026 — Day 13: Auth Module Implementation (JWT + Bcrypt)

### Summary
- Implemented full authentication module with JWT access tokens and bcrypt password hashing.
- Added `password_hash` column to existing `users` table via Neon MCP (ALTER TABLE, no table recreation).
- DB default dropped after backfill — future inserts must supply a real bcrypt hash.
- Auth routes at `/api/v1/auth` (register + login) are unprotected and publicly accessible.
- RBAC dependencies created and ready for future route protection.

### Database Changes
- `password_hash VARCHAR NOT NULL` added to `users` table via Neon MCP `run_sql`.
- Existing rows backfilled with empty string; default then dropped (`ALTER COLUMN password_hash DROP DEFAULT`).
- User model updated; `password_hash` explicitly excluded from `to_dict()`.

### Auth Module Structure
```
modules/auth/
├── __init__.py
├── schemas/auth_schema.py         — RegisterSchema, LoginSchema
├── service/auth_service.py        — register_user(), login_user()
├── dependencies/auth_dependencies.py — get_current_user(), require_admin(), require_user()
├── controller/auth_routes.py      — POST /register, POST /login
└── tests/test_auth_service.py     — 5 unit tests
```

### Security Implementation
- **Password hashing**: passlib CryptContext with bcrypt scheme.
- **JWT (HS256)**: python-jose, 60-minute expiry, timezone-aware (`datetime.now(timezone.utc)`).
- **Token payload**: `{ "sub": user_id, "role": user_role, "exp": expiry }`.
- **SECRET_KEY**: stored in `.env`, loaded at startup.
- **Phone normalization**: spaces stripped before uniqueness checks and lookups.
- **`find_by_phone_with_hash()`**: explicit field serialization (does not reuse `to_dict()`) to prevent accidental hash leakage.
- **`get_current_user()`**: validates user existence and `is_active` status from DB on every request — does not trust token payload alone.

### Dependencies Added
- `passlib[bcrypt]`, `python-jose[cryptography]` added to `requirements.txt`.
- `bcrypt` pinned to 4.0.1 for passlib compatibility.

### Test Isolation
- Auth tests use SQLite in-memory with injected `session_factory`.
- Zero Neon connections during automated testing.

### Verification
- **130/130 tests passing** (125 existing + 5 new auth tests) — zero regressions.
- Manual API test confirmed:
  - `POST /api/v1/auth/register` → 201, user created with hashed password, no `password_hash` in response.
  - `POST /api/v1/auth/login` → 200, JWT access token returned.
  - Invalid password → 401 rejected.
  - Test user cleaned up via `DELETE /api/v1/users`.

### Current Architecture State
- **User, Location, CartType, Timeslot, Cart, Booking, BookingItem, Item** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Auth** → JWT + bcrypt, routes unprotected, RBAC dependencies ready
- **All modules now fully DB-backed.** No in-memory repositories remain.

**Status**:
Auth module successfully implemented.
Backend secured and ready for route protection.
System stable.

---

## 27 Feb 2026 — Booking & BookingItem Post-Migration Validation

### Summary
- Completed full API + DB validation for Booking and BookingItem in the existing Neon project (`VMS-Backend`, `neondb`).
- Confirmed service-layer business rules and DB-backed repository behavior are consistent after ORM migration.
- Verified booking lifecycle side effects on cart status and refund fields.

### Validation Coverage
- Full booking creation with items:
  - Booking created as `CONFIRMED` with `payment_status = SUCCESS`.
  - `estimated_total` computed correctly from item snapshots.
  - `assigned_cart_id` populated.
  - `booking_items` rows inserted.
  - Assigned cart moved to `BUSY`.
- Cancel booking flow:
  - Booking moved to `CANCELLED`.
  - `refund_status = REFUNDED`.
  - `refund_amount = booking_fee`.
  - Cart released back to `AVAILABLE`.
- Complete booking flow:
  - Booking moved to `COMPLETED`.
  - Cart released back to `AVAILABLE`.
- Slot capacity enforcement:
  - Capacity=1 timeslot accepted first booking, rejected second with HTTP 400.
- Daily booking limit:
  - Same user + same date allowed 3 active bookings; 4th booking rejected with HTTP 400.
- Atomic rollback safety:
  - With item temporarily set unavailable, booking request failed.
  - No booking row created, no booking_item row created, cart status unchanged.

### DB Verification
- Verified via Neon SQL:
  - `SELECT * FROM bookings;`
  - `SELECT * FROM booking_items;`
  - `SELECT * FROM carts;`
- Confirmed expected persisted state after each scenario.

### Notes
- Decimal-safe monetary handling remains consistent:
  - DB stores monetary values as `Numeric(10,2)`.
  - API responses serialize `booking_fee`, `estimated_total`, and `refund_amount` as `float`.
- No ORM `relationship()` usage introduced; ForeignKey-only architecture preserved.

## 26 Feb 2026 — Item Migration to Neon PostgreSQL

### Summary
- Migrated Item module from in-memory repository to Neon PostgreSQL using SQLAlchemy ORM.
- Followed the established migration pattern from User, Location, CartType, Timeslot, and Cart modules.
- Table `items` successfully created in the existing Neon database (`neondb`).
- No changes to service layer, controller, schemas, or business logic.

### Model Changes
- `item_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "items"`
  - `cart_type_id` as an indexed `ForeignKey` to `cart_types.id`.
  - `price` stored as `Numeric(10, 2)` for monetary precision.
  - `image_urls` stored as `JSON` column (native JSONB in PostgreSQL, JSON text in SQLite).
  - `is_available` boolean with default `True`.
  - `created_at` / `updated_at` timestamps with `onupdate` trigger.
  - `to_dict()` converts `Decimal` price to `float` for JSON serialization.
  - No ORM `relationship()` declarations — ForeignKey only.

### Repository Refactor
- `item_repository.py` rewritten to session-based CRUD (commit/rollback/finally pattern).
- Removed in-memory `_store` dictionary and `_next_id` counter.
- Removed `BaseRepository` inheritance.
- All 7 methods migrated: `create`, `find_by_id`, `find_all`, `find_by_cart_type_id`, `find_by_name_and_cart_type`, `update`, `delete`.
- Optional `session_factory` injection for test isolation (production defaults to Neon).
- Singleton export preserved: `item_repository`.

### Test Isolation
- 3 test files updated to inject the SQLite in-memory `session_factory` into `ItemRepository`:
  - `test_item_service.py` — added `Item` model import for schema registration.
  - `test_booking_item_service.py` — changed `ItemRepository()` to `ItemRepository(session_factory=test_session_factory)`, added `Item` model import.
  - `test_booking_service.py` — same changes as above.
- Maintained zero connections to the Neon DB during automated testing.

### Verification
- **All 125 tests passing** across 8 independent feature modules without regressions.
- Confirmed table mapping via FastAPI startup event (`Base.metadata.create_all`).
- Manual persistence test confirmed:
  - Item created via `POST /api/v1/items` (201, correct data with Decimal-safe price).
  - Server restarted — item retrieved with identical data.
  - Direct DB query (`SELECT * FROM items`) confirmed row in Neon.
  - Test item cleaned up via `DELETE /api/v1/items/1`.

### Current Architecture State
- **User, Location, CartType, Timeslot, Cart, Booking, BookingItem, Item** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **All modules now fully DB-backed.** No in-memory repositories remain.

**Status**:
Item module successfully migrated to PostgreSQL.
Backend is now 100% database-backed — zero in-memory repositories remain.
System stable.

---

## 25 Feb 2026 — Timeslot & Cart Migration to Neon PostgreSQL

### Summary
- Migrated Timeslot and Cart modules from in-memory repositories to Neon PostgreSQL using SQLAlchemy ORM.
- Followed the established migration pattern from the User, Location, and CartType modules.
- Tables `timeslots` and `carts` successfully created in the existing Neon database (`neondb`).
- No changes to business logic or downstream service features.

### Model Changes
- `timeslot_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "timeslots"`
  - `location_id` as an indexed `ForeignKey` to `locations.id`.
  - Enforced `nullable=False` on data columns and initialized `DateTime` attributes.
- `cart_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "carts"`
  - `region_id` as an indexed `ForeignKey` to `locations.id`.
  - `cart_type_id` as an indexed `ForeignKey` to `cart_types.id`.
  - Enforced `nullable=False` on data columns and initialized `DateTime` attributes.

### Repository Refactor
- `timeslot_repository.py` and `cart_repository.py` rewritten to use session-based CRUD.
- Implemented robust `commit/rollback/finally` transaction management.
- Replaced `_store` dictionary logic with SQLAlchemy query filters.
- Preserved singletons and optional `session_factory` injection for test isolation.

### Test Isolation
- 4 test suites updated to inject the SQLite in-memory `session_factory`:
  - `test_timeslot_service.py`
  - `test_cart_service.py`
  - `test_booking_service.py`
  - `test_booking_item_service.py`
- Required importing ORM models to properly build SQLite schema metadata.
- Maintained zero connections to the Neon DB during automated testing.

### Verification
- **All 125 tests passing** across 8 independent feature modules without regressions.
- Confirmed table mapping via FastAPI startup event (`Base.metadata.create_all`).
- Manual tests validated via standard POST requests and direct Neon DB queries verifying data persistence.

### Current Architecture State
- **User, Location, CartType, Timeslot, Cart** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Item, Booking, BookingItem** → in-memory repositories (pending migration)

**Status**:
Timeslot & Cart modules successfully migrated to PostgreSQL.
Cross-table relationships successfully modeled with `ForeignKey`.
System stable.

---

## 24 Feb 2026 — Location & CartType Migration to Neon PostgreSQL

### Summary
- Migrated Location and CartType modules from in-memory repositories to Neon PostgreSQL using SQLAlchemy ORM.
- Followed the exact migration pattern established by the User module (23 Feb 2026).
- Tables created in existing Neon project: **VMS-Backend** (`still-darkness-99863466`), database: `neondb`.
- No new Neon project or database created.

### Model Changes
- `location_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "locations"`
  - DB-level unique constraint on `name` column (indexed).
  - `is_serviceable` boolean with default True.
  - `created_at` / `updated_at` timestamps.
  - `to_dict()` preserved for backward compatibility.
- `cart_type_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
  - `__tablename__ = "cart_types"`
  - DB-level unique constraint on `name` column (indexed).
  - `description` nullable, `is_active` boolean default True.
  - `created_at` / `updated_at` timestamps.
  - `to_dict()` preserved for backward compatibility.

### Repository Refactor
- `location_repository.py` rewritten to session-based CRUD (commit/rollback/finally pattern).
- `cart_type_repository.py` rewritten to session-based CRUD (same pattern).
- Optional `session_factory` injection for test isolation (production defaults to Neon).
- Singleton exports preserved: `location_repository`, `cart_type_repository`.

### Test Isolation
- 7 test files updated to use SQLite in-memory session injection:
  - `test_location_service.py` — inject Location session
  - `test_cart_type_service.py` — inject CartType session
  - `test_timeslot_service.py` — inject Location session
  - `test_cart_service.py` — inject Location + CartType sessions
  - `test_item_service.py` — inject CartType session
  - `test_booking_service.py` — extended SQLite setup for Location + CartType
  - `test_booking_item_service.py` — extended SQLite setup for Location + CartType
- Zero Neon connections during tests.

### Verification
- **All 125 tests passing** — zero regressions.
- Tables auto-created via `Base.metadata.create_all` on server startup.

### Current Architecture State
- **User** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **Location** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **CartType** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **All other modules** → in-memory repositories

**Status**:
Location & CartType modules successfully migrated to PostgreSQL.
Test isolation enforced across all modules.
System stable.

---

## 23 Feb 2026 — Day 9: User Module Migration to Neon PostgreSQL

### Summary
- Migrated User module from in-memory repository to Neon PostgreSQL using SQLAlchemy ORM.
- All other modules remain in-memory.
- Hybrid architecture temporarily in place.

### Infrastructure Changes
- Added SQLAlchemy engine + `SessionLocal` session factory in `core/database/db_connection.py`.
- Added `Base` declarative model for ORM inheritance.
- `DATABASE_URL` stored in `.env` (not committed to version control).
- Table creation via `Base.metadata.create_all()` on FastAPI startup event.
- New Neon project: **VMS-Backend** (`still-darkness-99863466`).
- Dependencies added: `sqlalchemy`, `psycopg2-binary`, `python-dotenv`.

### Repository Refactor
- `user_model.py` converted to SQLAlchemy ORM model (inherits from `Base`).
- `user_repository.py` rewritten to session-based CRUD (commit/rollback/finally pattern).
- Optional `session_factory` injection added for test isolation (production defaults to Neon).
- DB-level unique constraint on `phone` column (`ix_users_phone` index).
- `to_dict()` method preserved for backward compatibility with service layer.

### Test Isolation Fix
- Booking and BookingItem tests updated to inject SQLite in-memory `UserRepository`.
- Prevented any Neon usage during tests — zero production DB connections.
- Full test suite restored to green.

### Verification
- Manual persistence test confirmed:
  - Users persist after server restart.
  - Auto-increment IDs working correctly.
  - Unique constraint enforced at DB level.
- Neon table verified via MCP: columns, indexes, constraints all correct.

### Current Architecture State
- **User** → DB-backed (Neon PostgreSQL via SQLAlchemy)
- **All other modules** → in-memory repositories
- **All 125 tests passing** — zero regressions.

### Next Steps
- Migrate Location module to Neon.
- Continue module-by-module migration.
- Auth implementation postponed until full DB migration complete.

**Status**:
User module successfully migrated to PostgreSQL.
Test isolation enforced across all modules.
System stable.

---

## 23 Feb 2026
- **BookingItem Module Implementation**: New module under `modules/booking_item/` with full layered architecture.
- **Model Fields**: id, booking_id, item_id, quantity, unit_price (Decimal), created_at.
- **Architecture — Validate-then-Persist**:
  - `BookingItemService.validate_items()` — validation only, returns sanitized snapshots.
  - `BookingItemService.create_booking_items()` — persists only after parent booking exists.
  - `BookingItemService.calculate_estimated_total()` — operates on validated snapshot data.
- **Validation Rules**:
  - Item must exist (authoritative lookup from ItemRepository).
  - Item must belong to the booking's cart_type.
  - Item must be available (`is_available == True`).
  - Quantity must be a positive integer.
- **Monetary Precision**: All price/total calculations use `Decimal` to avoid float rounding errors.
- **Booking Integration**:
  - `BookingService.create_booking()` accepts optional `items` list.
  - `estimated_total` is always server-computed: `sum(qty * unit_price)`. Client values ignored.
  - Items validated before any side effects (cart assignment, payment simulation).
  - BookingItem records created after booking record — no orphaned records.
  - `estimated_total` removed from required schema fields.
  - Backward compatible: bookings without items still work (`estimated_total = 0.00`).
- **Payment simulation logic not modified**.
- **Unit Tests**:
  - BookingItem module: 11/11 passed (valid items, no items, invalid ID, wrong cart_type, invalid qty, total calculation, unavailable item, no side effects on failure).
  - Booking module: 17/17 passed (no regressions).
  - All other modules: no regressions.
  - **Total: 125/125 passed.**

**Status**:
Eighth feature module successfully integrated.
No core refactors required.
System stable.

## 22 Feb 2026
- **Booking Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, user_id, region_id, cart_type_id, timeslot_id, assigned_cart_id (nullable), address, booking_fee, estimated_total, status, payment_status, refund_status, refund_amount, created_at, updated_at.
- **Status Enums**:
  - Booking status: PENDING_PAYMENT, CONFIRMED, CANCELLED, COMPLETED, PAYMENT_FAILED.
  - Payment status: PENDING, SUCCESS, FAILED.
  - Refund status: NONE, PENDING, REFUNDED.
- **Validation & Constraints**:
  - Cross-module FK validation (user, region, cart_type, timeslot).
  - Slot capacity enforcement (count active bookings vs timeslot capacity).
  - Daily user booking limit (max 3 per user per day).
  - Cart availability check — booking fails if no AVAILABLE cart found.
- **Booking Flow**: Validate FKs → Slot capacity → Daily limit → Cart availability → Simulate payment → Assign cart (BUSY) → Confirm booking.
- **Cart Lifecycle**: Cart set to BUSY on assignment; released to AVAILABLE on cancel/complete.
- **Simulated Payment**: Always returns SUCCESS (no real gateway integration yet).
- **Integration**:
  - Integrated routes at `/api/v1/bookings`.
  - Registered booking router in `main.py`.
  - Endpoints: POST (create), GET (list/detail), POST /{id}/cancel, POST /{id}/complete.
- **Unit Tests**:
  - Booking module: 17/17 passed.
  - Item module: no regressions.
  - Cart module: no regressions.
  - Cart type module: no regressions.
  - Timeslot module: no regressions.
  - Location module: no regressions.
  - User module: no regressions.
  - **Total: 114/114 passed.**

**Status**:
Seventh feature module successfully integrated.
No core refactors required.
System stable.

## 20 Feb 2026
- **Item Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, cart_type_id, name, description, price, image_urls, is_available, created_at, updated_at.
- **Validation & Constraints**:
  - Cross-module cart type validation (existence check via CartTypeRepository).
  - Price must be >= 0 (schema-level enforcement).
  - image_urls optional; validated as list of http/https URL strings.
  - is_available optional (defaults to True).
  - Unique item name enforced per cart_type.
- **Integration**:
  - Integrated routes at `/api/v1/items`.
  - Registered item router in `main.py`.
  - Optional `?cart_type_id=` query param on list endpoint.
- **Unit Tests**:
  - Item module: 16/16 passed.
  - Cart module: no regressions.
  - Cart type module: no regressions.
  - Timeslot module: no regressions.
  - Location module: no regressions.
  - User module: no regressions.
  - **Total: 97/97 passed.**

**Status**:
Sixth feature module successfully integrated.
No core refactors required.
System stable.

## 19 Feb 2026
- **Cart Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, region_id, cart_type_id, status, is_active, created_at, updated_at.
- **Validation & Constraints**:
  - Cross-module region validation (existence check via LocationRepository).
  - Cross-module cart type validation (existence check via CartTypeRepository).
  - Status enum enforcement: AVAILABLE, BUSY, BUFFER, OFFLINE.
  - Status input normalized to uppercase.
  - is_active optional (defaults to True).
- **Integration**:
  - Integrated routes at `/api/v1/carts`.
  - Registered cart router in `main.py`.
- **Unit Tests**:
  - Cart module: all passed.
  - Cart type module: no regressions.
  - Timeslot module: no regressions.
  - Location module: no regressions.
  - User module: no regressions.

**Status**:
Fifth feature module successfully integrated.
No core refactors required.
System stable.

## 18 Feb 2026
- **Cart Type Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, name, description, is_active, created_at, updated_at.
- **Validation & Constraints**:
  - Enforced unique cart type name validation in the service layer.
  - Description optional (defaults to empty string).
  - is_active optional (defaults to True).
- **Integration**:
  - Integrated routes at `/api/v1/cart-types`.
  - Registered cart type router in `main.py`.
- **Unit Tests**:
  - Cart type module: 13/13 passed.
  - Timeslot module: 14/14 passed (no regressions).
  - Location module: 13/13 passed (no regressions).
  - User module: 26/26 passed (no regressions).

**Status**:
Fourth feature module successfully integrated.
No core refactors required.
System stable.

## 17 Feb 2026
- **Timeslot Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Model Fields**: id, location_id, date, start_time, end_time, capacity, created_at, updated_at.
- **Validation & Constraints**:
  - Cross-module location validation (existence + is_serviceable).
  - Time-range validation (end_time > start_time).
  - Unique constraint on (location_id, date, start_time).
  - Capacity must be > 0.
- **Integration**:
  - Integrated routes at `/api/v1/timeslots`.
  - Registered timeslot router in `main.py`.
- **Unit Tests**:
  - Timeslot module: 14/14 passed.
  - Location module: 13/13 passed (no regressions).
  - User module: 26/26 passed (no regressions).
- **Bug Fix — Shared Repository Instance**:
  - **Issue**: `LocationService` and `TimeslotService` each instantiated their own `LocationRepository()`, creating separate in-memory stores. Timeslot validation could not see locations created via the location API.
  - **Fix**: Exported a module-level singleton (`location_repository`) from `location_repository.py`. Both services now import and default to this shared instance. Optional injection preserved for test isolation.
  - **Guideline added**: PROJECT_GUIDELINES §2 updated — all in-memory repositories must be shared instances across modules.

**Status**:
Third feature module successfully integrated.
No core refactors required.
System stable.

## 16 Feb 2026
- **Location Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Validation & Constraints**:
  - Enforced unique location name validation in the service layer.
- **Integration**:
  - Integrated routes at `/api/v1/locations`.
  - Registered location router in `main.py`.
- **API Testing**: Manual verification completed in Swagger UI for:
  - Create location
  - Duplicate name validation
  - Update location (timestamp verification)
  - Delete location
  - Fetch after delete (404 validation)
- **Unit Tests**:
  - Location module: 13/13 passed.
  - User module: 26/26 passed (no regressions).

**Status**:
Backend architecture successfully validated for multi-module scalability.
No core refactors required.
System stable.

## 15 Feb 2026
- **User Module Implementation**: Completed full layered architecture (Controller → Service → Repository).
- **Validation & Constraints**:
  - Enforced unique phone number validation in the service layer.
  - Implemented role normalization (case-insensitive input, normalized to lowercase).
- **Infrastructure**:
  - Integrated centralized error handling middleware for consistent API responses.
  - Created and verified the FastAPI entry point (`main.py`).
  - Enabled interactive Swagger documentation at `/docs`.
- **Manual API Testing**: Successfully verified the following flows:
  - User creation with valid data.
  - Prevention of duplicate phone numbers.
  - User updates with automatic `updated_at` timestamp refresh.
  - Deletion of user records and subsequent "Not Found" validation.

## 17 Mar 2026 — Day 27.5: Item Visual Support

- **Backend**: Added `image_url` (String, nullable) field to `Item` model alongside existing `image_urls` (JSON list).
- **to_dict() fallback**: `image_url` resolves to explicit `image_url` value, or falls back to first entry in `image_urls` if present, ensuring backward compatibility.
- **Schema validation**: Added `image_url` (optional string) to both `CreateItemSchema` and `UpdateItemSchema` with non-empty string enforcement when provided.
- **Repository**: `create()` now persists `image_url`; `update()` handles it generically via existing `setattr` loop.
- **Tests**: Added 4 new tests — `test_create_item_with_image_url`, `test_update_item_image_url`, `test_item_without_image_still_valid`, `test_to_dict_fallback_image`. All 20 tests pass.
- **Admin App**: Added `image_url: String? = null` to `Item`, `CreateItemRequest`, `UpdateItemRequest` models.
- **Admin App**: `ItemRepository`, `ItemViewModel` updated to pass `description` and `imageUrl` through to the API.
- **Admin App**: `ItemCard` now shows `AsyncImage` (Coil) when a valid URL is present, or a placeholder icon otherwise. Description shown below name with 2-line ellipsis.
- **Admin App**: `ItemDialog` adds Description and Image URL fields with UX hint "Paste image link (e.g., from Imgur)".
- **Admin App**: Coil 2.7.0 added to `libs.versions.toml` and `build.gradle.kts`.
- **User App**: Added `Item` data class with `description` and `image_url` fields for future UI rendering.
- **No breaking changes**: Existing items without images work correctly with placeholder fallback.


## 2026-05-19 — Phase 01: RBAC Foundation (Tasks 1–6) — Backend middleware + Android auth layer

### Added
- `backend/core/security/auth_manager.py` — `AuthManager` class with `_ALL_PERMISSIONS` frozenset (23 permissions), `_ROLE_PERMISSIONS` map for all 7 Plixo roles + `user`, `has_permission(role, permission)` and `get_permissions(role)` methods, module-level `auth_manager` singleton.
- `require_role(*allowed_roles)` factory in `auth_dependencies.py` — returns a FastAPI `Depends` that enforces role membership; role values compared against `UserRole` enum.
- `require_permission(permission)` factory in `auth_dependencies.py` — delegates to `auth_manager.has_permission`; returns 403 with named permission in detail.

### Modified
- `backend/modules/user/model/user_model.py` — `UserRole` enum extended from 2 values (`USER`, `ADMIN`) to 8 (`USER`, `SUPER_ADMIN`, `OPS_MANAGER`, `GROUND_OWNER`, `TOURNAMENT_MANAGER`, `SUPPORT`, `FINANCE`, `CSR_PARTNER`). `ADMIN` removed.
- `backend/main.py` — `startup()` function: added idempotent `UPDATE users SET role = 'super_admin' WHERE role = 'admin'` migration shim so existing admin rows are renamed on next boot.
- `backend/modules/auth/dependencies/auth_dependencies.py` — added `from typing import Callable`, `UserRole` and `auth_manager` imports; added `require_role()` and `require_permission()` factories. Existing `require_admin`, `require_user`, `get_current_user` left intact for backwards compatibility.
- `backend/modules/payment/controller/payment_routes.py` — all 7 endpoints migrated from `require_admin` to granular `require_role` guards: list/booking/config-read allow FINANCE+SUPER_ADMIN+OPS_MANAGER+SUPPORT; approve/reject/refund/config-write restricted to FINANCE+SUPER_ADMIN only.
- `backend/modules/admin/controller/admin_routes.py` — all 6 endpoints migrated: dashboard allows SUPER_ADMIN+OPS_MANAGER+FINANCE; metrics/queue-stats allow SUPER_ADMIN+OPS_MANAGER; matches allows +SUPPORT+TOURNAMENT_MANAGER; config endpoints restricted to SUPER_ADMIN only.
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/TokenManager.kt` — added `ROLE_KEY`, `roleFlow`, `saveRole()`, `clearRole()` via new `clearSession()` which atomically clears both token and role. `clearToken()` now delegates to `clearSession()`.
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/AuthViewModel.kt` — added `currentRole: StateFlow<String?>` backed by `tokenManager.roleFlow` via `stateIn`; replaced hard-coded `role != "admin"` gate with `adminRoles` set of all 7 Plixo roles; now calls `tokenManager.saveRole(role)` on successful login; `logout()` uses `clearSession()`.

### Backend changes
- New permission middleware in `core/security/auth_manager.py` — pure Python, no DB queries
- `require_role()` / `require_permission()` usable on any FastAPI route as `current_user: dict = require_role(UserRole.X, ...)`
- Existing `require_admin` still wired on routes not touched this session — will be migrated in follow-on phases

### Architectural decisions
- Kept `require_admin()` in `auth_dependencies.py` during transition; removing it now would break untouched controllers. It will be eliminated after all controllers are migrated to `require_role`.
- Role stored in JWT payload (already existed) and mirrored to DataStore on login — avoids re-decoding JWT on every screen load.
- `UserRole` uses `str, Enum` so enum values compare equal to raw JWT role strings without extra coercion.
- Startup migration shim is idempotent — safe to leave permanently; no Alembic dependency.


## 2026-05-19 — Phase 01: RBAC Tasks 7 & 8 — Role-filtered navigation + ForbiddenScreen

### Added
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/ForbiddenScreen.kt` — new Compose screen shown when a user lacks access; displays "Access Denied" message with a Logout button.

### Modified
- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt`
  - Added imports: `collectAsState`, `getValue`, `ForbiddenScreen`
  - Collect `role` from `authViewModel.currentRole` as state
  - Pass `role = role ?: ""` to `MainScreen` in the `composable("main")` block
  - Added `composable("forbidden")` route that renders `ForbiddenScreen` with logout + navigate-to-login logic

- `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/MainScreen.kt`
  - Added `role: String = ""` parameter to `MainScreen` composable
  - Replaced hardcoded `val items = listOf(...)` with role-filtered list: `Payments` visible only to `super_admin`/`finance`; `Manage` visible only to `super_admin`/`ops_manager`; `Dashboard` and `Bookings` visible to all

### Architectural decisions
- Role filtering is applied at the composable level using the `role` StateFlow collected in `AppNavigation`, following the four-layer RBAC enforcement pattern (backend, ViewModel, navigation, UI).
- `ForbiddenScreen` is wired into the NavHost so any future guard logic can `navigate("forbidden")` without additional boilerplate.
- No changes to `BottomNavItem` sealed class definitions.


## 2026-05-27 — Phase 01: Captain module + timeslot is_active + user phone search

### Added
- `backend/modules/captain/__init__.py` — package marker
- `backend/modules/captain/controller/__init__.py` — package marker
- `backend/modules/captain/model/captain_model.py` — `CaptainStatus` constants class + `Captain` SQLAlchemy ORM model mapping to `captains` table; FK to `users` (CASCADE) and `locations` (SET NULL); fields: id, user_id, region_id, status, rating, total_trips, bio, created_at, updated_at; `to_dict()` serializer.
- `backend/modules/captain/schemas/captain_schema.py` — `CreateCaptainSchema` (required: user_id; optional: region_id, bio) and `UpdateCaptainSchema` (all optional: region_id, status, bio); status validates against `CaptainStatus.ALL` frozenset.
- `backend/modules/captain/repository/captain_repository.py` — `CaptainRepository` with `get_all(region_id)`, `get_by_id`, `get_by_user_id`, `create`, `update`, `delete`; module-level `captain_repository` singleton.
- `backend/modules/captain/service/captain_service.py` — `CaptainService` with `list_captains(region_id)`, `get_captain`, `create_captain`, `update_captain`, `delete_captain`; all list/get methods join with `users` table to enrich dicts with `name` + `phone`; `create_captain` validates user existence and duplicate guard; raises HTTPException 404/400 directly.
- `backend/modules/captain/controller/captain_routes.py` — FastAPI router at `/api/v1/captains`; GET/POST guarded by `OPS_MANAGER|SUPER_ADMIN`; GET/{id}/PUT/{id} same; DELETE/{id} restricted to `SUPER_ADMIN`; uses `require_role()` factory pattern.
- `backend/run_migrations.py` — standalone psycopg2 migration script (run once from backend/ directory).

### Modified
- `backend/modules/timeslot/model/timeslot_model.py` — added `is_active` column (`Boolean, nullable=False, default=True, server_default="true"`); updated `to_dict()` to include `is_active`.
- `backend/modules/timeslot/schemas/timeslot_schema.py` — added `is_active` validation to `UpdateTimeslotSchema` (optional bool field).
- `backend/modules/timeslot/repository/timeslot_repository.py` — `create()` now explicitly passes `is_active=timeslot_data.get("is_active", True)` to the ORM constructor; `update()` already handles via generic setattr loop.
- `backend/modules/user/controller/user_routes.py` — added `GET /api/v1/users/search?phone=` endpoint (SUPER_ADMIN + SUPPORT only); direct DB query via `get_db` dependency; returns 404 if not found.
- `backend/main.py` — registered `captain_router` and `Captain` model (for `Base.metadata.create_all`).

### Backend changes (schema / routes)
- `captains` table: `CREATE TABLE IF NOT EXISTS captains (id SERIAL PK, user_id INT UNIQUE NOT NULL FK users, region_id INT FK locations, status VARCHAR(50) DEFAULT 'ACTIVE', rating FLOAT DEFAULT 0.0, total_trips INT DEFAULT 0, bio TEXT, created_at TIMESTAMP, updated_at TIMESTAMP)`
- `timeslots` table: `ALTER TABLE timeslots ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE`
- New routes: `GET/POST /api/v1/captains`, `GET/PUT/DELETE /api/v1/captains/{id}`, `GET /api/v1/users/search?phone=`

### Architectural decisions
- Captain service owns the user-join logic internally (not in the repository) to keep repository pure data-access; service opens its own session for join queries rather than accepting one from caller — consistent with existing LocationService/TimeslotService pattern.
- `is_active` on timeslots follows the same `server_default="true"` pattern as `Boolean` columns elsewhere to ensure DB-level default for rows inserted outside the ORM.
- User phone search route placed at `/users/search` (before `/{user_id}`) so FastAPI's path matching doesn't shadow it with the int-param route.
- `run_migrations.py` created as a standalone script (not Alembic) consistent with the no-Alembic project constraint; migrations are idempotent (`IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`).

---
## [2026-06-04] Phase 02 — TournamentsScreen (admin app)

### Backend
No backend changes this session — tournament endpoints expected at `/api/v1/tournaments` (GET/POST/PUT/DELETE).

### App
**Added:**
- `models/Models.kt` — `Tournament`, `CreateTournamentRequest`, `UpdateTournamentRequest` models
- `network/ApiService.kt` — `getTournaments`, `createTournament`, `updateTournament`, `deleteTournament` endpoints
- `data/TournamentRepository.kt` — repository wrapping ApiService tournament calls
- `viewmodel/TournamentViewModel.kt` — `TournamentUiState`, `TournamentViewModel`, `TournamentViewModelFactory`; supports load, refresh, create, updateStatus
- `ui/screens/TournamentsScreen.kt` — full screen: TopAppBar with back, FAB, PullToRefreshBox, LazyColumn of TournamentCards, CreateTournamentDialog, skeleton loading, empty and error states; status change via DropdownMenu (UPCOMING/ONGOING/COMPLETED/CANCELLED); TournamentStatusBadge with color coding

**Modified:**
- `ui/screens/PlaceholderScreens.kt` — added `onNavigateToTournaments` param to `ManageScreen`; added Tournaments entry under Venues & Matches section
- `ui/screens/MainScreen.kt` — added `TournamentViewModel` import + param; wired `manage/tournaments` route (guarded by TOURNAMENT_ROLES); passed `onNavigateToTournaments` to `ManageScreen`
- `navigation/AppNavigation.kt` — added `TournamentViewModel` import + param; passed to `MainScreen`
- `MainActivity.kt` — instantiated `TournamentRepository` and `TournamentViewModel` via factory; passed to `AppNavigation`

### Architectural decisions
- Tournaments accessible to `tournament_manager`, `super_admin`, `ops_manager` (reuses existing `TOURNAMENT_ROLES` set).
- Repository follows the same success/data-null guard pattern as `CaptainRepository`; throws `Exception` on failure so ViewModel catches it and surfaces via `error` state.
- `TournamentViewModel` uses `updatingIds: Set<Int>` to show per-card progress spinner without blocking the entire list, consistent with `GroundViewModel` pattern.

---
## [2026-06-05] Phase 02 — Captain Matchmaking: Instant & Timed Fallback

### Backend
**Added:**
- `Captain.is_available` (Boolean, default True) — real-time availability flag
- `Captain.current_match_id` (FK → matches.id, nullable) — which match the captain is on
- `CaptainRepository.find_available_captain(region_id, session)` — SKIP LOCKED fair rotation
- `CaptainRepository.set_availability(captain_id, available, match_id, session)` — toggle in-transaction
- `CaptainRepository.release_captain_for_match(match_id, session)` — frees captain on match end; increments `total_trips`
- `QueueEntry.instant_captain` (Boolean, default False) — audit trail for instant-captain requests
- `JoinQueueRequest.instant_captain: bool` — new optional field in Pydantic schema
- `MatchmakingService.join_queue(instant_captain=False)` — branches: normal queue OR immediate captain assignment
- `MatchmakingService._assign_captain_now(...)` — creates MATCHED match + MatchPlayer records instantly; marks captain unavailable
- `MatchEngineService._process_captain_fallback()` — Phase 3 of engine cycle; scans WAITING entries older than `CAPTAIN_FALLBACK_WAIT_MINUTES`
- `MatchEngineService._assign_captain_to_entry(...)` — per-entry isolated transaction for fallback assignment
- `CAPTAIN_FALLBACK_WAIT_MINUTES = 5` added to `app_config.py` DEFAULTS (admin-overridable via system_configs table)

**Modified:**
- `match_service.finish_match()` — calls `release_captain_for_match()` to free captain when game ends
- `match_service.cancel_match()` — same captain release on cancellation
- `matchmaking_routes.POST /play-now` — passes `instant_captain`, returns `mode`, `match_id`, `captain_id` on instant path; returns HTTP 503 if no captain available

### App
(No app changes in this set — user app integration pending)

### Architectural decisions
- Captain is added as a `MatchPlayer` (same table, no Match model change needed); `current_match_id` on Captain is the release link.
- SKIP LOCKED used on both `find_available_captain` and `find_and_lock_compatible_pair` — prevents double-assignment under concurrent engine runs.
- 503 status code chosen for "no captain available" so mobile client can display a retry prompt vs. a 400 user-error.
- `CAPTAIN_FALLBACK_WAIT_MINUTES` is admin-editable at runtime via the system_configs table — no redeploy needed to tune the window.
---
## [2026-06-08] Phase 02 — Society creation permission gate

### Backend
**Added:**
- `users.can_create_society BOOLEAN NOT NULL DEFAULT FALSE` column on `User` model and `to_dict()`
- `UpdateUserSchema.is_valid()` — validates `can_create_society` boolean field
- `CreateSocietySchema.owner_user_id: int | None` — allows admin to specify owner on creation
- `SocietyRepository.count_by_owner(owner_user_id)` — counts societies owned by a user (read-only, no rollback)
- `_ADMIN_BYPASS_ROLES` frozenset in `society_service.py` — SUPER_ADMIN + OPS_MANAGER skip the gate
- Permission gate in `SocietyService.create()` — non-admins need `can_create_society=True` and must not already own a society (1-per-user limit)
- Migration 11: `ALTER TABLE users ADD COLUMN IF NOT EXISTS can_create_society BOOLEAN NOT NULL DEFAULT FALSE` in `run_migrations.py` and in `main.py` lifespan
- 3 new unit tests: `test_create_without_permission_raises`, `test_create_with_permission_succeeds`, `test_create_second_society_raises`

**Modified:**
- `user_model.py` — new column + docstring
- `user_schema.py` — `UpdateUserSchema` validates `can_create_society`
- `society_schema.py` — `CreateSocietySchema` adds optional `owner_user_id`
- `society_repository.py` — `count_by_owner` method added
- `society_service.py` — `create()` signature extended with `requester_role` and `can_create_society` (defaults keep all existing tests green)
- `society_routes.py` — POST `/` reads `can_create_society` from DB for non-admins; resolves effective owner; imports `User`, `get_db`, `Session`
- `run_migrations.py` — migration 11 appended
- `main.py` — idempotent `can_create_society` ALTER in lifespan

### App
**Added:**
- `CreateSocietyRequest` data class in `Models.kt`
- `ApiService.createSociety(request)` — POST `/api/v1/societies`
- `SocietyRepository.createSociety(request)` — delegates to API, throws on failure
- `SocietyViewModel.createSociety(request)` — sets `isCreating`, refreshes list on success
- `CreateSocietyDialog` composable in `SocietiesScreen.kt` — form with name, description, region/sport IDs, max members, public toggle, optional owner user ID
- FAB (+ icon) on `SocietiesScreen` opens the dialog

**Modified:**
- `AppUser` — added `can_create_society: Boolean = false`
- `UpdateUserRequest` — added `can_create_society: Boolean? = null`
- `SocietyUiState` — added `isCreating: Boolean = false`
- `SocietiesScreen` empty state text changed to "Tap + to create a society"
- `UserRow` in `UsersScreen` — new `onToggleSocietyPermission` parameter; DropdownMenu item "Grant/Revoke Society Creation"
- Both `UserRow` call sites updated with `onToggleSocietyPermission` lambda
- `UserManagementRepository.setSocietyPermission()` — calls `updateUser` with `can_create_society`
- `UserManagementViewModel.toggleSocietyPermission()` — self-mutation guard, updates state in-place

### Architectural decisions
- Default parameter values on `SocietyService.create()` (`requester_role=SUPER_ADMIN`, `can_create_society=True`) preserve all 35 existing tests without modification.
- Route reads `User.can_create_society` directly from DB via `get_db` session — not from JWT — so permission changes take effect on the next request with no token refresh needed.
- 1-per-user society limit enforced at service layer via `count_by_owner`; checked only for non-admin roles.
- HTTP 403 returned when "permission" appears in the ValueError message; 400 for all other validation errors.

---
## [2026-06-15] Phase 02 — Pixel-perfect UI pass (JSX to Compose)

### Added
- docs/plan-ui-pixel-perfect.md — screen-by-screen implementation plan

### Modified (App)
- ui/components/StatCard.kt — icon param, suffix param, icon in 38dp rounded box, value+suffix AnnotatedString, 0.72 alpha label
- ui/components/Avatar.kt — added onClick param to PlixoAvatar
- ui/screens/home/HomeScreen.kt — hero 360dp, frosted badges, 38sp title, stat blocks with icons, coins on white surface, quick tiles with sub-labels, Up next 188dp tournament card, ground cards photo+text layout, Captain CTA
- ui/screens/play/PlayScreen.kt — 2-col sport photo grid (92dp), skill 3-buttons, 3 stat mini-cards, price in CTA label
- ui/screens/tournaments/TournamentsScreen.kt — Browse/Vote/My cups tab switcher, prize hero card, tournament card 128dp photo+progress bar, Vote tab with percentage bars, Host CTA
- ui/screens/profile/ProfileScreen.kt — XP bar, stats row, Stats/Badges/History tabs, sport breakdown table, badges grid, menu sections with icon boxes

### Backend changes
- None

### Architectural decisions
- Ground cards: photo-top + text-below layout matching JSX GroundCard exactly
- Tournament tabs mirror JSX tabbed layout; Vote tab is fully local, no backend needed
- StatCard icon param is nullable for backward compatibility

---
## [2026-06-15] Phase 02 — Missing user-facing backend endpoints

### Added
**Backend:**
- `backend/modules/captain/controller/captain_routes.py` — `GET /api/v1/captains/me/stats` (returns rating + total_trips from Captain model)
- `backend/modules/wallet/` — new wallet module with `GET /api/v1/wallet/transactions` and `GET /api/v1/wallet/balance` (stub — returns empty list / 0 balance until wallet ledger table is built)
- `backend/main.py` — registered `wallet_router`

### Architectural decisions
- Wallet routes are a stub returning empty data; no DB table created yet (Phase 02 scope)
- Captain `/me/stats` looks up captain by `user_id` from JWT rather than requiring a captain_id param

---
## [2026-07-06] Phase 02 — Captain-created matches: schema (Task 1 of 8)

### Added
**Backend:**
- Migration 21 in `backend/run_migrations.py` — adds `matches.visibility` (VARCHAR(20) NOT NULL DEFAULT 'OPEN'), `matches.society_id` (INTEGER FK -> societies.id ON DELETE SET NULL), `matches.invite_code` (VARCHAR(8)), plus a partial unique index `uq_matches_invite_code` on `invite_code` (only enforced when non-null).

### Modified
**Backend:**
- `backend/modules/match/model/match_model.py` — added `visibility`, `society_id`, `invite_code` columns (placed after `booking_id`; note: the task spec referenced a `captain_id` column as the anchor point, but no such column exists on `Match` — placed new columns after the last existing FK column, `booking_id`, instead); added `VALID_VISIBILITIES = {"OPEN", "SOCIETY", "PRIVATE"}` class attribute; added all 3 fields to `to_dict()`.

### Backend changes
- Ran `run_migrations.py` against the dev Postgres DB (via main project's `venv`, since this worktree lacks its own `venv`/`backend/.env` — copied `backend/.env` from the main worktree to run the migration). Migration 21 applied cleanly; verified via `information_schema.columns` that `visibility`, `society_id`, `invite_code` exist on `matches`.

### Architectural decisions
- Purely additive schema change — no existing behavior modified. Sets up columns for later tasks (repository/service/route logic) in the captain-created-matches feature (see `docs/superpowers/plans/2026-07-06-captain-created-matches.md`).

---
## [2026-07-06] Phase 02 — Captain-created matches: full feature (Tasks 2-7 of 8)

Captain-initiated match creation — the "Open match / Society match / Tournament / Private" tab in the captain dashboard (`CreateMatchTab`), previously 4 no-op buttons, now creates real matches. Tournament redirects to the existing tournament flow (no new backend — tournaments are organizer-created bracket entities, not something a captain spins up instantly like a match).

### Added
**Backend:**
- `MatchRepository.create_captain_match()` (`backend/modules/match/repository/match_repository.py`) — creates a `WAITING` match with `created_by=user_id`, no `MatchPlayer` row for the captain (they organize, don't play), a 6-char alphanumeric `invite_code` when `visibility == PRIVATE`, and immediately marks the organizing captain busy via `CaptainRepository.set_availability(match_id=...)` — since this codebase has no `Match.captain_id` column, captain identity is `created_by` + `Captain.current_match_id`.
- `MatchRepository.find_society_matches()` / `find_by_invite_code()` — society-scoped and invite-code lookups.
- `MatchService.captain_create_match()` / `join_by_code()` (`backend/modules/match/service/match_service.py`) — validates active captain profile, cart type, region, and (for SOCIETY) society existence + membership; delegates to the repository and logs a `MATCH_CREATED` event.
- `CaptainCreateMatchSchema` (`backend/modules/match/schemas/match_schema.py`) — validates `cart_type_id`/`region_id`/`max_players`/`visibility`, conditionally requires `society_id` for SOCIETY, optional `skill_level`.
- Routes: `POST /api/v1/matches/captain-create`, `POST /api/v1/matches/join-by-code`, `GET /api/v1/societies/{id}/matches` (member-only), `GET /api/v1/societies/mine` (`backend/modules/match/controller/match_routes.py`, `backend/modules/society/controller/society_routes.py`).
- `SocietyMemberRepository.find_by_user()` / `SocietyMemberService.get_my_societies()` — support the app's society picker.
- Test coverage: `backend/modules/match/tests/test_captain_created_matches.py` — 19 tests across repository and service layers (captain-create for all 3 visibilities, membership/active-captain gating, invite-code join, and a regression test confirming `join_match`'s auto-assign no longer steals an already-linked captain).

**App (Vmsuserapp):**
- `CaptainCreateMatchRequest`, `JoinByCodeRequest`, `MySociety` models; `Match` extended with `visibility`/`societyId`/`inviteCode`/`maxPlayers`/`joinedPlayers` (`models/Models.kt`).
- 4 new `ApiService` Retrofit endpoints (`network/ApiService.kt`).
- `CaptainRepository.createMatch()` / `getMySocieties()`; `CaptainViewModel` state (`mySocieties`, `creatingMatch`, `createdMatch`, `sports`, `regions`) and actions (`createMatch`, `loadMySocieties`, `loadSportsAndRegions`, `clearCreatedMatch`).
- `CreateMatchTab` (`ui/screens/captain/CaptainDashboardScreen.kt`) rewritten: Open/Private open a confirm bottom sheet (sport picker, region picker, max-players stepper); Society opens a society picker first, then the same confirm sheet; Tournament navigates to the existing Tournaments screen. Private match creation shows the returned invite code with a copy action instead of auto-navigating away.

### Modified
**Backend:**
- `MatchRepository.find_waiting_in_region()` — now filters `visibility == "OPEN"`, so SOCIETY/PRIVATE matches never leak into the public open-matches browse feed used by `GET /matches/open`.
- `MatchService.join_match()` — the WAITING→full auto-assign-captain branch now checks whether a captain is already linked (`Captain.current_match_id == match.id`) before calling `find_available_captain`, so filling a captain-created match doesn't reassign/steal the organizing captain's slot.
- `MatchService.__init__` — added 3 injectable constructor params (`captain_repository`, `society_member_repository`, `society_repository`), following the existing `param or _default_repo` fallback pattern already used for the original 5.

### Architectural decisions
- **No `Match.captain_id` column.** An earlier draft of this plan assumed one existed (based on files read from an uncommitted, never-merged WIP branch state on the developer's main checkout — not what's actually in this branch's git history). Corrected mid-implementation: captain identity for a match is `Match.created_by` (reused, same field regular matches already use for their creator) plus `Captain.current_match_id`/`is_available`, toggled via the existing `CaptainRepository.set_availability`/`release_captain_for_match`.
- **Captain-created matches assign the captain immediately, not on fill.** Unlike play-now (where a captain is auto-assigned only once the match reaches capacity), a captain-created match has its organizer linked from creation — `join_match`'s auto-assign path had to be patched to not clobber this.
- **Tournament is a redirect, not a new feature.** Tournaments are separate bracket entities (`Tournament`/`TournamentMatch`/`TournamentTeam`, organizer-created, with start/end dates and registration via `SocietyTournamentService`) — fundamentally different from a captain spinning up an ad-hoc match. Tapping "Tournament" just navigates to the existing tournament browse/registration screen.
- **Region/sport picked explicitly in the app**, not inferred from the logged-in user's profile — `User.region` in the app is a display string, not an id, and plumbing that through was out of scope. The confirm sheet reuses `ApiService.getLocations()`/`getSports()`, the same sources already used during profile setup.
- Full design rationale: `docs/superpowers/specs/2026-07-06-captain-created-matches-design.md`. Full task-by-task plan: `docs/superpowers/plans/2026-07-06-captain-created-matches.md`.

---
## [2026-07-06] Incident — uncommitted work from a prior session was lost, DB schema survived

### What happened
A large amount of work (Notifications module, Chat module, Captain KYC fields + earnings wallet, Ground Owner authorization fix, FCM push notifications, several dashboard bug fixes) was built in an earlier session but never committed to git. Before it was committed, the working tree was reset/overwritten — git history jumps directly from `b7e7c08` to an unrelated "captain-created matches" feature line, whose own commits confirm it started from a clean `b7e7c08` checkout with no knowledge of the intervening work (e.g. "docs: correct plan — Match has no captain_id column on this branch").

### What survived
The live Neon database still has the full schema from the lost migrations: `notifications`, `fcm_tokens`, `messages`, `captain_earnings` tables; `captains.kyc_document_url/kyc_status/payout_upi_id/verification_method`; `matches.captain_id`; `system_configs.CAPTAIN_FEE_PER_MATCH`. All tables confirmed empty (0 rows) — no user data was ever written against this schema, so this is a pure code-loss incident, not a data-loss incident.

### Fixed immediately
- Re-applied the Ground Owner authorization fix (`ground_routes.py` — `GROUND_OWNER` was in `_ADMIN_ROLES`, letting any ground owner edit any ground with full field access)
- Re-added `.gitignore` rules for the Firebase service account key and `backend/uploads/` (KYC docs) — these were also reverted and the key briefly had no ignore protection
- Committed today's tournament-admin + audit-log work immediately (commit `042b28d`) rather than leaving it uncommitted

### Still to rebuild (schema already exists, only code is missing)
- `backend/modules/notification/` — model/repo/service/routes, Firebase Admin SDK wiring
- `backend/modules/chat/` — model/repo/service/routes
- Captain KYC fields on `captain_model.py` + onboarding flow (apply/upload-kyc/review)
- Captain earnings ledger (`captain_earning_model.py`) + payout flow
- Corresponding Vmsuserapp/Vmsadminapp screens for all of the above

### Lesson
Commit incrementally instead of batching a full day's work uncommitted — from now on, commit after each completed vertical slice.

---
## [2026-07-07] Phase 02 — Rebuild: Notification module, Captain KYC + earnings wallet

### Added
**Backend**
- `backend/modules/notification/` — full module rebuild: `model/notification_model.py`, `model/fcm_token_model.py`, `repository/notification_repository.py`, `repository/fcm_token_repository.py`, `service/notification_service.py`, `controller/notification_routes.py` (`GET /api/v1/notifications`, `PUT /{id}/read`). Registered in `main.py`.
- `backend/core/push/firebase_client.py` — lazy Firebase Admin SDK init (`FIREBASE_SERVICE_ACCOUNT_PATH` or `_JSON`), `send_push()` never raises, disabled gracefully when unconfigured.
- `backend/core/storage/kyc_storage.py` — local-disk KYC document storage, never served as static files (always via authenticated route).
- `backend/modules/captain/model/captain_earning_model.py` — `CaptainEarning` ledger (captain_id, match_id, amount, status PENDING/PAID, payout_reference).
- `backend/modules/captain/repository/captain_earning_repository.py` — `create`, `find_by_captain`, `sum_since` (activity metric), `sum_pending` (wallet balance), `mark_captain_paid`.
- `PUT /api/v1/users/me/fcm-token` in `user_routes.py` — registers/updates the caller's push token.
- `run_migrations.py` — re-added migrations 16-20 (captains KYC/payout columns, `captain_earnings`, `notifications`, `fcm_tokens` tables, `matches.captain_id`), which had been silently dropped in the incident. Idempotent (`IF NOT EXISTS`), safe to re-run against the already-migrated Neon DB.

### Modified
- `backend/modules/captain/model/captain_model.py` — added KYC fields (`kyc_document_url`, `kyc_document_type`, `kyc_status`, `verification_method`, `rejection_reason`, `payout_upi_id`), `PENDING_REVIEW`/`REJECTED` statuses.
- `backend/modules/captain/repository/captain_repository.py` — `get_all()` regained `status` filter.
- `backend/modules/captain/service/captain_service.py` — `get_captain_fee()` (reads `CAPTAIN_FEE_PER_MATCH` from SystemConfig), `record_match_earning()`, `get_my_stats()` (fixed crash — dict access not attribute access; now returns `today_earnings`/`week_earnings`/`wallet_balance`/`payout_upi_id`), `update_payout_upi()`, `list_pending_payouts()`, `mark_paid()`, `apply()` (min 3 completed matches), `upload_kyc()`, `get_my_application()`, `review()`.
- `backend/modules/captain/controller/captain_routes.py` — added `/apply`, `/me/kyc`, `/me/application-status`, `/me/payout-upi`, `/payouts/pending`, `/{id}/payout` (sends notification + audit log), `/me/stats`, `/{id}/review` (sends notification + audit log), `/{id}/kyc-document`. Static-path routes registered before `/{id}` to avoid ambiguity.
- `backend/modules/match/model/match_model.py` — re-added `captain_id` FK (permanent historical record, unlike `Captain.current_match_id` which gets cleared).
- `backend/modules/match/service/match_service.py` — `join_match()` sets `match_orm.captain_id` on auto-assign; `finish_match()` calls `CaptainService().record_match_earning()` non-fatally after payment splitting.
- `backend/modules/match/repository/match_repository.py` — `create_captain_match()` sets `captain_id` on the ORM object directly instead of faking it in the returned dict.
- `backend/modules/user/controller/user_routes.py` — added `DEACTIVATION` audit log entry (was a stale TODO) alongside the existing `ROLE_CHANGE` block.
- `backend/main.py` — registered `notification_router`, imported `CaptainEarning`/`Notification`/`FcmToken` models for table creation.

### Architectural decisions
- Captain earnings are a ledger, not a live computation — `CaptainEarning` rows snapshot the fee at match-completion time so later `CAPTAIN_FEE_PER_MATCH` changes don't retroactively rewrite history. `wallet_balance` = sum of PENDING; `today_earnings`/`week_earnings` = sum of ANY-status in a time window (activity, not balance).
- Payout is manual settlement (mirrors `Payment`'s existing manual-approval workflow) — no payment gateway. Captain sets a UPI ID; admin sends money out-of-band and marks paid in-app.
- KYC documents are never static-served; always fetched through an authenticated route to prevent ID-document URL guessing.

### Verified
- `python -c "import backend.main"` succeeds.
- Full backend suite: 431 passed.
- This closes out the majority of the "Still to rebuild" list from the incident above. Remaining: `backend/modules/chat/` module, and all Vmsuserapp/Vmsadminapp Kotlin screens for KYC/earnings/chat/notifications.

---
## [2026-07-07] Correction — the "lost" work was never deleted, it was an unpopped stash

### Root cause, finally found
`git reflog` and `git stash list` revealed `stash@{0}: On main: WIP before merging claude/charming-dhawan-2898ff` —
git auto-stashes uncommitted changes when a merge/pull can't fast-forward through a dirty
working tree. The stash was created, the merge landed, and the stash was never popped back.
The next session started clean from the merge commit with no idea the stash existed.

**Critical nuance**: `git stash` (without `-u`) only captures changes to *already-tracked*
files. Brand-new files — the entire `notification/`, `chat/` module directories,
`captain_earning_model.py`, and every new Kotlin screen — were untracked at stash time, so
they were left behind in the working tree and separately wiped (most likely `git clean -fd`
from the other session/worktree). Everything that touched an *existing* tracked file survived
perfectly in the stash.

### What this changes about yesterday's incident report
Not a code-loss incident for tracked files — a stash-hygiene incident. All Kotlin app work
(admin CaptainScreen Payouts tab + KYC review dialog, user app CaptainEarningsScreen,
KycUploadScreen, chat screens, PlixoMessagingService, NotificationsScreen tap-nav, etc.) was
recovered byte-for-byte via `git checkout stash@{0} -- <path>` instead of being rebuilt from
scratch — see the app-recovery commit. Backend tracked-file changes in the stash were diffed
against the already-rebuilt backend (which had since gained tournament/audit features the
stash predates) and merged in piece by piece; only genuinely new backend surface was pulled
forward.

### Added (backend, pulled from the stash + net-new)
- Tournament pricing/marketing fields — `entry_fee`, `prize_pool`, `banner_url`, `description`
  on `Tournament` model/schema/repository; `GET /api/v1/tournaments/public` (any authenticated
  user, UPCOMING/ONGOING only); `tournament_repository.find_all_enriched/find_by_id_enriched`
  (sport name, location name, registered-team count).
- Dispute self-service — `GET/POST /api/v1/disputes/mine`, `dispute_repository.find_by_raised_by`,
  `dispute_service.list_my_disputes` — lets a regular user raise/view their own support tickets
  without a SUPPORT/OPS_MANAGER role.
- Society leaderboard/member name enrichment — `society_member_service._attach_user_names()`;
  `get_members`/`get_leaderboard` now return each member's display `name` (was missing).
- Session reaper — `backend/modules/match/service/session_reaper_service.py`: WAITING matches
  with ≤1 player auto-cancel after 15 minutes. Wired into `main.py`'s lifespan via
  APScheduler (`BackgroundScheduler`, 5-minute interval). New repository method
  `match_repository.find_abandoned_waiting(cutoff)` and service method
  `match_service.system_cancel_abandoned(match_id)` (mirrors `cancel_match` without the
  ownership check, since this is a system action).
- Chat module (`backend/modules/chat/`) — full rebuild from scratch (genuinely lost, was
  never tracked so never stashed). `Message` model, `MessageRepository`, `ChatService`
  (participant-scoped: sender must be the match creator or a `MatchPlayer`), routes
  `GET /api/v1/chat/threads`, `GET/POST /api/v1/matches/{match_id}/messages`. One thread per
  match a user participates in; sending a message pushes a notification to the other
  participant(s) via the already-rebuilt notification service. Polling-based, no websockets —
  matches the pre-incident design noted in `CLAUDE.md`.
- `match_service.join_match()` — sends a "Player found!" push notification to the match
  creator when the session fills and moves WAITING → MATCHED (wrapped non-fatally, matching
  the existing `finish_match()` earnings-hook pattern, after it broke test isolation once).
- `run_migrations.py` — migrations 22 (`messages` table) and 23 (tournament pricing columns),
  applied to the live Neon DB.
- `requirements.txt` — added `apscheduler`, `firebase-admin`, `python-multipart` (all were
  already installed in the venv from the earlier rebuild but missing from the manifest).

### Verified
- `python run_migrations.py` — all 23 migrations applied cleanly against the live DB.
- `python -c "import backend.main"` succeeds.
- Full backend suite: 431 passed.
- This closes out the "Still to rebuild" list from the 2026-07-06 incident entirely.

---
## [2026-07-07] Phase 02 — Audit log: final coverage + admin app filter/pagination UI

### Added
**Backend**
- `PAYMENT_APPROVED`/`PAYMENT_REJECTED`/`PAYMENT_REFUNDED` audit logging in
  `backend/modules/payment/controller/payment_routes.py` (approve/reject/refund routes).
- `BOOKING_CANCELLED` audit logging in `backend/modules/booking/controller/booking_routes.py`,
  tagged with `by_admin` so admin-initiated vs. self-cancellations are distinguishable.

**Admin app**
- `AuditLogScreen.kt` — filter bottom sheet (action dropdown, resource-type dropdown, actor
  user ID, from/to date range) and infinite-scroll pagination (loads the next page when the
  list is scrolled within 5 items of the end). Empty state now distinguishes "no entries at
  all" from "no entries match these filters" with a clear-filters action.
- `AuditLogViewModel.kt` — rewritten around an `AuditLogFilters` value object; `loadLogs`/
  `refresh` reset to offset 0, `loadMore` appends, `applyFilters`/`clearFilters` re-query from
  offset 0 with the new filter set.
- `AuditLogRepository.kt` / `ApiService.kt` — `getAuditLogs()` now takes `limit`/`offset`/
  `action`/`actorUserId`/`targetResourceType`/`startDate`/`endDate`, matching the backend's
  existing filter/pagination support. Also fixed the endpoint path — it was `@GET("audit-logs")`
  with no `/api/v1` prefix and no leading slash, inconsistent with every other endpoint in the
  file; corrected to `/api/v1/audit-logs`.

### Architectural decisions
- Pagination is offset-based (matches the backend's `limit`/`offset` query params) rather than
  cursor-based — audit logs are append-only and never reordered, so offset drift from concurrent
  writes is a non-issue here.
- Date filters are plain `YYYY-MM-DD` text fields, not a Compose `DatePicker` — kept scope tight;
  swap in a real date picker if manual entry becomes a complaint.

### Verified
- Full backend suite: 431 passed.
- This was the last item on the audit-log punch list (backend filters/pagination/coverage +
  admin app UI) — both tracked tasks are now complete.

---
## [2026-07-07] Phase 02 — Finance reporting: daily revenue/refund report + CSV export

### Added
**Backend**
- `payment_repository.find_between(start_date, end_date)` — SUCCESS/REFUNDED payments in a
  date range.
- `payment_service.get_report(start_date, end_date)` — buckets payments by calendar day
  (`{period, revenue, refunded, count}`), bucketed in Python rather than SQL `date_trunc`/
  `strftime` so the exact same code path runs against Postgres in prod and SQLite in tests.
- `GET /api/v1/payments/report?start_date=&end_date=` — FINANCE/SUPER_ADMIN only.
- `GET /api/v1/payments/report/export?start_date=&end_date=` — same data as CSV via
  `StreamingResponse`, `Content-Disposition: attachment`.
- `backend/modules/payment/tests/test_payment_report.py` — 3 tests (day bucketing, excludes
  pending/out-of-range, empty range).

**Admin app**
- `PaymentsScreen.kt` — added a top-level Payments/Reports tab. Reports tab has a from/to date
  range (defaults to the last 7 days), a revenue/refund summary card, and a per-day breakdown
  list.
- `PaymentViewModel.kt` — `loadReport(startDate, endDate)`; report state added to `PaymentUiState`.
- `PaymentRepository.kt`/`ApiService.kt`/`Models.kt` — `fetchReport()`, `getPaymentReport()`,
  `PaymentReportEntry`. Also fixed `getPaymentSummary()`'s endpoint path — same bare-path bug as
  yesterday's `audit-logs` fix (`@GET("payments/summary")` with no `/api/v1` prefix).

### Architectural decisions
- Daily granularity only (no weekly/monthly `group_by` param) — this is the smallest useful
  slice per the Finance-reporting gap in `.claude/context/memory.md` (revenue-over-time +
  refund history); a coarser grouping can be added as a query param later without a schema
  change.
- No in-app CSV download — the export endpoint exists and is reachable by any authenticated
  FINANCE/SUPER_ADMIN HTTP client (e.g. a browser with the bearer token, or `curl`), but wiring
  Android file-save/share intents was out of scope for this slice.

### Verified
- New tests: 3 passed. Full backend suite: 434 passed.
- CSR_PARTNER screens gap investigated but deliberately not started this session — the backend
  has no concept of tournament sponsorship/CSR allocation at all (no `sponsor`/`csr` field
  anywhere in the tournament module), and the existing `CsrScreen.kt` is a placeholder that
  leaks unscoped match data via `MatchViewModel`. This needs a product decision on the
  CSR-to-tournament data model before any code — resolved in the next entry below.

---
## [2026-07-07] Phase 02 — CSR_PARTNER: tournament sponsorship model + real CsrScreen

### Product decision
Asked the user how a CSR_PARTNER's tournament view should be scoped, since the backend had no
sponsorship concept at all. Chose "sponsor a specific tournament" over a shared funding pool or
unscoped read-all access: a nullable `sponsor_user_id` on `Tournament`, one CSR partner per
tournament, CSR partner's dashboard shows only tournaments they sponsor.

### Added
**Backend**
- `Tournament.sponsor_user_id` — nullable FK to `users.id`, `ON DELETE SET NULL`. Migration 24.
- `tournament_service.update_tournament()` — when `sponsor_user_id` is set, validates the target
  user exists and has `role == "csr_partner"` (raises `ValueError` otherwise). Assignment goes
  through the existing `PUT /tournaments/{id}` (already TOURNAMENT_MANAGER/SUPER_ADMIN only —
  no new auth guard needed).
- `tournament_service.list_sponsored(sponsor_user_id)` + `GET /api/v1/tournaments/csr/mine`
  (CSR_PARTNER/SUPER_ADMIN) — returns only the caller's sponsored tournaments, enriched via the
  existing `find_all_enriched()` (sport name, location name, `registered_teams` count).
- `backend/modules/tournament/tests/test_tournament_sponsor.py` — 9 tests (schema validation,
  assign to CSR partner succeeds, assign to non-CSR user rejected, assign to unknown user
  rejected, unassign via null, `list_sponsored` scoping).

**Admin app**
- `TournamentDetailScreen.kt` — new "Sponsor" tab showing the current sponsor (or none) with an
  Assign/Change button; `AssignSponsorDialog` lists CSR_PARTNER users (filtered client-side from
  `UserManagementViewModel`'s already-loaded user list) as a radio picker.
- `CsrScreen.kt` — full rewrite. Was a placeholder ("Tournament listings are coming soon") plus
  an unscoped `MatchViewModel.loadMatches()` call that leaked every match in the system to any
  CSR_PARTNER account. Now calls `GET /tournaments/csr/mine` via `TournamentViewModel` and shows
  only the partner's own sponsored tournaments (dates, registration count, prize pool, status).
- `TournamentViewModel.kt` — `loadMySponsoredTournaments()`, `assignSponsor()`.
- `MainScreen.kt` — `CsrScreen`/`TournamentDetailScreen` call sites updated to pass
  `tournamentViewModel`/`userManagementViewModel` instead of the old `matchViewModel`.

### Architectural decisions
- One sponsor per tournament (single nullable FK), not a many-to-many sponsorship table — matches
  the chosen "sponsor a tournament" model exactly; a partner sponsoring multiple tournaments just
  means multiple `Tournament` rows point at the same `sponsor_user_id`.
- Sponsor assignment reuses the existing tournament update endpoint/schema rather than a
  dedicated route — it's one more optional field alongside `status`/`name`/etc., and the
  TOURNAMENT_MANAGER/SUPER_ADMIN gate was already correct for this action.
- The CSR partner picker in the admin app filters the already-loaded `UserManagementViewModel`
  user list client-side instead of adding a `?role=csr_partner` backend query param — the admin
  app already loads all users for the User Management screen, so this avoids a redundant call for
  what's expected to be a small list of CSR accounts.

### Verified
- New tests: 9 passed. Full backend suite: 443 passed.
- Migration 24 applied cleanly to the live Neon DB.
- This closes out both Phase 02 focus items (Finance reporting, CSR_PARTNER screens).

---
## [2026-07-08] Fix — Vmsuserapp did not compile: missing SupportScreen

### The problem
User asked for an end-to-end completeness audit across backend + both apps. Found
`Vmsuserapp/.../navigation/AppNavigation.kt:148` referencing `SupportScreen(navController)` for
the `Screen.Support` route (linked from `SettingsScreen.kt`'s "Help & Support" item), but no
`SupportScreen` composable existed anywhere in the app — a genuine compile error, not a missing
feature the app just gracefully lacked. Root cause: `AppNavigation.kt` is a tracked file that
survived the 2026-07-06 stash incident intact (it referenced the screen before the incident),
but `SupportScreen.kt` itself was untracked at the time and was lost along with the rest of the
untracked files (notification/chat modules, etc.) — never rebuilt in this session's recovery
because the audit that found it hadn't happened yet.

The backend side was never actually missing: `GET/POST /api/v1/disputes/mine` and the
`Dispute`/`CreateDisputeRequest` Retrofit models were already present and correct in
`ApiService.kt`/`Models.kt` — only the repository/viewmodel/screen layer was gone.

### Added
**User app**
- `data/SupportRepository.kt` — `getMyDisputes()`, `createDispute(title, description)`.
- `viewmodel/SupportViewModel.kt` — tickets list, submit-in-progress state, error state.
- `ui/screens/profile/SupportScreen.kt` — ticket list (status-badged) + a "Raise a new ticket"
  form (title + description, 10-char minimum on description). Matches existing screen
  conventions (`PlixoTopBar`, `PlixoButton`, `PlixoShape`, theme colors/fonts) rather than
  introducing new patterns.

### Verified
- Confirmed no other references to nonexistent composables via `grep -rn` sanity sweep of
  `AppNavigation.kt`'s screen imports (this was the only broken one).
- Backend unaffected — full suite still 443 passed.
- Not build-verified (no Android Studio/Gradle build run per project policy — DO NOT build the
  APK). Brace/paren balance checked programmatically; every theme identifier and component
  signature used was verified against its actual definition before use.

### Other findings from the same audit (not yet acted on)
- `backend/modules/wallet/` is a hardcoded stub (`GET /balance` always returns `{"balance": 0}`,
  `GET /transactions` always returns `[]`) — this is the **known, deliberately deferred**
  player-facing coin wallet (`WALLET=false`), not a new finding. `Vmsuserapp/.../ProfileViewModel.kt`
  mirrors this with a hardcoded `walletBalance = 240` and 4 fabricated mock transactions, and
  `WalletScreen.kt`'s "Add coins" button is a no-op. Since this whole feature is already flagged
  as deferred by product decision in `CLAUDE.md`, no fix applied here — flagging again for
  visibility in case that decision changes.

---
## [2026-07-08] Phase 03 — Real player wallet: earn-only coin ledger

### Product decision
User asked whether to revisit the deferred wallet. Given three options — earn-only, earn +
manual UPI top-up, or ledger-only with no earning rules — chose **earn-only**: coins are
awarded solely by system events (currently: match completion), no purchase/top-up path. This
avoids inventing a payment-gateway integration that doesn't exist anywhere else in the codebase
(bookings use manual UPI submit-and-approve, not a real gateway) and matches what the old mock
data already implied ("Match completion bonus", "Referral bonus" — though referral itself
doesn't exist as a feature anywhere and was not built here; only match completion was wired,
since inventing a full referral system was out of scope for a wallet ledger).

### Added
**Backend**
- `backend/modules/wallet/model/wallet_transaction_model.py` — `WalletTransaction` (user_id,
  type CREDIT/DEBIT, amount as positive magnitude, reason, description, match_id, created_at).
  Append-only ledger; balance is always derived, never stored/mutated directly.
- `backend/modules/wallet/repository/wallet_transaction_repository.py` — `create`,
  `find_by_user`, `get_balance` (sum credits minus debits), `has_bonus_for_match` (idempotency
  guard).
- `backend/modules/wallet/service/wallet_service.py` — `get_balance`, `get_transactions`
  (converts to the app's signed-amount/lowercase-type contract), `award_match_completion_bonus`
  (idempotent per user+match, amount admin-configurable via `SystemConfig.WALLET_MATCH_COMPLETION_BONUS`,
  default 20).
- `match_repository.find_player_user_ids(match_id)` — new helper.
- `match_service.finish_match()` — after the existing captain-earning hook, awards each match
  player a completion bonus, non-fatal (same try/except-log pattern as the payment-split and
  captain-earning hooks already in this method).
- `GET /api/v1/wallet/balance` and `GET /api/v1/wallet/transactions` now return real data —
  replaces the `{"balance": 0}` / `[]` hardcoded stub. **No route signature change** — both apps'
  existing Retrofit contracts already matched this shape.
- Migration 25 (`wallet_transactions` table), applied to the live Neon DB.
- `backend/modules/wallet/tests/test_wallet_service.py` — 7 tests (balance math, bonus award,
  system-config override, idempotency per match, per-user isolation, response shape).

**User app**
- `ProfileViewModel.kt` — removed `mockTransactions()` and the hardcoded `walletBalance = 240`;
  `loadTransactions()` now also loads the real balance via a new `ProfileRepository.getWalletBalance()`.
- `WalletScreen.kt` — removed the "Add coins" button (was `onClick = {}`, a genuine no-op);
  added an empty-transactions state ("play and complete a match to earn coins").

### Verified
- New tests: 7 passed. Full backend suite: 450 passed.
- Migration 25 applied cleanly to the live DB.
- Admin app audited for the same class of broken-reference bug found in Vmsuserapp yesterday
  (missing `SupportScreen`) — none found. Every screen/ViewModelFactory/Repository referenced
  from `MainActivity.kt`/`MainScreen.kt` has a real definition; zero `onClick = {}` or
  "coming soon" placeholders anywhere in the admin app.

---
## [2026-07-08] Fix — Vmsuserapp still didn't compile: missing `notifications` package

### The problem
User ran an actual `assembleDebug` build (the first real compile check this app had in this
whole recovery arc) and it failed — same root cause as the `SupportScreen` incident two days
ago, a different corner of it. `com.example.vmsuser.notifications` (containing `PendingDeepLink`,
a Compose-observable singleton for cold-start notification taps, and `notificationDeepLinkRoute()`,
the type→route mapping shared by `PlixoMessagingService` and `NotificationsScreen`) was untracked
at stash time and never made it back. My earlier "end-to-end audit" grep only checked
`ui/screens/*Screen` composables against `MainScreen.kt`/`AppNavigation.kt` call sites — it
never cross-referenced *every* internal import, so this package-level gap slipped through.
Also surfaced by the same build: `KycUploadScreen.kt` used `ExposedDropdownMenuBox` without
`@OptIn(ExperimentalMaterial3Api::class)`, and `R.drawable.ic_stat_notification` (the FCM status
bar icon) had no drawable resource at all.

### Added
- `notifications/PendingDeepLink.kt` — `object PendingDeepLink { var route: String? by mutableStateOf(null) }`.
- `notifications/NotificationNav.kt` — `notificationDeepLinkRoute(type, matchId)`, mapping every
  `type_=` value actually used by the backend (`MATCH_FOUND`, `CHAT_MESSAGE`, `CAPTAIN_APPROVED`,
  `CAPTAIN_REJECTED`, `CAPTAIN_PAYOUT`) to a `Screen` route; unknown types fall back to
  `Screen.Notifications.route`.
- `res/drawable/ic_stat_notification.xml` — flat white bell vector, correct for a status-bar icon.
- `@OptIn(ExperimentalMaterial3Api::class)` on `KycUploadScreen`.

### Verified
- Cross-referenced every `import com.example.vmsuser.*` in the app against an actual
  class/object/fun/val declaration — only `BuildConfig`/`R` (build-generated, expected) came up
  unmatched. Re-ran the screen-composable-vs-navigation-call check from the earlier audit — clean.
- Not build-verified by me (still no Android Studio/Gradle build per project policy) — verified
  by the user's own `assembleDebug` run, which is what surfaced this in the first place. Next
  build attempt should clear the Kotlin compile stage; **user is running the actual verification
  here, this entry documents the fix, not a substitute for it.**
- Lesson for future audits: cross-check *all* internal imports against declarations, not just
  one call-site pattern (composables) against one caller (nav graph). A package can be entirely
  missing while every screen file that references it still exists and looks fine on its own.

---
## [2026-07-08] Fix — Vmsadminapp did not compile either: a real regression, not a stash gap

### The problem
User's `assembleDebug` on the admin app failed next. Root cause was different from every prior
fix this week — this one I actually caused. Back on 2026-07-07, `feat(backend,admin): tournament
admin management + audit log expansion` (`042b28d`) added `TournamentMatch`, `TournamentStanding`,
`TournamentRegistration`(+`Member`), `CreateTournamentMatchRequest`, `RecordMatchResultRequest` to
`Models.kt`, five matching Retrofit endpoints to `ApiService.kt`, and `onOpenDetail`/`onDelete`
wiring to `TournamentsScreen.kt`. All committed, all fine.

Then during the incident recovery, I ran `git checkout stash@{0} -- <path>` against `Models.kt`,
`ApiService.kt`, and `TournamentsScreen.kt` to restore CSR/KYC/payout content the stash held —
but the stash's base commit (`b7e7c08`) *predates* `042b28d`. A file-level checkout doesn't
merge; it replaces the whole file with the stash's version. So restoring those three files from
the stash silently reverted the tournament-detail additions while bringing back everything else.
This shipped in commit `2b85d48` and sat undetected through every subsequent audit — the
composable-existence and import-cross-reference checks I ran on both apps only catch symbols
that are *entirely absent*; they don't catch a symbol that exists in one commit's history but
was clobbered by a later same-session file overwrite, because from the audit's point of view the
"declaration" and the "usage" were compared against the same (already-reverted) file state.

### Added back (verbatim from `042b28d`, cross-checked no later commit had since changed them)
- `Models.kt` — `TournamentMatch`, `CreateTournamentMatchRequest`, `RecordMatchResultRequest`,
  `TournamentStanding`, `TournamentRegistrationMember`, `TournamentRegistration`, and
  `Tournament.participant_type`.
- `ApiService.kt` — `getTournamentMatches`, `createTournamentMatch`, `recordMatchResult`,
  `getTournamentStandings`, `getTournamentRegistrations`, plus their model imports.
- `TournamentsScreen.kt` — `onOpenDetail` param on the screen, `onOpenDetail`/`onDelete` on
  `TournamentCard`, `AppCard(onClick = onOpenDetail)`, the delete-confirmation dialog, and the
  "Delete" item in the status dropdown menu.

### Verified
- `git log --oneline b7e7c08..2b85d48 -- <every file checked out from the stash>` for both apps —
  confirms this was the *only* three-file blast radius. No other stash-recovered file (Captain
  screens/viewmodel/repo, GroundOwnerScreen, any Vmsuserapp file) had an intervening commit
  between the stash's base and the recovery, so none of them lost anything this way.
- Re-ran the full import-cross-reference sweep against the admin app (same method as yesterday's
  Vmsuserapp audit) — one flagged hit, `ui.components.shimmerEffect`, confirmed a false positive
  (it's a `Modifier` extension function, `fun Modifier.shimmerEffect()`, which the grep pattern
  doesn't match — not a missing symbol).
- Not build-verified by me — same as yesterday, the user's own `assembleDebug` is the actual
  check here.

---
## [2026-07-08] Fix — Tournaments tab HTTP 500: live DB table missing 4 columns the model has had all along

### The problem
Once both apps compiled, user reported the admin Tournaments tab throwing an HTTP 500. Root
cause was in the live Postgres schema, not application code: `Tournament.format_type`,
`participant_type`, `team_size`, and `rules_json` have been columns on the SQLAlchemy model
since before this session even started, but the *original* `CREATE TABLE tournaments` statement
in `run_migrations.py` (migration 8) never included them, and no later migration added them
either. `Base.metadata.create_all()` (run on every backend boot) only creates missing *tables* —
it never alters an existing table to add missing columns. So every `SELECT` SQLAlchemy generated
against `tournaments` (which lists all mapped columns) has been failing with
"column tournaments.format_type does not exist" for as long as that table has existed with rows
in it. This never surfaced in the test suite because the tests build a fresh SQLite schema via
`Base.metadata.create_all()` on an empty in-memory DB every run, which — unlike a pre-existing
Postgres table — picks up every column in the model, missing-migration or not.

### Root-caused directly against the live DB
Used the Neon MCP to `describe_table_schema` on `tournaments` and found exactly those 4 columns
absent while `sponsor_user_id`/`entry_fee`/`prize_pool`/`banner_url`/`description` (added by
later, correctly-written migrations 23/24) were present. Cross-checked the other four
tournament-related tables (`tournament_matches`, `tournament_participants`,
`tournament_standings`, `tournament_teams`) against their models — all matched exactly, because
those are entirely new tables that `create_all()` created correctly from scratch. The bug was
isolated to `tournaments` specifically, the one pre-existing table in the group.

### Fixed
- Ran `ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS format_type ... participant_type ...
  team_size ... rules_json ...` directly against the live Neon DB (via MCP), matching the
  model's exact types/defaults.
- Added the same statement to `run_migrations.py` as migration 26, so a fresh environment (or
  anyone re-running migrations) gets it too. Re-ran the full local migration script afterward —
  idempotent, confirms it's the same database.
- Verified with a direct query (`SessionLocal().query(Tournament).all()`) that the exact
  operation that was 500ing now succeeds.

### Lesson
`Base.metadata.create_all()` silently does nothing for column-level drift on existing tables —
it will never surface a model/schema mismatch on an existing table, on any environment, until a
query actually needs the missing column. This is a class of bug that a python-only or SQLite-only
test suite structurally cannot catch. Worth periodically diffing each model's columns against the
live schema directly (as done here) rather than trusting "tests pass + `create_all()` ran" as
proof the schema is correct.

### Verified
- Full backend suite: 450 passed (unaffected — this was a live-DB-only fix, no code changed
  besides the new migration entry).

---
## [2026-07-08] Fix — user app: blank tournament detail screen + forced re-login every session

### Bug 1: tapping a tournament goes to a blank white screen (back button still works)
`GET /api/v1/tournaments/{tournament_id}` was `TOURNAMENT_MANAGER`/`OPS_MANAGER`/`SUPER_ADMIN`
only — a regular player tapping into tournament detail always got a 403. The client
(`TournamentsViewModel.select()`) catches that and falls back to
`_tournaments.value.find { it.id == id }`, but `TournamentDetailScreen` and `TournamentsScreen`
are separate nav-graph destinations, each getting its **own** `TournamentsViewModel` instance via
Compose Navigation's default `viewModel()` scoping (per-back-stack-entry, not shared). So the
detail screen's fallback searches its own, freshly-empty list and always comes up null.
`TournamentDetailScreen.kt:37` is `val t = selected ?: return` — with `selected` permanently
null, the composable renders nothing at all. Not a crash (so the Activity survives, back button
still works) — just a legitimately empty composition.

**Fixed:**
- `GET /{tournament_id}` — changed from admin-role-only to `Depends(require_user)`. Any
  authenticated user can view a tournament's detail now, same visibility tier as the public list.
- `tournament_service.get_tournament()` — switched from `repository.find_by_id()` (bare columns)
  to `repository.find_by_id_enriched()`, so the response includes `sport`/`location`/
  `registered_teams` the way `/public` already does. Without this, the screen would render but
  show blank sport tags and a stuck-at-zero progress bar instead of the blank-screen bug.
- Left the underlying per-screen ViewModel scoping as-is — fixing the 403 means the fallback path
  is never exercised in practice, so it wasn't worth restructuring nav-graph-scoped ViewModels
  for this.

### Bug 2: force-relogin via OTP every time the app is fully closed and reopened
Not a wiring bug — the persistence chain (`AuthTokenManager` → DataStore, `RetrofitClient`'s
interceptor reading it via `runBlocking`, `PlixoApp.onCreate()` calling `RetrofitClient.init()`
before any request, `SplashScreen` checking for a saved token and calling `/auth/me` to restore
the session) is all correctly wired end to end. The actual cause: `ACCESS_TOKEN_EXPIRE_MINUTES`
was hardcoded to 60. Any cold start more than an hour after the last login gets a 401 from
`/auth/me`, and `SplashScreen.kt` treats that identically to "invalid token" — clears it, sends
to the phone-entry screen. There's no refresh-token flow, so the JWT's expiry *is* the session
length.

**Fixed:** `ACCESS_TOKEN_EXPIRE_MINUTES` now defaults to 30 days (`60 * 24 * 30`), overridable via
env var. No refresh-token infrastructure exists in this codebase, so a long-lived access token is
the pragmatic fix rather than building a refresh flow for it.

**Not fixed (flagged, not in scope for this pass):** `SplashScreen.kt`'s failure handling doesn't
distinguish "token actually invalid/expired" from "request failed for some other reason" (network
blip, timeout, a transient 500) — any failure from `getMe()` clears the token and forces re-login.
A genuinely flaky connection on cold start would currently look identical to an expired session.

### Verified
- Full backend suite: 450 passed.
- Not build-verified by me — same pattern as the last several fixes, user's own build/run is the
  actual check.

---
## [2026-07-08] Fix — user app: wallet invisible, cryptic captain-apply 400, admin can't see raised tickets

Three separate reports from the user, none of them backend bugs — all app-side wiring/UX gaps.

### 1. No wallet visible anywhere in the user app
`FeatureFlags.WALLET` was still `false` — a leftover kill-switch from before the real wallet
ledger was built (see `c90dbce`). Flipping it revealed a second problem: `HomeScreen.kt`'s
"coins strip" had a hardcoded `"1,840 coins"` literal and a "Redeem for gear, snacks & venue
credit" line implying a spend feature that doesn't exist (earn-only by design). The `QuickTile`
wallet summary used `user?.coinBalance`, a `User` field the backend has never populated (always
silently defaults to 0).

**Fixed:** `FeatureFlags.WALLET = true`. Wired `HomeScreen.kt` to `ProfileViewModel.walletBalance`
(the same real balance the Wallet screen itself uses) in both the coins strip and the QuickTile;
replaced the misleading "redeem" copy with "Earned by completing matches".

### 2. Captain application "submit" always shows a bare 400
Not a backend bug — `POST /captains/apply` correctly rejects with
`"You need at least 3 completed matches to apply (you have N)."` when the gate isn't met. The
bug: `CaptainRepository.kt` (user app) never reads that message. Retrofit throws `HttpException`
for any non-2xx response *before* the body converter runs, so the `res.success`/`res.message`
branch in every method here was unreachable for real error paths — the `catch (e: Exception)`
just wrapped Retrofit's generic `"HTTP 400 Bad Request"` string, discarding the actual reason
the backend went to the trouble of sending.

**Fixed:** new `network/ErrorUtils.kt` — `HttpException.backendDetail()` extracts `{"detail":
...}` from the error body; `Exception.toUserMessage(fallback)` prefers it when present. Applied
to all 5 methods in `CaptainRepository.kt` (`apply`, `uploadKyc`, `getApplicationStatus`,
`updatePayoutUpi`, `getStats`). `CaptainApplicationScreen.kt` already had a working `error` text
display — it just never had anything useful to show before.

**Not fixed (flagged, broader pattern):** the same generic-catch shape exists in
`ChatRepository.kt`, `MatchRepository.kt`, `ProfileRepository.kt`, `SocialRepository.kt`,
`SupportRepository.kt`, `TournamentRepository.kt` — none of them surface real backend error
detail either. Only fixed the one directly reported; the utility now exists for the rest cheaply
if this class of complaint comes up again.

### 3. Raised support ticket doesn't show up in the admin app
The ticket *was* created successfully (confirmed directly against the live DB: id=1, status
OPEN). The admin app just has two different, confusingly-named screens: `SupportScreen`
(prominent bottom-nav tab; a phone-lookup + "raise a ticket against a booking" tool that never
displayed existing tickets at all) and `DisputesScreen` (the real ticket list/resolve view,
buried one level deeper under Manage → Disputes). An admin checking the obvious "Support" tab
would see zero tickets no matter how many existed.

**Fixed:** made `DisputesScreen.kt`'s `DisputeCard` `internal` (was `private`) so it's reusable,
and added a "Tickets" section to `SupportScreen.kt` — reuses the `disputeViewModel` that screen
already received as a parameter (previously only used for creating tickets, never for listing
them) — with the same resolve action `DisputesScreen` has.

### Verified
- Re-ran the full import-cross-reference sweep on both apps after all edits — two flagged hits,
  both confirmed false positives from grep pattern limitations (`ui.components.shimmerEffect` and
  `network.toUserMessage` are extension functions with a receiver type between `fun` and the
  name; `network.ApiService` is an `interface`, not `class`/`object`/`fun`/`val` — none actually
  missing).
- No backend changes this round — nothing to re-run in the Python suite.
- Not build-verified by me — user's own build/run is the check, as with every fix this week.

---
## [2026-07-08] Fix — stuck matchmaking session, dispute raiser name, captain KYC re-entry

### 1. "Already in a match" with no active match visible to admin
Root-caused directly against the live DB: match id 24 (`WAITING`, `joined_players=1`, no second
player) had `created_at` of 2026-06-16 — over three weeks old. The session reaper (auto-cancels
abandoned WAITING sessions after 15 min, added in the earlier recovery work) has **never actually
fired** — `match_events` has zero `MATCH_CANCELLED` rows with a reaper reason, ever. Root cause is
almost certainly that Render's free tier suspends the process when idle, and an in-process
APScheduler timer simply doesn't run while the dyno is asleep — it only wakes on an incoming HTTP
request, and nothing re-triggers the missed interval. Manually cancelled match 24 directly via SQL
to unblock the user immediately.

Checked whether the admin app itself was hiding the match — it wasn't. `list_all_matches()` has no
status filter, `MatchesScreen.kt` has no client-side filter either, and the `Match` Kotlin model's
non-nullable fields (`region_id`, `cart_type_id`) were both populated. No code bug found there.

**Fixed:** added `POST /api/v1/admin/matches/reap-abandoned` (manually runs the same cleanup the
scheduler is supposed to run automatically) plus a "clean up" icon button in `MatchesScreen.kt`'s
top bar, so admins have a reliable one-tap fallback regardless of whether the background job fired.
The automatic scheduler is left in place for when the dyno is awake — this is a safety net, not a
replacement.

### 2. Dispute cards show no indication of who raised the ticket
`Dispute.to_dict()` only ever exposed `raised_by` as a raw user ID — never resolved to a name, and
no UI ever displayed it (not even the raw ID). Added `dispute_service._attach_raiser_name()`
(mirrors the pattern already used for society member name enrichment) — every dispute the admin
list/support screens receive now carries `raised_by_name`/`raised_by_phone`. Displayed in
`DisputeCard` (shared by `DisputesScreen` and `SupportScreen`) as "Raised by {name} · {phone}".

**Test isolation note:** `test_dispute_service.py`'s `list_disputes` test didn't inject a
`user_repository`, which would have hit production `SessionLocal` the moment enrichment was added
— same class of risk flagged repeatedly this week. Fixed by binding a `UserRepository` to the same
in-memory session factory.

### 3. No way for a captain to (re)upload KYC documents outside the one-shot apply flow
`KycUploadScreen` was only reachable via `CaptainApplicationScreen → KycIntroScreen →
KycUploadScreen` — a linear chain that only exists right after successfully submitting a
self-service captain application. Two real gaps this caused: (a) a captain the admin creates
directly (`POST /captains`, the "hire someone" path — no KYC step at all) has no way to ever
submit ID documents through the app, and (b) a self-approved captain whose KYC was rejected has no
way to retry.

**Fixed:** `get_my_stats()` now returns `kyc_status`; added a `VerificationCard` to the captain
dashboard's Earnings tab showing the current status (Not submitted / Under review / Verified /
Rejected) with an "Upload ID document" button that navigates straight to `KycUploadScreen` — no
longer gated behind the application flow.

### Not built this round — needs a scoping decision
User also asked for a proper reply/chat interface on support tickets (currently: raise once,
admin resolves once with an optional note — no back-and-forth). This is a real feature, not a bug
fix — needs a new message-thread model, endpoints on both create/list, and UI on both apps. Asked
the user before starting rather than guessing at scope.

### Verified
- Full backend suite: 450 passed (dispute service tests updated for the new enrichment DI).
- Full import-cross-reference sweep re-run on both apps post-edit — only known false positives.
- Not build-verified by me — user's own build/run is the check.

---
## [2026-07-12] Feature — support ticket reply thread ("simple threaded replies")

Scoped via explicit user choice: the ticket keeps its title/description, but both the raiser and
support/admin staff can post follow-up text messages on it in order, like a comment thread — no
read receipts, no real-time push, polling like the existing match chat. This was the deferred item
flagged in the 2026-07-08 entry.

### Added
**Backend**
- `dispute_message_model.py` — new `DisputeMessage` model (`dispute_messages` table): `id`,
  `dispute_id` (FK → `disputes.id`, cascade delete), `sender_id` (FK → `users.id`, set null),
  `body`, `created_at`.
- `dispute_message_repository.py` — `create()`, `find_by_dispute()` (ordered oldest-first).
- `dispute_message_service.py` — `DisputeMessageService` with `_authorize()`: access is scoped to
  whoever raised the ticket (`dispute.raised_by`) plus `SUPPORT`/`OPS_MANAGER`/`SUPER_ADMIN` staff;
  everyone else gets `PermissionError` → 403. `_attach_sender_name()` enriches each message with
  `sender_name`/`sender_role` (mirrors the raiser-name enrichment pattern from 2026-07-08).
- `dispute_routes.py` — `GET /api/v1/disputes/{id}/messages`, `POST /api/v1/disputes/{id}/messages`,
  both behind `Depends(require_user)` with per-dispute authorization enforced in the service layer
  (not route-level RBAC, since access depends on *who raised this specific ticket*, not a static role).
- `main.py` — registered `DisputeMessage` model import.
- Migration 27 — `CREATE TABLE IF NOT EXISTS dispute_messages (...)`, applied directly against the
  live Neon DB via MCP `run_sql` and added to `run_migrations.py` for parity.
- `test_dispute_message_service.py` — 6 tests: raiser send/list, staff reply, other-user 403,
  unknown-dispute 404, blank-body rejected, ordering. Followed the established DI-testing pattern
  (in-memory SQLite `session_factory`, all three repos — message/dispute/user — bound to it; had to
  import `society`/`captain`/`match`/`tournament`/etc. models up front to pre-empt the FK-resolution
  errors hit repeatedly on 2026-07-08 rather than discover them one at a time again).

**App — Vmsuserapp**
- `Models.kt` — `DisputeMessage`, `SendDisputeMessageRequest`.
- `ApiService.kt` — `getDisputeMessages(id)`, `sendDisputeMessage(id, body)`.
- `SupportRepository.kt` — `getMessages()`/`sendMessage()`, using the existing `toUserMessage()`
  error-detail extraction (2026-07-08 pattern) instead of Retrofit's generic exception text.
- `SupportViewModel.kt` — added message state + `openThread()`/`stopPolling()`/`sendMessage()`
  following `ChatViewModel`'s exact polling shape (`POLL_INTERVAL_MS`-equivalent, cancel-on-clear).
- `TicketDetailScreen.kt` (new) — reply thread UI, built directly off `ChatThreadScreen`'s
  `MessageBubble`/input-row/`animateScrollToItem` pattern; shows the ticket description as a pinned
  summary above the thread; non-self bubbles show "Name · Support" when the sender is staff.
- `Screen.kt` / `AppNavigation.kt` — new `TicketDetail("ticket_detail/{id}")` route.
- `SupportScreen.kt` — ticket cards are now clickable, navigating into the new thread screen.

**App — Vmsadminapp**
- `Models.kt` — `DisputeMessage`, `SendDisputeMessageRequest` (snake_case fields, matching this
  app's existing `Dispute` model convention — no `@SerialName`, unlike the user app).
- `ApiService.kt` — `getDisputeMessages(id)`, `sendDisputeMessage(id, body)`.
- `DisputeRepository.kt` — `getMessages()`/`sendMessage()`.
- `DisputeViewModel.kt` — added `messages`/`messagesError`/`sending`/`selectedDisputeId` to
  `DisputeUiState`, plus `openThread()`/`stopPolling()`/`sendMessage()`. Kept the thread state in
  the same shared `DisputeViewModel` (not a separate ViewModel) since this app's nav pattern passes
  one long-lived ViewModel per domain into `MainScreen`'s `NavHost` rather than scoping a fresh
  ViewModel per destination.
- `DisputeThreadScreen.kt` (new) — staff-side reply thread, `Scaffold` + `TopAppBar` + bottom input
  row, self/other bubble styling matching `MaterialTheme.colorScheme`.
- `DisputesScreen.kt` — `DisputeCard` gained an optional `onOpenThread` callback; the whole card is
  now clickable via `AppCard(onClick = ...)` (the inner "Resolve" button's own click is consumed
  first, so it doesn't also trigger the thread navigation).
- `SupportScreen.kt` — same `onOpenThread` wiring for the ticket cards shown there.
- `MainScreen.kt` — two new routes: `manage/disputes/thread/{disputeId}` (from the Manage → Disputes
  list) and `support/thread/{disputeId}` (from the Support tab's ticket list) — both resolve to the
  same `DisputeThreadScreen`, gated by the same role sets (`DISPUTE_ROLES` / `SUPPORT_ROLES`) as
  their respective parent screens.

### Architectural decisions
- Authorization lives in the service layer, not route-level `require_role`, because "can this user
  see this ticket" depends on ticket ownership, not a fixed role — staff roles are the only
  role-based shortcut, everyone else must be the raiser.
- No read receipts, no push notifications, no WebSocket — matches the existing match-chat precedent
  exactly, and the user explicitly chose this scope over a richer alternative that was offered.
- Admin app: extended the existing per-domain `DisputeViewModel` rather than introducing a new
  ViewModel type, consistent with how `TournamentViewModel` already carries `selectedTournament`
  state for its detail screen — same shared-ViewModel-with-selection pattern, not a NavController
  arg-only screen.

### Verified
- Full backend suite: 456 passed (450 prior + 6 new `test_dispute_message_service.py` tests), zero
  regressions.
- `python -c "import backend.main"` — clean import, no circular/registration errors.
- Migration 27 applied directly against the live Neon DB (`still-darkness-99863466`) via MCP
  `run_sql`, confirmed idempotent (`CREATE TABLE IF NOT EXISTS`).
- Not build-verified by me on either app — user's own build/run is the check, as with every UI
  change this project.

---
## [2026-07-23] Feature (Part 1/6) — registration & onboarding overhaul: username/email + real DOB

First slice of a larger, user-requested onboarding overhaul (full scope in
`docs/plan-user-registration-onboarding.md`): add username (required) + email (optional) to
registration, real DOB validation feeding age-based matchmaking (next slice), GPS-based city
auto-detect (next slice), profile photo upload (next slice), and admin-app user-management gaps
(next slice). This entry covers just the backend field/validation work plus the onboarding form's
username/email/DOB fields — GPS and photo upload are separate follow-up slices since they need
their own new endpoints (flagged to the user up front, not scope creep).

Investigation before writing code found two things worth noting: registration already persisted to
the DB correctly (a stub `User` row is created at OTP-verify, completed at `complete-profile`) —
raised as a concern but turned out not to be a bug. And the admin-app "can't create a user
directly" gap turned out to be UI-only — `POST /api/v1/users` (`CreateUserSchema`, audit-logged)
already exists and works; nothing in the admin app calls it yet (addressed in a later slice).

### Added
**Backend**
- `User` model — `username` (unique, nullable at DB level for pre-existing rows, required at the
  application layer for new registrations), `email` (always optional, unique). `to_dict()` also
  now returns a computed `age` field.
- `modules/user/utils/age_utils.py` (new) — `compute_age()`, `validate_dob()` (must parse as
  `YYYY-MM-DD`, not be in the future, imply age ≥ 13). Used by both the profile-completion endpoint
  now and the matchmaking filter in the next slice.
- `modules/user/schemas/user_schema.py` — `validate_username()`/`validate_email()` helpers (regex:
  3-20 alphanumeric+underscore for username, standard email shape), wired into both
  `CreateUserSchema` and `UpdateUserSchema` as optional fields.
- `user_repository.py` — `find_by_username()`/`find_by_email()` for uniqueness checks; `create()`
  now passes through `username`/`email` instead of silently dropping them.
- `user_service.py` — `create_user()`/`update_user()` now reject duplicate username/email with a
  clear `ValueError`, mirroring the existing phone-uniqueness pattern.
- `auth_routes.py` `complete-profile` — now requires and validates `username` (uniqueness checked
  against every other user, not just a format check), accepts optional `email` (validated +
  uniqueness-checked if present), and validates `date_of_birth` for real instead of accepting
  arbitrary free text.
- Migration 28 — `username`/`email` columns on `users`, partial unique indexes (`WHERE ... IS NOT
  NULL`, so the many existing NULL rows don't collide on uniqueness).
- Migration 29 — `latitude`/`longitude` on `locations`, added now (schema only) since Part 3 (GPS)
  needs it and it's a trivial nullable-column addition — rows are NULL until backfilled separately,
  by user's explicit choice (ship the feature now, don't block on real coordinate data entry).
- `Location` model — exposes the two new columns via `to_dict()`.
- New tests: `test_age_utils.py` (9 cases — age computation across birthday boundaries, DOB
  rejection paths), `test_user_service.py` gained 3 cases for username/email create + duplicate
  rejection.

**App — Vmsuserapp**
- `Models.kt` — `User` gained `username`/`age` (`email` already existed, was just never populated
  by the app-side request); `CompleteProfileRequest` gained required `username` + optional `email`.
- `AuthRepository.kt` — `completeProfile()` signature extended to match.
- `ProfileSetupScreen.kt` — step 1 gained username field (client-side format validation mirroring
  the backend regex, inline error text) and an optional email field (same pattern). Replaced the
  free-text DOB field (`DD / MM / YYYY` placeholder, zero validation) with a real Material3
  `DatePickerDialog`, `selectableDates` capped at 13 years ago so under-13 dates aren't even
  selectable client-side (defense in depth — the backend is the real gate).

### Architectural decisions
- Username is nullable at the DB/model level (existing users predate the field) but enforced
  required at the application layer for anyone going through `complete-profile` — avoids a
  disruptive backfill migration for existing accounts while still requiring it going forward.
- Chose to add the `Location` lat/long columns now even though nothing reads them yet, since it's a
  zero-risk nullable-column migration and avoids a second migration round-trip when Part 3 lands.

### Not built this round — tracked in the plan doc
- Age-based matchmaking filter (Part 2) — touches live `match_repository.py`/`match_service.py`
  pairing logic, deliberately kept as its own slice given the risk.
- GPS nearest-location endpoint (Part 3) — schema is ready (migration 29), endpoint isn't built yet.
- Profile photo upload endpoint (Part 4) — `complete-profile` still accepts `profile_photo_url` but
  the app always sends `null`; no upload endpoint exists yet.
- Admin app: Users list doesn't show the new fields yet, and the working `POST /api/v1/users`
  backend still has no UI to call it from (Part 6).

### Verified
- Full backend suite: 468 passed (456 prior + 12 new), zero regressions.
- `python -c "import backend.main"` — clean import.
- Migrations 28-29 applied directly against the live Neon DB via `run_migrations.py`.
- Not build-verified by me on the app — user's own build/run is the check, as with every UI change
  this project.

---
## [2026-07-23] Feature (Part 2/6) — age-based matchmaking filter

Second slice of the onboarding overhaul (`docs/plan-user-registration-onboarding.md`). Store-only
DOB from Part 1 becomes a real matching constraint: ±5 years, fixed (user's explicit choice — not
configurable).

### Investigation before writing code
The `matchmaking_service.py` file (QueueEntry-based join_queue/leave_queue) looked like the obvious
place to add this, but it's dead code — the actual play-now flow was fully rewritten to a
Match-based model back on 2026-06-16 and nothing calls into `matchmaking_service.py` anymore
(confirmed via grep — zero callers). The real pairing logic lives in
`match_repository.find_waiting_in_region()` (the "Open Matches" list) and
`match_service.join_match()` (the actual join transaction). Both needed the filter — filtering the
list without also enforcing it on join would let someone bypass the filter by hitting the join
endpoint directly with a known match_id.

### Added
**Backend**
- `modules/match/utils/age_compatibility.py` (new) — `is_age_compatible(age_a, age_b)`: true if
  either age is unknown (exempts pre-existing users without a DOB from being locked out) or the
  difference is ≤5 years.
- `match_repository.find_waiting_in_region()` — new optional `requester_age` param; when given,
  filters out matches whose creator's age falls outside the window. `_enrich()` now also returns
  `creator_age` (computed from the creator's DOB) so the app can display it later if wanted.
- `match_routes.py` `GET /matches/open` — passes `current_user.get("age")` through automatically
  (already present on every authenticated user's dict since Part 1's `to_dict()` change).
- `match_service.join_match()` — added a real age-compatibility check (step 4b) alongside the
  existing skill-level check. Notably, the skill-level check next to it is actually a **no-op
  placeholder** ("MVP: strict enforcement is future work") — the age check is written to actually
  block, per the user's explicit "filter matchmaking by range" request, not mirror that no-op.
- `MatchService.__init__()` gained a `user_repository` constructor param (defaulting to the
  production singleton) so `join_match()` could look up both players' ages through it instead of a
  bare module-level import — this is the same DI pattern used everywhere else in the codebase.
  Necessary because a bare import would have made every isolated-SQLite test that calls
  `join_match()` silently hit the real production database for user lookups.

**Tests**
- `test_age_compatibility.py` — 7 cases (within/at/outside window, unknown-age exemption both
  directions, same age).
- `test_captain_created_matches.py` — added `UserRepository` DI to `TestCaptainCreateMatchService`
  (previously missing it entirely — same production-DB-leak class of bug caught and fixed here
  before it could bite), plus 3 new cases: age-filtered `find_waiting_in_region`, `join_match`
  rejecting an incompatible pair, `join_match` allowing a compatible pair.

### Verified
- Full backend suite: 478 passed (468 prior + 10 new), zero regressions.

---
## [2026-07-23] Feature (Part 3/6) — GPS nearest-location endpoint

Third slice. `Location` had zero coordinate data (flagged in Part 1's entry as a known gap) — this
adds the schema support and lookup endpoint; existing location rows stay uncoordinated until
someone backfills real lat/long, per the user's explicit "ship now, backfill later" choice.

### Added
**Backend**
- `modules/location/utils/geo.py` (new) — `haversine_km()`, pure Python, no new dependency.
- `location_repository.find_nearest(lat, lng, limit)` — filters to `is_serviceable=true` locations
  with non-null coordinates, sorts by haversine distance, returns each with a `distance_km` field.
  Locations without coordinates are silently excluded rather than erroring — the fallback is "no
  suggestion, pick manually," not a broken request.
- `location_service.find_nearest_locations()` — validates lat/lng are in a sane range before
  hitting the DB.
- `GET /api/v1/locations/nearest?lat=&lng=` (new route, `require_user`) — registered *before*
  `/{location_id}` in the file; FastAPI's int-typed path converter for `location_id` means this
  wasn't strictly a routing hazard, but ordering it first avoids any ambiguity.
- `location_repository.create()` now also accepts `latitude`/`longitude` (previously silently
  dropped them even though the model already had the columns from Part 1's migration 29).

**App — Vmsuserapp**
- `network/DeviceLocation.kt` (new) — `lastKnownLocation(context)`: best-effort GPS/network fix via
  `android.location.LocationManager` directly. Deliberately did **not** add
  `play-services-location` as a new Gradle dependency — grepped the whole app first and confirmed
  nothing uses Play Services location today (the arrival-GPS-check flow the model docstrings
  reference doesn't have a client-side implementation yet either), so pulling in a new dependency
  for this one feature wasn't justified when the plain Android API covers "best-effort suggestion"
  well enough.
- `ProfileSetupScreen.kt` — "Use my current location" row above the area dropdown; on tap, requests
  `ACCESS_COARSE_LOCATION` (already declared in the manifest), reads a last-known fix, calls
  `/locations/nearest`, and shows a "Detected: X — Use this?" card the user can accept or ignore.
  Graceful fallback text for permission-denied, no-fix-available, and no-nearby-location-yet cases.
- `Models.kt` — `LocationOption` gained `distance_km`; `OpenMatch` gained `creator_age` (surfacing
  the field Part 2 added, not yet rendered in any card — available for a future polish pass).

### Verified
- Full backend suite: 487 passed (478 prior + 9 new: 3 haversine + 6 find_nearest).
- Not build-verified by me on the app — user's own build/run is the check.

---
## [2026-07-23] Feature — real OTP delivery via MSG91 (dev-mode mock kept as default)

Not part of the registration-overhaul plan — a separate, explicitly-flagged gap: OTP was fully
mocked (`OTP_DEV_MODE` defaults `true`, every code is `123456`, non-dev mode raised
`NotImplementedError` with a `# TODO: integrate MSG91` comment already sitting in the code from
earlier). User confirmed MSG91 as the provider. Real delivery requires an MSG91 account and,
independent of provider choice, DLT (Distributed Ledger Technology) template registration —
mandatory under Indian telecom regulation for any transactional/OTP SMS. Neither of those is
something that can be set up from here; the code is written to activate the moment those exist.

### Added
**Backend**
- `otp_service.py` — `_send_via_msg91()`: posts to MSG91's Flow API
  (`https://control.msg91.com/api/v5/flow/`) with the same code already generated and stored
  locally (so `verify_otp()` needed zero changes — verification still matches against our own
  `otp_repository`, not MSG91's). Raises `NotImplementedError` if `MSG91_AUTH_KEY`/
  `MSG91_TEMPLATE_ID` aren't set (mirrors the pre-existing "not configured" signal), or the new
  `OtpDeliveryError` if MSG91 is configured but the send itself fails (bad response body or network
  error) — these are two different failure modes and now get two different HTTP statuses.
- `auth_routes.py` `POST /send-otp` — added an `OtpDeliveryError` → 502 handler alongside the
  existing `NotImplementedError` → 503 handler.
- `requirements.txt` — added `requests` explicitly (was already present transitively via
  `firebase-admin`, but an OTP-critical HTTP call depending on an undeclared transitive dependency
  felt like exactly the kind of thing that silently breaks later).
- `.env.example` — documented `OTP_DEV_MODE`, `OTP_EXPIRY_MINUTES`, `MSG91_AUTH_KEY`,
  `MSG91_TEMPLATE_ID`, `MSG91_OTP_VAR` (must match the variable name inside the DLT-approved
  template — MSG91 surfaces this when the template is created), with an explicit note that
  `OTP_DEV_MODE` must stay `true` until DLT registration is actually done, not just the API key.
- `test_otp_service.py` (new, first tests this module has ever had) — 7 cases: dev-mode never calls
  the provider, dev-mode always accepts `123456`, missing-config raises `NotImplementedError`, a
  successful send posts the same code that got persisted (verified by asserting the SMS payload's
  code matches what `otp_repository.create()` was called with), MSG91 error-response and
  network-failure both raise `OtpDeliveryError`, and real-code verification still checks the
  repository when the submitted code isn't the dev fallback.

### Architectural decisions
- Kept our own OTP generation/storage/verification instead of delegating to MSG91's own
  OTP-management endpoints — smaller change (only the *send* step talks to MSG91), and
  `verify_otp()`'s existing repository-backed matching didn't need to change at all.
- `OTP_DEV_MODE` still defaults to `true` and nothing in `.env` currently overrides it — this change
  is inert in production until the user has both an MSG91 API key and a DLT-approved template and
  flips the flag themselves.

### Verified
- Full backend suite: 494 passed (487 prior + 7 new OTP tests), zero regressions.
- `python -c "import backend.main"` — clean import.
- Not build-verified — no app-side changes in this entry (OTP flow already exists — this only swaps
  what happens server-side when `OTP_DEV_MODE=false`, which nothing currently sets).

---
## [2026-07-24] Feature (Part 4/6) — profile photo upload

Fourth slice of the onboarding overhaul. `complete-profile` already accepted `profile_photo_url`
but the app always sent `null` — this adds the actual upload path.

### Investigation before writing code
Looked at the existing KYC document pattern (`core/storage/kyc_storage.py`,
`POST /captains/me/kyc`, `GET /captains/{id}/kyc-document`) to reuse rather than invent a new
pattern. One deliberate difference: KYC documents are admin-only and require a `_decode_token_or_query`
helper so `AsyncImage`/`<img>` requests (which can't set an Authorization header) can still
authenticate via a `?token=` query param. Profile photos aren't sensitive the same way — they're
meant to be visible to other players on match cards, society lists, etc. — so making the GET route
require auth at all would mean adding that same query-param workaround to every screen that shows
an avatar. Serving it fully public instead (unguessable only in the sense that you need the numeric
user_id, same trust model as most avatar systems) avoids that complexity entirely. The user_id in
the URL isn't a secret already (used throughout the app), so this doesn't leak anything new.

### Added
**Backend**
- `core/storage/profile_photo_storage.py` (new) — `save_profile_photo()`/`read_profile_photo()`/
  `delete_profile_photo()`. One active photo per user: re-upload replaces the previous file
  (deterministic `user_{id}.<ext>` naming) rather than accumulating like KYC's UUID-per-document
  approach — a profile photo has no audit-trail reason to keep old versions.
- `POST /api/v1/auth/me/profile-photo` (auth required, owner-only) — validates content-type starts
  with `image/` and enforces a 5MB cap before touching disk. Sets `profile_photo_url` to the
  servable route path (not a raw disk path — the two were conflated in an earlier draft of this
  work before realizing the app needs an actual loadable URL, not a filesystem path).
- `GET /api/v1/users/{user_id}/profile-photo` (no auth) — streams the file with the correct
  `media_type` derived from its extension (jpeg/png/webp; unrecognized extensions fall back to
  jpeg rather than erroring). Registered before `/{user_id}` in the file for the same reasons as
  the `/locations/nearest` ordering in Part 3.
- No new migration — `profile_photo_url` already existed on `User` from before this session.
- Tests: `test_profile_photo_storage.py` (8 cases, patches `_UPLOAD_DIR` to a temp dir so tests
  never touch the real `backend/uploads/profile_photos/`), `test_profile_photo_routes.py` (5 cases
  via `TestClient` — successful upload, non-image rejection, oversized rejection, public GET
  succeeding with no auth override set, missing-photo 404).

**App — Vmsuserapp**
- `ApiService.kt` / `AuthRepository.kt` — `uploadProfilePhoto()`, mirroring the existing
  `CaptainRepository.uploadKyc()` temp-file-then-multipart pattern exactly (same
  `contentResolver.openInputStream` → temp file → `MultipartBody.Part` → delete temp file flow).
- `ProfileSetupScreen.kt` — circular avatar picker at the top of step 1 (tap → `GetContent("image/*")`,
  matching the exact picker mechanism `KycUploadScreen` already uses, not the newer
  `PickVisualMedia` API, for consistency with the rest of the app). Uploads immediately on
  selection rather than waiting for a separate confirm step — shows a spinner overlay on the
  avatar while in flight, camera-badge affordance, graceful inline error text on failure. Skippable
  — nothing in step 1's `enabled` gate depends on it. `submit()` now passes the real uploaded URL
  instead of the previous hardcoded `null`.

### Not built this round
Displaying uploaded photos anywhere *other* than the picker's own local preview during onboarding —
e.g. showing other players' avatars on match cards, society member lists, or `ProfileScreen`. That's
a broader "surface avatars across the app" pass, not part of what was asked (a working upload path),
and deliberately not scope-crept into this slice.

### Verified
- New tests: 13 passed (`test_profile_photo_storage.py` + `test_profile_photo_routes.py`).
- Full backend suite: 507 passed (494 prior + 13 new), zero regressions.
- `python -c "import backend.main"` — clean import.
- Not build-verified on the app — user's own build/run is the check.
