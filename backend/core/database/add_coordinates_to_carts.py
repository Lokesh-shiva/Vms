"""One-shot migration: adds latitude and longitude columns to carts table."""

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from core.database.db_connection import engine
from sqlalchemy import text


def run():
    with engine.connect() as conn:
        # Check and add latitude
        result = conn.execute(
            text("""
            SELECT column_name
            FROM information_schema.columns
            WHERE table_name = 'carts' AND column_name = 'latitude'
        """)
        )
        if result.fetchone():
            print("Column latitude already exists on carts table -- skipping.")
        else:
            conn.execute(
                text("""
                ALTER TABLE carts
                ADD COLUMN latitude FLOAT
            """)
            )
            print("SUCCESS: latitude column added to carts table.")

        # Check and add longitude
        result = conn.execute(
            text("""
            SELECT column_name
            FROM information_schema.columns
            WHERE table_name = 'carts' AND column_name = 'longitude'
        """)
        )
        if result.fetchone():
            print("Column longitude already exists on carts table -- skipping.")
        else:
            conn.execute(
                text("""
                ALTER TABLE carts
                ADD COLUMN longitude FLOAT
            """)
            )
            print("SUCCESS: longitude column added to carts table.")

        conn.commit()


if __name__ == "__main__":
    run()
