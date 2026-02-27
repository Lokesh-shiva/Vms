"""
Database Connection Manager — SQLAlchemy + Neon PostgreSQL

Provides:
    - engine: SQLAlchemy engine connected to Neon via DATABASE_URL
    - SessionLocal: session factory for creating DB sessions
    - Base: declarative base for all ORM models
    - get_db(): FastAPI dependency that yields a session per request
"""

import os
from pathlib import Path

from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# ── Load .env from backend root ──────────────────────────────────────

_env_path = Path(__file__).resolve().parents[2] / ".env"
load_dotenv(_env_path)

DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    raise RuntimeError("DATABASE_URL is not set. Check your .env file.")

# ── SQLAlchemy Core Objects ──────────────────────────────────────────

engine = create_engine(DATABASE_URL, pool_pre_ping=True)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)

Base = declarative_base()


# ── FastAPI Dependency ───────────────────────────────────────────────

def get_db():
    """
    Yield a database session for the duration of a request.
    Automatically closes the session when the request is done.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
