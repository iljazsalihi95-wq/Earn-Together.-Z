package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/** Native swipe-based 2048 with persistent board, best score, moves and premium WET presentation. */
class Merge2048View(context: Context): View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs=ProgressPreferences.open(context,"merge2048")
    private val board=Array(4){IntArray(4)}
    private var score=0
    private var best=prefs.getInt("best",0)
    private var moves=prefs.getInt("moves",0)
    private var maxTile=prefs.getInt("max_tile",2).coerceAtLeast(2)
    private var downX=0f; private var downY=0f
    private var won=false; private var lost=false
    private val navy=0xff070d20.toInt(); private val panel=0xff111d3b.toInt(); private val gold=0xffffc857.toInt()

    init {
        val saved=prefs.getString("board",null)?.split(",")?.mapNotNull{it.toIntOrNull()}
        if(saved?.size==16 && saved.all{it>=0 && it<=131072}) {
            for(i in 0..15)board[i/4][i%4]=saved[i]
            score=prefs.getInt("score",0); moves=prefs.getInt("moves",0)
            maxTile=maxOf(maxTile, board.maxOf{row->row.maxOrNull()?:0})
            won=maxTile>=2048; lost=!canMove()
        } else restart()
    }
    fun restart(){board.forEach{it.fill(0)};score=0;moves=0;maxTile=2;won=false;lost=false;spawn();spawn();save();invalidate()}
    private fun spawn(){val free=(0..15).filter{board[it/4][it%4]==0};if(free.isEmpty())return;val i=free.random();board[i/4][i%4]=if(Random.nextInt(10)==0)4 else 2}
    private fun save(){best=maxOf(best,score);maxTile=maxOf(maxTile,board.maxOf{it.maxOrNull()?:0});prefs.edit().putString("board",board.flatMap{it.asList()}.joinToString(",")).putInt("score",score).putInt("best",best).putInt("moves",moves).putInt("max_tile",maxTile).apply()}
    private fun canMove():Boolean{for(r in 0..3)for(c in 0..3){if(board[r][c]==0)return true;if(r<3&&board[r][c]==board[r+1][c])return true;if(c<3&&board[r][c]==board[r][c+1])return true};return false}
    private fun compress(line:IntArray):IntArray{val nums=line.filter{it!=0};val out=mutableListOf<Int>();var i=0;while(i<nums.size){if(i+1<nums.size&&nums[i]==nums[i+1]){val v=nums[i]*2;out+=v;score+=v;i+=2}else{out+=nums[i];i++}};while(out.size<4)out+=0;return out.toIntArray()}
    private fun move(direction:Int){if(lost)return;val old=board.flatMap{it.asList()};for(i in 0..3){val line=IntArray(4){j->when(direction){0->board[i][j];1->board[i][3-j];2->board[j][i];else->board[3-j][i]}};val result=compress(line);for(j in 0..3)when(direction){0->board[i][j]=result[j];1->board[i][3-j]=result[j];2->board[j][i]=result[j];else->board[3-j][i]=result[j]}};if(old!=board.flatMap{it.asList()}){moves++;spawn();maxTile=maxOf(maxTile,board.maxOf{it.maxOrNull()?:0});won=maxTile>=2048;lost=!canMove();save();performHapticFeedback(if(won)HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP);invalidate()}}
    override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;return true};MotionEvent.ACTION_UP->{val dx=e.x-downX;val dy=e.y-downY;if(maxOf(abs(dx),abs(dy))<35f)return true;if(abs(dx)>abs(dy))move(if(dx<0)0 else 1)else move(if(dy<0)2 else 3);return true}};return true}
    private fun stat(c:Canvas,label:String,value:String,x:Float,color:Int){paint.color=0xff132344.toInt();c.drawRoundRect(x,132f,x+145f,190f,18f,18f,paint);paint.color=0xff8fa0c8.toInt();paint.textSize=12f;c.drawText(label,x+13,151f,paint);paint.color=color;paint.textSize=23f;c.drawText(value,x+13,180f,paint)}
    private fun tileColor(v:Int)=when(v){0->0xff172544.toInt();2->0xff315ea8.toInt();4->0xff4b68c6.toInt();8->0xff7355d6.toInt();16->0xff9852d6.toInt();32->0xffc552ba.toInt();64->0xffef628f.toInt();128->0xffd99a42.toInt();256->0xffe9ad42.toInt();512->0xfff2be49.toInt();1024->0xffffce58.toInt();else->gold}
    override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(navy);paint.typeface=Typeface.DEFAULT_BOLD
        paint.shader=LinearGradient(0f,0f,width.toFloat(),230f,intArrayOf(0xff301369.toInt(),0xff082552.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),205f,paint);paint.shader=null
        paint.color=gold;paint.textSize=18f;c.drawText("WE EARN TOGETHER",28f,42f,paint);paint.color=Color.WHITE;paint.textSize=34f;c.drawText("MERGE 2048",28f,86f,paint);paint.color=0xffaebde1.toInt();paint.textSize=15f;c.drawText("Swipe • merge equal tiles • build your highest number",28f,116f,paint)
        stat(c,"SCORE",score.toString(),28f,0xff35c6fa.toInt());stat(c,"BEST",best.toString(),190f,gold);stat(c,"MAX",maxTile.toString(),352f,0xffa478ff.toInt())
        val cell=min((width-40f)/4f,160f);val ox=(width-4*cell)/2f;val oy=225f
        paint.color=0xff0d1731.toInt();c.drawRoundRect(ox-8,oy-8,ox+4*cell+8,oy+4*cell+8,26f,26f,paint)
        for(r in 0..3)for(col in 0..3){val v=board[r][col];val x=ox+col*cell+4;val y=oy+r*cell+4;paint.shader=LinearGradient(x,y,x+cell,y+cell,intArrayOf(tileColor(v),if(v==0)panel else Color.rgb((Color.red(tileColor(v))*.68f).toInt(),(Color.green(tileColor(v))*.68f).toInt(),(Color.blue(tileColor(v))*.68f).toInt())),null,Shader.TileMode.CLAMP);c.drawRoundRect(x,y,x+cell-8,y+cell-8,18f,18f,paint);paint.shader=null;if(v!=0){paint.color=if(v>=1024)0xff10152b.toInt() else Color.WHITE;paint.textSize=when{v>=10000->cell*.22f;v>=1000->cell*.27f;else->cell*.36f};val s=v.toString();c.drawText(s,x+(cell-8-paint.measureText(s))/2,y+cell*.61f,paint)}}
        val infoY=oy+4*cell+42f;paint.color=0xffaebde1.toInt();paint.textSize=15f;c.drawText("MOVES  $moves",ox,infoY,paint);paint.color=gold;c.drawText(if(won)"2048 UNLOCKED • KEEP GOING" else "CHASE 2048",ox+180f,infoY,paint)
        if(lost){paint.color=0xee090f24.toInt();c.drawRoundRect(30f,300f,width-30f,555f,30f,30f,paint);paint.color=gold;paint.textSize=38f;c.drawText("NO MOVES",58f,370f,paint);paint.color=Color.WHITE;paint.textSize=24f;c.drawText("Score $score   •   Best $best",58f,420f,paint);paint.color=0xffaebde1.toInt();paint.textSize=17f;c.drawText("Restart and build a stronger run",58f,466f,paint)}
    }
}
