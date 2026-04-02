## Milestone v1.1 Requirements

### Edge Cases & Inventory
- [x] **EDGE-01**: System must automatically identify matches where the arrival deadline (e.g., 15 mins) has passed and at least one player has not arrived.
- [x] **EDGE-02**: System must cancel matches triggered by the arrival deadline and apply a default penalty profile to the ghosting user.
- [x] **EDGE-03**: System must automatically execute ground booking release logic (call BookingService) when a match is cancelled.
- [x] **EDGE-04**: System must safely re-queue the user who *did* arrive (re-match) without charging them again.

### UX & Retention Strings
- [x] **UX-01**: Matchmaking endpoints (`play-now` and `status`) must return dynamic, human-readable strings explaining the estimated wait time (e.g., "match likely in 1 min", "2 players nearby").
- [x] **UX-02**: Pricing API endpoints must calculate a human-readable `reason` for the dynamically generated demand prices (e.g., "High demand in your area") and attach it to the JSON response.

## Future Requirements (Deferred)
- Websocket implementation for real-time tracking (to replace polling)
- Multi-player match expansion (>2 players per Match)
- Variable penalties scaling by frequency

## Out of Scope
- Building a complex administration UI for evaluating penalty appeals. (Admins will rely directly on DB records for v1.1 to keep scope minimal).

---
*Traceability:*
