package com.weearntogether.play

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.DragEvent
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.random.Random

/** Native View-based HD 2D Block Puzzle. Every tile is a real Android View. */
class PremiumBlockPuzzleView(context: Context) : LinearLayout(context) {
    private val prefs=ProgressPreferences.open(context,"block_puzzle_hd2d")
    private val board=Array(8){IntArray(8)}; private val cells=Array(8){arrayOfNulls<TextView>(8)}
    private val boardView=GridLayout(context); private val tray=LinearLayout(context); private val hud=TextView(context); private val fx=TextView(context)
    private val palette=intArrayOf(0xff35c6fa.toInt(),0xffa478ff.toInt(),0xffffc857.toInt(),0xff49e1aa.toInt(),0xffff668f.toInt())
    private val shapes=listOf(arrayOf(0 to 0),arrayOf(0 to 0,1 to 0),arrayOf(0 to 0,1 to 0,2 to 0),arrayOf(0 to 0,0 to 1),arrayOf(0 to 0,0 to 1,0 to 2),arrayOf(0 to 0,1 to 0,0 to 1,1 to 1),arrayOf(0 to 0,1 to 0,2 to 0,1 to 1),arrayOf(0 to 0,0 to 1,1 to 1),arrayOf(1 to 0,0 to 1,1 to 1),arrayOf(0 to 0,1 to 0,2 to 0,0 to 1),arrayOf(0 to 0,1 to 0,2 to 0,2 to 1),arrayOf(0 to 0,1 to 0,0 to 1,1 to 1,0 to 2,1 to 2))
    private var offers=IntArray(3); private var score=0; private var best=0; private var level=1; private var totalLines=0; private var levelLines=0; private var combo=0; private var gameOver=false; private val gap=dp(3)
    init{
        orientation=VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(14),dp(108),dp(14),dp(16));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff071020.toInt(),0xff14265a.toInt(),0xff25104f.toInt(),0xff071020.toInt()))
        hud.apply{setTextColor(Color.WHITE);textSize=15f;gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);letterSpacing=.04f};addView(hud,LayoutParams(-1,dp(52)))
        fx.apply{textSize=13f;gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);setTextColor(0xffffd56a.toInt());alpha=0f};addView(fx,LayoutParams(-1,dp(28)))
        boardView.columnCount=8;boardView.rowCount=8;boardView.background=rounded(0xff09152f.toInt(),0x6658c8ff,22);boardView.setPadding(dp(8),dp(8),dp(8),dp(8));addView(boardView,LayoutParams(-1,0,1f).apply{setMargins(0,dp(4),0,dp(12))})
        tray.orientation=HORIZONTAL;tray.gravity=Gravity.CENTER;tray.background=rounded(0xaa101a3a.toInt(),0x55ffc857,22);tray.setPadding(dp(8),dp(8),dp(8),dp(8));addView(tray,LayoutParams(-1,dp(126)))
        buildBoard();restoreOrNew();boardView.setOnDragListener{_,e->handleBoardDrag(e)}
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun goal()=(5+level/12).coerceAtMost(20)
    private fun rounded(fill:Int,stroke:Int,r:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(fill,shade(fill))).apply{cornerRadius=dp(r).toFloat();setStroke(dp(1),stroke)}
    private fun shade(c:Int)=Color.rgb((Color.red(c)*.66f).toInt(),(Color.green(c)*.66f).toInt(),(Color.blue(c)*.66f).toInt())
    private fun light(c:Int)=Color.rgb((Color.red(c)+70).coerceAtMost(255),(Color.green(c)+70).coerceAtMost(255),(Color.blue(c)+70).coerceAtMost(255))
    private fun emptyTile()=rounded(0xff13254b.toInt(),0x334f9fe8,9)
    private fun filledTile(color:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(light(color),color,shade(color))).apply{cornerRadius=dp(10).toFloat();setStroke(dp(2),0x66ffffff)}
    private fun buildBoard(){boardView.removeAllViews();repeat(8){r->repeat(8){c->val v=TextView(context).apply{gravity=Gravity.CENTER;background=emptyTile();elevation=dp(2).toFloat()};cells[r][c]=v;boardView.addView(v,GridLayout.LayoutParams(GridLayout.spec(r,1,1f),GridLayout.spec(c,1,1f)).apply{width=0;height=0;setMargins(gap,gap,gap,gap)})}}}
    private fun restoreOrNew(){val raw=prefs.getString("board",null);if(raw!=null){val a=raw.split(',').mapNotNull{it.toIntOrNull()};if(a.size==64)for(i in a.indices)board[i/8][i%8]=a[i]};score=prefs.getInt("score",0);best=prefs.getInt("best",0);level=prefs.getInt("level",1).coerceIn(1,250);totalLines=prefs.getInt("total_lines",prefs.getInt("lines",0));levelLines=prefs.getInt("level_lines",0).coerceAtLeast(0);combo=prefs.getInt("combo",0);val saved=prefs.getString("offers",null)?.split(',')?.mapNotNull{it.toIntOrNull()};offers=if(saved?.size==3)saved.map{it.coerceIn(-1,shapes.lastIndex)}.toIntArray() else nextOffers();if(offers.all{it<0})offers=nextOffers();gameOver=!hasAnyMove();refresh()}
    private fun save(){best=max(best,score);prefs.edit().putString("board",board.flatMap{it.asList()}.joinToString(",")).putString("offers",offers.joinToString(",")).putInt("score",score).putInt("best",best).putInt("level",level).putInt("lines",totalLines).putInt("total_lines",totalLines).putInt("level_lines",levelLines).putInt("combo",combo).apply()}
    private fun nextOffers():IntArray{val hard=(level/20).coerceAtMost(5);return IntArray(3){if(Random.nextInt(8)<hard) Random.nextInt(5,shapes.size) else Random.nextInt(shapes.size)}}
    private fun refresh(){for(r in 0..7)for(c in 0..7){val v=board[r][c];cells[r][c]?.apply{background=if(v==0)emptyTile() else filledTile(palette[(v-1).coerceIn(0,palette.lastIndex)]);alpha=if(v==0).72f else 1f;scaleX=1f;scaleY=1f;rotation=0f}};hud.text=if(gameOver)"GAME OVER   SCORE $score   BEST $best\nRESTART TO PLAY AGAIN" else "SCORE  $score     BEST  $best     LEVEL  $level/250\nGOAL  $levelLines/${goal()}     TOTAL  $totalLines     COMBO  x$combo";tray.removeAllViews();offers.forEachIndexed{i,s->tray.addView(if(s<0||gameOver)View(context) else pieceView(i,s),LayoutParams(0,-1,1f).apply{setMargins(dp(5),0,dp(5),0)})}}
    private fun pieceView(index:Int,shapeIndex:Int):View{val shape=shapes[shapeIndex];val maxX=shape.maxOf{it.first};val maxY=shape.maxOf{it.second};return GridLayout(context).apply{columnCount=maxX+1;rowCount=maxY+1;gravity=Gravity.CENTER;background=rounded(0x221b83d4,0x3358c8ff,16);isClickable=true;shape.forEach{(x,y)->addView(View(context).apply{background=filledTile(palette[index%palette.size]);elevation=dp(5).toFloat()},GridLayout.LayoutParams(GridLayout.spec(y),GridLayout.spec(x)).apply{width=dp(25);height=dp(25);setMargins(dp(2),dp(2),dp(2),dp(2))})};setOnTouchListener{v,e->if(!gameOver&&e.action==MotionEvent.ACTION_DOWN){v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);v.startDragAndDrop(ClipData.newPlainText("piece",index.toString()),DragShadowBuilder(v),index,0);true}else false}}}
    private fun handleBoardDrag(e:DragEvent):Boolean{if(e.action!=DragEvent.ACTION_DROP)return true;if(gameOver)return true;val offer=(e.localState as? Int)?:return false;if(offer !in offers.indices||offers[offer]<0)return false;val cw=boardView.width/8f;val ch=boardView.height/8f;val c=(e.x/cw).toInt().coerceIn(0,7);val r=(e.y/ch).toInt().coerceIn(0,7);val shape=shapes[offers[offer]];val anchorC=c-shape.minOf{it.first};val anchorR=r-shape.minOf{it.second};if(!canPlace(shape,anchorR,anchorC)){performHapticFeedback(HapticFeedbackConstants.REJECT);fx.text="NO SPACE";fx.alpha=1f;fx.animate().alpha(0f).setDuration(450).start();return true};val color=(offer%palette.size)+1;shape.forEach{(x,y)->board[anchorR+y][anchorC+x]=color};offers[offer]=-1;score+=shape.size*12*level;val rows=(0..7).filter{rr->board[rr].all{it!=0}};val cols=(0..7).filter{cc->(0..7).all{rr->board[rr][cc]!=0}};val cleared=rows.size+cols.size;combo=if(cleared>0)combo+1 else 0;if(cleared>0){animateClear(rows,cols);totalLines+=cleared;levelLines+=cleared;score+=cleared*120*level+combo*50;while(levelLines>=goal()&&level<250){val oldGoal=goal();levelLines-=oldGoal;level++;score+=level*250;fx.text="LEVEL $level  ✦  BONUS +${level*250}";fx.alpha=1f}};rows.forEach{rr->for(cc in 0..7)board[rr][cc]=0};cols.forEach{cc->for(rr in 0..7)board[rr][cc]=0};if(offers.all{it<0})offers=nextOffers();gameOver=!hasAnyMove();save();performHapticFeedback(if(cleared>0)HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP);postDelayed({refresh()},if(cleared>0)220 else 0);return true}
    private fun animateClear(rows:List<Int>,cols:List<Int>){val targets=mutableSetOf<TextView>();rows.forEach{r->for(c in 0..7)cells[r][c]?.let{targets+=it}};cols.forEach{c->for(r in 0..7)cells[r][c]?.let{targets+=it}};targets.forEachIndexed{i,v->v.animate().scaleX(1.35f).scaleY(1.35f).alpha(.08f).rotationBy(if(i%2==0)18f else -18f).setDuration(190).start()};fx.text=when{combo>=4->"SUPER COMBO x$combo  ✦";combo>=2->"COMBO x$combo  +${rows.size+cols.size} LINES";else->"LINE CLEAR  +${rows.size+cols.size}"};fx.alpha=1f;fx.scaleX=.8f;fx.scaleY=.8f;fx.animate().alpha(0f).scaleX(1.25f).scaleY(1.25f).setDuration(700).start();boardView.animate().scaleX(1.018f).scaleY(1.018f).setDuration(90).withEndAction{boardView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()}.start()}
    private fun canPlace(shape:Array<Pair<Int,Int>>,r:Int,c:Int)=shape.all{(x,y)->r+y in 0..7&&c+x in 0..7&&board[r+y][c+x]==0}
    private fun hasAnyMove():Boolean{for(s in offers)if(s>=0){val shape=shapes[s];for(r in 0..7)for(c in 0..7)if(canPlace(shape,r,c))return true};return false}
    fun restart(){best=max(best,score);for(r in board)r.fill(0);score=0;levelLines=0;combo=0;gameOver=false;offers=nextOffers();save();refresh()}
}
