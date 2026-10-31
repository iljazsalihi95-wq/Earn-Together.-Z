package com.weearntogether.play

/** Albanian presenter speech helpers. Visual text stays untouched; only narration is normalized. */
object ArenaAlbanianSpeech {
    private val units = listOf("zero","një","dy","tre","katër","pesë","gjashtë","shtatë","tetë","nëntë","dhjetë","njëmbëdhjetë","dymbëdhjetë","trembëdhjetë","katërmbëdhjetë","pesëmbëdhjetë","gjashtëmbëdhjetë","shtatëmbëdhjetë","tetëmbëdhjetë","nëntëmbëdhjetë")
    fun number(n:Long):String {
        if(n<0)return "minus "+number(-n)
        if(n<20)return units[n.toInt()]
        if(n<100)return listOf("","","njëzet","tridhjetë","dyzet","pesëdhjetë","gjashtëdhjetë","shtatëdhjetë","tetëdhjetë","nëntëdhjetë")[(n/10).toInt()]+if(n%10>0)" e "+number(n%10) else ""
        if(n<1000)return units[(n/100).toInt()]+"qind"+if(n%100>0)" e "+number(n%100) else ""
        for((scale,word) in listOf(1000000000L to "miliardë",1000000L to "milionë",1000L to "mijë"))if(n>=scale)return number(n/scale)+" "+word+if(n%scale>0)" e "+number(n%scale) else ""
        return n.toString()
    }
    fun forVoice(text:String, locale:String):String {
        if(locale!="sq")return text
        var t=text
        for((pattern,spoken) in listOf("a(?:\\s*\\.\\s*|\\s+)s" to "alejhi selam","r(?:\\s*\\.\\s*|\\s+)a" to "radijallahu anhu","xh(?:\\s*\\.\\s*|\\s+)sh" to "xhel-le shanuhu"))
            t=t.replace(Regex("(?<![\\p{L}\\d])"+pattern+"\\.?(?![\\p{L}\\d])",RegexOption.IGNORE_CASE),spoken)
        t=t.replace("ﷺ","sal-lallahu alejhi ve sel-lem")
        t=t.replace(Regex("\\b(\\d{1,3}):(\\d{1,3})\\b")){"surja "+number(it.groupValues[1].toLong())+", ajeti "+number(it.groupValues[2].toLong())}
        t=t.replace(Regex("\\b(\\d{1,2})[./](\\d{1,2})[./](\\d{4})\\b")){
            val months=listOf("","janar","shkurt","mars","prill","maj","qershor","korrik","gusht","shtator","tetor","nëntor","dhjetor")
            number(it.groupValues[1].toLong())+" "+(months.getOrNull(it.groupValues[2].toInt())?:it.groupValues[2])+" "+number(it.groupValues[3].toLong())
        }
        t=t.replace(Regex("\\b(\\d+)[,.](\\d+)\\b")){number(it.groupValues[1].toLong())+" presje "+it.groupValues[2].map{c->number(c.digitToInt().toLong())}.joinToString(" ")}
        t=t.replace(Regex("(?<![\\p{L}\\d])\\d{1,12}(?![\\p{L}\\d])")){number(it.value.toLong())}.replace("%"," për qind")
        return t.replace(Regex("Kur[’']ani",RegexOption.IGNORE_CASE),"Kurani").replace(Regex("\\s+")," ").trim()
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
