package com.floently.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.floently.learn.auth.LearnAuthScreen
import com.floently.learn.design.KVColor
import com.floently.learn.design.KVSpacing
import com.floently.learn.home.KieliValmisHomeScreen
import com.floently.learn.state.LearnAppPhase
import com.floently.learn.state.LearnAppViewModel

class MainActivity : ComponentActivity() {
    private val appState: LearnAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val ui by appState.uiState.collectAsState()

                when (ui.phase) {
                    LearnAppPhase.Bootstrapping -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(KVColor.Canvas)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(KVSpacing.m)
                            ) {
                                CircularProgressIndicator(color = KVColor.BrandBright)
                                Text(
                                    text = "KieliValmis",
                                    color = KVColor.TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Checking your session…",
                                    color = KVColor.TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    LearnAppPhase.SignedOut -> {
                        LearnAuthScreen(appState = appState)
                    }

                    LearnAppPhase.SignedIn -> {
                        KieliValmisHomeScreen()
                    }
                }
            }
        }
    }
}
