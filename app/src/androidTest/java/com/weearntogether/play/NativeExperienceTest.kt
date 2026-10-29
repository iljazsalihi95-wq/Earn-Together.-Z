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
