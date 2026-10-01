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

void floently_read_string_free(char *pointer);

#ifdef __cplusplus
}
#endif

#endif
