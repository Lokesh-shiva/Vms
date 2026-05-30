# backend/modules/billing/tests/test_billing_calculator.py
import math
import pytest

from modules.billing.billing_calculator import (
    calculate_blocks,
    calculate_time_bill,
    BillingError,
)


class TestCalculateBlocks:
    def test_zero_minutes_is_one_block(self):
        # Any started session bills at least one block
        assert calculate_blocks(0, block_duration_minutes=45) == 1

    def test_under_one_block(self):
        assert calculate_blocks(30, block_duration_minutes=45) == 1

    def test_exactly_one_block(self):
        assert calculate_blocks(45, block_duration_minutes=45) == 1

    def test_one_minute_into_second_block(self):
        assert calculate_blocks(46, block_duration_minutes=45) == 2

    def test_exactly_two_blocks(self):
        assert calculate_blocks(90, block_duration_minutes=45) == 2

    def test_into_third_block(self):
        assert calculate_blocks(91, block_duration_minutes=45) == 3

    def test_custom_block_size(self):
        assert calculate_blocks(60, block_duration_minutes=30) == 2

    def test_negative_minutes_raises(self):
        with pytest.raises(BillingError):
            calculate_blocks(-5, block_duration_minutes=45)

    def test_zero_block_duration_raises(self):
        with pytest.raises(BillingError):
            calculate_blocks(45, block_duration_minutes=0)


class TestCalculateTimeBill:
    def test_one_block_no_surge(self):
        bill = calculate_time_bill(
            session_minutes=30, rate_per_block=60.0,
            block_duration_minutes=45, surge_multiplier=1.0,
        )
        assert bill == 60.0

    def test_two_blocks_no_surge(self):
        bill = calculate_time_bill(
            session_minutes=90, rate_per_block=60.0,
            block_duration_minutes=45, surge_multiplier=1.0,
        )
        assert bill == 120.0

    def test_surge_one_point_five(self):
        bill = calculate_time_bill(
            session_minutes=90, rate_per_block=60.0,
            block_duration_minutes=45, surge_multiplier=1.5,
        )
        assert bill == 180.0

    def test_rounds_to_two_decimals(self):
        bill = calculate_time_bill(
            session_minutes=45, rate_per_block=33.33,
            block_duration_minutes=45, surge_multiplier=1.1,
        )
        # 1 * 33.33 * 1.1 = 36.663 -> 36.66
        assert bill == 36.66

    def test_negative_rate_raises(self):
        with pytest.raises(BillingError):
            calculate_time_bill(
                session_minutes=45, rate_per_block=-1.0,
                block_duration_minutes=45, surge_multiplier=1.0,
            )

    def test_surge_below_one_raises(self):
        with pytest.raises(BillingError):
            calculate_time_bill(
                session_minutes=45, rate_per_block=60.0,
                block_duration_minutes=45, surge_multiplier=0.5,
            )
