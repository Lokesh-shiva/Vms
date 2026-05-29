from datetime import datetime
from modules.billing.billing_calculator import compute_session_bill


def test_compute_session_bill_basic():
    start = datetime(2026, 7, 1, 6, 0, 0)
    end = datetime(2026, 7, 1, 7, 30, 0)  # 90 minutes
    result = compute_session_bill(
        started_at=start, ended_at=end,
        rate_per_block=60.0, block_duration_minutes=45,
        max_duration_minutes=180, surge_multiplier=1.0,
    )
    assert result["session_minutes"] == 90
    assert result["session_blocks"] == 2
    assert result["time_bill_amount"] == 120.0
    assert result["surge_multiplier_snapshot"] == 1.0


def test_compute_session_bill_caps_at_max_duration():
    start = datetime(2026, 7, 1, 6, 0, 0)
    end = datetime(2026, 7, 1, 12, 0, 0)  # 360 minutes, capped to 180
    result = compute_session_bill(
        started_at=start, ended_at=end,
        rate_per_block=60.0, block_duration_minutes=45,
        max_duration_minutes=180, surge_multiplier=1.0,
    )
    assert result["session_minutes"] == 180
    assert result["session_blocks"] == 4  # ceil(180/45)
    assert result["time_bill_amount"] == 240.0


def test_compute_session_bill_with_surge():
    start = datetime(2026, 7, 1, 6, 0, 0)
    end = datetime(2026, 7, 1, 6, 30, 0)  # 30 min -> 1 block
    result = compute_session_bill(
        started_at=start, ended_at=end,
        rate_per_block=60.0, block_duration_minutes=45,
        max_duration_minutes=180, surge_multiplier=2.0,
    )
    assert result["session_minutes"] == 30
    assert result["session_blocks"] == 1
    assert result["time_bill_amount"] == 120.0
    assert result["surge_multiplier_snapshot"] == 2.0
