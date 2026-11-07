package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ArenaLearningGamesTest {
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))} else emptyList()
    private fun click(a:Activity,text:String){inst.runOnMainSync{views(a.window.decorView).filterIsInstance<Button>().first{it.text.toString()==text}.performClick()};inst.waitForIdleSync()}
    @Test fun associationCluesScoreOnceAndSurviveReopening(){
        val context=inst.targetContext
        val prefs=context.getSharedPreferences("arena_learning_guest",0);prefs.edit().clear().commit()
        fun launch()=inst.startActivitySync(Intent(context,ArenaLearningGamesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("native_qa",true))
        var a=launch();click(a,"◆ Gjeje fjalën e duhur")
        val round=JSONObject(prefs.getString("round_word","{}")!!)
        assertEquals(9,round.getJSONArray("rounds").getJSONObject(0).getJSONArray("clues").length())
        click(a,"A1");click(a,"B2");click(a,"C3")
        assertEquals(273,JSONObject(prefs.getString("round_word","{}")!!).getInt("revealed"))
        click(a,"GJEJ ZGJIDHJEN FINALE")
        val dialogRoot=inst.uiAutomation.rootInActiveWindow
        val input=dialogRoot.findAccessibilityNodeInfosByText("Shkruaj zgjidhjen").first()
        val args=android.os.Bundle().apply{
            putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Sureja El-Asr")
        }
        assertTrue(input.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,args))
        assertTrue(dialogRoot.findAccessibilityNodeInfosByText("Kontrollo").first()
            .performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        inst.waitForIdleSync()
        val dir=java.io.File(context.getExternalFilesDir(null),"native-qa").apply{mkdirs()}
        val screenshot=inst.uiAutomation.takeScreenshot()
        java.io.File(dir,"22-arena-word-game-ilaz-presenter.png").outputStream().use{screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        screenshot.recycle()
        val scored=JSONObject(prefs.getString("round_word","{}")!!)
        assertEquals(10,scored.getInt("score"));assertTrue(scored.getBoolean("answered"))
        inst.runOnMainSync{a.finish()};inst.waitForIdleSync()
        a=launch();click(a,"◆ Gjeje fjalën e duhur")
        assertEquals(10,JSONObject(prefs.getString("round_word","{}")!!).getInt("score"))
        click(a,"VAZHDO ›")
        assertEquals(1,JSONObject(prefs.getString("round_word","{}")!!).getInt("index"))
        inst.runOnMainSync{a.finish()}
    }
    @Test fun fiftyWorksWithThreeChoicesAndSurvivesSaving(){
        val ctx=inst.targetContext;val store=ArenaOfflineStore(ctx,"three-choice-help")
        val s=store.start("");val q=s.getJSONObject("question")
        q.put("choices",org.json.JSONArray(listOf("One","Two","Three"))).put("correct",1)
        s.put("presentation_pending",false)
        val help=ArenaLifelines.use(s,"fifty")
        assertEquals(1,help.getJSONArray("hidden").length())
        assertFalse(ArenaLifelines.hidden(s).contains(1))
        store.save(s)
        val restored=store.saved()!!
        assertTrue(ArenaLifelines.used(restored,"fifty"))
        assertEquals(ArenaLifelines.hidden(s),ArenaLifelines.hidden(restored))
    }
    @Test fun normalQuestionsOpenMissingWordChallengeWithoutChangingRankedScore(){
        val ctx=inst.targetContext
        ctx.getSharedPreferences("arena_cloud_guest",0).edit().clear().putBoolean("audio",false).commit()
        val store=ArenaOfflineStore(ctx,"guest");val s=store.start("")
        s.put("index",1).put("question",s.getJSONArray("local_questions").getJSONObject(1)).put("presentation_pending",false)
        store.answer(s,s.getJSONObject("question").getInt("correct"))
        val score=s.getInt("score")
        val monitor=inst.addMonitor(ArenaLearningGamesActivity::class.java.name,null,false)
        val a=inst.startActivitySync(Intent(ctx,ArenaCloudActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("native_qa",true))
        try {
            click(a,"VAZHDO ›")
            val challenge=monitor.waitForActivityWithTimeout(4000)
            assertNotNull(challenge)
            assertEquals(1,store.saved()!!.getInt("index"));assertEquals(score,store.saved()!!.getInt("score"))
            assertTrue(views(challenge.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.contains("[ … ]")})
            inst.runOnMainSync{challenge.finish()};inst.waitForIdleSync()
            assertEquals(1,store.saved()!!.getInt("index"));assertEquals(score,store.saved()!!.getInt("score"))
        }finally{inst.removeMonitor(monitor);inst.runOnMainSync{a.finish()}}
    }
    @Test fun verseCompletionAlwaysRestoresExactlyTheSourcedVerse(){
        val ctx=inst.targetContext
        val verses=ctx.assets.open("quiz-arena/verse-completion-sq.json").bufferedReader().use{JSONObject(it.readText()).getJSONArray("verses")}
        for(i in 0 until verses.length()){
            val row=verses.getJSONObject(i);assertTrue(row.getString("url").startsWith("https://quran.com/"))
            val words=row.getString("arabic").split(" ")
            for(index in words.indices){val masked=words.mapIndexed{n,w->if(n==index)"[ … ]" else w}.joinToString(" ");assertEquals(row.getString("arabic"),masked.replace("[ … ]",words[index]))}
        }
    }
}
