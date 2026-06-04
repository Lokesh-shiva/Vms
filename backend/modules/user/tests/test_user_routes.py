"""
Tests for GET /api/v1/users/assignable-roles.
"""
import pytest
from fastapi.testclient import TestClient

from backend.main import app
from modules.auth.dependencies.auth_dependencies import get_current_user


def _override_role(role: str):
    def _dep():
        return {"id": 1, "role": role, "is_active": True}
    return _dep


@pytest.fixture()
def super_admin_client():
    app.dependency_overrides[get_current_user] = _override_role("super_admin")
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture()
def ops_manager_client():
    app.dependency_overrides[get_current_user] = _override_role("ops_manager")
    yield TestClient(app)
    app.dependency_overrides.clear()


def test_super_admin_gets_all_roles(super_admin_client):
    """SUPER_ADMIN receives all 8 assignable roles."""
    resp = super_admin_client.get("/api/v1/users/assignable-roles")
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    roles = data["data"]["assignable_roles"]
    assert set(roles) == {
        "super_admin", "ops_manager", "ground_owner",
        "tournament_manager", "support", "finance", "csr_partner", "user",
    }


def test_non_admin_gets_empty_list(ops_manager_client):
    """Non-SUPER_ADMIN caller gets an empty assignable-roles list."""
    resp = ops_manager_client.get("/api/v1/users/assignable-roles")
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    assert data["data"]["assignable_roles"] == []


def test_unauthenticated_gets_401():
    """No token → 401."""
    from backend.main import app as _app
    _app.dependency_overrides.clear()
    client = TestClient(_app)
    resp = client.get("/api/v1/users/assignable-roles")
    assert resp.status_code == 401
