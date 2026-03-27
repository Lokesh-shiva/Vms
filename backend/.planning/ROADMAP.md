# Execution Roadmap

**5 phases** | **15 requirements mapped** | All v1 requirements covered ✓

| # | Phase | Goal | Requirements |
|---|-------|------|--------------|
| 1 | DB Models & Pricing | Setup core DB schema updates and dynamic pricing logic | PRICE-01, PRICE-02 |
| 2 | Queue Management | Implement QueueEntry CRUD, status endpoints, and concurrency locking | QUEUE-01, QUEUE-02, QUEUE-03, QUEUE-04 |
| 3 | Matching Engine | Implement cron/service logic to group queues and create matches with bookings | MATCH-01, MATCH-02, MATCH-03, MATCH-04 |
| 4 | Match Lifecycle | Add endpoints for arrival, in-progress, and completion states | LIFECYCLE-01, LIFECYCLE-02, LIFECYCLE-03 |
| 5 | Post-Match Payments | Trigger split payments at match completion | PAY-01, PAY-02 |

### Phase Details

**Phase 1: DB Models & Pricing**
Goal: Setup core DB schema updates and dynamic pricing logic
Requirements: PRICE-01, PRICE-02
Success criteria:
1. `QueueEntry` table created and models updated.
2. Pricing module computes correct dynamic prices based on mocked demand factors.

**Phase 2: Queue Management**
Goal: Implement QueueEntry CRUD, status endpoints, and concurrency locking
Requirements: QUEUE-01, QUEUE-02, QUEUE-03, QUEUE-04
Success criteria:
1. User can successfully join and leave the queue via `/matchmaking/play-now` and `/matchmaking/leave`.
2. Wait times and queue counts are returned accurately.

**Phase 3: Matching Engine**
Goal: Implement cron/service logic to group queues and create matches with bookings
Requirements: MATCH-01, MATCH-02, MATCH-03, MATCH-04
Success criteria:
1. Two compatible queued users are correctly grouped into a new Match.
2. Ground booking is automatically created exactly when the match finalizes without race conditions.

**Phase 4: Match Lifecycle**
Goal: Add endpoints for arrival, in-progress, and completion states
Requirements: LIFECYCLE-01, LIFECYCLE-02, LIFECYCLE-03
Success criteria:
1. Both users trigger `/match/arrive` resulting in `IN_PROGRESS` state.
2. Match accurately transitioned to `COMPLETED` when done.

**Phase 5: Post-Match Payments**
Goal: Trigger split payments at match completion
Requirements: PAY-01, PAY-02
Success criteria:
1. Match completion fires payment creation.
2. Payment splits equally per player and appears in Payment module.
