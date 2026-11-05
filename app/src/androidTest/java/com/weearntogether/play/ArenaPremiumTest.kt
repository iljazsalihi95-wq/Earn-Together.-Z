package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioFormat
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ArenaPremiumTest {
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))} else emptyList()
    @Test fun approvedSheetSnapshotHasAtLeast200DistinctQuestionsAndDifficulty(){
        val bank=ArenaOfflineStore(inst.targetContext,"premium-bank-test").questions
        assertTrue(bank.size>=200);assertEquals(bank.size,bank.map{it.prompt.trim().lowercase()}.distinct().size)
        assertTrue(bank.count{it.id.startsWith("premium-20261105-sq-")}==75)
        assertTrue(bank.any{it.difficulty==3});assertTrue(bank.all{it.source.isNotBlank()&&it.why.isNotBlank()&&it.correct in it.choices.indices})
    }
    @Test fun lifelinesSurviveReopeningAndCannotBeUsedTwiceInRound(){
        val ctx=inst.targetContext;ctx.getSharedPreferences("arena_offline_v1_premium-lifelines",0).edit().clear().commit()
        val store=ArenaOfflineStore(ctx,"premium-lifelines");val s=store.start("Teknologji");s.put("presentation_pending",false)
        val fifty=ArenaLifelines.use(s,"fifty");assertEquals(2,fifty.getJSONArray("hidden").length());assertFalse(ArenaLifelines.hidden(s).contains(s.getJSONObject("question").getInt("correct")))
        val friend=ArenaLifelines.use(s,"friend");assertTrue(friend.getBoolean("simulated"));assertFalse(friend.has("correct"))
        val audience=ArenaLifelines.use(s,"audience");val votes=audience.getJSONArray("votes");assertEquals(100,(0 until votes.length()).sumOf{votes.getInt(it)})
        store.save(s);val reopened=ArenaOfflineStore(ctx,"premium-lifelines").saved()!!;assertTrue(ArenaLifelines.used(reopened,"friend"));assertEquals(ArenaLifelines.hidden(s),ArenaLifelines.hidden(reopened))
        try{ArenaLifelines.use(reopened,"friend");fail("A second friend call must be refused")}catch(_:IllegalStateException){}
        store.answer(reopened,0);store.next(reopened);reopened.put("presentation_pending",false)
        try{ArenaLifelines.use(reopened,"friend");fail("Next must not reset round lifelines")}catch(_:IllegalStateException){}
    }
    @Test fun switchingQuestionKeepsRoundSizeScoreAndUniqueQuestionIds(){
        val store=ArenaOfflineStore(inst.targetContext,"premium-switch");val s=store.start("Teknologji");s.put("presentation_pending",false)
        val old=s.getJSONObject("question").getString("id");val count=s.getInt("total")
        ArenaLifelines.use(s,"switch");store.save(s)
        assertNotEquals(old,s.getJSONObject("question").getString("id"));assertEquals(count,s.getInt("total"));assertEquals(0,s.getInt("score"));assertTrue(s.getBoolean("presentation_pending"))
        val qs=s.getJSONArray("local_questions");assertEquals(qs.length(),(0 until qs.length()).map{qs.getJSONObject(it).getString("id")}.distinct().size)
        assertTrue(ArenaLifelines.used(ArenaOfflineStore(inst.targetContext,"premium-switch").saved()!!,"switch"))
    }
    @Test fun ttsMouthFollowsRealPcmIncludingSilence(){
        val pcm=ArenaPcmStream();pcm.configure(24000,AudioFormat.ENCODING_PCM_16BIT,1)
        val data=ByteBuffer.allocate(24000*2*3/10).order(ByteOrder.LITTLE_ENDIAN)
        repeat(7200){data.putShort(if(it in 2400 until 4800)12000.toShort() else 0)};pcm.append(data.array())
        assertEquals(0f,pcm.at(40),.001f);assertTrue(pcm.at(140)>.5f);assertEquals(0f,pcm.at(240),.001f)
    }
    @Test fun decodedPlaybackEnvelopeFollowsAudioTimeRatherThanAnimationClock(){
        val samples=7200;val bytes=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36+samples*2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(24000).putInt(48000).putShort(2).putShort(16).put("data".toByteArray()).putInt(samples*2)
        repeat(samples){bytes.putShort(if(it in 2400 until 4800)12000.toShort() else 0)}
        val file=File(inst.targetContext.cacheDir,"arena-envelope-test.wav");file.writeBytes(bytes.array())
        val e=ArenaSpeechEnvelope.decode(file);assertEquals(0f,e.at(40),.02f);assertTrue(e.at(140)>.5f);assertEquals(0f,e.at(240),.02f);file.delete()
    }
    @Test fun selectedPhotoIsOrientedAndStoredLocally(){
        val ctx=inst.targetContext;val f=File(ctx.cacheDir,"arena-portrait-test.jpg")
        val b=Bitmap.createBitmap(40,80,Bitmap.Config.ARGB_8888);b.eraseColor(Color.BLUE);f.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
        android.media.ExifInterface(f.path).apply{setAttribute(android.media.ExifInterface.TAG_ORIENTATION,android.media.ExifInterface.ORIENTATION_ROTATE_90.toString());saveAttributes()}
        assertTrue(PlayerPortrait.receive(ctx,Activity.RESULT_OK,Intent().setData(Uri.fromFile(f))))
        val saved=PlayerPortrait.load(ctx)!!;assertEquals(80,saved.width);assertEquals(40,saved.height);saved.recycle();f.delete()
    }
    @Test fun nameClothingAndUsedLifelinesRemainOnTheRealStageAfterRelaunch(){
        val ctx=inst.targetContext;ArenaCloudActivity.qaTransport=null
        ctx.getSharedPreferences("arena_cloud_guest",0).edit().clear().putBoolean("audio",false).putString("name","Ilaz QA").putInt("suit",0xff583a79.toInt()).putInt("tie",0xffbd3455.toInt()).commit()
        ctx.getSharedPreferences("arena_offline_v1_guest",0).edit().clear().commit()
        val store=ArenaOfflineStore(ctx,"guest");val s=store.start("Teknologji");s.put("presentation_pending",false);ArenaLifelines.use(s,"fifty");store.save(s)
        fun launch()=inst.startActivitySync(Intent(ctx,ArenaCloudActivity::class.java).putExtra("native_qa",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).also{inst.waitForIdleSync()}
        var activity=launch()
        try{
            repeat(2){
                val scene=views(activity.window.decorView).filterIsInstance<QuizArenaScene>().first();assertEquals("Ilaz QA",scene.playerName);assertEquals(0xff583a79.toInt(),scene.suitColor);assertEquals(0xffbd3455.toInt(),scene.tieColor)
                assertTrue(views(activity.window.decorView).filterIsInstance<Button>().any{it.text.contains("50:50")&&!it.isEnabled})
                if(it==0){inst.runOnMainSync{activity.finish()};activity=launch()}
            }
            val dir=File(ctx.getExternalFilesDir(null),"native-qa").apply{mkdirs()}
            val screenshot=inst.uiAutomation.takeScreenshot();File(dir,"21-arena-presenter-clothing-lifelines.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)};screenshot.recycle()
        }finally{inst.runOnMainSync{activity.finish()};ctx.getSharedPreferences("arena_cloud_guest",0).edit().clear().commit();ctx.getSharedPreferences("arena_offline_v1_guest",0).edit().clear().commit()}
    }
}
