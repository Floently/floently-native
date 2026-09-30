use floently_read_core::{
    build_manifest,
    position_for_progress,
    prefetch_indexes,
    segment_for_logical_time,
    ReadingManifest,
    ReadingSegment,
};
use serde::{Deserialize, Serialize};
use wasm_bindgen::prelude::*;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct ManifestDto {
    schema_version: u32,
    document_id: String,
    revision_id: String,
    title: String,
    language: String,
    word_count: usize,
    text_scalar_length: usize,
    estimated_source_duration_ms: u64,
    segments: Vec<SegmentDto>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct SegmentDto {
    id: String,
    index: usize,
    text: String,
    scalar_start: usize,
    scalar_end: usize,
    word_start: usize,
    word_end: usize,
    word_count: usize,
    estimated_source_duration_ms: u64,
    logical_start_ms: u64,
    logical_end_ms: u64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct PositionDto {
    index: usize,
    fraction: f64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct LogicalTimeDto {
    index: usize,
    local_offset_ms: u64,
}

impl From<ReadingManifest> for ManifestDto {
    fn from(value: ReadingManifest) -> Self {
        Self {
            schema_version: value.schema_version,
            document_id: value.document_id,
            revision_id: value.revision_id,
            title: value.title,
            language: value.language,
            word_count: value.word_count,
            text_scalar_length: value.text_scalar_length,
            estimated_source_duration_ms: value.estimated_source_duration_ms,
            segments: value.segments.into_iter().map(SegmentDto::from).collect(),
        }
    }
}

impl From<ReadingSegment> for SegmentDto {
    fn from(value: ReadingSegment) -> Self {
        Self {
            id: value.id,
            index: value.index,
            text: value.text,
            scalar_start: value.scalar_start,
            scalar_end: value.scalar_end,
            word_start: value.word_start,
            word_end: value.word_end,
            word_count: value.word_count,
            estimated_source_duration_ms: value.estimated_source_duration_ms,
            logical_start_ms: value.logical_start_ms,
            logical_end_ms: value.logical_end_ms,
        }
    }
}

impl From<ManifestDto> for ReadingManifest {
    fn from(value: ManifestDto) -> Self {
        Self {
            schema_version: value.schema_version,
            document_id: value.document_id,
            revision_id: value.revision_id,
            title: value.title,
            language: value.language,
            word_count: value.word_count,
            text_scalar_length: value.text_scalar_length,
            estimated_source_duration_ms: value.estimated_source_duration_ms,
            segments: value.segments.into_iter().map(ReadingSegment::from).collect(),
        }
    }
}

impl From<SegmentDto> for ReadingSegment {
    fn from(value: SegmentDto) -> Self {
        Self {
            id: value.id,
            index: value.index,
            text: value.text,
            scalar_start: value.scalar_start,
            scalar_end: value.scalar_end,
            word_start: value.word_start,
            word_end: value.word_end,
            word_count: value.word_count,
            estimated_source_duration_ms: value.estimated_source_duration_ms,
            logical_start_ms: value.logical_start_ms,
            logical_end_ms: value.logical_end_ms,
        }
    }
}

fn encode<T: Serialize>(value: &T) -> Result<String, JsValue> {
    serde_json::to_string(value)
        .map_err(|error| JsValue::from_str(&format!("JSON encoding failed: {error}")))
}

fn decode_manifest(value: &str) -> Result<ReadingManifest, JsValue> {
    let dto: ManifestDto = serde_json::from_str(value)
        .map_err(|error| JsValue::from_str(&format!("ReadingManifest decode failed: {error}")))?;
    Ok(dto.into())
}

#[wasm_bindgen]
pub fn read_core_contract_version() -> u32 {
    1
}

#[wasm_bindgen]
pub fn build_manifest_json(
    document_id: String,
    revision_id: String,
    title: String,
    language: String,
    text: String,
    max_scalars: usize,
) -> Result<String, JsValue> {
    let manifest = build_manifest(
        document_id,
        revision_id,
        title,
        language,
        &text,
        max_scalars,
    );
    encode(&ManifestDto::from(manifest))
}

#[wasm_bindgen]
pub fn position_for_progress_json(
    manifest_json: &str,
    progress: f64,
) -> Result<String, JsValue> {
    let manifest = decode_manifest(manifest_json)?;
    let position = position_for_progress(&manifest, progress);
    encode(&PositionDto {
        index: position.index,
        fraction: position.fraction,
    })
}

#[wasm_bindgen]
pub fn segment_for_logical_time_json(
    manifest_json: &str,
    elapsed_ms: u64,
) -> Result<String, JsValue> {
    let manifest = decode_manifest(manifest_json)?;
    let Some((index, local_offset_ms)) = segment_for_logical_time(&manifest, elapsed_ms) else {
        return Ok("null".to_string());
    };
    encode(&LogicalTimeDto {
        index,
        local_offset_ms,
    })
}

#[wasm_bindgen]
pub fn prefetch_indexes_json(
    manifest_json: &str,
    active_index: usize,
    horizon_ms: u64,
    max_segments: usize,
) -> Result<String, JsValue> {
    let manifest = decode_manifest(manifest_json)?;
    encode(&prefetch_indexes(
        &manifest,
        active_index,
        horizon_ms,
        max_segments,
    ))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn wasm_dto_round_trip_preserves_manifest_semantics() {
        let manifest = build_manifest(
            "doc",
            "rev",
            "Title",
            "en",
            "Hello world. This is Floently Read.",
            600,
        );
        let encoded = serde_json::to_string(&ManifestDto::from(manifest.clone())).unwrap();
        let decoded = decode_manifest(&encoded).unwrap();

        assert_eq!(decoded.document_id, manifest.document_id);
        assert_eq!(decoded.word_count, manifest.word_count);
        assert_eq!(decoded.text_scalar_length, manifest.text_scalar_length);
        assert_eq!(decoded.segments, manifest.segments);
    }
}
