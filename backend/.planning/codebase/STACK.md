# Tech Stack

## Core Technologies
- **Language**: Python 3.12+ (or 3.x)
- **Framework**: FastAPI (Modular, async-first web framework)
- **Server**: Uvicorn[standard] (ASGI server)
- **Database**: PostgreSQL (managed on Neon)
- **ORM**: SQLAlchemy (with Pydantic validation via FastAPI)
- **Database Driver**: psycopg2-binary
- **Environment Management**: python-dotenv

## Authentication & Security
- **Authentication**: python-jose[cryptography] (JWT)
- **Hashing**: bcrypt (Password hashing)
- **Authorization**: Role-Based Access Control (RBAC) via FastAPI dependencies (e.g., `require_admin`, `get_current_user`).

## Testing Quality
- **Test Framework**: pytest
- Extensive test suites run globally or at the module level.

## Deployment & Configuration
- **Config Files**: `.env`, `.env.example`
- **Dependencies List**: `requirements.txt`
- **Main Entry**: `main.py`
