package com.floently.learn.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PreferencesName = "floently_learn_preferences"
private const val LanguageCodeKey = "learn_language_code"

@Composable
fun rememberLearnLanguageState(): MutableState<LearnLanguage> {
    val context = LocalContext.current
    return remember(context) {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        mutableStateOf(LearnLanguage.fromCode(preferences.getString(LanguageCodeKey, LearnLanguage.EN.code)))
    }
}

fun persistLearnLanguage(context: Context, language: LearnLanguage) {
    context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(LanguageCodeKey, language.code)
        .apply()
}
