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
    EN("en", "English", "English", LearnTextDirection.Ltr, true, LearnTranslationStatus.Complete),
    RU("ru", "Russian", "Русский", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    ET("et", "Estonian", "Eesti", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    UK("uk", "Ukrainian", "Українська", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    AR("ar", "Arabic", "العربية", LearnTextDirection.Rtl, false, LearnTranslationStatus.InProgress),
    SO("so", "Somali", "Soomaali", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    FA("fa", "Persian", "فارسی", LearnTextDirection.Rtl, false, LearnTranslationStatus.InProgress),
    ZH("zh", "Chinese", "中文", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    SQ("sq", "Albanian", "Shqip", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    KU("ku", "Kurdish", "Kurdî", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    VI("vi", "Vietnamese", "Tiếng Việt", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    BN("bn", "Bengali", "বাংলা", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    TR("tr", "Turkish", "Türkçe", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    TL("tl", "Tagalog", "Tagalog", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    TH("th", "Thai", "ไทย", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    NE("ne", "Nepali", "नेपाली", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    ES("es", "Spanish", "Español", LearnTextDirection.Ltr, false, LearnTranslationStatus.InProgress),
    UR("ur", "Urdu", "اردو", LearnTextDirection.Rtl, false, LearnTranslationStatus.InProgress);

    val displayLabel: String
        get() = "$nativeLabel ($label)"

    companion object {
        val enabledLanguages: List<LearnLanguage>
            get() = entries.filter { it.enabled }

        val reviewLanguages: List<LearnLanguage>
            get() = entries.filter { !it.enabled && it.translationStatus == LearnTranslationStatus.InProgress }

        fun fromCode(code: String?): LearnLanguage = entries.firstOrNull { it.code == code } ?: EN
    }
}
