package com.floently.read

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class ReadingManifestSynthesisStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("generating")
    GENERATING,

    @SerialName("ready")
    READY,

    @SerialName("failed")
    FAILED
}

@Serializable
data class ReadingManifestSynthesisV1(
    val status: ReadingManifestSynthesisStatus,
    val requestKey: String? = null,
    val voiceId: String? = null,
    val provider: String? = null,
    val model: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class ReadingManifestAudioV1(
    val uri: String,
    val durationMs: Long,
    val contentHash: String? = null,
    val cacheKey: String? = null,
    val voiceId: String? = null,
    val provider: String? = null,
    val model: String? = null,
    val format: String? = null
)

@Serializable
data class ReadingManifestTimingV1(
    val scalarStart: Int,
    val scalarEnd: Int,
    val startMs: Long,
    val endMs: Long
)

@Serializable
data class ReadingManifestSegmentV1(
    val id: String,
    val index: Int,
    val text: String,
    val scalarStart: Int,
    val scalarEnd: Int,
    val wordStart: Int,
    val wordEnd: Int,
    val wordCount: Int,
    val estimatedSourceDurationMs: Long,
    val logicalStartMs: Long,
    val logicalEndMs: Long,
    val synthesis: ReadingManifestSynthesisV1? = null,
    val audio: ReadingManifestAudioV1? = null,
    val timings: List<ReadingManifestTimingV1> = emptyList()
) {
    val isAudioReady: Boolean
        get() = synthesis?.status == ReadingManifestSynthesisStatus.READY &&
            !audio?.uri.isNullOrBlank()
}

@Serializable
data class ReadingManifestV1(
    val schemaVersion: Int,
    val documentId: String,
    val revisionId: String,
    val title: String,
    val language: String,
    val wordCount: Int,
    val textScalarLength: Int,
    val estimatedSourceDurationMs: Long,
    val segments: List<ReadingManifestSegmentV1>
) {
    val readyAudioPrefix: List<ReadingManifestSegmentV1>
        get() = segments.takeWhile { it.isAudioReady }

    fun validate(): ReadingManifestV1 {
        if (schemaVersion != 1) {
            throw ReadingManifestValidationException(
                "Unsupported ReadingManifest schema version: $schemaVersion"
            )
        }
        if (wordCount < 0 || textScalarLength < 0 || estimatedSourceDurationMs < 0L) {
            throw ReadingManifestValidationException(
                "ReadingManifest contains a negative document value."
            )
        }

        var expectedIndex = 0
        var expectedWordStart = 0
        var expectedLogicalStart = 0L
        var previousScalarEnd = 0
        var totalWords = 0

        for (segment in segments) {
            if (segment.index != expectedIndex) {
                throw ReadingManifestValidationException(
                    "Segment index drift: expected $expectedIndex, got ${segment.index}."
                )
            }

            if (
                segment.scalarStart < previousScalarEnd ||
                segment.scalarStart < 0 ||
                segment.scalarEnd <= segment.scalarStart ||
                segment.scalarEnd > textScalarLength
            ) {
                throw ReadingManifestValidationException(
                    "Segment ${segment.index} has an invalid canonical scalar range."
                )
            }

            if (
                segment.wordStart != expectedWordStart ||
                segment.wordEnd < segment.wordStart ||
                segment.wordEnd - segment.wordStart != segment.wordCount
            ) {
                throw ReadingManifestValidationException(
                    "Segment ${segment.index} has an invalid word range."
                )
            }

            if (
                segment.logicalStartMs != expectedLogicalStart ||
                segment.logicalEndMs < segment.logicalStartMs ||
                segment.estimatedSourceDurationMs < 0L
            ) {
                throw ReadingManifestValidationException(
                    "Segment ${segment.index} has an invalid logical time range."
                )
            }

            if (
                segment.synthesis?.status == ReadingManifestSynthesisStatus.READY &&
                segment.audio?.uri.isNullOrBlank()
            ) {
                throw ReadingManifestValidationException(
                    "Segment ${segment.index} is ready but has no audio URI."
                )
            }

            if (segment.audio?.durationMs?.let { it < 0L } == true) {
                throw ReadingManifestValidationException(
                    "Segment ${segment.index} has an invalid audio duration."
                )
            }

            var priorTimingEnd = 0L
            for (timing in segment.timings) {
                if (
                    timing.scalarStart < segment.scalarStart ||
                    timing.scalarEnd <= timing.scalarStart ||
                    timing.scalarEnd > segment.scalarEnd ||
                    timing.startMs < priorTimingEnd ||
                    timing.endMs < timing.startMs
                ) {
                    throw ReadingManifestValidationException(
                        "Segment ${segment.index} has invalid timing metadata."
                    )
                }
                priorTimingEnd = timing.endMs
            }

            totalWords += segment.wordCount
            expectedIndex += 1
            expectedWordStart = segment.wordEnd
            expectedLogicalStart = segment.logicalEndMs
            previousScalarEnd = segment.scalarEnd
        }

        if (totalWords != wordCount) {
            throw ReadingManifestValidationException(
                "Document wordCount mismatch: expected $wordCount, got $totalWords."
            )
        }

        if (expectedLogicalStart != estimatedSourceDurationMs) {
            throw ReadingManifestValidationException(
                "Document duration mismatch: expected $estimatedSourceDurationMs ms, " +
                    "got $expectedLogicalStart ms."
            )
        }

        return this
    }
}

class ReadingManifestValidationException(message: String) : IllegalArgumentException(message)

object ReadingManifestV1Codec {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = true
    }

    fun decode(value: String): ReadingManifestV1 =
        json.decodeFromString<ReadingManifestV1>(value).validate()
}
