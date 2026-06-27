package com.floently.learn.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val LearnLanguagePrefsName = "floently_learn_language"
private const val LearnLanguageKey = "selected_language"

data class LearnLanguageController(
    val language: LearnLanguage,
    val copy: LearnCopy,
    val selectLanguage: (LearnLanguage) -> Unit
)

@Composable
fun rememberLearnLanguageController(): LearnLanguageController {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(LearnLanguagePrefsName, Context.MODE_PRIVATE)
    }
    var selectedLanguage by remember {
        mutableStateOf(LearnLanguage.fromCode(preferences.getString(LearnLanguageKey, null)))
    }

    return LearnLanguageController(
        language = selectedLanguage,
        copy = LearnTranslations.copy(selectedLanguage),
        selectLanguage = { language ->
            selectedLanguage = language
            preferences.edit().putString(LearnLanguageKey, language.code).apply()
        }
    )
}
