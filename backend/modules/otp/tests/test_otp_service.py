import unittest
from unittest.mock import MagicMock, patch

import requests

from modules.otp.service.otp_service import OtpService, OtpDeliveryError


class TestOtpServiceDevMode(unittest.TestCase):
    """OTP_DEV_MODE=true (the default) — no real SMS provider involved."""

    @patch("modules.otp.service.otp_service.otp_repository")
    @patch("modules.otp.service.otp_service.requests.post")
    def test_dev_mode_never_calls_provider(self, mock_post, mock_repo):
        service = OtpService()
        service.send_otp("+919876543210")
        mock_post.assert_not_called()
        mock_repo.create.assert_called_once()

    @patch("modules.otp.service.otp_service.otp_repository")
    def test_dev_mode_always_verifies_123456(self, mock_repo):
        service = OtpService()
        self.assertTrue(service.verify_otp("+919876543210", "123456"))
        mock_repo.find_active.assert_not_called()


class TestOtpServiceMsg91(unittest.TestCase):
    """OTP_DEV_MODE=false — real MSG91 delivery path."""

    @patch("modules.otp.service.otp_service._DEV_MODE", False)
    @patch("modules.otp.service.otp_service._MSG91_AUTH_KEY", None)
    @patch("modules.otp.service.otp_service.otp_repository")
    def test_missing_config_raises_not_implemented(self, mock_repo):
        service = OtpService()
        with self.assertRaises(NotImplementedError):
            service.send_otp("+919876543210")

    @patch("modules.otp.service.otp_service._DEV_MODE", False)
    @patch("modules.otp.service.otp_service._MSG91_AUTH_KEY", "test-key")
    @patch("modules.otp.service.otp_service._MSG91_TEMPLATE_ID", "test-template")
    @patch("modules.otp.service.otp_service.otp_repository")
    @patch("modules.otp.service.otp_service.requests.post")
    def test_successful_send_calls_msg91_with_code(self, mock_post, mock_repo):
        mock_post.return_value = MagicMock(json=lambda: {"type": "success"})
        service = OtpService()
        service.send_otp("+919876543210")

        mock_post.assert_called_once()
        _, kwargs = mock_post.call_args
        self.assertEqual(kwargs["headers"]["authkey"], "test-key")
        self.assertEqual(kwargs["json"]["template_id"], "test-template")
        self.assertEqual(kwargs["json"]["recipients"][0]["mobiles"], "919876543210")
        # The code sent to MSG91 must be the same one persisted for later verification.
        stored_code = mock_repo.create.call_args.kwargs["code"]
        self.assertEqual(kwargs["json"]["recipients"][0]["OTP"], stored_code)

    @patch("modules.otp.service.otp_service._DEV_MODE", False)
    @patch("modules.otp.service.otp_service._MSG91_AUTH_KEY", "test-key")
    @patch("modules.otp.service.otp_service._MSG91_TEMPLATE_ID", "test-template")
    @patch("modules.otp.service.otp_service.otp_repository")
    @patch("modules.otp.service.otp_service.requests.post")
    def test_msg91_error_response_raises_delivery_error(self, mock_post, mock_repo):
        mock_post.return_value = MagicMock(json=lambda: {"type": "error", "message": "bad template"})
        service = OtpService()
        with self.assertRaises(OtpDeliveryError):
            service.send_otp("+919876543210")

    @patch("modules.otp.service.otp_service._DEV_MODE", False)
    @patch("modules.otp.service.otp_service._MSG91_AUTH_KEY", "test-key")
    @patch("modules.otp.service.otp_service._MSG91_TEMPLATE_ID", "test-template")
    @patch("modules.otp.service.otp_service.otp_repository")
    @patch("modules.otp.service.otp_service.requests.post")
    def test_network_failure_raises_delivery_error(self, mock_post, mock_repo):
        mock_post.side_effect = requests.ConnectionError("network down")
        service = OtpService()
        with self.assertRaises(OtpDeliveryError):
            service.send_otp("+919876543210")

    @patch("modules.otp.service.otp_service._DEV_MODE", False)
    @patch("modules.otp.service.otp_service._MSG91_AUTH_KEY", "test-key")
    @patch("modules.otp.service.otp_service._MSG91_TEMPLATE_ID", "test-template")
    @patch("modules.otp.service.otp_service.otp_repository")
    def test_verify_otp_checks_repository_when_not_dev_code(self, mock_repo):
        mock_repo.find_active.return_value = None
        service = OtpService()
        self.assertFalse(service.verify_otp("+919876543210", "654321"))
        mock_repo.find_active.assert_called_once()
