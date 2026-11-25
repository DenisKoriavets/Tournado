import hmac
import json
import logging
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Annotated

from fastapi import Depends, FastAPI, File, Form, HTTPException, Request, UploadFile, status
from pydantic import TypeAdapter, ValidationError

from app.config import Settings, get_settings
from app.schemas import AnalyzeDisputeResponse, RosterEntry
from app.services.analyzer import analyze_dispute_screenshot
from app.services.providers import VisionProvider, build_provider

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
LOG = logging.getLogger("tournado.ai")

_ROSTER_ADAPTER = TypeAdapter(list[RosterEntry | str])


@asynccontextmanager
async def lifespan(_: FastAPI):
    settings = get_settings()
    LOG.info("AI provider: %s (groq key configured: %s)", settings.provider, bool(settings.groq_api_key))
    if not settings.api_token:
        LOG.warning("AI_SERVICE_API_TOKEN is not set - the analyze endpoint is NOT protected")
    yield


app = FastAPI(title="Tournado AI Arbitration Service", version="0.2.0", lifespan=lifespan)


def get_provider(settings: Annotated[Settings, Depends(get_settings)]) -> VisionProvider:
    return build_provider(settings)


def require_api_token(request: Request, settings: Annotated[Settings, Depends(get_settings)]) -> None:
    """Single shared-token auth for the internal API (the service is not publicly exposed)."""
    if not settings.api_token:
        return
    header = request.headers.get("Authorization", "")
    scheme, _, token = header.partition(" ")
    if scheme.lower() != "bearer" or not hmac.compare_digest(token.strip(), settings.api_token):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid internal API token",
            headers={"WWW-Authenticate": "Bearer"},
        )


def parse_roster(raw: str, field_name: str) -> list[RosterEntry]:
    """Roster form field: JSON array of {displayName, gameIdentifier} objects (plain strings also accepted)."""
    try:
        items = _ROSTER_ADAPTER.validate_python(json.loads(raw or "[]"))
    except (json.JSONDecodeError, ValidationError) as exc:
        raise HTTPException(
            status_code=422,
            detail=f"'{field_name}' must be a JSON array of roster entries",
        ) from exc
    return [RosterEntry(displayName=item) if isinstance(item, str) else item for item in items]


@app.get("/api/v1/ai/health")
async def health(settings: Annotated[Settings, Depends(get_settings)]):
    return {
        "status": "UP",
        "service": "tournado-ai-service",
        "provider": settings.provider,
        "model": settings.groq_model if settings.provider == "groq" else None,
        "providerConfigured": settings.provider != "groq" or bool(settings.groq_api_key),
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


@app.post(
    "/api/v1/ai/analyze-dispute",
    response_model=AnalyzeDisputeResponse,
    dependencies=[Depends(require_api_token)],
)
async def analyze_dispute(
    settings: Annotated[Settings, Depends(get_settings)],
    provider: Annotated[VisionProvider, Depends(get_provider)],
    screenshot: UploadFile = File(...),
    discipline: str = Form("CS2"),
    team1_name: str = Form("Team 1"),
    team2_name: str = Form("Team 2"),
    team1_roster: str = Form("[]"),
    team2_roster: str = Form("[]"),
) -> AnalyzeDisputeResponse:
    roster1 = parse_roster(team1_roster, "team1_roster")
    roster2 = parse_roster(team2_roster, "team2_roster")
    raw_image = await screenshot.read(settings.max_image_bytes + 1)

    result = await analyze_dispute_screenshot(
        raw_image=raw_image,
        discipline=discipline,
        team1_name=team1_name,
        team2_name=team2_name,
        team1_roster=roster1,
        team2_roster=roster2,
        provider=provider,
        settings=settings,
    )
    LOG.info(
        "analyze-dispute discipline=%s provider=%s status=%s reason=%s confidence=%s time=%sms",
        discipline, result.provider, result.status, result.reason, result.confidence, result.processingTimeMs,
    )
    return result
