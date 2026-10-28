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

/** Dedicated immersive host for the native HD 2D Block Puzzle. */
class BlockPuzzleActivity : Activity() {
    private lateinit var game: PremiumBlockPuzzleView
    private var dialogOpen=false
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private fun ui(en:String,sq:String,de:String,it:String):String=when(Locale.getDefault().language.lowercase(Locale.ROOT)){
        "sq"->sq; "de"->de; "it"->it; else->en
    }

    private fun chip(text:String, accent:Boolean=false, action:()->Unit)=TextView(this).apply {
        this.text=text;textSize=13f;setTextColor(if(accent)0xff07152c.toInt() else Color.WHITE);gravity=Gravity.CENTER
        setTypeface(null,Typeface.BOLD);isClickable=true;isFocusable=true;elevation=dp(10).toFloat()
        background=if(accent)GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xffffdf72.toInt(),0xffffb72f.toInt())).apply{cornerRadius=dp(23).toFloat();setStroke(dp(1),0xffffedaa.toInt())}
        else GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xf0162d5b.toInt(),0xf00a72c7.toInt())).apply{cornerRadius=dp(23).toFloat();setStroke(dp(1),0x8858c8ff.toInt())}
        setOnClickListener{performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);action()}
    }

    private fun statusChip(text:String)=TextView(this).apply{
        this.text=text;textSize=10f;setTextColor(0xffffd66b.toInt());gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);letterSpacing=.06f;elevation=dp(6).toFloat()
        background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xdd102a54.toInt(),0xdd24164d.toInt(),0xdd0b5f9f.toInt())).apply{cornerRadius=dp(16).toFloat();setStroke(dp(1),0x88ffc857.toInt())}
    }

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);requestWindowFeature(Window.FEATURE_NO_TITLE);window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=Color.TRANSPARENT;window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);enterImmersive()
        game=PremiumBlockPuzzleView(this)
        val host=FrameLayout(this).apply{setBackgroundColor(0xff070d20.toInt());isClickable=true;isFocusable=true;addView(game,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT))}
        val header=View(this).apply{background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff0a1a3b.toInt(),0xff241050.toInt(),0xff071a38.toInt())).apply{setStroke(dp(1),0x5569cfff)};elevation=dp(4).toFloat()}
        host.addView(header,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,dp(108),Gravity.TOP))
        val title=TextView(this).apply{text="BLOCK PUZZLE HD 2D";textSize=17f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);letterSpacing=.07f;elevation=dp(8).toFloat()}
        host.addView(title,FrameLayout.LayoutParams(dp(190),dp(46),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=dp(8)})
        val back=chip("‹  "+ui("BACK","PRAPA","ZURÜCK","INDIETRO")){finish()}
        host.addView(back,FrameLayout.LayoutParams(dp(104),dp(46),Gravity.TOP or Gravity.START).apply{leftMargin=dp(12);topMargin=dp(8)})
        val restart=chip("↻  "+ui("RESTART","RIFILLO","NEUSTART","RIPROVA"),true){confirmRestart()}
        host.addView(restart,FrameLayout.LayoutParams(dp(126),dp(46),Gravity.TOP or Gravity.END).apply{rightMargin=dp(12);topMargin=dp(8)})
        val status=statusChip(ui("NATIVE HD 2D  •  AUTO-SAVE  •  250 LEVELS","NATIVE HD 2D  •  AUTO-RUAJTJE  •  250 NIVELE","NATIVE HD 2D  •  AUTO-SPEICHERN  •  250 LEVEL","NATIVE HD 2D  •  SALVATAGGIO AUTO  •  250 LIVELLI"))
        host.addView(status,FrameLayout.LayoutParams(dp(280),dp(32),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=dp(64)})
        val guide=TextView(this).apply{
            text=ui("DRAG A PIECE TO THE BOARD  •  CLEAR ROWS & COLUMNS","TËRHIQ PJESËN NË TABELË  •  PASTRO RRESHTA & KOLONA","STEIN AUFS FELD ZIEHEN  •  REIHEN & SPALTEN LÖSCHEN","TRASCINA IL PEZZO  •  COMPLETA RIGHE E COLONNE")
            textSize=10f;setTextColor(0xffb8c9ef.toInt());gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);letterSpacing=.035f
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xcc09172f.toInt(),0xcc241248.toInt(),0xcc09172f.toInt())).apply{cornerRadius=dp(15).toFloat();setStroke(dp(1),0x4458c8ff)}
        }
        host.addView(guide,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,dp(32),Gravity.BOTTOM).apply{leftMargin=dp(20);rightMargin=dp(20);bottomMargin=dp(6)})
        setContentView(host)
    }

    private fun confirmRestart(){
        if(dialogOpen)return;dialogOpen=true
        val dialog=AlertDialog.Builder(this).setTitle(ui("Restart level?","Rifillo nivelin?","Level neu starten?","Riavviare il livello?"))
            .setMessage(ui("The current board restarts. Saved best progress stays intact.","Tabela aktuale rifillon. Progresi më i mirë mbetet i ruajtur.","Das aktuelle Spielfeld startet neu. Der Bestfortschritt bleibt gespeichert.","La griglia attuale riparte. I migliori progressi restano salvati."))
            .setNegativeButton(ui("CANCEL","ANULO","ABBRECHEN","ANNULLA"),null).setPositiveButton(ui("RESTART","RIFILLO","NEUSTART","RIPROVA")){_,_->game.restart()}.create()
        dialog.setOnDismissListener{dialogOpen=false;enterImmersive()};dialog.show()
    }
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus&&!dialogOpen)enterImmersive()}
    override fun onResume(){super.onResume();if(!dialogOpen)enterImmersive()}
    private fun enterImmersive(){if(android.os.Build.VERSION.SDK_INT>=30){window.setDecorFitsSystemWindows(false);window.insetsController?.let{it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars());it.systemBarsBehavior=WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE}}else{@Suppress("DEPRECATION") window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE}}
}
