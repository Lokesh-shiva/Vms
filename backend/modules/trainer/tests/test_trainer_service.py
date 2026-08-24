import unittest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.trainer.model.trainer_model import Trainer  # noqa: F401
from modules.trainer.repository.trainer_repository import TrainerRepository
from modules.trainer.service.trainer_service import TrainerService


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestTrainerService(unittest.TestCase):
    def setUp(self):
        self.repo = TrainerRepository(session_factory=_factory())
        self.service = TrainerService(trainer_repository=self.repo)

    def test_create_trainer(self):
        trainer = self.service.create_trainer({"name": "Coach Ravi", "rate_per_session": 500})
        self.assertEqual(trainer["name"], "Coach Ravi")
        self.assertTrue(trainer["is_active"])

    def test_create_requires_name(self):
        with self.assertRaises(ValueError):
            self.service.create_trainer({"rate_per_session": 500})

    def test_create_requires_positive_rate(self):
        with self.assertRaises(ValueError):
            self.service.create_trainer({"name": "Coach Ravi", "rate_per_session": 0})

    def test_update_trainer(self):
        trainer = self.service.create_trainer({"name": "Coach Ravi", "rate_per_session": 500})
        updated = self.service.update_trainer(trainer["id"], {"is_active": False})
        self.assertFalse(updated["is_active"])

    def test_update_unknown_trainer_raises(self):
        with self.assertRaises(ValueError):
            self.service.update_trainer(999, {"is_active": False})

    def test_delete_trainer(self):
        trainer = self.service.create_trainer({"name": "Coach Ravi", "rate_per_session": 500})
        self.assertTrue(self.service.delete_trainer(trainer["id"]))
        with self.assertRaises(ValueError):
            self.service.get_trainer(trainer["id"])

    def test_list_trainers_includes_inactive(self):
        self.service.create_trainer({"name": "Active Coach", "rate_per_session": 500})
        t2 = self.service.create_trainer({"name": "Inactive Coach", "rate_per_session": 400})
        self.service.update_trainer(t2["id"], {"is_active": False})
        all_trainers = self.service.list_trainers()
        self.assertEqual(len(all_trainers), 2)
        active_only = self.service.list_trainers(active_only=True)
        self.assertEqual(len(active_only), 1)


if __name__ == "__main__":
    unittest.main()
