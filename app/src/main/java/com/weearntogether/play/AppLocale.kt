package com.weearntogether.play

import java.util.Locale

/** Phone-locale UI. Supported: English, German and Italian. All other locales fall back to English. */
object AppLocale {
    private fun lang(): String = when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
        "de" -> "de"
        "it" -> "it"
        else -> "en"
    }

    private val de = mapOf(
        "Mirë se vjen" to "Willkommen",
        "Luaj. Përparoi. Zhblloko sfida." to "Spielen. Fortschritt machen. Herausforderungen freischalten.",
        "Krijo profilin lokal për të ruajtur progresin." to "Erstelle ein lokales Profil, um deinen Fortschritt zu speichern.",
        "Emri në lojë" to "Spielername", "VAZHDO" to "WEITER", "Lojtar" to "Spieler",
        "Zgjidh lojën dhe ngjitu në renditje." to "Wähle ein Spiel und verbessere deinen Rang.",
        "FEATURED" to "EMPFOHLEN", "TË GJITHA LOJËRAT" to "ALLE SPIELE", "PROGRESI IM" to "MEIN FORTSCHRITT",
        "Gjuajtje e shpejtë • nivele • rekord" to "Schnelle Schüsse • Level • Highscore",
        "Strategji • kombinime • progres" to "Strategie • Combos • Fortschritt",
        "Bashko numrat dhe arrij rekordin" to "Zahlen verbinden und Highscore knacken",
        "10 pyetje • rezultat i menjëhershëm" to "10 Fragen • sofortiges Ergebnis",
        "Gjej çiftet me sa më pak lëvizje" to "Finde Paare mit möglichst wenigen Zügen",
        "10 sfida të shpejta matematikore" to "10 schnelle Mathe-Herausforderungen",
        "Profili" to "Profil", "Ruaj emrin" to "Namen speichern", "Progresi" to "Fortschritt", "Kthehu" to "Zurück",
        "SFIDA DITORE" to "TÄGLICHE HERAUSFORDERUNG", "Bonusi ditor është marrë." to "Tagesbonus bereits abgeholt.",
        "Merr 100 pikë" to "100 Punkte holen", "Syno • gjuaj • pastro tabelën. Prek topin 'Tjetra' për ta ndërruar." to "Zielen • schießen • Feld leeren. Tippe auf die nächste Kugel zum Tauschen.",
        "Rifillo" to "Neu starten", "Luaj përsëri" to "Nochmal spielen", "Rezultati" to "Ergebnis", "PËRFUNDUAR" to "GESCHAFFT",
        "Sa ditë ka një javë?" to "Wie viele Tage hat eine Woche?", "Cili planet quhet Planeti i Kuq?" to "Welcher Planet wird der Rote Planet genannt?",
        "Cili është oqeani më i madh?" to "Welcher ist der größte Ozean?", "Sa brinjë ka trekëndëshi?" to "Wie viele Seiten hat ein Dreieck?",
        "Cila është kryeqyteti i Francës?" to "Was ist die Hauptstadt von Frankreich?", "Sa minuta ka një orë?" to "Wie viele Minuten hat eine Stunde?",
        "Cili është simboli kimik i ujit?" to "Wie lautet die chemische Formel für Wasser?", "Cila gjuhë flitet kryesisht në Brazil?" to "Welche Sprache wird hauptsächlich in Brasilien gesprochen?",
        "Venusi" to "Venus", "Marsi" to "Mars", "Jupiteri" to "Jupiter", "Saturni" to "Saturn", "Atlantiku" to "Atlantik", "Indian" to "Indischer Ozean", "Paqësori" to "Pazifik", "Arktiku" to "Arktischer Ozean", "Roma" to "Rom", "Parisi" to "Paris", "Berlini" to "Berlin", "Vjena" to "Wien", "Spanjisht" to "Spanisch", "Portugalisht" to "Portugiesisch", "Italisht" to "Italienisch", "Frëngjisht" to "Französisch"
    )
    private val it = mapOf(
        "Mirë se vjen" to "Benvenuto", "Luaj. Përparoi. Zhblloko sfida." to "Gioca. Avanza. Sblocca sfide.",
        "Krijo profilin lokal për të ruajtur progresin." to "Crea un profilo locale per salvare i progressi.", "Emri në lojë" to "Nome giocatore", "VAZHDO" to "CONTINUA", "Lojtar" to "Giocatore",
        "Zgjidh lojën dhe ngjitu në renditje." to "Scegli un gioco e scala la classifica.", "FEATURED" to "IN EVIDENZA", "TË GJITHA LOJËRAT" to "TUTTI I GIOCHI", "PROGRESI IM" to "I MIEI PROGRESSI",
        "Gjuajtje e shpejtë • nivele • rekord" to "Tiri rapidi • livelli • record", "Strategji • kombinime • progres" to "Strategia • combo • progressi", "Bashko numrat dhe arrij rekordin" to "Unisci i numeri e batti il record", "10 pyetje • rezultat i menjëhershëm" to "10 domande • risultato immediato", "Gjej çiftet me sa më pak lëvizje" to "Trova le coppie con meno mosse", "10 sfida të shpejta matematikore" to "10 rapide sfide matematiche",
        "Profili" to "Profilo", "Ruaj emrin" to "Salva nome", "Progresi" to "Progressi", "Kthehu" to "Indietro", "SFIDA DITORE" to "SFIDA GIORNALIERA", "Bonusi ditor është marrë." to "Bonus giornaliero già riscattato.", "Merr 100 pikë" to "Ottieni 100 punti", "Rifillo" to "Ricomincia", "Luaj përsëri" to "Gioca ancora", "Rezultati" to "Risultato", "PËRFUNDUAR" to "COMPLETATO"
    )
    private val en = mapOf(
        "Mirë se vjen" to "Welcome", "Luaj. Përparoi. Zhblloko sfida." to "Play. Progress. Unlock challenges.", "Krijo profilin lokal për të ruajtur progresin." to "Create a local profile to save your progress.", "Emri në lojë" to "Player name", "VAZHDO" to "CONTINUE", "Lojtar" to "Player",
        "Zgjidh lojën dhe ngjitu në renditje." to "Choose a game and climb the ranks.", "FEATURED" to "FEATURED", "TË GJITHA LOJËRAT" to "ALL GAMES", "PROGRESI IM" to "MY PROGRESS",
        "Gjuajtje e shpejtë • nivele • rekord" to "Fast shooting • levels • high score", "Strategji • kombinime • progres" to "Strategy • combos • progress", "Bashko numrat dhe arrij rekordin" to "Merge numbers and beat your high score", "10 pyetje • rezultat i menjëhershëm" to "10 questions • instant result", "Gjej çiftet me sa më pak lëvizje" to "Find pairs in as few moves as possible", "10 sfida të shpejta matematikore" to "10 fast math challenges",
        "Profili" to "Profile", "Ruaj emrin" to "Save name", "Progresi" to "Progress", "Kthehu" to "Back", "SFIDA DITORE" to "DAILY CHALLENGE", "Bonusi ditor është marrë." to "Daily bonus already claimed.", "Merr 100 pikë" to "Claim 100 points", "Syno • gjuaj • pastro tabelën. Prek topin 'Tjetra' për ta ndërruar." to "Aim • shoot • clear the board. Tap the next bubble to swap.", "Rifillo" to "Restart", "Luaj përsëri" to "Play again", "Rezultati" to "Result", "PËRFUNDUAR" to "COMPLETED"
    )

    fun t(value: String): String {
        val map = when (lang()) { "de" -> de; "it" -> it; else -> en }
        map[value]?.let { return it }
        // Never expose Albanian-only dynamic UI on unsupported translations; convert common fragments.
        var s = value
        val common = if (lang()=="de") listOf("PIKË" to "PUNKTE","SOT" to "HEUTE","Lojëra:" to "Spiele:","Rekord:" to "Rekord:","Lëvizje" to "Züge","Çifte" to "Paare","PYETJA" to "FRAGE","SFIDA" to "RUNDE") else if(lang()=="it") listOf("PIKË" to "PUNTI","SOT" to "OGGI","Lojëra:" to "Giochi:","Rekord:" to "Record:","Lëvizje" to "Mosse","Çifte" to "Coppie","PYETJA" to "DOMANDA","SFIDA" to "SFIDA") else listOf("PIKË" to "POINTS","SOT" to "TODAY","Lojëra:" to "Games:","Rekord:" to "Best:","Lëvizje" to "Moves","Çifte" to "Pairs","PYETJA" to "QUESTION","SFIDA" to "ROUND")
        common.forEach { (a,b) -> s=s.replace(a,b) }
        return s
    }
}
