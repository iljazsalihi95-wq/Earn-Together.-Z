package com.weearntogether.play

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

/** Self-contained 8x8 block puzzle. Select a shape then tap the board. */
class BlockPuzzleView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val board = Array(8) { IntArray(8) }
    private val palette = intArrayOf(0xff35c6fa.toInt(),0xffa478ff.toInt(),0xffffc857.toInt(),0xff49e1aa.toInt())
    private val shapes = listOf(
        arrayOf(0 to 0), arrayOf(0 to 0, 1 to 0),
        arrayOf(0 to 0, 1 to 0, 2 to 0),
        arrayOf(0 to 0, 0 to 1),
        arrayOf(0 to 0, 1 to 0, 0 to 1),
        arrayOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
        arrayOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),
        arrayOf(0 to 0, 0 to 1, 0 to 2),
        arrayOf(0 to 0, 1 to 0, 2 to 0, 0 to 1)
    )
    private var offers = MutableList(3) { Random.nextInt(shapes.size) }
    private var selected = 0
    private var score = 0
    private var best = context.getSharedPreferences("block_puzzle",Context.MODE_PRIVATE).getInt("best",0)
    private var gameOver = false
    private val prefs = context.getSharedPreferences("block_puzzle",Context.MODE_PRIVATE)
    init {
        val saved = prefs.getString("board", null)
        val savedOffers = prefs.getString("offers", null)
        if (saved != null && saved.length == 64 && saved.all { it in '0'..'4' } &&
            savedOffers != null && savedOffers.split(",").size == 3) {
            for (r in 0..7) for (c in 0..7) board[r][c] = saved[r * 8 + c].digitToInt()
            val parsed = savedOffers.split(",").mapNotNull { it.toIntOrNull() }
            if (parsed.size == 3 && parsed.all { it in -1 until shapes.size }) {
                offers = parsed.toMutableList()
                score = prefs.getInt("score", 0)
                gameOver = !anyMoves()
            }
        }
    }
    private fun saveGame() {
        prefs.edit()
            .putString("board", board.joinToString("") { row -> row.joinToString("") })
            .putString("offers", offers.joinToString(","))
            .putInt("score", score)
            .apply()
    }
    private fun paint(c: Canvas, color: Int, size: Float, x: Float, y: Float) {
        p.color=color; c.drawRoundRect(x,y,x+size,y+size,7f,7f,p)
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val width=width.toFloat()
        val cell=min((width-28f)/8f,75f)
        val ox=(width-cell*8)/2f
        val oy=115f
        p.color=Color.WHITE;p.textSize=38f;p.isFakeBoldText=true
        c.drawText("Pikë: $score",ox,57f,p)
        p.textSize=25f;p.color=0xffffc857.toInt()
        c.drawText("Rekordi: $best",ox,91f,p)
        for(y in 0..7)for(x in 0..7) {
            paint(c,if(board[y][x]==0) 0xff223252.toInt() else palette[(board[y][x]-1)%palette.size],cell-5,ox+x*cell+2,oy+y*cell+2)
        }
        val sy=oy+8*cell+55f
        p.color=Color.WHITE;p.textSize=25f;c.drawText("Zgjidh bllokun:",ox,sy-15,p)
        offers.forEachIndexed { idx, id ->
            val bx=ox+idx*width/3f
            p.color=if(selected==idx) 0xff6950e9.toInt() else 0xff253657.toInt()
            c.drawRoundRect(bx,sy,bx+width/3f-10,sy+135f,13f,13f,p)
            if(id>=0)shapes[id].forEach { (dx,dy)->paint(c,palette[idx%palette.size],24f,bx+12+dx*28f,sy+20+dy*28f) }
        }
        if(gameOver) {
            p.color=0xee0a1027.toInt();c.drawRoundRect(ox,oy+90,ox+cell*8,oy+300,20f,20f,p)
            p.color=Color.WHITE;p.textSize=35f;c.drawText("Fund i lojës",ox+25,oy+160,p)
            p.textSize=23f;c.drawText("Shtyp Rifillo lojën",ox+25,oy+220,p)
        }
    }
    private fun fits(id:Int,bx:Int,by:Int):Boolean =
        shapes[id].all { (dx,dy)-> bx+dx in 0..7 && by+dy in 0..7 && board[by+dy][bx+dx]==0 }
    private fun anyMoves():Boolean = offers.any { id -> id>=0 && (0..7).any { y->(0..7).any { x->fits(id,x,y) } } }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        if(e.action!=MotionEvent.ACTION_UP)return true
        if(gameOver)return true
        val cell=min((width.toFloat()-28f)/8f,75f)
        val ox=(width-cell*8)/2f
        val oy=115f
        val sy=oy+8*cell+55f
        if(e.y>=sy && e.y<=sy+140f) {
            selected=((e.x-ox)/(width/3f)).toInt().coerceIn(0,2)
            invalidate();return true
        }
        if (e.x < ox || e.x >= ox + cell * 8 || e.y < oy || e.y >= oy + cell * 8) return true
        val x=((e.x-ox)/cell).toInt()
        val y=((e.y-oy)/cell).toInt()
        val id=offers[selected]
        if(id<0 || !fits(id,x,y))return true
        shapes[id].forEach { (dx,dy)->board[y+dy][x+dx]=selected+1 }
        score+=shapes[id].size
        offers[selected]=-1
        var lines=0
        val rows=(0..7).filter { row->board[row].all { it!=0 } }
        val cols=(0..7).filter { col->(0..7).all { row->board[row][col]!=0 } }
        rows.forEach { row->board[row].fill(0) }
        cols.forEach { col->(0..7).forEach { row->board[row][col]=0 } }
        lines=rows.size+cols.size
        score+=lines*lines*10
        if(score>best) {
            best=score
            context.getSharedPreferences("block_puzzle",Context.MODE_PRIVATE).edit().putInt("best",best).apply()
        }
        if(offers.all { it<0 })offers=MutableList(3){Random.nextInt(shapes.size)}
        gameOver=!anyMoves()
        saveGame()
        invalidate()
        return true
    }
}
