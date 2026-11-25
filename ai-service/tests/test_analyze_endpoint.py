import json

from fastapi.testclient import TestClient

from app.main import app
from tests.conftest import CT_ROSTER, TERRORIST_ROSTER

client = TestClient(app)
URL = "/api/v1/ai/analyze-dispute"


def _form(team1_roster=None, team2_roster=None):
    return {
        "discipline": "CS2",
        "team1_name": "QA Terror Squad",
        "team2_name": "QA Vegayta Five",
        "team1_roster": json.dumps(team1_roster if team1_roster is not None else TERRORIST_ROSTER),
        "team2_roster": json.dumps(team2_roster if team2_roster is not None else CT_ROSTER),
    }


def test_mock_provider_returns_contract_shaped_success(png_bytes):
    response = client.post(URL, data=_form(), files={"screenshot": ("shot.png", png_bytes, "image/png")})

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "SUCCESS"
    assert body["recognizedScore"] == {"team1": 13, "team2": 9}
    assert body["confidence"] == 0.985
    assert body["allRosterNicknamesFound"] is True
    assert body["nicknamesDetected"][:2] == ["BOT Mayer", "BOT Bassey"]
    assert {check["side"] for check in body["nicknameChecks"]} == {"TEAM1", "TEAM2"}
    assert body["provider"] == "mock"
    assert isinstance(body["processingTimeMs"], int)


def test_plain_string_rosters_are_accepted(png_bytes):
    response = client.post(
        URL,
        data=_form(team1_roster=["Alpha"], team2_roster=["Bravo"]),
        files={"screenshot": ("shot.png", png_bytes, "image/png")},
    )

    assert response.status_code == 200
    assert response.json()["nicknamesDetected"] == ["Alpha", "Bravo"]


def test_non_image_upload_degrades_to_unrecognized():
    response = client.post(URL, data=_form(), files={"screenshot": ("doc.pdf", b"%PDF-1.7 not an image", "application/pdf")})

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UNRECOGNIZED"
    assert body["reason"] == "INVALID_IMAGE"
    assert body["recognizedScore"] is None


def test_invalid_roster_json_is_rejected(png_bytes):
    form = _form()
    form["team1_roster"] = "{not json"

    response = client.post(URL, data=form, files={"screenshot": ("shot.png", png_bytes, "image/png")})

    assert response.status_code == 422


def test_missing_token_is_rejected_when_token_configured(monkeypatch, png_bytes):
    monkeypatch.setenv("AI_SERVICE_API_TOKEN", "internal-secret")

    response = client.post(URL, data=_form(), files={"screenshot": ("shot.png", png_bytes, "image/png")})

    assert response.status_code == 401


def test_wrong_token_is_rejected(monkeypatch, png_bytes):
    monkeypatch.setenv("AI_SERVICE_API_TOKEN", "internal-secret")

    response = client.post(
        URL,
        data=_form(),
        files={"screenshot": ("shot.png", png_bytes, "image/png")},
        headers={"Authorization": "Bearer nope"},
    )

    assert response.status_code == 401


def test_valid_token_is_accepted(monkeypatch, png_bytes):
    monkeypatch.setenv("AI_SERVICE_API_TOKEN", "internal-secret")

    response = client.post(
        URL,
        data=_form(),
        files={"screenshot": ("shot.png", png_bytes, "image/png")},
        headers={"Authorization": "Bearer internal-secret"},
    )

    assert response.status_code == 200
    assert response.json()["status"] == "SUCCESS"


def test_health_does_not_require_token(monkeypatch):
    monkeypatch.setenv("AI_SERVICE_API_TOKEN", "internal-secret")

    assert client.get("/api/v1/ai/health").status_code == 200


def test_groq_without_key_degrades_to_unrecognized(monkeypatch, png_bytes):
    monkeypatch.setenv("AI_PROVIDER", "groq")

    response = client.post(URL, data=_form(), files={"screenshot": ("shot.png", png_bytes, "image/png")})

    assert response.status_code == 200
    assert response.json()["status"] == "UNRECOGNIZED"
    assert response.json()["reason"] == "PROVIDER_NOT_CONFIGURED"
