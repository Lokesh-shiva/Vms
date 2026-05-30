"""
Database cleanup + seed script for Plixo Control Centre.

Wipes all legacy test data and populates clean demo records.
Safe to re-run — deletes in FK order, then inserts fresh data.

Run from backend/ directory:
    python db_seed.py
"""

import os
from pathlib import Path
from datetime import date, datetime
import bcrypt
from dotenv import load_dotenv
import psycopg2

load_dotenv(Path(__file__).parent / ".env")
conn = psycopg2.connect(os.environ["DATABASE_URL"])
conn.autocommit = False
cur = conn.cursor()


def run(sql, params=None):
    cur.execute(sql, params or ())


def h(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


print("=== Plixo DB Seed ===\n")

# ─────────────────────────────────────────────────────────────────────
# STEP 1 — Wipe all dependent tables (FK order, deepest first)
# ─────────────────────────────────────────────────────────────────────
print("[1/7] Clearing dependent tables...")

run("DELETE FROM booking_items;")
run("DELETE FROM match_penalties;")
run("DELETE FROM match_events;")
run("DELETE FROM match_players;")
run("DELETE FROM queue_entries;")
run("DELETE FROM payments;")
run("DELETE FROM matches;")
run("DELETE FROM bookings;")
run("DELETE FROM captain_profiles;")
run("DELETE FROM captains;")
run("DELETE FROM region_cart_type_configs;")
run("DELETE FROM carts;")
run("DELETE FROM timeslots;")
run("DELETE FROM items;")

# ─────────────────────────────────────────────────────────────────────
# STEP 2 — Clean core lookup tables
# ─────────────────────────────────────────────────────────────────────
print("[2/7] Cleaning core tables...")

# Keep only 3 real regions
run("DELETE FROM locations WHERE id NOT IN (1, 3, 5);")
run("UPDATE locations SET name = 'Vizag Central'    WHERE id = 1;")
run("UPDATE locations SET name = 'Gajuwaka'         WHERE id = 3;")
run("UPDATE locations SET name = 'Vizag North Zone' WHERE id = 5;")

# Replace old vendor cart types with actual sports
run("DELETE FROM cart_types;")
run("""
INSERT INTO cart_types (id, name, description, is_active, created_at, updated_at)
VALUES
  (1, 'Cricket',   'Cricket grounds and matches',      TRUE, NOW(), NOW()),
  (2, 'Football',  'Football pitches and matches',     TRUE, NOW(), NOW()),
  (3, 'Badminton', 'Badminton courts',                 TRUE, NOW(), NOW()),
  (4, 'Volleyball','Volleyball courts',                TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
  SET name = EXCLUDED.name,
      description = EXCLUDED.description,
      updated_at = NOW();
""")
# Bump sequence past 4
run("SELECT setval('cart_types_id_seq', 10, false);")

# Keep only your main super_admin; delete all other users
run("DELETE FROM users WHERE id != 12;")

# ─────────────────────────────────────────────────────────────────────
# STEP 3 — Seed: Timeslots (future dates)
# ─────────────────────────────────────────────────────────────────────
print("[3/7] Seeding timeslots...")

run("DELETE FROM timeslots;")
run("""
INSERT INTO timeslots (location_id, date, start_time, end_time, capacity, is_active, created_at, updated_at)
VALUES
  (1, '2026-07-01', '06:00', '07:30', 8,  TRUE, NOW(), NOW()),
  (1, '2026-07-01', '08:00', '09:30', 8,  TRUE, NOW(), NOW()),
  (1, '2026-07-01', '17:00', '18:30', 10, TRUE, NOW(), NOW()),
  (1, '2026-07-02', '06:00', '07:30', 8,  TRUE, NOW(), NOW()),
  (3, '2026-07-01', '07:00', '08:30', 6,  TRUE, NOW(), NOW()),
  (3, '2026-07-02', '17:00', '18:30', 6,  TRUE, NOW(), NOW()),
  (5, '2026-07-01', '06:30', '08:00', 5,  TRUE, NOW(), NOW());
""")

# ─────────────────────────────────────────────────────────────────────
# STEP 4 — Seed: Grounds / Carts
# ─────────────────────────────────────────────────────────────────────
print("[4/7] Seeding grounds...")

run("""
INSERT INTO carts (label, region_id, cart_type_id, status, is_active, created_at, updated_at)
VALUES
  ('Vizag Cricket Ground A',   1, 1, 'AVAILABLE', TRUE, NOW(), NOW()),
  ('Vizag Cricket Ground B',   1, 1, 'AVAILABLE', TRUE, NOW(), NOW()),
  ('Vizag Football Pitch',     1, 2, 'AVAILABLE', TRUE, NOW(), NOW()),
  ('Gajuwaka Cricket Ground',  3, 1, 'AVAILABLE', TRUE, NOW(), NOW()),
  ('Gajuwaka Football Pitch',  3, 2, 'AVAILABLE', TRUE, NOW(), NOW()),
  ('North Zone Badminton Hall',5, 3, 'AVAILABLE', TRUE, NOW(), NOW());
""")

# ─────────────────────────────────────────────────────────────────────
# STEP 5 — Seed: Pricing configs
# ─────────────────────────────────────────────────────────────────────
print("[5/7] Seeding pricing configs...")

run("""
INSERT INTO region_cart_type_configs
  (region_id, cart_type_id, booking_fee, cancellation_fee_pct, platform_fee_pct,
   matching_fee, rate_per_block, block_duration_minutes, max_duration_minutes,
   surge_enabled, surge_multiplier, is_active, created_at, updated_at)
VALUES
  (1, 1, 150.00, 10.00, 5.00, 150.00, 60.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (1, 2, 120.00, 10.00, 5.00, 120.00, 50.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (1, 3,  80.00, 10.00, 5.00,  80.00, 40.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (3, 1, 100.00, 10.00, 5.00, 100.00, 55.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (3, 2,  90.00, 10.00, 5.00,  90.00, 45.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW()),
  (5, 3,  70.00, 10.00, 5.00,  70.00, 35.00, 45, 180, FALSE, 1.0, TRUE, NOW(), NOW());
""")

# ─────────────────────────────────────────────────────────────────────
# STEP 6 — Seed: Items (equipment add-ons)
# ─────────────────────────────────────────────────────────────────────
print("[6/7] Seeding items...")

run("""
INSERT INTO items (cart_type_id, name, description, price, is_available, created_at, updated_at)
VALUES
  (1, 'Cricket Bat Rental',     'Standard MRF bat',            50.00, TRUE, NOW(), NOW()),
  (1, 'Cricket Kit (Full)',     'Bat + pads + gloves + helmet', 150.00, TRUE, NOW(), NOW()),
  (2, 'Football',               'Nivia match ball',             40.00, TRUE, NOW(), NOW()),
  (2, 'Goalkeeper Gloves',      'Pair of goalkeeper gloves',    60.00, TRUE, NOW(), NOW()),
  (3, 'Badminton Racket Pair',  '2 rackets + shuttlecocks',     70.00, TRUE, NOW(), NOW()),
  (4, 'Volleyball',             'Professional volleyball',      40.00, TRUE, NOW(), NOW());
""")

# ─────────────────────────────────────────────────────────────────────
# STEP 7 — Seed: Demo bookings + payments
# ─────────────────────────────────────────────────────────────────────
print("[7/7] Seeding demo bookings + payments...")

# Fetch the first timeslot ids we just created
cur.execute("SELECT id FROM timeslots ORDER BY id LIMIT 4;")
ts_ids = [row[0] for row in cur.fetchall()]
ts1, ts2, ts3, ts4 = ts_ids[0], ts_ids[1], ts_ids[2], ts_ids[3]

# Fetch first carts
cur.execute("SELECT id FROM carts ORDER BY id LIMIT 3;")
cart_ids = [row[0] for row in cur.fetchall()]
cart1, cart2, cart3 = cart_ids[0], cart_ids[1], cart_ids[2]

uid = 12  # S. Lokeshwara Rao

run(f"""
INSERT INTO bookings
  (user_id, region_id, cart_type_id, timeslot_id, assigned_cart_id,
   address, booking_fee, estimated_total, status, payment_status,
   refund_status, refund_amount, date,
   cancellation_fee_pct_snapshot, platform_fee_pct_snapshot,
   created_at, updated_at)
VALUES
  ({uid}, 1, 1, {ts1}, {cart1},
   'MVP Colony, Vizag', 150.00, 200.00, 'CONFIRMED', 'SUCCESS',
   'NONE', 0.00, '2026-07-01', 10.00, 5.00, NOW(), NOW()),

  ({uid}, 1, 2, {ts2}, {cart3},
   'Seethammadhara, Vizag', 120.00, 160.00, 'IN_PROGRESS', 'SUCCESS',
   'NONE', 0.00, '2026-07-01', 10.00, 5.00, NOW(), NOW()),

  ({uid}, 3, 1, {ts3}, NULL,
   'Gajuwaka Main Rd', 100.00, 100.00, 'PENDING_PAYMENT', 'PENDING',
   'NONE', 0.00, '2026-07-01', 10.00, 5.00, NOW(), NOW()),

  ({uid}, 1, 1, {ts4}, {cart2},
   'Rushikonda Beach Rd', 150.00, 150.00, 'COMPLETED', 'SUCCESS',
   'NONE', 0.00, '2026-07-02', 10.00, 5.00, NOW(), NOW());
""")

cur.execute("SELECT id FROM bookings ORDER BY id;")
b_ids = [row[0] for row in cur.fetchall()]
b1, b2, b3, b4 = b_ids[0], b_ids[1], b_ids[2], b_ids[3]

run(f"""
INSERT INTO payments
  (booking_id, user_id, provider, amount, reference_code,
   transaction_id, status, created_at, updated_at)
VALUES
  ({b1}, {uid}, 'MANUAL_UPI', 200.00, 'PLX-{b1}-1001',
   'UPI-TXN-DEMO-001', 'SUCCESS', NOW(), NOW()),

  ({b2}, {uid}, 'MANUAL_UPI', 160.00, 'PLX-{b2}-1002',
   'UPI-TXN-DEMO-002', 'SUCCESS', NOW(), NOW()),

  ({b3}, {uid}, 'MANUAL_UPI', 100.00, 'PLX-{b3}-1003',
   NULL, 'UNDER_REVIEW', NOW(), NOW()),

  ({b4}, {uid}, 'MANUAL_UPI', 150.00, 'PLX-{b4}-1004',
   'UPI-TXN-DEMO-004', 'SUCCESS', NOW(), NOW());
""")

# ─────────────────────────────────────────────────────────────────────
# STEP 8 — Seed: Demo matches
# ─────────────────────────────────────────────────────────────────────
run(f"""
INSERT INTO matches
  (created_by, region_id, cart_type_id, cart_id, sport_id,
   skill_level, max_players, joined_players, status, created_at, updated_at)
VALUES
  ({uid}, 1, 1, {cart1}, 2, 'BEGINNER',      4, 2, 'OPEN',      NOW(), NOW()),
  ({uid}, 1, 2, {cart3}, 1, 'INTERMEDIATE',  6, 4, 'OPEN',      NOW(), NOW()),
  ({uid}, 3, 1, NULL,    2, 'BEGINNER',      4, 4, 'MATCHED',   NOW(), NOW()),
  ({uid}, 1, 1, {cart2}, 2, 'ADVANCED',      4, 4, 'COMPLETED', NOW(), NOW());
""")

# ─────────────────────────────────────────────────────────────────────
# Commit
# ─────────────────────────────────────────────────────────────────────
conn.commit()
cur.close()
conn.close()

print("\n[OK] Database seeded successfully.")
print("\nSummary:")
print("  Users    : 1 (S. Lokeshwara Rao, super_admin)")
print("  Regions  : 3 (Vizag Central, Gajuwaka, Vizag North Zone)")
print("  Sports   : 4 (Cricket, Football, Badminton, Volleyball)")
print("  Grounds  : 6")
print("  Timeslots: 7")
print("  Pricing  : 6 configs")
print("  Items    : 6")
print("  Bookings : 4 (CONFIRMED, IN_PROGRESS, PENDING_PAYMENT, COMPLETED)")
print("  Payments : 4 (3x SUCCESS, 1x UNDER_REVIEW)")
print("  Matches  : 4 (2x OPEN, 1x MATCHED, 1x COMPLETED)")
