package com.floently.read

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.floently.shared.api.FloentlyApiClient
import com.floently.shared.auth.FloentlyAuthService
import com.floently.shared.auth.FloentlySecureSessionStore
import com.floently.shared.billing.FloentlyAccessService
import com.floently.shared.billing.FloentlyAccessStatus
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import com.floently.shared.design.floentlyPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ReadReleaseTab {
    Home,
    Library,
    Settings
}

private enum class ReadAccessPhase {
    Idle,
    Checking,
    Granted,
    Blocked,
    Failed
}

private class ReadAccessState {
    var phase by mutableStateOf(ReadAccessPhase.Idle)
        private set
    var status by mutableStateOf<FloentlyAccessStatus?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    suspend fun refresh(
        sessionStore: FloentlySecureSessionStore
    ) {
        val token = sessionStore.session?.token
            ?.takeIf { it.isNotBlank() }

        if (token == null) {
            phase = ReadAccessPhase.Idle
            status = null
            errorMessage = null
            return
        }

        phase = ReadAccessPhase.Checking
        errorMessage = null

        runCatching {
            val api = FloentlyApiClient(
                baseUrl = "https://learn-api.floently.com",
                tokenProvider = { token }
            )
            FloentlyAccessService(api).fetchStatus()
        }
            .onSuccess { value ->
                status = value
                phase = if (
                    value.readAccess
                    || value.isInternalAllAccess
                ) {
                    ReadAccessPhase.Granted
                } else {
                    ReadAccessPhase.Blocked
                }
            }
            .onFailure {
                errorMessage = it.message
                    ?: "Could not verify Read access."
                phase = ReadAccessPhase.Failed
            }
    }
}

@Composable
fun ReadReleaseApp(
    initialUrl: String?,
    playbackController: ReadPlaybackController,
    voiceSettings: ReadVoiceSettings
) {
    val context = LocalContext.current
    val sessionStore = remember {
        FloentlySecureSessionStore(context)
    }
    val accessState = remember { ReadAccessState() }
    val projectStore = remember { ReadProjectStore() }
    var sessionRevision by remember { mutableIntStateOf(0) }

    fun clearAccountState() {
        playbackController.clear()
        projectStore.reset()
        sessionStore.clear()
        sessionRevision += 1
    }

    val session = sessionStore.session

    LaunchedEffect(
        session?.token,
        sessionRevision
    ) {
        accessState.refresh(sessionStore)
    }

    when {
        session == null -> {
            ReadAuthScreen(
                sessionStore = sessionStore,
                onAuthenticated = {
                    sessionRevision += 1
                }
            )
        }

        accessState.phase == ReadAccessPhase.Checking
            || accessState.phase == ReadAccessPhase.Idle -> {
            ReadCenteredState(
                title = "Opening Read",
                message = "Checking your Floently access…"
            )
        }

        accessState.phase == ReadAccessPhase.Blocked -> {
            ReadEntitlementScreen(
                sessionStore = sessionStore,
                onRefresh = {
                    sessionRevision += 1
                },
                onSignedOut = ::clearAccountState
            )
        }

        accessState.phase == ReadAccessPhase.Failed -> {
            ReadAccessFailureScreen(
                sessionStore = sessionStore,
                message = accessState.errorMessage
                    ?: "Could not verify Read access.",
                onRetry = {
                    sessionRevision += 1
                },
                onSignedOut = ::clearAccountState
            )
        }

        else -> {
            ReadMainShell(
                initialUrl = initialUrl,
                sessionStore = sessionStore,
                accessStatus = accessState.status,
                projectStore = projectStore,
                playbackController = playbackController,
                voiceSettings = voiceSettings,
                onSignedOut = ::clearAccountState
            )
        }
    }
}

@Composable
private fun ReadCenteredState(
    title: String,
    message: String
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    FloentlyScreen(product = FloentlyProduct.Read) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = palette.accent)
            Spacer(Modifier.height(18.dp))
            Text(
                title,
                color = palette.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = palette.muted,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ReadAuthScreen(
    sessionStore: FloentlySecureSessionStore,
    onAuthenticated: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    FloentlyScreen(product = FloentlyProduct.Read) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Floently Read",
                color = palette.accent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(36.dp))
            Text(
                if (creating) {
                    "Build your listening library."
                } else {
                    "Welcome back."
                },
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Read websites and documents with one persistent native listening session.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(28.dp))

            ReadSurfaceCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AuthModeButton(
                        title = "Sign in",
                        selected = !creating,
                        modifier = Modifier.weight(1f)
                    ) {
                        creating = false
                        errorMessage = null
                    }
                    AuthModeButton(
                        title = "Create",
                        selected = creating,
                        modifier = Modifier.weight(1f)
                    ) {
                        creating = true
                        errorMessage = null
                    }
                }

                if (creating) {
                    ReadTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Name"
                    )
                }

                ReadTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    keyboardType = KeyboardType.Email
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation =
                        PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(
                        it,
                        color = Color(0xFFFFB86B),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                resetMessage?.let {
                    Text(
                        it,
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    enabled = !busy,
                    onClick = {
                        val normalizedEmail = email.trim()
                        if (
                            !normalizedEmail.contains("@")
                            || password.length < 6
                        ) {
                            errorMessage =
                                "Enter a valid email and at least 6 password characters."
                            return@Button
                        }

                        busy = true
                        errorMessage = null
                        resetMessage = null

                        scope.launch {
                            runCatching {
                                val api = FloentlyApiClient(
                                    baseUrl =
                                        "https://learn-api.floently.com"
                                )
                                val auth = FloentlyAuthService(
                                    api,
                                    sessionStore
                                )

                                if (creating) {
                                    auth.register(
                                        email = normalizedEmail,
                                        password = password,
                                        name = name.trim()
                                            .takeIf {
                                                it.isNotEmpty()
                                            }
                                    )
                                } else {
                                    auth.login(
                                        email = normalizedEmail,
                                        password = password
                                    )
                                }
                            }
                                .onSuccess {
                                    onAuthenticated()
                                }
                                .onFailure {
                                    errorMessage = it.message
                                        ?: "Sign in failed."
                                }
                            busy = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent
                    ),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(
                        horizontal = 20.dp,
                        vertical = 15.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (busy) {
                            "Please wait…"
                        } else if (creating) {
                            "Create account"
                        } else {
                            "Sign in"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!creating) {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            val normalizedEmail = email.trim()
                            if (!normalizedEmail.contains("@")) {
                                errorMessage =
                                    "Enter your email first."
                                return@TextButton
                            }

                            busy = true
                            errorMessage = null

                            scope.launch {
                                runCatching {
                                    val api = FloentlyApiClient(
                                        baseUrl =
                                            "https://learn-api.floently.com"
                                    )
                                    FloentlyAuthService(
                                        api,
                                        sessionStore
                                    ).requestPasswordReset(
                                        normalizedEmail
                                    )
                                }
                                    .onSuccess {
                                        resetMessage =
                                            "Password reset instructions have been requested."
                                    }
                                    .onFailure {
                                        errorMessage = it.message
                                            ?: "Could not request password reset."
                                    }
                                busy = false
                            }
                        }
                    ) {
                        Text(
                            "Forgot password?",
                            color = palette.accent
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Website passwords remain inside the native browser. Floently only processes text when you explicitly ask Read to read it.",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = {
                        openReadExternalUrl(
                            context,
                            "https://www.floently.com/learn/privacy"
                        )
                    }
                ) {
                    Text("Privacy", color = palette.accent)
                }
                TextButton(
                    onClick = {
                        openReadExternalUrl(
                            context,
                            "https://www.floently.com/learn/terms"
                        )
                    }
                ) {
                    Text("Terms", color = palette.accent)
                }
                TextButton(
                    onClick = {
                        openReadExternalUrl(
                            context,
                            "https://www.floently.com/learn/support"
                        )
                    }
                ) {
                    Text("Support", color = palette.accent)
                }
            }
        }
    }
}

@Composable
private fun AuthModeButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                palette.accent
            } else {
                palette.backgroundBottom
            }
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Text(title)
    }
}

@Composable
private fun ReadTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReadEntitlementScreen(
    sessionStore: FloentlySecureSessionStore,
    onRefresh: () -> Unit,
    onSignedOut: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)

    FloentlyScreen(product = FloentlyProduct.Read) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "Read access",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Your Floently account is signed in, but Read is not enabled for this account.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(26.dp))

            ReadSurfaceCard {
                BenefitLine("Native reading browser")
                BenefitLine("Document library and imports")
                BenefitLine("Persistent voices, speed and resume")

                Text(
                    "This first native release verifies existing Floently Read entitlement. Purchase and plan changes stay outside this build until the native store flow is approved.",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh access")
                }
            }

            ReadAccessAccountDeletionControl(
                sessionStore = sessionStore,
                onDeleted = onSignedOut
            )

            TextButton(
                onClick = onSignedOut
            ) {
                Text(
                    "Sign out",
                    color = palette.muted
                )
            }
        }
    }
}

@Composable
private fun BenefitLine(text: String) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓", color = palette.accent)
        Text(text, color = palette.text)
    }
}

@Composable
private fun ReadAccessFailureScreen(
    sessionStore: FloentlySecureSessionStore,
    message: String,
    onRetry: () -> Unit,
    onSignedOut: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)

    FloentlyScreen(product = FloentlyProduct.Read) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "Couldn’t verify Read access",
                color = palette.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                color = palette.muted,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent
                )
            ) {
                Text("Try again")
            }
            ReadAccessAccountDeletionControl(
                sessionStore = sessionStore,
                onDeleted = onSignedOut
            )
            TextButton(onClick = onSignedOut) {
                Text("Sign out", color = palette.muted)
            }
        }
    }
}

@Composable
private fun ReadAccessAccountDeletionControl(
    sessionStore: FloentlySecureSessionStore,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            enabled = !deleting,
            onClick = {
                showDialog = true
            }
        ) {
            Text(
                if (deleting) {
                    "Deleting account…"
                } else {
                    "Delete account"
                },
                color = Color(0xFFFF6B7A)
            )
        }

        error?.let {
            Text(
                it,
                color = Color(0xFFFFB86B),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!deleting) {
                    showDialog = false
                }
            },
            title = {
                Text("Delete your Floently account?")
            },
            text = {
                Text(
                    "This starts permanent deletion of your Floently account and associated personal data where deletion is legally possible. Store subscriptions must be cancelled separately in the App Store or Google Play."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        val token =
                            sessionStore.session?.token
                        if (token.isNullOrBlank()) {
                            showDialog = false
                            error =
                                "Your session expired. Sign in again before deleting your account."
                            return@TextButton
                        }

                        deleting = true
                        error = null

                        scope.launch {
                            runCatching {
                                val api = FloentlyApiClient(
                                    baseUrl =
                                        "https://learn-api.floently.com",
                                    tokenProvider = { token }
                                )
                                FloentlyAuthService(
                                    api,
                                    sessionStore
                                ).deleteAccount(
                                    "read_native_access_screen"
                                )
                            }
                                .onSuccess {
                                    showDialog = false
                                    onDeleted()
                                }
                                .onFailure {
                                    showDialog = false
                                    error =
                                        it.message
                                            ?: "Account deletion failed."
                                }
                            deleting = false
                        }
                    }
                ) {
                    Text(
                        "Delete account permanently",
                        color = Color(0xFFFF6B7A)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadMainShell(
    initialUrl: String?,
    sessionStore: FloentlySecureSessionStore,
    accessStatus: FloentlyAccessStatus?,
    projectStore: ReadProjectStore,
    playbackController: ReadPlaybackController,
    voiceSettings: ReadVoiceSettings,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = floentlyPalette(FloentlyProduct.Read)

    var tab by remember { mutableStateOf(ReadReleaseTab.Home) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showPasteSheet by remember { mutableStateOf(false) }
    var showUrlSheet by remember { mutableStateOf(false) }
    var browserOpen by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf<String?>(null) }
    var activeProject by remember {
        mutableStateOf<ReadContentProject?>(null)
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
            }

            val token = sessionStore.session?.token
            if (token != null) {
                scope.launch {
                    runCatching {
                        projectStore.addFile(
                            uri = uri,
                            resolver = context.contentResolver,
                            accessToken = token
                        )
                    }
                        .onSuccess {
                            activeProject = it
                        }
                        .onFailure {
                            projectStore.errorMessage =
                                it.message
                                    ?: "Could not import this file."
                        }
                }
            }
        }
    }

    LaunchedEffect(
        sessionStore.session?.token
    ) {
        val token = sessionStore.session?.token
            ?: return@LaunchedEffect
        projectStore.refresh(token)
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            browserUrl = initialUrl
            browserOpen = true
        }
    }

    when {
        browserOpen -> {
            ReadBrowserScreen(
                initialUrl = browserUrl,
                playbackController = playbackController,
                voiceSettings = voiceSettings,
                onExit = {
                    browserOpen = false
                    browserUrl = null
                }
            )
        }

        activeProject != null -> {
            ReadProjectReaderScreen(
                project = activeProject!!,
                sessionStore = sessionStore,
                projectStore = projectStore,
                playbackController = playbackController,
                voiceSettings = voiceSettings,
                onExit = {
                    activeProject = null
                }
            )
        }

        else -> {
            Scaffold(
                containerColor = palette.backgroundTop,
                bottomBar = {
                    NavigationBar(
                        containerColor =
                            palette.backgroundBottom
                    ) {
                        ReleaseNavItem(
                            selected =
                                tab == ReadReleaseTab.Home,
                            glyph = "⌂",
                            label = "Read"
                        ) {
                            tab = ReadReleaseTab.Home
                        }
                        ReleaseNavItem(
                            selected =
                                tab == ReadReleaseTab.Library,
                            glyph = "▤",
                            label = "Library"
                        ) {
                            tab = ReadReleaseTab.Library
                        }
                        ReleaseNavItem(
                            selected =
                                tab == ReadReleaseTab.Settings,
                            glyph = "⚙",
                            label = "Settings"
                        ) {
                            tab = ReadReleaseTab.Settings
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    when (tab) {
                        ReadReleaseTab.Home -> {
                            ReadHomeScreen(
                                sessionStore = sessionStore,
                                projectStore = projectStore,
                                playbackController =
                                    playbackController,
                                onOpenProject = {
                                    activeProject = it
                                },
                                onAdd = {
                                    showAddSheet = true
                                },
                                onBrowser = {
                                    browserOpen = true
                                },
                                onLibrary = {
                                    tab = ReadReleaseTab.Library
                                }
                            )
                        }

                        ReadReleaseTab.Library -> {
                            ReadLibraryScreen(
                                sessionStore = sessionStore,
                                projectStore = projectStore,
                                onOpenProject = {
                                    activeProject = it
                                },
                                onAdd = {
                                    showAddSheet = true
                                }
                            )
                        }

                        ReadReleaseTab.Settings -> {
                            ReadSettingsScreen(
                                sessionStore = sessionStore,
                                accessStatus = accessStatus,
                                playbackController =
                                    playbackController,
                                onSignedOut = onSignedOut
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        )
        ModalBottomSheet(
            onDismissRequest = {
                showAddSheet = false
            },
            sheetState = sheetState
        ) {
            ReadAddSourceSheet(
                onFiles = {
                    showAddSheet = false
                    filePicker.launch(
                        arrayOf(
                            "application/pdf",
                            "text/plain",
                            "application/rtf",
                            "application/epub+zip",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                },
                onPaste = {
                    showAddSheet = false
                    showPasteSheet = true
                },
                onLink = {
                    showAddSheet = false
                    showUrlSheet = true
                },
                onWebsite = {
                    showAddSheet = false
                    browserOpen = true
                }
            )
        }
    }

    if (showPasteSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        ModalBottomSheet(
            onDismissRequest = {
                showPasteSheet = false
            },
            sheetState = sheetState
        ) {
            ReadPasteTextSheet(
                sessionStore = sessionStore,
                projectStore = projectStore,
                onCreated = {
                    showPasteSheet = false
                    activeProject = it
                }
            )
        }
    }

    if (showUrlSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        )
        ModalBottomSheet(
            onDismissRequest = {
                showUrlSheet = false
            },
            sheetState = sheetState
        ) {
            ReadUrlImportSheet(
                sessionStore = sessionStore,
                projectStore = projectStore,
                onCreated = {
                    showUrlSheet = false
                    activeProject = it
                }
            )
        }
    }
}

@Composable
private fun ReleaseNavItem(
    selected: Boolean,
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)

    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) {
                palette.accent
            } else {
                palette.muted
            }
        ),
        contentPadding = PaddingValues(
            horizontal = 18.dp,
            vertical = 8.dp
        ),
        modifier = Modifier.height(58.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                glyph,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}

@Composable
private fun ReadHomeScreen(
    sessionStore: FloentlySecureSessionStore,
    projectStore: ReadProjectStore,
    playbackController: ReadPlaybackController,
    onOpenProject: (ReadContentProject) -> Unit,
    onAdd: () -> Unit,
    onBrowser: () -> Unit,
    onLibrary: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    val scope = rememberCoroutineScope()
    val session = sessionStore.session

    FloentlyScreen(product = FloentlyProduct.Read) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = 28.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Read",
                            color = palette.text,
                            style =
                                MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Listen without leaving the source.",
                            color = palette.muted
                        )
                    }

                    Surface(
                        color = palette.backgroundBottom,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                accountInitial(
                                    session?.user?.name,
                                    session?.user?.email
                                ),
                                color = palette.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (playbackController.snapshot.visible) {
                item {
                    Spacer(Modifier.height(30.dp))
                    SectionLabel("CONTINUE")
                    Spacer(Modifier.height(10.dp))

                    ReadSurfaceCard(
                        modifier = Modifier.clickable {
                            playbackController.togglePlayPause()
                        }
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    playbackController.snapshot.title,
                                    color = palette.text,
                                    style =
                                        MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    playbackController.snapshot.status,
                                    color = palette.muted
                                )
                            }

                            Surface(
                                color = palette.accent,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Text(
                                        if (
                                            playbackController
                                                .snapshot
                                                .isPlaying
                                        ) "Ⅱ" else "▶",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val duration =
                            playbackController.snapshot.durationMs
                                .coerceAtLeast(1L)
                        LinearProgressIndicator(
                            progress = {
                                (
                                    playbackController
                                        .snapshot
                                        .positionMs
                                        .toFloat()
                                        / duration.toFloat()
                                    ).coerceIn(0f, 1f)
                            },
                            color = palette.accent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionLabel("LIBRARY")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onLibrary) {
                        Text(
                            "See all",
                            color = palette.accent
                        )
                    }
                }
            }

            if (projectStore.projects.isEmpty()) {
                item {
                    ReadEmptyLibraryCard(onAdd)
                }
            } else {
                items(
                    projectStore.projects.take(3),
                    key = { it.id }
                ) { project ->
                    ReadProjectRow(
                        project = project,
                        onClick = {
                            onOpenProject(project)
                        }
                    )
                }
            }

            if (
                projectStore.activity != "idle"
                && projectStore.activity != "loading"
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    ReadImportProgress(
                        message = projectStore.activity
                    )
                }
            }

            projectStore.errorMessage?.let { error ->
                item {
                    Spacer(Modifier.height(16.dp))
                    ReadStatusBanner(
                        text = error,
                        actionTitle = "Retry"
                    ) {
                        val token = sessionStore.session?.token
                        if (token != null) {
                            scope.launch {
                                projectStore.refresh(token)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(
                            "+  Add to Read",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onBrowser,
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                palette.backgroundBottom
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .width(64.dp)
                            .height(54.dp)
                    ) {
                        Text("◎")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadLibraryScreen(
    sessionStore: FloentlySecureSessionStore,
    projectStore: ReadProjectStore,
    onOpenProject: (ReadContentProject) -> Unit,
    onAdd: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var pendingDelete by remember {
        mutableStateOf<ReadContentProject?>(null)
    }

    val filtered = remember(
        projectStore.projects,
        search
    ) {
        val query = search.trim()
        if (query.isEmpty()) {
            projectStore.projects
        } else {
            projectStore.projects.filter {
                it.title.contains(
                    query,
                    ignoreCase = true
                )
                    || it.displaySource.contains(
                        query,
                        ignoreCase = true
                    )
            }
        }
    }

    FloentlyScreen(product = FloentlyProduct.Read) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Library",
                    color = palette.text,
                    style =
                        MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            palette.backgroundBottom
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(
                        "+",
                        style =
                            MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ReadTextField(
                value = search,
                onValueChange = { search = it },
                label = "Search your library"
            )
            Spacer(Modifier.height(12.dp))

            if (
                projectStore.activity != "idle"
                && projectStore.activity != "loading"
            ) {
                ReadImportProgress(
                    message = projectStore.activity
                )
                Spacer(Modifier.height(12.dp))
            }

            when {
                projectStore.activity == "loading"
                    && projectStore.projects.isEmpty() -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = palette.accent
                        )
                    }
                }

                filtered.isEmpty() -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        ReadEmptyLibraryCard(onAdd)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            filtered,
                            key = { it.id }
                        ) {
                            ReadProjectRow(
                                project = it,
                                onClick = {
                                    onOpenProject(it)
                                },
                                onDelete = {
                                    pendingDelete = it
                                }
                            )
                        }

                        item {
                            TextButton(
                                onClick = {
                                    val token =
                                        sessionStore
                                            .session
                                            ?.token
                                    if (token != null) {
                                        scope.launch {
                                            projectStore
                                                .refresh(token)
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    "Refresh library",
                                    color = palette.accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
            },
            title = {
                Text("Remove this reading?")
            },
            text = {
                Text(
                    "“" + project.title +
                        "” will be removed from your synced Read library."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        val token =
                            sessionStore.session?.token
                        if (token == null) {
                            projectStore.errorMessage =
                                "Sign in again to change your library."
                        } else {
                            scope.launch {
                                runCatching {
                                    projectStore.delete(
                                        project = project,
                                        accessToken = token
                                    )
                                }.onFailure {
                                    projectStore.errorMessage =
                                        it.message
                                            ?: "Could not remove this reading."
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        "Remove",
                        color = Color(0xFFFF8A80)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReadProjectRow(
    project: ReadContentProject,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp)
        ) {
            Surface(
                color = palette.backgroundBottom,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(
                    width = 48.dp,
                    height = 62.dp
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (
                            project.sourceType.lowercase()
                        ) {
                            "pdf" -> "PDF"
                            "web", "website", "url" -> "◎"
                            else -> "Aa"
                        },
                        color = palette.accent,
                        style =
                            MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.title,
                    color = palette.text,
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    buildString {
                        append(project.displaySource)
                        if (project.wordCount > 0) {
                            append("  •  ")
                            append(project.wordCount)
                            append(" words")
                        }
                    },
                    color = palette.muted,
                    style =
                        MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            Text(
                "›",
                color = palette.muted,
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (onDelete != null) {
            Box {
                TextButton(
                    onClick = {
                        menuOpen = true
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .width(42.dp)
                        .height(48.dp)
                ) {
                    Text(
                        "⋯",
                        color = palette.muted,
                        style =
                            MaterialTheme.typography.titleLarge
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = {
                        menuOpen = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Remove from Library")
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadEmptyLibraryCard(
    onAdd: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)

    ReadSurfaceCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "▤",
                color = palette.accent,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Your library is ready",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Add a document, paste text, or open a live website.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAdd) {
                Text(
                    "Add your first source",
                    color = palette.accent
                )
            }
        }
    }
}

@Composable
private fun ReadSettingsScreen(
    sessionStore: FloentlySecureSessionStore,
    accessStatus: FloentlyAccessStatus?,
    playbackController: ReadPlaybackController,
    onSignedOut: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = sessionStore.session
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    var deletingAccount by remember {
        mutableStateOf(false)
    }
    var deleteError by remember {
        mutableStateOf<String?>(null)
    }

    FloentlyScreen(product = FloentlyProduct.Read) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Settings",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(22.dp))

            ReadSurfaceCard {
                Text(
                    "Account",
                    color = palette.text,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    session?.user?.name
                        ?: session?.user?.email
                        ?: "Floently account",
                    color = palette.text,
                    style = MaterialTheme.typography.titleMedium
                )
                session?.user?.email
                    ?.takeIf {
                        it != session?.user?.name
                    }
                    ?.let {
                        Text(it, color = palette.muted)
                    }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Read access",
                        color = palette.text
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        when {
                            accessStatus
                                ?.isInternalAllAccess == true ->
                                "Internal"
                            accessStatus?.readAccess == true ->
                                accessStatus.billingTier
                                    ?.replace("_", " ")
                                    ?.replaceFirstChar {
                                        it.titlecase()
                                    }
                                    ?: "Active"
                            else -> "Unavailable"
                        },
                        color = palette.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            ReadSurfaceCard {
                Text(
                    "Playback",
                    color = palette.text,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Speed", color = palette.text)
                    Spacer(Modifier.weight(1f))
                    Text(
                        speedLabel(
                            playbackController.snapshot.speed
                        ),
                        color = palette.muted
                    )
                }
                Text(
                    "Voice and speed are document-level settings and do not reset between hidden audio segments.",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))

            ReadSurfaceCard {
                Text(
                    "Privacy & support",
                    color = palette.text,
                    fontWeight = FontWeight.Bold
                )
                LegalLinkRow("Privacy Policy") {
                    openReadExternalUrl(
                        context,
                        "https://www.floently.com/learn/privacy"
                    )
                }
                LegalLinkRow("Terms of Use") {
                    openReadExternalUrl(
                        context,
                        "https://www.floently.com/learn/terms"
                    )
                }
                LegalLinkRow("Support") {
                    openReadExternalUrl(
                        context,
                        "https://www.floently.com/learn/support"
                    )
                }
                LegalLinkRow("Account deletion information") {
                    openReadExternalUrl(
                        context,
                        "https://www.floently.com/learn/delete-account"
                    )
                }
            }

            deleteError?.let {
                Spacer(Modifier.height(12.dp))
                ReadStatusBanner(text = it)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onSignedOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        palette.backgroundBottom
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Sign out", color = palette.text)
            }

            Spacer(Modifier.height(10.dp))
            Button(
                enabled = !deletingAccount,
                onClick = {
                    showDeleteDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF7B1F2D)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (deletingAccount) {
                        "Deleting account…"
                    } else {
                        "Delete Account"
                    }
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!deletingAccount) {
                    showDeleteDialog = false
                }
            },
            title = {
                Text("Delete your Floently account?")
            },
            text = {
                Text(
                    "This permanently deletes your Floently account and associated personal data where deletion is legally possible. Store subscriptions must still be cancelled in the App Store or Google Play."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !deletingAccount,
                    onClick = {
                        val token =
                            sessionStore.session?.token
                        if (token.isNullOrBlank()) {
                            showDeleteDialog = false
                            deleteError =
                                "Your session expired. Sign in again."
                            return@TextButton
                        }

                        deletingAccount = true
                        deleteError = null

                        scope.launch {
                            runCatching {
                                val api = FloentlyApiClient(
                                    baseUrl =
                                        "https://learn-api.floently.com",
                                    tokenProvider = { token }
                                )
                                FloentlyAuthService(
                                    api,
                                    sessionStore
                                ).deleteAccount(
                                    "in_app_settings"
                                )
                            }
                                .onSuccess {
                                    showDeleteDialog = false
                                    onSignedOut()
                                }
                                .onFailure {
                                    showDeleteDialog = false
                                    deleteError =
                                        it.message
                                            ?: "Account deletion failed."
                                }
                            deletingAccount = false
                        }
                    }
                ) {
                    Text(
                        "Delete Account",
                        color = Color(0xFFFF6B7A)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deletingAccount,
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LegalLinkRow(
    title: String,
    onClick: () -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Text(
            title,
            color = palette.accent,
            modifier = Modifier.weight(1f)
        )
        Text("›", color = palette.muted)
    }
}

@Composable
private fun ReadAddSourceSheet(
    onFiles: () -> Unit,
    onPaste: () -> Unit,
    onLink: () -> Unit,
    onWebsite: () -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 28.dp
            )
    ) {
        Text(
            "Add to Read",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Choose a source. Read keeps one listening session across documents and live websites.",
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        SourceChoice(
            glyph = "▣",
            title = "Files",
            subtitle = "PDF, EPUB, DOCX, TXT and Markdown",
            onClick = onFiles
        )
        SourceChoice(
            glyph = "Aa",
            title = "Paste text",
            subtitle =
                "Turn notes or copied text into a saved project",
            onClick = onPaste
        )
        SourceChoice(
            glyph = "↗",
            title = "Import link",
            subtitle =
                "Save an article or public page into your library",
            onClick = onLink
        )
        SourceChoice(
            glyph = "◎",
            title = "Live website",
            subtitle =
                "Keep the original page visible and interactive",
            onClick = onWebsite
        )
    }
}

@Composable
private fun SourceChoice(
    glyph: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme
            .surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(glyph)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                )
            }
            Text("›")
        }
    }
}

@Composable
private fun ReadUrlImportSheet(
    sessionStore: FloentlySecureSessionStore,
    projectStore: ReadProjectStore,
    onCreated: (ReadContentProject) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                bottom = 28.dp
            )
    ) {
        Text(
            "Import link",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Save a public article or page as a library reading. For signed-in or interactive sites, use Live website instead.",
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )

        ReadTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title optional"
        )

        ReadTextField(
            value = sourceUrl,
            onValueChange = { sourceUrl = it },
            label = "https://example.com/article",
            keyboardType = KeyboardType.Uri
        )

        error?.let {
            Text(
                it,
                color = Color(0xFFFF8A80),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            enabled = !busy && sourceUrl.isNotBlank(),
            onClick = {
                val token = sessionStore.session?.token
                if (token == null) {
                    error = "Sign in again to save this link."
                    return@Button
                }

                busy = true
                error = null

                scope.launch {
                    runCatching {
                        projectStore.addUrl(
                            title = title,
                            sourceUrl = sourceUrl,
                            accessToken = token
                        )
                    }
                        .onSuccess(onCreated)
                        .onFailure {
                            error = it.message
                                ?: "Could not import this link."
                        }
                    busy = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                if (busy) "Importing…" else "Save and open"
            )
        }
    }
}

@Composable
private fun ReadPasteTextSheet(
    sessionStore: FloentlySecureSessionStore,
    projectStore: ReadProjectStore,
    onCreated: (ReadContentProject) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(
                start = 20.dp,
                end = 20.dp,
                bottom = 24.dp
            )
    ) {
        Text(
            "Paste text",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(14.dp))

        ReadTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title optional"
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text.split(Regex("\\s+"))
                    .count { it.isNotBlank() }
                    .toString() + " words",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A80),
                    maxLines = 2
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            enabled = !busy && text.isNotBlank(),
            onClick = {
                val token = sessionStore.session?.token
                if (token == null) {
                    error = "Sign in again to save this text."
                    return@Button
                }

                busy = true
                error = null

                scope.launch {
                    runCatching {
                        projectStore.addText(
                            title = title,
                            text = text,
                            accessToken = token
                        )
                    }
                        .onSuccess(onCreated)
                        .onFailure {
                            error = it.message
                                ?: "Could not save this text."
                        }
                    busy = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (busy) "Adding…" else "Add to Read")
        }
    }
}

@Composable
private fun ReadProjectReaderScreen(
    project: ReadContentProject,
    sessionStore: FloentlySecureSessionStore,
    projectStore: ReadProjectStore,
    playbackController: ReadPlaybackController,
    voiceSettings: ReadVoiceSettings,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val palette = floentlyPalette(FloentlyProduct.Read)
    var hydrated by remember {
        mutableStateOf<ReadContentProject?>(null)
    }
    var manifest by remember {
        mutableStateOf<ReadingManifestV1?>(null)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var preparing by remember { mutableStateOf(true) }
    var retryRevision by remember { mutableIntStateOf(0) }

    LaunchedEffect(
        project.id,
        retryRevision
    ) {
        val token = sessionStore.session?.token
        if (token == null) {
            errorMessage = "Your session expired. Sign in again."
            preparing = false
            return@LaunchedEffect
        }

        preparing = true
        errorMessage = null

        runCatching {
            val full = projectStore.hydrate(
                project,
                token
            )
            val text = full.rawText
                ?.takeIf { it.isNotBlank() }
                ?: error(
                    "The project did not include readable text."
                )

            val built = withContext(Dispatchers.Default) {
                ReadCoreNative.buildManifest(
                    documentId = full.id,
                    revisionId = full.revisionId,
                    title = full.title,
                    language = full.language ?: "auto",
                    text = text
                )
            }
            full to built
        }
            .onSuccess {
                ReadRemoteProjectProgressBridge.apply(
                    context = context,
                    project = it.first,
                    manifest = it.second,
                    voiceSettings = voiceSettings
                )
                hydrated = it.first
                manifest = it.second
            }
            .onFailure {
                errorMessage = it.message
                    ?: "Could not prepare this document."
            }

        preparing = false
    }

    ReadProjectProgressSyncEffect(
        project = hydrated ?: project,
        manifest = manifest,
        sessionStore = sessionStore,
        projectStore = projectStore,
        playbackController = playbackController,
        voiceSettings = voiceSettings
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundTop)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.backgroundBottom)
                .padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                )
        ) {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    "‹",
                    color = palette.text,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Text(
                project.title,
                color = palette.text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Button(
                enabled = manifest != null && !preparing,
                onClick = {
                    val value = manifest
                        ?: return@Button
                    val voice =
                        hydrated?.progress?.voiceId
                            ?: project.progress?.voiceId
                            ?: voiceSettings.voiceId(value.language)
                    playbackController.loadManifest(
                        manifest = value,
                        voiceId = voice,
                        autoplay = true
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                if (preparing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("▶")
                }
            }
        }

        val duration = playbackController
            .snapshot
            .durationMs
            .coerceAtLeast(1L)
        LinearProgressIndicator(
            progress = {
                if (
                    playbackController.snapshot.visible
                    && playbackController.snapshot.title
                        == project.title
                ) {
                    (
                        playbackController
                            .snapshot
                            .positionMs
                            .toFloat()
                            / duration.toFloat()
                        ).coerceIn(0f, 1f)
                } else {
                    0f
                }
            },
            color = palette.accent,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        )

        errorMessage?.let {
            ReadStatusBanner(
                text = it,
                actionTitle = "Retry"
            ) {
                retryRevision += 1
            }
        }

        val text = hydrated?.rawText
        if (text == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = palette.accent
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Preparing readable content…",
                        color = palette.muted
                    )
                }
            }
        } else {
            val paragraphs = remember(text) {
                text.replace("\r\n", "\n")
                    .split(Regex("\\n\\s*\\n"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .ifEmpty { listOf(text) }
            }

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 28.dp,
                    bottom = 120.dp
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(paragraphs) { paragraph ->
                    Text(
                        paragraph,
                        color = palette.text,
                        style =
                            MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = floentlyPalette(FloentlyProduct.Read)

    Column(
        verticalArrangement =
            Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                palette.backgroundBottom.copy(
                    alpha = 0.92f
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        content = content
    )
}

@Composable
private fun SectionLabel(text: String) {
    val palette = floentlyPalette(FloentlyProduct.Read)
    Text(
        text,
        color = palette.muted,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ReadImportProgress(
    message: String
) {
    val palette = floentlyPalette(FloentlyProduct.Read)

    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.backgroundBottom)
            .padding(14.dp)
    ) {
        Text(
            message,
            color = palette.text,
            fontWeight = FontWeight.SemiBold
        )
        LinearProgressIndicator(
            color = palette.accent,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Read will open the document as soon as readable content is available. Audio can continue preparing afterward.",
            color = palette.muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ReadStatusBanner(
    text: String,
    actionTitle: String? = null,
    action: (() -> Unit)? = null
) {
    val palette = floentlyPalette(FloentlyProduct.Read)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.backgroundBottom)
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            )
    ) {
        Text(
            "!",
            color = Color(0xFFFFB86B),
            fontWeight = FontWeight.Bold
        )
        Text(
            text,
            color = palette.text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        if (actionTitle != null && action != null) {
            TextButton(onClick = action) {
                Text(
                    actionTitle,
                    color = palette.accent
                )
            }
        }
    }
}

private fun accountInitial(
    name: String?,
    email: String?
): String =
    (
        name?.trim()?.firstOrNull()
            ?: email?.trim()?.firstOrNull()
            ?: 'F'
        ).uppercase()

private fun openReadExternalUrl(
    context: Context,
    url: String
) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }
}

private fun speedLabel(value: Float): String =
    if (kotlin.math.abs(value - value.toInt()) < 0.01f) {
        value.toInt().toString() + "×"
    } else {
        "%.2g".format(value) + "×"
    }
