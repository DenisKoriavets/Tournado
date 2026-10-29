import os
from datetime import datetime, timezone
from fastapi import FastAPI, File, Form, UploadFile

app = FastAPI(title="Tournado AI Arbitration Service", version="0.1.0")

@app.get("/api/v1/ai/health")
async def health():
    return {
        "status": "UP",
        "service": "tournado-ai-service",
        "provider": os.getenv("AI_PROVIDER", "mock"),
        "timestamp": datetime.now(timezone.utc).isoformat()
    }

@app.post("/api/v1/ai/analyze-dispute")
async def analyze_dispute(
    screenshot: UploadFile = File(...),
    discipline: str = Form("CS2"),
    team1_name: str = Form("Team 1"),
    team2_name: str = Form("Team 2"),
    team1_roster: str = Form("[]"),
    team2_roster: str = Form("[]")
):
    return {
        "status": "SUCCESS",
        "recognizedScore": {
            "team1": 13,
            "team2": 9
        },
        "confidence": 0.985,
        "nicknamesDetected": ["CyberHawk", "s1mple_kma", "b1t_junior", "electro", "perfecto"],
        "allRosterNicknamesFound": True,
        "processingTimeMs": 120
    }