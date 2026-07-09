package com.floently.learn.webentry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floently.learn.auth.LearnAuthMode
import com.floently.learn.auth.LearnAuthScreen
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

enum class M36PublicRoute {
    FloentlyLanding,
    LearnLanding,
    ProductGateway,
    EmployersCities,
    BookDemo,
    Support,
    Legal,
    SignIn,
    CreateAccount,
    ForgotPassword,
    ResetPassword,
    Placement,
    Account,
    Billing,
    Checkout,
    CheckoutSuccess,
    CheckoutCancel
}

@Composable
fun M36PublicFlowRouter(
    isBusy: Boolean,
    errorMessage: String?,
    onSubmit: (LearnAuthMode, String, String, String?) -> Unit
) {
    var route by remember { mutableStateOf(M36PublicRoute.FloentlyLanding) }

    when (route) {
        M36PublicRoute.FloentlyLanding -> M35PublicFloentlyLandingNativeScreen(
            onBookDemo = { route = M36PublicRoute.BookDemo },
            onSignIn = { route = M36PublicRoute.LearnLanding },
            onSupport = { route = M36PublicRoute.Support },
            onLegal = { route = M36PublicRoute.Legal }
        )

        M36PublicRoute.LearnLanding -> M35LearnLandingNativeScreen(
            onStartPlacement = { route = M36PublicRoute.Placement },
            onSignIn = { route = M36PublicRoute.SignIn },
            onBookDemo = { route = M36PublicRoute.BookDemo }
        )

        M36PublicRoute.Placement -> M35OnboardingPlacementNativeScreen(
            onComplete = { route = M36PublicRoute.SignIn },
            onSkip = { route = M36PublicRoute.SignIn }
        )

        M36PublicRoute.SignIn -> LearnAuthScreen(
            isBusy = isBusy,
            errorMessage = errorMessage,
            initialMode = LearnAuthMode.SignIn,
            onForgotPassword = { route = M36PublicRoute.ForgotPassword },
            onSubmit = onSubmit
        )

        M36PublicRoute.CreateAccount -> LearnAuthScreen(
            isBusy = isBusy,
            errorMessage = errorMessage,
            initialMode = LearnAuthMode.Create,
            onForgotPassword = { route = M36PublicRoute.ForgotPassword },
            onSubmit = onSubmit
        )

        else -> M36RouteInfoScreen(
            route = route,
            onBack = {
                route = when (route) {
                    M36PublicRoute.ForgotPassword,
                    M36PublicRoute.ResetPassword -> M36PublicRoute.SignIn
                    M36PublicRoute.CheckoutSuccess,
                    M36PublicRoute.CheckoutCancel -> M36PublicRoute.Billing
                    else -> M36PublicRoute.LearnLanding
                }
            },
            onRoute = { route = it }
        )
    }
}

@Composable
private fun M36RouteInfoScreen(
    route: M36PublicRoute,
    onBack: () -> Unit,
    onRoute: (M36PublicRoute) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val externalUrl = route.externalUrl()

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = route.titleText(),
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = route.bodyText(),
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            externalUrl?.let { url ->
                FloentlyPrimaryButton(
                    title = "Open link",
                    product = FloentlyProduct.Learn,
                    onClick = { uriHandler.openUri(url) }
                )
            }

            M36RouteButton("Product gateway", M36PublicRoute.ProductGateway, palette, onRoute)
            M36RouteButton("Create account", M36PublicRoute.CreateAccount, palette, onRoute)
            M36RouteButton("Account", M36PublicRoute.Account, palette, onRoute)
            M36RouteButton("Billing", M36PublicRoute.Billing, palette, onRoute)
            M36RouteButton("Checkout", M36PublicRoute.Checkout, palette, onRoute)
            M36RouteButton("Checkout success", M36PublicRoute.CheckoutSuccess, palette, onRoute)
            M36RouteButton("Checkout cancel", M36PublicRoute.CheckoutCancel, palette, onRoute)
            M36RouteButton("Back", null, palette) { onBack() }
        }
    }
}

@Composable
private fun M36RouteButton(
    label: String,
    target: M36PublicRoute?,
    palette: FloentlyPalette,
    onRoute: (M36PublicRoute) -> Unit
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (target != null) onRoute(target)
            }
    ) {
        Row(modifier = Modifier.padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.text, fontWeight = FontWeight.Bold)
            Text("->", color = palette.primary, textAlign = TextAlign.End)
        }
    }
}

private fun M36PublicRoute.titleText(): String = when (this) {
    M36PublicRoute.ProductGateway -> "Floently product gateway"
    M36PublicRoute.EmployersCities -> "Organizations, employers, and cities"
    M36PublicRoute.BookDemo -> "Book a demo"
    M36PublicRoute.Support -> "Contact and support"
    M36PublicRoute.Legal -> "Privacy and terms"
    M36PublicRoute.ForgotPassword -> "Forgot password"
    M36PublicRoute.ResetPassword -> "Reset password"
    M36PublicRoute.Account -> "Account route"
    M36PublicRoute.Billing -> "Billing route"
    M36PublicRoute.Checkout -> "Checkout / payment route"
    M36PublicRoute.CheckoutSuccess -> "Payment success"
    M36PublicRoute.CheckoutCancel -> "Payment cancelled"
    else -> "Floently route"
}

private fun M36PublicRoute.bodyText(): String = when (this) {
    M36PublicRoute.ProductGateway -> "Gateway route for choosing Learn, Read, or Create without dead navigation."
    M36PublicRoute.EmployersCities -> "Public route for employer, organization, city, and programme access."
    M36PublicRoute.BookDemo -> "Demo requests route to Floently pilot contact."
    M36PublicRoute.Support -> "Support and contact route for public and signed-out users."
    M36PublicRoute.Legal -> "Legal route for privacy policy and terms of use."
    M36PublicRoute.ForgotPassword -> "Password reset request route. Backend wiring remains with auth integration."
    M36PublicRoute.ResetPassword -> "Reset password completion route for deep links and recovery."
    M36PublicRoute.Account -> "Signed-in users reach account from the Learn shell. Signed-out users are routed to auth first."
    M36PublicRoute.Billing -> "Billing and plan route boundary before Google Play Billing / web checkout integration."
    M36PublicRoute.Checkout -> "Checkout/payment route boundary for Android billing and Stripe/web handoff."
    M36PublicRoute.CheckoutSuccess -> "Payment success route boundary."
    M36PublicRoute.CheckoutCancel -> "Payment cancel route boundary."
    else -> "Public route boundary."
}

private fun M36PublicRoute.externalUrl(): String? = when (this) {
    M36PublicRoute.BookDemo -> "mailto:pilots@floently.com?subject=Floently%20demo%20request"
    M36PublicRoute.Support -> "https://learn.floently.com/support"
    M36PublicRoute.Legal -> "https://learn.floently.com/privacy-policy"
    else -> null
}
