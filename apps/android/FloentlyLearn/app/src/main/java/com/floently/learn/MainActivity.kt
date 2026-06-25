package com.floently.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.floently.learn.auth.LearnAuthScreen
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val signedIn = remember { mutableStateOf(false) }

                if (signedIn.value) {
                    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Floently Learn",
                                color = palette.text,
                                style = MaterialTheme.typography.displaySmall
                            )
                            Text(
                                text = "Native build foundation is running.",
                                color = palette.muted
                            )
                        }
                    }
                } else {
                    LearnAuthScreen(onContinue = { signedIn.value = true })
                }
            }
        }
    }
}
