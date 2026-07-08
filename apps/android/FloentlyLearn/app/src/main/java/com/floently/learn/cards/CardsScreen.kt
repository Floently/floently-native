package com.floently.learn.cards

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.floently.learn.audio.NativeTtsIconButton
import com.floently.learn.i18n.LearnCopy
import com.floently.learn.i18n.LearnLanguage
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private object WebCardColors {
    val backgroundTop = Color(0xFFF4F7FB)
    val backgroundCard = Color.White
    val paleBorder = Color(0x1F5572B0)
    val text = Color(0xFF243552)
    val muted = Color(0xFF6E82A4)
    val primary = Color(0xFF345EC3)
    val primaryDeep = Color(0xFF2D4FA5)
    val mastered = Color(0xFF4E8F6A)
    val difficult = Color(0xFFD64545)
    val learning = Color(0xFFB88A1A)
    val softBlue = Color(0xFFEDF3FF)
    val softBlueStrong = Color(0xFFDFEAFF)
    val barTrack = Color(0xFFD8E0EE)
}

@Composable
fun CardsScreen(
    repository: CardsRepository,
    copy: LearnCopy,
    selectedLanguage: LearnLanguage,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedDeckType by remember { mutableStateOf(CardsDeckType.Vocabulary) }
    var selectedOverlayCode by remember(selectedLanguage.code) { mutableStateOf(selectedLanguage.code) }
    var dashboardState by remember { mutableStateOf<CardsDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<CardsPracticeSession?>(null) }
    var banksVisible by remember { mutableStateOf(false) }

    LaunchedEffect(repository, selectedDeckType) {
        dashboardState = null
        statusMessage = null
        activeSession = null

        val dashboard = repository.dashboard(selectedDeckType)
        dashboardState = dashboard
        statusMessage = dashboard.errorMessage

        val firstDeck = dashboard.decks.firstOrNull { !it.locked }
        if (firstDeck != null) {
            when (val result = repository.startSession(firstDeck.id, CardsPracticeMode.Flip)) {
                is CardsSessionResult.Ready -> activeSession = result.session
                is CardsSessionResult.Blocked -> statusMessage = result.reason
                is CardsSessionResult.Error -> statusMessage = result.message
            }
        }
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Box(modifier = Modifier.fillMaxSize()) {
            StrictCardsBackground(isDark = true, palette = palette)

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StrictCardsBackBar(onBack = onBack, palette = palette)
                StrictCardModeTabs(
                    selected = selectedDeckType,
                    palette = palette,
                    onChange = { next ->
                        selectedDeckType = next
                    }
                )

                if (banksVisible) {
                    StrictCardBanksPanel(
                        dashboard = dashboardState,
                        palette = palette,
                        onClose = { banksVisible = false }
                    )
                }

                val session = activeSession
                when {
                    dashboardState == null -> StrictCardsLoading(palette = palette)
                    session != null -> StrictPracticeSession(
                        session = session,
                        repository = repository,
                        selectedOverlayCode = selectedOverlayCode,
                        selectedDeckType = selectedDeckType,
                        statusMessage = statusMessage,
                        palette = palette,
                        onSessionChange = { activeSession = it },
                        onStatusMessage = { statusMessage = it },
                        onToggleBanks = { banksVisible = !banksVisible },
                        onExit = onBack,
                        onOverlayLanguageSelected = { selectedOverlayCode = it },
                        selectedLanguage = selectedLanguage
                    )
                    else -> StrictCardsEmpty(
                        message = statusMessage ?: "No cards are available in this mode yet.",
                        palette = palette,
                        onReviewBanks = { banksVisible = true },
                        onBack = onBack
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}


@Composable
private fun StrictCardsBackBar(
    onBack: () -> Unit,
    palette: FloentlyPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, Color(0xFF40527A)),
            modifier = Modifier
                .height(34.dp)
                .clickable(onClick = onBack)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Back",
                    color = Color(0xFFB9C8EC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = Color(0xFF5E83FF),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, Color(0xFF7DA0FF))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "☰ Menu",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun StrictCardModeTabs(
    selected: CardsDeckType,
    palette: FloentlyPalette,
    onChange: (CardsDeckType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        StrictModePill("Vocabulary", CardsDeckType.Vocabulary, selected, palette, onChange)
        Spacer(modifier = Modifier.width(8.dp))
        StrictModePill("Sentences", CardsDeckType.Phrases, selected, palette, onChange)
        Spacer(modifier = Modifier.width(8.dp))
        StrictModePill("Grammar", CardsDeckType.Grammar, selected, palette, onChange)
    }
}


@Composable
private fun StrictModePill(
    label: String,
    value: CardsDeckType,
    selected: CardsDeckType,
    palette: FloentlyPalette,
    onChange: (CardsDeckType) -> Unit
) {
    val active = value == selected
    Surface(
        color = if (active) Color(0xFFEAF0FF).copy(alpha = 0.16f) else Color(0x1AFFFFFF),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (active) Color(0xFF8EACE7) else Color(0xFF31476B)),
        modifier = Modifier
            .height(32.dp)
            .clickable { onChange(value) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (active) Color(0xFF9DB7FF) else Color(0xFF8799BA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun StrictCardsBackground(
    isDark: Boolean,
    palette: FloentlyPalette
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-120).dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x591E325A) else Color(0xC7FFFFFF))
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 40.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x4D142850) else Color(0xE0EAF2FF))
        )
        Box(
            modifier = Modifier
                .width(360.dp)
                .height(180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = (-70).dp)
                .graphicsLayer(rotationZ = -18f)
                .clip(RoundedCornerShape(120.dp))
                .border(18.dp, palette.primary.copy(alpha = 0.18f), RoundedCornerShape(120.dp))
        )
        Box(
            modifier = Modifier
                .width(360.dp)
                .height(180.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = (-40).dp)
                .graphicsLayer(rotationZ = 12f)
                .clip(RoundedCornerShape(120.dp))
                .border(14.dp, palette.primary.copy(alpha = 0.12f), RoundedCornerShape(120.dp))
        )
    }
}

@Composable
private fun StrictCardsLoading(palette: FloentlyPalette) {
    Box(modifier = Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = palette.primary)
            Text(text = "Loading cards…", color = palette.muted)
        }
    }
}

@Composable
private fun StrictCardsEmpty(
    message: String,
    palette: FloentlyPalette,
    onReviewBanks: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth().heightIn(min = 430.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            Text(
                text = message,
                color = palette.text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            StrictSmallChip("Review banks", palette, onReviewBanks)
            StrictSmallChip("End session", palette, onBack)
        }
    }
}

@Composable
private fun StrictPracticeSession(
    session: CardsPracticeSession,
    repository: CardsRepository,
    selectedOverlayCode: String,
    selectedDeckType: CardsDeckType,
    selectedLanguage: LearnLanguage,
    statusMessage: String?,
    palette: FloentlyPalette,
    onSessionChange: (CardsPracticeSession) -> Unit,
    onStatusMessage: (String?) -> Unit,
    onToggleBanks: () -> Unit,
    onExit: () -> Unit,
    onOverlayLanguageSelected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val card = session.currentCard
    var showBack by remember(session.id, session.currentCardIndex) { mutableStateOf(false) }
    var showHintPopup by remember(session.id, session.currentCardIndex) { mutableStateOf(false) }
    val overlay = card?.overlayFor(selectedOverlayCode)
    val cardTone = toneColor(card)
    val progressRatio = if (session.cards.isEmpty()) 0f else min(1f, max(0.08f, (session.currentCardIndex + 1).toFloat() / session.cards.size.toFloat()))
    val activeIndicator = min(3, (progressRatio * 4f).toInt())
    val header = when (selectedDeckType) {
        CardsDeckType.Phrases -> "Sentences"
        CardsDeckType.Grammar -> "Grammar"
        else -> "Vocabulary"
    }

    if (session.completed || card == null) {
        StrictReviewComplete(
            repository = repository,
            session = session,
            palette = palette,
            onExit = onExit
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StrictRecallButton("↶ Recall", palette, onClick = {})
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = header,
                color = palette.soft,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            StrictRecallButton("Recall ↷", palette, onClick = {})
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 70.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.border.copy(alpha = 0.55f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressRatio)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.primary)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 452.dp)
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = palette.cardMuted,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth(0.90f)
            ) {
                Surface(
                    color = palette.card,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.padding(7.dp).heightIn(min = 438.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 438.dp).padding(horizontal = 18.dp, vertical = 16.dp)) {
                        if (selectedDeckType != CardsDeckType.Grammar) {
                            NativeTtsIconButton(
                                text = card.front,
                                label = "🔊",
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }

                        if (!showBack) {
                            StrictIconActionButton(
                                text = "Skip",
                                palette = palette,
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = {
                                    scope.launch {
                                        when (val result = repository.skipCard(session)) {
                                            is CardsSessionResult.Ready -> {
                                                showBack = false
                                                showHintPopup = false
                                                onStatusMessage(null)
                                                onSessionChange(result.session)
                                            }
                                            is CardsSessionResult.Blocked -> onStatusMessage(result.reason)
                                            is CardsSessionResult.Error -> onStatusMessage(result.message)
                                        }
                                    }
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(top = 54.dp, bottom = 88.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!showBack) {
                                AdaptiveCardCopy(
                                    text = card.front,
                                    variant = AdaptiveVariant.Front,
                                    color = cardTone,
                                    mode = selectedDeckType
                                )
                                Text(
                                    text = card.state.displayLabel(),
                                    color = palette.soft,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                StrictPromptBlock(
                                    card = card,
                                    overlay = overlay,
                                    palette = palette,
                                    selectedDeckType = selectedDeckType
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .border(1.dp, palette.border.copy(alpha = 0.65f), RoundedCornerShape(0.dp))
                                .padding(top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StrictFooterGhostButton(
                                text = "Show hint",
                                palette = palette,
                                onClick = { showHintPopup = true }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            StrictPrimaryCardAction(
                                text = if (showBack) "Check" else "↻",
                                palette = palette,
                                active = true,
                                onClick = { showBack = !showBack }
                            )
                        }
                    }
                }
            }
        }

        val hintText = overlay?.hint ?: card.hint
        if (showHintPopup && hintText.isNotBlank()) {
            StrictHintPopup(
                text = hintText,
                palette = palette,
                onDismiss = { showHintPopup = false }
            )
        }

        if (showBack) {
            StrictRatingPanel(
                palette = palette,
                onRate = { rating ->
                    scope.launch {
                        when (val result = repository.reviewCard(session, card.id, rating)) {
                            is CardsSessionResult.Ready -> {
                                showBack = false
                                showHintPopup = false
                                onStatusMessage(null)
                                onSessionChange(result.session)
                            }
                            is CardsSessionResult.Blocked -> onStatusMessage(result.reason)
                            is CardsSessionResult.Error -> onStatusMessage(result.message)
                        }
                    }
                }
            )
        }

        if (!statusMessage.isNullOrBlank()) {
            Surface(
                color = palette.cardMuted,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage,
                    color = palette.text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (index == activeIndicator) palette.primary else palette.border)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StrictSmallChip("Review banks", palette, onToggleBanks)
            StrictOverlayLanguageStrip(
                selectedOverlayCode = selectedOverlayCode,
                selectedLanguage = selectedLanguage,
                palette = palette,
                onSelect = onOverlayLanguageSelected
            )
            StrictEndSessionButton("End session", palette, onExit)
        }
    }
}

@Composable
private fun StrictHintPopup(
    text: String,
    palette: FloentlyPalette,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x9E040A18))
                    .clickable(onClick = onDismiss)
            )

            Surface(
                color = palette.card,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Surface(
                        color = palette.primary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "HINT",
                            color = palette.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = text,
                        color = palette.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    StrictPrimaryCardAction(
                        text = "Close",
                        palette = palette,
                        active = true,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun StrictOverlayLanguageStrip(
    selectedOverlayCode: String,
    selectedLanguage: LearnLanguage,
    palette: FloentlyPalette,
    onSelect: (String) -> Unit
) {
    val languages = listOf(selectedLanguage) + LearnLanguage.entries.filter { it.code != selectedLanguage.code }.take(3)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        languages.forEach { language ->
            val active = language.code == selectedOverlayCode
            Surface(
                color = if (active) palette.primary.copy(alpha = 0.18f) else palette.cardMuted,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, if (active) palette.primary else palette.border),
                modifier = Modifier.weight(1f).clickable { onSelect(language.code) }
            ) {
                Text(
                    text = language.displayLabel.take(10),
                    color = if (active) palette.primary else palette.muted,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StrictPromptBlock(
    card: StudyCard,
    overlay: CardI18nOverlay?,
    palette: FloentlyPalette,
    selectedDeckType: CardsDeckType
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdaptiveCardCopy(
            text = overlay?.meaning ?: card.back,
            variant = AdaptiveVariant.Prompt,
            color = palette.text,
            mode = selectedDeckType
        )
        val example = overlay?.example ?: card.example
        if (example.isNotBlank()) {
            AdaptiveCardCopy(
                text = example,
                variant = AdaptiveVariant.Context,
                color = palette.muted,
                mode = selectedDeckType
            )
            NativeTtsIconButton(text = example, label = "🔊")
        }
        if (card.tags.isNotEmpty()) {
            Text(
                text = card.tags.joinToString(" · "),
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

private enum class AdaptiveVariant {
    Front,
    Prompt,
    Context,
    Option,
    Hint
}

@Composable
private fun AdaptiveCardCopy(
    text: String,
    variant: AdaptiveVariant,
    color: Color,
    mode: CardsDeckType
) {
    val sentence = mode == CardsDeckType.Phrases
    val length = text.trim().length
    val size = when (variant) {
        AdaptiveVariant.Front -> when {
            sentence && length > 220 -> 22
            sentence && length > 160 -> 24
            sentence && length > 100 -> 26
            sentence && length > 60 -> 30
            sentence -> 34
            length > 160 -> 22
            length > 100 -> 26
            length > 60 -> 32
            else -> 40
        }
        AdaptiveVariant.Prompt -> when {
            sentence && length > 220 -> 15
            sentence && length > 140 -> 16
            else -> 18
        }
        AdaptiveVariant.Context -> 14
        AdaptiveVariant.Option -> if (length > 110) 13 else 15
        AdaptiveVariant.Hint -> 13
    }
    val lineHeight = when (variant) {
        AdaptiveVariant.Front -> size + 8
        AdaptiveVariant.Prompt -> if (sentence && length > 140) size + 7 else 24
        AdaptiveVariant.Context -> 20
        AdaptiveVariant.Option -> 20
        AdaptiveVariant.Hint -> 18
    }

    Text(
        text = text,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = if (variant == AdaptiveVariant.Context || variant == AdaptiveVariant.Hint) FontWeight.Normal else FontWeight.Bold,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        maxLines = when (variant) {
            AdaptiveVariant.Front -> if (sentence) 8 else 6
            AdaptiveVariant.Prompt -> if (sentence) 8 else 6
            AdaptiveVariant.Context -> 6
            AdaptiveVariant.Option -> 4
            AdaptiveVariant.Hint -> 5
        },
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StrictHintBubble(
    text: String,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = palette.muted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}


@Composable
private fun StrictRecallButton(
    text: String,
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF25376F),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF31487F)),
        modifier = Modifier
            .height(30.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color(0xFFB8C6E7),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}


@Composable
private fun StrictIconActionButton(
    text: String,
    palette: FloentlyPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF25376F),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF31487F)),
        shadowElevation = 4.dp,
        modifier = modifier
            .height(32.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color(0xFFBAC6E3),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun StrictFooterGhostButton(
    text: String,
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF33476D)),
        modifier = Modifier
            .height(52.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color(0xFFB8C4DA),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        }
    }
}


@Composable
private fun StrictPrimaryCardAction(
    text: String,
    palette: FloentlyPalette,
    active: Boolean,
    onClick: () -> Unit
) {
    val isCheck = text.length > 2
    Surface(
        color = Color(0xFF5E83FF),
        shape = RoundedCornerShape(if (isCheck) 32.dp else 999.dp),
        border = BorderStroke(1.dp, Color(0xFF7DB4FF)),
        shadowElevation = 10.dp,
        modifier = Modifier
            .width(if (isCheck) 158.dp else 82.dp)
            .height(if (isCheck) 72.dp else 82.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = if (isCheck) 20.sp else 38.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}


@Composable
private fun StrictSmallChip(
    text: String,
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF25376F),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF31487F)),
        shadowElevation = 4.dp,
        modifier = Modifier
            .height(34.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color(0xFFC0CBE2),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


@Composable
private fun StrictEndSessionButton(
    text: String,
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF25376F),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF31487F)),
        shadowElevation = 5.dp,
        modifier = Modifier
            .fillMaxWidth(0.52f)
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color(0xFFC0CBE2),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun StrictRatingPanel(
    palette: FloentlyPalette,
    onRate: (CardsReviewRating) -> Unit
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Reviewed",
                color = palette.accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Choose how well you remembered this card.",
                color = palette.text,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CardsReviewRating.entries.forEach { rating ->
                    Surface(
                        color = when (rating) {
                            CardsReviewRating.Again -> WebCardColors.difficult.copy(alpha = 0.18f)
                            CardsReviewRating.Hard -> WebCardColors.learning.copy(alpha = 0.18f)
                            CardsReviewRating.Good -> palette.primary.copy(alpha = 0.18f)
                            CardsReviewRating.Easy -> WebCardColors.mastered.copy(alpha = 0.18f)
                        },
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.weight(1f).clickable { onRate(rating) }
                    ) {
                        Text(
                            text = rating.displayName(),
                            color = palette.text,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrictReviewComplete(
    repository: CardsRepository,
    session: CardsPracticeSession,
    palette: FloentlyPalette,
    onExit: () -> Unit
) {
    val summary = repository.summarize(session)
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth().heightIn(min = 430.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = "Lopeta istunto",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kertasit ${summary.reviewedCards}/${summary.totalCards} korttia.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            summary.accuracyPreviewPercent?.let {
                Text(
                    text = "Tarkkuus $it%",
                    color = palette.accent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = summary.nextReviewText,
                color = palette.muted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            StrictEndSessionButton("Sulje", palette, onExit)
        }
    }
}

@Composable
private fun StrictCardBanksPanel(
    dashboard: CardsDashboardState?,
    palette: FloentlyPalette,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x9E040A18))
                    .clickable(onClick = onClose)
            )

            Surface(
                color = palette.cardMuted,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(18.dp)
                    .heightIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tarkastele pinoja",
                                color = palette.text,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Käytä värikoodattua kertautusta vaikeiden ja helppojen korttien läpikäyntiin.",
                                color = palette.muted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        StrictSmallChip("Sulje", palette, onClose)
                    }

                    val buckets = dashboard?.buckets ?: CardBankBuckets()
                    BankSection("Vaikeat", "Toistuvasti väärin tai vielä epävakaa.", buckets.difficult, WebCardColors.difficult, palette)
                    BankSection("Opitut", "Toistuvasti oikein ja valmis kevyempään kertaukseen.", buckets.learned, WebCardColors.mastered, palette)
                    BankSection("Opittavat", "Nähty ja kehittyy, mutta ei vielä vakaa.", buckets.learning, WebCardColors.learning, palette)
                }
            }
        }
    }
}

@Composable
private fun BankSection(
    title: String,
    subtitle: String,
    items: List<StudyCard>,
    dot: Color,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(text = subtitle, color = palette.muted, style = MaterialTheme.typography.bodySmall)
            if (items.isEmpty()) {
                Text(text = "Tässä pinossa ei ole vielä kohteita.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            } else {
                items.take(20).forEach { card ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = card.front, color = palette.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Nähty ${card.seenCount} · ${card.correctRate?.let { "${(it * 100).toInt()}% tarkkuus" } ?: "tarkkuus –"}", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(text = card.state.displayLabel(), color = dot, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun toneColor(card: StudyCard?): Color {
    return when (card?.state) {
        CardsCardState.Mastered -> WebCardColors.mastered
        CardsCardState.Difficult -> WebCardColors.difficult
        CardsCardState.Learning -> WebCardColors.learning
        else -> WebCardColors.primary
    }
}

private fun CardsReviewRating.displayName(): String = when (this) {
    CardsReviewRating.Again -> "Uudestaan"
    CardsReviewRating.Hard -> "Vaikea"
    CardsReviewRating.Good -> "Hyvä"
    CardsReviewRating.Easy -> "Helppo"
}
