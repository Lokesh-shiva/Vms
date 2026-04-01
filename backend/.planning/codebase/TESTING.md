# Testing

## Framework and Setup
- The testing framework is **pytest**.
- Tests are executed globally from `tests/` or modular tests from `modules/*/tests/`.

## Mocking and Strategies
- **Unit Testing**: Intensive mocking of dependencies using `unittest.mock.patch`. Service layers are tested by mocking the underlying Repository calls (e.g., testing `process_refund` without hitting DB).
- **Route Testing**: Uses FastAPI's `TestClient`. Role-based access logic is tested using dependency overrides (e.g., `app.dependency_overrides[get_current_user]`) to inject simulated admin/user states.

## Coverage Highlights
- Contains tests for deep business logic like **Fee Math Deductions**, **Reference Code Retries**, and **State Expirations**.
- Includes extensive RBAC testing ensuring endpoints block unauthorized roles (e.g., User deleting an admin).
- Total coverage involves >231 passing tests as of the latest logs.
