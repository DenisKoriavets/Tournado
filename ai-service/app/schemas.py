"""Pydantic models for the internal Core <-> AI contract (docs/contracts/api-contracts.md §4)."""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

AnalysisStatus = Literal["SUCCESS", "UNRECOGNIZED"]
TeamSlot = Literal["TEAM1", "TEAM2"]


class RosterEntry(BaseModel):
    """One locked-roster player as sent by the core (display name + in-game handle)."""

    model_config = ConfigDict(extra="ignore")

    displayName: str = Field(min_length=1, max_length=100)
    gameIdentifier: str | None = Field(default=None, max_length=100)


class RecognizedScore(BaseModel):
    team1: int = Field(ge=0)
    team2: int = Field(ge=0)


class NicknameCheck(BaseModel):
    """US-33: one nickname read from the screenshot, compared to the locked rosters."""

    nickname: str
    side: TeamSlot | None = Field(description="Team whose scoreboard side the nickname appeared on")
    inRoster: bool = Field(description="Nickname matches the locked roster of the team on whose side it appeared")
    matchedTeam: TeamSlot | None = Field(description="Team whose roster contains this nickname, if any")
    matchedRosterName: str | None = None


class AnalyzeDisputeResponse(BaseModel):
    status: AnalysisStatus
    recognizedScore: RecognizedScore | None
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    nicknamesDetected: list[str]
    nicknameChecks: list[NicknameCheck]
    allRosterNicknamesFound: bool
    reason: str | None = Field(default=None, description="Machine-readable reason when status is UNRECOGNIZED")
    provider: str
    processingTimeMs: int


# ---------------------------------------------------------------------------
# Provider output. The vision model's JSON is parsed with this strict schema; anything
# that does not validate is treated as "not recognised" (ADR-0006, NFR-22).
# ---------------------------------------------------------------------------


class ScoreboardSide(BaseModel):
    model_config = ConfigDict(extra="ignore")

    label: str | None = Field(default=None, max_length=100)
    score: int | None = Field(default=None, ge=0, le=999)
    players: list[str] = Field(default_factory=list, max_length=12)


class ScoreboardExtraction(BaseModel):
    model_config = ConfigDict(extra="ignore")

    readable: bool = True
    sides: list[ScoreboardSide] = Field(default_factory=list, max_length=4)
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)
