package com.floently.suite

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.create.CreateStudioShell
import com.floently.learn.app.LearnSignedInShell
import com.floently.read.ReadShell
import com.floently.shared.access.FloentlyAccessRepository
import com.floently.shared.access.FloentlyAccessResult
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun FloentlySuiteShell(
    session: FloentlyAuthSession,
    accessRepository: FloentlyAccessRepository,
    onSignOut: () -> Unit
) {
    var selectedProduct by remember { mutableStateOf<FloentlySuiteProduct?>(null) }
    var accessState by remember { mutableStateOf<FloentlySuiteAccessState?>(null) }
    val selected = selectedProduct

    LaunchedEffect(selected) {
        val product = selected ?: return@LaunchedEffect
        accessState = FloentlySuiteAccessState(product, true, false, null)
        accessState = when (val result = accessRepository.requireAccess(product.accessProduct)) {
            is FloentlyAccessResult.Allowed -> FloentlySuiteAccessState(product, false, true, null)
            is FloentlyAccessResult.Blocked -> FloentlySuiteAccessState(product, false, false, result.reason)
            is FloentlyAccessResult.Error -> FloentlySuiteAccessState(product, false, false, result.message)
        }
    }

    when {
        selected == null -> FloentlyProductSelector(session, onSelect = { selectedProduct = it }, onSignOut = onSignOut)
        accessState?.isChecking == true -> FloentlySuiteMessage(selected, "Checking ${selected.title} access...", "Each product is checked separately.")
        accessState?.isAllowed == true -> when (selected) {
            FloentlySuiteProduct.Learn -> LearnSignedInShell(session, onSignOut, onBackToSuite = { selectedProduct = null })
            FloentlySuiteProduct.Read -> ReadShell(session, onBackToSuite = { selectedProduct = null })
            FloentlySuiteProduct.Create -> CreateStudioShell(session, onBackToSuite = { selectedProduct = null })
        }
        else -> FloentlySuiteBlocked(selected, accessState?.message ?: "This product needs its own access.", onBack = { selectedProduct = null })
    }
}

@Composable
private fun FloentlyProductSelector(
    session: FloentlyAuthSession,
    onSelect: (FloentlySuiteProduct) -> Unit,
    onSignOut: () -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Floently", color = palette.text, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Signed in as ${session.user.email}", color = palette.muted, style = MaterialTheme.typography.titleMedium)
            Text("Choose a product. Learn, Read, and Create are checked separately.", color = palette.muted, style = MaterialTheme.typography.bodyLarge)
            FloentlySuiteProduct.entries.forEach { product ->
                FloentlyCard(product = product.designProduct) {
                    Text(product.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(product.subtitle, style = MaterialTheme.typography.bodyMedium)
                    FloentlyPrimaryButton("Open ${product.title}", product.designProduct, onClick = { onSelect(product) })
                }
            }
            FloentlyPrimaryButton("Sign out", FloentlyProduct.Learn, onClick = onSignOut)
        }
    }
}

@Composable
private fun FloentlySuiteMessage(product: FloentlySuiteProduct, title: String, body: String) {
    FloentlyScreen(product = product.designProduct) { palette ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FloentlySuiteBlocked(product: FloentlySuiteProduct, message: String, onBack: () -> Unit) {
    FloentlyScreen(product = product.designProduct) { palette ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${product.title} access needed", color = palette.text, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(message, color = palette.muted, style = MaterialTheme.typography.titleMedium)
            FloentlyCard(product = product.designProduct) {
                Text("Separate product access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Learn, Read, and Create Studio remain separate unless a bundle is added later.", style = MaterialTheme.typography.bodyMedium)
            }
            FloentlyPrimaryButton("Back to Floently", product.designProduct, onClick = onBack)
        }
    }
}
