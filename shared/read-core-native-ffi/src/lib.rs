use floently_read_core::{build_manifest, ReadingManifest, ReadingSegment};
use serde::Serialize;
use std::ffi::{c_char, CStr, CString};
use std::ptr;

#[derive(Debug, Clone, Serialize)]
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

#[derive(Debug, Clone, Serialize)]
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

fn build_manifest_json(
    document_id: &str,
    revision_id: &str,
    title: &str,
    language: &str,
    text: &str,
    max_scalars: usize,
) -> Result<String, String> {
    if document_id.trim().is_empty() {
        return Err("document_id must not be empty".to_string());
    }
    if revision_id.trim().is_empty() {
        return Err("revision_id must not be empty".to_string());
    }

    let manifest = build_manifest(
        document_id,
        revision_id,
        title,
        language,
        text,
        max_scalars.max(600),
    );

    serde_json::to_string(&ManifestDto::from(manifest))
        .map_err(|error| format!("manifest serialization failed: {error}"))
}

unsafe fn utf8_arg<'a>(
    pointer: *const c_char,
    name: &str,
) -> Result<&'a str, String> {
    if pointer.is_null() {
        return Err(format!("{name} must not be null"));
    }

    CStr::from_ptr(pointer)
        .to_str()
        .map_err(|_| format!("{name} must be valid UTF-8"))
}

/// Builds ReadingManifest v1 JSON using the shared Rust Read Core.
/// Returned memory belongs to the caller and must be released with
/// floently_read_string_free. A null result means validation or conversion
/// failed.
#[no_mangle]
pub unsafe extern "C" fn floently_read_build_manifest_json(
    document_id: *const c_char,
    revision_id: *const c_char,
    title: *const c_char,
    language: *const c_char,
    text: *const c_char,
    max_scalars: usize,
) -> *mut c_char {
    let result = (|| {
        build_manifest_json(
            utf8_arg(document_id, "document_id")?,
            utf8_arg(revision_id, "revision_id")?,
            utf8_arg(title, "title")?,
            utf8_arg(language, "language")?,
            utf8_arg(text, "text")?,
            max_scalars,
        )
    })();

    match result {
        Ok(json) => CString::new(json)
            .map(CString::into_raw)
            .unwrap_or(ptr::null_mut()),
        Err(_) => ptr::null_mut(),
    }
}

#[no_mangle]
pub unsafe extern "C" fn floently_read_string_free(pointer: *mut c_char) {
    if pointer.is_null() {
        return;
    }

    drop(CString::from_raw(pointer));
}

#[cfg(target_os = "android")]
mod android {
    use super::build_manifest_json;
    use jni::objects::{JClass, JString};
    use jni::sys::{jint, jstring};
    use jni::JNIEnv;
    use std::ptr;

    fn java_string(
        env: &mut JNIEnv<'_>,
        value: JString<'_>,
        name: &str,
    ) -> Result<String, String> {
        env.get_string(&value)
            .map(|text| text.into())
            .map_err(|error| format!("{name} could not be read: {error}"))
    }

    fn throw_and_null(
        env: &mut JNIEnv<'_>,
        message: impl AsRef<str>,
    ) -> jstring {
        let _ = env.throw_new(
            "java/lang/IllegalArgumentException",
            message.as_ref(),
        );
        ptr::null_mut()
    }

    #[no_mangle]
    pub extern "system" fn Java_com_floently_read_ReadCoreNative_nativeBuildManifestJson(
        mut env: JNIEnv<'_>,
        _class: JClass<'_>,
        document_id: JString<'_>,
        revision_id: JString<'_>,
        title: JString<'_>,
        language: JString<'_>,
        text: JString<'_>,
        max_scalars: jint,
    ) -> jstring {
        let result = (|| {
            let document_id =
                java_string(&mut env, document_id, "documentId")?;
            let revision_id =
                java_string(&mut env, revision_id, "revisionId")?;
            let title = java_string(&mut env, title, "title")?;
            let language =
                java_string(&mut env, language, "language")?;
            let text = java_string(&mut env, text, "text")?;

            build_manifest_json(
                &document_id,
                &revision_id,
                &title,
                &language,
                &text,
                usize::try_from(max_scalars.max(600)).unwrap_or(600),
            )
        })();

        let json = match result {
            Ok(json) => json,
            Err(message) => {
                return throw_and_null(&mut env, message);
            }
        };

        match env.new_string(json) {
            Ok(value) => value.into_raw(),
            Err(error) => throw_and_null(
                &mut env,
                format!("manifest result could not be returned: {error}"),
            ),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::Value;

    #[test]
    fn native_json_matches_core_timeline() {
        let text = (0..2200)
            .map(|index| format!("word{index}"))
            .collect::<Vec<_>>()
            .join(" ");

        let direct = build_manifest(
            "doc-1",
            "rev-1",
            "Long article",
            "en",
            &text,
            900,
        );

        let json = build_manifest_json(
            "doc-1",
            "rev-1",
            "Long article",
            "en",
            &text,
            900,
        )
        .expect("manifest json");

        let decoded: Value =
            serde_json::from_str(&json).expect("valid json");

        assert_eq!(decoded["schemaVersion"], 1);
        assert_eq!(decoded["documentId"], "doc-1");
        assert_eq!(decoded["revisionId"], "rev-1");
        assert_eq!(
            decoded["wordCount"].as_u64(),
            Some(direct.word_count as u64)
        );
        assert_eq!(
            decoded["textScalarLength"].as_u64(),
            Some(direct.text_scalar_length as u64)
        );
        assert_eq!(
            decoded["estimatedSourceDurationMs"].as_u64(),
            Some(direct.estimated_source_duration_ms)
        );
        assert_eq!(
            decoded["segments"].as_array().unwrap().len(),
            direct.segments.len()
        );

        for (json_segment, core_segment) in decoded["segments"]
            .as_array()
            .unwrap()
            .iter()
            .zip(direct.segments.iter())
        {
            assert_eq!(
                json_segment["index"].as_u64(),
                Some(core_segment.index as u64)
            );
            assert_eq!(
                json_segment["scalarStart"].as_u64(),
                Some(core_segment.scalar_start as u64)
            );
            assert_eq!(
                json_segment["scalarEnd"].as_u64(),
                Some(core_segment.scalar_end as u64)
            );
            assert_eq!(
                json_segment["logicalStartMs"].as_u64(),
                Some(core_segment.logical_start_ms)
            );
            assert_eq!(
                json_segment["logicalEndMs"].as_u64(),
                Some(core_segment.logical_end_ms)
            );
        }
    }

    #[test]
    fn rejects_empty_document_identity() {
        let error = build_manifest_json(
            "",
            "rev",
            "Title",
            "en",
            "Some text",
            1200,
        )
        .unwrap_err();

        assert!(error.contains("document_id"));
    }
}
