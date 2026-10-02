package com.floently.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.floently.learn.auth.LearnAuthScreen
import com.floently.learn.home.KieliValmisHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val isAuthenticated = remember { mutableStateOf(false) }

                if (isAuthenticated.value) {
                    KieliValmisHomeScreen()
                } else {
                    LearnAuthScreen(onContinue = { isAuthenticated.value = true })
                }
            }
        }
    }
}
