package com.floently.learn.yki

import androidx.compose.runtime.Composable
import com.floently.learn.i18n.LearnCopy

enum class YkiScreenMode {
    Practice,
    MockExam
}

@Composable
fun YkiFeatureScreen(
    repository: YkiRepository,
    copy: LearnCopy,
    mode: YkiScreenMode = YkiScreenMode.Practice,
    onBack: () -> Unit
) {
    YkiExactSectionScreen(
        mode = mode,
        onBack = onBack
    )
}
