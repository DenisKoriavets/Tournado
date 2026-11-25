"""Dispute screenshot analysis pipeline (US-32, US-33).

1. Validate and normalise the image (Pillow) - anything that is not a real image is "not recognised".
2. Ask the vision provider for a raw extraction: two sides, each with a score and nicknames.
3. Deterministically map sides to team1/team2 by matching nicknames against the locked rosters (ASM-22).
   The model never sees the rosters, so it cannot be steered into "finding" roster names.
4. Compare every detected nickname with the roster of the team on whose side it appeared (US-33).

Every failure degrades to ``status = UNRECOGNIZED`` with HTTP 200 (NFR-19, CLAUDE.md §8).
"""

import io
import logging
import time
import unicodedata
from dataclasses import dataclass
from difflib import SequenceMatcher

from PIL import Image, UnidentifiedImageError

from app.config import Settings
from app.schemas import (
    AnalyzeDisputeResponse,
    NicknameCheck,
    RecognizedScore,
    RosterEntry,
    ScoreboardExtraction,
    ScoreboardSide,
)
from app.services.providers import ProviderError, VisionProvider

LOG = logging.getLogger(__name__)

_SUPPORTED_FORMATS = {"PNG": "image/png", "JPEG": "image/jpeg", "WEBP": "image/webp"}
# Groq accepts base64 images up to 4 MB; keep a margin for the base64 overhead (+33%).
_MAX_PROVIDER_BYTES = 2_900_000
_MAX_PROVIDER_SIDE_PX = 2560
_FUZZY_THRESHOLD = 0.85
_MIN_CONTAINMENT_LEN = 4

TEAM1, TEAM2 = "TEAM1", "TEAM2"


@dataclass(frozen=True)
class PreparedImage:
    data: bytes
    mime_type: str


class ImageRejected(Exception):
    pass


def prepare_image(raw: bytes, max_bytes: int) -> PreparedImage:
    """Validate the upload is a real PNG/JPEG/WEBP image; downscale/re-encode if too big for the provider."""
    if not raw:
        raise ImageRejected("EMPTY_IMAGE")
    if len(raw) > max_bytes:
        raise ImageRejected("IMAGE_TOO_LARGE")
    try:
        with Image.open(io.BytesIO(raw)) as probe:
            probe.verify()
        image = Image.open(io.BytesIO(raw))
        image.load()
    except (UnidentifiedImageError, OSError, SyntaxError, ValueError) as exc:
        raise ImageRejected("INVALID_IMAGE") from exc

    image_format = (image.format or "").upper()
    if image_format not in _SUPPORTED_FORMATS:
        raise ImageRejected("UNSUPPORTED_IMAGE_FORMAT")

    if len(raw) <= _MAX_PROVIDER_BYTES and max(image.size) <= _MAX_PROVIDER_SIDE_PX:
        return PreparedImage(raw, _SUPPORTED_FORMATS[image_format])

    image = image.convert("RGB")
    image.thumbnail((_MAX_PROVIDER_SIDE_PX, _MAX_PROVIDER_SIDE_PX))
    quality = 90
    while True:
        buffer = io.BytesIO()
        image.save(buffer, format="JPEG", quality=quality)
        if buffer.tell() <= _MAX_PROVIDER_BYTES or quality <= 50:
            return PreparedImage(buffer.getvalue(), "image/jpeg")
        quality -= 10


def normalize_nickname(value: str | None) -> str:
    """Case-insensitive, punctuation/whitespace-insensitive form used for comparison."""
    if not value:
        return ""
    folded = unicodedata.normalize("NFKC", value).casefold()
    return "".join(ch for ch in folded if ch.isalnum())


def _similarity(nickname: str, candidate: str) -> float:
    if not nickname or not candidate:
        return 0.0
    if nickname == candidate:
        return 1.0
    shorter, longer = sorted((nickname, candidate), key=len)
    # Clan tags / prefixes on the scoreboard, e.g. "[KMA] CyberHawk" vs roster "CyberHawk".
    if len(shorter) >= _MIN_CONTAINMENT_LEN and shorter in longer:
        return 0.9
    return SequenceMatcher(None, nickname, candidate).ratio()


@dataclass(frozen=True)
class RosterMatch:
    team: str
    roster_name: str
    score: float


def best_roster_match(nickname: str, rosters: dict[str, list[RosterEntry]]) -> RosterMatch | None:
    normalized = normalize_nickname(nickname)
    best: RosterMatch | None = None
    for team, roster in rosters.items():
        for entry in roster:
            for candidate in (entry.displayName, entry.gameIdentifier):
                score = _similarity(normalized, normalize_nickname(candidate))
                if score >= _FUZZY_THRESHOLD and (best is None or score > best.score):
                    best = RosterMatch(team=team, roster_name=entry.displayName, score=score)
    return best


def _count_team_matches(side: ScoreboardSide, team: str, rosters: dict[str, list[RosterEntry]]) -> int:
    count = 0
    for nickname in side.players:
        match = best_roster_match(nickname, rosters)
        if match is not None and match.team == team:
            count += 1
    return count


def assign_sides(
    sides: list[ScoreboardSide],
    team1_name: str,
    team2_name: str,
    rosters: dict[str, list[RosterEntry]],
) -> tuple[ScoreboardSide, ScoreboardSide] | None:
    """Return (team1_side, team2_side) or None if the side of each team cannot be determined (ASM-22)."""
    first, second = sides
    straight = _count_team_matches(first, TEAM1, rosters) + _count_team_matches(second, TEAM2, rosters)
    swapped = _count_team_matches(second, TEAM1, rosters) + _count_team_matches(first, TEAM2, rosters)
    if straight > swapped:
        return first, second
    if swapped > straight:
        return second, first

    # No roster evidence either way: fall back to team names displayed as side labels.
    def label_score(side: ScoreboardSide, team_name: str) -> float:
        return _similarity(normalize_nickname(side.label), normalize_nickname(team_name))

    straight_label = label_score(first, team1_name) + label_score(second, team2_name)
    swapped_label = label_score(second, team1_name) + label_score(first, team2_name)
    if straight_label > swapped_label and straight_label >= _FUZZY_THRESHOLD:
        return first, second
    if swapped_label > straight_label and swapped_label >= _FUZZY_THRESHOLD:
        return second, first
    return None


def build_nickname_checks(
    sides_by_team: dict[str, ScoreboardSide] | None,
    extraction: ScoreboardExtraction,
    rosters: dict[str, list[RosterEntry]],
) -> list[NicknameCheck]:
    if sides_by_team is not None:
        placed = [(team, nickname) for team, side in sides_by_team.items() for nickname in side.players]
    else:
        placed = [(None, nickname) for side in extraction.sides for nickname in side.players]

    checks: list[NicknameCheck] = []
    for side_team, nickname in placed:
        cleaned = nickname.strip()
        if not cleaned:
            continue
        match = best_roster_match(cleaned, rosters)
        matched_team = match.team if match else None
        in_roster = match is not None and (side_team is None or matched_team == side_team)
        checks.append(
            NicknameCheck(
                nickname=cleaned,
                side=side_team,
                inRoster=in_roster,
                matchedTeam=matched_team,
                matchedRosterName=match.roster_name if match else None,
            )
        )
    return checks


def _unrecognized(
    reason: str,
    provider: str,
    started: float,
    checks: list[NicknameCheck] | None = None,
    confidence: float | None = None,
) -> AnalyzeDisputeResponse:
    checks = checks or []
    return AnalyzeDisputeResponse(
        status="UNRECOGNIZED",
        recognizedScore=None,
        confidence=confidence,
        nicknamesDetected=[check.nickname for check in checks],
        nicknameChecks=checks,
        allRosterNicknamesFound=bool(checks) and all(check.inRoster for check in checks),
        reason=reason,
        provider=provider,
        processingTimeMs=int((time.perf_counter() - started) * 1000),
    )


async def analyze_dispute_screenshot(
    *,
    raw_image: bytes,
    discipline: str,
    team1_name: str,
    team2_name: str,
    team1_roster: list[RosterEntry],
    team2_roster: list[RosterEntry],
    provider: VisionProvider,
    settings: Settings,
) -> AnalyzeDisputeResponse:
    started = time.perf_counter()

    try:
        image = prepare_image(raw_image, settings.max_image_bytes)
    except ImageRejected as exc:
        return _unrecognized(str(exc), provider.name, started)

    try:
        extraction = await provider.extract_scoreboard(
            image.data, image.mime_type, discipline, team1_name, team2_name, team1_roster, team2_roster
        )
    except ProviderError as exc:
        LOG.warning("Vision provider '%s' failed: %s", provider.name, exc.reason)
        return _unrecognized(exc.reason, provider.name, started)
    except Exception:  # noqa: BLE001 - the AI must never block a dispute (NFR-19)
        LOG.exception("Unexpected vision provider failure")
        return _unrecognized("PROVIDER_ERROR", provider.name, started)

    rosters = {TEAM1: team1_roster, TEAM2: team2_roster}

    if not extraction.readable or len(extraction.sides) != 2:
        checks = build_nickname_checks(None, extraction, rosters)
        return _unrecognized("SCOREBOARD_NOT_READABLE", provider.name, started, checks, extraction.confidence)

    assignment = assign_sides(extraction.sides, team1_name, team2_name, rosters)
    if assignment is None:
        checks = build_nickname_checks(None, extraction, rosters)
        return _unrecognized("TEAM_SIDES_UNDETERMINED", provider.name, started, checks, extraction.confidence)

    team1_side, team2_side = assignment
    checks = build_nickname_checks({TEAM1: team1_side, TEAM2: team2_side}, extraction, rosters)

    if team1_side.score is None or team2_side.score is None:
        return _unrecognized("SCORE_NOT_READABLE", provider.name, started, checks, extraction.confidence)
    if extraction.confidence < settings.min_confidence:
        return _unrecognized("LOW_CONFIDENCE", provider.name, started, checks, extraction.confidence)

    return AnalyzeDisputeResponse(
        status="SUCCESS",
        recognizedScore=RecognizedScore(team1=team1_side.score, team2=team2_side.score),
        confidence=round(extraction.confidence, 3),
        nicknamesDetected=[check.nickname for check in checks],
        nicknameChecks=checks,
        allRosterNicknamesFound=bool(checks) and all(check.inRoster for check in checks),
        reason=None,
        provider=provider.name,
        processingTimeMs=int((time.perf_counter() - started) * 1000),
    )
