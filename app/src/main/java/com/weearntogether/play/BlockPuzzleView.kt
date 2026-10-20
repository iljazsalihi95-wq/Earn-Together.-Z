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

/** Premium self-contained 8x8 block puzzle with drag preview and persisted progress. */
class BlockPuzzleView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val board = Array(8) { IntArray(8) }
    private val palette = intArrayOf(0xff35c6fa.toInt(), 0xffa478ff.toInt(), 0xffffc857.toInt(), 0xff49e1aa.toInt())
    private val shapes = listOf(
        arrayOf(0 to 0), arrayOf(0 to 0, 1 to 0), arrayOf(0 to 0, 1 to 0, 2 to 0),
        arrayOf(0 to 0, 0 to 1), arrayOf(0 to 0, 1 to 0, 0 to 1),
        arrayOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), arrayOf(0 to 0, 1 to 0, 2 to 0, 1 to 1),
        arrayOf(0 to 0, 0 to 1, 0 to 2), arrayOf(0 to 0, 1 to 0, 2 to 0, 0 to 1)
    )
    private val prefs = context.getSharedPreferences("block_puzzle", Context.MODE_PRIVATE)
    private var offers = MutableList(3) { Random.nextInt(shapes.size) }
    private var selected = 0
    private var score = 0
    private var best = prefs.getInt("best", 0)
    private var level = prefs.getInt("level", 1).coerceAtLeast(1)
    private var totalLines = prefs.getInt("total_lines", 0)
    private var gameOver = false
    private var dragging = false
    private var previewX = -1
    private var previewY = -1

    init {
        val saved = prefs.getString("board", null)
        val savedOffers = prefs.getString("offers", null)
        if (saved != null && saved.length == 64 && saved.all { it in '0'..'4' } && savedOffers != null) {
            val parsed = savedOffers.split(",").mapNotNull { it.toIntOrNull() }
            if (parsed.size == 3 && parsed.all { it in -1 until shapes.size }) {
                for (r in 0..7) for (c in 0..7) board[r][c] = saved[r * 8 + c].digitToInt()
                offers = parsed.toMutableList()
                score = prefs.getInt("score", 0)
                gameOver = !anyMoves()
            }
        }
    }

    fun restart() {
        for (row in board) row.fill(0)
        offers = MutableList(3) { Random.nextInt(shapes.size) }
        selected = 0; score = 0; gameOver = false; dragging = false
        previewX = -1; previewY = -1
        saveGame(); invalidate()
    }

    private fun saveGame() {
        prefs.edit()
            .putString("board", board.joinToString("") { row -> row.joinToString("") })
            .putString("offers", offers.joinToString(","))
            .putInt("score", score).putInt("best", best)
            .putInt("level", level).putInt("total_lines", totalLines).apply()
    }

    private fun block(c: Canvas, color: Int, size: Float, x: Float, y: Float, alpha: Int = 255) {
        p.color = color; p.alpha = alpha
        c.drawRoundRect(x, y, x + size, y + size, 8f, 8f, p)
        p.alpha = 255
    }

    private fun geometry(): FloatArray {
        val cell = min((width.toFloat() - 28f) / 8f, 75f)
        return floatArrayOf(cell, (width - cell * 8) / 2f, 145f)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(0xff091329.toInt())
        val (cell, ox, oy) = geometry()
        p.isFakeBoldText = true; p.color = Color.WHITE; p.textSize = 34f
        c.drawText("BLOCK PUZZLE", ox, 48f, p)
        p.textSize = 23f; p.color = 0xff35c6fa.toInt(); c.drawText("Pikë $score", ox, 83f, p)
        p.color = 0xffffc857.toInt(); c.drawText("Rekord $best", ox + 145f, 83f, p)
        p.color = 0xffa478ff.toInt(); c.drawText("Nivel $level", ox, 116f, p)
        p.color = 0xff49e1aa.toInt(); c.drawText("Rreshta $totalLines", ox + 145f, 116f, p)

        for (y in 0..7) for (x in 0..7) {
            block(c, if (board[y][x] == 0) 0xff1b2948.toInt() else palette[(board[y][x] - 1) % palette.size], cell - 5, ox + x * cell + 2, oy + y * cell + 2)
        }

        val id = offers.getOrNull(selected) ?: -1
        if (dragging && id >= 0 && previewX >= 0 && previewY >= 0) {
            val valid = fits(id, previewX, previewY)
            shapes[id].forEach { (dx, dy) ->
                val x = previewX + dx; val y = previewY + dy
                if (x in 0..7 && y in 0..7) block(c, if (valid) 0xff49e1aa.toInt() else 0xffff5d73.toInt(), cell - 5, ox + x * cell + 2, oy + y * cell + 2, 185)
            }
        }

        val sy = oy + 8 * cell + 45f
        p.color = Color.WHITE; p.textSize = 22f; c.drawText("Tërhiq një bllok në tabelë", ox, sy - 12f, p)
        offers.forEachIndexed { idx, shapeId ->
            val cardW = (width - ox * 2) / 3f
            val bx = ox + idx * cardW
            p.color = if (selected == idx) 0xff6247d7.toInt() else 0xff18294a.toInt()
            c.drawRoundRect(bx, sy, bx + cardW - 8, sy + 120f, 16f, 16f, p)
            if (shapeId >= 0) shapes[shapeId].forEach { (dx, dy) -> block(c, palette[idx % palette.size], 23f, bx + 13 + dx * 27f, sy + 20 + dy * 27f) }
        }

        if (gameOver) {
            p.color = 0xee080f24.toInt(); c.drawRoundRect(ox + 8, oy + 95, ox + cell * 8 - 8, oy + 315, 22f, 22f, p)
            p.color = 0xffffc857.toInt(); p.textSize = 36f; c.drawText("Fund i lojës", ox + 32, oy + 165, p)
            p.color = Color.WHITE; p.textSize = 23f; c.drawText("Pikë: $score  •  Rekord: $best", ox + 32, oy + 210, p)
            c.drawText("Shtyp Rifillo për një lojë të re", ox + 32, oy + 258, p)
        }
    }

    private fun fits(id: Int, bx: Int, by: Int): Boolean = id in shapes.indices && shapes[id].all { (dx, dy) -> bx + dx in 0..7 && by + dy in 0..7 && board[by + dy][bx + dx] == 0 }
    private fun anyMoves(): Boolean = offers.any { id -> id >= 0 && (0..7).any { y -> (0..7).any { x -> fits(id, x, y) } } }

    private fun boardPosition(x: Float, y: Float): Pair<Int, Int>? {
        val (cell, ox, oy) = geometry()
        if (x < ox || x >= ox + cell * 8 || y < oy || y >= oy + cell * 8) return null
        return (((x - ox) / cell).toInt()) to (((y - oy) / cell).toInt())
    }

    private fun placeSelected(x: Int, y: Int) {
        val id = offers[selected]
        if (id < 0 || !fits(id, x, y)) return
        val placedColor = selected + 1
        shapes[id].forEach { (dx, dy) -> board[y + dy][x + dx] = placedColor }
        score += shapes[id].size
        offers[selected] = -1
        val rows = (0..7).filter { row -> board[row].all { it != 0 } }
        val cols = (0..7).filter { col -> (0..7).all { row -> board[row][col] != 0 } }
        rows.forEach { board[it].fill(0) }
        cols.forEach { col -> (0..7).forEach { row -> board[row][col] = 0 } }
        val lines = rows.size + cols.size
        if (lines > 0) {
            totalLines += lines
            score += lines * lines * 12 + level * 2
            level = 1 + totalLines / 12
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } else performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (score > best) best = score
        if (offers.all { it < 0 }) offers = MutableList(3) { Random.nextInt(shapes.size) }
        selected = offers.indexOfFirst { it >= 0 }.let { if (it >= 0) it else 0 }
        gameOver = !anyMoves()
        saveGame()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (gameOver) return true
        val (cell, ox, oy) = geometry()
        val sy = oy + 8 * cell + 45f
        val cardW = (width - ox * 2) / 3f
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (e.y in sy..(sy + 125f) && e.x >= ox) {
                    selected = ((e.x - ox) / cardW).toInt().coerceIn(0, 2)
                    if (offers[selected] >= 0) { dragging = true; performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
                    invalidate(); return true
                }
                boardPosition(e.x, e.y)?.let { (x, y) -> previewX = x; previewY = y }
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                boardPosition(e.x, e.y - cell * 1.2f)?.let { (x, y) -> previewX = x; previewY = y } ?: run { previewX = -1; previewY = -1 }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (dragging && previewX >= 0 && previewY >= 0) placeSelected(previewX, previewY)
                else boardPosition(e.x, e.y)?.let { (x, y) -> placeSelected(x, y) }
                dragging = false; previewX = -1; previewY = -1
                invalidate(); return true
            }
            MotionEvent.ACTION_CANCEL -> { dragging = false; previewX = -1; previewY = -1; invalidate() }
        }
        return true
    }
}
