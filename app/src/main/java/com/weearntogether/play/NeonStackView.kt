package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** Original native timing game: tap to stack moving blocks. Progress and high score are persistent. */
class NeonStackView(context: Context) : View(context) {
    private data class Layer(val x: Float, val w: Float, val y: Float, val color: Int)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs=context.getSharedPreferences("neon_stack",Context.MODE_PRIVATE)
    private val layers=mutableListOf<Layer>()
    private val palette=intArrayOf(0xff35c6fa.toInt(),0xff8b6cff.toInt(),0xffffc857.toInt(),0xff48e0b2.toInt())
    private var best=prefs.getInt("best",0)
    private var score=0
    private var movingX=0f
    private var movingW=0f
    private var direction=1f
    private var speed=330f
    private var lastFrame=0L
    private var gameOver=false
    private var started=false

    fun restart(){layers.clear();score=0;direction=1f;speed=330f;gameOver=false;started=false;lastFrame=0L;invalidate()}
    private fun blockH()=52f
    private fun floorY()=height-170f
    private fun ensureStarted(){if(started||width<=0)return;val w=width*.58f;layers+=Layer((width-w)/2f,w,floorY(),palette[0]);movingW=w;movingX=18f;started=true;postInvalidateOnAnimation()}
    private fun rounded(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int){paint.shader=LinearGradient(x,y,x+w,y+h,lighten(color),color,Shader.TileMode.CLAMP);c.drawRoundRect(x,y,x+w,y+h,15f,15f,paint);paint.shader=null;paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;paint.color=0x66ffffff;c.drawRoundRect(x+2,y+2,x+w-2,y+h-2,13f,13f,paint);paint.style=Paint.Style.FILL}
    private fun lighten(color:Int)=Color.rgb((Color.red(color)+60).coerceAtMost(255),(Color.green(color)+60).coerceAtMost(255),(Color.blue(color)+60).coerceAtMost(255))
    override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(0xff070d20.toInt());ensureStarted();paint.isFakeBoldText=true
        paint.shader=LinearGradient(0f,0f,width.toFloat(),260f,0xff29115d.toInt(),0xff071b3a.toInt(),Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),190f,paint);paint.shader=null
        paint.color=0xffffc857.toInt();paint.textSize=18f;c.drawText("WE EARN TOGETHER",28f,40f,paint);paint.color=Color.WHITE;paint.textSize=35f;c.drawText("NEON STACK",28f,84f,paint);paint.color=0xff9eadd0.toInt();paint.textSize=16f;c.drawText("Tap at the right moment • build the highest tower",28f,116f,paint)
        fun stat(label:String,value:String,x:Float,color:Int){paint.color=0xff132344.toInt();c.drawRoundRect(x,132f,x+145f,188f,18f,18f,paint);paint.color=0xff8fa0c8.toInt();paint.textSize=13f;c.drawText(label,x+13,151f,paint);paint.color=color;paint.textSize=24f;c.drawText(value,x+13,178f,paint)}
        stat("SCORE",score.toString(),28f,0xff35c6fa.toInt());stat("BEST",best.toString(),185f,0xffffc857.toInt());stat("SPEED","${(speed/330f*100).toInt()}%",342f,0xffa478ff.toInt())
        val visible=layers.takeLast(9);val base=floorY();visible.forEachIndexed{i,l->val y=base-(visible.size-1-i)*blockH();rounded(c,l.x,y,l.w,blockH()-6,l.color)}
        if(!gameOver){val y=base-visible.size*blockH();rounded(c,movingX,y,movingW,blockH()-6,palette[(score+1)%palette.size]);paint.color=0xff91a1c5.toInt();paint.textSize=17f;c.drawText("TAP TO DROP",28f,height-70f,paint);animate()}
        else {paint.color=0xee090f24.toInt();c.drawRoundRect(30f,245f,width-30f,515f,32f,32f,paint);paint.color=0xffffc857.toInt();paint.textSize=38f;c.drawText("RUN OVER",58f,315f,paint);paint.color=Color.WHITE;paint.textSize=25f;c.drawText("Score $score   •   Best $best",58f,365f,paint);paint.color=0xff9eadd0.toInt();paint.textSize=18f;c.drawText("Restart and beat your tower",58f,410f,paint)}
    }
    private fun animate(){val now=System.nanoTime();val dt=if(lastFrame==0L).016f else ((now-lastFrame)/1e9f).coerceIn(.005f,.04f);lastFrame=now;movingX+=direction*speed*dt;if(movingX<=12f){movingX=12f;direction=1f}else if(movingX+movingW>=width-12f){movingX=width-12f-movingW;direction=-1f};postInvalidateOnAnimation()}
    private fun drop(){if(gameOver||!started)return;val prev=layers.last();val left=maxOf(movingX,prev.x);val right=minOf(movingX+movingW,prev.x+prev.w);val overlap=right-left;if(overlap<22f){gameOver=true;best=maxOf(best,score);prefs.edit().putInt("best",best).apply();performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);invalidate();return};val perfect=abs(movingX-prev.x)<9f;val newW=if(perfect)prev.w else overlap;val newX=if(perfect)prev.x else left;score++;best=maxOf(best,score);layers+=Layer(newX,newW,prev.y-blockH(),palette[score%palette.size]);movingW=newW;movingX=if(direction>0)12f else width-12f-newW;speed=(330f+score*16f).coerceAtMost(720f);prefs.edit().putInt("best",best).putInt("last_score",score).apply();performHapticFeedback(if(perfect)HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP);invalidate()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.actionMasked==MotionEvent.ACTION_DOWN){drop();return true};return true}
}
