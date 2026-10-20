package com.weearntogether.play

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

/** Native 8x8 block puzzle with drag placement, combos, saved progress and balanced offers. */
class BlockPuzzleView(context: Context) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG); private val board=Array(8){IntArray(8)}
    private val palette=intArrayOf(0xff35c6fa.toInt(),0xffa478ff.toInt(),0xffffc857.toInt(),0xff49e1aa.toInt())
    private val shapes=listOf(arrayOf(0 to 0),arrayOf(0 to 0,1 to 0),arrayOf(0 to 0,1 to 0,2 to 0),arrayOf(0 to 0,0 to 1),arrayOf(0 to 0,1 to 0,0 to 1),arrayOf(0 to 0,1 to 0,0 to 1,1 to 1),arrayOf(0 to 0,1 to 0,2 to 0,1 to 1),arrayOf(0 to 0,0 to 1,0 to 2),arrayOf(0 to 0,1 to 0,2 to 0,0 to 1),arrayOf(0 to 0,1 to 0,2 to 0,3 to 0),arrayOf(0 to 0,0 to 1,0 to 2,1 to 2))
    private val prefs=context.getSharedPreferences("block_puzzle",Context.MODE_PRIVATE)
    private var offers=MutableList(3){Random.nextInt(shapes.size)};private var selected=0;private var score=0;private var best=prefs.getInt("best",0);private var level=prefs.getInt("level",1).coerceAtLeast(1);private var totalLines=prefs.getInt("total_lines",0);private var combo=prefs.getInt("combo",0);private var bestCombo=prefs.getInt("best_combo",0);private var gameOver=false;private var dragging=false;private var previewX=-1;private var previewY=-1
    init{val s=prefs.getString("board",null);val o=prefs.getString("offers",null);if(s!=null&&s.length==64&&s.all{it in '0'..'4'}&&o!=null){val a=o.split(",").mapNotNull{it.toIntOrNull()};if(a.size==3&&a.all{it in -1 until shapes.size}){for(r in 0..7)for(c in 0..7)board[r][c]=s[r*8+c].digitToInt();offers=a.toMutableList();score=prefs.getInt("score",0);gameOver=!anyMoves()}}}
    fun restart(){for(row in board)row.fill(0);offers=freshOffers();selected=0;score=0;level=1;totalLines=0;combo=0;gameOver=false;dragging=false;previewX=-1;previewY=-1;save();invalidate()}
    private fun freshOffers():MutableList<Int>{val easy=shapes.indices.filter{i->shapes[i].size<=3};val out=mutableListOf<Int>();out+=easy.random();while(out.size<3)out+=Random.nextInt(shapes.size);return out.shuffled().toMutableList()}
    private fun save(){prefs.edit().putString("board",board.joinToString(""){it.joinToString("")}).putString("offers",offers.joinToString(",")).putInt("score",score).putInt("best",best).putInt("level",level).putInt("total_lines",totalLines).putInt("combo",combo).putInt("best_combo",bestCombo).apply()}
    private fun block(c:Canvas,color:Int,size:Float,x:Float,y:Float,alpha:Int=255){p.color=color;p.alpha=alpha;c.drawRoundRect(x,y,x+size,y+size,9f,9f,p);p.alpha=255}
    private fun geo():FloatArray{val cell=min((width-28f)/8f,75f);return floatArrayOf(cell,(width-cell*8)/2f,155f)}
    override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(0xff071022.toInt());val(cell,ox,oy)=geo();p.isFakeBoldText=true;p.color=Color.WHITE;p.textSize=34f;c.drawText("BLOCK PUZZLE",ox,48f,p);p.textSize=21f;p.color=0xff35c6fa.toInt();c.drawText("$score PIKË",ox,82f,p);p.color=0xffffc857.toInt();c.drawText("BEST $best",ox+160,82f,p);p.color=0xffa478ff.toInt();c.drawText("NIVEL $level",ox,116f,p);p.color=if(combo>=2)0xffffc857.toInt() else 0xff49e1aa.toInt();c.drawText("COMBO x$combo",ox+160,116f,p)
        for(y in 0..7)for(x in 0..7)block(c,if(board[y][x]==0)0xff172544.toInt() else palette[(board[y][x]-1)%palette.size],cell-5,ox+x*cell+2,oy+y*cell+2)
        val id=offers.getOrNull(selected)?:-1;if(dragging&&id>=0&&previewX>=0&&previewY>=0){val valid=fits(id,previewX,previewY);shapes[id].forEach{(dx,dy)->val x=previewX+dx;val y=previewY+dy;if(x in 0..7&&y in 0..7)block(c,if(valid)0xff49e1aa.toInt() else 0xffff5d73.toInt(),cell-5,ox+x*cell+2,oy+y*cell+2,195)}}
        val sy=oy+8*cell+38;p.color=Color.LTGRAY;p.textSize=18f;c.drawText("ZGJIDH • TËRHIQ • PASTRO RRESHTA",ox,sy-10,p);offers.forEachIndexed{idx,sid->val w=(width-ox*2)/3f;val bx=ox+idx*w;p.color=if(selected==idx)0xff6247d7.toInt() else 0xff152647.toInt();c.drawRoundRect(bx,sy,bx+w-8,sy+125,18f,18f,p);if(sid>=0)shapes[sid].forEach{(dx,dy)->block(c,palette[idx%palette.size],23f,bx+12+dx*27,sy+22+dy*27)}}
        if(gameOver){p.color=0xee080f24.toInt();c.drawRoundRect(ox+8,oy+95,ox+cell*8-8,oy+330,24f,24f,p);p.color=0xffffc857.toInt();p.textSize=36f;c.drawText("GAME OVER",ox+32,oy+165,p);p.color=Color.WHITE;p.textSize=22f;c.drawText("$score pikë • best $best",ox+32,oy+210,p);c.drawText("Combo rekord x$bestCombo",ox+32,oy+250,p);c.drawText("Rifillo për lojë të re",ox+32,oy+292,p)}}
    private fun fits(id:Int,bx:Int,by:Int)=id in shapes.indices&&shapes[id].all{(dx,dy)->bx+dx in 0..7&&by+dy in 0..7&&board[by+dy][bx+dx]==0}
    private fun anyMoves()=offers.any{id->id>=0&&(0..7).any{y->(0..7).any{x->fits(id,x,y)}}}
    private fun boardPos(x:Float,y:Float):Pair<Int,Int>?{val(cell,ox,oy)=geo();if(x<ox||x>=ox+cell*8||y<oy||y>=oy+cell*8)return null;return ((x-ox)/cell).toInt() to ((y-oy)/cell).toInt()}
    private fun place(x:Int,y:Int){val id=offers[selected];if(id<0||!fits(id,x,y))return;shapes[id].forEach{(dx,dy)->board[y+dy][x+dx]=selected+1};score+=shapes[id].size*level;offers[selected]=-1;val rows=(0..7).filter{r->board[r].all{it!=0}};val cols=(0..7).filter{col->(0..7).all{r->board[r][col]!=0}};rows.forEach{board[it].fill(0)};cols.forEach{col->(0..7).forEach{r->board[r][col]=0}};val lines=rows.size+cols.size;if(lines>0){combo++;bestCombo=maxOf(bestCombo,combo);totalLines+=lines;score+=lines*25*level+combo*15;level=1+totalLines/10;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)}else{combo=0;performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)};best=maxOf(best,score);if(offers.all{it<0})offers=freshOffers();selected=offers.indexOfFirst{it>=0}.let{if(it>=0)it else 0};gameOver=!anyMoves();save()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(gameOver)return true;val(cell,ox,oy)=geo();val sy=oy+8*cell+38;val w=(width-ox*2)/3f;when(e.actionMasked){MotionEvent.ACTION_DOWN->{if(e.y in sy..sy+130&&e.x>=ox){selected=((e.x-ox)/w).toInt().coerceIn(0,2);if(offers[selected]>=0){dragging=true;performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)};invalidate();return true};boardPos(e.x,e.y)?.let{(x,y)->previewX=x;previewY=y}};MotionEvent.ACTION_MOVE->if(dragging){boardPos(e.x,e.y-cell*1.25f)?.let{(x,y)->previewX=x;previewY=y}?:run{previewX=-1;previewY=-1};invalidate()};MotionEvent.ACTION_UP->{if(dragging&&previewX>=0&&previewY>=0)place(previewX,previewY)else boardPos(e.x,e.y)?.let{(x,y)->place(x,y)};dragging=false;previewX=-1;previewY=-1;invalidate();return true};MotionEvent.ACTION_CANCEL->{dragging=false;previewX=-1;previewY=-1;invalidate()}};return true}
}
