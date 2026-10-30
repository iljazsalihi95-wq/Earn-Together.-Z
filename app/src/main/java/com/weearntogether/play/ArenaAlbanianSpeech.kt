package com.weearntogether.play

/** Albanian presenter speech helpers. Visual text stays untouched; only narration is normalized. */
object ArenaAlbanianSpeech {
    private val splitSH = Regex("(?i)([a-zçë])-s-h([a-zçë])")
    private val standaloneSH = Regex("(?i)\\bs-h")

    fun forVoice(text:String, locale:String):String {
        if(locale != "sq") return text
        return text
            .replace(Regex("(?i)mus-haf"), "mus haf")
            .replace(Regex("(?i)as-hab(ë|e)?")) { m -> if(m.value.endsWith("ë",true)) "as habë" else "as habe" }
            .replace(splitSH, "$1s h$2")
            .replace(standaloneSH, "s h")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun correctReaction(questionIndex:Int):String = when(questionIndex % 6) {
        0 -> "MashaAllah, bukur! E ke qëlluar."
        1 -> "Shumë mirë! Përgjigjja është e saktë."
        2 -> "Saktë! Vazhdo kështu."
        3 -> "MashaAllah! Edhe një përgjigje e bukur."
        4 -> "Bravo, e gjete! Të lumtë."
        else -> "Suksese! E ke qëlluar saktë."
    }

    fun wrongReaction(questionIndex:Int):String = when(questionIndex % 4) {
        0 -> "Këtë herë jo. Shiko shpjegimin dhe vazhdo."
        1 -> "Afër. Le ta mësojmë përgjigjen e saktë."
        2 -> "Nuk është kjo. S'ka problem, vazhdojmë me shpjegimin."
        else -> "Provo përsëri në pyetjen tjetër. Tani shiko pse kjo përgjigje është e saktë."
    }

    fun explanation(correct:String, explanation:String, source:String=""):String = buildString {
        append("Përgjigjja e saktë është: ").append(correct.trim()).append(". ")
        if(explanation.isNotBlank()) append(explanation.trim())
        if(source.isNotBlank()) append(" Burimi: ").append(source.trim())
    }.trim()
}
