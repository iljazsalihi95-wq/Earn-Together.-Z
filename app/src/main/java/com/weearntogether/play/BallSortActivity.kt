package com.weearntogether.play

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.*
import android.widget.*
import android.graphics.drawable.GradientDrawable
import android.animation.ValueAnimator
import kotlin.math.sin

class BallSortActivity:Activity(){
    private val prefs by lazy { ProgressPreferences.open(this,"ball_sort") }
    private lateinit var ads:NativeGameAds;private var rewardedUndos=0;private lateinit var game:BallSortGame;private lateinit var board:BallSortBoard;private lateinit var hud:TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(state:Bundle?){super.onCreate(state);ads=NativeGameAds(this,"ball_sort");rewardedUndos=prefs.getInt("rewarded_undos",0);val level=prefs.getInt("level",1);val saved=runCatching{val a=org.json.JSONArray(prefs.getString("board",null));(0 until a.length()).map{i->val r=a.getJSONArray(i);(0 until r.length()).map{r.getInt(it)}}}.getOrNull();game=BallSortGame(level,saved);game.moves=prefs.getInt("moves",0);show()}
    private fun show(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(12),dp(18),dp(14));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff0c1b3c.toInt(),0xff291d58.toInt(),0xff091b35.toInt()))};root.setOnApplyWindowInsetsListener{v,i->v.setPadding(dp(18)+i.systemWindowInsetLeft,dp(12)+i.systemWindowInsetTop,dp(18)+i.systemWindowInsetRight,dp(14)+i.systemWindowInsetBottom);i}
        val top=LinearLayout(this);fun btn(s:String,go:()->Unit)=Button(this).apply{text=s;isAllCaps=false;setTextColor(Color.WHITE);textSize=15f;setTypeface(null,Typeface.BOLD);background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff24579c.toInt(),0xff43327d.toInt())).apply{cornerRadius=dp(15).toFloat();setStroke(dp(1),0x667dbaff)};setOnClickListener{go()}}
        top.addView(btn("‹ Ballina"){finish()},LinearLayout.LayoutParams(0,dp(52),1f));top.addView(btn("↶ Kthe"){if(!board.busy&&(game.moves<=5||rewardedUndos>0)&&game.undo()){if(game.moves>=5)rewardedUndos--;save();board.selected=-1;board.invalidate();status()}},LinearLayout.LayoutParams(0,dp(52),1f));root.addView(top)
        root.addView(TextView(this).apply{text="BALL SORT";textSize=30f;setTextColor(0xffffd278.toInt());gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD)})
        hud=TextView(this).apply{textSize=16f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(0,dp(10),0,dp(10))};root.addView(hud)
        board=BallSortBoard(this,game){save();status();if(game.won())android.app.AlertDialog.Builder(this).setTitle("Nivel i përfunduar!").setMessage("Të gjitha ngjyrat u renditën në ${game.moves} lëvizje.").setCancelable(false).setPositiveButton("NIVELI TJETËR"){_,_->game=BallSortGame(game.level+1);save();show();ads.naturalBreak()}.show()};root.addView(board,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply{text="Prek tubin burim, pastaj tubin pritës.\nVetëm topa të së njëjtës ngjyrë vendosen bashkë.";textSize=14f;setTextColor(0xffc0cdef.toInt());gravity=Gravity.CENTER;setPadding(0,dp(10),0,dp(10))})
        root.addView(btn("RIFILLO NIVELIN"){if(!board.busy)android.app.AlertDialog.Builder(this).setTitle("Rifillo nivelin?").setNegativeButton("Anulo",null).setPositiveButton("Rifillo"){_,_->game=BallSortGame(game.level);save();show()}.show()});root.addView(btn("VIDEO • +3 KTHIME"){ads.rewarded(onGameplayReward={rewardedUndos+=3;save();Toast.makeText(this,"+3 kthime bonus",Toast.LENGTH_SHORT).show()})});ads.banner(root);setContentView(root);status()
    }
    private fun status(){hud.text="NIVELI ${game.level}    •    ${game.moves} LËVIZJE"}
    private fun save(){val a=org.json.JSONArray();game.tubes.forEach{a.put(org.json.JSONArray(it))};prefs.edit().putInt("level",game.level).putInt("moves",game.moves).putInt("rewarded_undos",rewardedUndos).putString("board",a.toString()).apply()}
    override fun onResume(){super.onResume();if(::ads.isInitialized)ads.resume()}
    override fun onPause(){save();ads.pause();super.onPause()}
    override fun onDestroy(){ads.destroy();super.onDestroy()}
}
class BallSortBoard(context:android.content.Context,private val game:BallSortGame,private val changed:()->Unit):View(context){
    var selected=-1;var busy=false;private var animator:ValueAnimator?=null
    private val sprites=StudioAssets.get(context,"balls");private val p=Paint(3);private val tubes=mutableListOf<RectF>();private val palette=intArrayOf(0xffff5f88.toInt(),0xff46cbff.toInt(),0xffffce53.toInt(),0xff6cdda2.toInt(),0xffb28aff.toInt(),0xffff9851.toInt(),0xffea72dd.toInt(),0xff7fe4d8.toInt());private var flying:Int?=null;private var from=0;private var to=0;private var fraction=0f;private var targetSlot=0;private var sourceSlot=0
    override fun onDraw(c:Canvas){super.onDraw(c);tubes.clear();val columns=if(game.tubes.size<=6)3 else 5;val rows=(game.tubes.size+columns-1)/columns;val cellW=width.toFloat()/columns;val cellH=height.toFloat()/rows;val tw=minOf(cellW*.62f,cellH*.23f);val th=tw*3.9f;val radius=tw*.38f
        game.tubes.forEachIndexed{i,balls->val row=i/columns;val count=minOf(columns,game.tubes.size-row*columns);val x=(width-count*cellW)/2+(i%columns+.5f)*cellW;val y=row*cellH+(cellH-th)/2+if(selected==i)-12f else 0f;val r=RectF(x-tw/2,y,x+tw/2,y+th);tubes+=r
            p.style=Paint.Style.FILL;p.shader=LinearGradient(r.left,0f,r.right,0f,intArrayOf(0x304f8cd9,0x1257a9eb,0x5578caff),null,Shader.TileMode.CLAMP);c.drawRoundRect(r,tw*.3f,tw*.3f,p);p.shader=null
            balls.forEachIndexed{j,color->if(!(busy&&i==from&&j==balls.lastIndex))ball(c,x,r.bottom-radius-9-j*radius*2.03f,radius,palette[color])}
            p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=if(selected==i)0xffffd479.toInt() else 0x99b9e5ff.toInt();c.drawRoundRect(r,tw*.3f,tw*.3f,p);p.strokeWidth=5f;p.color=0x44ffffff;c.drawLine(r.left+tw*.15f,r.top+18,r.left+tw*.15f,r.bottom-tw*.3f,p);p.style=Paint.Style.FILL
        }
        val f=flying;if(f!=null&&tubes.size>maxOf(from,to)){val a=tubes[from];val b=tubes[to];val sx=a.centerX();val sy=a.bottom-radius-9-sourceSlot*radius*2.03f;val ex=b.centerX();val ey=b.bottom-radius-9-targetSlot*radius*2.03f;val mid=minOf(a.top,b.top)-radius*1.5f;val x:Float;val y:Float;when{fraction<.3f->{x=sx;y=sy+(mid-sy)*fraction/.3f};fraction<.7f->{x=sx+(ex-sx)*(fraction-.3f)/.4f;y=mid};else->{x=ex;y=mid+(ey-mid)*(fraction-.7f)/.3f}};ball(c,x,y,radius,palette[f])}
    }
    private fun ball(c:Canvas,x:Float,y:Float,r:Float,color:Int){p.alpha=255;val i=palette.indexOf(color).coerceAtLeast(0);val cw=sprites.width/4;val ch=sprites.height/2;p.shader=null;c.drawBitmap(sprites,Rect((i%4)*cw,(i/4)*ch,(i%4+1)*cw,(i/4+1)*ch),RectF(x-r,y-r,x+r,y+r),p)}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action!=MotionEvent.ACTION_UP)return true;performClick();if(busy)return true;val n=tubes.indexOfFirst{it.contains(e.x,e.y)};if(n<0)return true;if(selected<0){if(game.tubes[n].isNotEmpty())selected=n;invalidate();return true};if(n==selected){selected=-1;invalidate();return true};if(!game.canMove(selected,n)){performHapticFeedback(HapticFeedbackConstants.REJECT);selected=if(game.tubes[n].isNotEmpty())n else selected;invalidate();return true}
        from=selected;to=n;sourceSlot=game.tubes[from].lastIndex;targetSlot=game.tubes[to].size;flying=game.tubes[from].last();selected=-1;busy=true;fraction=0f;animator=ValueAnimator.ofFloat(0f,1f).apply{duration=560;addUpdateListener{fraction=it.animatedValue as Float;invalidate()};addListener(object:android.animation.AnimatorListenerAdapter(){override fun onAnimationEnd(a:android.animation.Animator){if(flying!=null){game.move(from,to);flying=null;busy=false;invalidate();changed()}}});start()};return true}
    override fun performClick():Boolean{super.performClick();return true}
    override fun onDetachedFromWindow(){flying=null;busy=false;animator?.cancel();super.onDetachedFromWindow()}
}
class BallSortCover(context:android.content.Context):View(context){private val p=Paint(3);override fun onDraw(c:Canvas){super.onDraw(c);val w=width/4f;val colors=intArrayOf(0xffff6f91.toInt(),0xff5ecaff.toInt(),0xffffcd58.toInt());for(i in 0..2){val x=w*(i+1);p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=0xff82a9e9.toInt();c.drawRoundRect(x-w*.35f,height*.12f,x+w*.35f,height*.9f,12f,12f,p);p.style=Paint.Style.FILL;for(j in 0..2){p.shader=RadialGradient(x-w*.08f,height*(.78f-j*.22f),w*.4f,intArrayOf(Color.WHITE,colors[(i+j)%3],0xff282956.toInt()),null,Shader.TileMode.CLAMP);c.drawCircle(x,height*(.78f-j*.22f),w*.28f,p);p.shader=null}}}}
