package com.floently.read

enum class ReadSourceType {
    Text,
    Url,
    File
}

enum class ReadLanguage {
    Auto,
    Finnish,
    English,
    Swedish,
    French,
    Unknown
}

enum class ReadGenerationStatus {
    Idle,
    DetectingLanguage,
    ReadyToGenerate,
    Generated,
    ServicePending
}

data class ReadInputDraft(
    val sourceType: ReadSourceType,
    val rawInput: String,
    val detectedLanguage: ReadLanguage,
    val readAutomatically: Boolean,
    val status: ReadGenerationStatus,
    val message: String?
)

data class ReadDocument(
    val id: String,
    val title: String,
    val language: ReadLanguage,
    val preview: String,
    val createdText: String
)

data class ReadReaderSession(
    val document: ReadDocument,
    val currentChunk: String,
    val progressPercent: Int,
    val isPlaying: Boolean,
    val readAutomatically: Boolean
)

data class ReadDashboardState(
    val draft: ReadInputDraft,
    val activeSession: ReadReaderSession?,
    val savedDocuments: List<ReadDocument>,
    val isLoading: Boolean,
    val errorMessage: String?
)
