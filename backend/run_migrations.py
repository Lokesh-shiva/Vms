"""
One-time migration script — run from the backend/ directory:
    python run_migrations.py

Applies:
  1. ALTER TABLE timeslots ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE
  2. CREATE TABLE IF NOT EXISTS captains (...)
"""

import os
from pathlib import Path

import psycopg2
from dotenv import load_dotenv

# Load .env from the backend directory
load_dotenv(Path(__file__).parent / ".env")

DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    raise RuntimeError("DATABASE_URL not set — check backend/.env")

conn = psycopg2.connect(DATABASE_URL)
cur = conn.cursor()

print("Running migration 1: add timeslots.is_active ...")
cur.execute(
    "ALTER TABLE timeslots ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;"
)

print("Running migration 2: create captains table ...")
cur.execute(
    """
    CREATE TABLE IF NOT EXISTS captains (
        id          SERIAL PRIMARY KEY,
        user_id     INT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        region_id   INT REFERENCES locations(id) ON DELETE SET NULL,
        status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
        rating      FLOAT NOT NULL DEFAULT 0.0,
        total_trips INT NOT NULL DEFAULT 0,
        bio         TEXT,
        created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
    );
    """
)

conn.commit()
cur.close()
conn.close()
print("All migrations completed successfully.")
