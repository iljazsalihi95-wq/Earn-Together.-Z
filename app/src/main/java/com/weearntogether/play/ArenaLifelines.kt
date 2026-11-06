package com.weearntogether.play

import org.json.JSONArray
import org.json.JSONObject

/** Practice lifelines live inside the saved round, so closing the app cannot reset them.
 * Friend and audience explicitly simulate opinions; neither inspects the answer key. */
internal object ArenaLifelines {
    val kinds = setOf("fifty", "friend", "audience", "hint", "switch")
    fun used(s: JSONObject, kind: String) = s.optJSONObject("lifelines")?.has(kind) == true
    fun use(s: JSONObject, kind: String): JSONObject {
        require(kind in kinds) { "INVALID_LIFELINE" }
        check(!s.optBoolean("finished") && s.optJSONObject("feedback") == null && !s.optBoolean("presentation_pending") && !s.optBoolean("timed_out")) { "LIFELINE_UNAVAILABLE" }
        val used = s.optJSONObject("lifelines") ?: JSONObject().also { s.put("lifelines", it) }
        check(!used.has(kind)) { "LIFELINE_USED" }
        val q = s.getJSONObject("question"); val choices = q.getJSONArray("choices")
        val result = JSONObject().put("kind", kind).put("question_id", q.getString("id"))
        when (kind) {
            "fifty" -> {
                require(choices.length() >= 3) { "AT_LEAST_THREE_CHOICES_REQUIRED" }
                result.put("hidden", JSONArray((0 until choices.length()).filter { it != q.getInt("correct") }.shuffled().take(choices.length()-2)))
            }
            "friend" -> {
                result.put("text", simulatedFriend(q)).put("simulated", true)
            }
            "audience" -> {
                val weights = List(choices.length()) { (10..70).random() }; val sum = weights.sum()
                val votes = weights.map { it * 100 / sum }.toMutableList(); votes[0] += 100 - votes.sum()
                result.put("votes", JSONArray(votes)).put("simulated", true)
            }
            "hint" -> result.put("text", hint(q))
            "switch" -> {
                val all = s.getJSONArray("local_questions")
                val reserve=s.optJSONArray("local_reserve")
                check(reserve!=null&&reserve.length()>0){"NO_REPLACEMENT"}
                val replacement=reserve.getJSONObject(0);reserve.remove(0)
                all.put(s.getInt("index"),replacement);s.put("question",replacement)
                s.put("presentation_pending", true).put("presentation_remaining_ms", 90000)
            }
        }
        used.put(kind, result)
        return result
    }
    fun simulatedFriend(q:JSONObject):String {
        val choices=q.getJSONArray("choices")
        val guess=(0 until choices.length()).random()
        return "Unë do të provoja alternativën ${'A'+guess}, ${choices.getString(guess)}. Krahasoje me pyetjen para se të vendosësh. Ky është mendim i simuluar, jo garanci."
    }
    fun hint(q:JSONObject):String {
        val prompt=q.optString("prompt").lowercase(java.util.Locale.ROOT)
        val focus=when {
            listOf("nuk ","pavërtet", "përveç").any { it in prompt }->"Kujdes me mohimin: po kërkohet alternativa që NUK përputhet."
            listOf("vit", "mosh", "sa ").any { it in prompt }->"Dallo çfarë kërkohet: numër, moshë apo vit. Kontrollo edhe nëse datat janë hixhri apo miladi."
            listOf("kush", "cili", "cila").any { it in prompt }->"Dallo personin, emrin ose ngjarjen që kërkon pyetja dhe krahaso alternativat një nga një."
            else->"Përqendrohu te termi kryesor dhe kushtet që përmenden në pyetje."
        }
        return focus+"\n\n"+q.optString("prompt")+"\n\nOrientim lokal; përgjigjja mbetet e fshehur deri pas zgjedhjes."
    }
    fun hidden(s: JSONObject): Set<Int> {
        val r = s.optJSONObject("lifelines")?.optJSONObject("fifty") ?: return emptySet()
        if (r.optString("question_id") != s.optJSONObject("question")?.optString("id")) return emptySet()
        val values = r.optJSONArray("hidden") ?: return emptySet()
        return (0 until values.length()).map { values.getInt(it) }.toSet()
    }
}
