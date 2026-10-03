package com.floently.learn.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.design.KVCardStyle
import com.floently.learn.design.KVColor
import com.floently.learn.design.KVSpacing
import com.floently.learn.design.KVStatusTone
import com.floently.learn.design.KieliValmisCardSurface
import com.floently.learn.design.KieliValmisIconTile
import com.floently.learn.design.KieliValmisSectionHeader
import com.floently.learn.design.KieliValmisSegmentedControl
import com.floently.learn.design.KieliValmisStatusBanner
import com.floently.shared.learn.LearnCardDeckResponse
import com.floently.shared.learn.LearnOverviewService
import com.floently.shared.learn.fetchEverydayDeck
import kotlinx.coroutines.launch

@Composable
fun KieliValmisEverydayOverviewScreen(
    service: LearnOverviewService,
    accessState: CapabilityAccessState,
    retryAccess: () -> Unit,
    onBack: () -> Unit
) {
    var levelBand by remember { mutableStateOf("B1_B2") }
    var contentType by remember { mutableStateOf("vocabulary_card") }
    var deck by remember { mutableStateOf<LearnCardDeckResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val contentLabel = when (contentType) {
        "sentence_card" -> "Sentences"
        "grammar_card" -> "Grammar"
        else -> "Vocabulary"
    }

    suspend fun load() {
        isLoading = true
        errorMessage = null

        runCatching {
            service.fetchEverydayDeck(
                contentType = contentType,
                levelBand = levelBand
            )
        }.onSuccess {
            deck = it
        }.onFailure {
            errorMessage = it.message ?: "Could not load Everyday Finnish material."
        }

        isLoading = false
    }

    LaunchedEffect(levelBand, contentType, accessState) {
        if (accessState is CapabilityAccessState.Available) {
            load()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KVColor.Canvas)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KVSpacing.xl),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KVSpacing.ml)
                .padding(top = KVSpacing.m, bottom = KVSpacing.huge)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = KVColor.TextPrimary
                )
            }

            KieliValmisIconTile(Icons.Rounded.MenuBook)

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.s)) {
                Text(
                    text = "Everyday Finnish",
                    color = KVColor.TextPrimary,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Build practical Finnish through carefully structured vocabulary, sentence and grammar practice.",
                    color = KVColor.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }

            when (accessState) {
                CapabilityAccessState.Available -> {
                    KieliValmisStatusBanner(
                        message = "Everyday Finnish is available for this account.",
                        tone = KVStatusTone.Success
                    )
                }
                is CapabilityAccessState.Locked -> {
                    KieliValmisStatusBanner(
                        message = accessState.message,
                        tone = KVStatusTone.Warning
                    )
                }
                CapabilityAccessState.Unknown -> {
                    KieliValmisStatusBanner(
                        message = "We cannot confirm your Everyday Finnish access right now.",
                        tone = KVStatusTone.Info,
                        actionTitle = "Retry",
                        onAction = retryAccess
                    )
                }
            }

            if (accessState is CapabilityAccessState.Available) {
                Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.sm)) {
                    Text(
                        text = "Level",
                        color = KVColor.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    KieliValmisSegmentedControl(
                        options = listOf(
                            "A1_A2" to "A1–A2",
                            "B1_B2" to "B1–B2",
                            "C1_C2" to "C1–C2"
                        ),
                        selection = levelBand,
                        onSelect = { levelBand = it }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.sm)) {
                    Text(
                        text = "Practice material",
                        color = KVColor.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    KieliValmisSegmentedControl(
                        options = listOf(
                            "vocabulary_card" to "Words",
                            "sentence_card" to "Sentences",
                            "grammar_card" to "Grammar"
                        ),
                        selection = contentType,
                        onSelect = { contentType = it }
                    )
                }

                when {
                    isLoading && deck == null -> {
                        KieliValmisCardSurface(style = KVCardStyle.Standard) {
                            CircularProgressIndicator(
                                color = KVColor.BrandBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Loading learning material…",
                                color = KVColor.TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    errorMessage != null && deck == null -> {
                        KieliValmisStatusBanner(
                            message = errorMessage!!,
                            tone = KVStatusTone.Danger,
                            actionTitle = "Retry",
                            onAction = { scope.launch { load() } }
                        )
                    }

                    deck != null -> {
                        val data = deck!!

                        KieliValmisCardSurface(style = KVCardStyle.Hero) {
                            Text(
                                text = contentLabel.uppercase(),
                                color = KVColor.BrandBright,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${data.cards.size} items available",
                                color = KVColor.TextPrimary,
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Explore Finnish material for the selected level and activity type.",
                                color = KVColor.TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        if (data.cards.isEmpty()) {
                            KieliValmisStatusBanner(
                                message = "No release-safe material is currently available for this selection.",
                                tone = KVStatusTone.Info
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(KVSpacing.m)
                            ) {
                                KieliValmisSectionHeader("Examples")

                                data.cards.take(6).forEach { card ->
                                    EverydayMaterialRow(
                                        title = card.frontText,
                                        level = card.levelBand.replace("_", "–"),
                                        icon = when (card.contentType) {
                                            "grammar_card" -> Icons.Rounded.TextFields
                                            "sentence_card" -> Icons.Rounded.RecordVoiceOver
                                            else -> Icons.Rounded.Translate
                                        }
                                    )
                                }
                            }
                        }

                        errorMessage?.let {
                            KieliValmisStatusBanner(
                                message = it,
                                tone = KVStatusTone.Warning,
                                actionTitle = "Retry",
                                onAction = { scope.launch { load() } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EverydayMaterialRow(
    title: String,
    level: String,
    icon: ImageVector
) {
    KieliValmisCardSurface(style = KVCardStyle.Compact) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KVSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            KieliValmisIconTile(icon)

            Column(
                verticalArrangement = Arrangement.spacedBy(KVSpacing.xs),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = KVColor.TextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = level,
                    color = KVColor.TextTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
