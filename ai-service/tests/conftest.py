import io
from pathlib import Path

import pytest
from PIL import Image

RESOURCES_DIR = Path(__file__).resolve().parent.parent / "resources"

TERRORIST_ROSTER = [
    {"displayName": "BOT Mayer", "gameIdentifier": "76561190000000001"},
    {"displayName": "BOT Bassey", "gameIdentifier": "76561190000000002"},
    {"displayName": "BOT Colin", "gameIdentifier": "76561190000000003"},
    {"displayName": "BOT Yuri", "gameIdentifier": "76561190000000004"},
    {"displayName": "BOT Hastings", "gameIdentifier": "76561190000000005"},
]
CT_ROSTER = [
    {"displayName": "Vegayta", "gameIdentifier": "76561190000000011"},
    {"displayName": "BOT Ulysses", "gameIdentifier": "76561190000000012"},
    {"displayName": "BOT Efe", "gameIdentifier": "76561190000000013"},
    {"displayName": "BOT Panama", "gameIdentifier": "76561190000000014"},
    {"displayName": "BOT Dios", "gameIdentifier": "76561190000000015"},
]


def make_png_bytes(size: tuple[int, int] = (64, 32)) -> bytes:
    buffer = io.BytesIO()
    Image.new("RGB", size, color=(30, 30, 30)).save(buffer, format="PNG")
    return buffer.getvalue()


@pytest.fixture(autouse=True)
def isolated_env(monkeypatch):
    """Every test starts from a known configuration: mock provider, no token, no Groq key."""
    monkeypatch.setenv("AI_PROVIDER", "mock")
    monkeypatch.delenv("AI_SERVICE_API_TOKEN", raising=False)
    monkeypatch.delenv("GROQ_API_KEY", raising=False)
    monkeypatch.delenv("AI_MIN_CONFIDENCE", raising=False)


@pytest.fixture
def png_bytes() -> bytes:
    return make_png_bytes()


@pytest.fixture
def cs2_screenshot_bytes() -> bytes:
    return (RESOURCES_DIR / "test_screenshot_1.jpg").read_bytes()
