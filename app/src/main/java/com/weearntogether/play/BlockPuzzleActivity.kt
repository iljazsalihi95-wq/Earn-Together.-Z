package com.weearntogether.play

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
    private var dialogOpen = false
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private fun chip(text:String, accent:Boolean=false, action:()->Unit)=TextView(this).apply {
        this.text=text
        textSize=13f
        setTextColor(if(accent) 0xff07152c.toInt() else Color.WHITE)
        gravity=Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
        isClickable=true
        isFocusable=true
        elevation=dp(10).toFloat()
        background=if(accent) GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xffffdf72.toInt(),0xffffb72f.toInt())
        ).apply {
            cornerRadius=dp(22).toFloat()
            setStroke(dp(1),0xffffedaa.toInt())
        } else GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xf0162d5b.toInt(),0xf00a72c7.toInt())
        ).apply {
            cornerRadius=dp(22).toFloat()
            setStroke(dp(1),0x8858c8ff.toInt())
        }
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
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

        // Native navigation remains above the canvas, large enough for one-hand play.
        val back=chip("‹  BACK") { finish() }
        host.addView(back,FrameLayout.LayoutParams(dp(96),dp(46),Gravity.TOP or Gravity.START).apply {
            leftMargin=dp(12); topMargin=dp(8)
        })
        val restart=chip("↻  RESTART", accent=true) { confirmRestart() }
        host.addView(restart,FrameLayout.LayoutParams(dp(118),dp(46),Gravity.TOP or Gravity.END).apply {
            rightMargin=dp(12); topMargin=dp(8)
        })
        setContentView(host)
    }

    private fun confirmRestart() {
        if(dialogOpen) return
        dialogOpen=true
        val dialog=AlertDialog.Builder(this)
            .setTitle("Restart level?")
            .setMessage("Your saved campaign stays safe. Only the current board and level score restart.")
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("RESTART") { _, _ -> game.restart() }
            .create()
        dialog.setOnDismissListener { dialogOpen=false; enterImmersive() }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xffffb72f.toInt())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xff58c8ff.toInt())
        }
        dialog.show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !dialogOpen) enterImmersive()
    }

    override fun onResume() {
        super.onResume()
        if(!dialogOpen) enterImmersive()
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
