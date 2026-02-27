import unittest

from modules.user.schemas.user_schema import CreateUserSchema, UpdateUserSchema


class TestCreateUserSchema(unittest.TestCase):
    """Validation tests for CreateUserSchema."""

    def test_valid_input(self):
        schema = CreateUserSchema({"name": "Alice", "phone": "+1234567890"})
        self.assertTrue(schema.is_valid())
        self.assertEqual(schema.validated_data["role"], "user")

    def test_valid_input_with_admin_role(self):
        schema = CreateUserSchema({"name": "Alice", "phone": "+1234567890", "role": "admin"})
        self.assertTrue(schema.is_valid())
        self.assertEqual(schema.validated_data["role"], "admin")

    def test_missing_name(self):
        schema = CreateUserSchema({"phone": "+1234567890"})
        self.assertFalse(schema.is_valid())

    def test_invalid_phone(self):
        schema = CreateUserSchema({"name": "Alice", "phone": "abc"})
        self.assertFalse(schema.is_valid())

    def test_invalid_role_rejected(self):
        """Roles outside USER/ADMIN must be rejected."""
        for bad_role in ["superuser", "viewer", "staff", "root", ""]:
            schema = CreateUserSchema({"name": "X", "phone": "+1234567890", "role": bad_role})
            self.assertFalse(schema.is_valid(), f"Role '{bad_role}' should be rejected.")

    def test_valid_roles_accepted(self):
        """Only 'user' and 'admin' are valid."""
        for good_role in ["user", "admin"]:
            schema = CreateUserSchema({"name": "X", "phone": "+1234567890", "role": good_role})
            self.assertTrue(schema.is_valid(), f"Role '{good_role}' should be accepted.")

    def test_role_case_insensitive(self):
        """'USER', 'User', 'ADMIN', 'Admin' must all be accepted and stored lowercase."""
        for role_input, expected in [("USER", "user"), ("User", "user"), ("ADMIN", "admin"), ("Admin", "admin")]:
            schema = CreateUserSchema({"name": "X", "phone": "+1234567890", "role": role_input})
            self.assertTrue(schema.is_valid(), f"Role '{role_input}' should be accepted.")
            self.assertEqual(schema.validated_data["role"], expected)


class TestUpdateUserSchema(unittest.TestCase):
    """Validation tests for UpdateUserSchema."""

    def test_partial_update(self):
        schema = UpdateUserSchema({"name": "Updated Name"})
        self.assertTrue(schema.is_valid())
        self.assertIn("name", schema.validated_data)

    def test_empty_update(self):
        schema = UpdateUserSchema({})
        self.assertTrue(schema.is_valid())

    def test_invalid_role_update(self):
        schema = UpdateUserSchema({"role": "root"})
        self.assertFalse(schema.is_valid())

    def test_valid_role_update(self):
        schema = UpdateUserSchema({"role": "admin"})
        self.assertTrue(schema.is_valid())
        self.assertEqual(schema.validated_data["role"], "admin")

    def test_role_case_insensitive_update(self):
        """Update schema normalizes role to lowercase."""
        for role_input, expected in [("ADMIN", "admin"), ("User", "user")]:
            schema = UpdateUserSchema({"role": role_input})
            self.assertTrue(schema.is_valid(), f"Role '{role_input}' should be accepted.")
            self.assertEqual(schema.validated_data["role"], expected)


if __name__ == "__main__":
    unittest.main()
