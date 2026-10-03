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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MenuBook
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
import com.floently.shared.learn.LearnOverviewService
import com.floently.shared.learn.ProfessionalOverview
import com.floently.shared.learn.YKIPracticeOverview
import kotlinx.coroutines.launch

@Composable
fun KieliValmisYkiOverviewScreen(
    service: LearnOverviewService,
    accessState: CapabilityAccessState,
    retryAccess: () -> Unit,
    onBack: () -> Unit
) {
    var levelBand by remember { mutableStateOf("B1-B2") }
    var overview by remember { mutableStateOf<YKIPracticeOverview?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        isLoading = true
        errorMessage = null
        runCatching {
            service.fetchYkiOverview(levelBand)
        }.onSuccess {
            overview = it
        }.onFailure {
            errorMessage = it.message ?: "Could not load YKI practice information."
        }
        isLoading = false
    }

    LaunchedEffect(levelBand, accessState) {
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

            KieliValmisIconTile(Icons.Rounded.Badge)

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.s)) {
                Text(
                    text = "YKI preparation",
                    color = KVColor.TextPrimary,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Practice the four YKI skills using the existing certified practice bank.",
                    color = KVColor.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }

            PathwayAccessBanner(
                accessState = accessState,
                availableMessage = "YKI is available for this account.",
                unknownMessage = "We cannot confirm your YKI access right now.",
                retryAccess = retryAccess
            )

            if (accessState is CapabilityAccessState.Available) {
                Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.sm)) {
                    Text(
                        text = "Practice level",
                        color = KVColor.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    KieliValmisSegmentedControl(
                        options = listOf(
                            "A1-A2" to "A1-A2",
                            "B1-B2" to "B1-B2",
                            "C1-C2" to "C1-C2"
                        ),
                        selection = levelBand,
                        onSelect = { levelBand = it }
                    )
                }

                when {
                    isLoading && overview == null -> {
                        KieliValmisCardSurface(style = KVCardStyle.Standard) {
                            CircularProgressIndicator(
                                color = KVColor.BrandBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Loading YKI practice…",
                                color = KVColor.TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    errorMessage != null && overview == null -> {
                        KieliValmisStatusBanner(
                            message = errorMessage!!,
                            tone = KVStatusTone.Danger,
                            actionTitle = "Retry",
                            onAction = { scope.launch { load() } }
                        )
                    }

                    overview != null -> {
                        val data = overview!!

                        KieliValmisCardSurface(style = KVCardStyle.Hero) {
                            Text(
                                text = data.displayLevelBand,
                                color = KVColor.BrandBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Certified practice bank",
                                color = KVColor.TextPrimary,
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${data.totalTasks} practice tasks · about ${data.dailyPractice.minutes} min for a guided block",
                                color = KVColor.TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(KVSpacing.m)
                        ) {
                            KieliValmisSectionHeader("Skills in this level")

                            listOf(
                                "reading" to Icons.Rounded.MenuBook,
                                "listening" to Icons.Rounded.Headphones,
                                "writing" to Icons.Rounded.Edit,
                                "speaking" to Icons.Rounded.GraphicEq
                            ).forEach { (skill, icon) ->
                                YkiSkillAvailabilityRow(
                                    skill = skill,
                                    icon = icon,
                                    count = data.countsBySkill[skill] ?: 0
                                )
                            }
                        }

                        if (data.nextTask.isNotBlank()) {
                            KieliValmisCardSurface(style = KVCardStyle.Compact) {
                                Text(
                                    text = "Guided practice format",
                                    color = KVColor.TextTertiary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = data.nextTask,
                                    color = KVColor.TextPrimary,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp
                                )
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
private fun YkiSkillAvailabilityRow(
    skill: String,
    icon: ImageVector,
    count: Int
) {
    KieliValmisCardSurface(style = KVCardStyle.Compact) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KVSpacing.sm)
        ) {
            KieliValmisIconTile(icon)
            Column(
                verticalArrangement = Arrangement.spacedBy(KVSpacing.xs),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = skill.replaceFirstChar { it.uppercase() },
                    color = KVColor.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count tasks available",
                    color = KVColor.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun KieliValmisProfessionalOverviewScreen(
    service: LearnOverviewService,
    accessState: CapabilityAccessState,
    retryAccess: () -> Unit,
    onBack: () -> Unit
) {
    var overview by remember { mutableStateOf<ProfessionalOverview?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        isLoading = true
        errorMessage = null
        runCatching {
            service.fetchProfessionalOverview()
        }.onSuccess {
            overview = it
        }.onFailure {
            errorMessage = it.message ?: "Could not load professional Finnish tracks."
        }
        isLoading = false
    }

    LaunchedEffect(accessState) {
        if (accessState is CapabilityAccessState.Available && overview == null) {
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

            KieliValmisIconTile(Icons.Rounded.BusinessCenter)

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.s)) {
                Text(
                    text = "Work in Finland",
                    color = KVColor.TextPrimary,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Professional Finnish for real workplace communication and role-specific situations.",
                    color = KVColor.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }

            PathwayAccessBanner(
                accessState = accessState,
                availableMessage = "Professional Finnish is available for this account.",
                unknownMessage = "We cannot confirm your Professional Finnish access right now.",
                retryAccess = retryAccess
            )

            if (accessState is CapabilityAccessState.Available) {
                when {
                    isLoading && overview == null -> {
                        KieliValmisCardSurface(style = KVCardStyle.Standard) {
                            CircularProgressIndicator(
                                color = KVColor.BrandBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Loading professional tracks…",
                                color = KVColor.TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    errorMessage != null && overview == null -> {
                        KieliValmisStatusBanner(
                            message = errorMessage!!,
                            tone = KVStatusTone.Danger,
                            actionTitle = "Retry",
                            onAction = { scope.launch { load() } }
                        )
                    }

                    overview != null -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(KVSpacing.m)
                        ) {
                            KieliValmisSectionHeader("Work domains")

                            overview!!.tracks.forEach { track ->
                                KieliValmisCardSurface(style = KVCardStyle.Standard) {
                                    Text(
                                        text = track.title,
                                        color = KVColor.TextPrimary,
                                        fontSize = 18.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (track.coreTasks.isNotEmpty()) {
                                        Text(
                                            text = track.coreTasks
                                                .take(3)
                                                .joinToString(" · ") { task ->
                                                    task.replaceFirstChar { it.uppercase() }
                                                },
                                            color = KVColor.TextSecondary,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }

                                    Text(
                                        text = "${track.speakingScenarios.size} speaking · ${track.writingTasks.size} writing",
                                        color = KVColor.TextTertiary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium
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
private fun PathwayAccessBanner(
    accessState: CapabilityAccessState,
    availableMessage: String,
    unknownMessage: String,
    retryAccess: () -> Unit
) {
    when (accessState) {
        CapabilityAccessState.Available -> {
            KieliValmisStatusBanner(
                message = availableMessage,
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
                message = unknownMessage,
                tone = KVStatusTone.Info,
                actionTitle = "Retry",
                onAction = retryAccess
            )
        }
    }
}
