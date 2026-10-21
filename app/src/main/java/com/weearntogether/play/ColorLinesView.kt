package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import java.util.ArrayDeque
import kotlin.random.Random

/** Native strategic board game: move balls through open paths and build lines of five. */
class ColorLinesView(context: Context) : View(context) {
    private val n=9
    private val board=IntArray(n*n)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs=context.getSharedPreferences("color_lines",Context.MODE_PRIVATE)
    private val colors=intArrayOf(0xff35c6fa.toInt(),0xff9a72ff.toInt(),0xffffc857.toInt(),0xffff648a.toInt(),0xff45dfaa.toInt())
    private var next=IntArray(3){Random.nextInt(1,colors.size+1)}
    private var selected=-1
    private var score=prefs.getInt("score",0)
    private var best=prefs.getInt("best",0)
    private var gameOver=false
    private var cell=0f; private var left=0f; private var top=0f
    init { if(!restore()) newGame(false) }
    fun restart(){newGame(true);invalidate()}
    private fun newGame(clear:Boolean){board.fill(0);score=0;selected=-1;gameOver=false;repeat(5){spawnRandom()};next=IntArray(3){Random.nextInt(1,colors.size+1)};if(clear)save();invalidate()}
    private fun spawnRandom():Int{val empty=board.indices.filter{board[it]==0};if(empty.isEmpty())return -1;val p=empty.random();board[p]=Random.nextInt(1,colors.size+1);return p}
    private fun spawnColor(color:Int):Int{val empty=board.indices.filter{board[it]==0};if(empty.isEmpty())return -1;val p=empty.random();board[p]=color;return p}
    private fun save(){best=maxOf(best,score);prefs.edit().putInt("score",score).putInt("best",best).putString("board",board.joinToString(",")).putString("next",next.joinToString(",")).putBoolean("over",gameOver).apply()}
    private fun restore():Boolean{val s=prefs.getString("board",null)?:return false;val a=s.split(",").mapNotNull{it.toIntOrNull()};if(a.size!=board.size)return false;a.forEachIndexed{i,v->board[i]=v};score=prefs.getInt("score",0);best=prefs.getInt("best",0);gameOver=prefs.getBoolean("over",false);val savedNext=prefs.getString("next",null)?.split(",")?.mapNotNull{it.toIntOrNull()};next=if(savedNext?.size==3)savedNext.toIntArray() else IntArray(3){Random.nextInt(1,colors.size+1)};return true}
    override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(0xff070d20.toInt());paint.typeface=Typeface.DEFAULT_BOLD
        paint.shader=LinearGradient(0f,0f,width.toFloat(),220f,intArrayOf(0xff301369.toInt(),0xff082552.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),205f,paint);paint.shader=null
        paint.color=0xffffc857.toInt();paint.textSize=18f;c.drawText("WE EARN TOGETHER",28f,42f,paint);paint.color=Color.WHITE;paint.textSize=34f;c.drawText("COLOR LINES",28f,86f,paint);paint.color=0xff9eadd0.toInt();paint.textSize=15f;c.drawText("Move through open paths • connect 5 or more",28f,116f,paint)
        stat(c,"SCORE",score.toString(),28f,0xff35c6fa.toInt());stat(c,"BEST",best.toString(),190f,0xffffc857.toInt());stat(c,"OPEN",board.count{it==0}.toString(),352f,0xff9a72ff.toInt())
        cell=((width-40f)/n);left=20f;top=225f
        paint.style=Paint.Style.FILL
        for(r in 0 until n)for(col in 0 until n){val i=r*n+col;val x=left+col*cell;val y=top+r*cell;paint.color=if(i==selected)0xff334b82.toInt() else 0xff111d3b.toInt();c.drawRoundRect(x+2,y+2,x+cell-2,y+cell-2,10f,10f,paint);val v=board[i];if(v>0)drawBall(c,x+cell/2,y+cell/2,cell*.31f,colors[v-1])}
        val baseY=top+n*cell+38f;paint.color=0xff9eadd0.toInt();paint.textSize=15f;c.drawText("NEXT",28f,baseY,paint);next.forEachIndexed{i,v->drawBall(c,102f+i*45f,baseY-6f,14f,colors[v-1])};paint.color=0xff9eadd0.toInt();paint.textSize=14f;c.drawText("Plan your route before the next 3 arrive",255f,baseY,paint)
        if(gameOver){paint.color=0xee090f24.toInt();c.drawRoundRect(30f,300f,width-30f,560f,30f,30f,paint);paint.color=0xffffc857.toInt();paint.textSize=38f;c.drawText("BOARD FULL",58f,370f,paint);paint.color=Color.WHITE;paint.textSize=24f;c.drawText("Score $score   •   Best $best",58f,420f,paint);paint.color=0xff9eadd0.toInt();paint.textSize=17f;c.drawText("Restart and plan a cleaner route",58f,466f,paint)}
    }
    private fun drawBall(c:Canvas,cx:Float,cy:Float,rad:Float,color:Int){paint.shader=RadialGradient(cx-rad*.25f,cy-rad*.3f,rad*1.3f,intArrayOf(Color.WHITE,color,darken(color)),floatArrayOf(0f,.28f,1f),Shader.TileMode.CLAMP);c.drawCircle(cx,cy,rad,paint);paint.shader=null}
    private fun stat(c:Canvas,label:String,value:String,x:Float,color:Int){paint.color=0xff132344.toInt();c.drawRoundRect(x,135f,x+145f,192f,18f,18f,paint);paint.color=0xff8fa0c8.toInt();paint.textSize=12f;c.drawText(label,x+13,154f,paint);paint.color=color;paint.textSize=23f;c.drawText(value,x+13,182f,paint)}
    private fun darken(v:Int)=Color.rgb((Color.red(v)*.55f).toInt(),(Color.green(v)*.55f).toInt(),(Color.blue(v)*.55f).toInt())
    private fun reachable(from:Int,to:Int):Boolean{if(from==to||board[to]!=0)return false;val q:ArrayDeque<Int> = ArrayDeque();val seen=BooleanArray(board.size);q.add(from);seen[from]=true;while(q.isNotEmpty()){val p=q.removeFirst();val r=p/n;val c=p%n;val ns=intArrayOf(p-n,p+n,p-1,p+1);for((k,v) in ns.withIndex()){if(v !in board.indices)continue;if((k==2&&c==0)||(k==3&&c==n-1))continue;if(v==to)return true;if(!seen[v]&&board[v]==0){seen[v]=true;q.add(v)}}};return false}
    private fun clearLines(at:Int):Int{val color=board[at];if(color==0)return 0;val r=at/n;val c=at%n;val remove=mutableSetOf<Int>();val dirs=arrayOf(1 to 0,0 to 1,1 to 1,1 to -1);for((dr,dc) in dirs){val line=mutableListOf(at);var rr=r+dr;var cc=c+dc;while(rr in 0 until n&&cc in 0 until n&&board[rr*n+cc]==color){line+=rr*n+cc;rr+=dr;cc+=dc};rr=r-dr;cc=c-dc;while(rr in 0 until n&&cc in 0 until n&&board[rr*n+cc]==color){line+=rr*n+cc;rr-=dr;cc-=dc};if(line.size>=5)remove.addAll(line)};remove.forEach{board[it]=0};if(remove.isNotEmpty()){score+=remove.size*10+(remove.size-5)*5;best=maxOf(best,score);performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)};return remove.size}
    private fun move(from:Int,to:Int){val v=board[from];board[from]=0;board[to]=v;selected=-1;if(clearLines(to)==0){val upcoming=next.copyOf();next=IntArray(3){Random.nextInt(1,colors.size+1)};for(color in upcoming){val spawned=spawnColor(color);if(spawned>=0)clearLines(spawned)}};gameOver=board.none{it==0};save();performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);invalidate()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.actionMasked!=MotionEvent.ACTION_DOWN||gameOver)return true;if(e.y<top)return true;val col=((e.x-left)/cell).toInt();val row=((e.y-top)/cell).toInt();if(row !in 0 until n||col !in 0 until n)return true;val i=row*n+col;if(board[i]!=0){selected=i;invalidate()}else if(selected>=0&&reachable(selected,i)){move(selected,i)};return true}
}
