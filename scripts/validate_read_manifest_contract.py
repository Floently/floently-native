#!/usr/bin/env python3
"""Validate cross-language ReadingManifest v1 fixture invariants.

This deliberately uses only the Python standard library so the contract check
does not depend on a package installer. JSON Schema is still the normative
shape; this script enforces the semantic invariants that JSON Schema alone
cannot express cleanly.
"""

from __future__ import annotations

import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT / "shared/api-contracts/read/fixtures/reading-manifest-v1.sample.json"
UNICODE_FIXTURE = ROOT / "shared/api-contracts/read/fixtures/unicode-scalar-offsets-v1.json"


def fail(message: str) -> None:
    raise AssertionError(message)


def main() -> int:
    manifest = json.loads(FIXTURE.read_text(encoding="utf-8"))

    if manifest.get("schemaVersion") != 1:
        fail("fixture must use ReadingManifest schemaVersion 1")

    segments = manifest.get("segments")
    if not isinstance(segments, list) or not segments:
        fail("fixture must contain segments")

    text_scalar_length = manifest["textScalarLength"]
    expected_index = 0
    expected_word_start = 0
    expected_logical_start = 0
    total_words = 0
    previous_scalar_end = 0

    for segment in segments:
        if segment["index"] != expected_index:
            fail(f"segment index {segment['index']} is not contiguous")

        scalar_start = segment["scalarStart"]
        scalar_end = segment["scalarEnd"]
        if not (0 <= scalar_start < scalar_end <= text_scalar_length):
            fail(f"invalid scalar range for segment {expected_index}")
        if scalar_start < previous_scalar_end:
            fail(f"overlapping scalar range for segment {expected_index}")

        if segment["wordStart"] != expected_word_start:
            fail(f"wordStart drift at segment {expected_index}")
        if segment["wordEnd"] - segment["wordStart"] != segment["wordCount"]:
            fail(f"word count/range mismatch at segment {expected_index}")

        if segment["logicalStartMs"] != expected_logical_start:
            fail(f"logical timeline gap/drift at segment {expected_index}")
        if segment["logicalEndMs"] < segment["logicalStartMs"]:
            fail(f"negative logical duration at segment {expected_index}")

        synthesis = segment.get("synthesis")
        audio = segment.get("audio")
        if synthesis:
            status = synthesis["status"]
            if status not in {"pending", "generating", "ready", "failed"}:
                fail(f"invalid synthesis status at segment {expected_index}")
            if status == "ready" and not audio:
                fail(f"ready segment {expected_index} has no audio asset")

        timings = segment.get("timings", [])
        prior_end_ms = 0
        for timing in timings:
            if not (
                scalar_start <= timing["scalarStart"]
                < timing["scalarEnd"]
                <= scalar_end
            ):
                fail(f"timing scalar range escapes segment {expected_index}")
            if timing["startMs"] < prior_end_ms:
                fail(f"timings are not monotonic at segment {expected_index}")
            if timing["endMs"] < timing["startMs"]:
                fail(f"negative timing duration at segment {expected_index}")
            prior_end_ms = timing["endMs"]

        total_words += segment["wordCount"]
        expected_word_start = segment["wordEnd"]
        expected_logical_start = segment["logicalEndMs"]
        previous_scalar_end = scalar_end
        expected_index += 1

    if total_words != manifest["wordCount"]:
        fail("document wordCount does not equal segment wordCount sum")
    if expected_logical_start != manifest["estimatedSourceDurationMs"]:
        fail("document estimated duration does not equal final logical end")

    unicode_fixture = json.loads(
        UNICODE_FIXTURE.read_text(encoding="utf-8")
    )
    if unicode_fixture.get("schemaVersion") != 1:
        fail("unicode scalar fixture must use schemaVersion 1")

    for case in unicode_fixture.get("cases", []):
        text = case["text"]
        scalar_length = len(text)
        utf16_length = len(text.encode("utf-16-le")) // 2

        if scalar_length != case["scalarLength"]:
            fail(
                f"unicode scalar length mismatch for {case['id']}: "
                f"expected {case['scalarLength']}, got {scalar_length}"
            )
        if utf16_length != case["utf16Length"]:
            fail(
                f"utf16 length mismatch for {case['id']}: "
                f"expected {case['utf16Length']}, got {utf16_length}"
            )

    print(
        f"ReadingManifest fixture valid: {len(segments)} segments, "
        f"{total_words} words, {manifest['estimatedSourceDurationMs']} ms; "
        f"{len(unicode_fixture.get('cases', []))} Unicode scalar cases"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, KeyError, TypeError, ValueError) as error:
        print(f"ReadingManifest validation failed: {error}", file=sys.stderr)
        raise SystemExit(1)
