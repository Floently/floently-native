package com.floently.create

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
fun CreateStudioShell(
    session: FloentlyAuthSession,
    repository: CreateStudioRepository,
    onBackToSuite: () -> Unit
) {
    var dashboardState by remember { mutableStateOf<CreateStudioDashboardState?>(null) }
    var selectedTool by remember { mutableStateOf(CreateStudioToolType.Hooks) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository) {
        dashboardState = repository.dashboard()
        selectedTool = dashboardState?.draft?.selectedTool ?: CreateStudioToolType.Hooks
    }

    FloentlyScreen(product = FloentlyProduct.Create) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Floently Create Studio",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Signed in as ${session.user.email}",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )
            FloentlyCard(product = FloentlyProduct.Create) {
                Text(text = "Create-only native studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Create has its own dashboard, tools, result flow, and projects. It is not mixed with Read.", style = MaterialTheme.typography.bodyMedium)
            }

            dashboardState?.tools?.let { tools ->
                FloentlyCard(product = FloentlyProduct.Create) {
                    Text(text = "Direct functions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    tools.forEach { tool ->
                        Text(text = tool.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = tool.subtitle, style = MaterialTheme.typography.bodySmall)
                        FloentlyPrimaryButton(
                            title = "Use ${tool.title}",
                            product = FloentlyProduct.Create,
                            onClick = {
                                scope.launch {
                                    selectedTool = tool.type
                                    inputText = ""
                                    dashboardState = repository.selectTool(tool.type)
                                }
                            }
                        )
                    }
                }
            }

            FloentlyCard(product = FloentlyProduct.Create) {
                Text(text = "Input", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Selected: ${selectedTool.name}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Source text") }
                )
                FloentlyPrimaryButton(
                    title = "Generate",
                    product = FloentlyProduct.Create,
                    onClick = { scope.launch { dashboardState = repository.generate(selectedTool, inputText) } }
                )
            }

            dashboardState?.draft?.let { draft ->
                FloentlyCard(product = FloentlyProduct.Create) {
                    Text(text = "Run status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Tool: ${draft.selectedTool.name}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Status: ${draft.status.name}", style = MaterialTheme.typography.bodySmall)
                    draft.message?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                }
            }

            dashboardState?.latestResult?.let { result ->
                FloentlyCard(product = FloentlyProduct.Create) {
                    Text(text = "Result", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = result.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = result.body, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Export ready: ${result.exportReady}", style = MaterialTheme.typography.bodySmall)
                }
            }

            dashboardState?.projects?.let { projects ->
                FloentlyCard(product = FloentlyProduct.Create) {
                    Text(text = "Projects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    projects.forEach { project ->
                        Text(text = project.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = project.summary, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Last edited: ${project.lastEditedText}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            FloentlyPrimaryButton(title = "Back to Floently", product = FloentlyProduct.Create, onClick = onBackToSuite)
        }
    }
}
