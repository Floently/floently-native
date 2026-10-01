package com.floently.read

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.floently.shared.api.FloentlyApiClient
import com.floently.shared.auth.FloentlySecureSessionStore

data class ReadVoiceOption(
    val id: String,
    val name: String,
    val language: String,
    val locale: String,
    val gender: String? = null,
    val accent: String? = null
)

class ReadVoiceSettings(
    context: Context
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "floently_read_voice_settings",
        Context.MODE_PRIVATE
    )
    private val sessionStore =
        FloentlySecureSessionStore(appContext)

    var voices by mutableStateOf<List<ReadVoiceOption>>(
        emptyList()
    )
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var selectionRevision by mutableIntStateOf(0)
        private set

    fun voiceId(language: String): String {
        val key = languageKey(language)
        return preferences.getString(
            "voice::" + key,
            null
        )?.takeIf { it.isNotBlank() }
            ?: defaultVoiceId(language)
    }

    fun select(
        voiceId: String,
        language: String
    ) {
        val value = voiceId.trim()
        if (value.isBlank()) return

        preferences.edit()
            .putString(
                "voice::" + languageKey(language),
                value
            )
            .apply()

        selectionRevision += 1
    }

    fun availableVoices(
        language: String
    ): List<ReadVoiceOption> {
        val key = languageKey(language)
        val matching = voices.filter {
            languageKey(
                it.language.ifBlank { it.locale }
            ) == key
        }

        return if (matching.isNotEmpty()) {
            matching
        } else {
            voices
        }
    }

    suspend fun refresh() {
        if (isLoading) return
        isLoading = true
        loadError = null

        try {
            val api = FloentlyApiClient(
                baseUrl = "https://flowreader-api.onrender.com",
                tokenProvider = {
                    sessionStore.session?.token
                }
            )
            val payload = api.get("/api/voices/unified")
            val array = when {
                payload.has("voices") ->
                    payload.optJSONArray("voices")
                payload.has("available") ->
                    payload.optJSONArray("available")
                else -> null
            }

            val parsed = buildList {
                if (array != null) {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index)
                            ?: continue
                        val id = item.optString("id").trim()
                        if (id.isBlank()) continue

                        add(
                            ReadVoiceOption(
                                id = id,
                                name = item.optString("name")
                                    .ifBlank {
                                        item.optString("voiceName")
                                    }
                                    .ifBlank { id },
                                language =
                                    item.optString("language"),
                                locale =
                                    item.optString("locale"),
                                gender =
                                    item.optString("gender")
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                accent =
                                    item.optString("accent")
                                        .takeIf {
                                            it.isNotBlank()
                                        }
                            )
                        )
                    }
                }
            }

            voices = if (parsed.isNotEmpty()) {
                parsed
            } else {
                fallbackVoices
            }
        } catch (error: Exception) {
            loadError = error.localizedMessage
            if (voices.isEmpty()) {
                voices = fallbackVoices
            }
        } finally {
            isLoading = false
        }
    }

    companion object {
        fun defaultVoiceId(
            language: String
        ): String {
            val primary = language
                .trim()
                .lowercase()
                .substringBefore('-')

            return if (primary == "fi") {
                "azure:fi-FI-SelmaNeural"
            } else {
                "google:en-US-Neural2-C"
            }
        }

        private val fallbackVoices = listOf(
            ReadVoiceOption(
                id = "google:en-US-Neural2-C",
                name = "English Neural",
                language = "en",
                locale = "en-US"
            ),
            ReadVoiceOption(
                id = "azure:fi-FI-SelmaNeural",
                name = "Selma",
                language = "fi",
                locale = "fi-FI"
            )
        )

        private fun languageKey(
            language: String
        ): String =
            language.trim()
                .lowercase()
                .substringBefore('-')
                .ifBlank { "auto" }
    }
}
