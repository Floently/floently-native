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
    val audioScript: String = "",
    val activeRecording: Boolean = false,
    val finalSubmit: Boolean = false,
    val bankSource: String = "native-yki-mock-exam-bank"
) {
    val screenshotLabel: String
        get() = screenshots.joinToString(", ")
}

internal object YkiMockExamBank {
    fun tasks(): List<YkiMockExamTask> = listOf(
        YkiMockExamTask(
            bankTaskId = "mock-overview-0432",
            screenshots = listOf("IMG_0432"),
            skill = YkiMockSkill.Overview,
            phase = YkiMockPhase.Overview,
            section = "Overview",
            title = "Full YKI Exam simulation",
            prompt = "Complete the full mock exam in the same order as the original: reading, writing, listening and speaking."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-overview-0433",
            screenshots = listOf("IMG_0433"),
            skill = YkiMockSkill.Overview,
            phase = YkiMockPhase.Overview,
            section = "Overview",
            title = "Exam sections",
            prompt = "The exam contains separate sections. Answers are not corrected during the exam."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-overview-0434",
            screenshots = listOf("IMG_0434"),
            skill = YkiMockSkill.Overview,
            phase = YkiMockPhase.Overview,
            section = "Overview",
            title = "Ready to start",
            prompt = "Start YKI exam."
        ),

        YkiMockExamTask(
            bankTaskId = "mock-reading-0436-0437",
            screenshots = listOf("IMG_0436", "IMG_0437"),
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "Reading comprehension",
            title = "Reading task 1",
            prompt = "Choose the best answer. The result is shown only after the full exam.",
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
            bankTaskId = "mock-reading-0438-0440",
            screenshots = listOf("IMG_0438", "IMG_0439", "IMG_0440"),
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "Reading comprehension",
            title = "Reading task 2",
            prompt = "Choose the statement that matches the passage. The result is saved for final evaluation.",
            passage = "Työpaikalla otetaan käyttöön uusi digitaalinen järjestelmä. Työntekijöille järjestetään kaksi koulutusta, ja vanha järjestelmä toimii rinnalla vielä kuukauden ajan. Tavoitteena on, että kaikki osaavat käyttää uutta järjestelmää ennen kuin vanha poistuu käytöstä.",
            options = listOf(
                "The old system stops immediately.",
                "Training is only for managers.",
                "The old and new systems overlap for one month.",
                "The new system is cancelled."
            ),
            correctIndex = 2
        ),
        YkiMockExamTask(
            bankTaskId = "mock-reading-0441-0442",
            screenshots = listOf("IMG_0441", "IMG_0442"),
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "Reading comprehension",
            title = "Reading task 3",
            prompt = "Choose the best answer. No correction is shown during the exam.",
            passage = "Jos laskussa on virhe, ota yhteyttä asiakaspalveluun ennen eräpäivää. Näin asia voidaan korjata ajoissa eikä maksusta tule muistutusmaksua.",
            options = listOf(
                "To avoid an extra reminder fee.",
                "To cancel the service.",
                "To receive a new password.",
                "To change the address."
            ),
            correctIndex = 0
        ),

        YkiMockExamTask(
            bankTaskId = "mock-writing-0443-0444",
            screenshots = listOf("IMG_0443", "IMG_0444"),
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "Writing",
            title = "Writing task 1",
            prompt = "Write a message asking for a new appointment time. Save the answer before continuing."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-writing-0445-0446",
            screenshots = listOf("IMG_0445", "IMG_0446"),
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "Writing",
            title = "Writing task 2",
            prompt = "Write a clear reply. Include the reason, your suggestion, and a polite ending. Save the answer before continuing."
        ),

        YkiMockExamTask(
            bankTaskId = "mock-listening-prep-0467-0468",
            screenshots = listOf("IMG_0467", "IMG_0468"),
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.PreparationTimer,
            section = "Listening",
            title = "Listening preparation",
            prompt = "Read the instructions before the listening section starts.",
            timerSeconds = 30
        ),
        YkiMockExamTask(
            bankTaskId = "mock-listening-0469-0470",
            screenshots = listOf("IMG_0469", "IMG_0470"),
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.ListeningTimer,
            section = "Listening",
            title = "Listening task 1",
            prompt = "Listen and choose the best answer. Your answer is saved for final evaluation.",
            options = listOf(
                "The project has been cancelled.",
                "The delivery is delayed.",
                "The meeting is already finished.",
                "The customer wants a refund."
            ),
            correctIndex = 1,
            timerSeconds = 60,
            audioScript = "Projektin toimitus viivästyy kahdella päivällä, mutta asiakas saa uuden aikataulun tänään iltapäivällä."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-listening-0471-0472",
            screenshots = listOf("IMG_0471", "IMG_0472"),
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.ListeningTimer,
            section = "Listening",
            title = "Listening task 2",
            prompt = "Listen and choose the best answer. No correction is shown during the exam.",
            options = listOf(
                "The appointment is moved to Friday.",
                "The appointment is cancelled.",
                "The office is closed all week.",
                "The customer must call another number."
            ),
            correctIndex = 0,
            timerSeconds = 45,
            audioScript = "Aika siirretään perjantaille kello kymmenen. Jos aika ei sovi, asiakkaan täytyy ilmoittaa siitä viimeistään huomenna."
        ),
        YkiMockExamTask(
            bankTaskId = "mock-listening-0473",
            screenshots = listOf("IMG_0473"),
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.ListeningTimer,
            section = "Listening",
            title = "Listening task 3",
            prompt = "Listen and choose the best answer before continuing.",
            options = listOf(
                "The speaker asks for directions.",
                "The speaker reports a missing document.",
                "The speaker orders food.",
                "The speaker describes a holiday."
            ),
            correctIndex = 1,
            timerSeconds = 30,
            audioScript = "Puuttuva asiakirja pitää lähettää sähköpostilla ennen kokousta, jotta hakemus voidaan käsitellä ajoissa."
        ),

        YkiMockExamTask(
            bankTaskId = "mock-speaking-prep-0447-0451",
            screenshots = listOf("IMG_0447", "IMG_0448", "IMG_0449", "IMG_0450", "IMG_0451"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.PreparationTimer,
            section = "Speaking",
            title = "Speaking task 1 preparation",
            prompt = "Prepare to explain how you solve a problem at work. Recording starts automatically after preparation.",
            timerSeconds = 30
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-record-0452-0455",
            screenshots = listOf("IMG_0452", "IMG_0453", "IMG_0454", "IMG_0455"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.RecordingTimer,
            section = "Speaking",
            title = "Speaking task 1 recording",
            prompt = "Speak your answer. The recording is saved for final evaluation.",
            timerSeconds = 60,
            activeRecording = true
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-prep-0456-0457",
            screenshots = listOf("IMG_0456", "IMG_0457"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.PreparationTimer,
            section = "Speaking",
            title = "Speaking task 2 preparation",
            prompt = "Prepare to describe an everyday situation. Recording starts automatically after preparation.",
            timerSeconds = 10
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-record-0458-0466",
            screenshots = listOf("IMG_0458", "IMG_0459", "IMG_0460", "IMG_0461", "IMG_0462", "IMG_0463", "IMG_0465", "IMG_0466"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.RecordingTimer,
            section = "Speaking",
            title = "Speaking task 2 recording",
            prompt = "Record your spoken answer. The red state represents active recording.",
            timerSeconds = 60,
            activeRecording = true
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-prep-0474",
            screenshots = listOf("IMG_0474"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.PreparationTimer,
            section = "Speaking",
            title = "Speaking task 3 preparation",
            prompt = "Prepare your final spoken answer.",
            timerSeconds = 30
        ),
        YkiMockExamTask(
            bankTaskId = "mock-speaking-record-0475-0477",
            screenshots = listOf("IMG_0475", "IMG_0476", "IMG_0477"),
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.RecordingTimer,
            section = "Speaking",
            title = "Speaking task 3 recording",
            prompt = "Record the final speaking answer. Submit exam appears after sending.",
            timerSeconds = 60,
            activeRecording = true,
            finalSubmit = true
        )
    )

    val requiredScreenshotIds = listOf(
        "IMG_0432", "IMG_0433", "IMG_0434",
        "IMG_0436", "IMG_0437", "IMG_0438", "IMG_0439", "IMG_0440", "IMG_0441", "IMG_0442",
        "IMG_0443", "IMG_0444", "IMG_0445", "IMG_0446", "IMG_0447", "IMG_0448", "IMG_0449", "IMG_0450", "IMG_0451",
        "IMG_0452", "IMG_0453", "IMG_0454", "IMG_0455", "IMG_0456", "IMG_0457", "IMG_0458", "IMG_0459", "IMG_0460", "IMG_0461", "IMG_0462", "IMG_0463",
        "IMG_0465", "IMG_0466", "IMG_0467", "IMG_0468", "IMG_0469", "IMG_0470", "IMG_0471", "IMG_0472", "IMG_0473", "IMG_0474", "IMG_0475", "IMG_0476", "IMG_0477", "IMG_0478", "IMG_0479"
    )
}
