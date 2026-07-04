package com.floently.learn.yki

internal enum class YkiPracticeSkill {
    Reading,
    Listening,
    Writing,
    Speaking
}

internal data class YkiPracticeBankTask(
    val screenshot: String,
    val skill: YkiPracticeSkill,
    val number: String,
    val title: String,
    val cefr: String,
    val passage: String,
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val defaultWrongIndex: Int = -1,
    val answerText: String = "",
    val wordCounter: String = "",
    val saved: Boolean = false,
    val audioScript: String = "",
    val bankSource: String = "native-yki-practice-bank"
)
internal object YkiPracticeBank {
    fun practiceTasks(): List<YkiPracticeBankTask> = listOf(
    YkiPracticeBankTask(
        screenshot = "IMG_0410-IMG_0411",
        skill = YkiPracticeSkill.Reading,
        number = "1/17",
        title = "Lue artikkeli maahanmuutosta ja vastaa.",
        cefr = "B2 — Can understand articles on social topics.",
        passage = "Suomi tarvitsee lähivuosikymmeninä merkittävää maahanmuuttoa väestön ikääntymisestä johtuvan työvoimapulan paikkaamiseksi. Erityisesti sosiaali- ja terveysala sekä rakennussektori kärsivät jo nyt osaajapulasta.\n\nMaahanmuuttajien kotoutuminen on avainasemassa. Onnistunut kotoutuminen edellyttää kielitaitoa, työllistymistä ja sosiaalisia verkostoja. Suomen kielen oppiminen on usein suurin este nopealle työllistymiselle.\n\nViranomaiset ja järjestöt tarjoavat kotouttamispalveluja, mutta resurssit eivät aina riitä yksilölliseen tukeen. Erityisesti heikosti koulutettujen maahanmuuttajien kohdalla prosessi voi venyä vuosiksi. Asiantuntijat korostavat, että panostaminen varhaiseen kielenopetukseen ja työllistymisen tukeen maksaa itsensä takaisin yhteiskunnalle.",
        question = "Miksi Suomi tarvitsee maahanmuuttoa asiantuntijoiden mukaan?",
        options = listOf(
            "Maahanmuuttajat tuovat uutta teknologiaa Suomeen",
            "Ikääntymisestä johtuva työvoimapula on keskeinen syy",
            "Suomen syntyvyys on laskenut alle nollatason",
            "Suomessa ei enää ole riittävästi nuoria kouluttautumaan"
        ),
        correctIndex = 1
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0412",
        skill = YkiPracticeSkill.Reading,
        number = "2/17",
        title = "Lue artikkeli etätyöstä ja vastaa.",
        cefr = "B2 — Can understand articles and reports on contemporary work.",
        passage = "Etätyö on yleistynyt monilla aloilla nopeasti. Osa työntekijöistä kokee sen helpottavan arkea, koska työmatkoihin ei kulu aikaa ja työpäivää on helpompi rytmittää kotona. Sen sijaan perheen kanssa ahtaissa oloissa asuvat kokevat usein etätyön raskaammaksi kuin toimistossa työskentelyn.\n\nOsa asiantuntijoista on huolissaan siitä, että pitkittynyt etätyö heikentää tiimien yhteenkuuluvuutta ja vaikeuttaa uusien työntekijöiden perehdyttämistä. Ratkaisuna monet yritykset ovat ottaneet käyttöön hybridimallin, jossa toimistolla käydään muutamana päivänä viikossa.",
        question = "Miksi etätyö voi olla haastavampaa joillekin työntekijöille kuin toisille?",
        options = listOf(
            "Koska etätyö on aina vähemmän tuottavaa kuin toimistotyö",
            "Koska työnantajat eivät luota etätyöntekijöihin",
            "Koska ahtaat asuinolosuhteet voivat tehdä etätyöstä raskaampaa",
            "Koska kotona ei ole riittävästi teknologiaa"
        ),
        correctIndex = 2
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0413-IMG_0414",
        skill = YkiPracticeSkill.Reading,
        number = "3/17",
        title = "Lue lehtiartikkeli ja vastaa.",
        cefr = "B2 — Can understand articles and reports on contemporary problems.",
        passage = "Suomen terveydenhuoltojärjestelmä perustuu universaaliin hoitovelvollisuuteen: jokainen Suomessa asuva on oikeutettu terveydenhoitopalveluihin asuinpaikastaan riippumatta. Käytännössä palvelut tuotetaan kuntien, hyvinvointialueiden ja yksityisen sektorin yhteistyönä.\n\nViime vuosina yksityisten terveyspalvelujen käyttö on kasvanut huomattavasti. Syynä on usein julkisen sektorin pitkät jonotusajat erikoissairaanhoitoon. Maksukykyisillä on mahdollisuus ohittaa jonot yksityisellä vastaanotolla, mikä herättää kysymyksiä tasa-arvosta.\n\nSosiaali- ja terveysministeriö on pyrkinyt purkamaan hoitojonoja lisäämällä lähipalveluja ja tehostamalla digitaalisia palveluja. Digilääkäripalveluiden käyttö on erityisesti nuorten ja työssäkäyvien parissa yleistynyt nopeasti.",
        question = "Mitä artikkeli sanoo yksityisten terveyspalvelujen lisääntymisestä?",
        options = listOf(
            "Se on parantanut kaikkien suomalaisten pääsyä hoitoon tasapuolisesti",
            "Se on vähentänyt digitaalisten palvelujen tarvetta",
            "Se herättää kysymyksiä yhdenvertaisuudesta, koska hoitoon pääsy riippuu maksukyvystä",
            "Se on korvannut kokonaan julkisen terveydenhuollon"
        ),
        correctIndex = 2,
        defaultWrongIndex = 0
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0415",
        skill = YkiPracticeSkill.Reading,
        number = "4/17",
        title = "Lue artikkeli julkisista palveluista ja vastaa.",
        cefr = "B2 — Can understand argumentation in public-service texts.",
        passage = "Kehitys on tuonut mukanaan tehokkuutta ja joustavuutta. Palveluja voi käyttää vuorokauden ympäri, ja jonotusajat ovat lyhentyneet. Kuitenkin kaikille digitaaliset palvelut eivät ole yhtä helppokäyttöisiä. Ikääntyneet, maahanmuuttajat ja henkilöt, joilla on heikko digilukutaito, tarvitsevat usein henkilökohtaista tukea asioinnissa.\n\nJulkishallinto onkin pyrkinyt pitämään henkilökohtaisen asioinnin vaihtoehdon saatavilla niille, jotka eivät pysty tai halua käyttää sähköisiä kanavia.",
        question = "Mikä on artikkelin pääviesti digitalisaatiosta julkisissa palveluissa?",
        options = listOf(
            "Digilukutaito on parantunut kaikissa väestöryhmissä",
            "Digitalisointi on hyödyllistä, mutta kaikki eivät pysty hyödyntämään sitä yhtäläisesti",
            "Julkishallinto on luopunut kokonaan perinteisestä asioinnista",
            "Digitalisointi on täysin epäonnistunut julkisissa palveluissa"
        ),
        correctIndex = 1
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0416",
        skill = YkiPracticeSkill.Reading,
        number = "5/17",
        title = "Lue artikkeli ympäristöasioista ja vastaa.",
        cefr = "B2 — Can understand opinion and criticism in articles.",
        passage = "Silti kuluttajavalinnoilla on merkitystä. Kasvipohjaisen ruokavalion yleistyminen, joukkoliikenteen suosiminen ja energiatehokkaan asumisen lisääntyminen ovat konkreettisia tapoja pienentää omaa hiilijalanjälkeä.\n\nKriitikot kuitenkin muistuttavat, että vastuun siirtäminen yksilöille vie huomion pois rakenteellisista ratkaisuista, kuten energiapolitiikasta ja teollisuuden sääntelystä. Tehokkain muutos syntyy yhdistämällä poliittiset päätökset ja yksilöllinen toiminta.",
        question = "Mitä kriitikot sanovat yksilöiden vastuusta ympäristöasioissa?",
        options = listOf(
            "Vastuun painottaminen yksilöille voi haitata rakenteellisten ratkaisujen hakemista",
            "Yksilöt ovat päävastuussa ilmastonmuutoksen torjumisesta",
            "Yksilöiden valinnoilla ei ole mitään merkitystä",
            "Kierrätys on riittävä toimenpide ilmastonmuutoksen hidastamiseen"
        ),
        correctIndex = 0,
        audioScript = "Kuuntele ote luennosta. Eksekutiivinen funktio tarkoittaa kykyä vaihtaa tehtävien välillä ja hallita huomiota."
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0417-IMG_0418",
        skill = YkiPracticeSkill.Listening,
        number = "6/17",
        title = "Kuuntele radiouutinen ja vastaa.",
        cefr = "B2 — Can understand radio news and identify the main information.",
        passage = "",
        question = "Mitä liikuntakeskuksen vastustajat ehdottavat?",
        options = listOf(
            "Nykyisten tilojen kunnostamista uuden rakentamisen sijaan",
            "Hankkeen kokonaan peruuttamista",
            "Halvempaa rakennustapaa",
            "Yksityistä rahoitusta hankkeelle"
        ),
        correctIndex = 0,
        defaultWrongIndex = 1,
        audioScript = "Kuuntele radiouutinen. Liikuntakeskuksen vastustajat ehdottavat nykyisten tilojen kunnostamista uuden rakentamisen sijaan."
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0419",
        skill = YkiPracticeSkill.Listening,
        number = "7/17",
        title = "Kuuntele ote luennosta ja vastaa.",
        cefr = "B2 — Can follow extended speech in a lecture on familiar topics.",
        passage = "",
        question = "Mitä tarkoitetaan eksekutiivisella funktiolla tässä yhteydessä?",
        options = listOf(
            "Kykyä vaihtaa tehtävien välillä ja hallita huomiota",
            "Laajaa sanavarastoa molemmissa kielissä",
            "Pitkäkestoista muistia",
            "Kykyä puhua kahta kieltä samanaikaisesti"
        ),
        correctIndex = 0
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0420",
        skill = YkiPracticeSkill.Listening,
        number = "8/17",
        title = "Kuuntele haastattelu ja vastaa.",
        cefr = "B2 — Can understand interviews on professional topics.",
        passage = "",
        question = "Mitä asiantuntija pitää parhaana keinona löytää töitä nuorille?",
        options = listOf(
            "Kansainvälinen kokemus ulkomailla",
            "Mahdollisimman monen hakemuksen lähettäminen",
            "Työnhakukurssien suorittaminen",
            "Verkostoituminen opiskeluaikana ja alan tapahtumissa"
        ),
        correctIndex = 3,
        audioScript = "Kuuntele haastattelu. Asiantuntija sanoo, että verkostoituminen opiskeluaikana ja alan tapahtumissa auttaa nuoria löytämään töitä."
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0421",
        skill = YkiPracticeSkill.Listening,
        number = "9/17",
        title = "Kuuntele työtovereiden keskustelu ja vastaa.",
        cefr = "B1 — Can follow the main points of extended discussion on familiar topics.",
        passage = "",
        question = "Miksi projekti on myöhässä?",
        options = listOf(
            "Projektitiimillä on liian vähän resursseja",
            "Alihankkija ei ole toimittanut osia ajoissa",
            "Johto muutti projektin tavoitteita",
            "Asiakas on muuttanut vaatimuksiaan"
        ),
        correctIndex = 1,
        defaultWrongIndex = 0,
        audioScript = "Kuuntele työtovereiden keskustelu. Projekti on myöhässä, koska alihankkija ei ole toimittanut osia ajoissa."
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0422",
        skill = YkiPracticeSkill.Writing,
        number = "10/17",
        title = "Kirjoita mielipidekirjoitus.",
        cefr = "B1 — Can write accounts of experiences, expressing opinions with reasons.",
        passage = "Jotkut ihmiset ajattelevat, että älypuhelimet ovat tehneet sosiaalisesta elämästä köyhempää, koska ihmiset katsovat puhelimiaan seurueessa ollessaan. Toiset taas ajattelevat, että älypuhelimet ovat parantaneet yhteydenpitoa.\n\nKirjoita 80-120 sanaa. Esitä oma mielipiteesi ja perustele se kahdella argumentilla. Käytä asiatyyliä.\n\nAloita esittämällä mielipiteesi selkeästi. Kirjoita kaksi selkeää perustelua. Päätä lyhyellä yhteenvedolla tai johtopäätöksellä.",
        answerText = "Hyvä sulle kuuluu on ollut parisuhde on tosi pelottavaa kaikki mitä rakastin sitä ei se ole niin helppoa se ei vastannut mitään ei seuraa sua ja voidaan sopia myös se on ihan ok jos se ei vastannut vielä yksi on joukosta löydät täältä ei oo vielä siinä viestissä on ollut tosi kiltti tyttö ei se on ihan ok mutta en haluaa olla mukana tekemässä tikusta ei oo vielä nukkunut",
        wordCounter = "69 / 100 words"
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0423",
        skill = YkiPracticeSkill.Writing,
        number = "11/17",
        title = "Kirjoita argumentoiva teksti.",
        cefr = "B2 — Can write an essay presenting arguments for and against a position.",
        passage = "Kaupunki harkitsee yksityisautoilun rajoittamista keskustassa ilmanlaadun ja liikenteen sujuvuuden parantamiseksi. Monet asukkaat vastustavat muutosta.\n\nKirjoita 100-130 sanaa. Esitä sekä puolesta- että vastaargumentteja ja päätä omaan kantaasi. Käytä asiallista ja selkeää kieltä.\n\nEsitä vähintään yksi puolesta- ja yksi vastaargumentti. Perustele oma kantasi loppukappaleessa. Vältä liian arkista kieltä.",
        answerText = "Yö on tosi vaikea saada kokeilemaan jos yöllinen se että alle 5 vuota ei oo vielä siinä viestissä saat olla missä tahansa Yu ei oo vielä nukkunut muutama vuosi on vaihtunut se että kiusan se että kiusan sua ja sun Tarja Turunen olen yrittänyt että se ei vastannut mitään muuta kuin suomi ei se että alle ja kuoli ja se näkyy sun pitää sinut lämpimänä tai jäähtyneenä se että alle 5 ei kerro koko ajan lisää se on ihan hyvä mutta",
        wordCounter = "79 / 115 words"
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0424-IMG_0425",
        skill = YkiPracticeSkill.Writing,
        number = "12/17",
        title = "Kirjoita lyhyt raportti.",
        cefr = "B2 — Can write a structured report on a familiar topic.",
        passage = "Olet osallistunut yhteisösi asukasiltaan, jossa käsiteltiin lähipuiston kunnostamista. Kirjoita lyhyt raportti kokouksen tuloksista puiston suunnitteluryhmälle.\n\nRaportin tulee sisältää:\n- kokouksessa esitetyt ongelmat\n- asukkaiden toiveet\n- suositeltava seuraava toimenpide\n\nKirjoita 90-120 sanaa.\n\nKäytä selkeää rakennetta: ongelmat > toiveet > toimenpide. Raporttikirjoitus on tiivistä ja asiallista.",
        answerText = "Vuoden ensimmäinen kokonainen broileri on tosi pelottavaa ja se näkyy myös siinä tapauksessa jos se ei ole mitään järkeä ja sen jälkeen lupasit monta vuotta hyvää kuvaa ei ole mitään järkeä on ollut parisuhde ja se näkyy sun käytössä on myös se on ihan hyvä juttu siinä vaiheessa että se ei vastannut vielä ole mutta en löytänyt mutta en löytänyt nopeasti palaa takaisin ja masturboida webcam amatoori isot upeat ja se näkyy myös päätyi kuukausi ja",
        wordCounter = "76 / 105 words",
        saved = true
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0426-IMG_0427",
        skill = YkiPracticeSkill.Writing,
        number = "13/17",
        title = "Kirjoita virallinen sähköpostiviesti.",
        cefr = "B1 — Can write a formal letter or email on familiar topics.",
        passage = "Olet tilannut verkkokaupasta tuotteen, joka on tullut rikki. Kirjoita reklamaatioviesti verkkokaupan asiakaspalveluun.\n\nViestin tulee sisältää:\n- tilausnumero (keksitty, esim. TK-20481)\n- kuvaus ongelmasta\n- mitä toivot tilanteen ratkaisuksi\n\nKirjoita 80-110 sanaa virallisella asiakaspalvelutyyliä käyttäen.\n\nKäytä virallista tervehdystä ja lopetusta. Esitä faktat selkeässä järjestyksessä. Pyyntösi pitää olla yksiselitteinen.",
        answerText = "Nuo on ollut tosi huono puoli että on aika erota asiasanat arviointi on tosi vaikea löytää jos se ei ole vielä julkisia että se ei ole vielä julkisia että se ei vastannut mitään tekemistä niin että ne on niin tärkeä nyt on tilanne oli joku suunnitelma se että kiusan se että kiusan sua ei oo vielä siinä viestissä on tosi pelottavaa kaikki nähtävä se että kiusan se on ihan ok jos et haluaa ostaa mitä lupasit monta",
        wordCounter = "77 / 95 words",
        saved = true
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0428",
        skill = YkiPracticeSkill.Speaking,
        number = "14/17",
        title = "Keskustelu työn haasteista.",
        cefr = "B2 — Can take an active part in discussions in familiar contexts.",
        passage = "Osallistu keskusteluun työn stressistä AI-kumppanin kanssa. Kerro omista kokemuksistasi, anna mielipiteesi stressinhallinnan keinoista ja kysy kumppanin ajatuksia.\n\nReagoi AI:n vastauksiin — älä vain monologoi. Käytä korjauskieltä jos et ymmärrä: 'Tarkoitatko, että...?'"
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0429",
        skill = YkiPracticeSkill.Speaking,
        number = "15/17",
        title = "Terveydenhuollon roolipeli.",
        cefr = "B2 — Can describe experiences and give reasons for professional decisions.",
        passage = "Harjoittele terveydenhuollon tilanteita AI-kumppanin kanssa. Olet potilas, joka käy lääkärissä. Kerro oireesi selkeästi ja kysy tietoa hoitovaihtoehdoista.\n\nKäytä täsmällistä kieltä oireista: milloin alkoi, kuinka vakava, mikä helpottaa. Esitä myös yksi kysymys lääkärille."
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0430",
        skill = YkiPracticeSkill.Speaking,
        number = "16/17",
        title = "Mielipide suomalaisesta ruokakulttuurista.",
        cefr = "B1 — Can express and justify opinions on familiar topics.",
        passage = "Kerro suomalaisesta ruokakulttuurista AI-kumppanille. Puhu ainakin kahdesta suomalaisesta ruoasta ja sano, pidätkö niistä vai et. Perustele mielipiteesi.\n\nKäytä rakennetta: mainitse ruoka > anna mielipide > perustele. Vältä liian lyhyitä vastauksia."
    ),
    YkiPracticeBankTask(
        screenshot = "IMG_0431",
        skill = YkiPracticeSkill.Speaking,
        number = "17/17",
        title = "Ajanvaraus puhelimitse.",
        cefr = "B1 — Can deal with most situations likely to arise when booking appointments.",
        passage = "Soitat palvelutoimistoon varataksesi ajan. Sinun täytyy selittää syy käyntiisi, ehdottaa sopivaa aikaa ja vahvistaa varaus. Harjoittele tätä puhelinkeskustelua AI-kumppanin kanssa.\n\nKäytä kohteliasta kieltä. Kuuntele AI:n kysymykset tarkasti ja vastaa täysillä lauseilla. Tavoite on viisi vuoroa."
    )
)
}
