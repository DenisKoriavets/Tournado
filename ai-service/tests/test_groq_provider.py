import json
import os

import httpx
import pytest

from app.config import get_settings
from app.schemas import RosterEntry
from app.services.analyzer import analyze_dispute_screenshot
from app.services.providers import GroqVisionProvider, ProviderError, parse_model_content
from tests.conftest import CT_ROSTER, TERRORIST_ROSTER, make_png_bytes

MODEL_ANSWER = {
    "readable": True,
    "sides": [
        {"label": "COUNTER-TERRORISTS", "score": 1, "players": ["BOT Ulysses", "BOT Efe", "Vegayta", "BOT Panama", "BOT Dios"]},
        {"label": "TERRORISTS", "score": 0, "players": ["BOT Mayer", "BOT Bassey", "BOT Colin", "BOT Yuri", "BOT Hastings"]},
    ],
    "confidence": 0.96,
}


def _groq_settings(monkeypatch):
    monkeypatch.setenv("AI_PROVIDER", "groq")
    monkeypatch.setenv("GROQ_API_KEY", "test-key")
    return get_settings()


def _completion(content: str) -> dict:
    return {"choices": [{"message": {"role": "assistant", "content": content}}]}


async def test_groq_request_shape_and_parsing(monkeypatch):
    captured = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["auth"] = request.headers["Authorization"]
        captured["body"] = json.loads(request.content)
        return httpx.Response(200, json=_completion(json.dumps(MODEL_ANSWER)))

    provider = GroqVisionProvider(_groq_settings(monkeypatch), transport=httpx.MockTransport(handler))

    extraction = await provider.extract_scoreboard(make_png_bytes(), "image/png", "CS2", "A", "B", [], [])

    assert captured["url"] == "https://api.groq.com/openai/v1/chat/completions"
    assert captured["auth"] == "Bearer test-key"
    assert captured["body"]["model"] == "qwen/qwen3.8-27b"
    assert captured["body"]["temperature"] == 0
    content = captured["body"]["messages"][0]["content"]
    assert content[0]["type"] == "text" and "UNTRUSTED" in content[0]["text"]
    assert content[1]["image_url"]["url"].startswith("data:image/png;base64,")
    assert extraction.sides[0].score == 1


async def test_groq_client_error_raises_provider_error_without_retry(monkeypatch):
    calls = []

    def handler(request):
        calls.append(request)
        return httpx.Response(400, json={"error": {"message": "bad request"}})

    provider = GroqVisionProvider(_groq_settings(monkeypatch), transport=httpx.MockTransport(handler))

    with pytest.raises(ProviderError) as error:
        await provider.extract_scoreboard(make_png_bytes(), "image/png", "CS2", "A", "B", [], [])

    assert error.value.reason == "PROVIDER_ERROR"
    assert len(calls) == 1


async def test_groq_rate_limit_is_retried_once_then_succeeds(monkeypatch):
    responses = iter([
        httpx.Response(429, headers={"retry-after": "0"}, json={}),
        httpx.Response(200, json=_completion(json.dumps(MODEL_ANSWER))),
    ])
    provider = GroqVisionProvider(
        _groq_settings(monkeypatch), transport=httpx.MockTransport(lambda request: next(responses))
    )

    extraction = await provider.extract_scoreboard(make_png_bytes(), "image/png", "CS2", "A", "B", [], [])

    assert extraction.sides[1].label == "TERRORISTS"


async def test_groq_persistent_rate_limit_raises_rate_limited(monkeypatch):
    provider = GroqVisionProvider(
        _groq_settings(monkeypatch),
        transport=httpx.MockTransport(lambda request: httpx.Response(429, headers={"retry-after": "0"}, json={})),
    )

    with pytest.raises(ProviderError) as error:
        await provider.extract_scoreboard(make_png_bytes(), "image/png", "CS2", "A", "B", [], [])

    assert error.value.reason == "PROVIDER_RATE_LIMITED"


async def test_groq_network_error_raises_provider_unavailable(monkeypatch):
    def handler(request):
        raise httpx.ConnectTimeout("timeout", request=request)

    provider = GroqVisionProvider(_groq_settings(monkeypatch), transport=httpx.MockTransport(handler))

    with pytest.raises(ProviderError) as error:
        await provider.extract_scoreboard(make_png_bytes(), "image/png", "CS2", "A", "B", [], [])

    assert error.value.reason == "PROVIDER_UNAVAILABLE"


def test_parse_model_content_strips_fences_and_think_blocks():
    content = "<think>reasoning</think>\n```json\n" + json.dumps(MODEL_ANSWER) + "\n```"

    assert parse_model_content(content).confidence == 0.96


def test_parse_model_content_rejects_schema_violations():
    with pytest.raises(ProviderError):
        parse_model_content('{"sides": [{"score": -5, "players": []}], "confidence": 3}')
    with pytest.raises(ProviderError):
        parse_model_content("I cannot help with that")


async def test_full_pipeline_with_groq_answer_maps_sides_by_roster(monkeypatch):
    transport = httpx.MockTransport(lambda request: httpx.Response(200, json=_completion(json.dumps(MODEL_ANSWER))))
    settings = _groq_settings(monkeypatch)

    result = await analyze_dispute_screenshot(
        raw_image=make_png_bytes(),
        discipline="CS2",
        team1_name="QA Terror Squad",
        team2_name="QA Vegayta Five",
        team1_roster=[RosterEntry(**entry) for entry in TERRORIST_ROSTER],
        team2_roster=[RosterEntry(**entry) for entry in CT_ROSTER],
        provider=GroqVisionProvider(settings, transport=transport),
        settings=settings,
    )

    assert result.status == "SUCCESS"
    assert result.recognizedScore.model_dump() == {"team1": 0, "team2": 1}
    assert result.provider == "groq"


@pytest.mark.live
@pytest.mark.skipif(not os.getenv("LIVE_GROQ_API_KEY"), reason="set LIVE_GROQ_API_KEY to call the real Groq API")
async def test_live_groq_reads_cs2_test_screenshot(monkeypatch, cs2_screenshot_bytes):
    monkeypatch.setenv("AI_PROVIDER", "groq")
    monkeypatch.setenv("GROQ_API_KEY", os.environ["LIVE_GROQ_API_KEY"])
    settings = get_settings()

    result = await analyze_dispute_screenshot(
        raw_image=cs2_screenshot_bytes,
        discipline="CS2",
        team1_name="QA Terror Squad",
        team2_name="QA Vegayta Five",
        team1_roster=[RosterEntry(**entry) for entry in TERRORIST_ROSTER],
        team2_roster=[RosterEntry(**entry) for entry in CT_ROSTER],
        provider=GroqVisionProvider(settings),
        settings=settings,
    )

    assert result.status == "SUCCESS", result.reason
    assert result.recognizedScore.model_dump() == {"team1": 0, "team2": 1}
    assert result.allRosterNicknamesFound is True
