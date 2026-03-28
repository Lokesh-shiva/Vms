# Features & Requirements

## v1 Requirements

### Matchmaking Queue
- [x] **QUEUE-01**: User can join the matchmaking queue by providing `sport_id` and GPS region (detected).
- [x] **QUEUE-02**: System returns estimated wait time, players searching count, and dynamic price point upon queuing.
- [x] **QUEUE-03**: User can leave the queue before a match is formed.
- [x] **QUEUE-04**: User can query their queue status and updated wait time.

### Automated Match Creation
- [ ] **MATCH-01**: Service groups `QueueEntry` records by `region_id`, `sport_id`, and `skill_level` range.
- [ ] **MATCH-02**: When 2 compatible players exist, create a `Match` and assign both users to `MatchPlayer`.
- [ ] **MATCH-03**: Upon match creation, system automatically secures a ground using `BookingService`.
- [ ] **MATCH-04**: Concurrency control via DB row locking (`SELECT FOR UPDATE`) prevents duplicate matches.

### Match Lifecycle & Arrival
- [ ] **LIFECYCLE-01**: User can mark themselves as ARRIVED at the ground.
- [ ] **LIFECYCLE-02**: Once both players are ARRIVED, match transitions to IN_PROGRESS.
- [ ] **LIFECYCLE-03**: Admin/System can mark match as COMPLETED, recording the duration.

### Dynamic Pricing
- [ ] **PRICE-01**: Pricing service calculates dynamic price based on base price, time of day, and active queue count.
- [ ] **PRICE-02**: `get_active_queue_count` securely surfaces real-time demand for UI and pricing engine.

### Payments
- [ ] **PAY-01**: Post-match, system triggers payment creation.
- [ ] **PAY-02**: Payment is equally split between the 2 players.

## v2 Requirements
- Multi-player (>2) matchmaking logic.
- Penalty system for players who abandon matching queue post-locking.

## Out of Scope
- Websocket-based real-time match tracking (stick to REST polling for v1).

## Traceability
*(To be filled by roadmap)*
