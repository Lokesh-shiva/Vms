# Technical Debt & Concerns

## Known Architectural Limitations
- **Background Processes**: The system currently lacks a dedicated queue/job runner (like Celery). Because of this, "expirations" (e.g., of `PENDING_PAYMENT` bookings) are done lazily upon querying rather than actively at the moment of timeout.
- **State Synchronization**: The Payment status is mirrored in the Booking table. While `PaymentService` is the source of truth, careful orchestration ensures they don't drift.

## Fragile Areas
- **Circular Imports**: Certain domains closely interact (e.g., `booking_service` vs `payment_service`). To prevent runtime circular import crashes when auto-confirming a booking after payment, lazy imports are used inside methods. Refactoring this into an event bus could alleviate the tight coupling.

## Scalability Risks
- **Testing Integrity**: Currently, integrity constraint errors are detected by string-matching `psycopg2` exceptions. This could break if the underlying DB driver updates or changes its error message formatting.
- **Manual Automation**: Manual UPI approval requires admin effort per transaction.
