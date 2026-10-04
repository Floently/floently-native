//! Floently Read shared document core.
//!
//! This crate deliberately contains no platform UI or media-session code.
//! It owns deterministic document segmentation and logical position mapping
//! so Swift/Kotlin implementations can share the same semantics.

#[derive(Debug, Clone, PartialEq)]
pub struct ReadingSegment {
    pub id: String,
    pub index: usize,
    pub text: String,
    pub scalar_start: usize,
    pub scalar_end: usize,
    pub word_start: usize,
    pub word_end: usize,
    pub word_count: usize,
    pub estimated_source_duration_ms: u64,
    pub logical_start_ms: u64,
    pub logical_end_ms: u64,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ReadingManifest {
    pub schema_version: u32,
    pub document_id: String,
    pub revision_id: String,
    pub title: String,
    pub language: String,
    pub word_count: usize,
    pub text_scalar_length: usize,
    pub estimated_source_duration_ms: u64,
    pub segments: Vec<ReadingSegment>,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct SegmentPosition {
    pub index: usize,
    pub fraction: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SourceAnchorResolution {
    pub scalar_start: usize,
    pub scalar_length: usize,
}

#[derive(Debug, Clone, PartialEq)]
struct SegmentSlice {
    text: String,
    scalar_start: usize,
    scalar_end: usize,
}

const DEFAULT_WORDS_PER_MINUTE: f64 = 170.0;

pub fn build_manifest(
    document_id: impl Into<String>,
    revision_id: impl Into<String>,
    title: impl Into<String>,
    language: impl Into<String>,
    text: &str,
    max_scalars: usize,
) -> ReadingManifest {
    let normalized = normalize_text(text);
    let text_scalar_length = normalized.chars().count();
    let pieces = split_segments(&normalized, max_scalars.max(600));

    let mut word_cursor = 0usize;
    let mut logical_cursor_ms = 0u64;
    let mut segments = Vec::with_capacity(pieces.len());

    for (index, slice) in pieces.into_iter().enumerate() {
        let word_count = count_words(&slice.text);
        let duration_ms = estimate_duration_ms(word_count);
        let word_start = word_cursor;

        word_cursor += word_count;
        let logical_start_ms = logical_cursor_ms;
        logical_cursor_ms = logical_cursor_ms.saturating_add(duration_ms);

        segments.push(ReadingSegment {
            id: format!("segment-{index}"),
            index,
            text: slice.text,
            scalar_start: slice.scalar_start,
            scalar_end: slice.scalar_end,
            word_start,
            word_end: word_cursor,
            word_count,
            estimated_source_duration_ms: duration_ms,
            logical_start_ms,
            logical_end_ms: logical_cursor_ms,
        });
    }

    ReadingManifest {
        schema_version: 1,
        document_id: document_id.into(),
        revision_id: revision_id.into(),
        title: title.into(),
        language: language.into(),
        word_count: word_cursor,
        text_scalar_length,
        estimated_source_duration_ms: logical_cursor_ms,
        segments,
    }
}

pub fn position_for_progress(manifest: &ReadingManifest, progress: f64) -> SegmentPosition {
    if manifest.segments.is_empty() || manifest.text_scalar_length == 0 {
        return SegmentPosition {
            index: 0,
            fraction: 0.0,
        };
    }

    let bounded = progress.clamp(0.0, 1.0);
    let target = bounded * manifest.text_scalar_length as f64;

    for segment in &manifest.segments {
        let length = segment.scalar_end.saturating_sub(segment.scalar_start).max(1);

        if target < segment.scalar_end as f64
            || segment.index + 1 == manifest.segments.len()
        {
            let local = (target - segment.scalar_start as f64) / length as f64;
            return SegmentPosition {
                index: segment.index,
                fraction: local.clamp(0.0, 1.0),
            };
        }
    }

    SegmentPosition {
        index: manifest.segments.len() - 1,
        fraction: 1.0,
    }
}

pub fn progress_for_position(
    manifest: &ReadingManifest,
    index: usize,
    fraction: f64,
) -> f64 {
    if manifest.text_scalar_length == 0 {
        return 0.0;
    }

    let Some(segment) = manifest.segments.get(index) else {
        return 0.0;
    };

    let length = segment.scalar_end.saturating_sub(segment.scalar_start).max(1) as f64;
    let logical_scalars =
        segment.scalar_start as f64 + length * fraction.clamp(0.0, 1.0);

    (logical_scalars / manifest.text_scalar_length as f64).clamp(0.0, 1.0)
}

pub fn logical_time_for_scalar(
    manifest: &ReadingManifest,
    scalar_offset: usize,
) -> Option<u64> {
    if manifest.segments.is_empty() {
        return None;
    }

    let target = scalar_offset.min(manifest.text_scalar_length);

    for segment in &manifest.segments {
        if target < segment.scalar_end
            || segment.index + 1 == manifest.segments.len()
        {
            let scalar_span = segment
                .scalar_end
                .saturating_sub(segment.scalar_start)
                .max(1);
            let local_scalars = target
                .saturating_sub(segment.scalar_start)
                .min(scalar_span);
            let fraction = local_scalars as f64 / scalar_span as f64;
            let logical_span = segment
                .logical_end_ms
                .saturating_sub(segment.logical_start_ms);

            return Some(
                segment.logical_start_ms.saturating_add(
                    (logical_span as f64 * fraction)
                        .round()
                        .max(0.0) as u64,
                ),
            );
        }
    }

    None
}

pub fn segment_for_logical_time(
    manifest: &ReadingManifest,
    elapsed_ms: u64,
) -> Option<(usize, u64)> {
    if manifest.segments.is_empty() {
        return None;
    }

    let target = elapsed_ms.min(manifest.estimated_source_duration_ms);

    for segment in &manifest.segments {
        if target < segment.logical_end_ms
            || segment.index + 1 == manifest.segments.len()
        {
            return Some((
                segment.index,
                target.saturating_sub(segment.logical_start_ms),
            ));
        }
    }

    None
}

pub fn prefetch_indexes(
    manifest: &ReadingManifest,
    active_index: usize,
    horizon_ms: u64,
    max_segments: usize,
) -> Vec<usize> {
    let mut indexes = Vec::new();
    let mut buffered_ms = 0u64;
    let limit = max_segments.max(1);
    let horizon = horizon_ms.max(30_000);

    for index in active_index.saturating_add(1)..manifest.segments.len() {
        if indexes.len() >= limit {
            break;
        }

        indexes.push(index);
        buffered_ms = buffered_ms.saturating_add(
            manifest.segments[index].estimated_source_duration_ms,
        );

        if buffered_ms >= horizon {
            break;
        }
    }

    indexes
}

/// Resolves a previously source-anchored quote inside a new raw source
/// revision. Exact quote identity is mandatory. Prefix/suffix context is used
/// only to disambiguate repeated exact quotes; ambiguous evidence fails
/// closed rather than attaching an annotation to the wrong passage.
pub fn resolve_source_anchor(
    source_text: &str,
    quote: &str,
    prefix_context: &str,
    suffix_context: &str,
) -> Option<SourceAnchorResolution> {
    if source_text.is_empty() || quote.is_empty() {
        return None;
    }

    let quote_scalar_length = quote.chars().count();
    if quote_scalar_length == 0
        || quote_scalar_length > source_text.chars().count()
    {
        return None;
    }

    let source_chars: Vec<char> = source_text.chars().collect();
    let prefix_chars: Vec<char> = prefix_context.chars().collect();
    let suffix_chars: Vec<char> = suffix_context.chars().collect();
    let mut candidates = Vec::<(usize, usize)>::new();

    for (scalar_start, (byte_start, _)) in
        source_text.char_indices().enumerate()
    {
        if !source_text[byte_start..].starts_with(quote) {
            continue;
        }

        let scalar_end = scalar_start + quote_scalar_length;
        if scalar_end > source_chars.len() {
            continue;
        }

        let prefix_start = scalar_start.saturating_sub(32);
        let suffix_end = scalar_end
            .saturating_add(32)
            .min(source_chars.len());

        let prefix_score = common_suffix_len(
            &prefix_chars,
            &source_chars[prefix_start..scalar_start],
        );
        let suffix_score = common_prefix_len(
            &suffix_chars,
            &source_chars[scalar_end..suffix_end],
        );

        candidates.push((
            scalar_start,
            prefix_score.saturating_add(suffix_score),
        ));

        if candidates.len() > 128 {
            return None;
        }
    }

    if candidates.len() == 1 {
        return Some(SourceAnchorResolution {
            scalar_start: candidates[0].0,
            scalar_length: quote_scalar_length,
        });
    }

    if candidates.is_empty() {
        return None;
    }

    candidates.sort_by(|left, right| {
        right
            .1
            .cmp(&left.1)
            .then_with(|| left.0.cmp(&right.0))
    });

    let best = candidates[0];
    let second = candidates[1];

    if best.1 < 8 || best.1 <= second.1 {
        return None;
    }

    Some(SourceAnchorResolution {
        scalar_start: best.0,
        scalar_length: quote_scalar_length,
    })
}

fn common_prefix_len(left: &[char], right: &[char]) -> usize {
    left.iter()
        .zip(right.iter())
        .take_while(|(lhs, rhs)| lhs == rhs)
        .count()
}

fn common_suffix_len(left: &[char], right: &[char]) -> usize {
    left.iter()
        .rev()
        .zip(right.iter().rev())
        .take_while(|(lhs, rhs)| lhs == rhs)
        .count()
}

fn normalize_text(text: &str) -> String {
    text.replace("\r\n", "\n").replace('\r', "\n").trim().to_string()
}

fn estimate_duration_ms(word_count: usize) -> u64 {
    let words = word_count.max(1) as f64;
    ((words / DEFAULT_WORDS_PER_MINUTE) * 60_000.0)
        .round()
        .max(600.0) as u64
}

fn count_words(text: &str) -> usize {
    text.split_whitespace()
        .filter(|part| part.chars().any(|c| c.is_alphanumeric()))
        .count()
}

fn split_segments(text: &str, max_scalars: usize) -> Vec<SegmentSlice> {
    if text.is_empty() {
        return Vec::new();
    }

    let chars: Vec<char> = text.chars().collect();
    let mut segments = Vec::new();
    let mut cursor = 0usize;

    while cursor < chars.len() {
        while cursor < chars.len() && chars[cursor].is_whitespace() {
            cursor += 1;
        }

        if cursor >= chars.len() {
            break;
        }

        let max_end = cursor.saturating_add(max_scalars).min(chars.len());
        let mut end = choose_segment_end(&chars, cursor, max_end, max_scalars);

        while end > cursor && chars[end - 1].is_whitespace() {
            end -= 1;
        }

        if end <= cursor {
            end = max_end.max(cursor + 1).min(chars.len());
        }

        let segment_text: String = chars[cursor..end].iter().collect();

        segments.push(SegmentSlice {
            text: segment_text,
            scalar_start: cursor,
            scalar_end: end,
        });

        cursor = end;
    }

    segments
}

fn choose_segment_end(
    chars: &[char],
    start: usize,
    max_end: usize,
    max_scalars: usize,
) -> usize {
    if max_end >= chars.len() {
        return chars.len();
    }

    let minimum_preferred =
        start + (max_scalars.saturating_mul(3) / 5).min(max_end.saturating_sub(start));

    let mut sentence_boundary = None;
    let mut whitespace_boundary = None;

    for index in start..max_end {
        let ch = chars[index];

        if index + 1 >= minimum_preferred {
            if matches!(ch, '.' | '!' | '?' | '\n') {
                sentence_boundary = Some(index + 1);
            }

            if ch.is_whitespace() {
                whitespace_boundary = Some(index);
            }
        }
    }

    sentence_boundary
        .or(whitespace_boundary)
        .unwrap_or(max_end)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn builds_one_logical_timeline_for_long_text() {
        let text = "One two three. Four five six seven. Eight nine ten.";
        let manifest = build_manifest("d1", "r1", "Book", "en", text, 600);

        assert_eq!(manifest.schema_version, 1);
        assert_eq!(manifest.word_count, 10);
        assert_eq!(
            manifest.text_scalar_length,
            normalize_text(text).chars().count()
        );
        assert_eq!(manifest.segments.first().unwrap().logical_start_ms, 0);
        assert_eq!(
            manifest.segments.last().unwrap().logical_end_ms,
            manifest.estimated_source_duration_ms
        );
    }

    #[test]
    fn segment_ranges_match_the_canonical_document() {
        let text = (0..900)
            .map(|index| format!("w{index}"))
            .collect::<Vec<_>>()
            .join(" ");
        let canonical = normalize_text(&text);
        let canonical_chars: Vec<char> = canonical.chars().collect();
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);

        assert!(manifest.segments.len() > 1);

        for segment in &manifest.segments {
            let slice: String = canonical_chars[segment.scalar_start..segment.scalar_end]
                .iter()
                .collect();
            assert_eq!(slice, segment.text);
        }
    }

    #[test]
    fn maps_progress_to_segment_and_back() {
        let text = (0..2000)
            .map(|_| "word")
            .collect::<Vec<_>>()
            .join(" ");
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);

        assert!(manifest.segments.len() > 1);

        let position = position_for_progress(&manifest, 0.63);
        let roundtrip =
            progress_for_position(&manifest, position.index, position.fraction);

        assert!((roundtrip - 0.63).abs() < 0.01);
    }

    #[test]
    fn maps_canonical_scalar_to_document_time() {
        let text = (0..3000)
            .map(|index| format!("word{index}"))
            .collect::<Vec<_>>()
            .join(" ");
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);

        let middle_scalar = manifest.text_scalar_length / 2;
        let middle_time = logical_time_for_scalar(&manifest, middle_scalar).unwrap();
        let end_time =
            logical_time_for_scalar(&manifest, manifest.text_scalar_length).unwrap();

        assert!(middle_time > 0);
        assert!(middle_time < manifest.estimated_source_duration_ms);
        assert_eq!(end_time, manifest.estimated_source_duration_ms);
    }

    #[test]
    fn maps_document_time_without_exposing_segment_time() {
        let text = (0..3000)
            .map(|_| "word")
            .collect::<Vec<_>>()
            .join(" ");
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);
        let target = manifest.estimated_source_duration_ms / 2;
        let (_, local) = segment_for_logical_time(&manifest, target).unwrap();

        assert!(local <= target);
    }

    #[test]
    fn prefetches_by_time_horizon() {
        let text = (0..5000)
            .map(|_| "word")
            .collect::<Vec<_>>()
            .join(" ");
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);
        let indexes = prefetch_indexes(&manifest, 0, 120_000, 4);

        assert!(!indexes.is_empty());
        assert!(indexes.len() <= 4);
        assert_eq!(indexes[0], 1);
    }

    #[test]
    fn counts_unicode_scalars_not_utf16_units_or_graphemes() {
        let cases = [
            ("Floently Read", 13usize),
            ("Hyvää päivää", 12usize),
            ("e\u{0301}", 2usize),
            ("🙂", 1usize),
            ("👩‍💻", 3usize),
            ("🇫🇮", 2usize),
            ("مرحبا", 5usize),
            ("日本語", 3usize),
            ("A🙂e\u{0301}日本", 6usize),
        ];

        for (text, expected_scalars) in cases {
            let manifest = build_manifest(
                "unicode",
                "rev-1",
                "Unicode",
                "und",
                text,
                600,
            );

            assert_eq!(
                manifest.text_scalar_length,
                expected_scalars,
                "wrong scalar length for {text:?}"
            );

            if let Some(segment) = manifest.segments.first() {
                assert_eq!(segment.scalar_start, 0);
                assert_eq!(segment.scalar_end, expected_scalars);
                assert_eq!(segment.text.chars().count(), expected_scalars);
            }
        }
    }

    #[test]
    fn resolves_unique_source_anchor_with_unicode_scalar_coordinates() {
        let source = "Intro 🙂. Hyvää päivää. Closing.";
        let quote = "Hyvää päivää";
        let resolution = resolve_source_anchor(
            source,
            quote,
            "Intro 🙂. ",
            ". Closing.",
        )
        .expect("unique source anchor");

        assert_eq!(
            resolution.scalar_start,
            "Intro 🙂. ".chars().count()
        );
        assert_eq!(
            resolution.scalar_length,
            quote.chars().count()
        );
    }

    #[test]
    fn resolves_repeated_quote_only_with_unique_context() {
        let source =
            "Chapter one says target phrase here. Later target phrase closes.";
        let quote = "target phrase";
        let resolution = resolve_source_anchor(
            source,
            quote,
            "one says ",
            " here. Later",
        )
        .expect("context should identify first quote");

        assert_eq!(
            resolution.scalar_start,
            "Chapter one says ".chars().count()
        );
    }

    #[test]
    fn refuses_ambiguous_repeated_source_anchor() {
        let source = "same quote / same quote";
        assert_eq!(
            resolve_source_anchor(
                source,
                "same quote",
                "",
                "",
            ),
            None
        );
    }

    #[test]
    fn refuses_missing_source_anchor() {
        assert_eq!(
            resolve_source_anchor(
                "new revision",
                "old quote",
                "before",
                "after",
            ),
            None
        );
    }

    #[test]
    fn normalizes_line_endings_before_indexing() {
        let manifest = build_manifest(
            "d1",
            "r1",
            "Book",
            "en",
            " First line.\r\nSecond line.\rThird line. ",
            600,
        );

        assert_eq!(
            manifest.text_scalar_length,
            "First line.\nSecond line.\nThird line.".chars().count()
        );
    }
}
