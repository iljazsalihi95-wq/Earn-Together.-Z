package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class PuzzlePersistenceTest {
 private val inst get()=InstrumentationRegistry.getInstrumentation()
 private val ctx get()=inst.targetContext
 private fun launch(type:Class<out Activity>)=inst.startActivitySync(Intent(ctx,type).putExtra("native_qa",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).also{inst.waitForIdleSync()}
 private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
 private fun tap(v:View){val now=android.os.SystemClock.uptimeMillis();for(a in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){val e=MotionEvent.obtain(now,now+10,a,v.width/2f,v.height/2f,0);v.dispatchTouchEvent(e);e.recycle()}}
 @Test fun blockTouchPlacementPauseRestartAndRelaunchPreserveRealSavedState(){
  val p=ProgressPreferences.open(ctx,"block_puzzle_hd2d")
  p.edit().clear().putString("board",List(64){"0"}.joinToString(",")).putString("offers","0,1,2").putInt("score",0).putInt("best",100).putInt("level",3).commit()
  var a=launch(BlockPuzzleActivity::class.java)
  try {
   val game=views(a.window.decorView).filterIsInstance<PremiumBlockPuzzleView>().single()
   fun place(){val piece=views(game).first{it.contentDescription?.toString()=="Piece 1, 1 blocks"};tap(piece);views(game).filterIsInstance<GridLayout>().first{it.childCount==64}.getChildAt(0).performClick()}
   inst.runOnMainSync{game.setPaused(true);place()};inst.waitForIdleSync();assertEquals(0,p.getInt("score",-1))
   inst.runOnMainSync{game.setPaused(false);place()};android.os.SystemClock.sleep(500);inst.waitForIdleSync()
   assertTrue(p.getInt("score",0)>0);val saved=p.getString("board","");assertNotEquals("0",saved!!.split(',').first())
   inst.runOnMainSync{a.finish()};a=launch(BlockPuzzleActivity::class.java);assertEquals(saved,p.getString("board",null))
   inst.runOnMainSync{views(a.window.decorView).filterIsInstance<PremiumBlockPuzzleView>().single().restart()};inst.waitForIdleSync()
   assertEquals(0,p.getInt("score",-1));assertEquals(1,p.getInt("level",-1));assertTrue(p.getInt("best",0)>=100)
   assertTrue(p.getString("board","")!!.split(',').all{it=="0"})
  }finally{inst.runOnMainSync{a.finish()}}
 }
 @Test fun bubbleFlightPausesAndResumesWithoutDuplicateSettlement(){
  val p=ProgressPreferences.open(ctx,"bubble_shooter_hd2d")
  p.edit().clear().putString("grid",List(99){if(it<9)(it%3+1).toString()else "0"}.joinToString(",")).putInt("score",10).putInt("best",100).putInt("level",2).putInt("current",1).putInt("next",2).commit()
  var a=launch(BubbleShooterActivity::class.java)
  try {
   val game=views(a.window.decorView).filterIsInstance<PremiumBubbleShooterView>().single()
   inst.runOnMainSync{
    game.setPaused(false)
    val now=android.os.SystemClock.uptimeMillis()
    for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){val e=MotionEvent.obtain(now,now+10,action,game.width/2f,game.height*.4f,0);game.onTouchEvent(e);e.recycle()}
    game.setPaused(true)
   }
   android.os.SystemClock.sleep(1300);inst.waitForIdleSync();assertEquals(10,p.getInt("score",-1))
   inst.runOnMainSync{game.setPaused(false)};android.os.SystemClock.sleep(1600);inst.waitForIdleSync();assertTrue("Resumed shot must settle",p.getInt("score",0)>10)
   val score=p.getInt("score",0);val saved=p.getString("grid",null)
   android.os.SystemClock.sleep(700);assertEquals(score,p.getInt("score",0))
   inst.runOnMainSync{a.finish()};a=launch(BubbleShooterActivity::class.java);assertEquals(saved,p.getString("grid",null));assertEquals(score,p.getInt("score",0))
  }finally{inst.runOnMainSync{a.finish()}}
 }
}
