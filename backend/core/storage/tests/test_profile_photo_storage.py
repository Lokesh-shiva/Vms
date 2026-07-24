import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from core.storage import profile_photo_storage


class TestProfilePhotoStorage(unittest.TestCase):
    def setUp(self):
        self._tmpdir = tempfile.TemporaryDirectory()
        self._patcher = patch.object(
            profile_photo_storage, "_UPLOAD_DIR", Path(self._tmpdir.name)
        )
        self._patcher.start()

    def tearDown(self):
        self._patcher.stop()
        self._tmpdir.cleanup()

    def test_read_missing_returns_none(self):
        self.assertIsNone(profile_photo_storage.read_profile_photo(999))

    def test_save_then_read_roundtrip(self):
        profile_photo_storage.save_profile_photo(1, "photo.jpg", b"fake-jpeg-bytes")
        content, media_type = profile_photo_storage.read_profile_photo(1)
        self.assertEqual(content, b"fake-jpeg-bytes")
        self.assertEqual(media_type, "image/jpeg")

    def test_save_detects_png_media_type(self):
        profile_photo_storage.save_profile_photo(1, "photo.png", b"fake-png-bytes")
        _, media_type = profile_photo_storage.read_profile_photo(1)
        self.assertEqual(media_type, "image/png")

    def test_unknown_extension_falls_back_to_jpeg(self):
        profile_photo_storage.save_profile_photo(1, "photo.heic", b"fake-bytes")
        _, media_type = profile_photo_storage.read_profile_photo(1)
        self.assertEqual(media_type, "image/jpeg")

    def test_reupload_replaces_previous_file(self):
        profile_photo_storage.save_profile_photo(1, "photo.jpg", b"first")
        profile_photo_storage.save_profile_photo(1, "photo.png", b"second")
        content, media_type = profile_photo_storage.read_profile_photo(1)
        self.assertEqual(content, b"second")
        self.assertEqual(media_type, "image/png")
        # Only one file should remain for this user
        remaining = list(Path(self._tmpdir.name).glob("user_1.*"))
        self.assertEqual(len(remaining), 1)

    def test_photos_are_scoped_per_user(self):
        profile_photo_storage.save_profile_photo(1, "photo.jpg", b"user-one")
        profile_photo_storage.save_profile_photo(2, "photo.jpg", b"user-two")
        content1, _ = profile_photo_storage.read_profile_photo(1)
        content2, _ = profile_photo_storage.read_profile_photo(2)
        self.assertEqual(content1, b"user-one")
        self.assertEqual(content2, b"user-two")

    def test_delete_profile_photo(self):
        profile_photo_storage.save_profile_photo(1, "photo.jpg", b"data")
        self.assertTrue(profile_photo_storage.delete_profile_photo(1))
        self.assertIsNone(profile_photo_storage.read_profile_photo(1))

    def test_delete_missing_returns_false(self):
        self.assertFalse(profile_photo_storage.delete_profile_photo(999))
