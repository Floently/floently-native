package com.floently.learn.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.design.KVColor
import com.floently.learn.design.KVRadius
import com.floently.learn.design.KVSpacing
import com.floently.learn.design.KieliValmisPrimaryButton
import com.floently.learn.design.KieliValmisSegmentedControl
import com.floently.learn.design.KieliValmisTextField
import com.floently.learn.state.LearnAppViewModel
import kotlinx.coroutines.launch

private enum class AuthMode {
    SignIn,
    Create
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnAuthScreen(
    appState: LearnAppViewModel
) {
    val ui by appState.uiState.collectAsState()

    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordReset by remember { mutableStateOf(false) }

    val nameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val normalizedEmail = email.trim()
    val canSubmit = !ui.isAuthenticating &&
        normalizedEmail.contains("@") &&
        password.length >= 6

    fun submit() {
        if (!canSubmit) return
        focusManager.clearFocus()

        when (mode) {
            AuthMode.SignIn -> appState.signIn(normalizedEmail, password)
            AuthMode.Create -> appState.register(
                email = normalizedEmail,
                password = password,
                name = name
            )
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(KVColor.Canvas)
            .imePadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KVSpacing.xl),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KVSpacing.ml)
                .padding(top = KVSpacing.xl, bottom = KVSpacing.xl)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KVSpacing.sm)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(KVColor.BrandTint, RoundedCornerShape(KVRadius.m))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubble,
                        contentDescription = null,
                        tint = KVColor.BrandBright,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "KieliValmis",
                        color = KVColor.TextPrimary,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Finnish that moves with you",
                        color = KVColor.TextTertiary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.s)) {
                Text(
                    text = if (mode == AuthMode.SignIn) "Welcome back" else "Create your account",
                    color = KVColor.TextPrimary,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Use your Floently account for KieliValmis learning, YKI and professional Finnish.",
                    color = KVColor.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }

            KieliValmisSegmentedControl(
                options = listOf(
                    AuthMode.SignIn to "Sign in",
                    AuthMode.Create to "Create account"
                ),
                selection = mode,
                onSelect = {
                    mode = it
                    appState.clearAuthError()
                    if (it == AuthMode.Create && name.isEmpty()) {
                        nameFocus.requestFocus()
                    }
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(KVSpacing.m)) {
                if (mode == AuthMode.Create) {
                    KieliValmisTextField(
                        label = "Name (optional)",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Your name",
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { emailFocus.requestFocus() }
                        ),
                        modifier = Modifier.focusRequester(nameFocus)
                    )
                }

                KieliValmisTextField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "name@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocus.requestFocus() }
                    ),
                    modifier = Modifier.focusRequester(emailFocus)
                )

                KieliValmisTextField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { submit() }
                    ),
                    trailing = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                                tint = KVColor.TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.focusRequester(passwordFocus)
                )
            }

            ui.authError?.let { message ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KVSpacing.s),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = null,
                        tint = KVColor.Danger,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = message,
                        color = KVColor.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            KieliValmisPrimaryButton(
                title = if (ui.isAuthenticating) {
                    "Please wait…"
                } else if (mode == AuthMode.SignIn) {
                    "Sign in"
                } else {
                    "Create account"
                },
                enabled = canSubmit,
                onClick = ::submit
            )

            if (mode == AuthMode.SignIn) {
                TextButton(
                    onClick = { showPasswordReset = true }
                ) {
                    Text(
                        text = "Forgot password?",
                        color = KVColor.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showPasswordReset) {
        PasswordResetSheet(
            initialEmail = normalizedEmail,
            appState = appState,
            onDismiss = { showPasswordReset = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordResetSheet(
    initialEmail: String,
    appState: LearnAppViewModel,
    onDismiss: () -> Unit
) {
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var isBusy by remember { mutableStateOf(false) }
    var didRequest by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KVColor.Surface1
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KVSpacing.l),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KVSpacing.ml)
                .padding(bottom = KVSpacing.xl)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reset password",
                    color = KVColor.TextPrimary,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = KVColor.TextSecondary
                    )
                }
            }

            if (didRequest) {
                Text(
                    text = "Reset requested",
                    color = KVColor.Success,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "If this address is eligible for password reset, follow the instructions sent by the account service.",
                    color = KVColor.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            } else {
                Text(
                    text = "Enter the email address for your Floently account.",
                    color = KVColor.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                KieliValmisTextField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "name@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send
                    )
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = KVColor.Danger,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                KieliValmisPrimaryButton(
                    title = if (isBusy) "Please wait…" else "Send reset link",
                    enabled = !isBusy && email.contains("@"),
                    onClick = {
                        if (!isBusy && email.contains("@")) {
                            isBusy = true
                            errorMessage = null

                            scope.launch {
                                appState.requestPasswordReset(email)
                                    .onSuccess {
                                        didRequest = true
                                    }
                                    .onFailure { error ->
                                        errorMessage = error.message
                                            ?: "Could not request a password reset. Try again."
                                    }
                                isBusy = false
                            }
                        }
                    }
                )
            }
        }
    }
}
