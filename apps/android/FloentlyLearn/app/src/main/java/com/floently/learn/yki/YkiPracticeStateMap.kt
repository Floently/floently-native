package com.floently.learn.yki

internal enum class YkiPracticeScreenshotPurpose {
    Landing,
    ChoiceInitial,
    ChoiceCorrect,
    ChoiceWrong,
    LongPassage,
    ListeningInitial,
    ListeningCorrect,
    ListeningWrong,
    WritingInitial,
    WritingDraft,
    WritingSaved,
    SpeakingPrompt,
    SpeakingPreparing,
    SpeakingRecording,
    SpeakingSaved
}

internal data class YkiPracticeScreenshotState(
    val screenshot: String,
    val purpose: YkiPracticeScreenshotPurpose,
    val buttonLabel: String,
    val nextButtonLabel: String? = null,
    val prepareSeconds: Int = 0,
    val responseSeconds: Int = 0
)

internal object YkiPracticeStateMap {
    val states: List<YkiPracticeScreenshotState> = listOf(
        YkiPracticeScreenshotState("IMG_0409", YkiPracticeScreenshotPurpose.Landing, "Start exam block"),
        YkiPracticeScreenshotState("IMG_0410", YkiPracticeScreenshotPurpose.ChoiceInitial, "Check answer"),
        YkiPracticeScreenshotState("IMG_0411", YkiPracticeScreenshotPurpose.ChoiceCorrect, "Next task"),
        YkiPracticeScreenshotState("IMG_0412", YkiPracticeScreenshotPurpose.ChoiceCorrect, "Next task"),
        YkiPracticeScreenshotState("IMG_0413", YkiPracticeScreenshotPurpose.LongPassage, "Check answer"),
        YkiPracticeScreenshotState("IMG_0414", YkiPracticeScreenshotPurpose.ChoiceWrong, "Next task"),
        YkiPracticeScreenshotState("IMG_0415", YkiPracticeScreenshotPurpose.ChoiceWrong, "Next task"),
        YkiPracticeScreenshotState("IMG_0416", YkiPracticeScreenshotPurpose.ChoiceWrong, "Next task"),
        YkiPracticeScreenshotState("IMG_0417", YkiPracticeScreenshotPurpose.ListeningInitial, "Play audio", "Check answer"),
        YkiPracticeScreenshotState("IMG_0418", YkiPracticeScreenshotPurpose.ListeningWrong, "Play audio", "Next task"),
        YkiPracticeScreenshotState("IMG_0419", YkiPracticeScreenshotPurpose.ListeningCorrect, "Play audio", "Next task"),
        YkiPracticeScreenshotState("IMG_0420", YkiPracticeScreenshotPurpose.ListeningCorrect, "Play audio", "Next task"),
        YkiPracticeScreenshotState("IMG_0421", YkiPracticeScreenshotPurpose.ListeningWrong, "Play audio", "Next task"),
        YkiPracticeScreenshotState("IMG_0422", YkiPracticeScreenshotPurpose.WritingInitial, "Save answer", "Next task"),
        YkiPracticeScreenshotState("IMG_0423", YkiPracticeScreenshotPurpose.WritingDraft, "Save answer", "Next task"),
        YkiPracticeScreenshotState("IMG_0424", YkiPracticeScreenshotPurpose.WritingDraft, "Save answer", "Next task"),
        YkiPracticeScreenshotState("IMG_0425", YkiPracticeScreenshotPurpose.WritingSaved, "Next task"),
        YkiPracticeScreenshotState("IMG_0426", YkiPracticeScreenshotPurpose.WritingDraft, "Save answer", "Next task"),
        YkiPracticeScreenshotState("IMG_0427", YkiPracticeScreenshotPurpose.WritingSaved, "Next task"),
        YkiPracticeScreenshotState("IMG_0428", YkiPracticeScreenshotPurpose.SpeakingPrompt, "Start conversation roleplay", "Next task", prepareSeconds = 30, responseSeconds = 60),
        YkiPracticeScreenshotState("IMG_0429", YkiPracticeScreenshotPurpose.SpeakingPrompt, "Start conversation roleplay", "Next task", prepareSeconds = 30, responseSeconds = 60),
        YkiPracticeScreenshotState("IMG_0430", YkiPracticeScreenshotPurpose.SpeakingPrompt, "Start conversation roleplay", "Next task", prepareSeconds = 30, responseSeconds = 60),
        YkiPracticeScreenshotState("IMG_0431", YkiPracticeScreenshotPurpose.SpeakingPrompt, "Start conversation roleplay", "Next task", prepareSeconds = 30, responseSeconds = 60)
    )

    fun forTask(task: YkiPracticeBankTask): YkiPracticeScreenshotState =
        states.firstOrNull { state -> task.screenshot.contains(state.screenshot) }
            ?: YkiPracticeScreenshotState(task.screenshot, YkiPracticeScreenshotPurpose.ChoiceInitial, "Check answer")
}
