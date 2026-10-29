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
    private fun persistLocal(){val s=session?:return;if(isOfflineSession()){if(s.optJSONObject("feedback")==null)s.put("remaining_ms",(deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0));localStore.save(s)}}

    private val accounts by lazy{AccountStore(this)}
    private val prefs by lazy{getSharedPreferences("arena_cloud_"+(accounts.currentUserId()?:"guest"),MODE_PRIVATE)}
    private val client=BackendClient("https://script.google.com/macros/s/AKfycbzu9rso0BO_UhLBPi3TMqIQl44tZ12r0n-t143ydmWOoiVMPbm_YA1uF0rImIfEcvaERw/exec",8000)
    private val voiceClient=BackendClient("https://script.google.com/macros/s/AKfycbzu9rso0BO_UhLBPi3TMqIQl44tZ12r0n-t143ydmWOoiVMPbm_YA1uF0rImIfEcvaERw/exec",60_000)
    private var locale="sq";private var category="";private var session:JSONObject?=null;private var bootstrap:JSONObject?=null
    private var busy=false;private var live=false;private var resumed=false;private var audioEnabled=true
    private var deadline=0L;private var tick=-1;private var timer:android.os.CountDownTimer?=null
    private var player:android.media.MediaPlayer?=null;private var sound:android.media.ToneGenerator?=null
    private var applause:android.media.MediaPlayer?=null;private var voiceGeneration=0
    private lateinit var root:LinearLayout;private lateinit var status:TextView;private var scene:QuizArenaScene?=null
    private lateinit var ads:NativeGameAds
    private val gold=0xffffd782.toInt();private val blue=0xff64c8ff.toInt()
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun label(t:String,size:Int=16,color:Int=Color.WHITE)=TextView(this).apply{text=t;textSize=size.toFloat();setTextColor(color);setPadding(dp(6),dp(7),dp(6),dp(7));typeface=Typeface.DEFAULT_BOLD}
    private fun gradient(a:Int,b:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(a,b)).apply{cornerRadius=dp(20).toFloat();setStroke(dp(1),0x4468c5ff)}
    private fun add(v:View,h:Int=-2){root.addView(v,LinearLayout.LayoutParams(-1,if(h<0)h else dp(h)).apply{bottomMargin=dp(12)})}
    private fun button(t:String,action:()->Unit)=Button(this).apply{text=t;isAllCaps=false;textSize=16f;setTextColor(Color.WHITE);background=gradient(0xff1b62af.toInt(),0xff4e35a0.toInt());minHeight=dp(54);setOnClickListener{run{performHapticFeedback(1);animate().scaleX(.97f).scaleY(.97f).setDuration(70).withEndAction{animate().scaleX(1f).scaleY(1f).setDuration(100).start()}.start();action()}}}
    private fun shell(){val scroll=ScrollView(this).apply{isFillViewport=true;setBackgroundColor(0xff080d27.toInt());setOnApplyWindowInsetsListener{v,i->v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom);i}};root=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(14),dp(18),dp(26));background=gradient(0xff0b1734.toInt(),0xff261544.toInt())};scroll.addView(root);setContentView(scroll);val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};row.addView(button("‹"){if(session!=null){modeVersion++;home()} else finish()},LinearLayout.LayoutParams(dp(48),dp(48)));row.addView(label("QUIZ ARENA\nPREMIUM",19,gold),LinearLayout.LayoutParams(0,-2,1f));row.addView(button(if(audioEnabled)"♫" else "×"){audioEnabled=!audioEnabled;prefs.edit().putBoolean("audio",audioEnabled).apply();stopVoice();if(session!=null)render() else home()},LinearLayout.LayoutParams(dp(48),dp(48)));add(row);add(label("TESTIM DHE MËSIM",14,blue));status=label(if(isOfflineSession())"USHTRIM OFFLINE • Shqip • pikë vetëm lokale" else if(live)"● ARENA LIVE" else localStatus(),11,blue);add(status)}
    override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=0xff080d27.toInt();window.navigationBarColor=0xff080d27.toInt();locale=prefs.getString("locale","sq")?:"sq";audioEnabled=prefs.getBoolean("audio",true);ads=NativeGameAds(this,"quiz_arena");sound=runCatching{android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC,45)}.getOrNull();bootstrap=runCatching{JSONObject(prefs.getString("bootstrap","")?:"")}.getOrNull();tts=android.speech.tts.TextToSpeech(this){result->ttsReady=result==android.speech.tts.TextToSpeech.SUCCESS};home();localStore.saved()?.let{offline=true;accept(it)};if(!qa()||qaTransport!=null)refresh()}
    private fun call(action:String,data:JSONObject=JSONObject(),done:(JSONObject)->Unit){
        if(busy)return
        if(!accounts.isSignedIn()&&!(qa()&&qaTransport!=null)){live=false;if(session==null)home();return}
        busy=true;val version=modeVersion;status.text="Po provohet LIVE… Mund të luash OFFLINE."
        val payload=JSONObject(data.toString()).put("locale",locale)
        if(accounts.isSignedIn())payload.put("session_token",accounts.requireToken())
        network.execute{val result=runCatching{if(qa()&&qaTransport!=null)qaTransport!!.invoke(action,payload) else client.request(action,payload)}
            runOnUiThread{busy=false;if(isFinishing||isDestroyed||version!=modeVersion)return@runOnUiThread
                result.onSuccess{live=true;done(it)}.onFailure{live=false;if(session==null)home() else {status.text=if(isOfflineSession())"USHTRIM OFFLINE • Shqip" else "LIVE nuk përgjigjet. Sesioni LIVE mbetet i ruajtur.";add(button("VAZHDO NË USHTRIM OFFLINE"){startOffline(localStore.saved()!=null)})}}
            }
        }
    }
    private fun refresh(){call("arenaBootstrap"){if(it.optInt("available",0)<=0||it.optJSONArray("categories")==null){live=false;if(session==null)home();return@call};bootstrap=it;prefs.edit().putString("bootstrap",it.toString()).apply();if(session==null)home() else if(isOfflineSession())status.text="USHTRIM OFFLINE • LIVE u rikthye; hapet nga ballina pa humbur progresin lokal."}}
    private fun home(){persistLocal();timer?.cancel();stopVoice();session=null;offline=!live;shell();val hero=FrameLayout(this);val art=StudioArtwork(this,"arena");hero.addView(art,FrameLayout.LayoutParams(-1,-1));val overlay=LinearLayout(this).apply{orientation=1;gravity=Gravity.BOTTOM;setPadding(dp(18),dp(12),dp(18),dp(14));background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x00000000,0xee101635.toInt()))};overlay.addView(label("Hyr në skenën e diturisë",25,gold));overlay.addView(label("Mëso pas çdo përgjigjeje.\nPërparo, sfido veten, ndriço arenën.",14));hero.addView(overlay,FrameLayout.LayoutParams(-1,-1));add(hero,208);hero.alpha=0f;hero.animate().alpha(1f).setDuration(450).start()
        val profile=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};profile.addView(PlayerPortrait.image(this).apply{setOnClickListener{PlayerPortrait.pick(this@ArenaCloudActivity)}},LinearLayout.LayoutParams(dp(68),dp(68)));val name=EditText(this).apply{hint="Emri i lojtarit";setText(prefs.getString("name",accounts.currentName()?:"Lojtari"));setTextColor(Color.WHITE);setHintTextColor(0xffb5c5e6.toInt());setSingleLine();filters=arrayOf(android.text.InputFilter.LengthFilter(80))};profile.addView(name,LinearLayout.LayoutParams(0,-2,1f));add(profile);add(button("ZGJIDH AVATARIN / FOTON"){PlayerPortrait.pick(this)})
        val languages=arrayOf("Shqip","Deutsch","English","Türkçe");val codes=arrayOf("sq","de","en","tr");val spinner=Spinner(this).apply{adapter=ArrayAdapter(this@ArenaCloudActivity,android.R.layout.simple_spinner_dropdown_item,languages);setSelection(codes.indexOf(locale).coerceAtLeast(0));background=gradient(0xffc5e2ff.toInt(),0xffaabfff.toInt())};var initial=true;spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,position:Int,id:Long){if(initial){initial=false;return};locale=codes[position];prefs.edit().putString("locale",locale).apply();category="";refresh()}};add(spinner,48)
        add(button("RUAJ LOJTARIN"){val n=name.text.toString().trim();if(n.length<2){name.error="Të paktën 2 shkronja";return@button};prefs.edit().putString("name",n).apply();if(live)call("arenaRegister",JSONObject().put("name",n)){refresh()} else {status.text="Emri u ruajt në telefon • USHTRIM OFFLINE"}})
        val progress=if(live)bootstrap?.optJSONObject("progress") else localStore.progress();val level=progress?.optInt("level",1)?:1;add(label("NIVELI $level  •  ★ ${progress?.optInt("points",0)?:0} PIKË",20,gold));val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=20;this.progress=if(live)progress?.optJSONArray("completed")?.length()?:0 else (level-1).coerceAtLeast(0);progressTintList=android.content.res.ColorStateList.valueOf(blue)};add(bar)
        val saved=bootstrap?.optString("resume","").orEmpty().takeUnless{it=="null"}.orEmpty().ifEmpty{prefs.getString("active_id","")?:""};if(saved.isNotEmpty())add(button("▶ VAZHDO LOJËN E RUAJTUR"){call("arenaResume",JSONObject().put("session_id",saved)){accept(it);retryAnswer()}})
        if(localStore.saved()!=null)add(button("▶ VAZHDO USHTRIMIN OFFLINE"){startOffline(true)})
        val categories=if(live)bootstrap?.optJSONArray("categories")?:JSONArray() else JSONArray(localStore.categories());add(label("KATEGORITË",14,blue));val options=mutableListOf("Të gjitha");for(i in 0 until categories.length())options.add(categories.getString(i));val cats=Spinner(this).apply{adapter=ArrayAdapter(this@ArenaCloudActivity,android.R.layout.simple_spinner_dropdown_item,options);background=gradient(0xffc5e2ff.toInt(),0xffaabfff.toInt())};cats.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){category=if(pos==0)"" else options[pos]}};add(cats,48)
        add(label(if(live)"${bootstrap?.optInt("available",0)?:0} pyetje LIVE në gjuhën e zgjedhur" else "Pyetje lokale të gatshme: ${localStore.questions.size} • Shqip",12));add(button(if(live)"HYR NË ARENË • NIVELI $level" else "HYR NË USHTRIM OFFLINE • NIVELI $level"){val n=name.text.toString().trim();if(n.length<2){name.error="Shkruaj emrin";return@button};prefs.edit().putString("name",n).apply();if(!live){startOffline();return@button};call("arenaRegister",JSONObject().put("name",n)){call("arenaStart",JSONObject().put("level",level).put("category",category)){accept(it)}}})
        add(button("🏆 RENDITJA E LOJTARËVE"){if(!live){status.text="Renditja kërkon LIVE. Rekordi lokal: ${localStore.progress().optInt("best")} pikë.";return@button};call("arenaLeaderboard"){shell();add(label("RENDІTJA",25,gold));val rows=it.getJSONArray("ranking");if(rows.length()==0)add(label("Bëhu lojtari i parë që përfundon një nivel."));for(i in 0 until rows.length()){val r=rows.getJSONObject(i);add(label("${r.getInt("rank")}. ${r.getString("name")}   ★ ${r.getInt("points")}   • N${r.getInt("level")}"))};add(button("KTHEHU"){home()})}})
        add(button("RIPROVO LIDHJEN LIVE"){refresh()});add(button("USHTRIM OFFLINE • PA RENDITJE"){startOffline(localStore.saved()!=null)});add(label("Zëri i prezantuesit gjenerohet me AI. Recitimet kërkojnë audio autentike. Pikët e arenës janë rezultat loje.",11,0xffb9c9df.toInt()));ads.banner(root)
    }
    private fun accept(v:JSONObject){session=v;offline=v.optBoolean("offline");deadline=android.os.SystemClock.elapsedRealtime()+v.optLong("remaining_ms");if(offline)localStore.save(v) else prefs.edit().putString("session",v.toString()).putString("active_id",v.optString("session_id")).apply();render()}
    private fun render(){val s=session?:return;timer?.cancel();stopVoice();shell();val config=s.getJSONObject("config");add(label("${if(isOfflineSession())"USHTRIM OFFLINE" else "LIVE"} • NIVELI ${s.getInt("level")}  •  ${s.getInt("index")+1}/${s.getInt("total")}  •  ★ ${s.getInt("score")}",14,gold));scene=QuizArenaScene(this).apply{playerName=prefs.getString("name","")?:"";portrait=PlayerPortrait.load(this@ArenaCloudActivity);reaction=if(s.optJSONObject("feedback")?.optBoolean("right")==true)1 else if(s.optJSONObject("feedback")!=null)-1 else 0};add(scene!!,180)
        if(s.optBoolean("finished")){if(!isOfflineSession())prefs.edit().remove("active_id").remove("pending_answer").apply();add(label(if(s.optBoolean("passed"))"NIVELI U KALUA!" else "PROVO PËRSËRI",27,gold));add(label("${s.getInt("correct")} / ${s.getInt("total")} të sakta • ★ ${s.getInt("score")}",21));scene?.reaction=if(s.optBoolean("passed"))1 else -1;root.translationY=dp(22).toFloat();root.animate().translationY(0f).setDuration(550).start();add(button("VAZHDO • KAPËRCE SKENËN"){home();refresh()});val video=config.optString("video");if(video.startsWith("https://"))add(button("SHIKO VIDEON E KALIMIT"){video(video)});ads.naturalBreak();return}
        val q=s.getJSONObject("question");add(label(q.getString("category"),12,blue));add(label(q.getString("prompt"),22));val feedback=s.optJSONObject("feedback");val choices=q.getJSONArray("choices");for(i in 0 until choices.length()){val option=button("${'A'+i}   ${choices.getString(i)}"){sendAnswer(i)};option.gravity=Gravity.START or Gravity.CENTER_VERTICAL;option.isEnabled=feedback==null;if(feedback!=null&&i==feedback.getInt("correct"))option.background=gradient(0xff0b9a76.toInt(),0xff146b63.toInt());add(option)}
        if(feedback!=null){add(label(if(feedback.getBoolean("right"))"✓ SAKTË" else "GABIM • PËRGJIGJJA E SAKTË: ${'A'+feedback.getInt("correct")}",20,gold));add(label(feedback.getString("explanation"),17));add(label(feedback.optString("source"),12,blue));add(button("DËGJO SHPJEGIMIN AI"){voice("explanation")});add(button("VAZHDO ›"){if(isOfflineSession())accept(localStore.next(s)) else call("arenaNext",JSONObject().put("session_id",s.getString("session_id"))){accept(it)}});if(audioEnabled)voice("explanation")}
        else{val clock=label("",15,gold);add(clock);fun update(){clock.text="⏱ ${((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)+999)/1000}s • ${config.getInt("points")} pikë për saktë"};update();timer=object:android.os.CountDownTimer((deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1),200){override fun onTick(ms:Long){update();val sec=(ms/1000).toInt();if(sec!=tick){tick=sec;if(sec<10&&audioEnabled)sound?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,60)}};override fun onFinish(){clock.text="Koha përfundoi";if(resumed)sendAnswer(-1)}}.start();add(button("DËGJO PYETJEN AI"){voice("question")});val rec=q.optString("recitation");if(rec.startsWith("https://"))add(button("DËGJO RECITIMIN AUTENTIK"){playUrl(rec)});add(button("RIPROVO / SINKRONIZO"){retryAnswer()})}
    }
    private fun sendAnswer(choice:Int){val s=session?:return;if(isOfflineSession()){timer?.cancel();persistLocal();val answered=localStore.answer(s,choice);if(audioEnabled&&answered.optJSONObject("feedback")?.optBoolean("right")==true){applause?.release();applause=android.media.MediaPlayer.create(this,R.raw.arena_applause);applause?.start()};accept(answered);return};val q=s.optJSONObject("question")?:return;val data=JSONObject().put("session_id",s.getString("session_id")).put("question_id",q.getString("id")).put("choice",choice);prefs.edit().putString("pending_answer",data.toString()).commit();stopVoice();call("arenaAnswer",data){prefs.edit().remove("pending_answer").apply();if(audioEnabled&&it.optJSONObject("feedback")?.optBoolean("right")==true){applause?.release();applause=android.media.MediaPlayer.create(this,R.raw.arena_applause);applause?.start()};accept(it)}}
    private fun retryAnswer(){if(isOfflineSession()){render();return};val p=runCatching{JSONObject(prefs.getString("pending_answer","")?:"")}.getOrNull();if(p!=null)call("arenaAnswer",p){prefs.edit().remove("pending_answer").apply();accept(it)}else session?.let{call("arenaResume",JSONObject().put("session_id",it.getString("session_id"))){v->accept(v)}}}
    private fun stopVoice(){tts?.stop();voiceGeneration++;player?.release();player=null;scene?.speaking=false}
    private fun ttsFallback(phase:String){
        if(!audioEnabled)return;val s=session?:return;val q=s.optJSONObject("question")?:return
        val lang=if(isOfflineSession())"sq" else locale
        if(!ttsReady||(tts?.isLanguageAvailable(java.util.Locale(lang))?:-1)<0){status.text="Audioja nuk është e disponueshme. Loja vazhdon me tekst.";return}
        tts?.language=java.util.Locale(lang);val f=s.optJSONObject("feedback")
        val text=if(phase=="explanation"&&f!=null)q.getJSONArray("choices").optString(f.getInt("correct"))+". "+f.optString("explanation") else q.optString("prompt")+". "+(0 until q.getJSONArray("choices").length()).joinToString(". "){"${'A'+it}. "+q.getJSONArray("choices").getString(it)}
        tts?.speak(text,android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"arena-fallback")
    }
    private fun voice(phase:String){
        if(!audioEnabled)return;val s=session?:return;stopVoice();if(isOfflineSession()){ttsFallback(phase);return}
        val generation=voiceGeneration;val version=modeVersion
        val q=s.optJSONObject("question")?:return
        val cacheKey=java.security.MessageDigest.getInstance("SHA-256").digest((locale+q.toString()+s.optJSONObject("feedback").toString()+phase).toByteArray()).joinToString(""){"%02x".format(it)}
        val cachedAudio=java.io.File(cacheDir,"arena-voice-$cacheKey.mp3")
        fun playCached(){player=android.media.MediaPlayer().apply{setDataSource(cachedAudio.absolutePath);setOnPreparedListener{scene?.speaking=true;it.start()};setOnCompletionListener{scene?.speaking=false};setOnErrorListener{_,_,_->cachedAudio.delete();ttsFallback(phase);true};prepareAsync()}}
        if(cachedAudio.exists()){runCatching{playCached()}.onFailure{cachedAudio.delete();ttsFallback(phase)};return}
        if(!accounts.isSignedIn()){ttsFallback(phase);return}
        val payload=JSONObject().put("session_id",s.getString("session_id")).put("phase",phase).put("locale",locale).put("session_token",accounts.requireToken())
        voiceNetwork.execute{val r=runCatching{voiceClient.request("arenaVoice",payload)};runOnUiThread{if(isFinishing||isDestroyed||!resumed||generation!=voiceGeneration||version!=modeVersion)return@runOnUiThread
            r.onSuccess{runCatching{cachedAudio.writeBytes(android.util.Base64.decode(it.getString("audio"),android.util.Base64.DEFAULT));playCached()}.onFailure{ttsFallback(phase)}}.onFailure{ttsFallback(phase)}
        }}
    }
    private fun playUrl(url:String){stopVoice();player=android.media.MediaPlayer().apply{setDataSource(url);setOnPreparedListener{it.start()};setOnErrorListener{_,_,_->status.text="Audioja nuk është e disponueshme.";true};prepareAsync()}}
    private fun video(url:String){timer?.cancel();stopVoice();shell();val view=VideoView(this);add(view,230);val controls=MediaController(this);view.setMediaController(controls);controls.setAnchorView(view);view.setVideoURI(android.net.Uri.parse(url));view.setOnPreparedListener{it.start()};view.setOnCompletionListener{home();refresh()};view.setOnErrorListener{_,_,_->status.text="Videoja mungon. Progresi është ruajtur.";true};add(button("KAPËRCE • VAZHDO"){view.stopPlayback();home();refresh()})}
    @Deprecated("Activity back navigation") override fun onBackPressed(){if(session!=null){modeVersion++;home()}else super.onBackPressed()}
    override fun onActivityResult(code:Int,result:Int,data:android.content.Intent?){super.onActivityResult(code,result,data);if(code==PlayerPortrait.REQUEST&&PlayerPortrait.receive(this,result,data))home()}
    override fun onResume(){super.onResume();resumed=true;ads.resume();if(isOfflineSession()){deadline=android.os.SystemClock.elapsedRealtime()+(session?.optLong("remaining_ms")?:0);render()};if(!qa()){retryHandler.removeCallbacks(retry);retryHandler.postDelayed(retry,1000)}}
    override fun onPause(){persistLocal();timer?.cancel();retryHandler.removeCallbacks(retry);resumed=false;stopVoice();applause?.pause();ads.pause();super.onPause()}
    override fun onDestroy(){retryHandler.removeCallbacks(retry);tts?.shutdown();network.shutdownNow();voiceNetwork.shutdownNow();timer?.cancel();stopVoice();sound?.release();applause?.release();ads.destroy();super.onDestroy()}
}
