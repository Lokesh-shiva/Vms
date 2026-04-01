# Directory Structure

## Overview
The codebase organizes code strictly by feature, avoiding "type-based" global folders (e.g., a single `/models` for everything) in favor of decoupled modules.

## Root Level
```text
backend/
├── main.py                   # Application entry point/router registration
├── core/                     # Shared horizontal infrastructure
├── modules/                  # Vertical business slices (domains)
├── requirements.txt          # Python dependencies
├── tests/                    # System-level tests
└── DEV_LOG.md                # Extensive developer changelog
```

## Module Structure (Example: `modules/fee_config/`)
```text
modules/fee_config/
├── model/           # SQLAlchemy DB models (fee_config_model.py)
├── service/         # Business logic implementation
├── controller/      # API endpoints (FastAPI routers)
├── repository/      # CRUD and database transaction handling
├── schemas/         # Pydantic validation schemas for requests/responses
└── tests/           # Module-isolated unit tests
```

## Essential Files
- `main.py`: Registers all module routers and applies global middleware.
- `PROJECT_GUIDELINES.md`: Defines overarching architectural constraints and naming rules.
