package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** Original native timing game: tap to stack moving blocks. Active run and high score are persistent. */
class NeonStackView(context: Context) : View(context) {
    private data class Layer(val x: Float, val w: Float, val y: Float, val color: Int)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs=ProgressPreferences.open(context,"neon_stack")
    private val layers=mutableListOf<Layer>()
    private val palette=intArrayOf(0xff35c6fa.toInt(),0xff8b6cff.toInt(),0xffffc857.toInt(),0xff48e0b2.toInt())
    private var best=prefs.getInt("best",0)
    private var bestPerfect=prefs.getInt("best_perfect",0)
    private var score=0
    private var perfectStreak=0
    private var movingX=0f
    private var movingW=0f
    private var direction=1f
    private var speed=330f
    private var lastFrame=0L
    private var gameOver=false
    private var started=false
    private var restored=false
    private var flashStarted=0L
    private var flashPerfect=false

    fun restart(){layers.clear();score=0;perfectStreak=0;direction=1f;speed=330f;gameOver=false;started=false;restored=true;lastFrame=0L;flashStarted=0L;clearRun();invalidate()}
    private fun blockH()=52f
    private fun floorY()=height-170f
    private fun clearRun(){prefs.edit().remove("run_widths").remove("run_xs").remove("run_score").remove("run_perfect").remove("run_moving_w").remove("run_moving_x").remove("run_direction").remove("run_speed").apply()}
    private fun saveRun(){if(!started||gameOver||layers.isEmpty())return;val widths=layers.joinToString(","){it.w.toString()};val xs=layers.joinToString(","){it.x.toString()};prefs.edit().putString("run_widths",widths).putString("run_xs",xs).putInt("run_score",score).putInt("run_perfect",perfectStreak).putFloat("run_moving_w",movingW).putFloat("run_moving_x",movingX).putFloat("run_direction",direction).putFloat("run_speed",speed).apply()}
    private fun restoreRun():Boolean{if(restored)return started;restored=true;val widths=prefs.getString("run_widths",null)?.split(",")?.mapNotNull{it.toFloatOrNull()}?:return false;val xs=prefs.getString("run_xs",null)?.split(",")?.mapNotNull{it.toFloatOrNull()}?:return false;if(widths.isEmpty()||widths.size!=xs.size)return false;layers.clear();widths.indices.forEach{i->layers+=Layer(xs[i],widths[i],floorY()-i*blockH(),palette[i%palette.size])};score=prefs.getInt("run_score",(layers.size-1).coerceAtLeast(0));perfectStreak=prefs.getInt("run_perfect",0).coerceAtLeast(0);movingW=prefs.getFloat("run_moving_w",layers.last().w);movingX=prefs.getFloat("run_moving_x",18f);direction=prefs.getFloat("run_direction",1f);speed=prefs.getFloat("run_speed",330f+score*16f);started=true;gameOver=false;lastFrame=0L;return true}
    private fun ensureStarted(){if(started||width<=0)return;if(restoreRun()){postInvalidateOnAnimation();return};val w=width*.58f;layers+=Layer((width-w)/2f,w,floorY(),palette[0]);movingW=w;movingX=18f;started=true;saveRun();postInvalidateOnAnimation()}
    private fun rounded(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int){paint.shader=LinearGradient(x,y,x+w,y+h,lighten(color),color,Shader.TileMode.CLAMP);c.drawRoundRect(x,y,x+w,y+h,15f,15f,paint);paint.shader=null;paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;paint.color=0x66ffffff;c.drawRoundRect(x+2,y+2,x+w-2,y+h-2,13f,13f,paint);paint.style=Paint.Style.FILL}
    private fun lighten(color:Int)=Color.rgb((Color.red(color)+60).coerceAtMost(255),(Color.green(color)+60).coerceAtMost(255),(Color.blue(color)+60).coerceAtMost(255))
    override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(0xff070d20.toInt());ensureStarted();paint.isFakeBoldText=true
        paint.shader=LinearGradient(0f,0f,width.toFloat(),260f,intArrayOf(0xff29115d.toInt(),0xff071b3a.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),190f,paint);paint.shader=null
        paint.color=0xffffc857.toInt();paint.textSize=18f;c.drawText("WE EARN TOGETHER",28f,40f,paint);paint.color=Color.WHITE;paint.textSize=35f;c.drawText("NEON STACK",28f,84f,paint);paint.color=0xff9eadd0.toInt();paint.textSize=16f;c.drawText("Tap at the right moment • chain PERFECT drops",28f,116f,paint)
        fun stat(label:String,value:String,x:Float,color:Int){paint.color=0xff132344.toInt();c.drawRoundRect(x,132f,x+145f,188f,18f,18f,paint);paint.color=0xff8fa0c8.toInt();paint.textSize=13f;c.drawText(label,x+13,151f,paint);paint.color=color;paint.textSize=24f;c.drawText(value,x+13,178f,paint)}
        stat("SCORE",score.toString(),28f,0xff35c6fa.toInt());stat("BEST",best.toString(),185f,0xffffc857.toInt());stat("PERFECT",if(perfectStreak>0)"×$perfectStreak" else "—",342f,0xffa478ff.toInt())
        val visible=layers.takeLast(9);val base=floorY();visible.forEachIndexed{i,l->val y=base-(visible.size-1-i)*blockH();rounded(c,l.x,y,l.w,blockH()-6,l.color)}
        if(!gameOver){val y=base-visible.size*blockH();rounded(c,movingX,y,movingW,blockH()-6,palette[(score+1)%palette.size]);paint.color=0xff91a1c5.toInt();paint.textSize=17f;c.drawText("TAP TO DROP",28f,height-70f,paint);paint.color=0xff68789f.toInt();paint.textSize=13f;c.drawText("Speed ${(speed/330f*100).toInt()}%  •  Best perfect ×$bestPerfect",28f,height-43f,paint);advanceFrame()}
        else {paint.color=0xee090f24.toInt();c.drawRoundRect(30f,245f,width-30f,515f,32f,32f,paint);paint.color=0xffffc857.toInt();paint.textSize=38f;c.drawText("RUN OVER",58f,315f,paint);paint.color=Color.WHITE;paint.textSize=25f;c.drawText("Score $score   •   Best $best",58f,365f,paint);paint.color=0xff9eadd0.toInt();paint.textSize=18f;c.drawText("Tap anywhere to restart",58f,410f,paint);paint.color=0xffa478ff.toInt();paint.textSize=16f;c.drawText("Best PERFECT chain ×$bestPerfect",58f,447f,paint)}
        drawDropFeedback(c)
    }
    private fun drawDropFeedback(c:Canvas){if(flashStarted==0L)return;val t=((System.nanoTime()-flashStarted)/650_000_000f).coerceIn(0f,1f);if(t>=1f){flashStarted=0L;return};paint.textAlign=Paint.Align.CENTER;paint.style=Paint.Style.FILL;paint.color=if(flashPerfect)Color.argb(((1f-t)*255).toInt(),255,207,76) else Color.argb(((1f-t)*210).toInt(),101,214,255);paint.textSize=if(flashPerfect)30f else 20f;paint.isFakeBoldText=true;c.drawText(if(flashPerfect)"PERFECT ×$perfectStreak" else "NICE",width/2f,235f-24f*t,paint);paint.textAlign=Paint.Align.LEFT;postInvalidateOnAnimation()}
    private fun advanceFrame(){val now=System.nanoTime();val dt=if(lastFrame==0L).016f else ((now-lastFrame)/1e9f).coerceIn(.005f,.04f);lastFrame=now;movingX+=direction*speed*dt;if(movingX<=12f){movingX=12f;direction=1f}else if(movingX+movingW>=width-12f){movingX=width-12f-movingW;direction=-1f};postInvalidateOnAnimation()}
    private fun drop(){if(gameOver||!started)return;val prev=layers.last();val left=maxOf(movingX,prev.x);val right=minOf(movingX+movingW,prev.x+prev.w);val overlap=right-left;if(overlap<22f){gameOver=true;best=maxOf(best,score);bestPerfect=maxOf(bestPerfect,perfectStreak);prefs.edit().putInt("best",best).putInt("best_perfect",bestPerfect).apply();clearRun();performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);invalidate();return};val perfect=abs(movingX-prev.x)<9f;val newW=if(perfect)prev.w else overlap;val newX=if(perfect)prev.x else left;if(perfect){perfectStreak++;bestPerfect=maxOf(bestPerfect,perfectStreak);score+=1+(perfectStreak/3).coerceAtMost(4)}else{perfectStreak=0;score++};best=maxOf(best,score);layers+=Layer(newX,newW,prev.y-blockH(),palette[layers.size%palette.size]);movingW=newW;movingX=if(direction>0)12f else width-12f-newW;speed=(330f+layers.size*16f+perfectStreak*4f).coerceAtMost(760f);flashPerfect=perfect;flashStarted=System.nanoTime();prefs.edit().putInt("best",best).putInt("best_perfect",bestPerfect).putInt("last_score",score).apply();saveRun();performHapticFeedback(if(perfect)HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP);invalidate()}
    override fun onDetachedFromWindow(){saveRun();super.onDetachedFromWindow()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.actionMasked==MotionEvent.ACTION_DOWN){if(gameOver){restart();ensureStarted()}else drop();return true};return true}
}
