package com.floently.learn.yki

internal enum class YkiMockSkill {
    Overview,
    Reading,
    Writing,
    Listening,
    Speaking,
    Results
}

internal enum class YkiMockPhase {
    Overview,
    Choice,
    Writing,
    PreparationTimer,
    RecordingTimer,
    ListeningTimer,
    Submitted,
    Results
}

internal data class YkiMockExamTask(
    val bankTaskId: String,
    val screenshots: List<String>,
    val skill: YkiMockSkill,
    val phase: YkiMockPhase,
    val section: String,
    val title: String,
    val prompt: String,
    val passage: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val timerSeconds: Int = 0,
    val activeRecording: Boolean = false,
    val bankSource: String = "native-yki-mock-exam-bank"
) {
    val screenshotLabel: String
        get() = screenshots.joinToString(", ")
}

internal object YkiMockExamBank {
    fun tasks(): List<YkiMockExamTask> = listOf(
        YkiMockExamTask(
            bankTaskId = "mock-overview-1",
            screenshots = listOf("IMG_0432"),
            skill = YkiMockSkill.Overview,
            phase = YkiMockPhase.Overview,
            section = "Overview",
            title = "Full YKI Exam simulation",
            prompt = "This full simulation follows the source screenshots: reading, writing, timed listening, timed speaking, recording, submitted states, and final results."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-overview-2",
            screenshots = listOf("IMG_0433"),
            skill = YkiMockSkill.Overview,
            phase = YkiMockPhase.Overview,
            section = "Overview",
            title = "Exam sections",
            prompt = "Reading comprehension, writing, listening, and speaking are completed in order."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-overview-start",
            screenshots = listOf("IMG_0434"),
            skill = YkiMockSkill.Overview,
            phase = YkiMockPhase.Overview,
            section = "Overview",
            title = "Ready to start",
            prompt = "Start YKI exam."
        ),

        YkiMockExamTask(
            bankTaskId = "mock-reading-1",
            screenshots = listOf("IMG_0436", "IMG_0437"),
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "Reading comprehension",
            title = "Read the notice and answer",
            prompt = "What is the main purpose of the notice?",
            passage = "Kaupungin kirjasto ilmoittaa, että aukioloajat muuttuvat kesäkuun alusta. Asiakkaat voivat edelleen palauttaa kirjoja palautusluukun kautta myös silloin, kun kirjasto on kiinni.",
            options = listOf(
                "The library is closing permanently.",
                "Opening hours are changing.",
                "Customers cannot return books anymore.",
                "The library is moving to another address."
            ),
            correctIndex = 1
        ),
        YkiMockExamTask(
            bankTaskId = "mock-reading-2",
            screenshots = listOf("IMG_0438", "IMG_0439", "IMG_0440"),
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "Reading comprehension",
            title = "Long passage",
            prompt = "Which statement matches the passage?",
            passage = "Tyopaikalla otetaan kayttoon uusi digitaalinen jarjestelma. Tyontekijoille jarjestetaan kaksi koulutusta, ja vanha jarjestelma toimii rinnalla viela kuukauden ajan.",
            options = listOf(
                "The old system stops immediately.",
                "Training is optional and only online.",
                "The old and new systems overlap for one month.",
                "Only managers will use the new system."
            ),
            correctIndex = 2
        ),
        YkiMockExamTask(
            bankTaskId = "mock-reading-3",
            screenshots = listOf("IMG_0441", "IMG_0442"),
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "Reading comprehension",
            title = "Answer from text",
            prompt = "Why should the reader contact customer service?",
            passage = "Jos laskussa on virhe, ota yhteytta asiakaspalveluun ennen erapaivaa. Nain asia voidaan korjata ajoissa eika maksusta tule muistutusmaksua.",
            options = listOf(
                "To avoid an extra reminder fee.",
                "To cancel the service.",
                "To receive a new password.",
                "To change the address."
            ),
            correctIndex = 0
        ),

        YkiMockExamTask(
            bankTaskId = "mock-writing-1",
            screenshots = listOf("IMG_0443", "IMG_0444"),
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "Writing",
            title = "Write a message",
            prompt = "Write a short message asking for a new appointment time."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-writing-2",
            screenshots = listOf("IMG_0445", "IMG_0446"),
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "Writing",
            title = "Structured answer",
            prompt = "Write a clear reply. Include the reason, your suggestion, and a polite ending."
        ),

        YkiMockExamTask(
            bankTaskId = "mock-bridge-prep",
            screenshots = listOf("IMG_0447"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.PreparationTimer,
            section = "Speaking",
            title = "Prepare your answer",
            prompt = "You have a short preparation time before the recording begins.",
            timerSeconds = 7
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-prep-1",
            screenshots = listOf("IMG_0448", "IMG_0449", "IMG_0450", "IMG_0451"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.PreparationTimer,
            section = "Speaking",
            title = "Prepare answer",
            prompt = "Prepare to explain how you solve a problem at work.",
            timerSeconds = 30
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-record-1",
            screenshots = listOf("IMG_0452", "IMG_0453", "IMG_0454", "IMG_0455"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.RecordingTimer,
            section = "Speaking",
            title = "Recording task",
            prompt = "Speak your answer. Recording starts automatically after preparation.",
            timerSeconds = 60,
            activeRecording = true
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-prep-2",
            screenshots = listOf("IMG_0456", "IMG_0457"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.PreparationTimer,
            section = "Speaking",
            title = "Prepare next answer",
            prompt = "Prepare to describe an everyday situation.",
            timerSeconds = 10
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-record-2",
            screenshots = listOf("IMG_0458", "IMG_0459", "IMG_0460", "IMG_0461", "IMG_0462", "IMG_0463", "IMG_0465", "IMG_0466"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.RecordingTimer,
            section = "Speaking",
            title = "Recording task",
            prompt = "Record your spoken answer. The red state represents active recording.",
            timerSeconds = 60,
            activeRecording = true
        ),

        YkiMockExamTask(
            bankTaskId = "mock-listening-prep",
            screenshots = listOf("IMG_0467", "IMG_0468"),
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.PreparationTimer,
            section = "Listening",
            title = "Prepare for listening",
            prompt = "Read the question before the listening task starts.",
            timerSeconds = 30
        ),
        YkiMockExamTask(
            bankTaskId = "mock-listening-1",
            screenshots = listOf("IMG_0469", "IMG_0470", "IMG_0471", "IMG_0472", "IMG_0473"),
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.ListeningTimer,
            section = "Listening",
            title = "Timed listening answer",
            prompt = "Listen and choose the best answer before the timer ends.",
            options = listOf(
                "The project is cancelled.",
                "The delivery is delayed.",
                "The meeting has moved online.",
                "The customer has already paid."
            ),
            correctIndex = 1,
            timerSeconds = 60
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-prep-3",
            screenshots = listOf("IMG_0474"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.PreparationTimer,
            section = "Speaking",
            title = "Prepare final speaking answer",
            prompt = "Prepare your final spoken answer.",
            timerSeconds = 30
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-record-3",
            screenshots = listOf("IMG_0475", "IMG_0476", "IMG_0477"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.RecordingTimer,
            section = "Speaking",
            title = "Final recording",
            prompt = "Record the final speaking answer. Submit exam appears after the recording is sent.",
            timerSeconds = 60,
            activeRecording = true
        )
    )
}
