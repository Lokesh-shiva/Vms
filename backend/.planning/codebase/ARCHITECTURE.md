# Architecture

## Core Architectural Pattern
The system is built on a strict **Layered Architecture** inside a **Modular Monolith**.

## Layers
1. **Controller Layer (`controller/`)**: Exposes REST endpoints, performs HTTP request parsing, input validation (Pydantic), and passes data to the Service Layer. Never accesses the database.
2. **Service Layer (`service/`)**: Houses the core business logic. Responsible for state transitions (e.g., PENDING_PAYMENT to CONFIRMED), orchestrating multiple repository calls, and enforcing cross-aggregate rules.
3. **Repository Layer (`repository/`)**: Data abstraction layer mapping Python objects to PostgreSQL using SQLAlchemy ORM. Contains SQL queries and soft-delete implementations.
4. **Data Layer**: PostgreSQL Database.

## State Machines
- **Booking Machine**: Strict transitions: `PENDING_PAYMENT` -> `CONFIRMED` -> `IN_PROGRESS` -> `COMPLETED`. 
- **Payment Machine**: Transitions: `PENDING` -> `UNDER_REVIEW` -> `SUCCESS` -> `FAILED` -> `REFUNDED`.

## Core Engine (`core/`)
- Encapsulates shared infrastructure:
  - Database connection (`engine`, `Base`)
  - Middleware (`ErrorHandlerMiddleware`)
  - Common configuration logic and Security primitives.
