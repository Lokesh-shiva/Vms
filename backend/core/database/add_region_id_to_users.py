"""One-shot migration: adds region_id column to users table."""

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from core.database.db_connection import engine
from sqlalchemy import text


def run():
    with engine.connect() as conn:
        result = conn.execute(
            text("""
            SELECT column_name
            FROM information_schema.columns
            WHERE table_name = 'users' AND column_name = 'region_id'
        """)
        )
        if result.fetchone():
            print("Column region_id already exists on users table — skipping.")
            return

        conn.execute(
            text("""
            ALTER TABLE users
            ADD COLUMN region_id INTEGER REFERENCES locations(id)
        """)
        )
        conn.commit()
        print("SUCCESS: region_id column added to users table.")


if __name__ == "__main__":
    run()
