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
import java.util.Locale

/** Dedicated immersive host so Bubble Shooter owns the screen and all gestures. */
class BubbleShooterActivity : Activity() {
    private lateinit var game: BubbleShooterView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private fun ui(en:String,sq:String,de:String,it:String):String=when(Locale.getDefault().language.lowercase(Locale.ROOT)){
        "sq"->sq
        "de"->de
        "it"->it
        else->en
    }

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

    private fun statusChip(text:String)=TextView(this).apply {
        this.text=text
        textSize=11f
        setTextColor(0xffffd66b.toInt())
        gravity=Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
        letterSpacing=.08f
        elevation=dp(6).toFloat()
        background=GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xdd111a3a.toInt(),0xdd281552.toInt(),0xdd111a3a.toInt())
        ).apply {
            cornerRadius=dp(18).toFloat()
            setStroke(dp(1),0x88ffc857.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersive()

        game = BubbleShooterView(this)
        val host = FrameLayout(this).apply {
            setBackgroundColor(0xff070d20.toInt())
            isClickable = true
            isFocusable = true
            addView(game, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        val back=chip("‹  "+ui("BACK","PRAPA","ZURÜCK","INDIETRO")) { finish() }
        back.contentDescription=ui("Back","Kthehu prapa","Zurück","Indietro")
        host.addView(back,FrameLayout.LayoutParams(dp(104),dp(44),Gravity.TOP or Gravity.START).apply {
            leftMargin=dp(12); topMargin=dp(8)
        })
        val restart=chip("↻  "+ui("RESTART","RIFILLO","NEUSTART","RIPROVA")) { game.restart() }
        restart.contentDescription=ui("Restart level","Rifillo nivelin","Level neu starten","Riavvia il livello")
        host.addView(restart,FrameLayout.LayoutParams(dp(122),dp(44),Gravity.TOP or Gravity.END).apply {
            rightMargin=dp(12); topMargin=dp(8)
        })
        val status=statusChip(ui(
            "BUBBLE SHOOTER  •  AUTO-SAVE  •  250 LEVELS",
            "BUBBLE SHOOTER  •  AUTO-RUAJTJE  •  250 NIVELE",
            "BUBBLE SHOOTER  •  AUTO-SPEICHERN  •  250 LEVEL",
            "BUBBLE SHOOTER  •  SALVATAGGIO AUTO  •  250 LIVELLI"
        ))
        host.addView(status,FrameLayout.LayoutParams(dp(318),dp(34),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin=dp(58)
        })

        // A dedicated queue control makes the existing current/next-bubble mechanic explicit
        // and usable without stealing aim gestures from the full-screen playfield.
        val swap=chip("⇄  "+ui("SWAP","NDËRRO","TAUSCH","SCAMBIA")) { game.swapBubble() }
        swap.contentDescription=ui("Swap current and next bubble","Ndërro topin aktual me tjetrin","Aktuelle und nächste Kugel tauschen","Scambia la bolla attuale con la prossima")
        host.addView(swap,FrameLayout.LayoutParams(dp(118),dp(46),Gravity.BOTTOM or Gravity.END).apply {
            rightMargin=dp(14); bottomMargin=dp(16)
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
