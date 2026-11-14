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
    private var offline=false;private var modeVersion=0
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
    private var locale="sq";private var category="";private var session:JSONObject?=null;private var bootstrap:JSONObject?=null
    private var busy=false;private var live=false;private var resumed=false;private var audioEnabled=true
    private var deadline=0L;private var tick=-1;private var timer:android.os.CountDownTimer?=null
    private var player:android.media.MediaPlayer?=null;private var sound:android.media.ToneGenerator?=null
    private var soundBed: android.media.MediaPlayer? = null
    private var cue: android.media.MediaPlayer? = null
    private var applause:android.media.MediaPlayer?=null;private var voiceGeneration=0
    private val answerButtons=mutableListOf<Button>()
    private var narrationKey=""
    private var voiceFailure=""
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
    override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=0xff080d27.toInt();window.navigationBarColor=0xff080d27.toInt();locale=prefs.getString("locale","sq")?:"sq";audioEnabled=prefs.getBoolean("audio",true);ads=NativeGameAds(this,"quiz_arena");sound=runCatching{android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC,90)}.getOrNull();bootstrap=runCatching{JSONObject(prefs.getString("bootstrap","")?:"")}.getOrNull();home();localStore.saved()?.let{offline=true;accept(it)};if(!qa()||qaTransport!=null)refresh()}
    private fun call(action:String,data:JSONObject=JSONObject(),done:(JSONObject)->Unit){
        if(busy){android.widget.Toast.makeText(this,"Kërkesa e mëparshme është ende në proces.",android.widget.Toast.LENGTH_SHORT).show();return}
        if(!accounts.isSignedIn()&&!(qa()&&qaTransport!=null)){live=false;android.widget.Toast.makeText(this,"Sesioni LIVE kërkon hyrje në llogari. Hape profilin ose luaj OFFLINE.",android.widget.Toast.LENGTH_LONG).show();if(session==null)home();return}
        busy=true;val version=modeVersion;status.text=when(action){"arenaReady"->"Prezantuesi përfundoi • Po nis koha…";"arenaAnswer"->"Po kontrollohet zgjedhja jote…";"arenaNext"->"Po hapet pyetja tjetër…";"arenaLifeline"->"Po aktivizohet ndihma…";else->"Po provohet LIVE… Mund të luash OFFLINE."}
        val payload=JSONObject(data.toString()).put("locale",locale).put("protocol",2).put("strict_presentation",true)
        if(accounts.isSignedIn())payload.put("session_token",accounts.requireToken())
        network.execute{val result=runCatching{if(qa()&&qaTransport!=null)qaTransport!!.invoke(action,payload) else client.request(action,payload)}
            runOnUiThread{busy=false;if(isFinishing||isDestroyed||version!=modeVersion)return@runOnUiThread
                result.onSuccess{live=true;done(it)}.onFailure{error->if(action=="arenaLifeline"){
                    val code=(error as? BackendException)?.code?:"NETWORK"
                    status.text=when(code){
                        "INVALID_ACTION"->"Serveri aktiv nuk e njeh ndihmën; nevojitet përditësimi i Apps Script."
                        "LIFELINE_UNAVAILABLE"->"Ndihma hapet pasi mbaron leximi dhe para mbarimit të kohës."
                        "LIFELINE_USED"->"Këtë ndihmë e ke përdorur në këtë nivel."
                        else->"Ndihma nuk u realizua: $code."
                    }
                    if(code in setOf("NETWORK","TIMEOUT","UPSTREAM_TIMEOUT","ARENA_UPSTREAM_ERROR"))add(button("RIPROVO NDIHMËN"){call(action,data,done)})
                    return@onFailure
                };live=false;if(session==null){home();status.text="LIVE: "+(if(error is BackendException)error.code else "NETWORK")+" • "+(error.message?:"Lidhja nuk u realizua")} else {status.text=if(isOfflineSession())"USHTRIM OFFLINE • Shqip" else "LIVE nuk përgjigjet. Sesioni LIVE mbetet i ruajtur.";add(button("RIPROVO / VAZHDO LIVE"){call(action,data,done)});add(button("VAZHDO NË USHTRIM OFFLINE"){startOffline(localStore.saved()!=null)})}}
            }
        }
    }
    private fun refresh(){call("arenaBootstrap"){if(it.optInt("available",0)<=0||it.optJSONArray("categories")==null){live=false;if(session==null)home();return@call};bootstrap=it;prefs.edit().putString("bootstrap",it.toString()).apply();if(session==null)home() else if(isOfflineSession())status.text="USHTRIM OFFLINE • LIVE u rikthye; hapet nga ballina pa humbur progresin lokal."}}
    private fun addRewardPanel() {
        val coins = session?.optInt("arena_game_coins", -1)?.takeIf { it >= 0 }
            ?: bootstrap?.optInt("arena_game_coins", -1)?.takeIf { it >= 0 }
        add(label("REWARD COIN • PIKË VIRTUALE", 16, gold))
        if (coins != null && !isOfflineSession() && live) {
            add(label("Game Coins nga Quiz Arena: $coins · Pa vlerë monetare", 13, blue))
        } else {
            add(label("Pikët e kuizit shënohen veçmas nga Game Coins.", 13, blue))
        }
    }
    private fun home(){releaseStudioSound();phoneGeneration++;phoneDialog?.dismiss();phoneDialog=null;sound?.stopTone();persistLocal();timer?.cancel();stopVoice();session=null;offline=!live;shell();val hero=FrameLayout(this);val art=StudioArtwork(this,"arena");hero.addView(art,FrameLayout.LayoutParams(-1,-1));val overlay=LinearLayout(this).apply{orientation=1;gravity=Gravity.BOTTOM;setPadding(dp(18),dp(12),dp(18),dp(14));background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x00000000,0xee101635.toInt()))};overlay.addView(label("Hyr në skenën e diturisë",25,gold));overlay.addView(label("Mëso pas çdo përgjigjeje.\nPërparo, sfido veten, ndriço arenën.",14));hero.addView(overlay,FrameLayout.LayoutParams(-1,-1));add(hero,208);hero.alpha=0f;hero.animate().alpha(1f).setDuration(450).start()
        val profile=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};profile.addView(PlayerPortrait.image(this).apply{setOnClickListener{PlayerPortrait.pick(this@ArenaCloudActivity)}},LinearLayout.LayoutParams(dp(68),dp(68)));val name=EditText(this).apply{hint="Emri i lojtarit";setText(prefs.getString("name",accounts.currentName()?:"Lojtari"));setTextColor(Color.WHITE);setHintTextColor(0xffb5c5e6.toInt());setSingleLine();filters=arrayOf(android.text.InputFilter.LengthFilter(80))};profile.addView(name,LinearLayout.LayoutParams(0,-2,1f));add(profile);add(button("ZGJIDH AVATARIN / FOTON"){PlayerPortrait.pick(this)});add(button("KOSTUMI • KËMISHA • KRAVATA"){outfit()})
        val languages=arrayOf("Shqip", "Deutsch • vetëm pyetjet", "English • vetëm pyetjet", "Türkçe • vetëm pyetjet");val codes=arrayOf("sq","de","en","tr");val spinner=Spinner(this).apply{adapter=ArrayAdapter(this@ArenaCloudActivity,android.R.layout.simple_spinner_dropdown_item,languages);setSelection(codes.indexOf(locale).coerceAtLeast(0));background=gradient(0xffc5e2ff.toInt(),0xffaabfff.toInt())};var initial=true;spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,position:Int,id:Long){if(initial){initial=false;return};locale=codes[position];prefs.edit().putString("locale",locale).apply();category="";refresh()}};add(spinner,48)
        add(button("RUAJ LOJTARIN"){val n=name.text.toString().trim();if(n.length<2){name.error="Të paktën 2 shkronja";return@button};prefs.edit().putString("name",n).apply();if(live)call("arenaRegister",JSONObject().put("name",n)){refresh()} else {status.text="Emri u ruajt në telefon • USHTRIM OFFLINE"}})
        val progress=if(live)bootstrap?.optJSONObject("progress") else localStore.progress();val level=progress?.optInt("level",1)?:1;add(label("NIVELI $level  •  ★ ${progress?.optInt("points",0)?:0} PIKË",20,gold));val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=20;this.progress=if(live)progress?.optJSONArray("completed")?.length()?:0 else (level-1).coerceAtLeast(0);progressTintList=android.content.res.ColorStateList.valueOf(blue)};add(bar)
        val saved=bootstrap?.optString("resume","").orEmpty().takeUnless{it=="null"}.orEmpty().ifEmpty{prefs.getString("active_id","")?:""};if(saved.isNotEmpty())add(button("▶ VAZHDO LOJËN E RUAJTUR"){call("arenaResume",JSONObject().put("session_id",saved)){accept(it);retryAnswer()}})
        if(localStore.saved()!=null)add(button("▶ VAZHDO USHTRIMIN OFFLINE"){startOffline(true)})
        val categories=if(live)bootstrap?.optJSONArray("categories")?:JSONArray() else JSONArray(localStore.categories());add(label("KATEGORITË",14,blue));val options=mutableListOf("Të gjitha");for(i in 0 until categories.length())options.add(categories.getString(i));val cats=Spinner(this).apply{adapter=ArrayAdapter(this@ArenaCloudActivity,android.R.layout.simple_spinner_dropdown_item,options);background=gradient(0xffc5e2ff.toInt(),0xffaabfff.toInt())};cats.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){category=if(pos==0)"" else options[pos]}};add(cats,48)
        add(label(if(live)"${bootstrap?.optInt("available",0)?:0} pyetje LIVE në gjuhën e zgjedhur" else "Pyetje lokale të gatshme: ${localStore.questions.size} • Shqip",12));add(button(if(live)"HYR NË ARENË • NIVELI $level" else "HYR NË USHTRIM OFFLINE • NIVELI $level"){val n=name.text.toString().trim();if(n.length<2){name.error="Shkruaj emrin";return@button};prefs.edit().putString("name",n).apply();if(!live){startOffline();return@button};call("arenaRegister",JSONObject().put("name",n)){call("arenaStart",JSONObject().put("level",level).put("category",category)){accept(it)}}})
        addRewardPanel()
        add(button("🏆 RENDITJA E LOJTARËVE"){if(!live){status.text="Renditja kërkon LIVE. Rekordi lokal: ${localStore.progress().optInt("best")} pikë.";return@button};call("arenaLeaderboard"){shell();add(label("RENDІTJA",25,gold));val rows=it.getJSONArray("ranking");if(rows.length()==0)add(label("Bëhu lojtari i parë që përfundon një nivel."));for(i in 0 until rows.length()){val r=rows.getJSONObject(i);add(label("${r.getInt("rank")}. ${r.getString("name")}   ★ ${r.getInt("points")}   • N${r.getInt("level")}"))};add(button("KTHEHU"){home()})}})
        if(!accounts.isSignedIn())add(button("HYR NË LLOGARI • AKTIVIZO LIVE"){startActivity(android.content.Intent(this,MainActivity::class.java).putExtra("open_profile",true))});add(button("SFIDA TË REJA • FJALË • AJETE • MEMORIE"){startActivity(android.content.Intent(this,ArenaLearningGamesActivity::class.java))});add(button("RIPROVO LIDHJEN LIVE"){refresh()});add(button("USHTRIM OFFLINE • PA RENDITJE"){startOffline(localStore.saved()!=null)});add(label("Regjistrimet shqip përdoren kur janë të disponueshme; për pyetjet e tjera përdoret zëri AI. Recitimet kërkojnë audio autentike. Pikët e arenës janë rezultat loje.",11,0xffb9c9df.toInt()));ads.banner(root)
    }
    private fun accept(v:JSONObject){val previous=session;if(previous!=null&&questionKey(previous)!=questionKey(v)){phoneGeneration++;phoneDialog?.dismiss();phoneDialog=null;sound?.stopTone()};if(previous!=null&&questionKey(previous)==questionKey(v)&&previous.has("learning_choice"))v.put("learning_choice",previous.getInt("learning_choice"));val f=v.optJSONObject("feedback");if(f?.optString("reason")=="timeout"&&f.optInt("submitted_choice",-1)>=0)v.put("learning_choice",f.getInt("submitted_choice"));session=v;offline=v.optBoolean("offline");if(offline)locale="sq" else if(v.optString("locale") in setOf("sq","de","en","tr"))locale=v.getString("locale");if(v.optJSONObject("feedback")==null&&v.optLong("remaining_ms")<=0)v.put("timed_out",true);deadline=android.os.SystemClock.elapsedRealtime()+v.optLong("remaining_ms");if(offline)localStore.save(v) else prefs.edit().putString("session",v.toString()).putString("active_id",v.optString("session_id")).apply();render()}
    private fun render(){val s=session?:return;timer?.cancel();stopVoice();answerButtons.clear();updateStudioBed();shell();val config=s.getJSONObject("config");add(label("${if(isOfflineSession())"USHTRIM OFFLINE" else "LIVE"} • NIVELI ${s.getInt("level")}  •  ${s.getInt("index")+1}/${s.getInt("total")}  •  ★ ${s.getInt("score")}",14,gold));scene=QuizArenaScene(this).apply{playerName=prefs.getString("name","")?:"";useInitials=prefs.getBoolean("initial_avatar",false);portrait=if(useInitials)null else PlayerPortrait.load(this@ArenaCloudActivity);suitColor=prefs.getInt("suit",0xff273b68.toInt());shirtColor=prefs.getInt("shirt",Color.WHITE);tieColor=prefs.getInt("tie",0xffc99b4e.toInt());reaction=if(s.optJSONObject("feedback")?.optString("reason")=="timeout")0 else if(s.optJSONObject("feedback")?.optBoolean("right")==true)1 else if(s.optJSONObject("feedback")!=null)-1 else 0};add(scene!!,((resources.displayMetrics.widthPixels/resources.displayMetrics.density-40f)*560f/720f).toInt().coerceAtLeast(1))
        if(s.optBoolean("finished")){if(!isOfflineSession())prefs.edit().remove("active_id").remove("pending_answer").remove("pending_next").apply();add(label(if(s.optBoolean("passed"))"NIVELI U KALUA!" else "PROVO PËRSËRI",27,gold));add(label("${s.getInt("correct")} / ${s.getInt("total")} të sakta • ★ ${s.getInt("score")}",21));scene?.apply{reaction=if(s.optBoolean("passed"))1 else -1;questionPrompt=if(s.optBoolean("passed"))"NIVELI U KALUA!" else "PROVO PËRSËRI";questionCategory="${s.getInt("correct")} / ${s.getInt("total")} TË SAKTA";questionNumber="NIVELI ${s.getInt("level")}";timerText="★ ${s.getInt("score")}";stageStatus="REZULTATI YT"};root.translationY=dp(22).toFloat();root.animate().translationY(0f).setDuration(550).start();if(s.optBoolean("passed")&&resultCueKey!=questionKey(s)){resultCueKey=questionKey(s);playStudioCue(R.raw.arena_win)};addRewardPanel();add(button("VAZHDO • KAPËRCE SKENËN"){home();refresh()});val video=config.optString("video");if(video.startsWith("https://"))add(button("SHIKO VIDEON E KALIMIT"){video(video)});if(audioEnabled)voice("result"){ads.naturalBreak()} else ads.naturalBreak();return}
        val q=s.getJSONObject("question");scene?.apply{questionPrompt=q.getString("prompt");questionCategory=q.getString("category");questionNumber="PYETJA ${s.getInt("index")+1} / ${s.getInt("total")}";contentDescription=questionPrompt};val feedback=s.optJSONObject("feedback");val choices=q.getJSONArray("choices")
        val hidden = if (feedback == null) ArenaLifelines.hidden(s) else emptySet()
        scene?.apply {
            alternatives = List(choices.length()) { if (it in hidden) "—" else choices.getString(it) }
            correctChoice = feedback?.optInt("correct", -1) ?: -1
            selectedChoice = feedback?.optInt("choice", s.optInt("local_choice", -1)) ?: -1
        }
        add(label(q.getString("prompt"), 18))
        for(i in 0 until choices.length()){val option=button("${'A'+i}   ${choices.getString(i)}"){sendAnswer(i)};option.gravity=Gravity.START or Gravity.CENTER_VERTICAL;option.isEnabled=feedback==null&&!s.optBoolean("answer_pending")&&!s.optBoolean("presentation_pending");answerButtons.add(option);if(feedback!=null&&i==feedback.getInt("correct"))option.background=gradient(0xff0b9a76.toInt(),0xff146b63.toInt());if(feedback!=null&&i==feedback.optInt("choice",s.optInt("local_choice",-2))&&!feedback.getBoolean("right"))option.background=gradient(0xffa73551.toInt(),0xff682544.toInt());if(i in ArenaLifelines.hidden(s)&&feedback==null){option.isEnabled=false;option.alpha=.25f;option.text="${'A'+i}   —"};add(option)}
        if(feedback!=null){scene?.apply{timerText=if(feedback.optInt("choice",-2)==-1)"PA PËRGJIGJE" else if(feedback.getBoolean("right"))"SAKTË" else "GABIM";stageStatus="TESTIM DHE MËSIM"};add(label(if(feedback.optInt("choice",s.optInt("local_choice",-2))==-1)"KOHA PËRFUNDOI • PA PËRGJIGJE" else if(feedback.getBoolean("right"))"✓ SAKTË" else "GABIM • PËRGJIGJJA E SAKTË: ${'A'+feedback.getInt("correct")}",20,gold));val selected=feedback.optInt("choice",s.optInt("local_choice",-2));if(selected>=0)add(label("Zgjedhja jote: ${'A'+selected} • ${choices.optString(selected)}",14,blue));if(s.has("learning_choice"))add(label(lateChoiceText(s),16,blue));if(selected==-1)add(label("Përgjigjja e saktë: ${'A'+feedback.getInt("correct")} • ${choices.optString(feedback.getInt("correct"))}",16,gold));add(label(feedback.getString("explanation"),17));add(label(feedback.optString("source"),12,blue));add(button("DËGJO SHPJEGIMIN"){voice("explanation")});add(button("HAP SHPJEGIMIN • TESTIM DHE MËSIM"){showLearning(s)});add(button("VAZHDO ›"){continueAfterLearning(s)});add(button("PYET PREZANTUESIN PËR SHPJEGIMIN"){clarify(s)});presentFeedback(s)}
        else if(s.optBoolean("presentation_pending")){beginQuestionReading(s)}
        else if(s.optBoolean("timed_out")){timeoutControls()}
        else{val clock=label("",15,gold);fun update(){val sec=((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)+999)/1000;scene?.apply{timerText="%02d:%02d".format(sec/60,sec%60);stageStatus="KOHA E PËRGJIGJES";remainingFraction=(sec.toFloat()/(config.optInt("seconds",60).coerceAtLeast(1))).coerceIn(0f,1f)};clock.text="⏱ ${((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)+999)/1000}s • ${config.getInt("points")} pikë për saktë"};update();timer=object:android.os.CountDownTimer((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1),200){override fun onTick(ms:Long){update();val sec=(ms/1000).toInt();if(sec!=tick){tick=sec;if(audioEnabled&&sec>0&&scene?.speaking!=true&&phoneDialog?.isShowing!=true)sound?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,if(sec<=10)145 else 80)}};override fun onFinish(){clock.text="Koha përfundoi • Asnjë përgjigje nuk u zgjodh";if(resumed)expireQuestion()}}.start();addAnswerInputs(s);addLifelineControls(s);add(button("DËGJO PYETJEN"){voice("question")});val rec=q.optString("recitation");if(rec.startsWith("https://"))add(button("DËGJO RECITIMIN AUTENTIK"){playUrl(rec)});add(button("RIPROVO / SINKRONIZO"){retryAnswer()});if(audioEnabled&&narrationKey!=questionKey(s)){narrationKey=questionKey(s);voice("question")}}
    }

    private fun updateStudioBed() {
        val active = resumed && audioEnabled && !qa() && session?.let {
            it.optJSONObject("feedback") == null && !it.optBoolean("finished")
        } == true
        if (!active) {
            soundBed?.release()
            soundBed = null
            return
        }
        if (soundBed == null) {
            soundBed = android.media.MediaPlayer.create(this, R.raw.arena_bed)?.apply {
                isLooping = true
                setVolume(.32f, .32f)
                start()
            }
        }
    }

    private fun playStudioCue(resource: Int) {
        if (!audioEnabled || !resumed || qa()) return
        cue?.release()
        cue = android.media.MediaPlayer.create(this, resource)?.apply {
            setVolume(.55f, .55f)
            setOnCompletionListener {
                if (cue === it) cue = null
                it.release()
            }
            start()
        }
    }

    private fun releaseStudioSound() {
        soundBed?.release()
        soundBed = null
        cue?.release()
        cue = null
    }

    private var resultCueKey = ""
    private var celebratedKey=""
    private fun presentFeedback(s:JSONObject){
        val key=questionKey(s)
        fun learn(){if(!qa()&&learningShown!=key&&session===s&&resumed){learningShown=key;showLearning(s)}}
        fun narrate(){if(session===s&&resumed){if(audioEnabled)voice("explanation"){learn()} else learn()}}
        val correct=s.optJSONObject("feedback")?.optBoolean("right")==true
        if(correct&&celebratedKey!=key){
            celebratedKey=key
            scene?.reaction=1
            if(audioEnabled){
                stopVoice();val generation=voiceGeneration
                applause?.release()
                applause=android.media.MediaPlayer.create(this,R.raw.arena_applause)
                val clip=applause
                if(clip==null){narrate();return}
                clip.setOnCompletionListener { if(generation==voiceGeneration&&session===s&&resumed){applause?.release();applause=null;narrate()} }
                clip.setOnErrorListener { _,_,_->if(generation==voiceGeneration&&session===s&&resumed)narrate();true }
                clip.start()
            }else{
                retryHandler.postDelayed({if(session===s&&resumed)narrate()},2800)
            }
        } else {
            if (!correct && celebratedKey != key &&
                s.optJSONObject("feedback")?.optString("reason") != "timeout") {
                celebratedKey = key
                playStudioCue(R.raw.arena_wrong)
            }
            narrate()
        }
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
            if(!isFinishing&&!isDestroyed&&session===s)advanceWithChallenge(s)
        }
        val key="ad-break:"+questionKey(s)
        val boundary=(s.optInt("index")+1)%3==0
        if(boundary&&!prefs.getBoolean(key,false)){
            prefs.edit().putBoolean(key,true).apply()
            if(runCatching{ads.naturalBreak(advance)}.getOrDefault(false))return
        }
        advance()
    }

    private var pendingChallenge:JSONObject?=null
    private fun advanceWithChallenge(s:JSONObject){
        val key="challenge:"+questionKey(s)
        val boundary=(s.optInt("index")+1)%2==0&&s.optInt("index")+1<s.optInt("total")
        if(locale=="sq"&&boundary&&!prefs.getBoolean(key,false)){
            val modes=arrayOf("verse","memory","word","true_false","order")
            pendingChallenge=s;advancing=true
            startActivityForResult(android.content.Intent(this,ArenaLearningGamesActivity::class.java)
                .putExtra("quick_mode",modes[(s.optInt("index")/2)%modes.size]).putExtra("native_qa",qa()),3116)
            return
        }
        if(isOfflineSession())accept(localStore.next(s)) else nextLive(s)
    }
    private val speechAnswerRequest = 3117
    private var speechQuestionKey: String? = null

    private fun addAnswerInputs(s: JSONObject) {
        val row = LinearLayout(this)
        val key = questionKey(s)
        row.addView(button("🎙 Mikrofon") {
            if (!canConfirmAnswer(key)) return@button
            val speech = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "sq-AL")
                .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Thuaj shkronjën ose përgjigjen në shqip")
            try {
                speechQuestionKey = key
                startActivityForResult(speech, speechAnswerRequest)
            } catch (_: android.content.ActivityNotFoundException) {
                speechQuestionKey = null
                answerInput(key, "", "Mikrofoni nuk është i disponueshëm. Shkruaj përgjigjen.")
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("✎ Shkruaj") {
            answerInput(key, "", "Shkruaj shkronjën ose tekstin e alternativës.")
        }, LinearLayout.LayoutParams(0, -2, 1f))
        add(row)
    }

    private fun canConfirmAnswer(key: String): Boolean {
        val s = session ?: return false
        return questionKey(s) == key && !busy && !s.optBoolean("presentation_pending") &&
            !s.optBoolean("answer_pending") && !s.optBoolean("timed_out") &&
            s.optJSONObject("feedback") == null && android.os.SystemClock.elapsedRealtime() < deadline
    }

    private fun answerInput(key: String, transcript: String, message: String) {
        if (!canConfirmAnswer(key)) return
        val choices = session!!.getJSONObject("question").getJSONArray("choices")
        val input = EditText(this).apply { setSingleLine(); setText(transcript) }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Përgjigjja jote").setMessage(message).setView(input)
            .setNegativeButton("Anulo", null).setPositiveButton("Kontrollo", null).show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!canConfirmAnswer(key)) { dialog.dismiss(); return@setOnClickListener }
            fun normalize(value: String) = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT).trim()
                .trimEnd('.', '!', '?').replace(Regex("\\s+"), " ")
            val value = normalize(input.text.toString())
            val spoken = value.removePrefix("alternativa ").removePrefix("nen ").removePrefix("shkronja ")
            val letter = when(spoken) {
                "a", "aa" -> 0
                "b", "be", "bee" -> 1
                "c", "ce" -> 2
                "d", "de" -> 3
                else -> null
            }
            val matches = (0 until choices.length()).filter {
                it !in ArenaLifelines.hidden(session!!) &&
                    (it == letter || (value.isNotEmpty() && value == normalize(choices.getString(it))))
            }
            val choice = matches.singleOrNull()
            if (choice == null) {
                input.error = "Shkruaj një shkronjë ose tekstin e një alternative të dukshme."
                return@setOnClickListener
            }
            dialog.dismiss()
            android.app.AlertDialog.Builder(this).setTitle("Konfirmo përgjigjen")
                .setMessage("${'A' + choice} • ${choices.getString(choice)}")
                .setNegativeButton("Anulo", null).setPositiveButton("Dërgo") { _, _ ->
                    if (canConfirmAnswer(key) && choice !in ArenaLifelines.hidden(session!!)) sendAnswer(choice)
                    else Toast.makeText(this, "Përgjigjja nuk mund të dërgohet më.", Toast.LENGTH_SHORT).show()
                }.show()
        }
    }

    private fun addLifelineControls(s:JSONObject){
        add(label("NDIHMAT E STUDIOS",17,gold))
        add(label("Secila përdoret një herë për nivel. Publiku dhe shoku japin mendime të simuluara.", 12, blue))
        val items=listOf(
            Triple("fifty","½","50:50"),
            Triple("audience","♟","PUBLIKU"),
            Triple("friend","☎","TELEFONO"),
            Triple("hint","✦","NDIHMË AI"),
            Triple("switch","↻","NDËRRO")
        )
        val grid=android.widget.GridLayout(this).apply{
            columnCount=3
            setPadding(0,dp(5),0,dp(8))
        }
        for((kind,symbol,title) in items){
            val used=ArenaLifelines.used(s,kind)
            val choices=s.getJSONObject("question").getJSONArray("choices").length()
            val available=!used&&(kind!="fifty"||(if(isOfflineSession())choices>=3 else choices==4))&&
                (kind!="switch"||!isOfflineSession()||(s.optJSONArray("local_reserve")?.length()?:0)>0)
            val tile=android.widget.LinearLayout(this).apply{
                orientation=1;gravity=Gravity.CENTER
                background=gradient(0xff18375f.toInt(),0xff34265f.toInt())
                setPadding(dp(5),dp(12),dp(5),dp(12))
                alpha=if(available)1f else .42f
                isEnabled=available
                contentDescription="$title, "+if(used)"përdorur" else if(available)"aktiv" else "jo i disponueshëm"
                addView(label(symbol,30,gold).apply{gravity=Gravity.CENTER})
                addView(label(title,11,Color.WHITE).apply{gravity=Gravity.CENTER})
                addView(label(if(used)"PËRDORUR" else if(available)"AKTIVE" else "JO AKTIVE",9,blue).apply{gravity=Gravity.CENTER})
                setOnClickListener{if(available)useLifeline(s,kind)}
            }
            val params=android.widget.GridLayout.LayoutParams().apply{
                width=0;height=dp(112)
                columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f)
                setMargins(dp(3),dp(3),dp(3),dp(3))
            }
            grid.addView(tile,params)
        }
        add(grid)
        add(button("✦  BISEDË ME PREZANTUESIN"){clarify(s)})
    }
    private fun useLifeline(s:JSONObject,kind:String){
        if(session!==s){status.text="Pyetja ka ndryshuar. Provo ndihmën te pyetja aktuale.";return}
        if (ArenaLifelines.used(s, kind) || s.optBoolean("presentation_pending") ||
            s.optBoolean("answer_pending") || s.optJSONObject("feedback") != null ||
            android.os.SystemClock.elapsedRealtime() >= deadline) return
        if(busy){android.widget.Toast.makeText(this,"Po pritet përgjigjja e serverit. Provo sërish pas pak.",android.widget.Toast.LENGTH_LONG).show();return}
        if(isOfflineSession()){
            runCatching{
                persistLocal()
                val r=ArenaLifelines.use(s,kind)
                if(kind=="friend"){s.put("remaining_ms",(deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)+90000);deadline=android.os.SystemClock.elapsedRealtime()+s.getLong("remaining_ms")}
                localStore.save(s);render();showLifeline(s,r)
            }.onFailure{status.text="Ndihma: "+(it.message?:"Pyetja ka përfunduar. Vazhdo te pyetja tjetër.")}
        }else call("arenaLifeline",JSONObject().put("session_id",s.optString("session_id")).put("question_id",s.getJSONObject("question").getString("id")).put("kind",kind)){r->accept(r);r.optJSONObject("lifeline")?.let { showLifeline(r,it) }?:run { status.text="Serveri nuk ktheu ndihmën e kërkuar. Provo sinkronizimin LIVE." }}
    }
    private fun showLifeline(s:JSONObject,r:JSONObject){
        when(r.getString("kind")){
            "fifty"->{val hidden=ArenaLifelines.hidden(s);android.app.AlertDialog.Builder(this).setTitle("50:50 • U HOQËN ALTERNATIVAT") .setMessage(hidden.sorted().joinToString(", "){"${'A'+it}"}+"\nDy alternativa mbeten aktive. Zgjidh njërën.").setPositiveButton("VAZHDO",null).show()}
            "switch"->{status.text="Pyetja u ndërrua. Dëgjo pyetjen e re para se të nisë koha."}
            "friend"->phoneFriend(s,r)
            "audience"->{
                val votes=r.getJSONArray("votes");val currentScene=scene
                currentScene?.audienceVotes=IntArray(votes.length()){votes.getInt(it)}
                status.text="Publiku virtual po voton • mendim i simuluar."
                retryHandler.postDelayed({if(scene===currentScene){currentScene?.audienceVotes=null;currentScene?.invalidate()}},12000)
                add(button("KTHEHU NGA PUBLIKU NË SKENË"){currentScene?.audienceVotes=null;currentScene?.invalidate()})
            }
            "hint"->if(isOfflineSession())android.app.AlertDialog.Builder(this).setTitle("ORIENTIM PËR PYETJEN").setMessage(r.optString("text").ifBlank { ArenaLifelines.hint(s.getJSONObject("question")) }).setPositiveButton("VAZHDO",null).show() else aiDialog(s,"hint","")
        }
    }
    private fun clarify(s:JSONObject){
        val input=EditText(this).apply{hint="Çfarë dëshiron të sqarosh?";filters=arrayOf(android.text.InputFilter.LengthFilter(300))}
        android.app.AlertDialog.Builder(this).setTitle("BISEDË ME PREZANTUESIN").setView(input).setMessage(if(s.optJSONObject("feedback")==null)"Para përgjigjes sqarohen vetëm rregullat; zgjidhja mbetet e fshehur." else "Sqarimi mbështetet në shpjegimin dhe burimin e pyetjes.").setPositiveButton("PYET"){_,_->aiDialog(s,"clarify",input.text.toString())}.setNegativeButton("MBYLL",null).show()
    }
    private fun aiDialog(s:JSONObject,mode:String,message:String){
        if(isOfflineSession()){
            android.app.AlertDialog.Builder(this).setTitle("PREZANTUESI • OFFLINE").setMessage(s.optJSONObject("feedback")?.optString("explanation")?:"Zgjidh një alternativë para mbarimit të kohës. Shpjegimi hapet pas përgjigjes. Biseda AI kërkon internet dhe sesion LIVE.").setPositiveButton("KUPTOVA",null).show();return
        }
        val dialog=android.app.AlertDialog.Builder(this).setTitle("PREZANTUESI AI").setMessage("Po përgatitet sqarimi…").setPositiveButton("MBYLL",null).create();dialog.setOnDismissListener{stopVoice()};dialog.show()
        val version=modeVersion
        voiceNetwork.execute{val r=runCatching{BackendClient(BuildConfig.QUIZ_ARENA_WORKER_URL.trimEnd('/')+"/api/ai/respond",60000).request("arenaDialogue",JSONObject().put("session_token",accounts.requireToken()).put("session_id",s.optString("session_id")).put("question_id",s.getJSONObject("question").optString("id")).put("mode",mode).put("message",message).put("locale",locale))};runOnUiThread{if(!isFinishing&&!isDestroyed&&version==modeVersion&&session===s&&dialog.isShowing){
            dialogueText=r.getOrNull()?.optString("text").orEmpty();dialogueMessage=message
            if(dialogueText.isBlank())dialogueText=s.optJSONObject("feedback")?.optString("explanation")?:ArenaLifelines.hint(s.getJSONObject("question"))
            dialog.setMessage(dialogueText)
            if(dialogueText.isNotBlank()&&audioEnabled)voice("clarify")
        }}}
    }
    private fun phoneFriend(s:JSONObject,r:JSONObject){
        stopVoice();val generation=++phoneGeneration
        friendText=r.optString("text").ifBlank { if(isOfflineSession())ArenaLifelines.simulatedFriend(s.getJSONObject("question")) else "" }
        val dialog=android.app.AlertDialog.Builder(this).setTitle("TELEFONO SHOKUN"+if(isOfflineSession())" • SIMULIM" else " • MENDIM AI").setMessage("☎ Po telefonohet…").setNeutralButton("RIPROVO ZËRIN", null).setPositiveButton("MBYLL TELEFONATËN",null).create()
        (root.parent as? ScrollView)?.smoothScrollTo(0,0)
        phoneDialog=dialog;dialog.setOnDismissListener{if(generation==phoneGeneration){phoneGeneration++;stopVoice();sound?.stopTone();scene?.phoneState=""};phoneDialog=null};dialog.show();dialog.window?.apply{setGravity(Gravity.BOTTOM);clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)};scene?.phoneState="PO TELEFONOHET…"
        dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).apply {
            visibility = View.GONE
            setOnClickListener {
                visibility = View.GONE
                voice("friend"){if(generation==phoneGeneration&&dialog.isShowing)voice("phone_outro")}
            }
        }
        fun dial(){
        if(generation!=phoneGeneration||session!==s||!resumed||!dialog.isShowing)return
        scene?.phoneState="PO TELEFONOHET…"
        if(audioEnabled)sound?.startTone(android.media.ToneGenerator.TONE_SUP_RINGTONE,1600)
        retryHandler.postDelayed({
            if(generation==phoneGeneration&&session===s&&resumed&&dialog.isShowing){
                sound?.stopTone();scene?.phoneState="SHOKU U PËRGJIGJ"
                dialog.setMessage("Për ${prefs.getString("name","Lojtari")}.\n\n"+if(friendText.isNotBlank())friendText else "Shoku po mendon për pyetjen… Mendimi i tij nuk është garanci.")
                if(isOfflineSession()){scene?.phoneState="MENDIMI I SHOKUT";dialog.setMessage(friendText+"\n\nTelefonata me zë kërkon lidhje LIVE.")}
                else voiceNetwork.execute{
                    val response=runCatching{BackendClient(BuildConfig.QUIZ_ARENA_WORKER_URL.trimEnd('/')+"/api/ai/respond",60000).request("arenaDialogue",JSONObject().put("session_token",accounts.requireToken()).put("session_id",s.optString("session_id")).put("question_id",s.getJSONObject("question").optString("id")).put("mode","friend").put("locale",locale))}
                    runOnUiThread{if(generation==phoneGeneration&&session===s&&resumed&&dialog.isShowing){
                        friendText=response.getOrNull()?.optString("text").orEmpty()
                        if(friendText.isBlank()){
                            friendText=ArenaLifelines.simulatedFriend(s.getJSONObject("question"))
                            dialog.setTitle("TELEFONO SHOKUN • SIMULIM REZERVË")
                        }
                        dialog.setMessage(friendText+"\n\nMendimi i shokut nuk është garanci.")
                        if(response.isSuccess&&response.getOrNull()?.optString("text")?.isNotBlank()==true&&audioEnabled)voice("friend"){if(generation==phoneGeneration&&dialog.isShowing)voice("phone_outro")}
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
        add(button("RIPROVO ZËRIN E PREZANTUESIT"){voice("question")}.apply { isEnabled = audioEnabled })
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
    private fun sendAnswer(choice:Int){val s=session?:return;val question=s.optJSONObject("question")?:return;val choices=question.optJSONArray("choices")?:return;if(choice < -1 || choice >= choices.length() || choice in ArenaLifelines.hidden(s))return;if(choice == -1 && !s.optBoolean("timed_out"))return;if(s.optBoolean("presentation_pending"))return;if(s.optJSONObject("feedback")!=null||s.optBoolean("answer_pending"))return;if(busy){status.text="Po përfundon kërkesa LIVE. Provoje përsëri.";return};val late=choice>=0&&(s.optBoolean("timed_out")||android.os.SystemClock.elapsedRealtime()>=deadline);if(late){s.put("timed_out",true).put("remaining_ms",0).put("learning_choice",choice)};val submittedChoice=if(late)-1 else choice;timer?.cancel();stopVoice();s.put("answer_pending",true).put("local_choice",choice);answerButtons.forEach{it.isEnabled=false};if(choice>=0){answerButtons[choice].background=gradient(0xff815625.toInt(),0xff483359.toInt());scene?.selectedChoice=choice};if(isOfflineSession()){s.remove("answer_pending");timer?.cancel();persistLocal();val answered=localStore.answer(s,submittedChoice);answered.remove("answer_pending");accept(answered);return};val q=s.optJSONObject("question")?:return;val data=JSONObject().put("session_id",s.getString("session_id")).put("question_id",q.getString("id")).put("choice",submittedChoice);prefs.edit().putString("pending_answer",data.toString()).commit();stopVoice();call("arenaAnswer",data){prefs.edit().remove("pending_answer").apply();accept(it)}}
    private fun nextLive(s:JSONObject){
        val data=JSONObject().put("session_id",s.getString("session_id")).put("question_id",s.getJSONObject("question").getString("id"))
        prefs.edit().putString("pending_next",data.toString()).commit()
        call("arenaNext",data){prefs.edit().remove("pending_next").apply();accept(it)}
    }
    private fun retryAnswer(){if(isOfflineSession()){render();return};val next=runCatching{JSONObject(prefs.getString("pending_next","")?:"")}.getOrNull();if(next!=null){call("arenaNext",next){prefs.edit().remove("pending_next").apply();accept(it)};return};val p=runCatching{JSONObject(prefs.getString("pending_answer","")?:"")}.getOrNull();if(p!=null)call("arenaAnswer",p){prefs.edit().remove("pending_answer").apply();accept(it)}else session?.let{call("arenaResume",JSONObject().put("session_id",it.getString("session_id"))){v->accept(v)}}}
    private fun stopVoice(){voiceGeneration++;sound?.stopTone();applause?.release();applause=null;voiceCompletion=null;player?.release();player=null;speechEnvelope=null;scene?.speaking=false;scene?.friendSpeaking=false;scene?.speechEnergy=null}
    private fun narrationFinished(phase:String,key:String){
        if(key!=questionKey())return
        scene?.speaking=false;scene?.friendSpeaking=false
        val completion=voiceCompletion;voiceCompletion=null
        if(phase=="question")finishQuestionReading(key)
        if(phase=="friend"){scene?.phoneState="TELEFONATA PËRFUNDOI";phoneDialog?.setMessage(friendText+"\n\nFaleminderit! Tani zgjedhja të mbetet ty.")}
        completion?.invoke()
    }
    /** Playback uses the existing recorded/AI audio. A missing clip never changes voices. */
    private fun voiceUnavailable(phase:String){
        if(!resumed)return
        val s=session?:return
        scene?.speaking=false;scene?.friendSpeaking=false
        status.text="Audioja nuk u realizua"+(if(voiceFailure.isBlank())"" else ": $voiceFailure")+". Riprovo audion ose vazhdo me tekst."
        if(phase=="question"&&s.optBoolean("presentation_pending")){
            timer?.cancel()
            voiceCompletion=null
            // The existing Skip Reading control explicitly starts the answer clock.
            return
        }
        if (phase == "friend") {
            phoneDialog?.setMessage(friendText + "\n\nZëri nuk u ngarkua. Mund ta riprovosh.")
            phoneDialog?.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.visibility = View.VISIBLE
            voiceCompletion = null
            return
        }
        narrationFinished(phase,questionKey(s))
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
            // A reaction is optional. Its failure must not suppress the actual explanation.
            fun unavailable(){
                if(index<clips.lastIndex)playClip(index+1) else {val completion=voiceCompletion;voice(phase,false,completion)}
            }
            voiceNetwork.execute {
                val prepared=runCatching {
                    val updated=packageManager.getPackageInfo(packageName,0).lastUpdateTime
                    val file=java.io.File(cacheDir,"arena-recorded-"+updated+"-"+clips[index]+".mp3")
                    if(!file.isFile||file.length()==0L){
                        val temp=java.io.File.createTempFile("arena-recording-", ".part", cacheDir)
                        try {
                            assets.open("quiz-arena/voice-sq/"+clips[index]+".mp3").use { input ->
                                temp.outputStream().use { input.copyTo(it) }
                            }
                            check(temp.length()>0L&&temp.renameTo(file))
                        } finally {
                            temp.delete()
                        }
                    }
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
                                setOnErrorListener { _,_,_->if(current()){player?.release();player=null;unavailable()};true }
                                prepareAsync()
                            }
                        }.onFailure { unavailable() }
                    }.onFailure { unavailable() }
                }
            }
        }
        playClip(0)
        return true
    }
    private fun voice(phase:String,useRecordings:Boolean=true,onDone:(()->Unit)?=null){
        if(!audioEnabled){onDone?.invoke();return};val s=session?:return;val practice=s.optBoolean("offline");stopVoice();voiceCompletion=onDone;if(useRecordings&&recordedVoice(phase,s))return;if(practice&&(!accounts.isSignedIn()||phase !in setOf("question","explanation","intro"))){voiceUnavailable(phase);return}
        voiceFailure=""
        val generation=voiceGeneration;val version=modeVersion
        val q=s.optJSONObject("question")?:run{voiceUnavailable(phase);return}
        val cacheKey=java.security.MessageDigest.getInstance("SHA-256").digest(("worker-speech-v12|"+prefs.getString("name","")+locale+q.toString()+(s.optJSONObject("feedback")?.toString()?:"")+phase+if(phase=="clarify")dialogueText else if(phase=="friend")friendText else "").toByteArray()).joinToString(""){"%02x".format(it)}
        val cachedAudio=java.io.File(cacheDir,"arena-voice-$cacheKey.mp3")
        fun playCached(){
            scene?.speechEnergy={speechEnvelope?.at(runCatching{player?.currentPosition?:0}.getOrDefault(0))?:0f}
            player=android.media.MediaPlayer().apply{
                setDataSource(cachedAudio.absolutePath)
                setOnPreparedListener{
                    if(generation==voiceGeneration&&resumed&&session===s){scene?.speaking=true;scene?.friendSpeaking=phase=="friend";scene?.stageStatus=if(phase=="friend")"SHOKU PO FLET" else "PREZANTUESI PO FLET";it.start();runCatching{it.playbackParams=it.playbackParams.setSpeed(1f).setPitch(1f)}}
                }
                setOnCompletionListener{
                    if(generation==voiceGeneration&&resumed&&session===s)narrationFinished(phase,questionKey(s))
                }
                setOnErrorListener{_,_,_->if(generation==voiceGeneration&&resumed&&session===s){player?.release();player=null;cachedAudio.delete();voiceUnavailable(phase)};true}
                prepareAsync()
            }
        }
        if(cachedAudio.exists()){voiceNetwork.execute{val envelope=runCatching{ArenaSpeechEnvelope.decode(cachedAudio)}.getOrNull();runOnUiThread{if(generation==voiceGeneration&&resumed&&session===s){speechEnvelope=envelope;runCatching{playCached()}.onFailure{cachedAudio.delete();voiceUnavailable(phase)}}}};return}
        val workerUrl=BuildConfig.QUIZ_ARENA_WORKER_URL.trimEnd('/')
        val trustedWorker=runCatching{
            val uri=java.net.URI(workerUrl)
            uri.scheme=="https"&&uri.host=="quiz-arena-backend.lazisalihi.workers.dev"&&uri.rawUserInfo==null&&uri.port==-1&&uri.rawQuery==null&&uri.rawFragment==null
        }.getOrDefault(false)
        if(trustedWorker&&!qa()&&accounts.isSignedIn()){
            voiceNetwork.execute{
                if(generation!=voiceGeneration||version!=modeVersion||!resumed)return@execute
                val result=runCatching{
                    if(practice){
                        val health=(java.net.URL(workerUrl+"/health").openConnection() as java.net.HttpURLConnection)
                        try {
                            health.connectTimeout=8000;health.readTimeout=8000;health.instanceFollowRedirects=false
                            val ready=health.responseCode==200&&health.inputStream.bufferedReader().use{JSONObject(it.readText().take(8192))}.optBoolean("practice_speech")
                            if(!ready)throw java.io.IOException("PRACTICE_VOICE_UPDATE_REQUIRED")
                        }finally{health.disconnect()}
                    }
                    val connection=(java.net.URL(workerUrl+"/api/ai/speech").openConnection() as java.net.HttpURLConnection)
                    try{
                        connection.requestMethod="POST";connection.connectTimeout=12000;connection.readTimeout=45000
                        connection.instanceFollowRedirects=false;connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json")
                        val request=JSONObject().put("session_token",accounts.requireToken()).put("session_id",s.optString("session_id")).put("question_id",q.optString("id")).put("phase",phase).put("locale",locale).put("player_name",prefs.getString("name","Lojtari")).put("message",if(phase=="clarify")dialogueMessage else "")
                        if(practice){
                            request.put("practice",true).put("practice_prompt",q.optString("prompt")).put("practice_choices",q.getJSONArray("choices"))
                            if(phase=="explanation")request.put("choice",s.optJSONObject("feedback")?.optInt("choice",s.optInt("local_choice",-1))?:-1)
                        }
                        connection.outputStream.use{it.write(request.toString().toByteArray(Charsets.UTF_8))}
                        if(connection.responseCode !in 200..299){
                            val error=runCatching{connection.errorStream?.bufferedReader()?.use{JSONObject(it.readText().take(8192))}?.optString("error")}.getOrNull()
                            throw java.io.IOException(error?.takeIf{it.matches(Regex("[A-Z_]{2,80}"))}?:"HTTP ${connection.responseCode}")
                        }
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
                                    runCatching { playCached() }.onFailure { voiceUnavailable(phase) }
                                }
                            }
                        }
                    }.onFailure { voiceFailure=if(it.message=="PRACTICE_VOICE_UPDATE_REQUIRED")"Serveri i zërit kërkon përditësim" else it.message?.take(80)?:"Lidhja dështoi";voiceUnavailable(phase) }
                }
            }
            return
        }
        val testTransport=qaTransport?.takeIf { qa() }?:run{voiceUnavailable(phase);return}
        val payload=JSONObject().put("session_id",s.getString("session_id")).put("phase",phase).put("question_id",q.getString("id")).put("locale",locale);if(accounts.isSignedIn())payload.put("session_token",accounts.requireToken())
        voiceNetwork.execute{val r=runCatching{testTransport("arenaVoice",payload)};runOnUiThread{if(isFinishing||isDestroyed||!resumed||generation!=voiceGeneration||version!=modeVersion)return@runOnUiThread
            r.onSuccess{runCatching{require(it.optString("question_id")==q.getString("id")){"Audio question mismatch"};cachedAudio.writeBytes(android.util.Base64.decode(it.getString("audio"),android.util.Base64.DEFAULT));voiceNetwork.execute{val envelope=runCatching{ArenaSpeechEnvelope.decode(cachedAudio)}.getOrNull();runOnUiThread{if(generation==voiceGeneration&&resumed&&session===s){speechEnvelope=envelope;playCached()}}}}.onFailure{voiceUnavailable(phase)}}.onFailure{voiceUnavailable(phase)}
        }}
    }
    private fun playUrl(url:String){stopVoice();val generation=voiceGeneration;player=android.media.MediaPlayer().apply{setDataSource(url);setOnPreparedListener{if(generation==voiceGeneration&&resumed)it.start()};setOnErrorListener{_,_,_->status.text="Audioja nuk është e disponueshme.";true};prepareAsync()}}
    private fun video(url:String){timer?.cancel();stopVoice();shell();val view=VideoView(this);add(view,230);val controls=MediaController(this);view.setMediaController(controls);controls.setAnchorView(view);view.setVideoURI(android.net.Uri.parse(url));view.setOnPreparedListener{it.start()};view.setOnCompletionListener{home();refresh()};view.setOnErrorListener{_,_,_->status.text="Videoja mungon. Progresi është ruajtur.";true};add(button("KAPËRCE • VAZHDO"){view.stopPlayback();home();refresh()})}
    @Deprecated("Activity back navigation") override fun onBackPressed(){if(session!=null){modeVersion++;home()}else super.onBackPressed()}
    override fun onActivityResult(code:Int,result:Int,data:android.content.Intent?){super.onActivityResult(code,result,data);if(code==speechAnswerRequest){
        val key=speechQuestionKey; speechQuestionKey=null
        if(key!=null && result==RESULT_OK){
            val transcript=data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            answerInput(key,transcript,"Kontrollo tekstin që u kuptua dhe korrigjoje nëse duhet.")
        }
        return
    };if(code==3116){val s=pendingChallenge;pendingChallenge=null;advancing=false;if(s!=null&&session===s){if(result==RESULT_OK){prefs.edit().putBoolean("challenge:"+questionKey(s),true).apply();if(isOfflineSession())accept(localStore.next(s)) else nextLive(s)}else render()};return};if(code==PlayerPortrait.REQUEST&&PlayerPortrait.receive(this,result,data)){prefs.edit().putBoolean("initial_avatar",false).apply();PlayerPortrait.adjust(this){home()}}}
    override fun onResume(){super.onResume();resumed=true;ads.resume();if(isOfflineSession()&&!advancing){deadline=android.os.SystemClock.elapsedRealtime()+(session?.optLong("remaining_ms")?:0);render()}else if(session!=null&&!busy&&!advancing){retryAnswer()};if(!qa()){retryHandler.removeCallbacks(retry);retryHandler.postDelayed(retry,1000)}}
    override fun onPause(){releaseStudioSound();phoneGeneration++;phoneDialog?.dismiss();phoneDialog=null;scene?.phoneState="";sound?.stopTone();if(player!=null||scene?.speaking==true||session?.optBoolean("presentation_pending")==true)narrationKey="";persistLocal();timer?.cancel();retryHandler.removeCallbacks(retry);resumed=false;stopVoice();applause?.pause();ads.pause();super.onPause()}
    override fun onDestroy(){releaseStudioSound();learningDialog?.dismiss();retryHandler.removeCallbacks(retry);network.shutdownNow();voiceNetwork.shutdownNow();timer?.cancel();stopVoice();sound?.release();applause?.release();ads.destroy();super.onDestroy()}
}
