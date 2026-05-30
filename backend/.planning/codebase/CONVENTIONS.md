# Conventions

## API Conventions
- **RESTful Endpoints**: Resources are plural (`/api/v1/bookings`).
- **Standardized Response Envelope**: Every endpoint returns an object containing `{"success": true|false, "data": ..., "message": "..."}`.
- **Global Error Handling**: Unhandled exceptions are converted into standard 500 JSON responses via `ErrorHandlerMiddleware` to avoid raw tracebacks in production.

## Coding Conventions
- **Dependency Injection**: Heavy use of FastAPI's `Depends` for providing authenticated users and DB sessions.
- **SQLAlchemy Practices**: Models inherit from a shared `Base`. Custom database queries are restricted wholly to repository classes.
- **Naming**:
  - Folders and files use `snake_case`.
  - Service functions reflect domain behaviors (`approve_payment`, `confirm_booking`).
  - Repositories are typically named `{Entity}Repository`.

## Security Conventions
- **Token Claims**: JWTs carry `sub` (user id) and `role` to limit database hits.
- **Secret Management**: Passwords are never returned via APIs (excluded via model serialization `to_dict()` methods). Keys lived in `.env`.
