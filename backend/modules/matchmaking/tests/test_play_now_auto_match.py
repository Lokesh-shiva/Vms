"""
Route-level tests for POST /api/v1/matchmaking/play-now's auto-matchmaking
behavior: it should try to pair the caller with an existing waiting session
before starting a new solo one, and fall back gracefully if that join loses
a race.
"""

import unittest
from unittest.mock import MagicMock, patch

from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from core.database.db_connection import Base
from modules.location.model.location_model import Location  # noqa: F401
from modules.cart_type.model.cart_type_model import CartType  # noqa: F401
from modules.cart.model.cart_model import Cart  # noqa: F401
from modules.match.model.match_model import Match, MatchPlayer  # noqa: F401
from modules.user.model.user_model import User  # noqa: F401
from modules.timeslot.model.timeslot_model import Timeslot  # noqa: F401
from modules.sport.model.sport_model import Sport  # noqa: F401
from modules.booking.model.booking_model import Booking  # noqa: F401
from modules.booking_item.model.booking_item_model import BookingItem  # noqa: F401
from modules.item.model.item_model import Item  # noqa: F401
from modules.fee_config.model.fee_config_model import RegionCartTypeConfig  # noqa: F401
from modules.payment.model.payment_model import Payment  # noqa: F401
from modules.payment.model.system_config_model import SystemConfig  # noqa: F401
from modules.matchmaking.model.queue_entry_model import QueueEntry  # noqa: F401
from modules.captain.model.captain_model import Captain  # noqa: F401
from modules.society.model.society_model import Society  # noqa: F401

from modules.auth.dependencies.auth_dependencies import get_current_user
from modules.matchmaking.controller.matchmaking_routes import router as matchmaking_router

USER = {"id": 2, "name": "Player Two", "phone": "+1000000002", "role": "user", "is_active": True, "region_id": 1}


def _build_app() -> FastAPI:
    app = FastAPI()
    app.include_router(matchmaking_router)
    return app


class TestPlayNowAutoMatch(unittest.TestCase):
    def setUp(self):
        # TestClient dispatches the route through a worker thread (via anyio's
        # run_sync_in_worker_thread). A plain sqlite:///:memory: engine defaults
        # to SingletonThreadPool, which hands each thread its OWN separate
        # in-memory database — the route's SessionLocal() would see a blank,
        # tableless DB. StaticPool forces every checkout to share one
        # connection (and therefore one in-memory DB) across threads.
        engine = create_engine(
            "sqlite:///:memory:",
            connect_args={"check_same_thread": False},
            poolclass=StaticPool,
        )
        Base.metadata.create_all(bind=engine)
        factory = sessionmaker(bind=engine, autoflush=False, autocommit=False)

        # Seed a real CartType so the route's inline "resolve sport by name" lookup works.
        session = factory()
        session.add(CartType(id=1, name="Badminton", is_active=True))
        session.commit()
        session.close()

        self._session_local_patch = patch(
            "modules.matchmaking.controller.matchmaking_routes.SessionLocal", factory
        )
        self._session_local_patch.start()

        self.app = _build_app()
        self.client = TestClient(self.app, raise_server_exceptions=False)
        self.app.dependency_overrides[get_current_user] = lambda: USER

    def tearDown(self):
        self.app.dependency_overrides.clear()
        self._session_local_patch.stop()

    @patch("modules.matchmaking.controller.matchmaking_routes.match_service")
    @patch("modules.matchmaking.controller.matchmaking_routes.match_repository")
    def test_joins_existing_waiting_match_instead_of_creating_new_one(self, mock_repo, mock_service):
        mock_repo.find_active_by_user.return_value = None
        mock_repo.find_joinable_playnow.return_value = {"id": 7}
        mock_service.join_match.return_value = {"id": 7}
        mock_repo.find_by_id_enriched.return_value = {
            "id": 7, "status": "MATCHED", "joined_players": 2, "sport": "Badminton",
        }

        resp = self.client.post("/api/v1/matchmaking/play-now", json={"sport": "Badminton"})

        self.assertEqual(resp.status_code, 201)
        body = resp.json()["data"]
        self.assertTrue(body["match_found"])
        self.assertEqual(body["match_id"], 7)
        mock_service.join_match.assert_called_once_with(2, 7)
        mock_repo.create_play_now.assert_not_called()

    @patch("modules.matchmaking.controller.matchmaking_routes.match_service")
    @patch("modules.matchmaking.controller.matchmaking_routes.match_repository")
    def test_creates_new_session_when_nothing_joinable(self, mock_repo, mock_service):
        mock_repo.find_active_by_user.return_value = None
        mock_repo.find_joinable_playnow.return_value = None
        mock_repo.create_play_now.return_value = {"id": 9}
        mock_repo.find_by_id_enriched.return_value = {
            "id": 9, "status": "WAITING", "joined_players": 1, "sport": "Badminton",
        }

        resp = self.client.post("/api/v1/matchmaking/play-now", json={"sport": "Badminton"})

        self.assertEqual(resp.status_code, 201)
        body = resp.json()["data"]
        self.assertFalse(body["match_found"])
        mock_service.join_match.assert_not_called()
        mock_repo.create_play_now.assert_called_once()

    @patch("modules.matchmaking.controller.matchmaking_routes.match_service")
    @patch("modules.matchmaking.controller.matchmaking_routes.match_repository")
    def test_falls_back_to_new_session_if_join_loses_race(self, mock_repo, mock_service):
        mock_repo.find_active_by_user.return_value = None
        mock_repo.find_joinable_playnow.return_value = {"id": 7}
        mock_service.join_match.side_effect = ValueError("This match is already full.")
        mock_repo.create_play_now.return_value = {"id": 10}
        mock_repo.find_by_id_enriched.return_value = {
            "id": 10, "status": "WAITING", "joined_players": 1, "sport": "Badminton",
        }

        resp = self.client.post("/api/v1/matchmaking/play-now", json={"sport": "Badminton"})

        self.assertEqual(resp.status_code, 201)
        body = resp.json()["data"]
        self.assertEqual(body["match_id"], None)
        mock_repo.create_play_now.assert_called_once()

    @patch("modules.matchmaking.controller.matchmaking_routes.match_service")
    @patch("modules.matchmaking.controller.matchmaking_routes.match_repository")
    def test_rejects_when_already_in_an_active_match(self, mock_repo, mock_service):
        mock_repo.find_active_by_user.return_value = {"id": 1, "status": "WAITING"}

        resp = self.client.post("/api/v1/matchmaking/play-now", json={"sport": "Badminton"})

        self.assertEqual(resp.status_code, 400)
        mock_repo.find_joinable_playnow.assert_not_called()


if __name__ == "__main__":
    unittest.main()
