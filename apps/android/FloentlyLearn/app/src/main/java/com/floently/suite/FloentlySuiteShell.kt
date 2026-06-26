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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.create.CreateStudioShell
import com.floently.create.PreviewCreateStudioRepository
import com.floently.learn.app.LearnSignedInShell
import com.floently.read.PreviewReadRepository
import com.floently.read.ReadShell
import com.floently.shared.access.FloentlyAccessRepository
import com.floently.shared.access.FloentlyAccessResult
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.backend.FloentlyBackendDashboardState
import com.floently.shared.backend.PreviewFloentlyBackendRepository
import com.floently.shared.billing.FloentlyBillingDashboardState
import com.floently.shared.billing.PreviewFloentlyBillingRepository
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import com.floently.shared.release.FloentlyReleaseReadinessState
import com.floently.shared.release.PreviewFloentlyReleaseReadinessRepository
import kotlinx.coroutines.launch

@Composable
fun FloentlySuiteShell(
    session: FloentlyAuthSession,
    accessRepository: FloentlyAccessRepository,
    onSignOut: () -> Unit
) {
    var selectedProduct by remember { mutableStateOf<FloentlySuiteProduct?>(null) }
    var accessState by remember { mutableStateOf<FloentlySuiteAccessState?>(null) }
    var billingState by remember { mutableStateOf<FloentlyBillingDashboardState?>(null) }
    var backendState by remember { mutableStateOf<FloentlyBackendDashboardState?>(null) }
    var releaseState by remember { mutableStateOf<FloentlyReleaseReadinessState?>(null) }
    val readRepository = remember { PreviewReadRepository() }
    val createRepository = remember { PreviewCreateStudioRepository() }
    val billingRepository = remember { PreviewFloentlyBillingRepository() }
    val backendRepository = remember { PreviewFloentlyBackendRepository() }
    val releaseRepository = remember { PreviewFloentlyReleaseReadinessRepository() }
    val scope = rememberCoroutineScope()
    val selected = selectedProduct

    LaunchedEffect(Unit) {
        billingState = billingRepository.dashboard()
        backendState = backendRepository.dashboard()
        releaseState = releaseRepository.dashboard()
    }

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
        selected == null -> FloentlyProductSelector(
            session = session,
            billingState = billingState,
            backendState = backendState,
            releaseState = releaseState,
            onPrepareCheckout = { product ->
                scope.launch { billingState = billingRepository.prepareCheckout(product.accessProduct) }
            },
            onSelect = { selectedProduct = it },
            onSignOut = onSignOut
        )
        accessState?.isChecking == true -> FloentlySuiteMessage(selected, "Checking ${selected.title} access...", "Each product is checked separately.")
        accessState?.isAllowed == true -> when (selected) {
            FloentlySuiteProduct.Learn -> LearnSignedInShell(session, onSignOut, onBackToSuite = { selectedProduct = null })
            FloentlySuiteProduct.Read -> ReadShell(session, repository = readRepository, onBackToSuite = { selectedProduct = null })
            FloentlySuiteProduct.Create -> CreateStudioShell(session, repository = createRepository, onBackToSuite = { selectedProduct = null })
        }
        else -> FloentlySuiteBlocked(selected, accessState?.message ?: "This product needs its own access.", onBack = { selectedProduct = null })
    }
}

@Composable
private fun FloentlyProductSelector(
    session: FloentlyAuthSession,
    billingState: FloentlyBillingDashboardState?,
    backendState: FloentlyBackendDashboardState?,
    releaseState: FloentlyReleaseReadinessState?,
    onPrepareCheckout: (FloentlySuiteProduct) -> Unit,
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

            releaseState?.let { state ->
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text("Release readiness", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Verified: ${state.verifiedCount} | Wiring: ${state.wiringCount} | Blockers: ${state.blockerCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Store ready: ${state.storeReady}", style = MaterialTheme.typography.bodySmall)
                    state.gates.take(5).forEach { gate ->
                        Text("${gate.title}: ${gate.status.name}", style = MaterialTheme.typography.bodySmall)
                        Text("Next: ${gate.nextAction}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            backendState?.let { state ->
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text("Backend contracts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ready: ${state.readyCount} | Pending services: ${state.pendingCount}", style = MaterialTheme.typography.bodySmall)
                    state.contracts.take(4).forEach { contract ->
                        Text("${contract.title}: ${contract.status.name}", style = MaterialTheme.typography.bodySmall)
                        Text("${contract.method.name} ${contract.pathTemplate}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            billingState?.latestCheckoutIntent?.let { intent ->
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text("Checkout boundary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Product: ${intent.product.apiName}", style = MaterialTheme.typography.bodySmall)
                    Text("Plan: ${intent.planId}", style = MaterialTheme.typography.bodySmall)
                    Text("Status: ${intent.status.name}", style = MaterialTheme.typography.bodySmall)
                    Text(intent.message, style = MaterialTheme.typography.bodySmall)
                }
            }

            FloentlySuiteProduct.entries.forEach { product ->
                val plan = billingState?.plans?.firstOrNull { it.product == product.accessProduct }
                FloentlyCard(product = product.designProduct) {
                    Text(product.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(product.subtitle, style = MaterialTheme.typography.bodyMedium)
                    plan?.let {
                        Text("Plan: ${it.displayPrice}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${it.status.name}", style = MaterialTheme.typography.bodySmall)
                        Text("Checkout: ${it.checkoutStatus.name}", style = MaterialTheme.typography.bodySmall)
                        Text(it.accessNote, style = MaterialTheme.typography.bodySmall)
                    } ?: Text("Plan boundary loading...", style = MaterialTheme.typography.bodySmall)
                    FloentlyPrimaryButton("Open ${product.title}", product.designProduct, onClick = { onSelect(product) })
                    FloentlyPrimaryButton("Prepare checkout", product.designProduct, onClick = { onPrepareCheckout(product) })
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
