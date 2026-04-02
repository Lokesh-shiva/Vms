# Phase 08: Pricing & Queue UX Perception - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Attach human-readable natural-language strings to two existing API responses:
1. `matchmaking/status` (`get_queue_status()`) — add `wait_estimation_msg` field
2. `PricingService.calculate_price()` — add `reason` field to the return dict

This phase is backend-string-generation only. No new endpoints, no frontend changes, no new models.

</domain>

<decisions>
## Implementation Decisions

### D-01: Wait Message Format (WAITING state)
Use player-count-first with time estimate:
- Template: `"{N} players nearby — match likely in {X} mins"`
- N = `queue_count` (from pricing sub-object)
- X = `estimated_wait_seconds ÷ 60` (rounded, min 1)
- Example: `"2 players nearby — match likely in 2 mins"`

### D-02: Wait Message — Empty Queue (queue_count = 0)
- Message: `"No players nearby yet — hang tight"`
- Do NOT reveal exact queue size or estimate; keep it neutral.

### D-03: Wait Message — MATCHED State
Replace the search message entirely:
- Message: `"You're matched — arrive in 20 mins or lose your spot"`
- This reinforces the arrival deadline from Phase 06.

### D-04: Wait Message Placement
- Add `wait_estimation_msg` as a **top-level field** in `get_queue_status()` return dict.
- Sits alongside `entry`, `pricing`, `players_searching`, `estimated_wait_seconds`, `match_id`.
- The field is always present regardless of status.

### D-05: Pricing Reason — Separate per Trigger
Three distinct reason strings based on active surge trigger(s):
- **Demand surge** (`demand_factor > 1.0`): `"High demand in your area"`
  - Applies when `queue_count ≥ 5` (1.25×) or `queue_count ≥ 10` (1.5×)
- **Peak hours** (`time_factor > 1.0`): `"Peak hours (5–9 PM)"`
  - Applies when current hour is in [17, 20]
- **Both active** (demand AND peak): Return demand reason only (demand is primary driver and more actionable for user)
  - Alternative: could combine as "Peak hours + high demand" — but user chose separate, so demand takes precedence
- **No surge** (`time_factor = 1.0` AND `demand_factor = 1.0`): `"Standard pricing"`

### D-06: Pricing Reason Placement
Add `reason` field inside `PricingService.calculate_price()` return dict only.
Since `get_queue_status()` already calls `calculate_price()` and embeds the full pricing dict, `reason` automatically propagates to the queue status response's `pricing` sub-object.
No additional changes needed in `get_queue_status()` for pricing reason.

### Claude's Discretion
- Exact threshold logic for which surge reason to show when both triggers fire (demand takes precedence per D-05)
- Whether to store reason strings as module-level constants or inline — either is acceptable
- `estimated_wait_seconds` already exists in `get_queue_status()`; `wait_estimation_msg` is derived from the same data, no new DB queries needed

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Matchmaking
- `modules/matchmaking/service/matchmaking_service.py` — `get_queue_status()` return structure and `_WAIT_PER_PLAYER_SECONDS` constant (line 9). Wait message goes here.
- `modules/matchmaking/controller/matchmaking_routes.py` — `/status` route handler that calls `get_queue_status()`

### Pricing
- `modules/pricing/service/pricing_service.py` — `calculate_price()` method and `_DEMAND_TIERS` / `_PEAK_START` / `_PEAK_END` constants. `reason` field goes in the return dict here.
- `modules/pricing/controller/pricing_routes.py` — `/calculate` route that calls `calculate_price()`

### Prior Phase Context
- `.planning/phases/06-arrival-deadlines-penalties/06-CONTEXT.md` — 20-min arrival deadline decision (D-03 references this)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PricingService.get_time_factor()` — Already returns 1.5 for peak hours, 1.0 otherwise. Check `time_factor > 1.0` for peak detection.
- `PricingService.get_demand_factor()` — Already returns 1.25/1.5 for demand tiers. Check `demand_factor > 1.0` for demand detection.
- `_WAIT_PER_PLAYER_SECONDS = 120` — Used in `get_queue_status()` to compute `estimated_wait_seconds`. Reuse this for message generation.

### Established Patterns
- `calculate_price()` returns a plain `dict` — add `reason` key to same dict (no schema changes needed)
- `get_queue_status()` returns a plain `dict` — add `wait_estimation_msg` key to same dict
- No serialization layer — returns go straight to FastAPI response

### Integration Points
- Both methods are already being called by their respective routes; adding fields to their return dicts is sufficient — no route changes needed.

</code_context>

<specifics>
## Specific Ideas

- User specifically wants "Searching for opponent" tone for the wait messages
- "High demand in your area" is the preferred demand surge reason string (user's own words)
- Matched-state message must reference the 20-min deadline to reinforce urgency

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 08-pricing-queue-ux-perception*
*Context gathered: 2026-04-02*
