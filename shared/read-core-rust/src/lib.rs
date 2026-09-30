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
    pub char_start: usize,
    pub char_end: usize,
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
    pub text_length: usize,
    pub estimated_source_duration_ms: u64,
    pub segments: Vec<ReadingSegment>,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct SegmentPosition {
    pub index: usize,
    pub fraction: f64,
}

const DEFAULT_WORDS_PER_MINUTE: f64 = 170.0;

pub fn build_manifest(
    document_id: impl Into<String>,
    revision_id: impl Into<String>,
    title: impl Into<String>,
    language: impl Into<String>,
    text: &str,
    max_chars: usize,
) -> ReadingManifest {
    let normalized = text.replace("\r\n", "\n").trim().to_string();
    let pieces = split_segments(&normalized, max_chars.max(600));

    let mut char_cursor = 0usize;
    let mut word_cursor = 0usize;
    let mut logical_cursor_ms = 0u64;
    let mut segments = Vec::with_capacity(pieces.len());

    for (index, segment_text) in pieces.into_iter().enumerate() {
        let word_count = count_words(&segment_text);
        let duration_ms = estimate_duration_ms(word_count);
        let char_start = char_cursor;
        let word_start = word_cursor;

        char_cursor += segment_text.chars().count();
        word_cursor += word_count;

        let logical_start_ms = logical_cursor_ms;
        logical_cursor_ms = logical_cursor_ms.saturating_add(duration_ms);

        segments.push(ReadingSegment {
            id: format!("segment-{index}"),
            index,
            text: segment_text,
            char_start,
            char_end: char_cursor,
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
        text_length: char_cursor,
        estimated_source_duration_ms: logical_cursor_ms,
        segments,
    }
}

pub fn position_for_progress(manifest: &ReadingManifest, progress: f64) -> SegmentPosition {
    if manifest.segments.is_empty() || manifest.text_length == 0 {
        return SegmentPosition { index: 0, fraction: 0.0 };
    }

    let bounded = progress.clamp(0.0, 1.0);
    let target = bounded * manifest.text_length as f64;

    for segment in &manifest.segments {
        let length = segment.char_end.saturating_sub(segment.char_start).max(1);
        if target < segment.char_end as f64 || segment.index + 1 == manifest.segments.len() {
            let local = (target - segment.char_start as f64) / length as f64;
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
    if manifest.text_length == 0 {
        return 0.0;
    }
    let Some(segment) = manifest.segments.get(index) else {
        return 0.0;
    };
    let length = segment.char_end.saturating_sub(segment.char_start).max(1) as f64;
    let logical_chars = segment.char_start as f64 + length * fraction.clamp(0.0, 1.0);
    (logical_chars / manifest.text_length as f64).clamp(0.0, 1.0)
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
        if target < segment.logical_end_ms || segment.index + 1 == manifest.segments.len() {
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

fn split_segments(text: &str, max_chars: usize) -> Vec<String> {
    if text.is_empty() {
        return Vec::new();
    }

    let mut units = Vec::<String>::new();
    let mut current = String::new();

    for ch in text.chars() {
        current.push(ch);
        let sentence_boundary = matches!(ch, '.' | '!' | '?' | '\n');
        if sentence_boundary {
            let value = current.trim().to_string();
            if !value.is_empty() {
                units.push(value);
            }
            current.clear();
        }
    }

    let tail = current.trim();
    if !tail.is_empty() {
        units.push(tail.to_string());
    }

    let mut segments = Vec::new();
    let mut pending = String::new();

    for mut unit in units {
        while unit.chars().count() > max_chars {
            flush_pending(&mut pending, &mut segments);
            let (head, tail) = split_at_char_boundary(&unit, max_chars);
            segments.push(head.trim().to_string());
            unit = tail.trim().to_string();
        }

        if unit.is_empty() {
            continue;
        }

        let candidate_len = pending.chars().count()
            + if pending.is_empty() { 0 } else { 1 }
            + unit.chars().count();

        if candidate_len > max_chars {
            flush_pending(&mut pending, &mut segments);
        }

        if !pending.is_empty() {
            pending.push(' ');
        }
        pending.push_str(&unit);
    }

    flush_pending(&mut pending, &mut segments);
    segments
}

fn flush_pending(pending: &mut String, segments: &mut Vec<String>) {
    let value = pending.trim();
    if !value.is_empty() {
        segments.push(value.to_string());
    }
    pending.clear();
}

fn split_at_char_boundary(value: &str, max_chars: usize) -> (String, String) {
    let chars: Vec<char> = value.chars().collect();
    let mut cut = max_chars.min(chars.len());

    let floor = max_chars.saturating_mul(3) / 5;
    for i in (floor..cut).rev() {
        if chars[i].is_whitespace() {
            cut = i;
            break;
        }
    }

    let head: String = chars[..cut].iter().collect();
    let tail: String = chars[cut..].iter().collect();
    (head, tail)
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
        assert_eq!(manifest.segments.first().unwrap().logical_start_ms, 0);
        assert_eq!(
            manifest.segments.last().unwrap().logical_end_ms,
            manifest.estimated_source_duration_ms
        );
    }

    #[test]
    fn maps_progress_to_segment_and_back() {
        let text = (0..2000).map(|_| "word").collect::<Vec<_>>().join(" ");
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);
        assert!(manifest.segments.len() > 1);

        let position = position_for_progress(&manifest, 0.63);
        let roundtrip = progress_for_position(&manifest, position.index, position.fraction);
        assert!((roundtrip - 0.63).abs() < 0.01);
    }

    #[test]
    fn maps_document_time_without_exposing_segment_time() {
        let text = (0..3000).map(|_| "word").collect::<Vec<_>>().join(" ");
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);
        let target = manifest.estimated_source_duration_ms / 2;
        let (_, local) = segment_for_logical_time(&manifest, target).unwrap();
        assert!(local <= target);
    }

    #[test]
    fn prefetches_by_time_horizon() {
        let text = (0..5000).map(|_| "word").collect::<Vec<_>>().join(" ");
        let manifest = build_manifest("d1", "r1", "Book", "en", &text, 600);
        let indexes = prefetch_indexes(&manifest, 0, 120_000, 4);
        assert!(!indexes.is_empty());
        assert!(indexes.len() <= 4);
        assert_eq!(indexes[0], 1);
    }
}
