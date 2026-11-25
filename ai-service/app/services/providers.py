"""Vision providers behind a common adapter (ADR-0006).

* ``mock``  - deterministic answers, no network, no key (local dev / tests / walking skeleton).
* ``groq``  - Groq OpenAI-compatible chat completions with a vision model.

A provider returns a validated :class:`ScoreboardExtraction` or raises :class:`ProviderError`.
"""

import asyncio
import base64
import json
import logging
import re
from typing import Protocol

import httpx
from pydantic import ValidationError

from app.config import Settings
from app.schemas import RosterEntry, ScoreboardExtraction, ScoreboardSide
from app.services.prompt import build_scoreboard_prompt

LOG = logging.getLogger(__name__)

_THINK_BLOCK = re.compile(r"<think>.*?</think>", re.DOTALL | re.IGNORECASE)
_CODE_FENCE = re.compile(r"^```(?:json)?\s*|\s*```$", re.IGNORECASE)
# One bounded retry on 429 / 5xx; the core waits at most ~30 s for the whole analysis.
_MAX_RETRY_DELAY_SECONDS = 5.0
_DEFAULT_RETRY_DELAY_SECONDS = 2.0


class ProviderError(Exception):
    """The provider could not produce a usable extraction (network, HTTP, or schema failure)."""

    def __init__(self, reason: str, message: str):
        super().__init__(message)
        self.reason = reason


class VisionProvider(Protocol):
    name: str

    async def extract_scoreboard(
        self,
        image_bytes: bytes,
        mime_type: str,
        discipline: str,
        team1_name: str,
        team2_name: str,
        team1_roster: list[RosterEntry],
        team2_roster: list[RosterEntry],
    ) -> ScoreboardExtraction: ...


def parse_model_content(content: str) -> ScoreboardExtraction:
    """Parse the model's text answer into the strict extraction schema."""
    text = _THINK_BLOCK.sub("", content or "").strip()
    text = _CODE_FENCE.sub("", text).strip()
    if not text.startswith("{"):
        start, end = text.find("{"), text.rfind("}")
        if start == -1 or end <= start:
            raise ProviderError("INVALID_MODEL_OUTPUT", "Model answer contains no JSON object")
        text = text[start : end + 1]
    try:
        return ScoreboardExtraction.model_validate(json.loads(text))
    except (json.JSONDecodeError, ValidationError) as exc:
        raise ProviderError("INVALID_MODEL_OUTPUT", f"Model answer failed schema validation: {exc}") from exc


class MockVisionProvider:
    """Deterministic provider: pretends the screenshot shows team1 winning 13:9 with the locked rosters."""

    name = "mock"

    _FALLBACK_TEAM1 = ["CyberHawk", "s1mple_kma", "b1t_junior", "electro", "perfecto"]
    _FALLBACK_TEAM2 = ["TitanCaptain", "kpi_ace", "kpi_rush", "kpi_lurk", "kpi_awp"]

    async def extract_scoreboard(
        self,
        image_bytes: bytes,
        mime_type: str,
        discipline: str,
        team1_name: str,
        team2_name: str,
        team1_roster: list[RosterEntry],
        team2_roster: list[RosterEntry],
    ) -> ScoreboardExtraction:
        team1_players = [entry.displayName for entry in team1_roster] or list(self._FALLBACK_TEAM1)
        team2_players = [entry.displayName for entry in team2_roster] or list(self._FALLBACK_TEAM2)
        return ScoreboardExtraction(
            readable=True,
            sides=[
                ScoreboardSide(label=team1_name, score=13, players=team1_players),
                ScoreboardSide(label=team2_name, score=9, players=team2_players),
            ],
            confidence=0.985,
        )


class GroqVisionProvider:
    """Groq chat-completions vision call (same request shape as the reference curl)."""

    name = "groq"

    def __init__(self, settings: Settings, transport: httpx.AsyncBaseTransport | None = None):
        self._settings = settings
        self._transport = transport

    async def extract_scoreboard(
        self,
        image_bytes: bytes,
        mime_type: str,
        discipline: str,
        team1_name: str,
        team2_name: str,
        team1_roster: list[RosterEntry],
        team2_roster: list[RosterEntry],
    ) -> ScoreboardExtraction:
        if not self._settings.groq_api_key:
            raise ProviderError("PROVIDER_NOT_CONFIGURED", "GROQ_API_KEY is not set")

        data_url = f"data:{mime_type};base64,{base64.b64encode(image_bytes).decode('ascii')}"
        payload = {
            "model": self._settings.groq_model,
            "temperature": 0,
            "response_format": {"type": "json_object"},
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": build_scoreboard_prompt(discipline)},
                        {"type": "image_url", "image_url": {"url": data_url}},
                    ],
                }
            ],
        }
        headers = {"Authorization": f"Bearer {self._settings.groq_api_key}"}
        url = f"{self._settings.groq_base_url}/chat/completions"

        try:
            async with httpx.AsyncClient(
                timeout=self._settings.groq_timeout_seconds, transport=self._transport
            ) as client:
                response = await client.post(url, json=payload, headers=headers)
                if response.status_code == 429 or response.status_code >= 500:
                    delay = _retry_delay(response)
                    LOG.warning("Groq returned HTTP %s, retrying once in %.1fs", response.status_code, delay)
                    await asyncio.sleep(delay)
                    response = await client.post(url, json=payload, headers=headers)
        except httpx.HTTPError as exc:
            LOG.warning("Groq request failed: %s", type(exc).__name__)
            raise ProviderError("PROVIDER_UNAVAILABLE", f"Groq request failed: {type(exc).__name__}") from exc

        if response.status_code == 429:
            raise ProviderError("PROVIDER_RATE_LIMITED", "Groq rate limit exceeded")
        if response.status_code != 200:
            LOG.warning("Groq returned HTTP %s", response.status_code)
            raise ProviderError("PROVIDER_ERROR", f"Groq returned HTTP {response.status_code}")

        try:
            content = response.json()["choices"][0]["message"]["content"]
        except (ValueError, KeyError, IndexError, TypeError) as exc:
            raise ProviderError("INVALID_MODEL_OUTPUT", "Unexpected Groq response shape") from exc

        return parse_model_content(content)


def _retry_delay(response: httpx.Response) -> float:
    try:
        requested = float(response.headers.get("retry-after", _DEFAULT_RETRY_DELAY_SECONDS))
    except ValueError:
        requested = _DEFAULT_RETRY_DELAY_SECONDS
    return max(0.0, min(requested, _MAX_RETRY_DELAY_SECONDS))


def build_provider(settings: Settings) -> VisionProvider:
    if settings.provider == "groq":
        return GroqVisionProvider(settings)
    if settings.provider != "mock":
        LOG.warning("Unknown AI_PROVIDER '%s', falling back to mock", settings.provider)
    return MockVisionProvider()
