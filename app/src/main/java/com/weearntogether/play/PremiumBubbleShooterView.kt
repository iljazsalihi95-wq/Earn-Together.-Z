package com.weearntogether.play

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.random.Random

/** Native HD 2D Bubble Shooter made from Android Views; gameplay does not use Canvas. */
class PremiumBubbleShooterView(context: Context) : FrameLayout(context) {
    private val prefs = ProgressPreferences.open(context, "bubble_shooter_hd2d")
    private val rows = 10
    private val cols = 9
    private val grid = Array(rows) { IntArray(cols) }
    private val palette = intArrayOf(0xff35c6fa.toInt(), 0xffa478ff.toInt(), 0xffffc857.toInt(), 0xff49e1aa.toInt(), 0xffff638f.toInt())
    private val field = FrameLayout(context)
    private val hud = TextView(context)
    private val launcher = TextView(context)
    private val nextView = TextView(context)
    private var score = 0
    private var best = 0
    private var level = 1
    private var streak = 0
    private var current = 1
    private var next = 2
    private var busy = false
    private var cell = 1
    private var fieldTop = 0

    init {
        setBackgroundColor(0xff071020.toInt())
        field.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xff0a1734.toInt(), 0xff211047.toInt(), 0xff071a38.toInt())).apply {
            cornerRadius = dp(24).toFloat(); setStroke(dp(1), 0x6658c8ff)
        }
        addView(field, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply { setMargins(dp(12), dp(116), dp(12), dp(92)) })
        hud.apply { setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD); letterSpacing = .04f; elevation = dp(8).toFloat() }
        addView(hud, LayoutParams(LayoutParams.MATCH_PARENT, dp(48), Gravity.TOP).apply { topMargin = dp(112) })
        launcher.gravity = Gravity.CENTER; launcher.text = "●"; launcher.textSize = 40f; launcher.elevation = dp(12).toFloat()
        addView(launcher, LayoutParams(dp(66), dp(66), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(12) })
        nextView.gravity = Gravity.CENTER; nextView.textSize = 22f; nextView.elevation = dp(10).toFloat()
        addView(nextView, LayoutParams(dp(48), dp(48), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(18); leftMargin = dp(116) })
        restoreOrNew()
        post { rebuild() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bubble(color: Int) = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(light(color), color, shade(color))).apply {
        shape = GradientDrawable.OVAL; setStroke(dp(2), 0x99ffffff.toInt())
    }
    private fun light(c: Int) = Color.rgb((Color.red(c) + 80).coerceAtMost(255), (Color.green(c) + 80).coerceAtMost(255), (Color.blue(c) + 80).coerceAtMost(255))
    private fun shade(c: Int) = Color.rgb((Color.red(c) * .58f).toInt(), (Color.green(c) * .58f).toInt(), (Color.blue(c) * .58f).toInt())

    private fun restoreOrNew() {
        val raw = prefs.getString("grid", null)?.split(',')?.mapNotNull { it.toIntOrNull() }
        if (raw?.size == rows * cols) raw.forEachIndexed { i, v -> grid[i / cols][i % cols] = v.coerceIn(0, palette.size) }
        else newBoard()
        score = prefs.getInt("score", 0); best = prefs.getInt("best", 0); level = prefs.getInt("level", 1).coerceIn(1, 250); streak = prefs.getInt("streak", 0)
        current = prefs.getInt("current", randomBoardColor()).coerceIn(1, palette.size)
        next = prefs.getInt("next", randomBoardColor()).coerceIn(1, palette.size)
    }

    private fun newBoard() {
        for (r in 0 until rows) grid[r].fill(0)
        val fillRows = (4 + level / 35).coerceAtMost(7)
        for (r in 0 until fillRows) for (c in 0 until cols) if (Random.nextFloat() > .08f) grid[r][c] = 1 + Random.nextInt((3 + level / 45).coerceAtMost(palette.size))
    }

    private fun save() {
        best = max(best, score)
        prefs.edit().putString("grid", grid.flatMap { it.asList() }.joinToString(",")).putInt("score", score).putInt("best", best).putInt("level", level).putInt("streak", streak).putInt("current", current).putInt("next", next).apply()
    }

    private fun rebuild() {
        field.removeAllViews()
        cell = ((field.width - dp(16)) / cols).coerceAtLeast(dp(30))
        fieldTop = dp(52)
        for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] > 0) addBubble(r, c, grid[r][c])
        launcher.setTextColor(palette[current - 1]); launcher.background = bubble(palette[current - 1])
        nextView.setTextColor(palette[next - 1]); nextView.text = "●"; nextView.background = bubble(palette[next - 1])
        hud.text = "SCORE  $score     BEST  $best     LEVEL  $level/250\nSTREAK  x$streak     NEXT"
    }

    private fun addBubble(r: Int, c: Int, value: Int) {
        val v = TextView(context).apply { gravity = Gravity.CENTER; text = "•"; textSize = 18f; setTextColor(0xaaffffff.toInt()); background = bubble(palette[value - 1]); elevation = dp(7).toFloat() }
        field.addView(v, LayoutParams(cell - dp(5), cell - dp(5)).apply { leftMargin = dp(8) + c * cell; topMargin = fieldTop + r * cell })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP || busy) return true
        val localX = event.x - field.left
        val localY = event.y - field.top
        if (localY < fieldTop || localX < 0 || localX > field.width) return true
        val col = (localX / cell).toInt().coerceIn(0, cols - 1)
        shoot(col)
        return true
    }

    private fun shoot(col: Int) {
        val targetRow = landingRow(col)
        if (targetRow < 0) { performHapticFeedback(HapticFeedbackConstants.REJECT); return }
        busy = true
        val projectile = View(context).apply { background = bubble(palette[current - 1]); elevation = dp(18).toFloat() }
        val size = cell - dp(4)
        addView(projectile, LayoutParams(size, size).apply { gravity = Gravity.TOP or Gravity.START; leftMargin = width / 2 - size / 2; topMargin = height - dp(84) })
        val startX = width / 2f - size / 2f
        val startY = height - dp(84).toFloat()
        val targetX = field.left + dp(8) + col * cell.toFloat()
        val targetY = field.top + fieldTop + targetRow * cell.toFloat()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240L + (targetRow * 12L); interpolator = DecelerateInterpolator()
            addUpdateListener { a -> val t = a.animatedValue as Float; projectile.x = startX + (targetX - startX) * t; projectile.y = startY + (targetY - startY) * t; projectile.scaleX = 1f + .08f * (1f - t); projectile.scaleY = projectile.scaleX }
            addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) { removeView(projectile); settle(targetRow, col) } })
            start()
        }
    }

    private fun landingRow(col: Int): Int {
        for (r in rows - 1 downTo 0) if (grid[r][col] != 0) return if (r + 1 < rows) r + 1 else -1
        return 0
    }

    private fun settle(r: Int, c: Int) {
        grid[r][c] = current
        val group = flood(r, c, current)
        if (group.size >= 3) {
            group.forEach { (rr, cc) -> grid[rr][cc] = 0 }
            val dropped = dropDisconnected()
            streak++
            score += group.size * 45 * level + dropped * 70 * level + streak * 30
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } else {
            streak = 0; score += 10 * level; performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (grid.flatten().none { it != 0 }) { level = (level + 1).coerceAtMost(250); score += level * 500; newBoard() }
        current = next; next = randomBoardColor(); busy = false; save(); rebuild()
    }

    private fun flood(sr: Int, sc: Int, color: Int): Set<Pair<Int, Int>> {
        val seen = linkedSetOf<Pair<Int, Int>>(); val q = ArrayDeque<Pair<Int, Int>>(); q.add(sr to sc)
        while (q.isNotEmpty()) { val p = q.removeFirst(); if (p in seen) continue; val (r, c) = p; if (r !in 0 until rows || c !in 0 until cols || grid[r][c] != color) continue; seen.add(p); q.add(r - 1 to c); q.add(r + 1 to c); q.add(r to c - 1); q.add(r to c + 1) }
        return seen
    }

    private fun dropDisconnected(): Int {
        val keep = mutableSetOf<Pair<Int, Int>>(); val q = ArrayDeque<Pair<Int, Int>>()
        for (c in 0 until cols) if (grid[0][c] != 0) q.add(0 to c)
        while (q.isNotEmpty()) { val p = q.removeFirst(); if (!keep.add(p)) continue; val (r, c) = p; arrayOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1).forEach { (rr, cc) -> if (rr in 0 until rows && cc in 0 until cols && grid[rr][cc] != 0 && (rr to cc) !in keep) q.add(rr to cc) } }
        var dropped = 0; for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] != 0 && (r to c) !in keep) { grid[r][c] = 0; dropped++ }
        return dropped
    }

    private fun randomBoardColor(): Int {
        val present = grid.flatMap { it.asList() }.filter { it > 0 }.distinct()
        return if (present.isEmpty()) 1 + Random.nextInt(palette.size) else present.random()
    }

    fun swapBubble() { if (busy) return; val t = current; current = next; next = t; save(); rebuild(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    fun restart() { if (busy) return; score = 0; streak = 0; newBoard(); current = randomBoardColor(); next = randomBoardColor(); save(); rebuild() }
}
