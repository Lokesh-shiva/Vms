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

print("Running migration 3: add time-rate + surge columns to region_cart_type_configs ...")
cur.execute("""
    ALTER TABLE region_cart_type_configs
        ADD COLUMN IF NOT EXISTS matching_fee NUMERIC(10,2) NOT NULL DEFAULT 0,
        ADD COLUMN IF NOT EXISTS rate_per_block NUMERIC(10,2) NOT NULL DEFAULT 0,
        ADD COLUMN IF NOT EXISTS block_duration_minutes INT NOT NULL DEFAULT 45,
        ADD COLUMN IF NOT EXISTS max_duration_minutes INT NOT NULL DEFAULT 180,
        ADD COLUMN IF NOT EXISTS surge_enabled BOOLEAN NOT NULL DEFAULT FALSE,
        ADD COLUMN IF NOT EXISTS surge_multiplier NUMERIC(4,2) NOT NULL DEFAULT 1.0;
""")

print("Running migration 4: add session columns to bookings ...")
cur.execute("""
    ALTER TABLE bookings
        ADD COLUMN IF NOT EXISTS session_started_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS session_ended_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS session_minutes INT,
        ADD COLUMN IF NOT EXISTS session_blocks INT,
        ADD COLUMN IF NOT EXISTS time_bill_amount NUMERIC(10,2),
        ADD COLUMN IF NOT EXISTS surge_multiplier_snapshot NUMERIC(4,2);
""")

print("Running migration 5: add payment_type to payments ...")
cur.execute("""
    ALTER TABLE payments
        ADD COLUMN IF NOT EXISTS payment_type VARCHAR(50) NOT NULL DEFAULT 'MATCHING_FEE';
""")

print("Running migration 6: fix payments unique constraint for two-payment model ...")
cur.execute("""
    ALTER TABLE payments DROP CONSTRAINT IF EXISTS uq_payment_booking_user;
""")
cur.execute("""
    DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'uq_payment_booking_user_type'
        ) THEN
            ALTER TABLE payments
                ADD CONSTRAINT uq_payment_booking_user_type
                UNIQUE (booking_id, user_id, payment_type);
        END IF;
    END $$;
""")

print("Running migration 7: add owner_user_id to carts ...")
cur.execute("""
    ALTER TABLE carts
        ADD COLUMN IF NOT EXISTS owner_user_id INT REFERENCES users(id) ON DELETE SET NULL;
""")

print("Running migration 8: create tournaments table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS tournaments (
        id          SERIAL PRIMARY KEY,
        name        VARCHAR NOT NULL,
        sport_id    INT REFERENCES sports(id) ON DELETE SET NULL,
        region_id   INT REFERENCES locations(id) ON DELETE SET NULL,
        organizer   VARCHAR NOT NULL,
        start_date  DATE NOT NULL,
        end_date    DATE NOT NULL,
        max_teams   INT NOT NULL DEFAULT 8,
        status      VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
        created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 9: create disputes table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS disputes (
        id               SERIAL PRIMARY KEY,
        booking_id       INT REFERENCES bookings(id) ON DELETE SET NULL,
        user_id          INT REFERENCES users(id) ON DELETE SET NULL,
        raised_by        INT REFERENCES users(id) ON DELETE SET NULL,
        title            VARCHAR NOT NULL,
        description      TEXT NOT NULL,
        status           VARCHAR(50) NOT NULL DEFAULT 'OPEN',
        resolution_note  TEXT,
        created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 10: create audit_logs table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS audit_logs (
        id                   SERIAL PRIMARY KEY,
        action               VARCHAR NOT NULL,
        actor_user_id        INT REFERENCES users(id) ON DELETE SET NULL,
        target_resource_type VARCHAR,
        target_resource_id   INT,
        details              TEXT,
        created_at           TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 11: add can_create_society to users ...")
cur.execute("""
    ALTER TABLE users
        ADD COLUMN IF NOT EXISTS can_create_society BOOLEAN NOT NULL DEFAULT FALSE;
""")

print("Running migration 12: seed sports table ...")
cur.execute("""
    INSERT INTO sports (name, is_active) VALUES
        ('Cricket',    TRUE),
        ('Football',   TRUE),
        ('Badminton',  TRUE),
        ('Volleyball', TRUE),
        ('Basketball', TRUE),
        ('Tennis',     TRUE)
    ON CONFLICT (name) DO NOTHING;
""")

print("Running migration 13: seed Vizag locations ...")
cur.execute("""
    INSERT INTO locations (name) VALUES
        ('Vizag Central'),
        ('Gajuwaka'),
        ('Vizag North Zone'),
        ('Rushikonda'),
        ('Madhurawada'),
        ('Dwaraka Nagar'),
        ('MVP Colony'),
        ('Seethammadhara')
    ON CONFLICT (name) DO NOTHING;
""")

conn.commit()
cur.close()
conn.close()
print("All migrations completed successfully.")
