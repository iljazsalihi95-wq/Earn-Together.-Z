package com.weearntogether.play

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Device-only practice. Never writes CloudProgress, wallet receipts, or LIVE ranking. */
class ArenaOfflineStore(private val context:Context,user:String){
    private val prefs=context.getSharedPreferences("arena_offline_v1_"+user,Context.MODE_PRIVATE)
    private fun verifiedMathQuestions():List<QuizArenaActivity.Question>{
        data class MathQ(val prompt:String,val choices:List<String>,val correct:Int,val explanation:String)
        val rows=listOf(
            MathQ("Sa është 18 + 27?",listOf("35","45","55","46"),1,"18 + 27 = 45."),
            MathQ("Sa është 72 - 38?",listOf("34","44","36","32"),0,"72 - 38 = 34."),
            MathQ("Sa është 9 × 8?",listOf("63","72","81","64"),1,"9 × 8 = 72."),
            MathQ("Sa është 144 ÷ 12?",listOf("10","11","12","14"),2,"144 ÷ 12 = 12."),
            MathQ("Sa është 25% e numrit 80?",listOf("15","20","25","30"),1,"25% është një e katërta; 80 ÷ 4 = 20."),
            MathQ("Sa është 3/4 e numrit 40?",listOf("20","25","30","35"),2,"40 ÷ 4 = 10 dhe 10 × 3 = 30."),
            MathQ("Një trekëndësh ka kënde 50° dhe 60°. Sa është këndi i tretë?",listOf("60°","70°","80°","90°"),1,"Shuma e këndeve të trekëndëshit është 180°; 180 - 50 - 60 = 70°."),
            MathQ("Sa është perimetri i një drejtkëndëshi me gjatësi 8 cm dhe gjerësi 5 cm?",listOf("13 cm","26 cm","40 cm","30 cm"),1,"Perimetri është 2 × (8 + 5) = 26 cm."),
            MathQ("Sa është sipërfaqja e një drejtkëndëshi 7 cm × 6 cm?",listOf("13 cm²","36 cm²","42 cm²","48 cm²"),2,"Sipërfaqja është 7 × 6 = 42 cm²."),
            MathQ("Sa është 2³?",listOf("6","8","9","12"),1,"2³ = 2 × 2 × 2 = 8."),
            MathQ("Cili numër është 15 më i madh se 37?",listOf("42","47","52","62"),2,"37 + 15 = 52."),
            MathQ("Sa minuta janë 2 orë e 30 minuta?",listOf("120","130","150","180"),2,"2 orë janë 120 minuta; 120 + 30 = 150."),
            MathQ("Nëse 5 fletore kushtojnë 15 €, sa kushton një fletore?",listOf("2 €","3 €","4 €","5 €"),1,"15 ÷ 5 = 3 € për fletore."),
            MathQ("Sa është mesatarja e numrave 4, 6 dhe 8?",listOf("5","6","7","8"),1,"(4 + 6 + 8) ÷ 3 = 18 ÷ 3 = 6."),
            MathQ("Sa është 0,5 × 60?",listOf("20","30","40","50"),1,"0,5 është gjysma; gjysma e 60 është 30."),
            MathQ("Sa është 10% e numrit 350?",listOf("25","30","35","40"),2,"10% e 350 është 350 ÷ 10 = 35."),
            MathQ("Një katror ka perimetër 36 cm. Sa është gjatësia e një brinje?",listOf("6 cm","8 cm","9 cm","12 cm"),2,"Katrori ka katër brinjë të barabarta; 36 ÷ 4 = 9 cm."),
            MathQ("Sa është 7 × 6 - 10?",listOf("22","32","36","42"),1,"Së pari 7 × 6 = 42, pastaj 42 - 10 = 32."),
            MathQ("Cili është numri pasues në vargun 5, 10, 15, 20, ...?",listOf("21","24","25","30"),2,"Vargu rritet me 5; pas 20 vjen 25."),
            MathQ("Sa është 1/5 e numrit 100?",listOf("10","20","25","50"),1,"100 ÷ 5 = 20.")
        )
        return rows.mapIndexed{i,q->QuizArenaActivity.Question("Matematikë",q.prompt,q.choices,q.correct,q.explanation,"arena-math-${101+i}","Llogaritje aritmetike/gjeometri elementare — verifikuar lokalisht")}
    }
    val questions:List<QuizArenaActivity.Question> by lazy {
        val primary=runCatching{
            val data=JSONObject(context.assets.open("quiz-arena/sheet-bank-sq.json").bufferedReader().use{it.readText()}).getJSONArray("questions")
            (0 until data.length()).map{i->val q=data.getJSONObject(i);val options=q.getJSONArray("options");QuizArenaActivity.Question(q.getString("category"),q.getString("question"),(0 until options.length()).map{options.getString(it)},q.getInt("correctIndex"),q.getString("explanation"),q.getString("id"),q.getString("source"),q.optInt("difficulty",1))}
        }.getOrElse{val rows=JSONObject(context.assets.open("quiz-arena/sheet-snapshot.json").bufferedReader().use{it.readText()}).getJSONArray("rows");QuizBank(context).parse(rows)}
        val extra=emptyList<QuizArenaActivity.Question>()
        (primary+extra+verifiedMathQuestions()).filter{it.id.isNotBlank()&&it.prompt.isNotBlank()&&it.why.isNotBlank()&&it.source.isNotBlank()&&it.choices.size in 2..5&&it.choices.all(String::isNotBlank)&&it.correct in it.choices.indices}.distinctBy{it.prompt.trim().lowercase(Locale.ROOT)}
    }
    fun categories()=questions.filter{it.choices.size==4}.map{it.category}.filter{it.isNotBlank()}.distinct().sorted()
    fun progress():JSONObject=JSONObject().put("level",prefs.getInt("level",1)).put("points",prefs.getInt("points",0)).put("best",prefs.getInt("best",0))
    fun saved():JSONObject?=runCatching{JSONObject(prefs.getString("session",null)!!)}.getOrNull()?.takeIf{valid(it)&&!it.optBoolean("finished")}
    private fun valid(s:JSONObject):Boolean=runCatching{val qs=s.getJSONArray("local_questions");qs.length()>0&&s.getInt("index") in 0 until qs.length()&&(0 until qs.length()).all{val q=qs.getJSONObject(it);q.getInt("correct") in 0 until q.getJSONArray("choices").length()}}.getOrDefault(false)
    fun start(category:String):JSONObject{
        val categoryPool=questions.filter{it.choices.size==4&&(category.isEmpty()||it.category==category)};require(categoryPool.isNotEmpty());val ceiling=(1+prefs.getInt("level",1)/4).coerceAtMost(3);val graded=categoryPool.filter{it.difficulty<=ceiling}
        val level=prefs.getInt("level",1)
        // Arena rounds must feel like real quiz rounds, never 2-3 question demos.
        // Start at 10 questions and grow gradually to 15 when enough verified material exists.
        val target=(10+(level-1)/3).coerceAtMost(15)
        val pool=if(graded.size>=minOf(target,categoryPool.size))graded else categoryPool
        val shuffled=pool.shuffled();val selected=shuffled.take(minOf(target,pool.size))
        // The answer clock starts after narration and grants the agreed 60 seconds.
        val config=JSONObject().put("seconds",60).put("points",100+level*10).put("bonus",50).put("pass",60).put("video","")
        fun encoded(q:QuizArenaActivity.Question)=JSONObject().put("id",q.id).put("category",q.category).put("prompt",q.prompt).put("choices",JSONArray(q.choices)).put("correct",q.correct).put("explanation",q.why).put("source",q.source).put("difficulty",q.difficulty)
        val qs=JSONArray(selected.map{encoded(it)})
        val s=JSONObject().put("offline",true).put("session_id","offline-"+java.util.UUID.randomUUID()).put("locale","sq").put("level",level).put("category",category).put("config",config).put("local_questions",qs).put("local_reserve",JSONArray(shuffled.drop(selected.size).map{encoded(it)})).put("index",0).put("total",qs.length()).put("score",0).put("correct",0).put("answers",JSONArray()).put("finished",false)
        select(s);save(s);return s
    }
    private fun select(s:JSONObject){s.put("question",s.getJSONArray("local_questions").getJSONObject(s.getInt("index")));s.remove("feedback");s.remove("timed_out");s.remove("answer_pending");s.remove("local_choice");s.remove("learning_choice");s.put("presentation_pending",true);s.put("presentation_remaining_ms",90000);s.put("remaining_ms",s.getJSONObject("config").getLong("seconds")*1000)}
    fun save(s:JSONObject){require(valid(s));prefs.edit().putString("session",s.toString()).commit()}
    fun answer(s:JSONObject,choice:Int):JSONObject{
        if(s.optBoolean("finished")||s.optJSONObject("feedback")!=null)return s
        val q=s.getJSONObject("question");require(choice==-1||choice in 0 until q.getJSONArray("choices").length());val right=choice==q.getInt("correct")
        if(right){s.put("correct",s.getInt("correct")+1);s.put("score",s.getInt("score")+s.getJSONObject("config").getInt("points"))}
        s.getJSONArray("answers").put(JSONObject().put("id",q.getString("id")).put("choice",choice).put("right",right))
        s.put("feedback",JSONObject().put("right",right).put("choice",choice).put("reason",if(choice==-1)"timeout" else if(right)"correct" else "incorrect").put("correct",q.getInt("correct")).put("explanation",q.getString("explanation")).put("source",q.optString("source")));save(s);return s
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
