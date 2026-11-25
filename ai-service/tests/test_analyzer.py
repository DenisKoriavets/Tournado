import pytest

from app.config import get_settings
from app.schemas import RosterEntry, ScoreboardExtraction, ScoreboardSide
from app.services.analyzer import analyze_dispute_screenshot, normalize_nickname, prepare_image, ImageRejected
from app.services.providers import ProviderError
from tests.conftest import CT_ROSTER, TERRORIST_ROSTER, make_png_bytes

TEAM1 = [RosterEntry(**entry) for entry in TERRORIST_ROSTER]
TEAM2 = [RosterEntry(**entry) for entry in CT_ROSTER]

CT_SIDE = ScoreboardSide(
    label="COUNTER-TERRORISTS",
    score=1,
    players=["BOT Ulysses", "BOT Efe", "Vegayta", "BOT Panama", "BOT Dios"],
)
T_SIDE = ScoreboardSide(
    label="TERRORISTS",
    score=0,
    players=["BOT Mayer", "BOT Bassey", "BOT Colin", "BOT Yuri", "BOT Hastings"],
)


class StubProvider:
    name = "stub"

    def __init__(self, extraction=None, error=None):
        self._extraction = extraction
        self._error = error

    async def extract_scoreboard(self, *args, **kwargs):
        if self._error:
            raise self._error
        return self._extraction


async def _analyze(provider, team1=TEAM1, team2=TEAM2, image=None):
    return await analyze_dispute_screenshot(
        raw_image=image or make_png_bytes(),
        discipline="CS2",
        team1_name="QA Terror Squad",
        team2_name="QA Vegayta Five",
        team1_roster=team1,
        team2_roster=team2,
        provider=provider,
        settings=get_settings(),
    )


async def test_sides_are_mapped_to_teams_by_roster_even_when_scoreboard_order_differs():
    provider = StubProvider(ScoreboardExtraction(sides=[CT_SIDE, T_SIDE], confidence=0.97))

    result = await _analyze(provider)

    assert result.status == "SUCCESS"
    assert result.recognizedScore.model_dump() == {"team1": 0, "team2": 1}
    assert result.allRosterNicknamesFound is True
    assert result.confidence == 0.97


async def test_unknown_nickname_is_flagged_as_possible_ringer():
    ringer_side = T_SIDE.model_copy(update={"players": ["BOT Mayer", "BOT Bassey", "BOT Colin", "BOT Yuri", "xX_Ringer_Xx"]})
    provider = StubProvider(ScoreboardExtraction(sides=[CT_SIDE, ringer_side], confidence=0.9))

    result = await _analyze(provider)

    assert result.status == "SUCCESS"
    assert result.allRosterNicknamesFound is False
    ringer = next(check for check in result.nicknameChecks if check.nickname == "xX_Ringer_Xx")
    assert ringer.inRoster is False
    assert ringer.matchedTeam is None
    assert ringer.side == "TEAM1"


async def test_opponent_player_on_wrong_side_is_not_counted_as_in_roster():
    mixed_side = T_SIDE.model_copy(update={"players": ["BOT Mayer", "BOT Bassey", "BOT Colin", "BOT Yuri", "Vegayta"]})
    ct_side = CT_SIDE.model_copy(update={"players": ["BOT Ulysses", "BOT Efe", "BOT Panama", "BOT Dios"]})
    provider = StubProvider(ScoreboardExtraction(sides=[ct_side, mixed_side], confidence=0.9))

    result = await _analyze(provider)

    swapped = next(check for check in result.nicknameChecks if check.nickname == "Vegayta")
    assert swapped.inRoster is False
    assert swapped.matchedTeam == "TEAM2"
    assert result.allRosterNicknamesFound is False


async def test_nicknames_are_matched_case_and_punctuation_insensitively_with_ocr_tolerance():
    side = T_SIDE.model_copy(update={"players": ["bot mayer", "BOT_Bassey", "[KMA] BOT Colin", "BOT Yurl", "BOT Hastings"]})
    provider = StubProvider(ScoreboardExtraction(sides=[side, CT_SIDE], confidence=0.9))

    result = await _analyze(provider)

    assert result.allRosterNicknamesFound is True


async def test_sides_that_cannot_be_attributed_to_teams_are_unrecognized():
    unknown_a = ScoreboardSide(label="BLUE", score=2, players=["Stranger1"])
    unknown_b = ScoreboardSide(label="RED", score=1, players=["Stranger2"])
    provider = StubProvider(ScoreboardExtraction(sides=[unknown_a, unknown_b], confidence=0.99))

    result = await _analyze(provider)

    assert result.status == "UNRECOGNIZED"
    assert result.reason == "TEAM_SIDES_UNDETERMINED"
    assert result.recognizedScore is None
    assert result.nicknamesDetected == ["Stranger1", "Stranger2"]
    assert result.allRosterNicknamesFound is False


async def test_team_name_labels_are_used_when_no_roster_nickname_matches():
    side_a = ScoreboardSide(label="QA Vegayta Five", score=16, players=["Stranger1"])
    side_b = ScoreboardSide(label="QA Terror Squad", score=14, players=["Stranger2"])
    provider = StubProvider(ScoreboardExtraction(sides=[side_a, side_b], confidence=0.9))

    result = await _analyze(provider)

    assert result.status == "SUCCESS"
    assert result.recognizedScore.model_dump() == {"team1": 14, "team2": 16}


async def test_unreadable_score_is_unrecognized():
    provider = StubProvider(ScoreboardExtraction(sides=[CT_SIDE.model_copy(update={"score": None}), T_SIDE], confidence=0.9))

    result = await _analyze(provider)

    assert result.status == "UNRECOGNIZED"
    assert result.reason == "SCORE_NOT_READABLE"
    assert len(result.nicknameChecks) == 10


async def test_low_confidence_is_unrecognized():
    provider = StubProvider(ScoreboardExtraction(sides=[CT_SIDE, T_SIDE], confidence=0.2))

    result = await _analyze(provider)

    assert result.status == "UNRECOGNIZED"
    assert result.reason == "LOW_CONFIDENCE"
    assert result.confidence == 0.2


async def test_not_a_scoreboard_is_unrecognized():
    provider = StubProvider(ScoreboardExtraction(readable=False, sides=[], confidence=0.1))

    result = await _analyze(provider)

    assert result.status == "UNRECOGNIZED"
    assert result.reason == "SCOREBOARD_NOT_READABLE"


async def test_provider_error_degrades_to_unrecognized():
    provider = StubProvider(error=ProviderError("PROVIDER_UNAVAILABLE", "timeout"))

    result = await _analyze(provider)

    assert result.status == "UNRECOGNIZED"
    assert result.reason == "PROVIDER_UNAVAILABLE"


async def test_unexpected_provider_exception_degrades_to_unrecognized():
    provider = StubProvider(error=RuntimeError("boom"))

    result = await _analyze(provider)

    assert result.status == "UNRECOGNIZED"
    assert result.reason == "PROVIDER_ERROR"


def test_prepare_image_rejects_garbage_and_empty_payloads():
    with pytest.raises(ImageRejected, match="INVALID_IMAGE"):
        prepare_image(b"definitely not an image", 10_000_000)
    with pytest.raises(ImageRejected, match="EMPTY_IMAGE"):
        prepare_image(b"", 10_000_000)


def test_prepare_image_enforces_size_limit():
    with pytest.raises(ImageRejected, match="IMAGE_TOO_LARGE"):
        prepare_image(make_png_bytes(), 10)


def test_prepare_image_downscales_oversized_images():
    prepared = prepare_image(make_png_bytes((4000, 3000)), 50_000_000)

    assert prepared.mime_type == "image/jpeg"


def test_prepare_image_keeps_small_jpeg_as_is(cs2_screenshot_bytes):
    prepared = prepare_image(cs2_screenshot_bytes, 10_000_000)

    assert prepared.mime_type == "image/jpeg"
    assert prepared.data == cs2_screenshot_bytes


def test_normalize_nickname():
    assert normalize_nickname("  [KMA] Cyber_Hawk ") == "kmacyberhawk"
    assert normalize_nickname(None) == ""
