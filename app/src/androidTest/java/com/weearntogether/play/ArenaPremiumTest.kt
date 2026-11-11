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
    @Test fun seatedStudioRendersBothPortraitsAndRealChairAsset() {
        val ctx=inst.targetContext
        val body=StudioAssets.get(ctx,"seated-suit")
        assertTrue(body.hasAlpha());assertEquals(512,body.width);assertEquals(768,body.height)
        val selected=Bitmap.createBitmap(80,120,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.rgb(184,124,92))}
        inst.runOnMainSync {
            val stage=QuizArenaScene(ctx).apply{portrait=selected;playerName="TEST";questionPrompt="Cili planet është më afër Diellit?";questionCategory="SHKENCË";speaking=true;speechEnergy={.6f}}
            stage.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(560,View.MeasureSpec.EXACTLY));stage.layout(0,0,720,560)
            val bitmap=Bitmap.createBitmap(720,560,Bitmap.Config.ARGB_8888)
            stage.draw(android.graphics.Canvas(bitmap))
            val dir=File(ctx.getExternalFilesDir(null),"native-qa").apply{mkdirs()}
            File(dir,"22-arena-real-seated-studio.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            assertNotEquals("Player and host stage must have rendered pixels",Color.TRANSPARENT,bitmap.getPixel(170,390))
            bitmap.recycle()
        }
        selected.recycle()
    }
    @Test fun callerHasAVisiblePortraitAndMouthOpensOnlyWithPlaybackEnergy() {
        inst.runOnMainSync {
            val caller=ArenaPhoneParticipant()
            fun frame(speaking:Boolean,energy:Float)=Bitmap.createBitmap(720,560,Bitmap.Config.ARGB_8888).apply {
                caller.draw(android.graphics.Canvas(this),"SHOKU U PËRGJIGJ",speaking,energy,0L)
            }
            val waiting=frame(false,1f);val silent=frame(true,0f);val talking=frame(true,.9f)
            assertNotEquals("Caller avatar must be visible in the phone screen",Color.TRANSPARENT,waiting.getPixel(570,390))
            assertEquals("A silent playback frame must not invent mouth movement",waiting.getPixel(572,416),silent.getPixel(572,416))
            assertNotEquals("Actual audio energy must animate the caller mouth",silent.getPixel(572,416),talking.getPixel(572,416))
            val stage=QuizArenaScene(inst.targetContext).apply{playerName="LOJTARI";questionPrompt="Pyetja mbetet në studio gjatë telefonatës";questionCategory="QUIZ ARENA";phoneState="SHOKU U PËRGJIGJ";speaking=true;friendSpeaking=true;speechEnergy={.9f};timerText="00:42"}
            stage.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(560,View.MeasureSpec.EXACTLY));stage.layout(0,0,720,560)
            val bitmap=Bitmap.createBitmap(720,560,Bitmap.Config.ARGB_8888);stage.draw(android.graphics.Canvas(bitmap))
            val dir=File(inst.targetContext.getExternalFilesDir(null),"native-qa").apply{mkdirs()}
            File(dir,"23-arena-phone-participant.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            waiting.recycle();silent.recycle();talking.recycle();bitmap.recycle()
        }
    }
    @Test fun clothingSelectionChangesFabricWhilePreservingChairAndHands() {
        inst.runOnMainSync {
            val actor=ArenaSeatedActor(inst.targetContext)
            fun frame(suit:Int)=Bitmap.createBitmap(512,768,Bitmap.Config.ARGB_8888).apply{
                actor.draw(android.graphics.Canvas(this),null,"L",0f,0L,suit,Color.WHITE,0xffc99b4e.toInt())
            }
            val original=frame(0xff273b68.toInt());val selected=frame(0xffa74a79.toInt())
            assertNotEquals("Selected suit must reach the actual photographic character",original.getPixel(180,250),selected.getPixel(180,250))
            assertEquals("Chair must keep its original material",original.getPixel(40,480),selected.getPixel(40,480))
            assertEquals("Hands must retain their original skin colour",original.getPixel(100,390),selected.getPixel(100,390))
            original.recycle();selected.recycle()
        }
    }
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
                assertTrue(views(activity.window.decorView).any{it.contentDescription?.toString()=="50:50, përdorur"&&!it.isEnabled})
                if(it==0){inst.runOnMainSync{activity.finish()};activity=launch()}
            }
            val dir=File(ctx.getExternalFilesDir(null),"native-qa").apply{mkdirs()}
            val screenshot=inst.uiAutomation.takeScreenshot();File(dir,"21-arena-presenter-clothing-lifelines.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)};screenshot.recycle()
        }finally{inst.runOnMainSync{activity.finish()};ctx.getSharedPreferences("arena_cloud_guest",0).edit().clear().commit();ctx.getSharedPreferences("arena_offline_v1_guest",0).edit().clear().commit()}
    }
}
