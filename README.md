# VMS Backend

FastAPI-based backend for a modular Vehicle Management System (VMS), organized with a layered architecture:

- Controller layer for API routes
- Service layer for business logic
- Repository layer for data access
- SQLAlchemy ORM with PostgreSQL (Neon-compatible)

## Tech Stack

- Python 3.11+
- FastAPI
- SQLAlchemy
- PostgreSQL (`psycopg2-binary`)
- Uvicorn
- Pytest

## Project Layout

The active codebase is under `backend/`:

- `backend/main.py` - FastAPI application entrypoint
- `backend/core/` - shared infrastructure (DB, middleware, base classes)
- `backend/modules/` - feature modules (`user`, `location`, `timeslot`, `cart_type`, `cart`, `item`, `booking`, `booking_item`)
- `backend/requirements.txt` - Python dependencies
- `backend/DEV_LOG.md` - chronological implementation and validation log

## Quick Start

1) Create and activate a virtual environment

```bash
python -m venv venv
venv\Scripts\activate
```

2) Install dependencies

```bash
pip install -r backend/requirements.txt
```

3) Configure environment variables

- Copy `backend/.env.example` to `backend/.env`
- Set `DATABASE_URL`

4) Run the API server

```bash
uvicorn backend.main:app --reload --port 8000
```

API docs:

- Swagger UI: `http://127.0.0.1:8000/docs`
- ReDoc: `http://127.0.0.1:8000/redoc`

## Running Tests

From `backend/`:

```bash
pytest
```

## Notes

- Test suites are intentionally kept in the repository (`backend/modules/*/tests`).
- Local artifacts like virtual environments, cache folders, and generated test output files should not be committed.
