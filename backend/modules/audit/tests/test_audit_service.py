import unittest
from datetime import datetime, timedelta
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from core.database.db_connection import Base
from modules.audit.model.audit_model import AuditLog  # noqa: F401
from modules.audit.repository.audit_repository import AuditRepository
from modules.audit.service.audit_service import AuditService
import modules.user.model.user_model  # noqa: F401


def _factory():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)


class TestAuditService(unittest.TestCase):
    def setUp(self):
        repo = AuditRepository(session_factory=_factory())
        self.service = AuditService(repository=repo)

    def test_log_creates_entry(self):
        entry = self.service.log("ROLE_CHANGE", actor_user_id=1, target_resource_type="user", target_resource_id=2)
        self.assertEqual(entry["action"], "ROLE_CHANGE")
        self.assertEqual(entry["actor_user_id"], 1)

    def test_log_with_details(self):
        entry = self.service.log("REFUND", details={"payment_id": 5, "amount": 100.0})
        self.assertIn("payment_id", entry["details"])

    def test_list_logs(self):
        self.service.log("A")
        self.service.log("B")
        logs = self.service.list_logs()
        self.assertEqual(len(logs), 2)

    def test_log_never_raises(self):
        class BrokenRepo:
            def create(self, _): raise RuntimeError("DB down")
        bad_service = AuditService(repository=BrokenRepo())
        result = bad_service.log("TEST")
        self.assertEqual(result, {})

    def test_filter_by_action(self):
        self.service.log("ROLE_CHANGE", actor_user_id=1)
        self.service.log("REFUND", actor_user_id=1)
        logs = self.service.list_logs(action="ROLE_CHANGE")
        self.assertEqual(len(logs), 1)
        self.assertEqual(logs[0]["action"], "ROLE_CHANGE")

    def test_filter_by_actor(self):
        self.service.log("A", actor_user_id=1)
        self.service.log("A", actor_user_id=2)
        logs = self.service.list_logs(actor_user_id=2)
        self.assertEqual(len(logs), 1)
        self.assertEqual(logs[0]["actor_user_id"], 2)

    def test_filter_by_resource_type(self):
        self.service.log("A", target_resource_type="user")
        self.service.log("A", target_resource_type="ground")
        logs = self.service.list_logs(target_resource_type="ground")
        self.assertEqual(len(logs), 1)
        self.assertEqual(logs[0]["target_resource_type"], "ground")

    def test_pagination_offset(self):
        for i in range(5):
            self.service.log(f"EVENT_{i}")
        page1 = self.service.list_logs(limit=2, offset=0)
        page2 = self.service.list_logs(limit=2, offset=2)
        self.assertEqual(len(page1), 2)
        self.assertEqual(len(page2), 2)
        self.assertNotEqual([e["id"] for e in page1], [e["id"] for e in page2])

    def test_filter_by_date_range(self):
        self.service.log("OLD")
        old_entry = self.service.list_logs(limit=1)[0]
        # Manually backdate via repository update isn't exposed — instead assert
        # that a start_date in the future excludes everything just logged.
        future = datetime.utcnow() + timedelta(days=1)
        logs = self.service.list_logs(start_date=future)
        self.assertEqual(logs, [])
        self.assertIsNotNone(old_entry)
