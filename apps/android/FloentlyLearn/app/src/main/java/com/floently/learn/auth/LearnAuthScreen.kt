package com.floently.learn.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnAuthScreen(
    isBusy: Boolean,
    errorMessage: String?,
    initialMode: LearnAuthMode = LearnAuthMode.SignIn,
    onForgotPassword: (() -> Unit)? = null,
    onSubmit: (LearnAuthMode, String, String, String?) -> Unit
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (mode == LearnAuthMode.SignIn) "Welcome back" else "Create your account",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Continue to Floently Learn with your existing Floently account.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(28.dp))
            FloentlyCard(product = FloentlyProduct.Learn) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = mode == LearnAuthMode.SignIn,
                        onClick = { mode = LearnAuthMode.SignIn },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Sign in") }

                    SegmentedButton(
                        selected = mode == LearnAuthMode.Create,
                        onClick = { mode = LearnAuthMode.Create },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Create") }
                }

                if (mode == LearnAuthMode.Create) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name optional") },
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = credential,
                    onValueChange = { credential = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                if (mode == LearnAuthMode.SignIn) {
                    onForgotPassword?.let { forgot ->
                        Text(
                            text = "Forgot password?",
                            color = palette.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = forgot)
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(text = errorMessage, color = androidx.compose.ui.graphics.Color.Red)
                }

                FloentlyPrimaryButton(
                    title = if (isBusy) "Please wait..." else if (mode == LearnAuthMode.SignIn) "Sign in" else "Create account",
                    product = FloentlyProduct.Learn,
                    onClick = { if (!isBusy) onSubmit(mode, email, credential, name) }
                )
            }
        }
    }
}

enum class LearnAuthMode {
    SignIn,
    Create
}
