package com.floently.read

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.floently.shared.auth.FloentlySecureSessionStore
import java.time.Instant
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ReadProjectProgressPayload(
    val currentSegmentIndex: Int,
    val currentCharacterOffset: Int,
    val progressPercent: Double,
    val voiceId: String?,
    val playbackRate: Double
)

object ReadRemoteProjectProgressBridge {
    fun apply(
        context: Context,
        project: ReadContentProject,
        manifest: ReadingManifestV1,
        voiceSettings: ReadVoiceSettings
    ) {
        val progress = project.progress ?: return
        val store = ReadPlaybackResumeStore(context)
        val local = store.load(
            documentId = manifest.documentId,
            revisionId = manifest.revisionId
        )
        val remoteUpdatedAt = parseInstant(
            progress.updatedAt
        )?.toEpochMilli()

        if (
            local != null
            && (
                remoteUpdatedAt == null
                || remoteUpdatedAt <= local.updatedAtMs
            )
        ) {
            return
        }

        val boundedPercent = progress.progressPercent
            .coerceIn(0.0, 100.0)
        val logicalTimeMs = (
            manifest.estimatedSourceDurationMs.toDouble()
                * (boundedPercent / 100.0)
            )
            .toLong()
            .coerceAtLeast(0L)

        val preferredIndex = progress.currentSegmentIndex
        val segmentIndex = if (
            preferredIndex in manifest.segments.indices
        ) {
            preferredIndex
        } else {
            manifest.segments.indexOfFirst {
                logicalTimeMs < it.logicalEndMs
            }.takeIf { it >= 0 }
                ?: manifest.segments.indices.lastOrNull()
                ?: 0
        }

        val segment = manifest.segments
            .getOrNull(segmentIndex)
        val playbackSpeed = (
            progress.playbackRate
                ?: local?.playbackSpeed?.toDouble()
                ?: 1.0
            )
            .coerceIn(0.5, 3.0)
            .toFloat()

        store.save(
            ReadPlaybackResumeSnapshot(
                documentId = manifest.documentId,
                revisionId = manifest.revisionId,
                logicalTimeMs = logicalTimeMs,
                playbackSpeed = playbackSpeed,
                updatedAtMs = remoteUpdatedAt
                    ?: System.currentTimeMillis(),
                sourceScalarOffset = segment?.scalarStart,
                sourceSegmentId = segment?.id,
                sourceSegmentIndex = segment?.index,
                voiceId = progress.voiceId,
                renditionId = null
            )
        )

        progress.voiceId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                voiceSettings.select(
                    voiceId = it,
                    language = manifest.language
                )
            }
    }

    fun payload(
        manifest: ReadingManifestV1,
        snapshot: ReadPlayerUiSnapshot,
        voiceId: String?
    ): ReadProjectProgressPayload {
        val logicalDurationMs = maxOf(
            snapshot.durationMs,
            manifest.estimatedSourceDurationMs,
            1L
        )
        val elapsedMs = snapshot.positionMs
            .coerceIn(0L, logicalDurationMs)
        val percent = (
            elapsedMs.toDouble()
                / logicalDurationMs.toDouble()
                * 100.0
            )
            .coerceIn(0.0, 100.0)

        val index = manifest.segments.indexOfFirst {
            elapsedMs < it.logicalEndMs
        }.takeIf { it >= 0 }
            ?: manifest.segments.indices.lastOrNull()
            ?: 0

        val segment = manifest.segments.getOrNull(index)
            ?: return ReadProjectProgressPayload(
                currentSegmentIndex = 0,
                currentCharacterOffset = 0,
                progressPercent = percent,
                voiceId = voiceId,
                playbackRate = snapshot.speed.toDouble()
            )

        val segmentDuration = (
            segment.logicalEndMs - segment.logicalStartMs
            )
            .coerceAtLeast(1L)
        val localMs = (
            elapsedMs - segment.logicalStartMs
            )
            .coerceIn(0L, segmentDuration)
        val fraction = localMs.toDouble()
            / segmentDuration.toDouble()
        val scalarSpan = (
            segment.scalarEnd - segment.scalarStart
            )
            .coerceAtLeast(0)
        val scalarOffset = (
            segment.scalarStart
                + (scalarSpan.toDouble() * fraction)
                    .roundToInt()
            )
            .coerceIn(
                0,
                manifest.textScalarLength
                    .coerceAtLeast(0)
            )

        return ReadProjectProgressPayload(
            currentSegmentIndex = index,
            currentCharacterOffset = scalarOffset,
            progressPercent = percent,
            voiceId = voiceId,
            playbackRate = snapshot.speed.toDouble()
        )
    }

    private fun parseInstant(
        value: String
    ): Instant? =
        runCatching {
            Instant.parse(value)
        }.getOrNull()
}

@Composable
fun ReadProjectProgressSyncEffect(
    project: ReadContentProject,
    manifest: ReadingManifestV1?,
    sessionStore: FloentlySecureSessionStore,
    projectStore: ReadProjectStore,
    playbackController: ReadPlaybackController,
    voiceSettings: ReadVoiceSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastSyncedBucket by remember(
        project.id,
        project.revisionId
    ) {
        mutableIntStateOf(-1)
    }

    LaunchedEffect(
        project.id,
        project.revisionId,
        project.progress?.updatedAt,
        manifest?.revisionId
    ) {
        val value = manifest ?: return@LaunchedEffect

        ReadRemoteProjectProgressBridge.apply(
            context = context,
            project = project,
            manifest = value,
            voiceSettings = voiceSettings
        )
    }

    val snapshot = playbackController.snapshot
    LaunchedEffect(
        snapshot.positionMs,
        snapshot.isPlaying,
        manifest?.revisionId,
        playbackController.activeDocumentId,
        playbackController.activeRevisionId
    ) {
        val value = manifest ?: return@LaunchedEffect
        val token = sessionStore.session?.token
            ?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect

        if (
            playbackController.activeDocumentId
                != project.id
            || playbackController.activeRevisionId
                != project.revisionId
            || snapshot.positionMs <= 0L
        ) {
            return@LaunchedEffect
        }

        val bucket = (
            snapshot.positionMs / 10_000L
            )
            .toInt()

        val shouldSync = bucket != lastSyncedBucket
            || !snapshot.isPlaying

        if (!shouldSync) {
            return@LaunchedEffect
        }

        lastSyncedBucket = bucket
        val payload =
            ReadRemoteProjectProgressBridge.payload(
                manifest = value,
                snapshot = snapshot,
                voiceId =
                    playbackController.activeVoiceId
            )

        scope.launch {
            projectStore.syncProgress(
                projectId = project.id,
                currentSegmentIndex =
                    payload.currentSegmentIndex,
                currentCharacterOffset =
                    payload.currentCharacterOffset,
                progressPercent =
                    payload.progressPercent,
                voiceId = payload.voiceId,
                playbackRate = payload.playbackRate,
                accessToken = token
            )
        }
    }
}
