package com.floently.learn.i18n

enum class LearnTextDirection {
    Ltr,
    Rtl
}

enum class LearnTranslationStatus {
    Complete,
    Fallback,
    InProgress
}

enum class LearnLanguage(
    val code: String,
    val label: String,
    val nativeLabel: String,
    val direction: LearnTextDirection,
    val enabled: Boolean,
    val translationStatus: LearnTranslationStatus
) {
    FI("fi", "Finnish", "Suomi", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    SV("sv", "Swedish", "Svenska", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    RU("ru", "Russian", "Русский", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    ET("et", "Estonian", "Eesti", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    UK("uk", "Ukrainian", "Українська", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    AR("ar", "Arabic", "العربية", LearnTextDirection.Rtl, true, LearnTranslationStatus.Complete),
    EN("en", "English", "English", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    SO("so", "Somali", "Soomaali", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    FA("fa", "Persian", "فارسی", LearnTextDirection.Rtl, true, LearnTranslationStatus.Complete),
    ZH("zh", "Chinese", "中文", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    SQ("sq", "Albanian", "Shqip", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    KU("ku", "Kurdish", "Kurdî", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    VI("vi", "Vietnamese", "Tiếng Việt", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    BN("bn", "Bengali", "বাংলা", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    TR("tr", "Turkish", "Türkçe", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    TL("tl", "Tagalog", "Tagalog", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    TH("th", "Thai", "ไทย", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    NE("ne", "Nepali", "नेपाली", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    ES("es", "Spanish", "Español", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    UR("ur", "Urdu", "اردو", LearnTextDirection.Rtl, true, LearnTranslationStatus.Complete);

    val displayLabel: String
        get() = "$nativeLabel ($label)"

    companion object {
        val enabledLanguages: List<LearnLanguage>
            get() = entries.filter { it.enabled }

        val reviewLanguages: List<LearnLanguage>
            get() = entries.filter { !it.enabled || it.translationStatus != LearnTranslationStatus.Complete }

        fun fromCode(code: String?): LearnLanguage = entries.firstOrNull { it.code == code } ?: EN
    }
}
