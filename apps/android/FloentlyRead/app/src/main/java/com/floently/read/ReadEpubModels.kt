package com.floently.read

import java.io.File
import org.json.JSONObject

data class ReadEpubSpineItem(
    val index: Int,
    val idref: String,
    val path: String,
    val mediaType: String
)

data class ReadEpubPackage(
    val schemaVersion: Int,
    val title: String,
    val packagePath: String,
    val spine: List<ReadEpubSpineItem>
) {
    companion object {
        fun decode(
            json: String
        ): ReadEpubPackage {
            val root = JSONObject(json)
            val values =
                root.optJSONArray("spine")
                    ?: error(
                        "EPUB package spine is missing."
                    )
            val spine = buildList {
                for (
                    index
                    in 0 until values.length()
                ) {
                    val value =
                        values.optJSONObject(
                            index
                        ) ?: continue
                    val path =
                        value.optString("path")
                            .trim()
                    if (
                        path.isEmpty()
                        || path.startsWith("/")
                        || path.split("/")
                            .contains("..")
                    ) {
                        continue
                    }

                    add(
                        ReadEpubSpineItem(
                            index =
                                value.optInt(
                                    "index",
                                    size
                                ),
                            idref =
                                value.optString(
                                    "idref"
                                ),
                            path = path,
                            mediaType =
                                value.optString(
                                    "mediaType"
                                )
                        )
                    )
                }
            }

            require(spine.isNotEmpty()) {
                "EPUB package does not contain readable chapters."
            }

            return ReadEpubPackage(
                schemaVersion =
                    root.optInt(
                        "schemaVersion",
                        0
                    ),
                title =
                    root.optString(
                        "title"
                    ),
                packagePath =
                    root.optString(
                        "packagePath"
                    ),
                spine = spine
            )
        }
    }
}

data class ReadLocalEpubPackage(
    val metadata: ReadEpubPackage,
    val rootDirectory: File
) {
    fun chapterFile(
        index: Int
    ): File? {
        val item =
            metadata.spine
                .getOrNull(index)
                ?: return null
        val file = File(
            rootDirectory,
            item.path
        )
        return file.takeIf {
            it.isFile
        }
    }
}
