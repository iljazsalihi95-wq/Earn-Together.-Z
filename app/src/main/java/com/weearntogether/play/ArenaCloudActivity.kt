package com.weearntogether.play

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray

/** Competitive arena: identity, answers, timing and points are authoritative on the server. */
class ArenaCloudActivity:Activity(){
    internal companion object { var qaTransport:((String,JSONObject)->JSONObject)?=null }
    private val localStore by lazy{ArenaOfflineStore(this,accounts.currentUserId()?:"guest")}
    private var offline=false;private var modeVersion=0;private var tts:android.speech.tts.TextToSpeech?=null;private var ttsReady=false
    private val network=java.util.concurrent.Executors.newSingleThreadExecutor()
    private val voiceNetwork=java.util.concurrent.Executors.newSingleThreadExecutor()
    private val retryHandler=android.os.Handler(android.os.Looper.getMainLooper())
    private val retry=object:Runnable{override fun run(){if(resumed&&!live&&!busy)refresh();retryHandler.postDelayed(this,30000)}}
    private fun qa()=BuildConfig.DEBUG&&intent.getBooleanExtra("native_qa",false)
    private fun isOfflineSession()=session?.optBoolean("offline")==true
    private fun localStatus()="LIVE përkohësisht offline • Pyetje lokale të gatshme: ${localStore.questions.size} • USHTRIM OFFLINE"
    private fun startOffline(resume:Boolean=false){modeVersion++;offline=true;timer?.cancel();stopVoice();val s=if(resume)localStore.saved() else null;accept(s?:localStore.start(category.takeIf{it in localStore.categories()}?:""));status.text="USHTRIM OFFLINE • Shqip • pikë vetëm lokale"}
    private fun persistLocal(){val s=session?:return;if(isOfflineSession()){if(s.optJSONObject("feedback")==null&&!s.optBoolean("presentation_pending"))s.put("remaining_ms",(deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0));localStore.save(s)}}

    private val accounts by lazy{AccountStore(this)}
    private val prefs by lazy{getSharedPreferences("arena_cloud_"+(accounts.currentUserId()?:"guest"),MODE_PRIVATE)}
    private val client=BackendClient(BuildConfig.QUIZ_ARENA_WORKER_URL.trimEnd('/')+"/api/arena",30_000)
    private val voiceClient=BackendClient("https://script.google.com/macros/s/AKfycbzu9rso0BO_UhLBPi3TMqIQl44tZ12r0n-t143ydmWOoiVMPbm_YA1uF0rImIfEcvaERw/exec",60_000)
    private var locale="sq";private var category="";private var session:JSONObject?=null;private var bootstrap:JSONObject?=null
    private var busy=false;private var live=false;private var resumed=false;private var audioEnabled=true
    private var deadline=0L;private var tick=-1;private var timer:android.os.CountDownTimer?=null
    private var player:android.media.MediaPlayer?=null;private var sound:android.media.ToneGenerator?=null
    private var applause:android.media.MediaPlayer?=null;private var voiceGeneration=0
    private val answerButtons=mutableListOf<Button>()
    private var narrationKey=""
    private var speechEnvelope:ArenaSpeechEnvelope?=null
    private var friendText=""
    private var dialogueText="";private var dialogueMessage=""
    private var voiceCompletion:(()->Unit)?=null
    private var phoneDialog:android.app.AlertDialog?=null
    private var phoneGeneration=0
    private fun outfit(){
        val items=arrayOf("Kostum blu", "Kostum vjollcë", "Kostum i zi", "Këmishë e bardhë", "Këmishë blu", "Kravatë ari", "Kravatë e kuqe", "Avatar me iniciale")
        android.app.AlertDialog.Builder(this).setTitle("PARAQITJA E LOJTARIT").setItems(items){_,which->
            val edit=prefs.edit()
            when(which){0->edit.putInt("suit",0xff273b68.toInt());1->edit.putInt("suit",0xff583a79.toInt());2->edit.putInt("suit",0xff202433.toInt());3->edit.putInt("shirt",Color.WHITE);4->edit.putInt("shirt",0xffb6d8ff.toInt());5->edit.putInt("tie",0xffc99b4e.toInt());6->edit.putInt("tie",0xffbd3455.toInt());7->edit.putBoolean("initial_avatar",true)}
            edit.apply();home()
        }.setNegativeButton("MBYLL",null).show()
    }
    private var learningDialog:android.app.Dialog?=null
    private var learningShown=""
    private var advancing=false
    private fun questionKey(s:JSONObject?=session)=s?.let{it.optString("session_id")+":"+it.optJSONObject("question")?.optString("id")}.orEmpty()
    private lateinit var root:LinearLayout;private lateinit var status:TextView;private var scene:QuizArenaScene?=null
    private lateinit var ads:NativeGameAds
    private val gold=0xffffd782.toInt();private val blue=0xff64c8ff.toInt()
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun label(t:String,size:Int=16,color:Int=Color.WHITE)=TextView(this).apply{text=t;textSize=size.toFloat();setTextColor(color);setPadding(dp(6),dp(if(session!=null)4 else 7),dp(6),dp(if(session!=null)4 else 7));typeface=Typeface.DEFAULT_BOLD}
    private fun gradient(a:Int,b:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(a,b)).apply{cornerRadius=dp(20).toFloat();setStroke(dp(1),0x4468c5ff)}
    private fun add(v:View,h:Int=-2){root.addView(v,LinearLayout.LayoutParams(-1,if(h<0)h else dp(h)).apply{bottomMargin=dp(if(session!=null)6 else 12)})}
    private fun button(t:String,action:()->Unit)=Button(this).apply{text=t;isAllCaps=false;textSize=if(session!=null)15f else 16f;setTextColor(Color.WHITE);background=gradient(0xff1b62af.toInt(),0xff4e35a0.toInt());minHeight=dp(if(session!=null)46 else 54);setPadding(dp(12),dp(8),dp(12),dp(8));setOnClickListener{run{performHapticFeedback(1);animate().scaleX(.97f).scaleY(.97f).setDuration(70).withEndAction{animate().scaleX(1f).scaleY(1f).setDuration(100).start()}.start();action()}}}
    private fun shell(){
        learningDialog?.dismiss();val round=session!=null
        val scroll=ScrollView(this).apply{isFillViewport=true;setBackgroundColor(0xff080d27.toInt());setOnApplyWindowInsetsListener{v,i->v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom);i}}
        root=LinearLayout(this).apply{orientation=1;setPadding(dp(if(round)12 else 18),dp(if(round)6 else 14),dp(if(round)12 else 18),dp(20));background=gradient(0xff0b1734.toInt(),0xff261544.toInt())}
        scroll.addView(root);setContentView(scroll)
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val control=dp(if(round)38 else 48)
        row.addView(button("‹"){if(session!=null){modeVersion++;home()} else finish()},LinearLayout.LayoutParams(control,control))
        row.addView(label(if(round)"QUIZ ARENA PREMIUM" else "QUIZ ARENA\nPREMIUM",if(round)14 else 19,gold),LinearLayout.LayoutParams(0,-2,1f))
        row.addView(button(if(audioEnabled)"♫" else "×"){audioEnabled=!audioEnabled;prefs.edit().putBoolean("audio",audioEnabled).apply();stopVoice();if(session!=null)render() else home()},LinearLayout.LayoutParams(control,control))
        add(row);if(!round)add(label("TESTIM DHE MËSIM",14,blue))
        status=label(if(isOfflineSession())"USHTRIM OFFLINE • Shqip • pikë vetëm lokale" else if(live)"● ARENA LIVE" else localStatus(),11,blue);add(status)
    }
    override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=0xff080d27.toInt();window.navigationBarColor=0xff080d27.toInt();locale=prefs.getString("locale","sq")?:"sq";audioEnabled=prefs.getBoolean("audio",true);ads=NativeGameAds(this,"quiz_arena");sound=runCatching{android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC,90)}.getOrNull();bootstrap=runCatching{JSONObject(prefs.getString("bootstrap","")?:"")}.getOrNull();tts=android.speech.tts.TextToSpeech(this){result->ttsReady=result==android.speech.tts.TextToSpeech.SUCCESS};home();localStore.saved()?.let{offline=true;accept(it)};if(!qa()||qaTransport!=null)refresh()}
    private fun call(action:String,data:JSONObject=JSONObject(),done:(JSONObject)->Unit){
        if(busy){android.widget.Toast.makeText(this,"Kërkesa e mëparshme është ende në proces.",android.widget.Toast.LENGTH_SHORT).show();return}
        if(!accounts.isSignedIn()&&!(qa()&&qaTransport!=null)){live=false;android.widget.Toast.makeText(this,"Sesioni LIVE kërkon hyrje në llogari. Hape profilin ose luaj OFFLINE.",android.widget.Toast.LENGTH_LONG).show();if(session==null)home();return}
        busy=true;val version=modeVersion;status.text=when(action){"arenaReady"->"Prezantuesi përfundoi • Po nis koha…";"arenaAnswer"->"Po kontrollohet zgjedhja jote…";"arenaNext"->"Po hapet pyetja tjetër…";else->"Po provohet LIVE… Mund të luash OFFLINE."}
        val payload=JSONObject(data.toString()).put("locale",locale).put("protocol",2)
        if(accounts.isSignedIn())payload.put("session_token",accounts.requireToken())
        network.execute{val result=runCatching{if(qa()&&qaTransport!=null)qaTransport!!.invoke(action,payload) else client.request(action,payload)}
            runOnUiThread{busy=false;if(isFinishing||isDestroyed||version!=modeVersion)return@runOnUiThread
                result.onSuccess{live=true;done(it)}.onFailure{error->if(action=="arenaLifeline"&&error is BackendException&&error.code !in setOf("NETWORK","TIMEOUT")){status.text=if(error.code=="INVALID_ACTION")"Serveri aktiv duhet përditësuar për ndihmat LIVE. Progresi u ruajt." else "Ndihma nuk është e disponueshme: ${error.code}";return@onFailure};live=false;if(session==null)home() else {status.text=if(isOfflineSession())"USHTRIM OFFLINE • Shqip" else "LIVE nuk përgjigjet. Sesioni LIVE mbetet i ruajtur.";add(button("RIPROVO / VAZHDO LIVE"){call(action,data,done)});add(button("VAZHDO NË USHTRIM OFFLINE"){startOffline(localStore.saved()!=null)})}}
            }
        }
    }
    private fun refresh(){call("arenaBootstrap"){if(it.optInt("available",0)<=0||it.optJSONArray("categories")==null){live=false;if(session==null)home();return@call};bootstrap=it;prefs.edit().putString("bootstrap",it.toString()).apply();if(session==null)home() else if(isOfflineSession())status.text="USHTRIM OFFLINE • LIVE u rikthye; hapet nga ballina pa humbur progresin lokal."}}
    private fun home(){phoneGeneration++;phoneDialog?.dismiss();phoneDialog=null;sound?.stopTone();persistLocal();timer?.cancel();stopVoice();session=null;offline=!live;shell();val hero=FrameLayout(this);val art=StudioArtwork(this,"arena");hero.addView(art,FrameLayout.LayoutParams(-1,-1));val overlay=LinearLayout(this).apply{orientation=1;gravity=Gravity.BOTTOM;setPadding(dp(18),dp(12),dp(18),dp(14));background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x00000000,0xee101635.toInt()))};overlay.addView(label("Hyr në skenën e diturisë",25,gold));overlay.addView(label("Mëso pas çdo përgjigjeje.\nPërparo, sfido veten, ndriço arenën.",14));hero.addView(overlay,FrameLayout.LayoutParams(-1,-1));add(hero,208);hero.alpha=0f;hero.animate().alpha(1f).setDuration(450).start()
        val profile=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};profile.addView(PlayerPortrait.image(this).apply{setOnClickListener{PlayerPortrait.pick(this@ArenaCloudActivity)}},LinearLayout.LayoutParams(dp(68),dp(68)));val name=EditText(this).apply{hint="Emri i lojtarit";setText(prefs.getString("name",accounts.currentName()?:"Lojtari"));setTextColor(Color.WHITE);setHintTextColor(0xffb5c5e6.toInt());setSingleLine();filters=arrayOf(android.text.InputFilter.LengthFilter(80))};profile.addView(name,LinearLayout.LayoutParams(0,-2,1f));add(profile);add(button("ZGJIDH AVATARIN / FOTON"){PlayerPortrait.pick(this)});add(button("KOSTUMI • KËMISHA • KRAVATA"){outfit()})
        val languages=arrayOf("Shqip", "Deutsch • vetëm pyetjet", "English • vetëm pyetjet", "Türkçe • vetëm pyetjet");val codes=arrayOf("sq","de","en","tr");val spinner=Spinner(this).apply{adapter=ArrayAdapter(this@ArenaCloudActivity,android.R.layout.simple_spinner_dropdown_item,languages);setSelection(codes.indexOf(locale).coerceAtLeast(0));background=gradient(0xffc5e2ff.toInt(),0xffaabfff.toInt())};var initial=true;spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,position:Int,id:Long){if(initial){initial=false;return};locale=codes[position];prefs.edit().putString("locale",locale).apply();category="";refresh()}};add(spinner,48)
        add(button("RUAJ LOJTARIN"){val n=name.text.toString().trim();if(n.length<2){name.error="Të paktën 2 shkronja";return@button};prefs.edit().putString("name",n).apply();if(live)call("arenaRegister",JSONObject().put("name",n)){refresh()} else {status.text="Emri u ruajt në telefon • USHTRIM OFFLINE"}})
        val progress=if(live)bootstrap?.optJSONObject("progress") else localStore.progress();val level=progress?.optInt("level",1)?:1;add(label("NIVELI $level  •  ★ ${progress?.optInt("points",0)?:0} PIKË",20,gold));val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=20;this.progress=if(live)progress?.optJSONArray("completed")?.length()?:0 else (level-1).coerceAtLeast(0);progressTintList=android.content.res.ColorStateList.valueOf(blue)};add(bar)
        val saved=bootstrap?.optString("resume","").orEmpty().takeUnless{it=="null"}.orEmpty().ifEmpty{prefs.getString("active_id","")?:""};if(saved.isNotEmpty())add(button("▶ VAZHDO LOJËN E RUAJTUR"){call("arenaResume",JSONObject().put("session_id",saved)){accept(it);retryAnswer()}})
        if(localStore.saved()!=null)add(button("▶ VAZHDO USHTRIMIN OFFLINE"){startOffline(true)})
        val categories=if(live)bootstrap?.optJSONArray("categories")?:JSONArray() else JSONArray(localStore.categories());add(label("KATEGORITË",14,blue));val options=mutableListOf("Të gjitha");for(i in 0 until categories.length())options.add(categories.getString(i));val cats=Spinner(this).apply{adapter=ArrayAdapter(this@ArenaCloudActivity,android.R.layout.simple_spinner_dropdown_item,options);background=gradient(0xffc5e2ff.toInt(),0xffaabfff.toInt())};cats.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){category=if(pos==0)"" else options[pos]}};add(cats,48)
        add(label(if(live)"${bootstrap?.optInt("available",0)?:0} pyetje LIVE në gjuhën e zgjedhur" else "Pyetje lokale të gatshme: ${localStore.questions.size} • Shqip",12));add(button(if(live)"HYR NË ARENË • NIVELI $level" else "HYR NË USHTRIM OFFLINE • NIVELI $level"){val n=name.text.toString().trim();if(n.length<2){name.error="Shkruaj emrin";return@button};prefs.edit().putString("name",n).apply();if(!live){startOffline();return@button};call("arenaRegister",JSONObject().put("name",n)){call("arenaStart",JSONObject().put("level",level).put("category",category)){accept(it)}}})
        add(button("🏆 RENDITJA E LOJTARËVE"){if(!live){status.text="Renditja kërkon LIVE. Rekordi lokal: ${localStore.progress().optInt("best")} pikë.";return@button};call("arenaLeaderboard"){shell();add(label("RENDІTJA",25,gold));val rows=it.getJSONArray("ranking");if(rows.length()==0)add(label("Bëhu lojtari i parë që përfundon një nivel."));for(i in 0 until rows.length()){val r=rows.getJSONObject(i);add(label("${r.getInt("rank")}. ${r.getString("name")}   ★ ${r.getInt("points")}   • N${r.getInt("level")}"))};add(button("KTHEHU"){home()})}})
        add(button("RIPROVO LIDHJEN LIVE"){refresh()});add(button("USHTRIM OFFLINE • PA RENDITJE"){startOffline(localStore.saved()!=null)});add(label("Regjistrimet shqip përdoren kur janë të disponueshme; për pyetjet e tjera përdoret zëri AI. Recitimet kërkojnë audio autentike. Pikët e arenës janë rezultat loje.",11,0xffb9c9df.toInt()));ads.banner(root)
    }
    private fun accept(v:JSONObject){val previous=session;if(previous!=null&&questionKey(previous)!=questionKey(v)){phoneGeneration++;phoneDialog?.dismiss();phoneDialog=null;sound?.stopTone()};if(previous!=null&&questionKey(previous)==questionKey(v)&&previous.has("learning_choice"))v.put("learning_choice",previous.getInt("learning_choice"));val f=v.optJSONObject("feedback");if(f?.optString("reason")=="timeout"&&f.optInt("submitted_choice",-1)>=0)v.put("learning_choice",f.getInt("submitted_choice"));session=v;offline=v.optBoolean("offline");if(offline)locale="sq" else if(v.optString("locale") in setOf("sq","de","en","tr"))locale=v.getString("locale");if(v.optJSONObject("feedback")==null&&v.optLong("remaining_ms")<=0)v.put("timed_out",true);deadline=android.os.SystemClock.elapsedRealtime()+v.optLong("remaining_ms");if(offline)localStore.save(v) else prefs.edit().putString("session",v.toString()).putString("active_id",v.optString("session_id")).apply();render()}
    private fun render(){val s=session?:return;timer?.cancel();stopVoice();answerButtons.clear();shell();val config=s.getJSONObject("config");add(label("${if(isOfflineSession())"USHTRIM OFFLINE" else "LIVE"} • NIVELI ${s.getInt("level")}  •  ${s.getInt("index")+1}/${s.getInt("total")}  •  ★ ${s.getInt("score")}",14,gold));scene=QuizArenaScene(this).apply{playerName=prefs.getString("name","")?:"";portrait=if(prefs.getBoolean("initial_avatar",false))null else PlayerPortrait.load(this@ArenaCloudActivity);suitColor=prefs.getInt("suit",0xff273b68.toInt());shirtColor=prefs.getInt("shirt",Color.WHITE);tieColor=prefs.getInt("tie",0xffc99b4e.toInt());reaction=if(s.optJSONObject("feedback")?.optString("reason")=="timeout")0 else if(s.optJSONObject("feedback")?.optBoolean("right")==true)1 else if(s.optJSONObject("feedback")!=null)-1 else 0};add(scene!!,(resources.displayMetrics.heightPixels/resources.displayMetrics.density*.36f).toInt().coerceIn(200,270))
        if(s.optBoolean("finished")){if(!isOfflineSession())prefs.edit().remove("active_id").remove("pending_answer").remove("pending_next").apply();add(label(if(s.optBoolean("passed"))"NIVELI U KALUA!" else "PROVO PËRSËRI",27,gold));add(label("${s.getInt("correct")} / ${s.getInt("total")} të sakta • ★ ${s.getInt("score")}",21));scene?.apply{reaction=if(s.optBoolean("passed"))1 else -1;questionPrompt=if(s.optBoolean("passed"))"NIVELI U KALUA!" else "PROVO PËRSËRI";questionCategory="${s.getInt("correct")} / ${s.getInt("total")} TË SAKTA";questionNumber="NIVELI ${s.getInt("level")}";timerText="★ ${s.getInt("score")}";stageStatus="REZULTATI YT"};root.translationY=dp(22).toFloat();root.animate().translationY(0f).setDuration(550).start();add(button("VAZHDO • KAPËRCE SKENËN"){home();refresh()});val video=config.optString("video");if(video.startsWith("https://"))add(button("SHIKO VIDEON E KALIMIT"){video(video)});if(audioEnabled)voice("result");ads.naturalBreak();return}
        val q=s.getJSONObject("question");scene?.apply{questionPrompt=q.getString("prompt");questionCategory=q.getString("category");questionNumber="PYETJA ${s.getInt("index")+1} / ${s.getInt("total")}";contentDescription=questionPrompt};val feedback=s.optJSONObject("feedback");val choices=q.getJSONArray("choices");for(i in 0 until choices.length()){val option=button("${'A'+i}   ${choices.getString(i)}"){sendAnswer(i)};option.gravity=Gravity.START or Gravity.CENTER_VERTICAL;option.isEnabled=feedback==null&&!s.optBoolean("answer_pending")&&!s.optBoolean("presentation_pending");answerButtons.add(option);if(feedback!=null&&i==feedback.getInt("correct"))option.background=gradient(0xff0b9a76.toInt(),0xff146b63.toInt());if(feedback!=null&&i==feedback.optInt("choice",s.optInt("local_choice",-2))&&!feedback.getBoolean("right"))option.background=gradient(0xffa73551.toInt(),0xff682544.toInt());if(i in ArenaLifelines.hidden(s)&&feedback==null){option.isEnabled=false;option.alpha=.25f;option.text="${'A'+i}   —"};add(option)}
        if(feedback!=null){scene?.apply{timerText=if(feedback.optInt("choice",-2)==-1)"PA PËRGJIGJE" else if(feedback.getBoolean("right"))"SAKTË" else "GABIM";stageStatus="TESTIM DHE MËSIM"};add(label(if(feedback.optInt("choice",s.optInt("local_choice",-2))==-1)"KOHA PËRFUNDOI • PA PËRGJIGJE" else if(feedback.getBoolean("right"))"✓ SAKTË" else "GABIM • PËRGJIGJJA E SAKTË: ${'A'+feedback.getInt("correct")}",20,gold));val selected=feedback.optInt("choice",s.optInt("local_choice",-2));if(selected>=0)add(label("Zgjedhja jote: ${'A'+selected} • ${choices.optString(selected)}",14,blue));if(s.has("learning_choice"))add(label(lateChoiceText(s),16,blue));if(selected==-1)add(label("Përgjigjja e saktë: ${'A'+feedback.getInt("correct")} • ${choices.optString(feedback.getInt("correct"))}",16,gold));add(label(feedback.getString("explanation"),17));add(label(feedback.optString("source"),12,blue));add(button("DËGJO SHPJEGIMIN"){voice("explanation")});add(button("HAP SHPJEGIMIN • TESTIM DHE MËSIM"){showLearning(s)});add(button("VAZHDO ›"){continueAfterLearning(s)});add(button("PYET PREZANTUESEN PËR SHPJEGIMIN"){clarify(s)});if(audioEnabled)voice("explanation");if(!qa()&&learningShown!=questionKey(s)){learningShown=questionKey(s);root.post{if(session===s&&!isFinishing&&!isDestroyed)showLearning(s)}}}
        else if(s.optBoolean("presentation_pending")){beginQuestionReading(s)}
        else if(s.optBoolean("timed_out")){timeoutControls()}
        else{val clock=label("",15,gold);fun update(){val sec=((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)+999)/1000;scene?.apply{timerText="%02d:%02d".format(sec/60,sec%60);stageStatus="KOHA E PËRGJIGJES";remainingFraction=(sec.toFloat()/(config.optInt("seconds",30).coerceAtLeast(1))).coerceIn(0f,1f)};clock.text="⏱ ${((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)+999)/1000}s • ${config.getInt("points")} pikë për saktë"};update();timer=object:android.os.CountDownTimer((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1),200){override fun onTick(ms:Long){update();val sec=(ms/1000).toInt();if(sec!=tick){tick=sec;if(audioEnabled&&sec>0&&scene?.speaking!=true&&phoneDialog?.isShowing!=true)sound?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,if(sec<=10)145 else 80)}};override fun onFinish(){clock.text="Koha përfundoi • Asnjë përgjigje nuk u zgjodh";if(resumed)expireQuestion()}}.start();addLifelineControls(s);add(button("DËGJO PYETJEN"){voice("question")});val rec=q.optString("recitation");if(rec.startsWith("https://"))add(button("DËGJO RECITIMIN AUTENTIK"){playUrl(rec)});add(button("RIPROVO / SINKRONIZO"){retryAnswer()});if(audioEnabled&&narrationKey!=questionKey(s)){narrationKey=questionKey(s);voice("question")}}
    }

    private fun showLearning(s:JSONObject){
        if(learningDialog?.isShowing==true)return
        val f=s.optJSONObject("feedback")?:return
        val q=s.getJSONObject("question");val options=q.getJSONArray("choices")
        val panel=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(20),dp(20),dp(20));background=gradient(0xff11264b.toInt(),0xff321c56.toInt())}
        panel.addView(label("TESTIM DHE MËSIM",21,gold))
        panel.addView(label(q.optString("prompt"),17))
        if(s.has("learning_choice"))panel.addView(label(lateChoiceText(s),16,blue))
        panel.addView(label(if(f.optInt("choice",-1)==-1)"KOHA PËRFUNDOI • PA PËRGJIGJE" else if(f.optBoolean("right"))"✓ SAKTË" else "GABIM",18,blue))
        panel.addView(label("Përgjigjja e saktë: "+('A'+f.getInt("correct"))+" • "+options.optString(f.getInt("correct")),18,gold))
        panel.addView(label(f.optString("explanation"),17))
        val selected=f.optInt("choice",-1)
        if(selected>=0)panel.addView(label("Zgjedhja jote: ${'A'+selected} • ${options.optString(selected)}",14,blue))
        val source=f.optString("source").trim()
        if(source.isNotEmpty())panel.addView(label("BURIMI / LITERATURA\n"+source,13,blue).apply{setTextIsSelectable(true);autoLinkMask=android.text.util.Linkify.WEB_URLS;movementMethod=android.text.method.LinkMovementMethod.getInstance()})
        panel.addView(button("DËGJO SHPJEGIMIN"){voice("explanation")})
        val dialog=android.app.Dialog(this)
        panel.addView(button("E KUPTOVA • VAZHDO"){dialog.dismiss();continueAfterLearning(s)})
        panel.addView(button("KTHEHU TE PËRGJIGJJA"){dialog.dismiss()})
        dialog.setContentView(ScrollView(this).apply{addView(panel)})
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels*.94f).toInt(),-2)
        learningDialog=dialog
        dialog.setOnDismissListener{learningDialog=null}
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels*.94f).toInt(),(resources.displayMetrics.heightPixels*.85f).toInt())
    }
    private fun continueAfterLearning(s:JSONObject){
        if(advancing||session!==s||s.optJSONObject("feedback")==null)return
        stopVoice();advancing=true
        val advance={
            advancing=false
            if(!isFinishing&&!isDestroyed&&session===s){if(isOfflineSession())accept(localStore.next(s)) else nextLive(s)}
        }
        val key="ad-break:"+questionKey(s)
        val boundary=(s.optInt("index")+1)%3==0
        if(boundary&&!prefs.getBoolean(key,false)){
            prefs.edit().putBoolean(key,true).apply()
            if(runCatching{ads.naturalBreak(advance)}.getOrDefault(false))return
        }
        advance()
    }

    private fun addLifelineControls(s:JSONObject){
        val rows=listOf("fifty" to "50:50", "audience" to "PYET PUBLIKUN • SIMULIM", "friend" to "TELEFONO SHOKUN", "hint" to "ORIENTIM / NDIHMË AI", "switch" to "NDËRRO PYETJEN")
        for((kind,title) in rows)add(button(title+if(ArenaLifelines.used(s,kind))" • PËRDORUR" else " • 1 NË NIVEL"){
            if(!ArenaLifelines.used(s,kind))useLifeline(s,kind)
        }.apply{isEnabled=!ArenaLifelines.used(s,kind)&&(kind!="fifty"||s.getJSONObject("question").getJSONArray("choices").length()==4)&&(kind!="switch"||!isOfflineSession()||(s.optJSONArray("local_reserve")?.length()?:0)>0)})
        add(button("BISEDO ME PREZANTUESEN"){clarify(s)})
    }
    private fun useLifeline(s:JSONObject,kind:String){
        if(session!==s){status.text="Pyetja ka ndryshuar. Provo ndihmën te pyetja aktuale.";return}
        if(busy){android.widget.Toast.makeText(this,"Po pritet përgjigjja e serverit. Provo sërish pas pak.",android.widget.Toast.LENGTH_LONG).show();return}
        if(isOfflineSession()){
            runCatching{
                val r=ArenaLifelines.use(s,kind)
                if(kind=="friend"){s.put("remaining_ms",(deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)+90000);deadline=android.os.SystemClock.elapsedRealtime()+s.getLong("remaining_ms")}
                localStore.save(s);render();showLifeline(s,r)
            }.onFailure{status.text="Kjo ndihmë nuk është e disponueshme në këtë pyetje."}
        }else call("arenaLifeline",JSONObject().put("session_id",s.optString("session_id")).put("question_id",s.getJSONObject("question").getString("id")).put("kind",kind)){r->accept(r);showLifeline(r,r.getJSONObject("lifeline"))}
    }
    private fun showLifeline(s:JSONObject,r:JSONObject){
        when(r.getString("kind")){
            "friend"->phoneFriend(s,r)
            "audience"->{val votes=r.getJSONArray("votes");val body=(0 until votes.length()).joinToString("\n"){"${'A'+it}: ${votes.getInt(it)}%"};android.app.AlertDialog.Builder(this).setTitle("PUBLIKU • VOTIM I SIMULUAR").setMessage(body+"\n\nNuk janë vota të njerëzve realë.").setPositiveButton("VAZHDO",null).show()}
            "hint"->if(isOfflineSession())android.app.AlertDialog.Builder(this).setTitle("ORIENTIM LOKAL").setMessage(r.optString("text")).setPositiveButton("VAZHDO",null).show() else aiDialog(s,"hint","")
        }
    }
    private fun clarify(s:JSONObject){
        val input=EditText(this).apply{hint="Çfarë dëshiron të sqarosh?";filters=arrayOf(android.text.InputFilter.LengthFilter(300))}
        android.app.AlertDialog.Builder(this).setTitle("BISEDË ME PREZANTUESEN").setView(input).setMessage(if(s.optJSONObject("feedback")==null)"Para përgjigjes sqarohen vetëm rregullat; zgjidhja mbetet e fshehur." else "Sqarimi mbështetet në shpjegimin dhe burimin e pyetjes.").setPositiveButton("PYET"){_,_->aiDialog(s,"clarify",input.text.toString())}.setNegativeButton("MBYLL",null).show()
    }
    private fun aiDialog(s:JSONObject,mode:String,message:String){
        if(isOfflineSession()){
            android.app.AlertDialog.Builder(this).setTitle("PREZANTUESJA • OFFLINE").setMessage(s.optJSONObject("feedback")?.optString("explanation")?:"Zgjidh një alternativë para mbarimit të kohës. Shpjegimi hapet pas përgjigjes. Biseda AI kërkon internet dhe sesion LIVE.").setPositiveButton("KUPTOVA",null).show();return
        }
        val dialog=android.app.AlertDialog.Builder(this).setTitle("PREZANTUESJA AI").setMessage("Po përgatitet sqarimi…").setPositiveButton("MBYLL",null).create();dialog.setOnDismissListener{stopVoice()};dialog.show()
        val version=modeVersion
        voiceNetwork.execute{val r=runCatching{BackendClient(BuildConfig.QUIZ_ARENA_WORKER_URL.trimEnd('/')+"/api/ai/respond",60000).request("arenaDialogue",JSONObject().put("session_token",accounts.requireToken()).put("session_id",s.optString("session_id")).put("question_id",s.getJSONObject("question").optString("id")).put("mode",mode).put("message",message).put("locale",locale))};runOnUiThread{if(!isFinishing&&!isDestroyed&&version==modeVersion&&session===s&&dialog.isShowing){
            dialogueText=r.getOrNull()?.optString("text").orEmpty();dialogueMessage=message
            dialog.setMessage(dialogueText.ifBlank{"Shërbimi AI nuk u përgjigj. Pyetja dhe progresi mbeten të ruajtura."})
            if(dialogueText.isNotBlank()&&audioEnabled)voice("clarify")
        }}}
    }
    private fun phoneFriend(s:JSONObject,r:JSONObject){
        stopVoice();val generation=++phoneGeneration
        friendText=r.optString("text")
        val dialog=android.app.AlertDialog.Builder(this).setTitle("TELEFONO SHOKUN"+if(isOfflineSession())" • SIMULIM" else " • MENDIM AI").setMessage("☎ Po telefonohet…").setPositiveButton("MBYLL TELEFONATËN",null).create()
        phoneDialog=dialog;dialog.setOnDismissListener{if(generation==phoneGeneration){phoneGeneration++;stopVoice();sound?.stopTone();scene?.phoneState=""};phoneDialog=null};dialog.show();scene?.phoneState="PO TELEFONOHET…"
        fun dial(){
        if(generation!=phoneGeneration||session!==s||!resumed||!dialog.isShowing)return
        scene?.phoneState="PO TELEFONOHET…"
        if(audioEnabled)sound?.startTone(android.media.ToneGenerator.TONE_SUP_RINGTONE,1600)
        retryHandler.postDelayed({
            if(generation==phoneGeneration&&session===s&&resumed&&dialog.isShowing){
                sound?.stopTone();scene?.phoneState="SHOKU U PËRGJIGJ"
                dialog.setMessage("Për ${prefs.getString("name","Lojtari")}.\n\n"+if(friendText.isNotBlank())friendText else "Shoku po mendon për pyetjen… Mendimi i tij nuk është garanci.")
                if(isOfflineSession()){if(audioEnabled)voice("friend"){if(generation==phoneGeneration&&dialog.isShowing)voice("phone_outro")}}
                else voiceNetwork.execute{
                    val response=runCatching{BackendClient(BuildConfig.QUIZ_ARENA_WORKER_URL.trimEnd('/')+"/api/ai/respond",60000).request("arenaDialogue",JSONObject().put("session_token",accounts.requireToken()).put("session_id",s.optString("session_id")).put("question_id",s.getJSONObject("question").optString("id")).put("mode","friend").put("locale",locale))}
                    runOnUiThread{if(generation==phoneGeneration&&session===s&&resumed&&dialog.isShowing){
                        friendText=response.getOrNull()?.optString("text").orEmpty()
                        dialog.setMessage(if(friendText.isBlank())"Lidhja me shokun nuk u realizua. Mund të vazhdosh lojën." else friendText+"\n\nKy është mendim AI, jo garanci.")
                        if(friendText.isNotBlank()&&audioEnabled)voice("friend"){if(generation==phoneGeneration&&dialog.isShowing)voice("phone_outro")}
                    }}
                }
            }
        },1700)
        }
        if(audioEnabled)voice("phone_intro"){dial()} else dial()
    }
    private fun beginQuestionReading(s:JSONObject){
        scene?.apply{timerText="%02d:%02d".format(s.getJSONObject("config").getInt("seconds")/60,s.getJSONObject("config").getInt("seconds")%60);stageStatus="LEXIM • KOHA E NDALUR";remainingFraction=1f}
        add(button("KALO LEXIMIN • FILLO KOHËN"){stopVoice();finishQuestionReading(questionKey(s))})
        timer=object:android.os.CountDownTimer(s.optLong("presentation_remaining_ms",90000).coerceIn(1,75000),1000){override fun onTick(ms:Long){};override fun onFinish(){stopVoice();finishQuestionReading(questionKey(s))}}.start()
        narrationKey=questionKey(s);val welcome="welcome:"+s.optString("session_id");if(audioEnabled&&s.optInt("index")==0&&!prefs.getBoolean(welcome,false)){prefs.edit().putBoolean(welcome,true).apply();voice("intro"){if(session===s&&resumed)voice("question")}}else if(audioEnabled)voice("question") else finishQuestionReading(questionKey(s))
    }
    private fun finishQuestionReading(key:String){
        val s=session?:return;if(isFinishing||isDestroyed||key!=questionKey(s)||!s.optBoolean("presentation_pending")||s.optJSONObject("feedback")!=null)return
        if(!resumed)return
        if(busy){retryHandler.postDelayed({finishQuestionReading(key)},150);return}
        timer?.cancel();scene?.speaking=false
        if(isOfflineSession()){s.put("presentation_pending",false).put("remaining_ms",s.getJSONObject("config").getLong("seconds")*1000);accept(s)}
        else call("arenaReady",JSONObject().put("session_id",s.getString("session_id")).put("question_id",s.getJSONObject("question").getString("id"))){accept(it)}
    }
    private fun lateChoiceText(s:JSONObject):String{
        val choice=s.getInt("learning_choice")
        val q=s.getJSONObject("question")
        return "Zgjedhja jote pas kohës: ${'A'+choice} • ${q.getJSONArray("choices").optString(choice)} • VETËM PËR MËSIM, PA PIKË"
    }
    private fun timeoutControls(){scene?.apply{timerText="00:00";stageStatus="KOHA PËRFUNDOI";remainingFraction=0f};status.text="Koha përfundoi • Nuk është zgjedhur përgjigje";add(label("Koha përfundoi. Mund të zgjedhësh një alternativë për të mësuar, pa pikë, ose të hapësh shpjegimin.",16,gold));add(button("SHFAQ PËRGJIGJEN DHE SHPJEGIMIN"){sendAnswer(-1)})}
    private fun expireQuestion(){val s=session?:return;if(s.optJSONObject("feedback")!=null||s.optBoolean("answer_pending")||s.optBoolean("timed_out"))return;s.put("timed_out",true).put("remaining_ms",0);timer?.cancel();answerButtons.forEachIndexed{i,b->b.isEnabled=i !in ArenaLifelines.hidden(s)};if(isOfflineSession())localStore.save(s) else prefs.edit().putString("session",s.toString()).apply();timeoutControls()}
    private fun sendAnswer(choice:Int){val s=session?:return;val question=s.optJSONObject("question")?:return;val choices=question.optJSONArray("choices")?:return;if(choice < -1 || choice >= choices.length())return;if(choice == -1 && !s.optBoolean("timed_out"))return;if(s.optBoolean("presentation_pending"))return;if(s.optJSONObject("feedback")!=null||s.optBoolean("answer_pending"))return;if(busy){status.text="Po përfundon kërkesa LIVE. Provoje përsëri.";return};val late=choice>=0&&(s.optBoolean("timed_out")||android.os.SystemClock.elapsedRealtime()>=deadline);if(late){s.put("timed_out",true).put("remaining_ms",0).put("learning_choice",choice)};val submittedChoice=if(late)-1 else choice;timer?.cancel();stopVoice();s.put("answer_pending",true).put("local_choice",choice);answerButtons.forEach{it.isEnabled=false};if(isOfflineSession()){s.remove("answer_pending");timer?.cancel();persistLocal();val answered=localStore.answer(s,submittedChoice);answered.remove("answer_pending");if(audioEnabled&&answered.optJSONObject("feedback")?.optBoolean("right")==true){applause?.release();applause=android.media.MediaPlayer.create(this,R.raw.arena_applause);applause?.start()};accept(answered);return};val q=s.optJSONObject("question")?:return;val data=JSONObject().put("session_id",s.getString("session_id")).put("question_id",q.getString("id")).put("choice",submittedChoice);prefs.edit().putString("pending_answer",data.toString()).commit();stopVoice();call("arenaAnswer",data){prefs.edit().remove("pending_answer").apply();if(audioEnabled&&it.optJSONObject("feedback")?.optBoolean("right")==true){applause?.release();applause=android.media.MediaPlayer.create(this,R.raw.arena_applause);applause?.start()};accept(it)}}
    private fun nextLive(s:JSONObject){
        val data=JSONObject().put("session_id",s.getString("session_id")).put("question_id",s.getJSONObject("question").getString("id"))
        prefs.edit().putString("pending_next",data.toString()).commit()
        call("arenaNext",data){prefs.edit().remove("pending_next").apply();accept(it)}
    }
    private fun retryAnswer(){if(isOfflineSession()){render();return};val next=runCatching{JSONObject(prefs.getString("pending_next","")?:"")}.getOrNull();if(next!=null){call("arenaNext",next){prefs.edit().remove("pending_next").apply();accept(it)};return};val p=runCatching{JSONObject(prefs.getString("pending_answer","")?:"")}.getOrNull();if(p!=null)call("arenaAnswer",p){prefs.edit().remove("pending_answer").apply();accept(it)}else session?.let{call("arenaResume",JSONObject().put("session_id",it.getString("session_id"))){v->accept(v)}}}
    private fun stopVoice(){voiceCompletion=null;tts?.stop();voiceGeneration++;player?.release();player=null;speechEnvelope=null;scene?.speaking=false;scene?.friendSpeaking=false;scene?.speechEnergy=null}
    private fun narrationFinished(phase:String,key:String){
        if(key!=questionKey())return
        scene?.speaking=false;scene?.friendSpeaking=false
        val completion=voiceCompletion;voiceCompletion=null
        if(phase=="question")finishQuestionReading(key)
        if(phase=="friend"){scene?.phoneState="TELEFONATA PËRFUNDOI";phoneDialog?.setMessage(friendText+"\n\nFaleminderit! Tani zgjedhja të mbetet ty.")}
        completion?.invoke()
    }
    private fun ttsFallback(phase:String){
        if(!audioEnabled)return;val s=session?:return;val q=s.optJSONObject("question")?:return
        if(!isOfflineSession())status.text="Audioja AI nuk u ngarkua. Po provoj leximin zanor të pajisjes."
        val lang=locale
        if(!ttsReady||(tts?.isLanguageAvailable(java.util.Locale(lang))?:-1)<0){status.text="Audioja nuk është e disponueshme. Loja vazhdon me tekst.";narrationFinished(phase,questionKey(s));return}
        tts?.language=java.util.Locale(lang);val f=s.optJSONObject("feedback")
        val text=if(phase=="phone_intro")"${prefs.getString("name","Lojtari")}, po telefonojmë shokun për mendimin e tij. Ai nuk e ka përgjigjen e garantuar." else if(phase=="phone_outro")"Faleminderit për ndihmën! ${prefs.getString("name","Lojtari")}, tani zgjedhja të mbetet ty." else if(phase=="intro")"Mirë se erdhe në Quiz Arena Premium, ${prefs.getString("name","Lojtari")}! Je gati për nivelin ${s.optInt("level")}?" else if(phase=="clarify")dialogueText else if(phase=="friend")friendText.ifBlank{"Më vjen keq, lidhja me shokun nuk u realizua. Loja mund të vazhdojë."} else if(phase=="result")"${prefs.getString("name","Lojtari")}, ${s.optInt("correct")} përgjigje të sakta nga ${s.optInt("total")}. ${s.optInt("score")} pikë." else if(phase=="explanation"&&f!=null)q.getJSONArray("choices").optString(f.getInt("correct"))+". "+f.optString("explanation") else q.optString("prompt")+". "+(0 until q.getJSONArray("choices").length()).joinToString(". "){"${'A'+it}. "+q.getJSONArray("choices").getString(it)}
        val key=questionKey(s);val generation=voiceGeneration
        tts?.setPitch(if(phase=="friend") .78f else 1.08f)
        tts?.setSpeechRate(if(phase=="friend") .92f else .97f)
        val pcm=ArenaPcmStream()
        var started=0L
        tts?.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){
            override fun onBeginSynthesis(id:String?,sampleRateInHz:Int,audioFormat:Int,channelCount:Int){pcm.configure(sampleRateInHz,audioFormat,channelCount)}
            override fun onAudioAvailable(id:String?,audio:ByteArray?){if(audio!=null)pcm.append(audio)}
            override fun onStart(id:String?){runOnUiThread{if(generation==voiceGeneration){started=android.os.SystemClock.uptimeMillis();scene?.speechEnergy={pcm.at((android.os.SystemClock.uptimeMillis()-started).toInt())};scene?.speaking=true;scene?.friendSpeaking=phase=="friend";scene?.stageStatus=if(phase=="friend")"SHOKU PO FLET" else "PREZANTUESJA PO FLET"}}}
            override fun onDone(id:String?){runOnUiThread{if(generation==voiceGeneration)narrationFinished(phase,key)}}
            @Deprecated("TTS callback") override fun onError(id:String?){onDone(id)}
        })
        if(tts?.speak(ArenaAlbanianSpeech.forVoice(text,lang),android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"arena-fallback")==android.speech.tts.TextToSpeech.ERROR)narrationFinished(phase,key)
    }
    private fun trimVoiceCache(keep:java.io.File){
        val files=cacheDir.listFiles{f->f.name.startsWith("arena-voice-")&&f.extension=="mp3"}.orEmpty().sortedBy{it.lastModified()}
        var size=files.sumOf{it.length()}
        for(file in files){if(size<=64L*1024*1024)break;if(file!=keep){val bytes=file.length();if(file.delete())size-=bytes}}
    }
    // Prefer the presenter's bundled Albanian recordings, online and offline.
    private val recordedManifest by lazy {
        runCatching { assets.open("quiz-arena/voice-sq/manifest.json").bufferedReader().use { JSONObject(it.readText()) } }.getOrNull()
    }
    private fun recordedVoice(phase:String,s:JSONObject):Boolean {
        if(locale!="sq"||qa())return false
        val q=s.optJSONObject("question")
        val row=q?.let { recordedManifest?.optJSONObject("questions")?.optJSONObject(it.optString("id")) }
        // IDs alone are insufficient: live content or option order can change.
        val matches=row!=null&&q!=null&&row.optString("prompt")==q.optString("prompt")&&
            row.optJSONArray("choices")?.toString()==q.optJSONArray("choices")?.toString()
        val feedback=s.optJSONObject("feedback")
        val clips=when(phase){
            "intro"->listOf("intro")
            "phone_intro"->listOf("phone-intro")
            "phone_outro"->listOf("phone-outro")
            "timeout"->listOf("timeout")
            "result"->if(s.optBoolean("finished")&&s.optBoolean("passed"))listOf("level-up") else emptyList()
            "question"->if(matches)listOf(q!!.optString("id")+"-question") else emptyList()
            "explanation"->if(matches&&feedback!=null&&row!!.optInt("correct",-2)==feedback.optInt("correct",-1)) {
                val reaction=if(feedback.optInt("choice",s.optInt("local_choice",-2))==-1)"timeout"
                    else if(feedback.optBoolean("right"))if(s.optInt("index")%2==0)"correct-01" else "correct-02" else "wrong-01"
                listOf(reaction,q!!.optString("id")+"-explanation")
            }else emptyList()
            else->emptyList()
        }
        if(clips.isEmpty())return false
        val generation=voiceGeneration
        val key=questionKey(s)
        fun current()=generation==voiceGeneration&&resumed&&session===s&&!isFinishing&&!isDestroyed
        fun playClip(index:Int){
            if(!current())return
            if(index>=clips.size){narrationFinished(phase,key);return}
            voiceNetwork.execute {
                val prepared=runCatching {
                    val file=java.io.File(cacheDir,"arena-recorded-v1-"+clips[index]+".mp3")
                    if(!file.exists())assets.open("quiz-arena/voice-sq/"+clips[index]+".mp3").use { input->file.outputStream().use { input.copyTo(it) } }
                    file to runCatching { ArenaSpeechEnvelope.decode(file) }.getOrNull()
                }
                runOnUiThread {
                    if(current())prepared.onSuccess { (file,envelope)->
                        runCatching {
                            player?.release();speechEnvelope=envelope
                            scene?.speechEnergy={speechEnvelope?.at(runCatching { player?.currentPosition?:0 }.getOrDefault(0))?:0f}
                            player=android.media.MediaPlayer().apply {
                                setDataSource(file.absolutePath)
                                setOnPreparedListener { if(current()){scene?.speaking=true;scene?.friendSpeaking=false;scene?.stageStatus="PREZANTUESI PO FLET";it.start()} }
                                setOnCompletionListener { if(current())playClip(index+1) }
                                setOnErrorListener { _,_,_->if(current()){player?.release();player=null;ttsFallback(phase)};true }
                                prepareAsync()
                            }
                        }.onFailure { ttsFallback(phase) }
                    }.onFailure { ttsFallback(phase) }
                }
            }
        }
        playClip(0)
        return true
    }
    private fun voice(phase:String,onDone:(()->Unit)?=null){
        if(!audioEnabled){onDone?.invoke();return};val s=session?:return;stopVoice();voiceCompletion=onDone;if(recordedVoice(phase,s))return;if(isOfflineSession()){ttsFallback(phase);return}
        val generation=voiceGeneration;val version=modeVersion
        val q=s.optJSONObject("question")?:return
        val cacheKey=java.security.MessageDigest.getInstance("SHA-256").digest(("coral-v4|"+prefs.getString("name","")+locale+q.toString()+(s.optJSONObject("feedback")?.toString()?:"")+phase+if(phase=="clarify")dialogueText else if(phase=="friend")friendText else "").toByteArray()).joinToString(""){"%02x".format(it)}
        val cachedAudio=java.io.File(cacheDir,"arena-voice-$cacheKey.mp3")
        fun playCached(){
            scene?.speechEnergy={speechEnvelope?.at(runCatching{player?.currentPosition?:0}.getOrDefault(0))?:0f}
            player=android.media.MediaPlayer().apply{
                setDataSource(cachedAudio.absolutePath)
                setOnPreparedListener{
                    if(generation==voiceGeneration&&resumed&&session===s){scene?.speaking=true;scene?.friendSpeaking=phase=="friend";scene?.stageStatus=if(phase=="friend")"SHOKU PO FLET" else "PREZANTUESJA PO FLET";it.start()}
                }
                setOnCompletionListener{
                    if(generation==voiceGeneration&&resumed&&session===s)narrationFinished(phase,questionKey(s))
                }
                setOnErrorListener{_,_,_->cachedAudio.delete();ttsFallback(phase);true}
                prepareAsync()
            }
        }
        if(cachedAudio.exists()){voiceNetwork.execute{val envelope=runCatching{ArenaSpeechEnvelope.decode(cachedAudio)}.getOrNull();runOnUiThread{if(generation==voiceGeneration&&resumed&&session===s){speechEnvelope=envelope;runCatching{playCached()}.onFailure{cachedAudio.delete();ttsFallback(phase)}}}};return}
        val workerUrl=BuildConfig.QUIZ_ARENA_WORKER_URL.trimEnd('/')
        val trustedWorker=runCatching{
            val uri=java.net.URI(workerUrl)
            uri.scheme=="https"&&uri.host=="quiz-arena-backend.lazisalihi.workers.dev"&&uri.rawUserInfo==null&&uri.port==-1&&uri.rawQuery==null&&uri.rawFragment==null
        }.getOrDefault(false)
        if(trustedWorker&&!qa()&&accounts.isSignedIn()){
            val choices=q.optJSONArray("choices")?:org.json.JSONArray()
            val feedback=s.optJSONObject("feedback")
            val playerName=prefs.getString("name","Lojtari").orEmpty().take(80)
            val rawText=when(phase){
                "phone_intro"->"$"+"{playerName}, po telefonojmë shokun për mendimin e tij. Ai nuk e ka përgjigjen e garantuar."
                "phone_outro"->"Faleminderit për ndihmën! $"+"{playerName}, tani zgjedhja të mbetet ty."
                "intro"->"Mirë se erdhe në Quiz Arena Premium, $"+"{playerName}! Je gati për nivelin $"+"{s.optInt(\"level\")}?"
                "clarify"->dialogueText
                "friend"->friendText.ifBlank{"Lidhja me shokun nuk u realizua. Mund të vazhdosh lojën."}
                "result"->"$"+"{playerName}, $"+"{s.optInt(\"correct\")} përgjigje të sakta nga $"+"{s.optInt(\"total\")}. $"+"{s.optInt(\"score\")} pikë."
                "explanation"->if(feedback!=null){
                    val correct=feedback.optInt("correct",-1)
                    (if(correct in 0 until choices.length())"Përgjigjja e saktë është $"+"{'A'+correct}. $"+"{choices.optString(correct)}. " else "")+feedback.optString("explanation")
                }else "Shpjegimi nuk është ende i disponueshëm."
                else->q.optString("prompt")+". "+(0 until choices.length()).joinToString(". "){"$"+"{'A'+it}. $"+"{choices.optString(it)}"}
            }
            val speechText=ArenaAlbanianSpeech.forVoice(rawText,locale)
            voiceNetwork.execute{
                val result=runCatching{
                    val connection=(java.net.URL(workerUrl+"/api/ai/speech").openConnection() as java.net.HttpURLConnection)
                    try{
                        connection.requestMethod="POST";connection.connectTimeout=12000;connection.readTimeout=45000
                        connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json")
                        connection.outputStream.use{it.write(JSONObject().put("input",speechText).put("voice","coral").put("session_token",accounts.requireToken()).put("session_id",s.optString("session_id")).put("question_id",q.optString("id")).put("phase",phase).put("locale",locale).put("player_name",prefs.getString("name","Lojtari")).put("message",if(phase=="clarify")dialogueMessage else "").toString().toByteArray(Charsets.UTF_8))}
                        if(connection.responseCode !in 200..299)throw java.io.IOException("Worker speech HTTP ${connection.responseCode}")
                        if(!connection.contentType.orEmpty().lowercase(java.util.Locale.ROOT).startsWith("audio/"))throw java.io.IOException("Worker did not return audio")
                        val audio=connection.inputStream.use{input->
                            val buffer=ByteArray(8192)
                            val output=java.io.ByteArrayOutputStream()
                            while(true){
                                val count=input.read(buffer)
                                if(count<0)break
                                if(output.size()+count>8*1024*1024)throw java.io.IOException("Worker audio exceeds 8 MB")
                                output.write(buffer,0,count)
                            }
                            output.toByteArray()
                        }
                        if(audio.size<100)throw java.io.IOException("Empty Worker speech")
                        audio
                    }finally{connection.disconnect()}
                }
                runOnUiThread{
                    if(isFinishing||isDestroyed||!resumed||generation!=voiceGeneration||version!=modeVersion)return@runOnUiThread
                    result.onSuccess { audio ->
                        voiceNetwork.execute {
                            val envelope=runCatching { cachedAudio.writeBytes(audio);trimVoiceCache(cachedAudio);ArenaSpeechEnvelope.decode(cachedAudio) }.getOrNull()
                            runOnUiThread {
                                if(generation==voiceGeneration&&resumed&&session===s){
                                    speechEnvelope=envelope
                                    runCatching { playCached() }.onFailure { ttsFallback(phase) }
                                }
                            }
                        }
                    }.onFailure { ttsFallback(phase) }
                }
            }
            return
        }
        if(!accounts.isSignedIn()&&!(qa()&&qaTransport!=null)){ttsFallback(phase);return}
        val payload=JSONObject().put("session_id",s.getString("session_id")).put("phase",phase).put("question_id",q.getString("id")).put("locale",locale);if(accounts.isSignedIn())payload.put("session_token",accounts.requireToken())
        voiceNetwork.execute{val r=runCatching{if(qa()&&qaTransport!=null)qaTransport!!.invoke("arenaVoice",payload) else voiceClient.request("arenaVoice",payload)};runOnUiThread{if(isFinishing||isDestroyed||!resumed||generation!=voiceGeneration||version!=modeVersion)return@runOnUiThread
            r.onSuccess{runCatching{require(it.optString("question_id")==q.getString("id")){"Audio question mismatch"};cachedAudio.writeBytes(android.util.Base64.decode(it.getString("audio"),android.util.Base64.DEFAULT));voiceNetwork.execute{val envelope=runCatching{ArenaSpeechEnvelope.decode(cachedAudio)}.getOrNull();runOnUiThread{if(generation==voiceGeneration&&resumed&&session===s){speechEnvelope=envelope;playCached()}}}}.onFailure{ttsFallback(phase)}}.onFailure{ttsFallback(phase)}
        }}
    }
    private fun playUrl(url:String){stopVoice();val generation=voiceGeneration;player=android.media.MediaPlayer().apply{setDataSource(url);setOnPreparedListener{if(generation==voiceGeneration&&resumed)it.start()};setOnErrorListener{_,_,_->status.text="Audioja nuk është e disponueshme.";true};prepareAsync()}}
    private fun video(url:String){timer?.cancel();stopVoice();shell();val view=VideoView(this);add(view,230);val controls=MediaController(this);view.setMediaController(controls);controls.setAnchorView(view);view.setVideoURI(android.net.Uri.parse(url));view.setOnPreparedListener{it.start()};view.setOnCompletionListener{home();refresh()};view.setOnErrorListener{_,_,_->status.text="Videoja mungon. Progresi është ruajtur.";true};add(button("KAPËRCE • VAZHDO"){view.stopPlayback();home();refresh()})}
    @Deprecated("Activity back navigation") override fun onBackPressed(){if(session!=null){modeVersion++;home()}else super.onBackPressed()}
    override fun onActivityResult(code:Int,result:Int,data:android.content.Intent?){super.onActivityResult(code,result,data);if(code==PlayerPortrait.REQUEST&&PlayerPortrait.receive(this,result,data)){prefs.edit().putBoolean("initial_avatar",false).apply();PlayerPortrait.adjust(this){home()}}}
    override fun onResume(){super.onResume();resumed=true;ads.resume();if(isOfflineSession()){deadline=android.os.SystemClock.elapsedRealtime()+(session?.optLong("remaining_ms")?:0);render()}else if(session!=null&&!busy&&!advancing){retryAnswer()};if(!qa()){retryHandler.removeCallbacks(retry);retryHandler.postDelayed(retry,1000)}}
    override fun onPause(){phoneGeneration++;phoneDialog?.dismiss();phoneDialog=null;scene?.phoneState="";sound?.stopTone();if(session?.optBoolean("presentation_pending")==true)narrationKey="";persistLocal();timer?.cancel();retryHandler.removeCallbacks(retry);resumed=false;stopVoice();applause?.pause();ads.pause();super.onPause()}
    override fun onDestroy(){learningDialog?.dismiss();retryHandler.removeCallbacks(retry);tts?.shutdown();network.shutdownNow();voiceNetwork.shutdownNow();timer?.cancel();stopVoice();sound?.release();applause?.release();ads.destroy();super.onDestroy()}
}
