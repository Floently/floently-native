package com.floently.learn.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.design.KVCardStyle
import com.floently.learn.design.KVColor
import com.floently.learn.design.KVStatusTone
import com.floently.learn.design.KVSpacing
import com.floently.learn.design.KieliValmisCardSurface
import com.floently.learn.design.KieliValmisIconTile
import com.floently.learn.design.KieliValmisPathwayCard
import com.floently.learn.design.KieliValmisPrimaryButton
import com.floently.learn.design.KieliValmisReviewCard
import com.floently.learn.design.KieliValmisSectionHeader
import com.floently.learn.design.KieliValmisSecondaryButton
import com.floently.learn.design.KieliValmisSkillCard
import com.floently.learn.design.KieliValmisStatusBanner
import com.floently.learn.state.LearnAppViewModel

enum class KieliValmisDestination {
    Yki,
    Professional,
    Speaking,
    Listening,
    Reading,
    Writing,
    Review,
    Profile
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KieliValmisHomeScreen(
    appState: LearnAppViewModel
) {
    val ui by appState.uiState.collectAsState()

    var destination by remember { mutableStateOf<KieliValmisDestination?>(null) }
    var showPathPicker by remember { mutableStateOf(false) }

    destination?.let { target ->
        val accessState = when (target) {
            KieliValmisDestination.Yki -> pathwayAccessState(
                accessKnown = ui.accessStatus != null,
                allowed = ui.accessStatus?.let { access ->
                    access.ykiAccess || access.combinedAccess || access.isInternalAllAccess
                } == true,
                lockedMessage = "YKI access is not active for this account."
            )
            KieliValmisDestination.Professional -> pathwayAccessState(
                accessKnown = ui.accessStatus != null,
                allowed = ui.accessStatus?.let { access ->
                    access.professionalAccess || access.combinedAccess || access.isInternalAllAccess
                } == true,
                lockedMessage = "Professional Finnish access is not active for this account."
            )
            else -> CapabilityAccessState.Available
        }

        when (target) {
            KieliValmisDestination.Profile -> {
                KieliValmisProfileScreen(
                    appState = appState,
                    onBack = { destination = null }
                )
            }
            KieliValmisDestination.Yki -> {
                KieliValmisYkiOverviewScreen(
                    service = appState.overviewService,
                    accessState = accessState,
                    retryAccess = appState::refreshAccess,
                    onBack = { destination = null }
                )
            }
            KieliValmisDestination.Professional -> {
                KieliValmisProfessionalOverviewScreen(
                    service = appState.overviewService,
                    accessState = accessState,
                    retryAccess = appState::refreshAccess,
                    onBack = { destination = null }
                )
            }
            else -> {
                KieliValmisCapabilityScreen(
                    destination = target,
                    accessState = accessState,
                    retryAccess = appState::refreshAccess,
                    onBack = { destination = null }
                )
            }
        }
        return
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
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(KVSpacing.s),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "KieliValmis",
                        color = KVColor.TextPrimary,
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Finnish for life, work and YKI — built around what you should do next.",
                        color = KVColor.TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                }

                IconButton(onClick = { destination = KieliValmisDestination.Profile }) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Profile",
                        tint = KVColor.TextPrimary
                    )
                }
            }

            ui.connectionNotice?.let {
                KieliValmisStatusBanner(
                    message = it,
                    tone = KVStatusTone.Warning,
                    actionTitle = "Retry",
                    onAction = appState::refreshAccess
                )
            }

            ui.accessNotice?.let {
                KieliValmisStatusBanner(
                    message = it,
                    tone = KVStatusTone.Info,
                    actionTitle = "Retry",
                    onAction = appState::refreshAccess
                )
            }

            KieliValmisCardSurface(style = KVCardStyle.Hero) {
                Text(
                    text = "CONTINUE",
                    color = KVColor.BrandBright,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Start a learning session",
                    color = KVColor.TextPrimary,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Choose a pathway now. When your learning history is connected, this space will resume the most useful next activity.",
                    color = KVColor.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(KVSpacing.s))

                KieliValmisPrimaryButton(
                    title = "Choose a path",
                    onClick = { showPathPicker = true }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.m)) {
                KieliValmisSectionHeader("Your pathways")

                ResponsivePair(
                    first = { modifier ->
                        KieliValmisPathwayCard(
                            icon = Icons.Rounded.Badge,
                            title = "YKI preparation",
                            subtitle = ykiPathwaySubtitle(ui.accessStatus),
                            onClick = { destination = KieliValmisDestination.Yki },
                            modifier = modifier
                        )
                    },
                    second = { modifier ->
                        KieliValmisPathwayCard(
                            icon = Icons.Rounded.BusinessCenter,
                            title = "Work in Finland",
                            subtitle = professionalPathwaySubtitle(ui.accessStatus),
                            onClick = { destination = KieliValmisDestination.Professional },
                            modifier = modifier
                        )
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.m)) {
                KieliValmisSectionHeader("Practice by skill")

                ResponsivePair(
                    first = { modifier ->
                        KieliValmisSkillCard(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Speaking",
                            subtitle = "Guided speaking and roleplay.",
                            onClick = { destination = KieliValmisDestination.Speaking },
                            modifier = modifier
                        )
                    },
                    second = { modifier ->
                        KieliValmisSkillCard(
                            icon = Icons.Rounded.Headphones,
                            title = "Listening",
                            subtitle = "Understand Finnish in real situations.",
                            onClick = { destination = KieliValmisDestination.Listening },
                            modifier = modifier
                        )
                    }
                )

                ResponsivePair(
                    first = { modifier ->
                        KieliValmisSkillCard(
                            icon = Icons.Rounded.MenuBook,
                            title = "Reading",
                            subtitle = "Build comprehension with focused tasks.",
                            onClick = { destination = KieliValmisDestination.Reading },
                            modifier = modifier
                        )
                    },
                    second = { modifier ->
                        KieliValmisSkillCard(
                            icon = Icons.Rounded.Edit,
                            title = "Writing",
                            subtitle = "Practice useful and YKI-style writing.",
                            onClick = { destination = KieliValmisDestination.Writing },
                            modifier = modifier
                        )
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.m)) {
                KieliValmisSectionHeader("Review")
                KieliValmisReviewCard(
                    icon = Icons.Rounded.Autorenew,
                    onClick = { destination = KieliValmisDestination.Review }
                )
            }
        }
    }

    if (showPathPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPathPicker = false },
            containerColor = KVColor.Surface1
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(KVSpacing.m),
                modifier = Modifier
                    .padding(horizontal = KVSpacing.ml)
                    .padding(bottom = KVSpacing.xl)
            ) {
                Text(
                    text = "Choose a pathway",
                    color = KVColor.TextPrimary,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Start with the area you want to work on now.",
                    color = KVColor.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                KieliValmisPathwayCard(
                    icon = Icons.Rounded.Badge,
                    title = "YKI preparation",
                    subtitle = "Practice all four YKI skills.",
                    onClick = {
                        showPathPicker = false
                        destination = KieliValmisDestination.Yki
                    }
                )

                KieliValmisPathwayCard(
                    icon = Icons.Rounded.BusinessCenter,
                    title = "Work in Finland",
                    subtitle = "Professional and workplace Finnish.",
                    onClick = {
                        showPathPicker = false
                        destination = KieliValmisDestination.Professional
                    }
                )
            }
        }
    }
}

@Composable
private fun ResponsivePair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 350.dp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KVSpacing.m),
                modifier = Modifier.fillMaxWidth()
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(KVSpacing.m),
                modifier = Modifier.fillMaxWidth()
            ) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun KieliValmisCapabilityScreen(
    destination: KieliValmisDestination,
    accessState: CapabilityAccessState,
    retryAccess: () -> Unit,
    onBack: () -> Unit
) {
    val spec = capabilitySpec(destination)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KVColor.Canvas)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KVSpacing.l),
            modifier = Modifier
                .padding(horizontal = KVSpacing.ml)
                .padding(top = KVSpacing.m)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = KVColor.TextPrimary
                )
            }

            KieliValmisIconTile(spec.icon)

            Text(
                text = spec.title,
                color = KVColor.TextPrimary,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = spec.subtitle,
                color = KVColor.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )


            when (accessState) {
                CapabilityAccessState.Available -> {
                    KieliValmisStatusBanner(
                        message = "This pathway is available for your account. Native learning activities are being connected to the existing backend.",
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
                        message = "We cannot confirm your pathway access right now.",
                        tone = KVStatusTone.Info,
                        actionTitle = "Retry",
                        onAction = retryAccess
                    )
                }
            }
        }
    }
}

sealed interface CapabilityAccessState {
    data object Available : CapabilityAccessState
    data object Unknown : CapabilityAccessState
    data class Locked(val message: String) : CapabilityAccessState
}

private fun pathwayAccessState(
    accessKnown: Boolean,
    allowed: Boolean,
    lockedMessage: String
): CapabilityAccessState {
    if (!accessKnown) return CapabilityAccessState.Unknown
    return if (allowed) {
        CapabilityAccessState.Available
    } else {
        CapabilityAccessState.Locked(lockedMessage)
    }
}

private fun ykiPathwaySubtitle(
    access: com.floently.shared.billing.FloentlyAccessStatus?
): String {
    if (access == null) {
        return "Reading, listening, writing and speaking practice."
    }
    return if (access.ykiAccess || access.combinedAccess || access.isInternalAllAccess) {
        "Reading, listening, writing and speaking practice."
    } else {
        "YKI access is not active for this account."
    }
}

private fun professionalPathwaySubtitle(
    access: com.floently.shared.billing.FloentlyAccessStatus?
): String {
    if (access == null) {
        return "Professional Finnish and workplace communication."
    }
    return if (access.professionalAccess || access.combinedAccess || access.isInternalAllAccess) {
        "Professional Finnish and workplace communication."
    } else {
        "Professional Finnish access is not active for this account."
    }
}

@Composable
private fun KieliValmisProfileScreen(
    appState: LearnAppViewModel,
    onBack: () -> Unit
) {
    val ui by appState.uiState.collectAsState()
    val user = appState.currentUser
    val access = ui.accessStatus

    val accessSummary = when {
        access == null -> "Access status unavailable"
        access.isInternalAllAccess -> "Internal all-access"
        access.ykiAccess && access.professionalAccess -> "YKI + Professional Finnish"
        access.ykiAccess -> "YKI"
        access.professionalAccess -> "Professional Finnish"
        else -> "No active Learn pathway"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KVColor.Canvas)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KVSpacing.l),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KVSpacing.ml)
                .padding(top = KVSpacing.m, bottom = KVSpacing.xl)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = KVColor.TextPrimary
                )
            }

            KieliValmisIconTile(Icons.Rounded.Person)

            Text(
                text = "Profile",
                color = KVColor.TextPrimary,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )

            user?.let {
                Text(
                    text = it.name?.takeIf { name -> name.isNotBlank() } ?: it.email,
                    color = KVColor.TextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!it.name.isNullOrBlank()) {
                    Text(
                        text = it.email,
                        color = KVColor.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            KieliValmisCardSurface(style = KVCardStyle.Compact) {
                Text(
                    text = "Access",
                    color = KVColor.TextTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = accessSummary,
                    color = KVColor.TextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (!access?.accessibleProfessions.isNullOrEmpty()) {
                    Text(
                        text = access!!.accessibleProfessions.joinToString(" · ") {
                            it.replace("_", " ").split(" ").joinToString(" ") { part ->
                                part.replaceFirstChar { char -> char.uppercase() }
                            }
                        },
                        color = KVColor.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            ui.accessNotice?.let {
                KieliValmisStatusBanner(
                    message = it,
                    tone = KVStatusTone.Info,
                    actionTitle = "Retry",
                    onAction = appState::refreshAccess
                )
            }

            KieliValmisSecondaryButton(
                title = "Sign out",
                destructive = true,
                onClick = appState::logout
            )
        }
    }
}

private data class CapabilitySpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private fun capabilitySpec(destination: KieliValmisDestination): CapabilitySpec = when (destination) {
    KieliValmisDestination.Yki -> CapabilitySpec(
        "YKI preparation",
        "Practice reading, listening, writing and speaking in one structured pathway.",
        Icons.Rounded.Badge
    )
    KieliValmisDestination.Professional -> CapabilitySpec(
        "Work in Finland",
        "Profession-specific language, workplace communication and real-world scenarios.",
        Icons.Rounded.BusinessCenter
    )
    KieliValmisDestination.Speaking -> CapabilitySpec(
        "Speaking",
        "Guided speaking and roleplay sessions will appear here.",
        Icons.Rounded.GraphicEq
    )
    KieliValmisDestination.Listening -> CapabilitySpec(
        "Listening",
        "Listening activities and comprehension practice will appear here.",
        Icons.Rounded.Headphones
    )
    KieliValmisDestination.Reading -> CapabilitySpec(
        "Reading",
        "Focused Finnish reading activities will appear here.",
        Icons.Rounded.MenuBook
    )
    KieliValmisDestination.Writing -> CapabilitySpec(
        "Writing",
        "Writing prompts, feedback and YKI writing practice will appear here.",
        Icons.Rounded.Edit
    )
    KieliValmisDestination.Review -> CapabilitySpec(
        "Review",
        "Your due review and mistake-focused practice will appear here once learning history is available.",
        Icons.Rounded.Autorenew
    )
    KieliValmisDestination.Profile -> CapabilitySpec(
        "Profile",
        "Account, preferences and access settings will live here.",
        Icons.Rounded.Person
    )
}
