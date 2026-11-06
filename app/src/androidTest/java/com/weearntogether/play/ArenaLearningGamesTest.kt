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
    @Test fun typedAnswerScoresOnceAndSurvivesReopening(){
        val context=inst.targetContext
        val prefs=context.getSharedPreferences("arena_learning_guest",0);prefs.edit().clear().commit()
        fun launch()=inst.startActivitySync(Intent(context,ArenaLearningGamesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("native_qa",true))
        var a=launch();click(a,"◆ Gjeje fjalën e duhur")
        val round=JSONObject(prefs.getString("round_word","{}")!!)
        val answer=round.getJSONArray("rounds").getJSONObject(0).getString("answer")
        inst.runOnMainSync{views(a.window.decorView).filterIsInstance<EditText>().single().setText(answer)}
        click(a,"KONTROLLO FJALËN")
        val scored=JSONObject(prefs.getString("round_word","{}")!!)
        assertEquals(10,scored.getInt("score"));assertTrue(scored.getBoolean("answered"))
        inst.runOnMainSync{a.finish()};inst.waitForIdleSync()
        a=launch();click(a,"◆ Gjeje fjalën e duhur")
        assertEquals(10,JSONObject(prefs.getString("round_word","{}")!!).getInt("score"))
        click(a,"VAZHDO ›")
        assertEquals(1,JSONObject(prefs.getString("round_word","{}")!!).getInt("index"))
        inst.runOnMainSync{a.finish()}
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
