# CLAUDE.md - VMS Project Context

## Current Phase: Phase 05: Post-Match Payments
**Goal:** Implementation of automated split payments on match completion.

## Status: PLANNED
- Planning directory: `backend/.planning/phases/05-post-match-payments/`
- Context: `05-CONTEXT.md`
- Plan: `05-PLAN.md`

## Build & Test Commands
- Backend: `uvicorn main:app --port 8000 --reload` (Run from `backend/`)
- Test suite: `pytest` (To be implemented/run from `backend/`)
- Verification: `/match/finish` endpoint triggers payment creation.

## Decisions (Locked)
- **Separate Payment Records**: One per player.
- **Trigger**: Automatic in `MatchService.finish_match`.
- **Splitting**: Equal split of `Booking.estimated_total`.
- **Naming**: `VMS-{booking_id}-P{user_id}-{rand}`.

## Next Step
Execute Phase 05:
```bash
/gsd-execute-phase 05
```
(Using GSD logic)
