# Phase 05: Post-Match Payments - Context

**Gathered:** 2026-04-01
**Status:** Ready for planning
**Source:** User feedback on implementation plan

<domain>
## Phase Boundary

This phase implements the automatic creation of split payment records when a match is completed. This ensures that the costs for ground booking and service fees are shared equally between the matched players in a 2-player game.

</domain>

<decisions>
## Implementation Decisions

### Payment Records
- **Separate Payment Records**: Create two distinct `Payment` records for each match (one per player). This allows independent tracking of who has paid.

### Trigger Mechanism
- **Automatic on Finish**: The system creates payments immediately when `MatchService.finish_match` is called.

### Amount Calculation
- **Equal Split of Booking Total**: Take the `Booking.estimated_total` and divide by 2.

### Reference Code Strategy
- **Player-Suffix**: Use `VMS-{booking_id}-P1-{rand}` and `VMS-{booking_id}-P2-{rand}` for uniqueness and traceability.

### the agent's Discretion
- **Failure Handling**: If payment creation fails, log the error but do not roll back match completion (match is already over).
- **Payment Model Updates**: Add `user_id` and `match_id` to the `Payment` model to support individual tracking.
- **Unique Constraint**: Update the unique constraint on `booking_id` in the `payments` table to allow for split payment records.

</decisions>

<canonical_refs>
## Canonical References

### Match Module
- `backend/modules/match/service/match_service.py` — `finish_match` entry point.
- `backend/modules/match/model/match_model.py` — Match and MatchPlayer models.

### Payment Module
- `backend/modules/payment/service/payment_service.py` — Payment creation logic.
- `backend/modules/payment/model/payment_model.py` — Payment record schema.

</canonical_refs>

<specifics>
## Specific Ideas
- The split amount must be formatted to two decimal places.
- Payment records should default to `PENDING` status.

</specifics>

<deferred>
## Deferred Ideas
- Automated UPI verification (out of scope for manual approval workflow).
- Partial payments for no-shows (default is split total).

</deferred>

---

*Phase: 05-post-match-payments*
*Context gathered: 2026-04-01 via Discuss-Phase + User Feedback*
