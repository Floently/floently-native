package com.floently.read

interface ReadRepository {
    suspend fun dashboard(): ReadDashboardState
    suspend fun detectLanguage(input: String): ReadInputDraft
    suspend fun generate(input: String, readAutomatically: Boolean): ReadDashboardState
    suspend fun toggleReadAutomatically(enabled: Boolean): ReadDashboardState
}

class ServiceReadRepository(
    private val service: ReadService,
    private val fallback: ReadRepository = PreviewReadRepository()
) : ReadRepository {
    override suspend fun dashboard(): ReadDashboardState {
        return runCatching { service.dashboard() }.getOrElse { error ->
            fallback.dashboard().copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Read service is not available from the existing backend yet."
            )
        }
    }

    override suspend fun detectLanguage(input: String): ReadInputDraft {
        return runCatching { service.detectLanguage(input) }.getOrElse { error ->
            fallback.detectLanguage(input).copy(
                message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Read language detection is using the local fallback."
            )
        }
    }

    override suspend fun generate(input: String, readAutomatically: Boolean): ReadDashboardState {
        return runCatching { service.generate(input, readAutomatically) }.getOrElse { error ->
            fallback.generate(input, readAutomatically).copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Read generation service is not available from the existing backend yet."
            )
        }
    }

    override suspend fun toggleReadAutomatically(enabled: Boolean): ReadDashboardState {
        return runCatching { service.toggleReadAutomatically(enabled) }.getOrElse { error ->
            fallback.toggleReadAutomatically(enabled).copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Read settings service is not available from the existing backend yet."
            )
        }
    }
}

class PreviewReadRepository : ReadRepository {
    private var readAutomatically: Boolean = true

    override suspend fun dashboard(): ReadDashboardState = ReadDashboardState(
        draft = ReadInputDraft(
            sourceType = ReadSourceType.Text,
            rawInput = "",
            detectedLanguage = ReadLanguage.Auto,
            readAutomatically = readAutomatically,
            status = ReadGenerationStatus.Idle,
            message = "Paste text or a URL. File upload is next behind the native picker boundary."
        ),
        activeSession = null,
        savedDocuments = previewDocuments(),
        isLoading = false,
        errorMessage = null
    )

    override suspend fun detectLanguage(input: String): ReadInputDraft {
        val language = when {
            input.contains("ä", ignoreCase = true) || input.contains("ö", ignoreCase = true) -> ReadLanguage.Finnish
            input.startsWith("http", ignoreCase = true) -> ReadLanguage.Auto
            input.isBlank() -> ReadLanguage.Auto
            else -> ReadLanguage.English
        }
        return ReadInputDraft(
            sourceType = if (input.startsWith("http", ignoreCase = true)) ReadSourceType.Url else ReadSourceType.Text,
            rawInput = input,
            detectedLanguage = language,
            readAutomatically = readAutomatically,
            status = ReadGenerationStatus.ReadyToGenerate,
            message = "Language boundary ready: ${language.name}."
        )
    }

    override suspend fun generate(input: String, readAutomatically: Boolean): ReadDashboardState {
        this.readAutomatically = readAutomatically
        val detected = detectLanguage(input)
        val document = ReadDocument(
            id = "preview-generated-read",
            title = if (detected.sourceType == ReadSourceType.Url) "URL reading preview" else "Generated reading preview",
            language = detected.detectedLanguage,
            preview = input.take(140).ifBlank { "Preview reading text will appear here." },
            createdText = "Generated from native Read boundary"
        )
        val session = ReadReaderSession(
            document = document,
            currentChunk = document.preview,
            progressPercent = if (readAutomatically) 12 else 0,
            isPlaying = readAutomatically,
            readAutomatically = readAutomatically
        )
        return ReadDashboardState(
            draft = detected.copy(status = ReadGenerationStatus.Generated, message = "Reader session generated."),
            activeSession = session,
            savedDocuments = listOf(document) + previewDocuments(),
            isLoading = false,
            errorMessage = null
        )
    }

    override suspend fun toggleReadAutomatically(enabled: Boolean): ReadDashboardState {
        readAutomatically = enabled
        val current = dashboard()
        return current.copy(
            draft = current.draft.copy(
                readAutomatically = enabled,
                message = if (enabled) "Read automatically is on." else "Read automatically is off."
            )
        )
    }

    private fun previewDocuments(): List<ReadDocument> = listOf(
        ReadDocument(
            id = "read-preview-finnish",
            title = "Finnish article preview",
            language = ReadLanguage.Finnish,
            preview = "Tämä on tallennettu lukuesimerkki.",
            createdText = "Preview"
        ),
        ReadDocument(
            id = "read-preview-work",
            title = "Work text preview",
            language = ReadLanguage.English,
            preview = "Saved reading items will appear here after generation.",
            createdText = "Preview"
        )
    )
}
