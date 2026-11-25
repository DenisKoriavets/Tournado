"""Runtime configuration for the AI arbitration service (ADR-0006).

Settings are read from environment variables on every call so tests can override them
with ``monkeypatch.setenv`` without reloading modules.
"""

import os
from dataclasses import dataclass

DEFAULT_GROQ_MODEL = "qwen/qwen3.8-27b"
DEFAULT_GROQ_BASE_URL = "https://api.groq.com/openai/v1"


@dataclass(frozen=True)
class Settings:
    provider: str
    groq_api_key: str
    groq_model: str
    groq_base_url: str
    groq_timeout_seconds: float
    api_token: str
    min_confidence: float
    max_image_bytes: int


def _float_env(name: str, default: float) -> float:
    try:
        return float(os.getenv(name, str(default)))
    except ValueError:
        return default


def get_settings() -> Settings:
    return Settings(
        provider=os.getenv("AI_PROVIDER", "mock").strip().lower() or "mock",
        groq_api_key=os.getenv("GROQ_API_KEY", "").strip(),
        groq_model=os.getenv("GROQ_MODEL", DEFAULT_GROQ_MODEL).strip() or DEFAULT_GROQ_MODEL,
        groq_base_url=os.getenv("GROQ_BASE_URL", DEFAULT_GROQ_BASE_URL).rstrip("/"),
        groq_timeout_seconds=_float_env("GROQ_TIMEOUT_SECONDS", 25.0),
        api_token=os.getenv("AI_SERVICE_API_TOKEN", "").strip(),
        min_confidence=_float_env("AI_MIN_CONFIDENCE", 0.5),
        max_image_bytes=int(_float_env("AI_MAX_IMAGE_BYTES", 10 * 1024 * 1024)),
    )
