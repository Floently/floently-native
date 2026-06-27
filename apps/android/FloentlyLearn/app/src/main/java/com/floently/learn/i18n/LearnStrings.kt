package com.floently.learn.i18n

import com.floently.learn.navigation.LearnFeatureDestination

data class LearnStrings(
    val appTitle: String,
    val homeSubtitle: String,
    val welcomeBack: (String) -> String,
    val homeIntro: String,
    val recommendedNextStep: String,
    val continueRoleplay: String,
    val learnAreas: String,
    val accountTitle: String,
    val signedInAs: (String) -> String,
    val openAccount: String,
    val signOut: String,
    val backToProducts: String,
    val backToLearn: String,
    val settingsTitle: String,
    val settingsSubtitle: String,
    val languageTitle: String,
    val languageBody: String,
    val enabledLanguages: String,
    val reviewLanguages: String,
    val reviewLanguagesBody: String,
    val selectedSuffix: String,
    val releaseLanguageNote: String,
    val featureTitles: Map<LearnFeatureDestination, String>,
    val featureSubtitles: Map<LearnFeatureDestination, String>,
    val featureActions: Map<LearnFeatureDestination, String>,
    val featureMessages: Map<LearnFeatureDestination, String>
) {
    fun title(destination: LearnFeatureDestination): String = featureTitles[destination] ?: destination.title
    fun subtitle(destination: LearnFeatureDestination): String = featureSubtitles[destination] ?: destination.subtitle
    fun action(destination: LearnFeatureDestination): String = featureActions[destination] ?: destination.title
    fun message(destination: LearnFeatureDestination): String = featureMessages[destination].orEmpty()
}

fun learnStrings(language: LearnLanguage): LearnStrings = when (language) {
    LearnLanguage.FI -> finnishLearnStrings
    LearnLanguage.SV -> swedishLearnStrings
    else -> englishLearnStrings
}

private val englishLearnStrings = LearnStrings(
    appTitle = "Floently Learn",
    homeSubtitle = "Practice Finnish for real life, work, YKI, and everyday conversations.",
    welcomeBack = { name -> "Welcome back, $name." },
    homeIntro = "Choose one focused practice area and keep moving. Learn is being rebuilt to be even stronger than the previous app.",
    recommendedNextStep = "Recommended next step: conversation practice",
    continueRoleplay = "Continue with Roleplay",
    learnAreas = "Learn areas",
    accountTitle = "Account",
    signedInAs = { email -> "Signed in as $email. Manage your account, settings, language, or sign out when you are done." },
    openAccount = "Open account",
    signOut = "Sign out",
    backToProducts = "Back to Floently products",
    backToLearn = "Back to Learn",
    settingsTitle = "Settings",
    settingsSubtitle = "Choose your language and manage Learn preferences.",
    languageTitle = "Language",
    languageBody = "Finnish, Swedish, and English are enabled now. Other old app languages are preserved for review and future expansion.",
    enabledLanguages = "Enabled languages",
    reviewLanguages = "Review languages",
    reviewLanguagesBody = "These languages are preserved from the old app metadata but stay hidden until translation and RTL QA are complete.",
    selectedSuffix = "selected",
    releaseLanguageNote = "Language support restored for the native Learn 150% parity track.",
    featureTitles = mapOf(
        LearnFeatureDestination.YkiPractice to "YKI practice",
        LearnFeatureDestination.ProfessionalFinnish to "Professional Finnish",
        LearnFeatureDestination.Roleplay to "Roleplay",
        LearnFeatureDestination.Cards to "Cards",
        LearnFeatureDestination.Progress to "Progress",
        LearnFeatureDestination.Settings to "Settings",
        LearnFeatureDestination.Account to "Account"
    ),
    featureSubtitles = mapOf(
        LearnFeatureDestination.YkiPractice to "Exam-style Finnish practice for reading, writing, vocabulary, grammar, listening, and speaking.",
        LearnFeatureDestination.ProfessionalFinnish to "Workplace Finnish for interviews, meetings, messages, healthcare, and customer situations.",
        LearnFeatureDestination.Roleplay to "Conversation practice with coaching, variety, and beginner-safe guidance.",
        LearnFeatureDestination.Cards to "Vocabulary and sentence review with card banks and overlay support returning next.",
        LearnFeatureDestination.Progress to "Learning progress, streaks, and recent activity.",
        LearnFeatureDestination.Settings to "Language, preferences, support, privacy, and Learn controls.",
        LearnFeatureDestination.Account to "Subscription, device access, profile, and sign-out controls."
    ),
    featureActions = mapOf(
        LearnFeatureDestination.YkiPractice to "Practice YKI",
        LearnFeatureDestination.ProfessionalFinnish to "Practice work Finnish",
        LearnFeatureDestination.Roleplay to "Start roleplay",
        LearnFeatureDestination.Cards to "Review cards",
        LearnFeatureDestination.Progress to "View progress",
        LearnFeatureDestination.Settings to "Open settings",
        LearnFeatureDestination.Account to "Open account"
    ),
    featureMessages = mapOf(
        LearnFeatureDestination.YkiPractice to "Structured exam-style tasks with better native guidance.",
        LearnFeatureDestination.ProfessionalFinnish to "Practical work Finnish for real professional situations.",
        LearnFeatureDestination.Roleplay to "Dynamic conversation practice with anti-repetition verification.",
        LearnFeatureDestination.Cards to "Fast review now, card banks and overlays in the next augmentation.",
        LearnFeatureDestination.Progress to "Track streaks, activity, and learning progress.",
        LearnFeatureDestination.Settings to "Restore old language support and improve native preferences.",
        LearnFeatureDestination.Account to "Profile, access, device, and sign-out controls."
    )
)

private val finnishLearnStrings = englishLearnStrings.copy(
    homeSubtitle = "Harjoittele suomea arkeen, työhön, YKI-kokeeseen ja keskusteluihin.",
    welcomeBack = { name -> "Tervetuloa takaisin, $name." },
    homeIntro = "Valitse yksi harjoittelualue ja jatka eteenpäin. Learn rakennetaan natiivina paremmaksi kuin vanha sovellus.",
    recommendedNextStep = "Suositeltu seuraava askel: keskusteluharjoitus",
    continueRoleplay = "Jatka roolipeliin",
    learnAreas = "Harjoittelualueet",
    accountTitle = "Tili",
    signedInAs = { email -> "Olet kirjautunut sisään: $email. Voit hallita tiliä, asetuksia, kieltä tai kirjautua ulos." },
    openAccount = "Avaa tili",
    signOut = "Kirjaudu ulos",
    backToProducts = "Takaisin Floently-tuotteisiin",
    backToLearn = "Takaisin Learn-etusivulle",
    settingsTitle = "Asetukset",
    settingsSubtitle = "Valitse kieli ja hallitse Learn-asetuksia.",
    languageTitle = "Kieli",
    languageBody = "Suomi, ruotsi ja englanti ovat käytössä. Muut vanhan sovelluksen kielet on säilytetty tarkistusta ja myöhempää laajennusta varten.",
    enabledLanguages = "Käytössä olevat kielet",
    reviewLanguages = "Tarkistettavat kielet",
    reviewLanguagesBody = "Nämä kielet on säilytetty vanhasta sovelluksesta, mutta ne pysyvät piilossa kunnes käännökset ja RTL-testaus ovat valmiit.",
    selectedSuffix = "valittu",
    releaseLanguageNote = "Kielituki palautettu natiivin Learnin 150 % pariteettityöhön."
)

private val swedishLearnStrings = englishLearnStrings.copy(
    homeSubtitle = "Öva finska för vardag, arbete, YKI och samtal.",
    welcomeBack = { name -> "Välkommen tillbaka, $name." },
    homeIntro = "Välj ett övningsområde och fortsätt framåt. Learn byggs nativt för att bli bättre än den gamla appen.",
    recommendedNextStep = "Rekommenderat nästa steg: samtalsövning",
    continueRoleplay = "Fortsätt till rollspel",
    learnAreas = "Övningsområden",
    accountTitle = "Konto",
    signedInAs = { email -> "Inloggad som $email. Hantera konto, inställningar, språk eller logga ut." },
    openAccount = "Öppna konto",
    signOut = "Logga ut",
    backToProducts = "Tillbaka till Floently-produkter",
    backToLearn = "Tillbaka till Learn",
    settingsTitle = "Inställningar",
    settingsSubtitle = "Välj språk och hantera Learn-inställningar.",
    languageTitle = "Språk",
    languageBody = "Finska, svenska och engelska är aktiverade. Andra språk från den gamla appen finns kvar för granskning och framtida utökning.",
    enabledLanguages = "Aktiverade språk",
    reviewLanguages = "Språk för granskning",
    reviewLanguagesBody = "Dessa språk är bevarade från den gamla appen men hålls dolda tills översättning och RTL-testning är klara.",
    selectedSuffix = "valt",
    releaseLanguageNote = "Språkstödet har återställts för native Learn 150 %-paritet."
)
