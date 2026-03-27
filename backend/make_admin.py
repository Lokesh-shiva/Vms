from core.database.db_connection import SessionLocal
from modules.user.model.user_model import User

def make_all_users_admin():
    db = SessionLocal()
    try:
        users = db.query(User).all()
        for user in users:
            user.is_admin = True
        db.commit()
        print(f"Successfully made {len(users)} users admin.")
    except Exception as e:
        print(f"Error: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    make_all_users_admin()
