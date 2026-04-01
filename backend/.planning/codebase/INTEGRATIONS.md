# Integrations

## Database Integration
- **Neon Serverless Postgres**: The backend uses PostgreSQL via the `psycopg2-binary` driver. Database access is encapsulated inside repository classes using SQLAlchemy. Models define the schema, such as identity, bookings, and fee configurations.

## Payments
- **Manual UPI MVP**: Currently, there is no direct payment gateway (e.g., Razorpay, Stripe) integration. The API relies on deep links (`upi://pay?...`) generated inside `payment_service.py` with custom transaction notes (`tn=VMS-xxx`). The admin manually verifies UTRs to approve the payment.

## Potential Future Integrations
- **Messaging Queues**: Architecture notes indicate future readiness for event-driven processing (e.g., Redis, RabbitMQ) by keeping cross-module dependencies low.
- **Automated Payments**: Real webhook support could replace the current Admin Manual Approval once a payment gateway is integrated.
- **SMS/Email Gateway**: Currently missing, but typically required for booking confirmations and OTP authentication if expanded.
