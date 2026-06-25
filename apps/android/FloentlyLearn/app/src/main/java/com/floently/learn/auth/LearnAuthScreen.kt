package com.floently.learn.auth

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
    onContinue: () -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (mode == AuthMode.SignIn) "Welcome back" else "Create your account",
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
                        selected = mode == AuthMode.SignIn,
                        onClick = { mode = AuthMode.SignIn },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Sign in") }

                    SegmentedButton(
                        selected = mode == AuthMode.Create,
                        onClick = { mode = AuthMode.Create },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Create") }
                }

                if (mode == AuthMode.Create) {
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
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                if (error != null) {
                    Text(text = error.orEmpty(), color = androidx.compose.ui.graphics.Color.Red)
                }

                FloentlyPrimaryButton(
                    title = if (isBusy) "Please wait..." else if (mode == AuthMode.SignIn) "Sign in" else "Create account",
                    product = FloentlyProduct.Learn
                ) {
                    if (email.isBlank() || password.length < 6) {
                        error = "Enter a valid email and at least 6 characters."
                    } else {
                        isBusy = true
                        error = null
                        onContinue()
                    }
                }
            }
        }
    }
}

private enum class AuthMode {
    SignIn,
    Create
}
