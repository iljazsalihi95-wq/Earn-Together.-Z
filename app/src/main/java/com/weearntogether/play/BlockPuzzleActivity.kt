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
import java.util.Locale

/** Dedicated immersive host so Block Puzzle owns the full screen and all drag gestures. */
class BlockPuzzleActivity : Activity() {
    private lateinit var game: BlockPuzzleView
    private var dialogOpen=false
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private fun ui(en:String,sq:String,de:String,it:String):String=when(Locale.getDefault().language.lowercase(Locale.ROOT)){
        "sq"->sq
        "de"->de
        "it"->it
        else->en
    }

    private fun chip(text:String, accent:Boolean=false, action:()->Unit)=TextView(this).apply {
        this.text=text
        textSize=13f
        setTextColor(if(accent) 0xff07152c.toInt() else Color.WHITE)
        gravity=Gravity.CENTER
        setTypeface(null,Typeface.BOLD)
        isClickable=true
        isFocusable=true
        elevation=dp(10).toFloat()
        background=if(accent) GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xffffdf72.toInt(),0xffffb72f.toInt())
        ).apply {
            cornerRadius=dp(23).toFloat()
            setStroke(dp(1),0xffffedaa.toInt())
        } else GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xf0162d5b.toInt(),0xf00a72c7.toInt())
        ).apply {
            cornerRadius=dp(23).toFloat()
            setStroke(dp(1),0x8858c8ff.toInt())
        }
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
    }

    private fun statusChip(text:String)=TextView(this).apply {
        this.text=text
        textSize=11f
        setTextColor(0xffffd66b.toInt())
        gravity=Gravity.CENTER
        setTypeface(null,Typeface.BOLD)
        letterSpacing=.08f
        elevation=dp(6).toFloat()
        background=GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xdd102a54.toInt(),0xdd24164d.toInt(),0xdd0b5f9f.toInt())
        ).apply {
            cornerRadius=dp(18).toFloat()
            setStroke(dp(1),0x88ffc857.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.statusBarColor=Color.TRANSPARENT
        window.navigationBarColor=Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersive()

        game=BlockPuzzleView(this)
        val host=FrameLayout(this).apply {
            setBackgroundColor(0xff070d20.toInt())
            isClickable=true
            isFocusable=true
            addView(game,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT))
        }

        val back=chip("‹  "+ui("BACK","PRAPA","ZURÜCK","INDIETRO")){finish()}
        back.contentDescription=ui("Back","Kthehu prapa","Zurück","Indietro")
        host.addView(back,FrameLayout.LayoutParams(dp(104),dp(46),Gravity.TOP or Gravity.START).apply {
            leftMargin=dp(12);topMargin=dp(8)
        })

        val restart=chip("↻  "+ui("RESTART","RIFILLO","NEUSTART","RIPROVA"),accent=true){confirmRestart()}
        restart.contentDescription=ui("Restart level","Rifillo nivelin","Level neu starten","Riavvia il livello")
        host.addView(restart,FrameLayout.LayoutParams(dp(126),dp(46),Gravity.TOP or Gravity.END).apply {
            rightMargin=dp(12);topMargin=dp(8)
        })

        val status=statusChip(ui(
            "BLOCK PUZZLE  •  AUTO-SAVE  •  250 LEVELS",
            "BLOCK PUZZLE  •  AUTO-RUAJTJE  •  250 NIVELE",
            "BLOCK PUZZLE  •  AUTO-SPEICHERN  •  250 LEVEL",
            "BLOCK PUZZLE  •  SALVATAGGIO AUTO  •  250 LIVELLI"
        ))
        host.addView(status,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,dp(34),Gravity.TOP).apply {
            leftMargin=dp(16);rightMargin=dp(16);topMargin=dp(60)
        })
        setContentView(host)
    }

    private fun confirmRestart(){
        if(dialogOpen)return
        dialogOpen=true
        val dialog=AlertDialog.Builder(this)
            .setTitle(ui("Restart level?","Rifillo nivelin?","Level neu starten?","Riavviare il livello?"))
            .setMessage(ui(
                "Your current board and level score will restart. Campaign progress and best score stay saved.",
                "Tabela dhe pikët e nivelit aktual do të rifillojnë. Progresi i fushatës dhe rekordi mbeten të ruajtura.",
                "Das aktuelle Spielfeld und die Level-Punkte werden neu gestartet. Kampagnenfortschritt und Bestwert bleiben gespeichert.",
                "La griglia e il punteggio del livello attuale verranno riavviati. Campagna e record resteranno salvati."
            ))
            .setNegativeButton(ui("CANCEL","ANULO","ABBRECHEN","ANNULLA"),null)
            .setPositiveButton(ui("RESTART","RIFILLO","NEUSTART","RIPROVA")){_,_->game.restart()}
            .create()
        dialog.setOnDismissListener{dialogOpen=false;enterImmersive()}
        dialog.setOnShowListener{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xffffb72f.toInt())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xff58c8ff.toInt())
        }
        dialog.show()
    }

    override fun onWindowFocusChanged(hasFocus:Boolean){
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus&&!dialogOpen)enterImmersive()
    }

    override fun onResume(){
        super.onResume()
        if(!dialogOpen)enterImmersive()
    }

    private fun enterImmersive(){
        if(android.os.Build.VERSION.SDK_INT>=30){
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let{controller->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior=WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }else{
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility=
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}
