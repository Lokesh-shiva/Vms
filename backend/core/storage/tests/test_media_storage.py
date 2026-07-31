import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from core.storage import media_storage


class TestMediaStorage(unittest.TestCase):
    def setUp(self):
        self._tmpdir = tempfile.TemporaryDirectory()
        self._patcher = patch.object(media_storage, "_UPLOAD_ROOT", Path(self._tmpdir.name))
        self._patcher.start()

    def tearDown(self):
        self._patcher.stop()
        self._tmpdir.cleanup()

    def test_read_missing_returns_none(self):
        self.assertIsNone(media_storage.read_media("grounds", 999))

    def test_save_then_read_roundtrip(self):
        media_storage.save_media("grounds", 1, "photo.jpg", b"fake-jpeg-bytes")
        content, media_type = media_storage.read_media("grounds", 1)
        self.assertEqual(content, b"fake-jpeg-bytes")
        self.assertEqual(media_type, "image/jpeg")

    def test_namespaces_are_isolated(self):
        media_storage.save_media("grounds", 1, "photo.jpg", b"ground-photo")
        media_storage.save_media("sports", 1, "photo.jpg", b"sport-photo")
        ground_content, _ = media_storage.read_media("grounds", 1)
        sport_content, _ = media_storage.read_media("sports", 1)
        self.assertEqual(ground_content, b"ground-photo")
        self.assertEqual(sport_content, b"sport-photo")

    def test_reupload_replaces_previous_file(self):
        media_storage.save_media("grounds", 1, "photo.jpg", b"first")
        media_storage.save_media("grounds", 1, "photo.png", b"second")
        content, media_type = media_storage.read_media("grounds", 1)
        self.assertEqual(content, b"second")
        self.assertEqual(media_type, "image/png")

    def test_delete_media(self):
        media_storage.save_media("grounds", 1, "photo.jpg", b"data")
        self.assertTrue(media_storage.delete_media("grounds", 1))
        self.assertIsNone(media_storage.read_media("grounds", 1))

    def test_delete_missing_returns_false(self):
        self.assertFalse(media_storage.delete_media("grounds", 999))
