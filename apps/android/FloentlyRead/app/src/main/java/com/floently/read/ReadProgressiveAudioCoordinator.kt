package com.floently.read

import android.content.Context

class ReadProgressiveAudioCoordinator(
    context: Context,
    private val tts: ReadNativeTtsClient = ReadNativeTtsClient(),
    private val cache: ReadNativeAudioCache = ReadNativeAudioCache(context)
) {
    private val appContext = context.applicationContext
    suspend fun prepare(
        manifest: ReadingManifestV1,
        startingAt: Int,
        voiceId: String,
        accessToken: String? = null,
        accountIdentity: String? = null,
        horizonMs: Long = 120_000L,
        maxSegments: Int = 4
    ): List<ReadPlaybackSegment> {
        if (manifest.segments.isEmpty()) return emptyList()

        val start = startingAt.coerceIn(
            0,
            manifest.segments.lastIndex
        )
        val horizon = horizonMs.coerceAtLeast(30_000L)
        val limit = maxSegments.coerceAtLeast(1)

        val selected = mutableListOf<ReadingManifestSegmentV1>()
        var buffered = 0L

        for (segment in manifest.segments.drop(start)) {
            if (selected.size >= limit) break
            selected += segment
            buffered += (
                segment.logicalEndMs - segment.logicalStartMs
            ).coerceAtLeast(0L)

            if (buffered >= horizon) break
        }

        return selected.map { segment ->
            val lookupKey = tts.lookupKey(
                text = segment.text,
                language = manifest.language,
                voiceId = voiceId
            )
            val offline = accountIdentity?.let {
                ReadOfflineAudioStore.asset(
                    context = appContext,
                    accountIdentity = it,
                    manifest = manifest,
                    voiceId = voiceId,
                    segment = segment
                )
            }

            if (offline != null) {
                val renditionId =
                    offline.renditionId
                        ?: ReadNativeAudioIdentity
                            .renditionId(
                                documentId =
                                    manifest.documentId,
                                revisionId =
                                    manifest.revisionId,
                                language =
                                    manifest.language,
                                voiceId =
                                    voiceId,
                                provider = null,
                                model = null
                            )
                val timingMapId =
                    offline.timingMapId
                        ?: ReadNativeAudioIdentity
                            .timingMapId(
                                renditionId =
                                    renditionId,
                                segmentId =
                                    segment.id,
                                segmentIndex =
                                    segment.index,
                                logicalStartMs =
                                    segment.logicalStartMs,
                                logicalEndMs =
                                    segment.logicalEndMs,
                                physicalDurationMs =
                                    offline.durationMs
                            )

                ReadPlaybackSegment(
                    id = segment.id,
                    index = segment.index,
                    logicalStartMs = segment.logicalStartMs,
                    logicalEndMs = segment.logicalEndMs,
                    audioUri = offline.localFile
                        .toURI()
                        .toString(),
                    actualDurationMs =
                        offline.durationMs,
                    renditionId =
                        renditionId,
                    timingMapId =
                        timingMapId
                )
            } else {
                val cached = cache.cachedAsset(
                    lookupKey
                )

                if (cached != null) {
                    val renditionId =
                        ReadNativeAudioIdentity
                            .renditionId(
                                documentId =
                                    manifest.documentId,
                                revisionId =
                                    manifest.revisionId,
                                language =
                                    manifest.language,
                                voiceId =
                                    cached.voiceId,
                                provider =
                                    cached.provider,
                                model =
                                    cached.model
                            )
                    val timingMapId =
                        ReadNativeAudioIdentity
                            .timingMapId(
                                renditionId =
                                    renditionId,
                                segmentId =
                                    segment.id,
                                segmentIndex =
                                    segment.index,
                                logicalStartMs =
                                    segment.logicalStartMs,
                                logicalEndMs =
                                    segment.logicalEndMs,
                                physicalDurationMs =
                                    cached.durationMs
                            )

                    ReadPlaybackSegment(
                        id = segment.id,
                        index = segment.index,
                        logicalStartMs = segment.logicalStartMs,
                        logicalEndMs = segment.logicalEndMs,
                        audioUri = cached.localFile
                            .toURI()
                            .toString(),
                        actualDurationMs =
                            cached.durationMs,
                        renditionId =
                            renditionId,
                        timingMapId =
                            timingMapId
                    )
                } else {
                    val asset = tts.synthesize(
                        text = segment.text,
                        language = manifest.language,
                        voiceId = voiceId,
                        accessToken = accessToken
                    )
                    val local = cache.localFile(asset)
                    val renditionId =
                        ReadNativeAudioIdentity
                            .renditionId(
                                documentId =
                                    manifest.documentId,
                                revisionId =
                                    manifest.revisionId,
                                language =
                                    manifest.language,
                                voiceId =
                                    asset.voiceId,
                                provider =
                                    asset.provider,
                                model =
                                    asset.model
                            )
                    val timingMapId =
                        ReadNativeAudioIdentity
                            .timingMapId(
                                renditionId =
                                    renditionId,
                                segmentId =
                                    segment.id,
                                segmentIndex =
                                    segment.index,
                                logicalStartMs =
                                    segment.logicalStartMs,
                                logicalEndMs =
                                    segment.logicalEndMs,
                                physicalDurationMs =
                                    asset.durationMs
                            )

                    ReadPlaybackSegment(
                        id = segment.id,
                        index = segment.index,
                        logicalStartMs = segment.logicalStartMs,
                        logicalEndMs = segment.logicalEndMs,
                        audioUri = local.toURI().toString(),
                        actualDurationMs =
                            asset.durationMs,
                        renditionId =
                            renditionId,
                        timingMapId =
                            timingMapId
                    )
                }
            }
        }
    }

    suspend fun saveOffline(
        manifest: ReadingManifestV1,
        voiceId: String,
        accessToken: String?,
        accountIdentity: String
    ) {
        val segments = prepare(
            manifest = manifest,
            startingAt = 0,
            voiceId = voiceId,
            accessToken = accessToken,
            accountIdentity = accountIdentity,
            horizonMs = Long.MAX_VALUE / 4L,
            maxSegments =
                manifest.segments.size
                    .coerceAtLeast(1)
        )

        require(
            segments.size
                == manifest.segments.size
        ) {
            "Read could not finish the offline audio bundle."
        }

        ReadOfflineAudioStore.install(
            context = appContext,
            accountIdentity = accountIdentity,
            manifest = manifest,
            voiceId = voiceId,
            segments = segments
        )
    }

    suspend fun isAvailableOffline(
        manifest: ReadingManifestV1,
        voiceId: String,
        accountIdentity: String
    ): Boolean =
        ReadOfflineAudioStore.isComplete(
            context = appContext,
            accountIdentity = accountIdentity,
            manifest = manifest,
            voiceId = voiceId
        )

    suspend fun removeOffline(
        manifest: ReadingManifestV1,
        voiceId: String,
        accountIdentity: String
    ) {
        ReadOfflineAudioStore.remove(
            context = appContext,
            accountIdentity = accountIdentity,
            documentId = manifest.documentId,
            revisionId = manifest.revisionId,
            voiceId = voiceId
        )
    }

    suspend fun replaceProtectedSegments(
        segments: Collection<ReadPlaybackSegment>
    ) {
        cache.replaceProtectedUris(
            segments.mapNotNull { it.audioUri }
        )
    }

    fun playableDocument(
        manifest: ReadingManifestV1,
        segments: List<ReadPlaybackSegment>,
        author: String? = null
    ): ReadPlayableDocument = ReadPlayableDocument(
        id = manifest.documentId,
        revisionId = manifest.revisionId,
        title = manifest.title,
        author = author,
        estimatedDurationMs = manifest.estimatedSourceDurationMs,
        segments = segments.sortedBy { it.index }
    )
}
