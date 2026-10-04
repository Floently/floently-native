package com.floently.read

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.api.FloentlyApiClient
import com.floently.shared.api.FloentlyApiError
import com.floently.shared.auth.FloentlyAuthService
import com.floently.shared.auth.FloentlySecureSessionStore
import com.floently.shared.billing.FloentlyAccessService
import com.floently.shared.billing.FloentlyAccessStatus
import com.floently.shared.design.FloentlyDesignTokens
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import com.floently.shared.design.floentlyPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.DateFormat
import java.util.Date

private enum class ReadReleaseTab {
    Home,
    Library,
    Settings
}

private enum class ReadAccessPhase {
    Idle,
    Checking,
    Granted,
    GrantedOffline,
    Blocked,
    Failed
}

private class ReadAccessState(
    context: Context
) {
    private val applicationContext =
        context.applicationContext

    var phase by mutableStateOf(ReadAccessPhase.Idle)
        private set
    var status by mutableStateOf<FloentlyAccessStatus?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var offlineVerifiedAtMs by mutableStateOf<Long?>(null)
        private set

    suspend fun refresh(
        sessionStore: FloentlySecureSessionStore
    ) {
        val session = sessionStore.session
        val token = session?.token
            ?.takeIf { it.isNotBlank() }

        if (session == null || token == null) {
            phase = ReadAccessPhase.Idle
            status = null
            errorMessage = null
            offlineVerifiedAtMs = null
            return
        }

        val accountIdentity =
            readAccountIdentity(
                userId = session.user.id,
                email = session.user.email
            )

        phase = ReadAccessPhase.Checking
        errorMessage = null
        offlineVerifiedAtMs = null

        try {
            val api = FloentlyApiClient(
                baseUrl =
                    "https://learn-api.floently.com",
                tokenProvider = { token }
            )
            val value =
                FloentlyAccessService(api)
                    .fetchStatus()
            val granted =
                value.readAccess
                    || value.isInternalAllAccess

            status = value
            phase =
                if (granted) {
                    ReadAccessPhase.Granted
                } else {
                    ReadAccessPhase.Blocked
                }

            if (accountIdentity != null) {
                if (granted) {
                    ReadAccessLeaseStore.save(
                        context = applicationContext,
                        status = value,
                        accountIdentity =
                            accountIdentity
                    )
                } else {
                    ReadAccessLeaseStore.remove(
                        context = applicationContext,
                        accountIdentity =
                            accountIdentity
                    )
                }
            }
        } catch (
            error: CancellationException
        ) {
            throw error
        } catch (error: IOException) {
            if (
                restoreOfflineLease(
                    accountIdentity
                )
            ) {
                return
            }

            status = null
            errorMessage =
                error.message
                    ?: "Could not verify Read access."
            phase = ReadAccessPhase.Failed
        } catch (error: FloentlyApiError) {
            if (
                isTransientAccessError(error)
                && restoreOfflineLease(
                    accountIdentity
                )
            ) {
                return
            }

            status = null
            errorMessage =
                error.message
            phase = ReadAccessPhase.Failed
        } catch (error: Exception) {
            status = null
            errorMessage =
                error.message
                    ?: "Could not verify Read access."
            phase = ReadAccessPhase.Failed
        }
    }

    private suspend fun restoreOfflineLease(
        accountIdentity: String?
    ): Boolean {
        val identity =
            accountIdentity
                ?: return false
        val lease =
            ReadAccessLeaseStore
                .validGrantedLease(
                    context =
                        applicationContext,
                    accountIdentity = identity
                )
                ?: return false

        status = lease.status
        offlineVerifiedAtMs =
            lease.verifiedAtMs
        phase =
            ReadAccessPhase.GrantedOffline
        return true
    }

    private fun isTransientAccessError(
        error: FloentlyApiError
    ): Boolean {
        if (error.retryable) {
            return true
        }

        val code =
            error.code.uppercase()
        if (
            code == "HTTP_408"
            || code == "HTTP_429"
        ) {
            return true
        }

        val status =
            code.removePrefix("HTTP_")
                .takeIf {
                    code.startsWith(
                        "HTTP_"
                    )
                }
                ?.toIntOrNull()
                ?: return false

        return status in 500..599
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
    val accessState = remember(context) {
        ReadAccessState(context)
    }
    val projectStore = remember(context) {
        ReadProjectStore(context)
    }
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

    val activeManifest = playbackController.activeManifest
    val activeProject = remember(
        projectStore.projects,
        activeManifest?.documentId,
        activeManifest?.revisionId
    ) {
        activeManifest?.let { manifest ->
            projectStore.projects.firstOrNull { project ->
                project.id == manifest.documentId
                    && project.revisionId == manifest.revisionId
            }
        }
    }

    if (
        session != null
        && activeManifest != null
        && activeProject != null
    ) {
        ReadProjectProgressSyncEffect(
            project = activeProject,
            manifest = activeManifest,
            sessionStore = sessionStore,
            projectStore = projectStore,
            playbackController = playbackController
        )
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

        accessState.phase
            == ReadAccessPhase.GrantedOffline -> {
            ReadMainShell(
                initialUrl = initialUrl,
                sessionStore = sessionStore,
                accessStatus = accessState.status,
                offlineAccessVerifiedAtMs =
                    accessState
                        .offlineVerifiedAtMs,
                projectStore = projectStore,
                playbackController =
                    playbackController,
                voiceSettings = voiceSettings,
                onRecheckAccess = {
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
                offlineAccessVerifiedAtMs = null,
                projectStore = projectStore,
                playbackController = playbackController,
                voiceSettings = voiceSettings,
                onRecheckAccess = {
                    sessionRevision += 1
                },
                onSignedOut = ::clearAccountState
            )
        }
    }
}

@Composable
private fun ReadOfflineAccessLeaseBanner(
    verifiedAtMs: Long,
    onRecheck: () -> Unit
) {
    val palette =
        floentlyPalette(FloentlyProduct.Read)
    val verified = remember(
        verifiedAtMs
    ) {
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(
            Date(verifiedAtMs)
        )
    }

    Row(
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                palette.backgroundBottom
            )
            .padding(
                horizontal = 14.dp,
                vertical = 9.dp
            )
    ) {
        Text(
            "◌",
            color = palette.accent,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                "Offline mode",
                color = palette.text,
                style =
                    MaterialTheme.typography
                        .labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Access last verified "
                    + verified
                    + ".",
                color = palette.muted,
                style =
                    MaterialTheme.typography
                        .bodySmall
            )
        }
        TextButton(
            onClick = onRecheck
        ) {
            Text(
                "Recheck",
                color = palette.accent
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
                        color = FloentlyDesignTokens.Colors.warning,
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
                    shape = RoundedCornerShape(FloentlyDesignTokens.Radius.l),
                    contentPadding = PaddingValues(
                        horizontal =
                            FloentlyDesignTokens.Space.s5,
                        vertical = 0.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            FloentlyDesignTokens
                                .Control
                                .primaryHeight
                        )
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
                    shape = RoundedCornerShape(FloentlyDesignTokens.Radius.l),
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
    val context = LocalContext.current
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
                color = FloentlyDesignTokens.Colors.danger
            )
        }

        error?.let {
            Text(
                it,
                color = FloentlyDesignTokens.Colors.warning,
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
                                ReadOriginalDocumentStore
                                    .clearAll(context)
                                ReadProjectSnapshotStore
                                    .clearAll(context)
                                ReadOfflineAudioStore
                                    .clearAll(context)
                                ReadAccessLeaseStore
                                    .clearAll(context)
                                ReadProgressOutboxStore
                                    .clearAll(context)
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
                        color = FloentlyDesignTokens.Colors.danger
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
    offlineAccessVerifiedAtMs: Long?,
    projectStore: ReadProjectStore,
    playbackController: ReadPlaybackController,
    voiceSettings: ReadVoiceSettings,
    onRecheckAccess: () -> Unit,
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
        val session = sessionStore.session
            ?: return@LaunchedEffect
        projectStore.bindAccount(
            userId = session.user.id,
            email = session.user.email
        )
        projectStore.refresh(session.token)
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
                topBar = {
                    if (
                        offlineAccessVerifiedAtMs
                            != null
                    ) {
                        ReadOfflineAccessLeaseBanner(
                            verifiedAtMs =
                                offlineAccessVerifiedAtMs,
                            onRecheck =
                                onRecheckAccess
                        )
                    }
                },
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
                                playbackController =
                                    playbackController,
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
                            "text/markdown",
                            "text/html",
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
        shape = RoundedCornerShape(FloentlyDesignTokens.Radius.l),
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
                        shape = RoundedCornerShape(FloentlyDesignTokens.Radius.l),
                        modifier = Modifier
                            .weight(1f)
                            .height(
                                FloentlyDesignTokens
                                    .Control
                                    .primaryHeight
                            )
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
                        shape = RoundedCornerShape(FloentlyDesignTokens.Radius.l),
                        modifier = Modifier
                            .width(64.dp)
                            .height(
                                FloentlyDesignTokens
                                    .Control
                                    .primaryHeight
                            )
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
    playbackController: ReadPlaybackController,
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
                            val performDelete: () -> Unit = {
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

                            if (
                                playbackController
                                    .activeDocumentId
                                    == project.id
                            ) {
                                playbackController.clear(
                                    onComplete =
                                        performDelete
                                )
                            } else {
                                performDelete()
                            }
                        }
                    }
                ) {
                    Text(
                        "Remove",
                        color = FloentlyDesignTokens.Colors.danger
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
                    .height(
                        FloentlyDesignTokens
                            .Control
                            .primaryHeight
                    )
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
                        FloentlyDesignTokens.Colors.danger.copy(alpha = 0.34f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        FloentlyDesignTokens
                            .Control
                            .primaryHeight
                    )
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
                                ReadOriginalDocumentStore
                                    .clearAll(context)
                                ReadProjectSnapshotStore
                                    .clearAll(context)
                                ReadOfflineAudioStore
                                    .clearAll(context)
                                ReadAccessLeaseStore
                                    .clearAll(context)
                                ReadProgressOutboxStore
                                    .clearAll(context)
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
                        color = FloentlyDesignTokens.Colors.danger
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
        color =
            FloentlyDesignTokens.Colors.surface1,
        shape = RoundedCornerShape(
            FloentlyDesignTokens.Radius.m
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    FloentlyDesignTokens
                        .Space
                        .s4
                )
        ) {
            Surface(
                color =
                    FloentlyDesignTokens
                        .Colors
                        .surface2,
                shape = RoundedCornerShape(
                    FloentlyDesignTokens
                        .Radius
                        .m
                ),
                modifier = Modifier.size(
                    FloentlyDesignTokens
                        .Control
                        .compactHeight
                )
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
                color = FloentlyDesignTokens.Colors.danger,
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
                    color = FloentlyDesignTokens.Colors.danger,
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
    val scope = rememberCoroutineScope()
    val offlineCoordinator = remember(context) {
        ReadProgressiveAudioCoordinator(
            context = context,
            cache = ReadNativeAudioCache(
                context = context,
                maximumBytes =
                    Long.MAX_VALUE / 4L
            )
        )
    }
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
    var offlineSaved by remember {
        mutableStateOf(false)
    }
    var offlineBusy by remember {
        mutableStateOf(false)
    }
    var offlineError by remember {
        mutableStateOf<String?>(null)
    }
    var readerMenuExpanded by remember {
        mutableStateOf(false)
    }
    val originalPdfFile = remember(
        project.id
    ) {
        ReadOriginalDocumentStore.pdfFile(
            context = context,
            projectId = project.id
        )
    }
    val offlineVoiceId = manifest?.let {
        value ->
        if (
            playbackController.activeDocumentId
                == value.documentId
            && playbackController.activeRevisionId
                == value.revisionId
        ) {
            playbackController.activeVoiceId
                ?: hydrated?.progress?.voiceId
                ?: project.progress?.voiceId
                ?: voiceSettings.voiceId(
                    value.language
                )
        } else {
            hydrated?.progress?.voiceId
                ?: project.progress?.voiceId
                ?: voiceSettings.voiceId(
                    value.language
                )
        }
    }
    val offlineAccountIdentity =
        sessionStore.session?.user?.let {
            readAccountIdentity(
                userId = it.id,
                email = it.email
            )
        }

    fun startListening() {
        val value = manifest ?: return
        val voice = offlineVoiceId ?: return

        playbackController.loadManifest(
            manifest = value,
            voiceId = voice,
            autoplay = true
        )
    }

    fun toggleOffline() {
        val value = manifest ?: return
        val voice = offlineVoiceId ?: return
        val identity =
            offlineAccountIdentity ?: return
        val token =
            sessionStore.session?.token
                ?: return

        scope.launch {
            offlineBusy = true
            offlineError = null

            try {
                if (offlineSaved) {
                    if (
                        playbackController
                            .activeDocumentId
                            == value.documentId
                        && playbackController
                            .activeRevisionId
                            == value.revisionId
                        && playbackController
                            .activeVoiceId
                            == voice
                    ) {
                        offlineError =
                            "This offline voice is currently in use. Open another reading or stop the current session before removing it."
                    } else {
                        offlineCoordinator
                            .removeOffline(
                                manifest = value,
                                voiceId = voice,
                                accountIdentity =
                                    identity
                            )
                        offlineSaved = false
                    }
                } else {
                    offlineCoordinator
                        .saveOffline(
                            manifest = value,
                            voiceId = voice,
                            accessToken = token,
                            accountIdentity =
                                identity
                        )
                    offlineSaved =
                        offlineCoordinator
                            .isAvailableOffline(
                                manifest = value,
                                voiceId = voice,
                                accountIdentity =
                                    identity
                            )
                }
            } catch (
                error: CancellationException
            ) {
                throw error
            } catch (error: Exception) {
                offlineError =
                    error.localizedMessage
                        ?: "Could not save this document offline."
            } finally {
                offlineBusy = false
            }
        }
    }

    LaunchedEffect(
        manifest?.revisionId,
        offlineVoiceId,
        offlineAccountIdentity
    ) {
        val value = manifest
        val voice = offlineVoiceId
        val identity =
            offlineAccountIdentity

        offlineSaved =
            if (
                value != null
                && voice != null
                && identity != null
            ) {
                offlineCoordinator
                    .isAvailableOffline(
                        manifest = value,
                        voiceId = voice,
                        accountIdentity = identity
                    )
            } else {
                false
            }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundTop)
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    FloentlyDesignTokens
                        .Space
                        .s2
                ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    FloentlyDesignTokens
                        .Colors
                        .surface1
                )
                .padding(
                    horizontal =
                        FloentlyDesignTokens
                            .Space
                            .s1
                )
        ) {
            ReadReaderToolbarButton(
                symbol =
                    ReadReaderToolbarSymbol
                        .Back,
                contentDescription = "Back",
                onClick = onExit
            )

            Text(
                project.title,
                color = palette.text,
                fontWeight =
                    FontWeight.SemiBold,
                style =
                    MaterialTheme.typography
                        .titleMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Box {
                ReadReaderToolbarButton(
                    symbol =
                        ReadReaderToolbarSymbol
                            .More,
                    contentDescription =
                        "Reader options"
                ) {
                    readerMenuExpanded = true
                }

                DropdownMenu(
                    expanded =
                        readerMenuExpanded,
                    onDismissRequest = {
                        readerMenuExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (preparing) {
                                    "Preparing audio…"
                                } else {
                                    "Listen"
                                }
                            )
                        },
                        enabled =
                            manifest != null
                            && !preparing,
                        onClick = {
                            readerMenuExpanded = false
                            startListening()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (offlineBusy) {
                                    "Updating offline copy…"
                                } else if (
                                    offlineSaved
                                ) {
                                    "Remove offline download"
                                } else {
                                    "Save for offline listening"
                                }
                            )
                        },
                        enabled =
                            manifest != null
                            && offlineVoiceId
                                != null
                            && offlineAccountIdentity
                                != null
                            && !offlineBusy,
                        onClick = {
                            readerMenuExpanded = false
                            toggleOffline()
                        }
                    )
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
                    && playbackController.activeDocumentId
                        == project.id
                    && playbackController.activeRevisionId
                        == project.revisionId
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
            color =
                FloentlyDesignTokens
                    .Colors
                    .brand,
            trackColor =
                FloentlyDesignTokens
                    .Colors
                    .borderSoft,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )

        errorMessage?.let {
            ReadStatusBanner(
                text = it,
                actionTitle = "Retry"
            ) {
                retryRevision += 1
            }
        }

        offlineError?.let {
            ReadStatusBanner(
                text = it
            )
        }

        val text = hydrated?.rawText
        if (originalPdfFile != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            palette.backgroundBottom
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                ) {
                    Text(
                        "PDF",
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Original PDF pages · native reading layer ready",
                        color = palette.muted,
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }

                ReadOriginalPdfView(
                    file = originalPdfFile,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        } else if (text == null) {
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
            if (
                (hydrated ?: project)
                    .sourceType
                    .lowercase() == "pdf"
            ) {
                ReadStatusBanner(
                    text =
                        "The original PDF is not stored on this device. Showing the semantic reading layer."
                )
            }

            val paragraphs = remember(text) {
                text.replace("\r\n", "\n")
                    .split(Regex("\\n\\s*\\n"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .ifEmpty { listOf(text) }
            }

            Box(
                contentAlignment =
                    Alignment.TopCenter,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            18.dp
                        ),
                    contentPadding =
                        PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 28.dp,
                            bottom = 120.dp
                        ),
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(
                            max = 680.dp
                        )
                ) {
                    items(paragraphs) {
                        paragraph ->
                        Text(
                            paragraph,
                            color = palette.text,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge
                                    .copy(
                                        fontSize =
                                            20.sp,
                                        lineHeight =
                                            32.sp
                                    )
                        )
                    }
                }
            }
        }
    }
}

private enum class ReadReaderToolbarSymbol {
    Back,
    More
}

@Composable
private fun ReadReaderToolbarButton(
    symbol: ReadReaderToolbarSymbol,
    contentDescription: String,
    onClick: () -> Unit
) {
    val color =
        FloentlyDesignTokens
            .Colors
            .textPrimary

    Button(
        onClick = onClick,
        colors = ButtonDefaults
            .buttonColors(
                containerColor =
                    Color.Transparent,
                contentColor = color
            ),
        contentPadding =
            PaddingValues(0.dp),
        modifier = Modifier
            .size(
                FloentlyDesignTokens
                    .Control
                    .iconTarget
            )
            .semantics {
                this.contentDescription =
                    contentDescription
            }
    ) {
        Canvas(
            modifier = Modifier.size(
                22.dp
            )
        ) {
            val stroke = 2.dp.toPx()
            val cy = size.height / 2f

            when (symbol) {
                ReadReaderToolbarSymbol
                    .Back -> {
                    drawLine(
                        color = color,
                        start = Offset(
                            size.width * 0.62f,
                            size.height * 0.22f
                        ),
                        end = Offset(
                            size.width * 0.34f,
                            cy
                        ),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = color,
                        start = Offset(
                            size.width * 0.34f,
                            cy
                        ),
                        end = Offset(
                            size.width * 0.62f,
                            size.height * 0.78f
                        ),
                        strokeWidth = stroke
                    )
                }

                ReadReaderToolbarSymbol
                    .More -> {
                    listOf(
                        size.width * 0.30f,
                        size.width * 0.50f,
                        size.width * 0.70f
                    ).forEach { x ->
                        drawCircle(
                            color = color,
                            radius =
                                stroke * 0.9f,
                            center = Offset(
                                x,
                                cy
                            )
                        )
                    }
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
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                FloentlyDesignTokens
                    .Space
                    .s3
            ),
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    FloentlyDesignTokens
                        .Radius
                        .xl
                )
            )
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface2
            )
            .border(
                width = 1.dp,
                color =
                    FloentlyDesignTokens
                        .Colors
                        .borderSoft,
                shape = RoundedCornerShape(
                    FloentlyDesignTokens
                        .Radius
                        .xl
                )
            )
            .padding(
                FloentlyDesignTokens
                    .Space
                    .s5
            ),
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
        verticalArrangement =
            Arrangement.spacedBy(
                FloentlyDesignTokens
                    .Space
                    .s2
            ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    FloentlyDesignTokens
                        .Radius
                        .l
                )
            )
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface1
            )
            .padding(
                FloentlyDesignTokens
                    .Space
                    .s4
            )
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
            .clip(
                RoundedCornerShape(
                    FloentlyDesignTokens
                        .Radius
                        .m
                )
            )
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface1
            )
            .border(
                width = 1.dp,
                color =
                    FloentlyDesignTokens
                        .Colors
                        .borderSoft,
                shape = RoundedCornerShape(
                    FloentlyDesignTokens
                        .Radius
                        .m
                )
            )
            .padding(
                horizontal =
                    FloentlyDesignTokens
                        .Space
                        .s3,
                vertical = 10.dp
            )
    ) {
        Text(
            "!",
            color = FloentlyDesignTokens.Colors.warning,
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
