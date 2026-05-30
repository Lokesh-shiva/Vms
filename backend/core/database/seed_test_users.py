"""
Seed test users required by TestSprite UAT tests.

Test users (login via phone):
  1. +10000000001 / testpassword    — region_id=1  (TC001, TC003, TC004, TC005, TC007)
  2. +10000000002 / TestPass123!    — region_id=NULL (TC002)
  3. +10000000003 / TestPassword123! — region_id=1 (TC008)
  4. +10000000004 / StrongPassw0rd! — region_id=1 (TC006)
"""

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

import bcrypt
from core.database.db_connection import SessionLocal
from modules.user.model.user_model import User


def _hash(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


TEST_USERS = [
    {
        "name": "Test User",
        "phone": "+10000000001",
        "password": "testpassword",
        "region_id": 1,
    },
    {
        "name": "No Region User",
        "phone": "+10000000002",
        "password": "TestPass123!",
        "region_id": None,
    },
    {
        "name": "Test User No Entry",
        "phone": "+10000000003",
        "password": "TestPassword123!",
        "region_id": 1,
    },
    {
        "name": "Test User No Queue",
        "phone": "+10000000004",
        "password": "StrongPassw0rd!",
        "region_id": 1,
    },
]


def run():
    session = SessionLocal()
    try:
        for u in TEST_USERS:
            existing = session.query(User).filter(User.phone == u["phone"]).first()
            if existing:
                existing.password_hash = _hash(u["password"])
                existing.region_id = u["region_id"]
                session.commit()
                print(f"UPDATED: {u['phone']} (region_id={u['region_id']})")
            else:
                user = User(
                    name=u["name"],
                    phone=u["phone"],
                    password_hash=_hash(u["password"]),
                    role="user",
                    is_active=True,
                    region_id=u["region_id"],
                )
                session.add(user)
                session.commit()
                print(f"CREATED: {u['phone']} (region_id={u['region_id']})")
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()
    print("Seed complete.")


if __name__ == "__main__":
    run()
