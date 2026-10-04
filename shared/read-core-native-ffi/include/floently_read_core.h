#ifndef FLOENTLY_READ_CORE_H
#define FLOENTLY_READ_CORE_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

char *floently_read_build_manifest_json(
    const char *document_id,
    const char *revision_id,
    const char *title,
    const char *language,
    const char *text,
    size_t max_scalars
);

char *floently_read_resolve_source_anchor_json(
    const char *source_text,
    const char *quote,
    const char *prefix_context,
    const char *suffix_context
);

char *floently_read_extract_epub_json(
    const char *input_path,
    const char *output_directory
);

void floently_read_string_free(char *pointer);

#ifdef __cplusplus
}
#endif

#endif
