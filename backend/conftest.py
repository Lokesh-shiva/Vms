"""
Root conftest.py — adds the backend/ directory to sys.path so all tests
can use bare imports (from core.database..., from modules...) regardless
of whether pytest is run from the project root or the backend/ directory.
"""
import sys
import os

# Insert backend/ at the front of sys.path
sys.path.insert(0, os.path.dirname(__file__))
