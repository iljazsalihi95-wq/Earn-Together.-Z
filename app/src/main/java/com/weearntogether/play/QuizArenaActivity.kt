package com.weearntogether.play

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.media.ToneGenerator
import android.media.AudioManager
import java.util.Locale

/** Native arena: animated stage, accessible controls and pause-safe quiz rounds. */
class QuizArenaActivity : Activity(), TextToSpeech.OnInitListener {
    data class Question(val category:String,val prompt:String,val choices:List<String>,val correct:Int,val why:String,val id:String="",val source:String="")
    private lateinit var ads:NativeGameAds
    private var bank=listOf<Question>()
    private lateinit var bankStore:QuizBank
    private var bankLive=false;private var bankTotal=0
    private var playerName="";private var playerInput:EditText?=null
    private var remoteRound="";private var answers=org.json.JSONArray();private var resultsStatus:TextView?=null
    private var startedAt=0L;private var tickSecond=-1
    private var applause:android.media.MediaPlayer?=null;private var voiceReady=false
    private val accounts by lazy { AccountStore(this) }
    private var session = listOf<Question>()
    private var index=0; private var score=0; private var correctCount=0
    private var remaining=60000L; private var selected=-1
    private var removed=setOf<Int>(); private var helpUsed=false; private var playing=false
    private var timer:CountDownTimer?=null; private var tts:TextToSpeech?=null
    private var sound:ToneGenerator?=null; private var audioEnabled=true; private var resumed=false
    private val prefs by lazy { getSharedPreferences("quiz_arena",MODE_PRIVATE) }
    private val gold=0xffffce69.toInt(); private val white=Color.WHITE
    private lateinit var root:LinearLayout; private lateinit var hud:TextView
    private lateinit var clock:ProgressBar; private lateinit var scene:QuizArenaScene
    private lateinit var help:Button; private var buttons=listOf<Button>()
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun bg(a:Int,b:Int=a)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(a,b)).apply { cornerRadius=dp(20).toFloat();setStroke(dp(1),0x5576bfff) }
    private fun label(s:String,size:Int=18,color:Int=white)=TextView(this).apply { text=s;textSize=size.toFloat();setTextColor(color);setTypeface(null,Typeface.BOLD);setPadding(dp(6),dp(8),dp(6),dp(8)) }
    private fun button(s:String,action:()->Unit)=Button(this).apply { text=s;textSize=16f;isAllCaps=false;setTextColor(white);background=bg(0xff224e99.toInt(),0xff353075.toInt());setPadding(dp(16),dp(12),dp(16),dp(12));minHeight=dp(54);setOnClickListener { performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);action() } }
    private fun add(v:View){root.addView(v,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })}
    private fun shell(){val scroll=ScrollView(this).apply { isFillViewport=true;setBackgroundColor(0xff080f29.toInt()) };root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(12),dp(18),dp(24));background=bg(0xff0b1734.toInt(),0xff29184c.toInt()) };scroll.setOnApplyWindowInsetsListener { v,insets->v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom);insets };scroll.addView(root);setContentView(scroll)
        val top=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL };top.addView(button("‹"){ if(playing)confirmExit() else finish() },LinearLayout.LayoutParams(dp(52),dp(52)));top.addView(label("QUIZ ARENA",24,gold),LinearLayout.LayoutParams(0,-2,1f));top.addView(button(if(audioEnabled)"♪" else "×"){audioEnabled=!audioEnabled;prefs.edit().putBoolean("audio",audioEnabled).apply();if(!audioEnabled)tts?.stop();if(playing)renderQuestion() else menu()},LinearLayout.LayoutParams(dp(52),dp(52)));add(top)
    }
    override fun onCreate(state:Bundle?){super.onCreate(state);ads=NativeGameAds(this,"quiz_arena");bankStore=QuizBank(this);val initial=bankStore.cached();bank=initial.questions;bankTotal=initial.total;playerName=prefs.getString("playerName",accounts.currentName()?:"")?:"";window.statusBarColor=0xff080f29.toInt();window.navigationBarColor=0xff080f29.toInt();audioEnabled=prefs.getBoolean("audio",true);sound=runCatching { ToneGenerator(AudioManager.STREAM_MUSIC,55) }.getOrNull();tts=TextToSpeech(this,this)
        if(state!=null&&state.getBoolean("playing")){remoteRound=state.getString("remoteRound","");answers=org.json.JSONArray(state.getString("answers","[]"));playerName=state.getString("playerName",playerName);startedAt=state.getLong("startedAt",System.currentTimeMillis());val ids=state.getIntArray("session")?:intArrayOf();session=ids.toList().mapNotNull { bank.getOrNull(it) };index=state.getInt("index");score=state.getInt("score");correctCount=state.getInt("correctCount");remaining=state.getLong("remaining",60000L);selected=state.getInt("selected",-1);helpUsed=state.getBoolean("helpUsed");removed=(state.getIntArray("removed")?:intArrayOf()).toSet();playing=session.isNotEmpty()&&index in session.indices}
        if(playing)renderQuestion() else menu();if(!(BuildConfig.DEBUG&&intent.getBooleanExtra("native_qa",false)))refreshBank()
    }
    private fun menu(){playing=false;timer?.cancel();shell();add(label("LOJA JONË • WE EARN TOGETHER",12,gold));val avatar=PlayerPortrait.image(this).apply{setOnClickListener{PlayerPortrait.pick(this@QuizArenaActivity)}};root.addView(avatar,LinearLayout.LayoutParams(dp(72),dp(72)).apply{gravity=Gravity.CENTER_HORIZONTAL});add(button("ZGJIDH FOTON TËNDE"){PlayerPortrait.pick(this)});playerInput=EditText(this).apply{hint="Shkruaj emrin tënd";setText(playerName);setTextColor(white);setHintTextColor(0xffc7d8ff.toInt());setSingleLine(true);filters=arrayOf(android.text.InputFilter.LengthFilter(80));setPadding(dp(14),dp(12),dp(14),dp(12));background=bg(0xff1b2b58.toInt())};add(playerInput!!);add(label(if(bankLive)"SHEET LIVE • ${bank.size} pyetje për lojë / $bankTotal në bankë" else "KOPJE OFFLINE • ${bank.size} pyetje • rifresko për LIVE",12,gold));add(button("RIFRESKO PYETJET NGA SHEET-I"){refreshBank()});scene=QuizArenaScene(this);addScene();add(label("Hyr në arenën e diturisë",27));add(label("60 sekonda për pyetje. Zgjidh fushën, përdor ndihmën 50/50 dhe mund rekordin tënd.",15,0xffc7d8ff.toInt()));add(label("REKORDI  ★ ${prefs.getInt("best",0)}",18,gold))
        (listOf("TË GJITHA")+bank.map { it.category }.distinct()).forEach { category->val count=if(category=="TË GJITHA")bank.size else bank.count { it.category==category };add(button("$category   •   $count pyetje   ›"){begin(category)}) }
        add(label("Pyetjet: shqip • Pikët janë rezultat loje.",12,0xffb7c7e9.toInt()));ads.banner(root)
    }
    private fun begin(category:String){
        playerName=playerInput?.text?.toString()?.trim()?:playerName
        if(playerName.length<2){playerInput?.error="Shkruaj emrin tënd";return}
        prefs.edit().putString("playerName",playerName).apply()
        session=(if(category=="TË GJITHA")bank else bank.filter{it.category==category}).shuffled().take(20)
        if(session.isEmpty())return
        remoteRound="";answers=org.json.JSONArray()
        if(!accounts.isSignedIn()||(BuildConfig.DEBUG&&intent.getBooleanExtra("native_qa",false))){beginRound();return}
        shell();add(label("Duke hapur sfidën…",24));add(label("Po verifikohet lidhja me rezultatet e Sheet-it.",15))
        AuthWorker.executor.execute{val response=runCatching{BackendClient().request("quizArenaStart",org.json.JSONObject().put("session_token",accounts.requireToken()).put("name",playerName).put("question_ids",org.json.JSONArray(session.map{it.id})))};runOnUiThread{if(!isFinishing&&!isDestroyed){response.onSuccess{remoteRound=it.getString("round_id")}.onFailure{Toast.makeText(this,"Rezultatet LIVE nuk janë aktive. Kjo sfidë ruhet lokalisht.",Toast.LENGTH_LONG).show()};beginRound()}}}
    }
    private fun beginRound(){index=0;score=0;correctCount=0;helpUsed=false;startedAt=System.currentTimeMillis();playing=true;nextQuestion()}
    private fun addScene(){root.addView(scene,LinearLayout.LayoutParams(-1,dp(156)).apply { bottomMargin=dp(14) })}
    private fun nextQuestion(){remaining=60000L;selected=-1;removed=emptySet();renderQuestion();startTimer();speakCurrent()}
    private fun renderQuestion(){shell();hud=label("",15,gold);add(hud);clock=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=60000;progressTintList=android.content.res.ColorStateList.valueOf(gold) };add(clock);scene=QuizArenaScene(this);scene.playerName=playerName;scene.portrait=PlayerPortrait.load(this);scene.reaction=if(selected==-1)0 else if(selected==session[index].correct)1 else -1;addScene();val q=session[index];add(label(q.category,12,gold));add(label(q.prompt,20));buttons=q.choices.mapIndexed { i,choice->button("${'A'+i}   $choice"){answer(i)}.apply { gravity=Gravity.START or Gravity.CENTER_VERTICAL;isEnabled=selected==-1&&i !in removed;alpha=if(i in removed).22f else 1f;if(selected!=-1&&i==q.correct)background=bg(0xff087c65.toInt(),0xff155b4c.toInt()) else if(selected==i)background=bg(0xffa73862.toInt(),0xff66233f.toInt()) }.also { add(it) } }
        if(selected!=-1){add(label(if(selected==q.correct)"Saktë! +${100+(remaining/1000).toInt()} pikë" else if(selected==99)"Koha përfundoi" else "Përgjigjja e saktë: ${'A'+q.correct}",18,gold));add(label(q.why,16));if(q.source.isNotBlank())add(label("Burimi: ${q.source}",12,0xffb7c7e9.toInt()));add(button(if(index==session.lastIndex)"SHIKO REZULTATIN  ›" else "PYETJA TJETËR  ›"){if(index==session.lastIndex)results() else {index++;nextQuestion()}})}
        else {help=button(if(helpUsed)"50/50 u përdor" else "50 / 50  •  Hiq dy përgjigje"){if(!helpUsed){helpUsed=true;removed=q.choices.indices.filter { it!=q.correct }.shuffled().take(minOf(2,q.choices.size-2)).toSet();renderQuestion()}};help.isEnabled=!helpUsed&&q.choices.size>=3;add(help);add(button("LEXO PYETJEN ME ZË"){speakCurrent()});if(helpUsed)add(button("VIDEO • RIFITO NDIHMËN 50/50"){timer?.cancel();if(!ads.rewarded(onGameplayReward={helpUsed=false;renderQuestion()},onFinished={startTimer()}))startTimer()})};updateClock()
    }
    private fun updateClock(){if(!::hud.isInitialized)return;hud.text="$playerName • ${index+1}/${session.size}     ★ $score     ${(remaining+999)/1000}s";clock.progress=remaining.toInt()}
    private fun startTimer(){timer?.cancel();if(!resumed||!playing||selected!=-1)return;timer=object:CountDownTimer(remaining,100){override fun onTick(ms:Long){remaining=ms;val sec=(ms/1000).toInt();if(sec!=tickSecond){tickSecond=sec;if(sec in 0..9&&audioEnabled)sound?.startTone(ToneGenerator.TONE_PROP_BEEP,65)};updateClock()};override fun onFinish(){remaining=0;answer(99)}}.start()}
    private fun answer(n:Int){if(!playing||selected!=-1||n in removed)return;selected=n;answers.put(org.json.JSONObject().put("id",session[index].id).put("choice",if(n==99)-1 else n));timer?.cancel();tts?.stop();val right=n==session[index].correct;if(right){score+=100+(remaining/1000).toInt();correctCount++};if(audioEnabled){if(right)playApplause() else sound?.startTone(ToneGenerator.TONE_PROP_NACK,180)};renderQuestion()}
    private fun results(){playing=false;timer?.cancel();tts?.stop();val resultId=queueResult();val best=maxOf(score,prefs.getInt("best",0));prefs.edit().putInt("best",best).apply();shell();scene=QuizArenaScene(this).apply { reaction=1 };addScene();add(label("$playerName • SFIDA U PËRFUNDUA",24,gold));add(label("★ $score PIKË",34));add(label("$correctCount / ${session.size} përgjigje të sakta",21));add(label("Rekordi yt: $best pikë",18,gold));resultsStatus=label(if(remoteRound.isEmpty())"Rezultat lokal • serveri i rezultateve ende nuk është aktiv." else "Duke ruajtur rezultatin në Sheet…",13);add(resultsStatus!!);if(remoteRound.isNotEmpty()){submitResult(resultId);add(button("RIPROVO RUAJTJEN NË SHEET"){submitResult(resultId)})};add(button("LUAJ PËRSËRI"){menu()});add(button("KTHEHU NË BALLINË"){finish()});ads.banner(root);ads.naturalBreak()}
    override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS){voiceReady=(tts?.isLanguageAvailable(Locale("sq"))?:-1)>=android.speech.tts.TextToSpeech.LANG_AVAILABLE;if(voiceReady)tts?.language=Locale("sq");tts?.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){override fun onStart(id:String?){runOnUiThread{if(::scene.isInitialized)scene.speaking=true}};override fun onDone(id:String?){runOnUiThread{if(::scene.isInitialized)scene.speaking=false}};@Deprecated("TTS") override fun onError(id:String?){onDone(id)}})}}
    private fun speakCurrent(){if(!audioEnabled)return;if(!voiceReady){Toast.makeText(this,"Zëri shqip nuk është i instaluar në këtë telefon.",Toast.LENGTH_SHORT).show();return};if(!audioEnabled||!playing||selected!=-1)return;val q=session[index];tts?.speak(q.prompt+". "+q.choices.mapIndexed { i,s->"${'A'+i}. $s" }.joinToString(". "),TextToSpeech.QUEUE_FLUSH,null,"arena")}
    private fun refreshBank(){playerName=playerInput?.text?.toString()?.trim()?:playerName;CloudWorker.executor.execute {val result=runCatching{bankStore.refresh()};runOnUiThread{if(!isFinishing&&!isDestroyed&&!playing){result.onSuccess{bank=it.questions;bankLive=it.live;bankTotal=it.total;menu()}.onFailure{Toast.makeText(this,"S’ka lidhje LIVE. Kopja e ruajtur mbetet në dispozicion.",Toast.LENGTH_LONG).show()}}}}}
    private fun playApplause(){applause?.release();applause=android.media.MediaPlayer.create(this,R.raw.arena_applause);applause?.setVolume(.65f,.65f);applause?.start()}
    private fun queueResult():String{val id=java.util.UUID.randomUUID().toString();val result=org.json.JSONObject().put("id",id).put("round_id",remoteRound).put("user_id",accounts.currentUserId()).put("name",playerName).put("answers",answers).put("correct",correctCount).put("total",session.size).put("score",score).put("seconds",(System.currentTimeMillis()-startedAt)/1000).put("status",if(remoteRound.isEmpty())"LOCAL_ONLY" else "PENDING");val queue=org.json.JSONArray(prefs.getString("results","[]"));queue.put(result);prefs.edit().putString("results",queue.toString()).commit();return id}
    private fun submitResult(id:String){CloudWorker.executor.execute{val rows=org.json.JSONArray(prefs.getString("results","[]"));val result=(0 until rows.length()).map{rows.getJSONObject(it)}.firstOrNull{it.optString("id")==id}?:return@execute
        if(!accounts.isSignedIn()||result.optString("user_id")!=accounts.currentUserId())return@execute
        val response=runCatching{BackendClient().request("quizArenaFinish",org.json.JSONObject().put("session_token",accounts.requireToken()).put("round_id",result.getString("round_id")).put("answers",result.getJSONArray("answers")))}
        response.onSuccess{if(it.optBoolean("saved")){result.put("status","SAVED");prefs.edit().putString("results",rows.toString()).commit()}}
        runOnUiThread{if(!isFinishing&&!isDestroyed&&!playing)resultsStatus?.text=if(result.optString("status")=="SAVED")"✓ Rezultati u ruajt në Sheet me emrin $playerName." else "Nuk u konfirmua ruajtja. Kopja lokale mbetet e sigurt."}
    }}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:android.content.Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PlayerPortrait.REQUEST&&PlayerPortrait.receive(this,resultCode,data)){if(playing)renderQuestion() else menu()}}
    private fun confirmExit(){timer?.cancel();tts?.stop();android.app.AlertDialog.Builder(this).setTitle("Dil nga sfida?").setMessage("Rezultati i kësaj sfide nuk ka përfunduar.").setNegativeButton("VAZHDO"){_,_->startTimer()}.setPositiveButton("DIL"){_,_->menu()}.setOnCancelListener { startTimer() }.show()}
    @Deprecated("Activity back navigation") override fun onBackPressed(){if(playing)confirmExit() else super.onBackPressed()}
    override fun onResume(){super.onResume();resumed=true;if(::ads.isInitialized)ads.resume();startTimer()}
    override fun onPause(){resumed=false;if(::ads.isInitialized)ads.pause();timer?.cancel();tts?.stop();applause?.pause();if(::scene.isInitialized)scene.speaking=false;super.onPause()}
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putString("remoteRound",remoteRound);out.putString("answers",answers.toString());out.putString("playerName",playerName);out.putLong("startedAt",startedAt);out.putBoolean("playing",playing);out.putIntArray("session",session.map { bank.indexOf(it) }.toIntArray());out.putInt("index",index);out.putInt("score",score);out.putInt("correctCount",correctCount);out.putLong("remaining",remaining);out.putInt("selected",selected);out.putBoolean("helpUsed",helpUsed);out.putIntArray("removed",removed.toIntArray())}
    override fun onDestroy(){timer?.cancel();tts?.stop();tts?.shutdown();sound?.release();applause?.release();if(::ads.isInitialized)ads.destroy();super.onDestroy()}
}
