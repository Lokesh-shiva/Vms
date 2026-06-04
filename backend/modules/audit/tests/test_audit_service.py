import unittest
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
