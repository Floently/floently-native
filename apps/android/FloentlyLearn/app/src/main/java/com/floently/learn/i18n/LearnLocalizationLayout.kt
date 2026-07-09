package com.floently.learn.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun LearnLocalizationLayout(
    language: LearnLanguage,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (language.direction == LearnTextDirection.Rtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        content()
    }
}
