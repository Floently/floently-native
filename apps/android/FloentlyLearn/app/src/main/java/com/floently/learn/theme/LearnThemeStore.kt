package com.floently.learn.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.floently.shared.design.FloentlyThemeMode

private const val ThemePrefsName = "floently_learn_theme"
private const val ThemeModeKey = "theme_mode"

fun loadFloentlyThemeMode(context: Context): FloentlyThemeMode {
    val raw = context.getSharedPreferences(ThemePrefsName, Context.MODE_PRIVATE)
        .getString(ThemeModeKey, FloentlyThemeMode.System.name)
    return runCatching { FloentlyThemeMode.valueOf(raw ?: FloentlyThemeMode.System.name) }
        .getOrDefault(FloentlyThemeMode.System)
}

fun persistFloentlyThemeMode(context: Context, mode: FloentlyThemeMode) {
    context.getSharedPreferences(ThemePrefsName, Context.MODE_PRIVATE)
        .edit()
        .putString(ThemeModeKey, mode.name)
        .apply()
}

@Composable
fun rememberFloentlyThemeModeState(context: Context) =
    remember { mutableStateOf(loadFloentlyThemeMode(context)) }
