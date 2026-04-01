# Phase 01 Summary: DB Models & Pricing

**Completed:** 2026-03-27

## Overview
Phase 01 focused on setting up the core database schema updates and dynamic pricing logic required for the V1 sports matchmaking system. This involved introducing the `QueueEntry` and `Sport` models, updating the existing `Match` and `MatchPlayer` models to support the new lifecycle, and implementing a dynamic pricing engine based on current queue demand and time of day.

## Key Accomplishments
- **Database Schema Updates:**
  - Created `QueueEntry` model to track users waiting for a match.
  - Created `Sport` model to manage available sports.
  - Updated `Match` model to use `sport_id` and added new lifecycle statuses (`WAITING`, `MATCHED`, `ARRIVED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).
  - Updated `MatchPlayer` model to track arrival status (`has_arrived`).
- **Dynamic Pricing Engine:**
  - Implemented `PricingService` to calculate match prices dynamically.
  - Added time-based multipliers (peak hours from 17:00 to 21:00 UTC).
  - Added demand-based multipliers based on the active queue count for specific regions and sports.
- **Verification:**
  - Verified that all models correctly migrate and load on server startup.
  - Verified pricing engine calculations (base price, peak hour multiplier, and demand surge).

## Traceability
- **Requirements Satisfied:** PRICE-01, PRICE-02
- **Remaining Scope for Milestone:** Covered in subsequent phases.

## Notes for Future Phases
The dynamic pricing engine currently uses a mocked calculation for the demand factor that queries the `QueueEntry` table. Future scaling might require caching this active user count to prevent database bottlenecks.
