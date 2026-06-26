package com.floently.learn.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnLoadingScreen() {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
        ) {
            CircularProgressIndicator(color = palette.accent)
            Text(
                text = "Opening Floently Learn",
                color = palette.text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
