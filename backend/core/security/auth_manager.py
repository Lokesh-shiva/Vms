from __future__ import annotations

_ALL_PERMISSIONS: frozenset[str] = frozenset(
    {
        "manage_roles",
        "view_finance",
        "manage_finance",
        "view_operations",
        "manage_operations",
        "view_grounds",
        "manage_grounds",
        "view_tournaments",
        "manage_tournaments",
        "view_users",
        "manage_users",
        "view_config",
        "manage_config",
        "view_audit_log",
        "manage_audit_log",
        "view_disputes",
        "manage_disputes",
        "issue_refund",
        "approve_refund",
        "view_bookings",
        "view_matches",
        "view_queue",
        "manage_queue",
        "assign_captain",
        "manage_captain",
        "view_csr",
        "manage_csr",
    }
)

_ROLE_PERMISSIONS: dict[str, frozenset[str]] = {
    "super_admin": _ALL_PERMISSIONS,
    "ops_manager": frozenset(
        {
            "view_operations",
            "manage_operations",
            "view_matches",
            "view_queue",
            "manage_queue",
            "view_disputes",
            "manage_disputes",
            "view_bookings",
        }
    ),
    "ground_owner": frozenset(
        {
            "view_grounds",
            "view_bookings",
        }
    ),
    "tournament_manager": frozenset(
        {
            "view_tournaments",
            "manage_tournaments",
            "view_matches",
        }
    ),
    "support": frozenset(
        {
            "view_users",
            "view_bookings",
            "view_matches",
            "view_disputes",
            "manage_disputes",
            "issue_refund",
        }
    ),
    "finance": frozenset(
        {
            "view_finance",
            "manage_finance",
            "approve_refund",
            "issue_refund",
            "view_bookings",
        }
    ),
    "csr_partner": frozenset(
        {
            "view_csr",
            "view_tournaments",
        }
    ),
    "user": frozenset(),
}


class AuthManager:
    """
    Role-to-permission resolver for Plixo Control Centre.

    Permission checks are role-string-based (matching JWT 'role' claim).
    """

    def has_permission(self, role: str, permission: str) -> bool:
        """Return True if *role* includes *permission*."""
        return permission in _ROLE_PERMISSIONS.get(role, frozenset())

    def get_permissions(self, role: str) -> frozenset[str]:
        """Return all permissions for *role*."""
        return _ROLE_PERMISSIONS.get(role, frozenset())


auth_manager = AuthManager()
