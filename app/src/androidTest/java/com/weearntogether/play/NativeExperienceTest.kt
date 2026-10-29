package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*

/** Runs the actual Android screens, not an HTML reproduction. */
class NativeExperienceTest {
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private fun launch(type:Class<out Activity>):Activity {
        val intent=Intent(inst.targetContext,type).putExtra("native_qa",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return inst.startActivitySync(intent).also { inst.waitForIdleSync() }
    }
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))} else emptyList()
    private fun shot(name:String){inst.waitForIdleSync();android.os.SystemClock.sleep(500);inst.waitForIdleSync();val bitmap=inst.uiAutomation.takeScreenshot();assertNotNull(bitmap);val dir=java.io.File(inst.targetContext.getExternalFilesDir(null),"native-qa").apply{mkdirs()};java.io.File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
    @org.junit.Before fun resetArenaTestState(){
        ArenaCloudActivity.qaTransport=null
        inst.targetContext.getSharedPreferences("arena_offline_v1_guest",0).edit().clear().commit()
        inst.targetContext.getSharedPreferences("arena_cloud_guest",0).edit().clear().commit()
    }
    @Test fun premiumHomeDestinationsLaunchWithoutRuntimeCrash(){
        val destinations=listOf(
            BlockPuzzleActivity::class.java,
            BubbleShooterActivity::class.java,
            BallSortActivity::class.java,
            CupShuffleActivity::class.java,
            QuizArenaActivity::class.java,
            ArenaCloudActivity::class.java,
            Html5GamesActivity::class.java,
            GamePixActivity::class.java,
            AyetOfferwallActivity::class.java,
            WalletActivity::class.java,
            MainActivity::class.java
        )
        destinations.forEach { type ->
            val a=launch(type)
            try {
                val requiresAccount=type==Html5GamesActivity::class.java||type==WalletActivity::class.java
                if(requiresAccount&&!AccountStore(inst.targetContext).isSignedIn()){
                    assertTrue("${type.simpleName} must retain its signed-out access guard",a.isFinishing)
                }else assertFalse("${type.simpleName} finished during launch",a.isFinishing)
                assertNotNull("${type.simpleName} did not create a window",a.window?.decorView)
            } finally { inst.runOnMainSync{a.finish()} }
        }
    }
    @Test fun offlineEngineValidatesGradesResumesAndNeverDuplicatesPoints(){
        val ctx=inst.targetContext;ctx.getSharedPreferences("arena_offline_v1_qa_engine",0).edit().clear().commit()
        val store=ArenaOfflineStore(ctx,"qa_engine");assertTrue(store.questions.size>=80);assertTrue(store.questions.any{it.id.startsWith("general-")});assertTrue(store.categories().all{category->store.questions.any{it.category==category}})
        var state=store.start("");val q=state.getJSONObject("question");store.answer(state,q.getInt("correct"));val score=state.getInt("score");store.answer(state,q.getInt("correct"));assertEquals(score,state.getInt("score"))
        val reopened=ArenaOfflineStore(ctx,"qa_engine").saved()!!;assertEquals(state.toString(),reopened.toString());assertNotNull(reopened.optJSONObject("feedback"))
        state=store.next(reopened);store.answer(state,-1);assertFalse(state.getJSONObject("feedback").getBoolean("right"));assertTrue(state.getJSONObject("feedback").getString("explanation").isNotBlank())
        while(!state.optBoolean("finished")){if(state.optJSONObject("feedback")==null)store.answer(state,state.getJSONObject("question").getInt("correct"));state=store.next(state)}
        val total=store.progress().getInt("points");store.next(state);assertEquals(total,store.progress().getInt("points"));assertEquals(2,store.progress().getInt("level"));assertNull(store.saved());assertNull(ArenaOfflineStore(ctx,"another-user").saved())
    }
    @Test fun failedBackendStillStartsAndRestoresFullOfflineArena(){
        ArenaCloudActivity.qaTransport={_,_->throw BackendException("TIMEOUT","simulated Apps Script timeout")}
        var a=launch(ArenaCloudActivity::class.java)
        try{
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("HYR NË USHTRIM OFFLINE")}.performClick()};inst.waitForIdleSync();shot("06-arena-offline-question")
            assertTrue(views(a.window.decorView).filterIsInstance<TextView>().any{it.text.contains("USHTRIM OFFLINE")})
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("A   ")}.performClick()};inst.waitForIdleSync();shot("07-arena-offline-explanation")
            val saved=ArenaOfflineStore(inst.targetContext,"guest").saved()!!;assertNotNull(saved.optJSONObject("feedback"));assertEquals(1,saved.getJSONArray("answers").length())
            inst.runOnMainSync{a.finish()};a=launch(ArenaCloudActivity::class.java);assertTrue(views(a.window.decorView).filterIsInstance<Button>().any{it.text=="VAZHDO ›"});assertEquals(saved.getString("session_id"),ArenaOfflineStore(inst.targetContext,"guest").saved()!!.getString("session_id"))
            inst.runOnMainSync{a.onBackPressed()};assertTrue(views(a.window.decorView).filterIsInstance<Button>().any{it.text.contains("VAZHDO USHTRIMIN OFFLINE")})
        }finally{inst.runOnMainSync{a.finish()};ArenaCloudActivity.qaTransport=null}
    }
    @Test fun expiredTimerAndMissingAudioKeepOfflineControlsUsable(){
        val store=ArenaOfflineStore(inst.targetContext,"guest");val state=store.start("");state.put("remaining_ms",1).put("presentation_pending",false);store.save(state)
        val a=launch(ArenaCloudActivity::class.java)
        try{android.os.SystemClock.sleep(250);inst.waitForIdleSync();val saved=store.saved()!!;assertEquals(0,saved.getJSONArray("answers").length());assertNull(saved.optJSONObject("feedback"));assertTrue(saved.optBoolean("timed_out"));assertFalse(views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("A   ")}.isEnabled)
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text=="SHFAQ PËRGJIGJEN DHE SHPJEGIMIN"}.performClick()};inst.waitForIdleSync();assertEquals(-1,store.saved()!!.getJSONObject("feedback").getInt("choice"));assertTrue(views(a.window.decorView).filterIsInstance<TextView>().any{it.text=="KOHA PËRFUNDOI • PA PËRGJIGJE"})
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text=="DËGJO SHPJEGIMIN AI"}.performClick()};inst.waitForIdleSync();assertTrue(views(a.window.decorView).filterIsInstance<Button>().any{it.text=="VAZHDO ›"&&it.isEnabled})
        }finally{inst.runOnMainSync{a.finish()}}
    }
    @Test fun liveRecoveryPreservesSeparateOfflineSession(){
        val local=ArenaOfflineStore(inst.targetContext,"guest").start("");val id=local.getString("session_id")
        ArenaCloudActivity.qaTransport={action,_->if(action=="arenaBootstrap")org.json.JSONObject("{\"available\":12,\"categories\":[\"Logic\"],\"progress\":{\"level\":1,\"points\":0}}") else throw BackendException("TEST_ONLY","unsupported fixture")}
        val a=launch(ArenaCloudActivity::class.java)
        try{android.os.SystemClock.sleep(400);inst.waitForIdleSync();assertEquals(id,ArenaOfflineStore(inst.targetContext,"guest").saved()!!.getString("session_id"));inst.runOnMainSync{a.onBackPressed()};inst.waitForIdleSync();assertTrue(views(a.window.decorView).filterIsInstance<TextView>().any{it.text.contains("ARENA LIVE")});assertTrue(views(a.window.decorView).filterIsInstance<Button>().any{it.text.contains("VAZHDO USHTRIMIN OFFLINE")})
        }finally{inst.runOnMainSync{a.finish()};ArenaCloudActivity.qaTransport=null}
    }
    @Test fun timedOutLiveNextCanRetryWithoutSkippingQuestion(){
        val ctx=inst.targetContext;ctx.getSharedPreferences("arena_cloud_guest",0).edit().putBoolean("audio",false).commit()
        var index=0;var nextCalls=0
        fun state(feedback:Boolean=false)=org.json.JSONObject().put("session_id","qa-live").put("level",1).put("score",if(feedback||index>0)100 else 0).put("correct",if(feedback||index>0)1 else 0).put("index",index).put("total",2).put("remaining_ms",60000).put("config",org.json.JSONObject().put("points",100)).put("question",org.json.JSONObject().put("id","q$index").put("category","Logic").put("prompt",if(index==0)"First LIVE question" else "Second LIVE question").put("choices",org.json.JSONArray(listOf("One","Two","Three","Four")))).apply{if(feedback)put("feedback",org.json.JSONObject().put("right",true).put("correct",0).put("explanation","Educational explanation").put("source","Fixture"))}
        ArenaCloudActivity.qaTransport={action,p->when(action){
            "arenaBootstrap"->org.json.JSONObject("{\"available\":12,\"categories\":[\"Logic\"],\"progress\":{\"level\":1,\"points\":0}}")
            "arenaRegister"->org.json.JSONObject()
            "arenaStart"->state()
            "arenaAnswer"->state(true)
            "arenaNext"->{assertEquals("q0",p.getString("question_id"));index=1;nextCalls++;if(nextCalls==1)throw BackendException("TIMEOUT","response lost after server advanced");state()}
            "arenaResume"->state()
            else->throw BackendException("TEST_ONLY","unsupported fixture")
        }}
        val a=launch(ArenaCloudActivity::class.java)
        fun waitFor(text:String){val end=android.os.SystemClock.elapsedRealtime()+5000;while(android.os.SystemClock.elapsedRealtime()<end){inst.waitForIdleSync();if(views(a.window.decorView).any{(it is TextView&&it.text.contains(text))||(it is QuizArenaScene&&it.questionPrompt.contains(text))})return;android.os.SystemClock.sleep(40)};fail("Missing UI: $text")}
        try{
            waitFor("HYR NË ARENË")
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("HYR NË ARENË")}.performClick()};waitFor("First LIVE question")
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("A   ")}.performClick()};waitFor("Educational explanation")
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text=="VAZHDO ›"}.performClick()};waitFor("RIPROVO / VAZHDO LIVE")
            assertTrue(ctx.getSharedPreferences("arena_cloud_guest",0).contains("pending_next"))
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text=="RIPROVO / VAZHDO LIVE"}.performClick()};waitFor("Second LIVE question")
            assertEquals(2,nextCalls);assertFalse(ctx.getSharedPreferences("arena_cloud_guest",0).contains("pending_next"));shot("08-arena-live-next-recovered")
        }finally{inst.runOnMainSync{a.finish()};ArenaCloudActivity.qaTransport=null}
    }
    @Test fun liveStudioAutoReadsThenStartsClockAndNeverAutoSelects(){
        val ctx=inst.targetContext;var readyCalls=0;var answerCalls=0;var voiceCalls=0
        val wav=java.nio.ByteBuffer.allocate(40044).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply{put("RIFF".toByteArray());putInt(40036);put("WAVEfmt ".toByteArray());putInt(16);putShort(1);putShort(1);putInt(8000);putInt(16000);putShort(2);putShort(16);put("data".toByteArray());putInt(40000)}.array()
        fun state(presentation:Boolean,answered:Boolean=false)=org.json.JSONObject().put("session_id","qa-presentation").put("level",1).put("score",0).put("correct",0).put("index",0).put("total",2).put("remaining_ms",1000).put("presentation_pending",presentation).put("presentation_remaining_ms",90000).put("config",org.json.JSONObject().put("points",100).put("seconds",1)).put("question",org.json.JSONObject().put("id","q1").put("category","Logic").put("prompt","Pyetja në ekranin qendror të studios").put("choices",org.json.JSONArray(listOf("One","Two","Three","Four")))).apply{if(answered)put("feedback",org.json.JSONObject().put("right",false).put("choice",-1).put("reason","timeout").put("correct",1).put("explanation","Explanation after explicit request").put("source","Fixture"))}
        ArenaCloudActivity.qaTransport={action,p->when(action){
            "arenaBootstrap"->org.json.JSONObject("{\"available\":12,\"categories\":[\"Logic\"],\"progress\":{\"level\":1,\"points\":0}}")
            "arenaRegister"->org.json.JSONObject()
            "arenaStart"->state(true)
            "arenaVoice"->{voiceCalls++;org.json.JSONObject().put("audio",android.util.Base64.encodeToString(wav,android.util.Base64.NO_WRAP))}
            "arenaReady"->{readyCalls++;assertEquals(2,p.getInt("protocol"));state(false)}
            "arenaAnswer"->{answerCalls++;assertEquals(-1,p.getInt("choice"));state(false,true)}
            else->throw BackendException("TEST_ONLY","unsupported fixture")
        }}
        val a=launch(ArenaCloudActivity::class.java)
        fun waitUntil(condition:()->Boolean){val end=android.os.SystemClock.elapsedRealtime()+6000;while(android.os.SystemClock.elapsedRealtime()<end){inst.waitForIdleSync();if(condition())return;android.os.SystemClock.sleep(30)};fail("Studio state did not arrive")}
        try{
            waitUntil{views(a.window.decorView).filterIsInstance<Button>().any{it.text.startsWith("HYR NË ARENË")}}
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("HYR NË ARENË")}.performClick()}
            waitUntil{views(a.window.decorView).filterIsInstance<QuizArenaScene>().any{it.speaking}}
            val scene=views(a.window.decorView).filterIsInstance<QuizArenaScene>().first();assertTrue(scene.questionPrompt.contains("ekranin qendror"));assertEquals("LEXIM",scene.timerText);assertEquals(0,readyCalls);assertEquals(0,answerCalls);assertFalse(views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("A   ")}.isEnabled);shot("09-studio-reading-question")
            waitUntil{readyCalls==1&&views(a.window.decorView).filterIsInstance<QuizArenaScene>().any{it.timerText=="00:01"}};shot("10-studio-answer-clock")
            waitUntil{views(a.window.decorView).filterIsInstance<Button>().any{it.text=="SHFAQ PËRGJIGJEN DHE SHPJEGIMIN"}}
            assertEquals(0,answerCalls);assertFalse(views(a.window.decorView).filterIsInstance<Button>().any{it.text=="VAZHDO ›"})
            inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text=="SHFAQ PËRGJIGJEN DHE SHPJEGIMIN"}.performClick()}
            waitUntil{views(a.window.decorView).filterIsInstance<TextView>().any{it.text=="KOHA PËRFUNDOI • PA PËRGJIGJE"}};assertEquals(1,answerCalls);assertTrue(voiceCalls>=1);shot("11-studio-timeout-explicit-explanation")
        }finally{inst.runOnMainSync{a.finish()};ArenaCloudActivity.qaTransport=null}
    }
    @Test fun premiumArenaHasRealPlayerLanguageAndResumeControls(){val a=launch(ArenaCloudActivity::class.java);try{
        val list=views(a.window.decorView);assertTrue(list.any{it is StudioArtwork});assertTrue(list.any{it is EditText});assertEquals(2,list.filterIsInstance<android.widget.Spinner>().size);assertTrue(list.filterIsInstance<TextView>().any{it.text.toString()=="TESTIM DHE MËSIM"});assertTrue(list.filterIsInstance<Button>().any{it.text.contains("AVATARIN")});shot("05-premium-arena-home")
    }finally{inst.runOnMainSync{a.finish()}}}
    @Test fun homeShowsIllustratedCardsAndBallSort(){val a=launch(PremiumHomeActivity::class.java);try{
        val list=views(a.window.decorView);assertTrue(list.any{it is StudioArtwork});assertTrue(list.filterIsInstance<TextView>().any{it.text.toString()=="Ball Sort"});assertFalse(list.filterIsInstance<TextView>().any{it.text.contains("NATIVE HD 2D +")});shot("01-home")
    }finally{inst.runOnMainSync{a.finish()}}}
    @Test fun quizRequiresNameAndLoadsExistingSheetSnapshot(){val a=launch(QuizArenaActivity::class.java);try{
        val bank=QuizBank(inst.targetContext).cached();assertTrue(bank.questions.size>=80);assertTrue(bank.questions.any{it.choices.size==2});assertTrue(bank.questions.any{it.choices.size==5})
        inst.runOnMainSync{val list=views(a.window.decorView);list.filterIsInstance<EditText>().first().setText("Lojtari Test");list.filterIsInstance<Button>().first{it.text.startsWith("TË GJITHA")}.performClick()};inst.waitForIdleSync();shot("02-quiz-question")
        assertTrue(views(a.window.decorView).filterIsInstance<TextView>().any{it.text.contains("Lojtari Test")})
        inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text.startsWith("A   ")}.performClick()};inst.waitForIdleSync();shot("03-quiz-answer")
    }finally{inst.runOnMainSync{a.finish()}}}
    @Test fun ballSortHasRealRulesAndUndoAcrossLevels(){for(level in 1..100){val game=BallSortGame(level);val counts=game.tubes.flatten().groupingBy{it}.eachCount();assertEquals(game.colors,counts.size);assertTrue(counts.values.all{it==4});assertFalse(game.won());assertTrue(game.tubes.all{it.size<=4})}
        val g=BallSortGame(1,listOf(listOf(0,1),listOf(1),listOf(0),emptyList(),emptyList()));assertFalse(g.move(0,2));val before=g.tubes.map{it.toList()};assertTrue(g.move(0,1));assertTrue(g.undo());assertEquals(before,g.tubes)
        val a=launch(BallSortActivity::class.java);try{assertTrue(views(a.window.decorView).any{it is BallSortBoard});shot("04-ball-sort")}finally{inst.runOnMainSync{a.finish()}}
    }
}
