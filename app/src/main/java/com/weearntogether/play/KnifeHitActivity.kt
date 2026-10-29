package com.weearntogether.play

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import kotlin.math.*

/** Fantasy arcade renderer: stylised targets/blades only, not a real-world throwing simulation. */
class KnifeHitActivity:Activity(){
    private lateinit var game:KnifeHitView; private lateinit var ads:NativeGameAds
    override fun onCreate(state:Bundle?){super.onCreate(state);window.statusBarColor=0xff07152f.toInt();window.navigationBarColor=0xff07152f.toInt();window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);ads=NativeGameAds(this,"knife_hit")
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xff071020.toInt())}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xff101b43.toInt(),0xff43217d.toInt(),0xff225cc8.toInt()))}
        top.addView(Button(this).apply{text="‹";textSize=24f;setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(54),dp(48)))
        top.addView(TextView(this).apply{text="BLADE QUEST • HD 2D";textSize=18f;setTextColor(0xffffc857.toInt());setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f))
        top.addView(Button(this).apply{text="↻";textSize=20f;setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{game.restartLevel()}},LinearLayout.LayoutParams(dp(54),dp(48)))
        root.addView(top);game=KnifeHitView(this);root.addView(game,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onResume(){super.onResume();if(::ads.isInitialized)ads.resume()};override fun onPause(){if(::ads.isInitialized)ads.pause();super.onPause()};override fun onDestroy(){if(::ads.isInitialized)ads.destroy();super.onDestroy()}
}

class KnifeHitView(context:android.content.Context):View(context){
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val prefs=ProgressPreferences.open(context,"knife_hit")
    private var level=prefs.getInt("level",1).coerceIn(1,200);private var best=prefs.getInt("best",1).coerceAtLeast(1);private var score=prefs.getInt("score",0).coerceAtLeast(0)
    private var remaining=0;private var totalBlades=0;private var angle=0f;private var speed=1f;private var direction=1f;private var last=0L;private var hitFlash=0L;private var failFlash=0L;private var levelFlash=0L;private var boss=false;private var targetStyle=0
    private val stuck=mutableListOf<Float>();private val obstacles=mutableListOf<Float>()
    init{isFocusable=true;isClickable=true;setupLevel()}
    private fun setupLevel(){totalBlades=(5+(level-1)/4).coerceAtMost(12);remaining=totalBlades;boss=level%10==0;targetStyle=((level-1)/5)%5;speed=(.85f+level*.017f).coerceAtMost(3.2f);direction=if((level/3)%2==0)1f else -1f;stuck.clear();obstacles.clear();val count=when{boss->4;level>=35->3;level>=15->2;level>=6->1;else->0};repeat(count){obstacles.add((it*(360f/count.coerceAtLeast(1))+level*17f)%360f)};save();invalidate()}
    fun restartLevel(){setupLevel()};private fun save(){best=max(best,level);prefs.edit().putInt("level",level).putInt("best",best).putInt("score",score).apply()}
    private fun diff(a:Float,b:Float):Float{var d=abs(a-b)%360f;if(d>180)d=360-d;return d}
    private fun launch(){if(remaining<=0)return;val impact=90f;val occupied=(stuck+obstacles).any{diff(it,impact)<13f};if(occupied){failFlash=System.nanoTime();score=(score-15).coerceAtLeast(0);performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);invalidate();return};stuck.add(impact);remaining--;score+=20+level*3;hitFlash=System.nanoTime();performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);if(remaining==0){score+=150*level;levelFlash=System.nanoTime();level=(level+1).coerceAtMost(200);save();postDelayed({setupLevel()},850)}else save();invalidate()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_DOWN){launch();return true};return true}
    private fun label(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=Color.WHITE,center:Boolean=false){p.shader=null;p.style=Paint.Style.FILL;p.color=color;p.textSize=size;p.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);p.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p);p.textAlign=Paint.Align.LEFT}
    private fun drawBlade(c:Canvas,x:Float,y:Float,rotation:Float,scale:Float=1f,variant:Int=(level/8)%4){c.save();c.rotate(rotation,x,y);val w=width*.026f*scale;val h=height*.09f*scale;val metal=when(variant){1->intArrayOf(0xffffffff.toInt(),0xff8ee8ff.toInt(),0xff5276a8.toInt());2->intArrayOf(0xfffff2b0.toInt(),0xffffc857.toInt(),0xff8d5b20.toInt());3->intArrayOf(0xfff6e9ff.toInt(),0xffba7cff.toInt(),0xff56328c.toInt());else->intArrayOf(0xffffffff.toInt(),0xffc7d5f5.toInt(),0xff66779f.toInt())};p.shader=LinearGradient(x-w,y-h,x+w,y,metal,null,Shader.TileMode.CLAMP);val blade=Path().apply{moveTo(x,y-h);lineTo(x+w,y-h*.15f);lineTo(x+w*.58f,y+h*.18f);lineTo(x-w*.58f,y+h*.18f);lineTo(x-w,y-h*.15f);close()};c.drawPath(blade,p);p.shader=null;p.color=0xffffc857.toInt();c.drawRoundRect(x-w*1.45f,y+h*.12f,x+w*1.45f,y+h*.25f,w*.35f,w*.35f,p);p.color=when(variant){1->0xff167fb4.toInt();2->0xff7b451f.toInt();3->0xff7a35c9.toInt();else->0xff4d287f.toInt()};c.drawRoundRect(x-w*.72f,y+h*.23f,x+w*.72f,y+h*.62f,w*.32f,w*.32f,p);c.restore()}
    private fun targetColors():IntArray=when(targetStyle){1->intArrayOf(0xff55d7ff.toInt(),0xff1467a5.toInt(),0xff071b3e.toInt());2->intArrayOf(0xffffd86a.toInt(),0xffb05b28.toInt(),0xff3d1c26.toInt());3->intArrayOf(0xff68efb5.toInt(),0xff187d6d.toInt(),0xff082d3a.toInt());4->intArrayOf(0xffff7da8.toInt(),0xff8d3b9c.toInt(),0xff25154c.toInt());else->intArrayOf(0xffb58aff.toInt(),0xff53349b.toInt(),0xff111c49.toInt())}
    private fun stageName()=when(targetStyle){1->"ICE ORBIT";2->"GOLDEN CORE";3->"EMERALD RIFT";4->"NEON CROWN";else->"VIOLET FORGE"}
    private fun drawTarget(c:Canvas,cx:Float,cy:Float,r:Float){val colors=if(boss)intArrayOf(0xffffe27a.toInt(),0xffe54b75.toInt(),0xff40164d.toInt())else targetColors();p.shader=RadialGradient(cx-r*.28f,cy-r*.34f,r*1.18f,colors,null,Shader.TileMode.CLAMP);c.drawCircle(cx,cy,r,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=r*.055f;p.color=if(boss)0xfffff0a0.toInt() else 0xffffc857.toInt();c.drawCircle(cx,cy,r*.94f,p);p.strokeWidth=r*.018f;p.color=0x88ffffff.toInt();c.drawCircle(cx,cy,r*.67f,p);c.drawCircle(cx,cy,r*.38f,p);p.style=Paint.Style.FILL;for(i in 0..7){val a=Math.toRadians((i*45+angle*.28f).toDouble());p.color=Color.argb(75,255,255,255);c.drawCircle(cx+cos(a).toFloat()*r*.76f,cy+sin(a).toFloat()*r*.76f,r*.025f,p)}}
    override fun onDraw(c:Canvas){super.onDraw(c);val now=System.nanoTime();if(last==0L)last=now;val dt=((now-last)/1e9f).coerceAtMost(.04f);last=now;angle=(angle+speed*direction*92f*dt)%360f;if(level>=20&&level%7==0&&sin(now/8e8)>0.93)direction=-direction
        p.shader=LinearGradient(0f,0f,width.toFloat(),height.toFloat(),intArrayOf(0xff050b1d.toInt(),0xff251653.toInt(),0xff071b3e.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);p.shader=null
        label(c,"LEVEL $level/200",width*.055f,height*.055f,width*.041f,0xffffc857.toInt());label(c,"BEST $best",width*.40f,height*.055f,width*.035f,0xffd9ccff.toInt());label(c,"SCORE $score",width*.70f,height*.055f,width*.035f,Color.WHITE);label(c,if(boss)"BOSS STAGE" else stageName(),width/2f,height*.105f,width*.034f,if(boss)0xffff7d9b.toInt() else 0xff78dfff.toInt(),true)
        val cx=width/2f;val cy=height*.39f;val r=min(width,height)*.205f;drawTarget(c,cx,cy,r)
        for(a0 in obstacles){val a=Math.toRadians((a0+angle).toDouble());val x=cx+cos(a).toFloat()*r*.72f;val y=cy+sin(a).toFloat()*r*.72f;p.shader=RadialGradient(x,y,r*.13f,intArrayOf(0xffffe278.toInt(),0xffff4776.toInt(),0xff6c164c.toInt()),null,Shader.TileMode.CLAMP);c.drawCircle(x,y,r*.105f,p);p.shader=null}
        for((i,a0) in stuck.withIndex()){val a=Math.toRadians((a0+angle).toDouble());val x=cx+cos(a).toFloat()*r*1.18f;val y=cy+sin(a).toFloat()*r*1.18f;drawBlade(c,x,y,a0+angle+90f,.64f,(i+level/8)%4)}
        if(boss){label(c,"BOSS",cx,cy+r*.04f,r*.20f,0xffffef9a.toInt(),true);label(c,"${totalBlades-remaining}/$totalBlades",cx,cy+r*.27f,r*.10f,Color.WHITE,true)}
        label(c,"BLADES  $remaining",cx,height*.68f,width*.047f,Color.WHITE,true);val shown=remaining.coerceAtMost(12);val start=cx-(shown-1)*width*.031f;for(i in 0 until shown){val x=start+i*width*.062f;drawBlade(c,x,height*.755f,0f,.30f,(i+level/8)%4)}
        drawBlade(c,cx,height*.90f,0f,.82f,(level/8)%4);label(c,"TAP • HIT THE CORE",cx,height*.985f,width*.039f,0xffd8e5ff.toInt(),true)
        val hitAge=(now-hitFlash)/1e9f;if(hitFlash>0&&hitAge<.28f){p.style=Paint.Style.STROKE;p.strokeWidth=dp(5).toFloat();p.color=Color.argb(((1-hitAge/.28f)*210).toInt(),255,210,90);c.drawCircle(cx,cy,r*(.96f+hitAge*.9f),p);p.style=Paint.Style.FILL}
        val failAge=(now-failFlash)/1e9f;if(failFlash>0&&failAge<.30f){p.color=Color.argb(((1-failAge/.30f)*120).toInt(),255,55,90);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);label(c,"BLOCKED!",cx,height*.62f,width*.052f,0xffffd5dc.toInt(),true)}
        val clearAge=(now-levelFlash)/1e9f;if(levelFlash>0&&clearAge<.82f){p.color=Color.argb(((1-clearAge/.82f)*105).toInt(),255,200,87);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);label(c,"LEVEL CLEAR!",cx,height*.52f,width*.068f,Color.WHITE,true);label(c,"NEXT STAGE",cx,height*.57f,width*.038f,0xffffd66b.toInt(),true)}
        postInvalidateOnAnimation()
    }
}
