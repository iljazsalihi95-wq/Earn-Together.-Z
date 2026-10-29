package com.weearntogether.play

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.random.Random

/** Native HD 2D cup-and-ball memory game. No Canvas/WebView. */
class CupShuffleActivity : Activity() {
    private lateinit var ads:NativeGameAds
    private val navy = 0xff060c1f.toInt()
    private val purple = 0xff713fe8.toInt()
    private val blue = 0xff197ef2.toInt()
    private val gold = 0xffffc641.toInt()
    private val handler = Handler(Looper.getMainLooper())
    private val tone by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }
    private val prefs by lazy { ProgressPreferences.open(this, "cup_shuffle_hd2d") }
    private lateinit var stage: FrameLayout
    private lateinit var hud: TextView
    private lateinit var message: TextView
    private val cups = mutableListOf<TextView>()
    private var level = 1
    private var score = 0
    private var best = 0
    private var lives = 3
    private var ballCup = 0
    private var busy = true
    private var round = 0
    private var disposed = false
    private var shuffleAnimation: AnimatorSet? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun gradient(a: Int, b: Int, radius: Float) = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply { cornerRadius = radius }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);ads=NativeGameAds(this,"cups")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.navigationBarColor = navy
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        restore()
        buildUi()
        handler.postDelayed({ startRound() }, 550)
    }

    private fun restore() {
        level = prefs.getInt("level", 1).coerceIn(1, 250)
        score = prefs.getInt("score", 0)
        best = prefs.getInt("best", 0)
        lives = prefs.getInt("lives", 3).coerceIn(1, 3)
    }

    private fun save() {
        best = max(best, score)
        prefs.edit().putInt("level", level).putInt("score", score).putInt("best", best).putInt("lives", lives).apply()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(navy, 0xff171044.toInt(), 0xff061a37.toInt()))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val back = TextView(this).apply {
            text = "‹  BACK"; textSize = 14f; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            background = gradient(0xff25375f.toInt(), 0xff111a37.toInt(), dp(18).toFloat()); setOnClickListener { finish() }
        }
        top.addView(back, LinearLayout.LayoutParams(dp(92), dp(46)))
        top.addView(TextView(this).apply { text = "CUP SHUFFLE"; textSize = 22f; setTextColor(gold); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(46), 1f))
        val restart = TextView(this).apply {
            text = "RESTART"; textSize = 12f; setTextColor(navy); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            background = gradient(0xffffe79d.toInt(), gold, dp(18).toFloat()); setOnClickListener { restartGame() }
        }
        top.addView(restart, LinearLayout.LayoutParams(dp(92), dp(46)))
        root.addView(top)
        hud = TextView(this).apply { textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD); setPadding(0, dp(16), 0, dp(10)) }
        root.addView(hud, LinearLayout.LayoutParams(-1, dp(72)))
        message = TextView(this).apply { textSize = 16f; setTextColor(0xffffe5a0.toInt()); gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD); text = "Watch the golden ball" }
        root.addView(message, LinearLayout.LayoutParams(-1, dp(52)))
        stage = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xff08162f.toInt(), 0xff25134e.toInt(), 0xff092747.toInt())).apply { cornerRadius = dp(30).toFloat(); setStroke(dp(1), 0x6658c8ff) }
        }
        root.addView(stage, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(12) })
        root.addView(TextView(this).apply { text = "HD 2D • AUTO-SAVE • 250 LEVELS\nWatch the ball, follow the cups, then choose."; gravity = Gravity.CENTER; textSize = 11f; setTextColor(0xff9fb3dd.toInt()) }, LinearLayout.LayoutParams(-1, dp(52)))
        setContentView(root)
        stage.post { createCups() }
        updateHud()
    }

    private fun createCups() {
        stage.removeAllViews(); cups.clear()
        val cupW = dp(86); val cupH = dp(112)
        repeat(3) { i ->
            val cup = TextView(this).apply {
                text = "♜"; textSize = 62f; gravity = Gravity.CENTER; setTextColor(if (i == 1) 0xffd9c8ff.toInt() else 0xff82cfff.toInt())
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(if (i == 1) purple else blue, 0xff151b48.toInt())).apply { cornerRadius = dp(25).toFloat(); setStroke(dp(2), 0x88ffffff.toInt()) }
                elevation = dp(16).toFloat(); tag = i
                setOnClickListener { if (!busy) choose((it.tag as Int)) }
            }
            cups += cup
            stage.addView(cup, FrameLayout.LayoutParams(cupW, cupH).apply { gravity = Gravity.CENTER_VERTICAL; leftMargin = slotX(i, cupW) })
        }
    }

    private fun slotX(slot: Int, cupW: Int = dp(86)): Int {
        val usable = (stage.width - cupW).coerceAtLeast(dp(210))
        return when (slot) { 0 -> dp(14); 1 -> usable / 2; else -> usable - dp(14) }
    }

    private fun startRound() {
        if (disposed || isFinishing) return
        if (cups.size != 3) { handler.postDelayed({ startRound() }, 200); return }
        busy = true; round++
        ballCup = Random.nextInt(3)
        message.text = "WATCH THE GOLDEN BALL"
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
        showBall(ballCup)
        handler.postDelayed({ hideBallAndShuffle() }, 950L.coerceAtLeast(1500L - level * 8L))
    }

    private fun showBall(index: Int) {
        val ball = TextView(this).apply {
            text = "●"; textSize = 48f; gravity = Gravity.CENTER; setTextColor(gold); elevation = dp(8).toFloat(); tag = "ball"
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; colors = intArrayOf(0xfffff3b0.toInt(), gold, 0xffb76b16.toInt()); setStroke(dp(2), Color.WHITE) }
        }
        val cup = cups[index]
        stage.addView(ball, FrameLayout.LayoutParams(dp(54), dp(54)).apply { leftMargin = cup.left + dp(16); topMargin = (stage.height / 2) + dp(44) })
        cup.animate().translationY(-dp(72).toFloat()).setDuration(260).start()
    }

    private fun hideBallAndShuffle() {
        stage.findViewWithTag<View>("ball")?.let { stage.removeView(it) }
        cups.forEach { it.animate().translationY(0f).setDuration(220).start() }
        handler.postDelayed({ shuffleStep(0, (4 + level / 8).coerceAtMost(14)) }, 260)
    }

    private fun shuffleStep(step: Int, total: Int) {
        if (disposed || isFinishing) return
        if (step >= total) { busy = false; message.text = "CHOOSE THE CUP"; tone.startTone(ToneGenerator.TONE_PROP_ACK, 80); return }
        var a = Random.nextInt(3); var b = Random.nextInt(3)
        while (b == a) b = Random.nextInt(3)
        val ca = cups[a]; val cb = cups[b]
        val ax = ca.x; val bx = cb.x
        val duration = (430L - level * 7L).coerceAtLeast(135L)
        val up = if (step % 2 == 0) -dp(22).toFloat() else dp(18).toFloat()
        val animA = ObjectAnimator.ofFloat(ca, View.X, ax, bx)
        val animB = ObjectAnimator.ofFloat(cb, View.X, bx, ax)
        val liftA = ObjectAnimator.ofFloat(ca, View.TRANSLATION_Y, 0f, up, 0f)
        val liftB = ObjectAnimator.ofFloat(cb, View.TRANSLATION_Y, 0f, -up, 0f)
        shuffleAnimation = AnimatorSet().apply {
            playTogether(animA, animB, liftA, liftB); this.duration = duration; interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() { override fun onAnimationEnd(animation: android.animation.Animator) {
                if (disposed || isFinishing) return
                val temp = cups[a]; cups[a] = cups[b]; cups[b] = temp
                cups.forEachIndexed { idx, cup -> cup.tag = idx }
                if (ballCup == a) ballCup = b else if (ballCup == b) ballCup = a
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 35)
                shuffleStep(step + 1, total)
            } }); start()
        }
    }

    private fun choose(index: Int) {
        if (busy) return
        busy = true
        val correct = index == ballCup
        cups[index].performHapticFeedback(if (correct) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.REJECT)
        if (correct) {
            val gain = 100 * level + round * 10
            score += gain; best = max(best, score); level = (level + 1).coerceAtMost(250)
            message.text = "PERFECT!  +$gain"
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
            pulse(cups[index], gold)
        } else {
            lives--
            message.text = "BALL WAS HERE • ${lives.coerceAtLeast(0)} LIVES"
            tone.startTone(ToneGenerator.TONE_PROP_NACK, 220)
            pulse(cups[ballCup], 0xffff6d91.toInt())
            if (lives <= 0) { score = 0; lives = 3; level = max(1, level - 2); message.text = "NEW RUN • LEVEL $level" }
        }
        save(); updateHud(); if(!correct||!ads.naturalBreak{handler.postDelayed({startRound()},1250)})handler.postDelayed({ startRound() }, 1250)
    }

    private fun pulse(v: View, color: Int) {
        v.animate().scaleX(1.18f).scaleY(1.18f).setDuration(160).withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(230).start() }.start()
        val flash = View(this).apply { setBackgroundColor(color); alpha = .18f }
        stage.addView(flash, FrameLayout.LayoutParams(-1, -1)); flash.animate().alpha(0f).setDuration(420).withEndAction { stage.removeView(flash) }.start()
    }

    private fun updateHud() { hud.text = "LEVEL  $level/250     SCORE  $score     BEST  $best\nLIVES  ${"♥".repeat(lives)}     SPEED  ${(4 + level / 8).coerceAtMost(14)}" }

    private fun restartGame() { handler.removeCallbacksAndMessages(null); score = 0; lives = 3; round = 0; save(); updateHud(); message.text = "Watch the golden ball"; handler.postDelayed({ startRound() }, 450) }

    override fun onDestroy() { disposed = true; handler.removeCallbacksAndMessages(null); shuffleAnimation?.removeAllListeners(); shuffleAnimation?.cancel(); shuffleAnimation = null; cups.forEach { it.animate().cancel() }; tone.release(); save(); ads.destroy(); super.onDestroy() }
}
