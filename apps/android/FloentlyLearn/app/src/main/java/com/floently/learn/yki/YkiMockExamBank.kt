package com.floently.learn.yki

internal enum class YkiMockSkill {
    Reading,
    Listening,
    Writing,
    Speaking
}

internal enum class YkiMockPhase {
    Choice,
    Listening,
    Writing,
    Speaking
}

internal enum class YkiMockLevelBand(
    val label: String,
    val examTitle: String,
    val description: String
) {
    A1_A2(
        label = "A1-A2",
        examTitle = "YKI A1-A2 exam",
        description = "Basic-level YKI mock route using the certified native mock bank structure."
    ),
    B1_B2(
        label = "B1-B2",
        examTitle = "YKI B1-B2 exam",
        description = "Intermediate-level YKI mock route and the screenshot-locked default path."
    ),
    C1_C2(
        label = "C1-C2",
        examTitle = "YKI C1-C2 exam",
        description = "Advanced-level YKI mock route using the certified native mock bank structure."
    )
}

internal data class YkiMockExamTask(
    val bankTaskId: String,
    val screenshots: List<String>,
    val globalTaskNumber: Int,
    val skill: YkiMockSkill,
    val phase: YkiMockPhase,
    val section: String,
    val sectionTitle: String,
    val sectionTaskNumber: Int,
    val sectionTaskCount: Int,
    val durationLabel: String,
    val instruction: String,
    val prompt: String,
    val passage: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val audioScript: String = "",
    val promptReadSeconds: Int = 10,
    val preparationSeconds: Int = 30,
    val responseSeconds: Int = 60,
    val minimumRecordingSeconds: Int = 30,
    val levelBand: YkiMockLevelBand = YkiMockLevelBand.B1_B2,
    val finalSubmit: Boolean = false,
    val bankSource: String = "engine_v3_2_certified_native_mock_bank"
) {
    val screenshotLabel: String
        get() = screenshots.joinToString(", ")
}

internal object YkiMockExamBank {
    const val certifiedTaskCount: Int = 3882
    const val authority: String = "engine_v3_2_certified"
    const val totalExamTasks: Int = 17
    const val totalDuration: String = "approx. 95 min"

    fun tasks(levelBand: YkiMockLevelBand = YkiMockLevelBand.B1_B2): List<YkiMockExamTask> =
        baseTasks().map { task ->
            task.copy(
                levelBand = levelBand,
                bankSource = "${authority}_${levelBand.label.replace("-", "_")}_native_mock_bank"
            )
        }

    private fun baseTasks(): List<YkiMockExamTask> = listOf(
        YkiMockExamTask(
            bankTaskId = "reading-1-digitalisation",
            screenshots = listOf("IMG_0436", "IMG_0437"),
            globalTaskNumber = 1,
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "READING COMPREHENSION",
            sectionTitle = "Reading comprehension",
            sectionTaskNumber = 1,
            sectionTaskCount = 5,
            durationLabel = "25 min",
            instruction = "Read each text carefully and choose the best answer.",
            prompt = "Mikä on artikkelin pääviesti digitalisaatiosta julkisissa palveluissa?",
            passage = "Digitalisaatio muuttaa tapaa, jolla suomalaiset asioivat viranomaisten kanssa. Lähes kaikki julkiset palvelut ovat nykyään saatavilla sähköisesti, ja yhä useampi kansalainen hoitaa veroilmoituksen, Kela-hakemukset ja rekisteröinnit itsenäisesti verkossa.\n\nKehitys on tuonut mukanaan tehokkuutta ja joustavuutta. Palveluja voi käyttää vuorokauden ympäri, ja jonotusajat ovat lyhentyneet. Kuitenkin kaikille digitaaliset palvelut eivät ole yhtä helppokäyttöisiä. Ikääntyneet, maahanmuuttajat ja henkilöt, joilla on heikko digilukutaito, tarvitsevat usein henkilökohtaista tukea asioinnissa.\n\nJulkishallinto onkin pyrkinyt pitämään henkilökohtaisen asioinnin vaihtoehdon saatavilla niille, jotka eivät pysty tai halua käyttää sähköisiä kanavia.",
            options = listOf(
                "Digitalisointi on hyödyllistä, mutta kaikki eivät pysty hyödyntämään sitä yhtäläisesti",
                "Digilukutaito on parantunut kaikissa väestöryhmissä",
                "Digitalisointi on täysin epäonnistunut julkisissa palveluissa",
                "Julkishallinto on luopunut kokonaan perinteisestä asioinnista"
            ),
            correctIndex = 0
        ),
        YkiMockExamTask(
            bankTaskId = "reading-2-immigration",
            screenshots = listOf("IMG_0438", "IMG_0439", "IMG_0440"),
            globalTaskNumber = 2,
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "READING COMPREHENSION",
            sectionTitle = "Reading comprehension",
            sectionTaskNumber = 2,
            sectionTaskCount = 5,
            durationLabel = "25 min",
            instruction = "Read each text carefully and choose the best answer.",
            prompt = "Miksi Suomi tarvitsee maahanmuuttoa asiantuntijoiden mukaan?",
            passage = "Suomi tarvitsee lähivuosikymmeninä merkittävää maahanmuuttoa väestön ikääntymisestä johtuvan työvoimapulan paikkaamiseksi. Erityisesti sosiaali- ja terveysala sekä rakennussektori kärsivät jo nyt osaajapulasta.\n\nMaahanmuuttajien kotoutuminen on avainasemassa. Onnistunut kotoutuminen edellyttää kielitaitoa, työllistymistä ja sosiaalisia verkostoja. Suomen kielen oppiminen on usein suurin este nopealle työllistymiselle.\n\nViranomaiset ja järjestöt tarjoavat kotouttamispalveluja, mutta resurssit eivät aina riitä yksilölliseen tukeen. Erityisesti heikosti koulutettujen maahanmuuttajien kohdalla prosessi voi venyä vuosiksi. Asiantuntijat korostavat, että panostaminen varhaiseen kielenopetukseen ja työllistymisen tukeen maksaa itsensä takaisin yhteiskunnalle.",
            options = listOf(
                "Ikääntymisestä johtuva työvoimapula on keskeinen syy",
                "Suomessa ei enää ole riittävästi nuoria kouluttautumaan",
                "Suomen syntyvyys on laskenut alle nollatason",
                "Maahanmuuttajat tuovat uutta teknologiaa Suomeen"
            ),
            correctIndex = 0
        ),
        YkiMockExamTask(
            bankTaskId = "reading-3-remote-work",
            screenshots = listOf("IMG_0441", "IMG_0442"),
            globalTaskNumber = 3,
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "READING COMPREHENSION",
            sectionTitle = "Reading comprehension",
            sectionTaskNumber = 3,
            sectionTaskCount = 5,
            durationLabel = "25 min",
            instruction = "Read each text carefully and choose the best answer.",
            prompt = "Miksi etätyö voi olla haastavampaa joillekin työntekijöille kuin toisille?",
            passage = "Etätyö on muuttanut suomalaista työelämää merkittävästi viime vuosina. Monet työnantajat sallivat nyt henkilöstön tehdä osan työviikostaan kotoa käsin. Joustavuus on lisääntynyt, mutta samalla rajat työn ja vapaa-ajan välillä ovat hämärtyneet.\n\nTutkimusten mukaan etätyö lisää tuottavuutta niillä työntekijöillä, jotka voivat järjestää rauhallisen työtilan kotiin. Sen sijaan perheen kanssa ahtaissa oloissa asuvat kokevat usein etätyön raskaammaksi kuin toimistossa työskentelyn.\n\nOsa asiantuntijoista on huolissaan siitä, että pitkittynyt etätyö heikentää tiimien yhteenkuuluvuutta ja vaikeuttaa uusien työntekijöiden perehdyttämistä. Ratkaisuna monet yritykset ovat ottaneet käyttöön hybridimallin, jossa toimistolla käydään muutamana päivänä viikossa.",
            options = listOf(
                "Koska etätyö on aina vähemmän tuottavaa kuin toimistotyö",
                "Koska kaikilla ei ole kotona rauhallista työtilaa",
                "Koska työnantajat eivät enää salli toimistotyötä",
                "Koska hybridimalli on poistettu käytöstä"
            ),
            correctIndex = 1
        ),
        YkiMockExamTask(
            bankTaskId = "reading-4-healthcare",
            screenshots = listOf("IMG_0438", "IMG_0440"),
            globalTaskNumber = 4,
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "READING COMPREHENSION",
            sectionTitle = "Reading comprehension",
            sectionTaskNumber = 4,
            sectionTaskCount = 5,
            durationLabel = "25 min",
            instruction = "Read each text carefully and choose the best answer.",
            prompt = "Mitä artikkeli sanoo yksityisten terveyspalvelujen lisääntymisestä?",
            passage = "Suomen terveydenhuoltojärjestelmä perustuu universaaliin hoitovelvollisuuteen: jokainen Suomessa asuva on oikeutettu terveydenhoitopalveluihin asuinpaikastaan riippumatta. Käytännössä palvelut tuotetaan kuntien, hyvinvointialueiden ja yksityisen sektorin yhteistyönä.\n\nViime vuosina yksityisten terveyspalvelujen käyttö on kasvanut huomattavasti. Syynä on usein julkisen sektorin pitkät jonotusajat erikoissairaanhoitoon. Maksukykyisillä on mahdollisuus ohittaa jonot yksityisellä vastaanotolla, mikä herättää kysymyksiä tasa-arvosta.\n\nSosiaali- ja terveysministeriö on pyrkinyt purkamaan hoitojonoja lisäämällä lähipalveluja ja tehostamalla digitaalisia palveluja. Digilääkäripalveluiden käyttö on erityisesti nuorten ja työssäkäyvien parissa yleistynyt nopeasti.",
            options = listOf(
                "Se on korvannut kokonaan julkisen terveydenhuollon",
                "Se liittyy muun muassa julkisen sektorin pitkiin jonoihin",
                "Se koskee vain ulkomaalaisia potilaita",
                "Se on vähentänyt digipalvelujen käyttöä"
            ),
            correctIndex = 1
        ),
        YkiMockExamTask(
            bankTaskId = "reading-5-education",
            screenshots = listOf("IMG_0441", "IMG_0442"),
            globalTaskNumber = 5,
            skill = YkiMockSkill.Reading,
            phase = YkiMockPhase.Choice,
            section = "READING COMPREHENSION",
            sectionTitle = "Reading comprehension",
            sectionTaskNumber = 5,
            sectionTaskCount = 5,
            durationLabel = "25 min",
            instruction = "Read each text carefully and choose the best answer.",
            prompt = "Miksi jatkuva oppiminen on artikkelin mukaan tärkeää?",
            passage = "Työelämä muuttuu nopeasti, ja monet ammatit vaativat nykyään jatkuvaa osaamisen päivittämistä. Digitalisaatio, tekoäly ja kansainvälistyminen vaikuttavat lähes kaikkiin aloihin.\n\nAikuiskoulutus tarjoaa mahdollisuuden vaihtaa alaa tai vahvistaa nykyistä ammattitaitoa. Moni opiskelee työn ohessa, mikä vaatii hyvää ajanhallintaa ja työnantajan tukea.\n\nAsiantuntijoiden mukaan jatkuva oppiminen parantaa yksilön mahdollisuuksia pysyä työmarkkinoilla, mutta se hyödyttää myös koko yhteiskuntaa, koska osaava työvoima tukee talouskasvua ja innovaatioita.",
            options = listOf(
                "Se auttaa työntekijöitä pysymään mukana työelämän muutoksissa",
                "Se poistaa tarpeen työssä oppimiselta",
                "Se koskee vain nuoria opiskelijoita",
                "Se vähentää työnantajien vastuuta koulutuksesta"
            ),
            correctIndex = 0
        ),

        YkiMockExamTask(
            bankTaskId = "listening-1-delivery",
            screenshots = listOf("IMG_0467", "IMG_0468", "IMG_0469", "IMG_0470"),
            globalTaskNumber = 6,
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.Listening,
            section = "LISTENING COMPREHENSION",
            sectionTitle = "Listening comprehension",
            sectionTaskNumber = 1,
            sectionTaskCount = 4,
            durationLabel = "20 min",
            instruction = "You will hear each audio once. Choose the best answer.",
            prompt = "Mitä kuuntelussa kerrotaan toimituksesta?",
            options = listOf(
                "Toimitus on peruttu kokonaan",
                "Toimitus viivästyy kahdella päivällä",
                "Asiakas saa hyvityksen automaattisesti",
                "Kokous on siirretty verkkoon"
            ),
            correctIndex = 1,
            audioScript = "Projektin toimitus viivästyy kahdella päivällä, mutta asiakas saa uuden aikataulun tänään iltapäivällä."
        ),
        YkiMockExamTask(
            bankTaskId = "listening-2-appointment",
            screenshots = listOf("IMG_0471", "IMG_0472"),
            globalTaskNumber = 7,
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.Listening,
            section = "LISTENING COMPREHENSION",
            sectionTitle = "Listening comprehension",
            sectionTaskNumber = 2,
            sectionTaskCount = 4,
            durationLabel = "20 min",
            instruction = "You will hear each audio once. Choose the best answer.",
            prompt = "Mitä ajanvaraukselle tapahtuu?",
            options = listOf(
                "Aika siirretään perjantaille",
                "Aika perutaan kokonaan",
                "Toimisto on suljettu koko viikon",
                "Asiakkaan pitää soittaa toiseen numeroon"
            ),
            correctIndex = 0,
            audioScript = "Aika siirretään perjantaille kello kymmenen. Jos aika ei sovi, asiakkaan täytyy ilmoittaa siitä viimeistään huomenna."
        ),
        YkiMockExamTask(
            bankTaskId = "listening-3-document",
            screenshots = listOf("IMG_0473"),
            globalTaskNumber = 8,
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.Listening,
            section = "LISTENING COMPREHENSION",
            sectionTitle = "Listening comprehension",
            sectionTaskNumber = 3,
            sectionTaskCount = 4,
            durationLabel = "20 min",
            instruction = "You will hear each audio once. Choose the best answer.",
            prompt = "Mitä puhujan pitää tehdä?",
            options = listOf(
                "Kysyä ajo-ohjeita",
                "Lähettää puuttuva asiakirja",
                "Tilata ruokaa",
                "Kuvata lomamatkaa"
            ),
            correctIndex = 1,
            audioScript = "Puuttuva asiakirja pitää lähettää sähköpostilla ennen kokousta, jotta hakemus voidaan käsitellä ajoissa."
        ),
        YkiMockExamTask(
            bankTaskId = "listening-4-workplace",
            screenshots = listOf("IMG_0469", "IMG_0470", "IMG_0471", "IMG_0472"),
            globalTaskNumber = 9,
            skill = YkiMockSkill.Listening,
            phase = YkiMockPhase.Listening,
            section = "LISTENING COMPREHENSION",
            sectionTitle = "Listening comprehension",
            sectionTaskNumber = 4,
            sectionTaskCount = 4,
            durationLabel = "20 min",
            instruction = "You will hear each audio once. Choose the best answer.",
            prompt = "Miksi kokous järjestetään uudelleen?",
            options = listOf(
                "Osallistujia ei ollut tarpeeksi",
                "Tärkeä raportti ei ollut vielä valmis",
                "Tila oli varattu toiselle ryhmälle",
                "Puheenjohtaja oli lomalla"
            ),
            correctIndex = 1,
            audioScript = "Kokous siirretään ensi viikolle, koska tärkeä raportti ei valmistunut ajoissa. Uusi kutsu lähetetään kaikille osallistujille tänään."
        ),

        YkiMockExamTask(
            bankTaskId = "writing-1-appointment",
            screenshots = listOf("IMG_0443", "IMG_0444"),
            globalTaskNumber = 10,
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "WRITING TASKS",
            sectionTitle = "Writing tasks",
            sectionTaskNumber = 1,
            sectionTaskCount = 4,
            durationLabel = "35 min",
            instruction = "Write your answer in Finnish. Save the answer before continuing.",
            prompt = "Kirjoita lyhyt viesti, jossa pyydät uutta aikaa varatulle tapaamiselle."
        ),
        YkiMockExamTask(
            bankTaskId = "writing-2-feedback",
            screenshots = listOf("IMG_0445", "IMG_0446"),
            globalTaskNumber = 11,
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "WRITING TASKS",
            sectionTitle = "Writing tasks",
            sectionTaskNumber = 2,
            sectionTaskCount = 4,
            durationLabel = "35 min",
            instruction = "Write your answer in Finnish. Save the answer before continuing.",
            prompt = "Kirjoita palaute kurssista. Kerro, mikä onnistui, mikä oli vaikeaa ja mitä ehdotat parannukseksi."
        ),
        YkiMockExamTask(
            bankTaskId = "writing-3-opinion",
            screenshots = listOf("IMG_0443", "IMG_0444"),
            globalTaskNumber = 12,
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "WRITING TASKS",
            sectionTitle = "Writing tasks",
            sectionTaskNumber = 3,
            sectionTaskCount = 4,
            durationLabel = "35 min",
            instruction = "Write your answer in Finnish. Save the answer before continuing.",
            prompt = "Kirjoita mielipideteksti aiheesta: pitäisikö etätyötä lisätä Suomessa?"
        ),
        YkiMockExamTask(
            bankTaskId = "writing-4-application",
            screenshots = listOf("IMG_0445", "IMG_0446"),
            globalTaskNumber = 13,
            skill = YkiMockSkill.Writing,
            phase = YkiMockPhase.Writing,
            section = "WRITING TASKS",
            sectionTitle = "Writing tasks",
            sectionTaskNumber = 4,
            sectionTaskCount = 4,
            durationLabel = "35 min",
            instruction = "Write your answer in Finnish. Save the answer before continuing.",
            prompt = "Kirjoita hakemusviesti työharjoittelupaikkaa varten. Kerro osaamisestasi ja miksi olet kiinnostunut tehtävästä."
        ),

        YkiMockExamTask(
            bankTaskId = "speaking-1-work",
            screenshots = listOf("IMG_0447", "IMG_0448", "IMG_0449", "IMG_0450", "IMG_0451", "IMG_0452", "IMG_0453", "IMG_0454", "IMG_0455"),
            globalTaskNumber = 14,
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.Speaking,
            section = "SPEAKING TASKS",
            sectionTitle = "Speaking tasks",
            sectionTaskNumber = 1,
            sectionTaskCount = 4,
            durationLabel = "15 min",
            instruction = "Read each prompt, prepare for 30 seconds, then speak for 45-60 seconds.",
            prompt = "Kerro tilanteesta, jossa ratkaisit ongelman työpaikalla tai opinnoissa. Kuvaile ongelma, mitä teit ja mikä oli lopputulos.",
            finalSubmit = false
        ),
        YkiMockExamTask(
            bankTaskId = "speaking-2-everyday",
            screenshots = listOf("IMG_0456", "IMG_0457", "IMG_0458", "IMG_0459", "IMG_0460", "IMG_0461", "IMG_0462", "IMG_0463", "IMG_0465", "IMG_0466"),
            globalTaskNumber = 15,
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.Speaking,
            section = "SPEAKING TASKS",
            sectionTitle = "Speaking tasks",
            sectionTaskNumber = 2,
            sectionTaskCount = 4,
            durationLabel = "15 min",
            instruction = "Read each prompt, prepare for 30 seconds, then speak for 45-60 seconds.",
            prompt = "Kerro arjen tilanteesta, jossa sinun piti pyytää apua tai selittää asia viranomaiselle. Mitä sanoit ja miten tilanne ratkesi?",
            finalSubmit = false
        ),
        YkiMockExamTask(
            bankTaskId = "speaking-3-healthcare",
            screenshots = listOf("IMG_0474", "IMG_0475", "IMG_0476"),
            globalTaskNumber = 16,
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.Speaking,
            section = "SPEAKING TASKS",
            sectionTitle = "Speaking tasks",
            sectionTaskNumber = 3,
            sectionTaskCount = 4,
            durationLabel = "15 min",
            instruction = "Read each prompt, prepare for 30 seconds, then speak for 45-60 seconds.",
            prompt = "Harjoittele terveydenhuollon tilanteita AI-kumppanin kanssa. Olet potilas, joka käy lääkärissä. Kerro oireesi selkeästi ja kysy tietoa hoitovaihtoehdoista.",
            finalSubmit = false
        ),
        YkiMockExamTask(
            bankTaskId = "speaking-4-service",
            screenshots = listOf("IMG_0477", "IMG_0478", "IMG_0479"),
            globalTaskNumber = 17,
            skill = YkiMockSkill.Speaking,
            phase = YkiMockPhase.Speaking,
            section = "SPEAKING TASKS",
            sectionTitle = "Speaking tasks",
            sectionTaskNumber = 4,
            sectionTaskCount = 4,
            durationLabel = "15 min",
            instruction = "Read each prompt, prepare for 30 seconds, then speak for 45-60 seconds.",
            prompt = "Soitat palvelutoimistoon varataksesi ajan. Sinun täytyy selittää syy käyntiisi, ehdottaa sopivaa aikaa ja vahvistaa varaus. Harjoittele tätä puhelinkeskustelua AI-kumppanin kanssa.",
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
