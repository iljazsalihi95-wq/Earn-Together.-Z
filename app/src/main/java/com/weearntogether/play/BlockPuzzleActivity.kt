package com.weearntogether.play

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/** Dedicated immersive host so Block Puzzle owns the full screen and all drag gestures. */
class BlockPuzzleActivity : Activity() {
    private lateinit var game: BlockPuzzleView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private fun chip(text:String, action:()->Unit)=TextView(this).apply {
        this.text=text
        textSize=13f
        setTextColor(Color.WHITE)
        gravity=Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
        isClickable=true
        isFocusable=true
        elevation=dp(8).toFloat()
        background=GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xee4b2aa8.toInt(),0xee145aa7.toInt())
        ).apply {
            cornerRadius=dp(22).toFloat()
            setStroke(dp(1),0x99ffc857.toInt())
        }
        setOnClickListener { action() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersive()

        game = BlockPuzzleView(this)
        val host = FrameLayout(this).apply {
            setBackgroundColor(0xff070d20.toInt())
            isClickable = true
            isFocusable = true
            addView(game, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        // Keep navigation inside the native game instead of relying on Android system bars.
        val back=chip("‹  BACK") { finish() }
        host.addView(back,FrameLayout.LayoutParams(dp(92),dp(44),Gravity.TOP or Gravity.START).apply {
            leftMargin=dp(12); topMargin=dp(8)
        })
        val restart=chip("↻  RESTART") { game.restart() }
        host.addView(restart,FrameLayout.LayoutParams(dp(112),dp(44),Gravity.TOP or Gravity.END).apply {
            rightMargin=dp(12); topMargin=dp(8)
        })
        setContentView(host)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersive()
    }

    private fun enterImmersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}
