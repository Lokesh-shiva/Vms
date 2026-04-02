# Phase 07 Context: Match Teardown & Re-Queue
**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary
Handle match cancellation and resource recovery when players fail to arrive. Recover ground inventory (bookings) and prioritize innocent players back into the matchmaking queue.
</domain>

<decisions>
## Implementation Decisions

### 1. Inventory Release (Reuse)
- **D-01:** Reuse `BookingService.cancel_booking(match.booking_id)` for teardowns. This ensures consistent handling of cart release (`status='AVAILABLE'`) and refund logic if any payment was authorized.

### 2. Re-Queue Priority (Historical)
- **D-02:** Players who *did* arrive (`MatchPlayer.has_arrived == True`) are re-queued. 
- **Priority:** Implementation will set `created_at = datetime.utcnow() - timedelta(hours=2)` for the new `QueueEntry` to ensure they are at the front of the FIFO queue.

### 3. UX (Explicit Toast)
- **D-03:** The backend will flag re-queued entries. The frontend (via `matchmaking/status`) should show an explicit toast: "Your opponent didn't show up. You've been prioritized at the front of the queue."

### 4. No-Show Penalties (Strike System)
- **D-04:** Implement a **2-Strike Rule**. Users are excused for their first two no-shows.
- **Strike 1:** Increment `user.ghost_strikes` to 1. No penalty. Match cancelled.
- **Strike 2:** Increment `user.ghost_strikes` to 2. No penalty. Match cancelled.
- **Strike 3:** Reset `user.ghost_strikes` to 0. Issue a 4-hour "Ghosting" penalty (`MatchPenalty`). Match cancelled.
- **D-06:** Multi-no-show: If both players fail to arrive, both are independently evaluated by the strike system.

### 5. Repetition Guard
- **D-05:** Re-queued players should not trigger a second booking hold until a new match is formed. Since `BookingService.create_instant_match_booking` handles the hold atomically, this is naturally managed.

### 6. Persistence
- **D-07:** Add `ghost_strikes` (Integer, default=0) to the `User` model to track state across matches.

</decisions>

<canonical_refs>
## Canonical References
- `modules/match/service/match_engine_service.py` — Location of `cleanup_stale_matches`
- `modules/booking/service/booking_service.py` — `cancel_booking` implementation
- `modules/matchmaking/repository/queue_entry_repository.py` — `create` and `find_and_lock_compatible_pair`
</canonical_refs>
