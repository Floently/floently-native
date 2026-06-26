package com.floently.read

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun ReadShell(
    session: FloentlyAuthSession,
    repository: ReadRepository,
    onBackToSuite: () -> Unit
) {
    var dashboardState by remember { mutableStateOf<ReadDashboardState?>(null) }
    var inputText by remember { mutableStateOf("") }
    var readAutomatically by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository) {
        dashboardState = repository.dashboard()
        readAutomatically = dashboardState?.draft?.readAutomatically ?: true
    }

    FloentlyScreen(product = FloentlyProduct.Read) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Floently Read",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Signed in as ${session.user.email}",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Read) {
                Text(text = "Import", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Paste text or URL now. Native file picker and backend upload plug into this boundary next.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Text or URL") }
                )
                Text(text = "Read automatically: $readAutomatically", style = MaterialTheme.typography.bodySmall)
                FloentlyPrimaryButton(
                    title = if (readAutomatically) "Turn automatic reading off" else "Turn automatic reading on",
                    product = FloentlyProduct.Read,
                    onClick = {
                        scope.launch {
                            readAutomatically = !readAutomatically
                            dashboardState = repository.toggleReadAutomatically(readAutomatically)
                        }
                    }
                )
                FloentlyPrimaryButton(
                    title = "Detect language",
                    product = FloentlyProduct.Read,
                    onClick = { scope.launch { dashboardState = dashboardState?.copy(draft = repository.detectLanguage(inputText)) } }
                )
                FloentlyPrimaryButton(
                    title = "Generate reading session",
                    product = FloentlyProduct.Read,
                    onClick = { scope.launch { dashboardState = repository.generate(inputText, readAutomatically) } }
                )
            }

            dashboardState?.draft?.let { draft ->
                FloentlyCard(product = FloentlyProduct.Read) {
                    Text(text = "Generation status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Source: ${draft.sourceType.name}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Language: ${draft.detectedLanguage.name}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Status: ${draft.status.name}", style = MaterialTheme.typography.bodySmall)
                    draft.message?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                }
            }

            dashboardState?.activeSession?.let { sessionState ->
                FloentlyCard(product = FloentlyProduct.Read) {
                    Text(text = "Reader", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = sessionState.document.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = sessionState.currentChunk, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Progress: ${sessionState.progressPercent}%", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Playing: ${sessionState.isPlaying}", style = MaterialTheme.typography.bodySmall)
                }
            }

            dashboardState?.savedDocuments?.let { documents ->
                FloentlyCard(product = FloentlyProduct.Read) {
                    Text(text = "Saved library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    documents.forEach { document ->
                        Text(text = document.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${document.language.name}: ${document.preview}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            FloentlyPrimaryButton(title = "Back to Floently", product = FloentlyProduct.Read, onClick = onBackToSuite)
        }
    }
}
