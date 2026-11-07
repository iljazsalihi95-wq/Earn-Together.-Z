package com.weearntogether.play

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

/** Native learning rounds. Practice scores stay separate from server-ranked Arena scores. */
class ArenaLearningGamesActivity:Activity(){
    private lateinit var root:LinearLayout
    private lateinit var ads:NativeGameAds
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("arena_learning_"+(AccountStore(this).currentUserId()?:"guest"),MODE_PRIVATE) }
    private var mode="";private var state=JSONObject();private var scene:QuizArenaScene?=null
    private var applause:android.media.MediaPlayer?=null
    private var revision=0
    private var quickMode=false
    private val bank by lazy { ArenaOfflineStore(this,AccountStore(this).currentUserId()?:"guest").questions }
    private val verses by lazy { assets.open("quiz-arena/verse-completion-sq.json").bufferedReader().use { JSONObject(it.readText()).getJSONArray("verses") } }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun bg(a:Int=0xff14294c.toInt(),b:Int=0xff30204c.toInt())=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(a,b)).apply{cornerRadius=dp(18).toFloat();setStroke(dp(1),0xff7583ac.toInt())}
    private fun label(text:String,size:Int=18)=TextView(this).apply{this.text=text;textSize=size.toFloat();setTextColor(Color.WHITE);setPadding(dp(10),dp(10),dp(10),dp(10));gravity=Gravity.CENTER}
    private fun button(text:String,action:()->Unit)=Button(this).apply{this.text=text;isAllCaps=false;textSize=17f;setTextColor(Color.WHITE);background=bg();setPadding(dp(10),dp(8),dp(10),dp(8));setOnClickListener{action()}}
    private fun add(v:android.view.View){root.addView(v,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})}
    private fun shell(title:String){root=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(16),dp(16),dp(16));background=bg(0xff080f27.toInt(),0xff17122f.toInt())};setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)});add(label(title,26));add(label("SFIDA MËSIMORE • PROGRES NË TELEFON",12))}
    override fun onCreate(b:Bundle?){super.onCreate(b);ads=NativeGameAds(this,"quiz_arena_learning");quickMode=b?.getBoolean("quickMode")?:intent.hasExtra("quick_mode");mode=b?.getString("mode")?:intent.getStringExtra("quick_mode")?:"";state=runCatching{JSONObject(b?.getString("state")?:prefs.getString("round_$mode",null)?:"{}")}.getOrDefault(JSONObject());if(mode.isEmpty())menu() else {if(b==null&&quickMode)newRound();render()}}
    private fun menu(){revision++;mode="";scene=null;shell("QUIZ ARENA • SFIDA TË REJA")
        add(label("Zgjidh mënyrën e lojës. Mëso, kujto dhe përmirëso rekordin tënd.",17))
        listOf("word" to "◆ Gjeje fjalën e duhur","verse" to "▣ Plotëso ajetin","memory" to "◈ Kujtesa • gjej çiftet","true_false" to "✓ E saktë apo e gabuar","order" to "↔ Rendit fjalët").forEach{(key,title)->add(button(title){open(key)})}
        add(label("Rekordi: ${prefs.getInt("best",0)} pikë",18));add(button("KTHEHU NË ARENË"){finish()})
    }
    private fun open(key:String){mode=key;state=runCatching{JSONObject(prefs.getString("round_$mode","{}")?:"{}")}.getOrDefault(JSONObject());if(!state.has("rounds")&&!state.has("cards")||key=="word"&&state.optJSONArray("rounds")?.optJSONObject(0)?.has("clues")!=true)newRound();render()}
    private fun newRound(){revision++;state=JSONObject().put("index",0).put("score",0).put("answered",false)
        if(mode=="memory"){
            val words=listOf("Kuran","Sabër","Namaz","Zekat","Haxh","Dua").take(if(quickMode)3 else 6)
            state.put("cards",JSONArray((words+words).shuffled())).put("matched",JSONArray()).put("flipped",JSONArray()).put("moves",0)
        }else if(mode=="word"){
            val puzzles=listOf(
                Triple("Suretul Asr", listOf("Sabër","Besim","Vepra të mira","E vërteta","Kuran","Humbje","Këshillim","Tri ajete","Koha"), "Kurani 103:1–3"),
                Triple("Ramazani", listOf("Agjërim","Syfyr","Iftar","Teravi","Kuran","Muaji i nëntë","Fitër Bajrami","Hëna e re","Nata e Kadrit"), "Kurani 2:183–185"),
                Triple("Haxhi", listOf("Mekë","Ihram","Tavaf","Arafat","Mina","Muzdelife","Zemzem","Dhul-Hixhe","Qabeja"), "Kurani 2:196–203")
            )
            state.put("rounds",JSONArray(puzzles.take(if(quickMode)1 else puzzles.size).map { (solution,clues,source) ->
                JSONObject().put("prompt","Zbulo fjalët. Gjej lidhjen!").put("answer",solution)
                    .put("clues",JSONArray(clues)).put("source",source)
            }))
        }else if(mode=="verse"){
            val rows=mutableListOf<JSONObject>()
            for(i in 0 until verses.length()){
                val verse=verses.getJSONObject(i);val words=verse.getString("arabic").split(" ")
                for(index in words.indices){
                    val correct=words[index]
                    val pool=(0 until verses.length()).flatMap{verses.getJSONObject(it).getString("arabic").split(" ")}.filter{it!=correct}.distinct().shuffled().take(3)
                    rows.add(JSONObject().put("prompt",words.mapIndexed{n,w->if(n==index)"[ … ]" else w}.joinToString(" ")).put("answer",correct).put("choices",JSONArray((pool+correct).shuffled())).put("full",verse.getString("arabic")).put("source",verse.getString("reference")).put("url",verse.getString("url")))
                }
            }
            state.put("rounds",JSONArray(rows.shuffled().take(if(quickMode)1 else 10)))
        }else if(mode=="true_false"){
            val rows=bank.shuffled().take(if(quickMode)1 else 10).map { q ->
                val proposed=if(kotlin.random.Random.nextBoolean()) q.correct else q.choices.indices.filter { it!=q.correct }.random()
                JSONObject().put("prompt",q.prompt).put("proposed",q.choices[proposed])
                    .put("right_proposal",proposed==q.correct).put("answer",q.choices[q.correct])
                    .put("source",q.source).put("full",q.why)
            }
            state.put("rounds",JSONArray(rows))
        }else{
            val pool=bank.filter{it.choices[it.correct].length in 2..80 && (mode!="order"||it.choices[it.correct].split(" ").size in 2..8)}.shuffled().take(if(quickMode)1 else 10)
            state.put("rounds",JSONArray(pool.map{q->JSONObject().put("prompt",q.prompt).put("answer",q.choices[q.correct]).put("choices",JSONArray(q.choices.shuffled())).put("source",q.source).put("full",q.why)}))
        }
        save()
    }
    private fun save(){if(mode.isNotEmpty()&&!quickMode)prefs.edit().putString("round_$mode",state.toString()).apply()}
    private fun render(){revision++;if(mode.isEmpty()){menu();return};if(!state.has("rounds")&&!state.has("cards")||mode=="word"&&state.optJSONArray("rounds")?.optJSONObject(0)?.has("clues")!=true){newRound()}
        if(mode=="memory"){memory();return}
        val rows=state.getJSONArray("rounds");val index=state.optInt("index")
        if(index>=rows.length()){results();return}
        val q=rows.getJSONObject(index);val answered=state.optBoolean("answered")
        shell(when(mode){"verse"->"PLOTËSO AJETIN";"order"->"RENDIT FJALËT";"true_false"->"E SAKTË APO E GABUAR";else->"GJEJE FJALËN E DUHUR"})
        add(label("${index+1} / ${rows.length()}  •  ★ ${state.optInt("score")}",16))
        val progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=rows.length();progress=index};add(progress)
        scene=QuizArenaScene(this).apply{playerName="SFIDA";questionPrompt=if(mode=="verse")"Gjej fjalën që mungon" else q.getString("prompt");questionCategory=if(mode=="verse")q.optString("source") else "MËSO DHE KUJTO";questionNumber="PYETJA ${index+1}";timerText="★ ${state.optInt("score")}";stageStatus="ILAZ SALIHI";reaction=if(answered&&state.optBoolean("right"))1 else 0}
        root.addView(scene,LinearLayout.LayoutParams(-1,dp(if(mode=="verse")160 else 220)))
        if(mode=="verse"){add(label("Ushtrim plotësimi • teksti i plotë shfaqet pas zgjedhjes",13));add(label(q.getString("prompt"),30).apply{textDirection=android.view.View.TEXT_DIRECTION_RTL})}
        if(mode=="word"){associationBoard(q,answered)}else if(mode=="true_false"){
            add(label("Përgjigjja e propozuar",14))
            add(label(q.getString("proposed"),22))
            add(button("E saktë"){answer(q.getBoolean("right_proposal"))}.apply{isEnabled=!answered})
            add(button("E gabuar"){answer(!q.getBoolean("right_proposal"))}.apply{isEnabled=!answered})
        }else if(mode=="order"){
            val answer=q.getString("answer").split(" ");val selected=state.optJSONArray("selected")?:JSONArray();val chips=state.optJSONArray("chips")?:JSONArray(answer.shuffled()).also{state.put("chips",it);save()}
            add(label((0 until selected.length()).joinToString(" "){chips.getString(selected.getInt(it))}.ifBlank{"Prek fjalët në rendin e duhur"},21))
            for(i in 0 until chips.length())add(button(chips.getString(i)){selected.put(i);state.put("selected",selected);save();render()}.apply{isEnabled=!answered&&(0 until selected.length()).none{selected.getInt(it)==i}})
            if(!answered){add(button("PASTRO RENDITJEN"){state.remove("selected");save();render()});add(button("KONTROLLO"){if(selected.length()==chips.length())answer((0 until selected.length()).joinToString(" "){chips.getString(selected.getInt(it))}==q.getString("answer"))})}
        }else{
            val choices=q.getJSONArray("choices")
            for(i in 0 until choices.length())add(button(choices.getString(i)){answer(choices.getString(i)==q.getString("answer"))}.apply{isEnabled=!answered})
        }
        if(answered){add(label(if(state.optBoolean("right"))"MASHALLAH • SAKTË!" else "MËSO DHE PROVO PËRSËRI",21));add(label(q.getString("answer"),24));add(label(q.optString("full"),if(mode=="verse")28 else 17));add(label(q.optString("source"),13))
            if(q.optString("url").startsWith("https://quran.com/"))add(button("LEXO AJETIN NË BURIM"){startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(q.getString("url"))))})
            add(button("VAZHDO ›"){state.put("index",index+1).put("answered",false);state.remove("selected");state.remove("chips");state.remove("revealed");save();render()})
        }
        add(button(if(quickMode)"KTHEHU TE PYETJET" else "MENYJA E SFIDAVE"){if(quickMode){setResult(RESULT_CANCELED);finish()}else{save();menu()}})
    }
    private fun associationBoard(q:JSONObject,answered:Boolean){
        val clues=q.getJSONArray("clues")
        val revealed=state.optInt("revealed")
        for(rowIndex in 0..2){
            val row=LinearLayout(this)
            for(column in 0..2){
                val index=rowIndex*3+column
                val bit=1 shl index
                val code="${'A'+column}${rowIndex+1}"
                val open=answered||revealed and bit!=0
                val tile=button(if(open)clues.getString(index) else code){
                    state.put("revealed",revealed or bit);save();render()
                }.apply{
                    isEnabled=!open
                    textSize=14f
                    minHeight=dp(58)
                    contentDescription=if(open)"$code, ${clues.getString(index)}" else "Zbulo $code"
                    if(open)background=bg(0xff145b66.toInt(),0xff20416e.toInt())
                }
                row.addView(tile,LinearLayout.LayoutParams(0,-1,1f).apply{setMargins(dp(3),0,dp(3),0)})
            }
            add(row)
        }
        if(answered)return
        add(button("GJEJ ZGJIDHJEN FINALE"){
            val input=android.widget.EditText(this).apply{hint="Shkruaj zgjidhjen";setSingleLine()}
            val dialog=android.app.AlertDialog.Builder(this).setTitle("Cila është lidhja?")
                .setView(input).setNegativeButton("Kthehu",null).setPositiveButton("Kontrollo",null).show()
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                fun key(text:String)=java.text.Normalizer.normalize(text.lowercase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD)
                    .filter{it.isLetterOrDigit()}
                val guess=key(input.text.toString())
                if(guess.isEmpty()){input.error="Shkruaj përgjigjen.";return@setOnClickListener}
                val expected=key(q.getString("answer"))
                val alternatives=when(expected){
                    "suretulasr"->setOf("asr","elasr","surejaasr","surejaelasr")
                    "ramazani"->setOf("ramazan","muajiramazanit")
                    "haxhi"->setOf("haxh")
                    else->emptySet()
                }
                dialog.dismiss();answer(guess==expected||guess in alternatives)
            }
        }.apply{isEnabled=revealed!=0})
    }

    private fun answer(right:Boolean){if(state.optBoolean("answered"))return;state.put("answered",true).put("right",right);if(right)state.put("score",state.optInt("score")+10);save();render();if(right)celebrate()}
    private fun celebrate(){scene?.reaction=1;applause?.release();applause=android.media.MediaPlayer.create(this,R.raw.arena_applause);applause?.setOnCompletionListener{it.release();applause=null};applause?.start()}
    private fun memory(){shell("KUJTESA • GJEJ ÇIFTET");val cards=state.getJSONArray("cards");val matched=state.getJSONArray("matched");val flipped=state.getJSONArray("flipped")
        if(matched.length()==cards.length()){results();return}
        add(label("${matched.length()/2} / ${cards.length()/2} çifte • ${state.optInt("moves")} përpjekje",18))
        scene=QuizArenaScene(this).apply{playerName="KUJTESA";questionPrompt="Gjej dy kartat me të njëjtën fjalë";questionCategory="PËRQENDROHU DHE KUJTO";timerText="${matched.length()/2} / ${cards.length()/2}";stageStatus="ILAZ SALIHI"}
        root.addView(scene,LinearLayout.LayoutParams(-1,dp(170)))
        val grid=GridLayout(this).apply{columnCount=3};add(grid)
        fun contains(a:JSONArray,i:Int)=(0 until a.length()).any{a.getInt(it)==i}
        for(i in 0 until cards.length()){
            val done=contains(matched,i);val visible=done||contains(flipped,i)
            val tile=button(if(visible)cards.getString(i) else "◆"){if(!done&&!contains(flipped,i)&&flipped.length()<2){flipped.put(i);state.put("flipped",flipped);if(flipped.length()==2)state.put("moves",state.optInt("moves")+1);save();render()}}
            tile.isEnabled=!done&&flipped.length()<2&&!contains(flipped,i);tile.alpha=if(done).45f else 1f
            grid.addView(tile,GridLayout.LayoutParams().apply{width=0;height=dp(86);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(3),dp(3),dp(3),dp(3))})
        }
        add(button(if(quickMode)"KTHEHU TE PYETJET" else "MENYJA E SFIDAVE"){if(quickMode){setResult(RESULT_CANCELED);finish()}else{save();menu()}})
        if(flipped.length()==2){val version=revision;handler.postDelayed({if(version==revision&&!isFinishing){val a=flipped.getInt(0);val b=flipped.getInt(1);val right=cards.getString(a)==cards.getString(b);if(right){matched.put(a).put(b);state.put("score",state.optInt("score")+10)};state.put("matched",matched).put("flipped",JSONArray());save();render();if(right)celebrate()}},1100)}
    }
    private fun results(){if(quickMode){shell("SFIDA U PËRFUNDUA");add(label("★ ${state.optInt("score")} pikë praktike",24));add(label("Pikët e kësaj sfide ruhen veçmas nga renditja LIVE.",14));add(button("VAZHDO ME PYETJET"){setResult(RESULT_OK);finish()});return};shell("SFIDA U PËRFUNDUA");val score=state.optInt("score");prefs.edit().putInt("best",maxOf(score,prefs.getInt("best",0))).remove("round_$mode").apply();add(label("★ $score PIKË",32));add(label("Vazhdo të mësosh me një sfidë tjetër.",18));add(button("LUAJ PËRSËRI"){val current=state;val currentMode=mode;val next={if(state===current&&mode==currentMode&&!isFinishing){newRound();render()}};if(!ads.naturalBreak(next))next()});add(button("ZGJIDH NJË LOJË TJETËR"){menu()})}
    override fun onSaveInstanceState(out:Bundle){out.putBoolean("quickMode",quickMode);out.putString("mode",mode);out.putString("state",state.toString());super.onSaveInstanceState(out)}
    override fun onResume(){super.onResume();ads.resume();if(mode=="memory")render()}
    override fun onPause(){save();revision++;applause?.release();applause=null;ads.pause();super.onPause()}
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);applause?.release();ads.destroy();super.onDestroy()}
}
