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
    INSERT INTO locations (name, is_serviceable, created_at, updated_at) VALUES
        ('Vizag Central',    TRUE, NOW(), NOW()),
        ('Gajuwaka',         TRUE, NOW(), NOW()),
        ('Vizag North Zone', TRUE, NOW(), NOW()),
        ('Rushikonda',       TRUE, NOW(), NOW()),
        ('Madhurawada',      TRUE, NOW(), NOW()),
        ('Dwaraka Nagar',    TRUE, NOW(), NOW()),
        ('MVP Colony',       TRUE, NOW(), NOW()),
        ('Seethammadhara',   TRUE, NOW(), NOW())
    ON CONFLICT (name) DO NOTHING;
""")

print("Running migration 14: rebind queue_entries + matches sport_id FK to cart_types ...")
cur.execute("""
    ALTER TABLE queue_entries
        DROP CONSTRAINT IF EXISTS queue_entries_sport_id_fkey;
    ALTER TABLE queue_entries
        ADD CONSTRAINT queue_entries_sport_id_fkey
        FOREIGN KEY (sport_id) REFERENCES cart_types(id) ON DELETE RESTRICT;
""")
cur.execute("""
    ALTER TABLE matches
        DROP CONSTRAINT IF EXISTS matches_sport_id_fkey;
    ALTER TABLE matches
        ADD CONSTRAINT matches_sport_id_fkey
        FOREIGN KEY (sport_id) REFERENCES cart_types(id) ON DELETE SET NULL;
""")

print("Running migration 15: seed region_cart_type_configs for all Vizag locations x sports ...")
cur.execute("""
    INSERT INTO region_cart_type_configs
        (region_id, cart_type_id, booking_fee, cancellation_fee_pct, platform_fee_pct,
         matching_fee, rate_per_block, block_duration_minutes, max_duration_minutes,
         surge_enabled, surge_multiplier, is_active, created_at, updated_at)
    SELECT
        l.id, ct.id,
        0.00, 0.00, 10.00,
        200.00, 0.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()
    FROM locations l
    CROSS JOIN cart_types ct
    WHERE l.is_serviceable = TRUE AND ct.is_active = TRUE
    ON CONFLICT (region_id, cart_type_id) DO NOTHING;
""")

print("Running migration 16: add KYC + payout columns to captains ...")
cur.execute("""
    ALTER TABLE captains
        ADD COLUMN IF NOT EXISTS kyc_document_url VARCHAR,
        ADD COLUMN IF NOT EXISTS kyc_document_type VARCHAR(50),
        ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(50) NOT NULL DEFAULT 'NOT_SUBMITTED',
        ADD COLUMN IF NOT EXISTS verification_method VARCHAR(50),
        ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
        ADD COLUMN IF NOT EXISTS payout_upi_id VARCHAR;
""")

print("Running migration 17: create captain_earnings table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS captain_earnings (
        id               SERIAL PRIMARY KEY,
        captain_id       INT NOT NULL REFERENCES captains(id) ON DELETE CASCADE,
        match_id         INT REFERENCES matches(id) ON DELETE SET NULL,
        amount           NUMERIC(10,2) NOT NULL,
        status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        payout_reference VARCHAR,
        created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
        paid_at          TIMESTAMP
    );
""")

print("Running migration 18: create notifications table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS notifications (
        id         SERIAL PRIMARY KEY,
        user_id    INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        title      VARCHAR NOT NULL,
        body       TEXT NOT NULL,
        type       VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
        data_json  JSON,
        read       BOOLEAN NOT NULL DEFAULT FALSE,
        created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 19: create fcm_tokens table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS fcm_tokens (
        id         SERIAL PRIMARY KEY,
        user_id    INT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        token      VARCHAR NOT NULL,
        platform   VARCHAR(20) NOT NULL DEFAULT 'android',
        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 20: add captain_id to matches ...")
cur.execute("""
    ALTER TABLE matches
        ADD COLUMN IF NOT EXISTS captain_id INTEGER REFERENCES captains(id) ON DELETE SET NULL;
""")

print("Running migration 21: add match visibility, society_id, invite_code ...")
cur.execute("""
    ALTER TABLE matches
        ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'OPEN',
        ADD COLUMN IF NOT EXISTS society_id INTEGER REFERENCES societies(id) ON DELETE SET NULL,
        ADD COLUMN IF NOT EXISTS invite_code VARCHAR(8);
""")
cur.execute("""
    CREATE UNIQUE INDEX IF NOT EXISTS uq_matches_invite_code
        ON matches (invite_code) WHERE invite_code IS NOT NULL;
""")

print("Running migration 22: create messages table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS messages (
        id         SERIAL PRIMARY KEY,
        match_id   INT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
        sender_id  INT REFERENCES users(id) ON DELETE SET NULL,
        body       TEXT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 23: add pricing/description columns to tournaments ...")
cur.execute("""
    ALTER TABLE tournaments
        ADD COLUMN IF NOT EXISTS entry_fee INT NOT NULL DEFAULT 0,
        ADD COLUMN IF NOT EXISTS prize_pool VARCHAR(100) NOT NULL DEFAULT '',
        ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500),
        ADD COLUMN IF NOT EXISTS description TEXT;
""")

print("Running migration 24: add sponsor_user_id to tournaments ...")
cur.execute("""
    ALTER TABLE tournaments
        ADD COLUMN IF NOT EXISTS sponsor_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL;
""")

print("Running migration 25: create wallet_transactions table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS wallet_transactions (
        id          SERIAL PRIMARY KEY,
        user_id     INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        type        VARCHAR(10) NOT NULL,
        amount      INT NOT NULL,
        reason      VARCHAR(50) NOT NULL,
        description TEXT NOT NULL,
        match_id    INT REFERENCES matches(id) ON DELETE SET NULL,
        created_at  TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 26: add format_type/participant_type/team_size/rules_json to tournaments ...")
cur.execute("""
    ALTER TABLE tournaments
        ADD COLUMN IF NOT EXISTS format_type VARCHAR(50) NOT NULL DEFAULT 'LEAGUE',
        ADD COLUMN IF NOT EXISTS participant_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
        ADD COLUMN IF NOT EXISTS team_size INTEGER NOT NULL DEFAULT 1,
        ADD COLUMN IF NOT EXISTS rules_json JSON NOT NULL DEFAULT '{}'::json;
""")

print("Running migration 27: create dispute_messages table ...")
cur.execute("""
    CREATE TABLE IF NOT EXISTS dispute_messages (
        id         SERIAL PRIMARY KEY,
        dispute_id INT NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
        sender_id  INT REFERENCES users(id) ON DELETE SET NULL,
        body       TEXT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );
""")

print("Running migration 28: add username/email to users ...")
cur.execute("""
    ALTER TABLE users
        ADD COLUMN IF NOT EXISTS username VARCHAR,
        ADD COLUMN IF NOT EXISTS email VARCHAR;
""")
cur.execute("""
    CREATE UNIQUE INDEX IF NOT EXISTS ix_users_username ON users (username) WHERE username IS NOT NULL;
""")
cur.execute("""
    CREATE UNIQUE INDEX IF NOT EXISTS ix_users_email ON users (email) WHERE email IS NOT NULL;
""")

print("Running migration 29: add latitude/longitude to locations ...")
cur.execute("""
    ALTER TABLE locations
        ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
        ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
""")

print("Running migration 30: add image_url to carts (grounds) ...")
cur.execute("""
    ALTER TABLE carts ADD COLUMN IF NOT EXISTS image_url VARCHAR;
""")

conn.commit()
cur.close()
conn.close()
print("All migrations completed successfully.")
