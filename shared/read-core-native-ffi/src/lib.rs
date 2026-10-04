use floently_read_core::{build_manifest, ReadingManifest, ReadingSegment};
use percent_encoding::percent_decode_str;
use roxmltree::Document;
use serde::Serialize;
use std::collections::HashMap;
use std::ffi::{c_char, CStr, CString};
use std::fs::{self, File};
use std::io;
use std::path::{Component, Path, PathBuf};
use std::ptr;
use zip::ZipArchive;

const EPUB_MAX_ENTRIES: usize = 20_000;
const EPUB_MAX_ENTRY_BYTES: u64 = 96 * 1024 * 1024;
const EPUB_MAX_TOTAL_BYTES: u64 = 512 * 1024 * 1024;

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

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct EpubPackageDto {
    schema_version: u32,
    title: String,
    package_path: String,
    spine: Vec<EpubSpineItemDto>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct EpubSpineItemDto {
    index: usize,
    idref: String,
    path: String,
    media_type: String,
}

#[derive(Debug, Clone)]
struct EpubManifestItem {
    href: String,
    media_type: String,
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

fn extract_epub_json(
    input_path: &str,
    output_directory: &str,
) -> Result<String, String> {
    let input = Path::new(input_path);
    let output = Path::new(output_directory);

    if !input.is_file() {
        return Err("EPUB input file does not exist".to_string());
    }

    if output.as_os_str().is_empty() {
        return Err("EPUB output directory must not be empty".to_string());
    }

    if output.exists() {
        fs::remove_dir_all(output)
            .map_err(|error| format!("EPUB output could not be reset: {error}"))?;
    }
    fs::create_dir_all(output)
        .map_err(|error| format!("EPUB output could not be created: {error}"))?;

    let result = (|| {
        let file = File::open(input)
            .map_err(|error| format!("EPUB could not be opened: {error}"))?;
        let mut archive = ZipArchive::new(file)
            .map_err(|error| format!("EPUB ZIP is invalid: {error}"))?;

        if archive.len() == 0 {
            return Err("EPUB archive is empty".to_string());
        }
        if archive.len() > EPUB_MAX_ENTRIES {
            return Err("EPUB contains too many archive entries".to_string());
        }

        let mut total_uncompressed = 0u64;

        for index in 0..archive.len() {
            let mut entry = archive
                .by_index(index)
                .map_err(|error| format!("EPUB entry could not be read: {error}"))?;

            if entry.encrypted() {
                return Err("Encrypted EPUB files are not supported".to_string());
            }
            if entry.is_symlink() {
                return Err("EPUB symlink entries are not supported".to_string());
            }

            let size = entry.size();
            if size > EPUB_MAX_ENTRY_BYTES {
                return Err("EPUB contains an oversized archive entry".to_string());
            }

            total_uncompressed = total_uncompressed
                .checked_add(size)
                .ok_or_else(|| "EPUB size overflow".to_string())?;
            if total_uncompressed > EPUB_MAX_TOTAL_BYTES {
                return Err("EPUB expands beyond the supported size limit".to_string());
            }

            let relative = entry
                .enclosed_name()
                .ok_or_else(|| "EPUB contains an unsafe archive path".to_string())?;
            let destination = output.join(&relative);

            if entry.is_dir() {
                fs::create_dir_all(&destination)
                    .map_err(|error| format!("EPUB directory could not be created: {error}"))?;
                continue;
            }

            if let Some(parent) = destination.parent() {
                fs::create_dir_all(parent)
                    .map_err(|error| format!("EPUB directory could not be created: {error}"))?;
            }

            let mut target = File::create(&destination)
                .map_err(|error| format!("EPUB entry could not be created: {error}"))?;
            io::copy(&mut entry, &mut target)
                .map_err(|error| format!("EPUB entry could not be extracted: {error}"))?;
        }

        parse_extracted_epub(output)
    })();

    match result {
        Ok(package) => serde_json::to_string(&package)
            .map_err(|error| format!("EPUB package serialization failed: {error}")),
        Err(error) => {
            let _ = fs::remove_dir_all(output);
            Err(error)
        }
    }
}

fn parse_extracted_epub(output: &Path) -> Result<EpubPackageDto, String> {
    let container_path = output.join("META-INF").join("container.xml");
    let container_xml = fs::read_to_string(&container_path)
        .map_err(|error| format!("EPUB container.xml could not be read: {error}"))?;
    let container = Document::parse(&container_xml)
        .map_err(|error| format!("EPUB container.xml is invalid: {error}"))?;

    let rootfile = container
        .descendants()
        .find(|node| node.is_element() && node.tag_name().name() == "rootfile")
        .and_then(|node| node.attribute("full-path"))
        .ok_or_else(|| "EPUB container does not identify a package document".to_string())?;

    let package_relative = resolve_epub_relative(Path::new(""), rootfile)?;
    let package_file = output.join(&package_relative);
    if !package_file.is_file() {
        return Err("EPUB package document is missing".to_string());
    }

    let package_xml = fs::read_to_string(&package_file)
        .map_err(|error| format!("EPUB package document could not be read: {error}"))?;
    let package_document = Document::parse(&package_xml)
        .map_err(|error| format!("EPUB package document is invalid: {error}"))?;

    let title = package_document
        .descendants()
        .find(|node| node.is_element() && node.tag_name().name() == "title")
        .and_then(|node| node.text())
        .map(str::trim)
        .filter(|value| !value.is_empty())
        .unwrap_or("")
        .to_string();

    let mut manifest = HashMap::<String, EpubManifestItem>::new();

    for node in package_document
        .descendants()
        .filter(|node| node.is_element() && node.tag_name().name() == "item")
    {
        let Some(id) = node.attribute("id").map(str::trim).filter(|value| !value.is_empty())
        else {
            continue;
        };
        let Some(href) = node
            .attribute("href")
            .map(str::trim)
            .filter(|value| !value.is_empty())
        else {
            continue;
        };

        manifest.insert(
            id.to_string(),
            EpubManifestItem {
                href: href.to_string(),
                media_type: node.attribute("media-type").unwrap_or("").trim().to_string(),
            },
        );
    }

    let package_parent = package_relative.parent().unwrap_or(Path::new(""));
    let mut spine = Vec::<EpubSpineItemDto>::new();

    for node in package_document
        .descendants()
        .filter(|node| node.is_element() && node.tag_name().name() == "itemref")
    {
        let Some(idref) = node
            .attribute("idref")
            .map(str::trim)
            .filter(|value| !value.is_empty())
        else {
            continue;
        };
        let Some(item) = manifest.get(idref) else {
            continue;
        };

        if item.media_type != "application/xhtml+xml"
            && item.media_type != "text/html"
            && !item.href.to_ascii_lowercase().ends_with(".xhtml")
            && !item.href.to_ascii_lowercase().ends_with(".html")
            && !item.href.to_ascii_lowercase().ends_with(".htm")
        {
            continue;
        }

        let relative = resolve_epub_relative(package_parent, &item.href)?;
        if !output.join(&relative).is_file() {
            continue;
        }

        spine.push(EpubSpineItemDto {
            index: spine.len(),
            idref: idref.to_string(),
            path: portable_relative_path(&relative),
            media_type: item.media_type.clone(),
        });
    }

    if spine.is_empty() {
        return Err("EPUB package does not contain readable spine documents".to_string());
    }

    Ok(EpubPackageDto {
        schema_version: 1,
        title,
        package_path: portable_relative_path(&package_relative),
        spine,
    })
}

fn resolve_epub_relative(base: &Path, raw_href: &str) -> Result<PathBuf, String> {
    let path_part = raw_href
        .split(|character| character == '#' || character == '?')
        .next()
        .unwrap_or("")
        .trim();

    if path_part.is_empty() {
        return Err("EPUB contains an empty content path".to_string());
    }

    let decoded = percent_decode_str(path_part)
        .decode_utf8()
        .map_err(|_| "EPUB contains a non-UTF-8 content path".to_string())?;

    normalize_relative_path(&base.join(decoded.as_ref()))
}

fn normalize_relative_path(path: &Path) -> Result<PathBuf, String> {
    let mut normalized = PathBuf::new();

    for component in path.components() {
        match component {
            Component::Normal(value) => normalized.push(value),
            Component::CurDir => {}
            Component::ParentDir => {
                if !normalized.pop() {
                    return Err("EPUB content path escapes its package".to_string());
                }
            }
            Component::RootDir | Component::Prefix(_) => {
                return Err("EPUB contains an absolute content path".to_string());
            }
        }
    }

    if normalized.as_os_str().is_empty() {
        return Err("EPUB content path is empty after normalization".to_string());
    }

    Ok(normalized)
}

fn portable_relative_path(path: &Path) -> String {
    path.to_string_lossy().replace('\\', "/")
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

/// Extracts an EPUB archive into the caller-provided private directory and
/// returns JSON describing the package spine. The extractor rejects unsafe
/// paths, links, encryption, excessive archive fan-out and oversized output.
#[no_mangle]
pub unsafe extern "C" fn floently_read_extract_epub_json(
    input_path: *const c_char,
    output_directory: *const c_char,
) -> *mut c_char {
    let result = (|| {
        extract_epub_json(
            utf8_arg(input_path, "input_path")?,
            utf8_arg(output_directory, "output_directory")?,
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
    use super::{build_manifest_json, extract_epub_json};
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

    #[no_mangle]
    pub extern "system" fn Java_com_floently_read_ReadCoreNative_nativeExtractEpubJson(
        mut env: JNIEnv<'_>,
        _class: JClass<'_>,
        input_path: JString<'_>,
        output_directory: JString<'_>,
    ) -> jstring {
        let result = (|| {
            let input_path =
                java_string(&mut env, input_path, "inputPath")?;
            let output_directory =
                java_string(&mut env, output_directory, "outputDirectory")?;

            extract_epub_json(&input_path, &output_directory)
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
                format!("EPUB result could not be returned: {error}"),
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

    #[test]
    fn resolves_epub_paths_without_leaving_package() {
        assert_eq!(
            resolve_epub_relative(
                Path::new("OEBPS"),
                "chapters/one.xhtml#section"
            )
            .unwrap(),
            PathBuf::from("OEBPS/chapters/one.xhtml")
        );

        assert!(
            resolve_epub_relative(
                Path::new("OEBPS"),
                "../../outside.xhtml"
            )
            .is_err()
        );
    }

    #[test]
    fn decodes_percent_encoded_epub_paths() {
        assert_eq!(
            resolve_epub_relative(
                Path::new("OPS"),
                "Text/Chapter%201.xhtml"
            )
            .unwrap(),
            PathBuf::from("OPS/Text/Chapter 1.xhtml")
        );
    }
}
