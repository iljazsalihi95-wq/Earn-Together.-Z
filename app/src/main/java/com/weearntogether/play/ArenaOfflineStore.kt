package com.weearntogether.play

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Device-only practice. Never writes CloudProgress, wallet receipts, or LIVE ranking. */
class ArenaOfflineStore(private val context:Context,user:String){
    private val prefs=context.getSharedPreferences("arena_offline_v1_"+user,Context.MODE_PRIVATE)
    val questions:List<QuizArenaActivity.Question> by lazy {
        val primary=runCatching{val rows=JSONObject(context.assets.open("quiz-arena/sheet-snapshot.json").bufferedReader().use{it.readText()}).getJSONArray("rows");QuizBank(context).parse(rows)}.getOrDefault(emptyList())
        val extra=runCatching{val data=JSONObject(context.assets.open("quiz-arena/general-sq.json").bufferedReader().use{it.readText()}).getJSONArray("questions");(0 until data.length()).mapNotNull{i->val q=data.getJSONObject(i);if(q.optString("reviewStatus") !in setOf("verified","verified_source","verified_calculation"))return@mapNotNull null;val options=q.optJSONArray("options")?:return@mapNotNull null;val sources=q.optJSONArray("sources")?:JSONArray();QuizArenaActivity.Question(q.optString("category"),q.optString("question"),(0 until options.length()).map{options.optString(it)},q.optInt("correctIndex",-1),q.optString("explanation"),q.optString("id"),(0 until sources.length()).joinToString("; "){sources.getJSONObject(it).optString("url",sources.getJSONObject(it).optString("work"))})}}.getOrDefault(emptyList())
        (primary+extra).filter{it.id.isNotBlank()&&it.prompt.isNotBlank()&&it.why.isNotBlank()&&it.source.isNotBlank()&&it.choices.size in 2..5&&it.choices.all(String::isNotBlank)&&it.correct in it.choices.indices}.distinctBy{it.prompt.trim().lowercase(Locale.ROOT)}
    }
    fun categories()=questions.map{it.category}.distinct().sorted()
    fun progress():JSONObject=JSONObject().put("level",prefs.getInt("level",1)).put("points",prefs.getInt("points",0)).put("best",prefs.getInt("best",0))
    fun saved():JSONObject?=runCatching{JSONObject(prefs.getString("session",null)!!)}.getOrNull()?.takeIf{valid(it)&&!it.optBoolean("finished")}
    private fun valid(s:JSONObject):Boolean=runCatching{val qs=s.getJSONArray("local_questions");qs.length()>0&&s.getInt("index") in 0 until qs.length()&&(0 until qs.length()).all{val q=qs.getJSONObject(it);q.getInt("correct") in 0 until q.getJSONArray("choices").length()}}.getOrDefault(false)
    fun start(category:String):JSONObject{
        val pool=questions.filter{category.isEmpty()||it.category==category};require(pool.isNotEmpty())
        val level=prefs.getInt("level",1);val selected=pool.shuffled().take(minOf(5+(level-1)/4,pool.size))
        val config=JSONObject().put("seconds",maxOf(20,60-level*2)).put("points",100+level*10).put("bonus",50).put("pass",60).put("video","")
        val qs=JSONArray(selected.map{q->JSONObject().put("id",q.id).put("category",q.category).put("prompt",q.prompt).put("choices",JSONArray(q.choices)).put("correct",q.correct).put("explanation",q.why).put("source",q.source)})
        val s=JSONObject().put("offline",true).put("session_id","offline-"+java.util.UUID.randomUUID()).put("locale","sq").put("level",level).put("category",category).put("config",config).put("local_questions",qs).put("index",0).put("total",qs.length()).put("score",0).put("correct",0).put("answers",JSONArray()).put("finished",false)
        select(s);save(s);return s
    }
    private fun select(s:JSONObject){s.put("question",s.getJSONArray("local_questions").getJSONObject(s.getInt("index")));s.remove("feedback");s.put("remaining_ms",s.getJSONObject("config").getLong("seconds")*1000)}
    fun save(s:JSONObject){require(valid(s));prefs.edit().putString("session",s.toString()).commit()}
    fun answer(s:JSONObject,choice:Int):JSONObject{
        if(s.optBoolean("finished")||s.optJSONObject("feedback")!=null)return s
        val q=s.getJSONObject("question");require(choice==-1||choice in 0 until q.getJSONArray("choices").length());val right=choice==q.getInt("correct")
        if(right){s.put("correct",s.getInt("correct")+1);s.put("score",s.getInt("score")+s.getJSONObject("config").getInt("points"))}
        s.getJSONArray("answers").put(JSONObject().put("id",q.getString("id")).put("choice",choice).put("right",right))
        s.put("feedback",JSONObject().put("right",right).put("correct",q.getInt("correct")).put("explanation",q.getString("explanation")).put("source",q.optString("source")));save(s);return s
    }
    fun next(s:JSONObject):JSONObject{
        if(s.optBoolean("finished"))return s;require(s.optJSONObject("feedback")!=null)
        if(s.getInt("index")+1<s.getInt("total")){s.put("index",s.getInt("index")+1);select(s);save(s);return s}
        val passed=s.getInt("correct")*100/s.getInt("total")>=s.getJSONObject("config").getInt("pass");s.put("finished",true).put("passed",passed)
        if(s.getInt("correct")==s.getInt("total"))s.put("score",s.getInt("score")+s.getJSONObject("config").getInt("bonus"))
        // One synchronous transaction records both completion and totals; repeated Next is harmless.
        prefs.edit().putInt("best",maxOf(prefs.getInt("best",0),s.getInt("score"))).putInt("points",prefs.getInt("points",0)+s.getInt("score")).putInt("level",if(passed)minOf(20,s.getInt("level")+1) else s.getInt("level")).putString("session",s.toString()).commit();return s
    }
}
