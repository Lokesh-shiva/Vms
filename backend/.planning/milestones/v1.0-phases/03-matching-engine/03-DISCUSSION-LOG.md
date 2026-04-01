# Discussion Log - Phase 03: Matching Engine
Date: 2026-03-30
Workflow: gsd-discuss-phase

- **Q: Engine Invocation**
  - Options Presented: A) Background worker task via asyncio, B) REST endpoint triggered continuously by external cron [Recommended], C) Synchronous trigger
  - User Selected: B
- **Q: Skill Strictness**
  - Options Presented: A) Exact match only [Recommended], B) Expandable range over time
  - User Selected: A
- **Q: Booking Fallback**
  - Options Presented: A) Keep them in WAITING queue until ground opens [Recommended], B) Cancel queue entries indicating no availability
  - User Selected: A
- **Q: Queue Prioritization**
  - Options Presented: A) Strict FIFO matching by `created_at` [Recommended], B) Random prioritization
  - User Selected: A
