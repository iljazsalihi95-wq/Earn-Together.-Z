package com.weearntogether.play

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import kotlin.math.*

/** Stylised fantasy arcade game. No real-world throwing instructions are represented. */
class KnifeHitActivity:Activity(){
    private lateinit var game:KnifeHitView
    private lateinit var ads:NativeGameAds
    override fun onCreate(state:Bundle?){super.onCreate(state);window.statusBarColor=0xff07152f.toInt();window.navigationBarColor=0xff07152f.toInt();ads=NativeGameAds(this,"knife_hit");
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xff071020.toInt())}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xff101b43.toInt(),0xff43217d.toInt(),0xff225cc8.toInt()))}
        top.addView(Button(this).apply{text="‹";textSize=24f;setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(54),dp(48)))
        top.addView(TextView(this).apply{text="KNIFE HIT • ARCADE";textSize=18f;setTextColor(0xffffc857.toInt());setTypeface(null,android.graphics.Typeface.BOLD);gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f))
        top.addView(Button(this).apply{text="↻";textSize=20f;setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{game.restartLevel()}},LinearLayout.LayoutParams(dp(54),dp(48)))
        root.addView(top);game=KnifeHitView(this);root.addView(game,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onResume(){super.onResume();if(::ads.isInitialized)ads.resume()}
    override fun onPause(){if(::ads.isInitialized)ads.pause();super.onPause()}
    override fun onDestroy(){if(::ads.isInitialized)ads.destroy();super.onDestroy()}
}

class KnifeHitView(context:android.content.Context):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val prefs=ProgressPreferences.open(context,"knife_hit")
    private var level=prefs.getInt("level",1).coerceIn(1,200);private var best=prefs.getInt("best",1).coerceAtLeast(1);private var score=prefs.getInt("score",0).coerceAtLeast(0)
    private var remaining=0;private var angle=0f;private var speed=1.15f;private var direction=1f;private var last=0L;private var hitFlash=0L;private var failFlash=0L;private var boss=false
    private val stuck=mutableListOf<Float>();private val obstacles=mutableListOf<Float>()
    init{isFocusable=true;setupLevel()}
    private fun setupLevel(){remaining=(5+(level-1)/3).coerceAtMost(13);boss=level%10==0;speed=(1.0f+level*.018f).coerceAtMost(3.4f);direction=if(level%4<2)1f else -1f;stuck.clear();obstacles.clear();val count=if(boss)3 else ((level-1)/18).coerceAtMost(2);repeat(count){obstacles.add((it*137f+level*29f)%360f)};save();invalidate()}
    fun restartLevel(){setupLevel()}
    private fun save(){best=max(best,level);prefs.edit().putInt("level",level).putInt("best",best).putInt("score",score).apply()}
    private fun diff(a:Float,b:Float):Float{var d=abs(a-b)%360f;if(d>180)d=360-d;return d}
    private fun launch(){if(remaining<=0)return;val impact=90f;val occupied=(stuck+obstacles).any{diff(it,impact)<15f};if(occupied){failFlash=System.nanoTime();score=(score-25).coerceAtLeast(0);remaining=(remaining+1).coerceAtMost(14);performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);save();invalidate();return};stuck.add(impact);remaining--;score+=10+level*2;hitFlash=System.nanoTime();performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);if(remaining==0){score+=100*level;level=(level+1).coerceAtMost(200);postDelayed({setupLevel()},500)};save();invalidate()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_DOWN){launch();return true};return true}
    private fun label(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=Color.WHITE,center:Boolean=false){p.shader=null;p.style=Paint.Style.FILL;p.color=color;p.textSize=size;p.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);p.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p);p.textAlign=Paint.Align.LEFT}
    private fun drawBlade(c:Canvas,x:Float,y:Float,rotation:Float,scale:Float=1f){c.save();c.rotate(rotation,x,y);val w=width*.035f*scale;val h=height*.13f*scale;p.shader=LinearGradient(x-w,y-h,x+w,y,intArrayOf(0xffffffff.toInt(),0xffb9c9ed.toInt(),0xff6f7da4.toInt()),null,Shader.TileMode.CLAMP);val blade=Path().apply{moveTo(x,y-h);lineTo(x+w,y-h*.18f);lineTo(x+w*.55f,y+h*.18f);lineTo(x-w*.55f,y+h*.18f);lineTo(x-w,y-h*.18f);close()};c.drawPath(blade,p);p.shader=null;p.color=0xffffc857.toInt();c.drawRoundRect(x-w*1.3f,y+h*.12f,x+w*1.3f,y+h*.25f,w*.3f,w*.3f,p);p.color=0xff5b2b9b.toInt();c.drawRoundRect(x-w*.72f,y+h*.23f,x+w*.72f,y+h*.55f,w*.32f,w*.32f,p);c.restore()}
    override fun onDraw(c:Canvas){super.onDraw(c);val now=System.nanoTime();if(last==0L)last=now;val dt=((now-last)/1e9f).coerceAtMost(.04f);last=now;angle=(angle+speed*direction*95f*dt)%360f;if(level%7==0&&sin(now/7e8)>0.78)direction=-direction
        p.shader=LinearGradient(0f,0f,width.toFloat(),height.toFloat(),intArrayOf(0xff071020.toInt(),0xff251653.toInt(),0xff0a1c3d.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);p.shader=null
        label(c,"LEVEL $level/200",width*.06f,height*.07f,width*.043f,0xffffc857.toInt());label(c,"BEST $best",width*.39f,height*.07f,width*.038f,0xffd9ccff.toInt());label(c,"SCORE $score",width*.68f,height*.07f,width*.038f,Color.WHITE)
        val cx=width/2f;val cy=height*.39f;val r=min(width,height)*.205f;p.shader=RadialGradient(cx-r*.3f,cy-r*.35f,r*1.25f,intArrayOf(if(boss)0xffffb84d.toInt() else 0xff9b6cff.toInt(),0xff3c2b88.toInt(),0xff101b43.toInt()),null,Shader.TileMode.CLAMP);c.drawCircle(cx,cy,r,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=r*.055f;p.color=0xffffc857.toInt();c.drawCircle(cx,cy,r*.93f,p);p.style=Paint.Style.FILL
        for(a0 in obstacles){val a=Math.toRadians((a0+angle).toDouble());val x=cx+cos(a).toFloat()*r*.72f;val y=cy+sin(a).toFloat()*r*.72f;p.color=0xffff5577.toInt();c.drawCircle(x,y,r*.09f,p)}
        for(a0 in stuck){val a=Math.toRadians((a0+angle).toDouble());val x=cx+cos(a).toFloat()*r*1.22f;val y=cy+sin(a).toFloat()*r*1.22f;drawBlade(c,x,y,(a0+angle+90f),.72f)}
        if(boss)label(c,"BOSS",cx,cy+r*.08f,r*.22f,0xffffd66b.toInt(),true)
        label(c,"BLADES  $remaining",cx,height*.68f,width*.05f,Color.WHITE,true);for(i in 0 until remaining.coerceAtMost(10)){p.color=if(i<5)0xffffc857.toInt() else 0xff9b6cff.toInt();c.drawRoundRect(width*.16f+i*(width*.07f),height*.715f,width*.19f+i*(width*.07f),height*.75f,width*.01f,width*.01f,p)}
        drawBlade(c,cx,height*.88f,0f,.9f);label(c,"TAP TO HIT",cx,height*.97f,width*.045f,0xffd8e5ff.toInt(),true)
        val hitAge=(now-hitFlash)/1e9f;if(hitFlash>0&&hitAge<.22f){p.color=Color.argb(((1-hitAge/.22f)*120).toInt(),255,200,87);c.drawCircle(cx,cy,r*(1f+hitAge),p)}
        val failAge=(now-failFlash)/1e9f;if(failFlash>0&&failAge<.28f){p.color=Color.argb(((1-failAge/.28f)*115).toInt(),255,55,90);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);label(c,"BLOCKED!",cx,height*.61f,width*.055f,0xffffd5dc.toInt(),true)}
        postInvalidateOnAnimation()
    }
}
