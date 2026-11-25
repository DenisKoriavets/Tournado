from httpx import ASGITransport, AsyncClient

from app.main import app


async def test_ai_health_endpoint():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        response = await ac.get("/api/v1/ai/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "tournado-ai-service"
    assert data["provider"] == "mock"


async def test_ai_health_reports_unconfigured_groq(monkeypatch):
    monkeypatch.setenv("AI_PROVIDER", "groq")
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        response = await ac.get("/api/v1/ai/health")
    assert response.json()["providerConfigured"] is False
