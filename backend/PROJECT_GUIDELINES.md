# Backend Project Guidelines

This document defines the architectural and development standards for the V1 backend system. These guidelines ensure scalability, maintainability, and consistency across all feature modules.

## 1. Architecture Overview

The system follows a strict **Layered Architecture** with a clear separation of concerns.

*   **Controller Layer**: Handles HTTP requests, validates input, and orchestrates calls to the service layer.
*   **Service Layer**: Contains all business logic. It serves as the intermediary between controllers and repositories.
*   **Repository Layer**: Manages direct database interactions. No business logic resides here.
*   **Database**: The persistence layer.

The architecture distinguishes between the **Core Layer** (infrastructure and shared utilities) and **Feature Modules** (domain-specific logic).

## 2. Core Principles

*   **Infrastructure Only in Core**: The `core` directory is reserved for infrastructure code, shared utilities, and global configurations. It must not contain business logic.
*   **Isolated Business Logic**: All business logic must reside within dedicated feature modules.
*   **No Direct Database Access**: Controllers are strictly prohibited from accessing the database directly. All data access must go through the Service layer, which in turn uses the Repository layer.
*   **Modular Features**: Every feature must be designed as a self-contained module with defined boundaries.
*   **Shared Repository Instances**: All in-memory repositories must be shared instances across modules. Each repository module must export a single module-level instance to ensure consistent state.

## 3. Module Structure Standard

Each feature module must adhere to the following directory structure:

```
feature-name/
├── model/      # Database entities and data models
├── service/    # Business logic implementation
├── controller/ # API endpoints and request handling
├── repository/ # Database access methods
├── schemas/    # Data Transfer Objects (DTOs) and validation schemas
└── tests/      # Unit and integration tests for the module
```

## 4. API Standards

*   **RESTful Naming**: API endpoints must follow standard RESTful conventions (e.g., `GET /resource`, `POST /resource`, `GET /resource/:id`).
*   **Consistent Response Format**: All API responses must strictly adhere to a standardized JSON structure (e.g., separating `data`, `meta`, and `error` fields).
*   **Centralized Error Handling**: Errors must be handled centrally using a global exception handler or middleware to ensure consistent error responses across the application.

## 5. Security Guidelines

*   **Authentication**: All protected routes must require valid authentication tokens.
*   **Authorization (RBAC)**: Implement Role-Based Access Control to restrict access based on user roles.
*   **Input Validation**: Strict input validation is mandatory at the schema level for all incoming requests. Never trust client data.

## 6. Scalability Rules

*   **Modular Addition**: New features must be added as new, independent modules.
*   **Stable Core**: The core infrastructure code should be stable and not require modification when adding standard features.
*   **Event-Driven Ready**: Design interactions between modules to be loosely coupled, allowing for future migration to an event-driven architecture (e.g., using message queues) without major refactoring.

## 7. Code Quality Standards

*   **Clear Naming**: Use descriptive and consistent naming conventions for variables, functions, and classes.
*   **Small, Focused Functions**: adherence to the Single Responsibility Principle. Functions should be small and do one thing well.
*   **Business Logic in Service**: Keep controllers thin; all business rules and complex logic belong in the Service layer.
*   **Repository Focus**: Repositories are for data access only. They should not contain business logic.
*   **Testing Mandate**: Every module requires comprehensive unit tests covering its service and repository logic.
