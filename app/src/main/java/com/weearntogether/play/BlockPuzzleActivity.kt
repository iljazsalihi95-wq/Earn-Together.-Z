package com.weearntogether.play

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
    private lateinit var ads:NativeGameAds
    private lateinit var game: PremiumBlockPuzzleView
    private lateinit var status: TextView
    private val loyalty by lazy { LoyaltyProgress(this) }
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
        this.text=text;textSize=10f;setTextColor(0xffffd66b.toInt());gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);letterSpacing=.04f;elevation=dp(6).toFloat()
        background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xdd102a54.toInt(),0xdd24164d.toInt(),0xdd0b5f9f.toInt())).apply{cornerRadius=dp(16).toFloat();setStroke(dp(1),0x88ffc857.toInt())}
    }

    private fun savedProgressLabel():String{
        val prefs=ProgressPreferences.open(this,"block_puzzle_hd2d")
        val level=prefs.getInt("level",1).coerceIn(1,250)
        val best=prefs.getInt("best",0).coerceAtLeast(0)
        return ui("AUTO-SAVE • LEVEL $level/250 • BEST $best","AUTO-RUAJTJE • NIVELI $level/250 • REKORDI $best","AUTO-SPEICHERN • LEVEL $level/250 • BEST $best","SALVATAGGIO AUTO • LIVELLO $level/250 • BEST $best")
    }

    private fun refreshSavedProgress(){
        if(::status.isInitialized){status.text=savedProgressLabel();status.contentDescription=status.text}
    }

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);ads=NativeGameAds(this,"block");requestWindowFeature(Window.FEATURE_NO_TITLE);window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=Color.TRANSPARENT;window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);enterImmersive()
        game=PremiumBlockPuzzleView(this){loyalty.recordGameProgress(10);refreshSavedProgress();ads.naturalBreak()}
        val host=FrameLayout(this).apply{setBackgroundColor(0xff070d20.toInt());isClickable=true;isFocusable=true;addView(game,FrameLayout.LayoutParams(-1,-1))}
        val header=View(this).apply{background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff0a1a3b.toInt(),0xff241050.toInt(),0xff071a38.toInt())).apply{setStroke(dp(1),0x5569cfff)};elevation=dp(4).toFloat()}
        host.addView(header,FrameLayout.LayoutParams(-1,dp(108),Gravity.TOP))
        host.addView(TextView(this).apply{text="BLOCK PUZZLE HD 2D";textSize=17f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);letterSpacing=.07f;elevation=dp(8).toFloat()},FrameLayout.LayoutParams(dp(190),dp(46),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=dp(8)})
        host.addView(chip("‹  "+ui("BACK","PRAPA","ZURÜCK","INDIETRO")){finish()},FrameLayout.LayoutParams(dp(104),dp(46),Gravity.TOP or Gravity.START).apply{leftMargin=dp(12);topMargin=dp(8)})
        host.addView(chip("↻  "+ui("RESTART","RIFILLO","NEUSTART","RIPROVA"),true){confirmRestart()},FrameLayout.LayoutParams(dp(126),dp(46),Gravity.TOP or Gravity.END).apply{rightMargin=dp(12);topMargin=dp(8)})
        status=statusChip(savedProgressLabel()).apply{contentDescription=text};host.addView(status,FrameLayout.LayoutParams(dp(300),dp(32),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=dp(64)})

        val guide=TextView(this).apply{text=ui("DRAG A PIECE TO THE BOARD  •  CLEAR ROWS & COLUMNS","TËRHIQ PJESËN NË TABELË  •  PASTRO RRESHTA & KOLONA","STEIN AUFS FELD ZIEHEN  •  REIHEN & SPALTEN LÖSCHEN","TRASCINA IL PEZZO  •  COMPLETA RIGHE E COLONNE");textSize=10f;setTextColor(0xffb8c9ef.toInt());gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);letterSpacing=.035f;background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xcc09172f.toInt(),0xcc241248.toInt(),0xcc09172f.toInt())).apply{cornerRadius=dp(15).toFloat();setStroke(dp(1),0x4458c8ff)}}
        host.addView(guide,FrameLayout.LayoutParams(-1,dp(30),Gravity.BOTTOM).apply{leftMargin=dp(20);rightMargin=dp(20);bottomMargin=dp(52)})

        val videoBonus=TextView(this).apply{text=ui("VIDEO • CLEAR","VIDEO • PASTRO","VIDEO • LÖSCHEN","VIDEO • CANCELLA");contentDescription=text;textSize=9f;setTextColor(0xff07152c.toInt());gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);isClickable=true;isFocusable=true;elevation=dp(9).toFloat();background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xffffe27a.toInt(),0xffffb62e.toInt(),0xffffd65d.toInt())).apply{cornerRadius=dp(18).toFloat();setStroke(dp(1),0xffffefae.toInt())};setOnClickListener{performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);ads.rewarded(onGameplayReward={game.videoRescue();refreshSavedProgress()})}}
        host.addView(videoBonus,FrameLayout.LayoutParams(dp(116),dp(40),Gravity.BOTTOM or Gravity.START).apply{leftMargin=dp(10);bottomMargin=dp(6)})

        val knife=chip(ui("KNIFE","THIKA","MESSER","COLTELLI")){startActivity(Intent(this,KnifeHitActivity::class.java))}
        knife.contentDescription=ui("Open Knife Hit","Hap Knife Hit","Knife Hit öffnen","Apri Knife Hit")
        host.addView(knife,FrameLayout.LayoutParams(dp(96),dp(40),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{bottomMargin=dp(6)})

        val nextGame=chip(ui("BUBBLES ›","TOPAT ›","BUBBLES ›","BOLLE ›")){startActivity(Intent(this,BubbleShooterActivity::class.java))}
        nextGame.contentDescription=ui("Open Bubble Shooter","Hap Bubble Shooter","Bubble Shooter öffnen","Apri Bubble Shooter")
        host.addView(nextGame,FrameLayout.LayoutParams(dp(116),dp(40),Gravity.BOTTOM or Gravity.END).apply{rightMargin=dp(10);bottomMargin=dp(6)})
        setContentView(host)
    }

    private fun confirmRestart(){
        if(dialogOpen)return;dialogOpen=true
        val dialog=AlertDialog.Builder(this).setTitle(ui("Restart level?","Rifillo nivelin?","Level neu starten?","Riavviare il livello?"))
            .setMessage(ui("The current board restarts. Saved best progress stays intact.","Tabela aktuale rifillon. Progresi më i mirë mbetet i ruajtur.","Das aktuelle Spielfeld startet neu. Der Bestfortschritt bleibt gespeichert.","La griglia attuale riparte. I migliori progressi restano salvati."))
            .setNegativeButton(ui("CANCEL","ANULO","ABBRECHEN","ANNULLA"),null).setPositiveButton(ui("RESTART","RIFILLO","NEUSTART","RIPROVA")){_,_->game.restart();refreshSavedProgress()}.create()
        dialog.setOnDismissListener{dialogOpen=false;refreshSavedProgress();enterImmersive()};dialog.show()
    }
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus&&!dialogOpen)enterImmersive()}
    override fun onResume(){super.onResume();if(::ads.isInitialized)ads.resume();refreshSavedProgress();if(!dialogOpen)enterImmersive()}
    private fun enterImmersive(){if(android.os.Build.VERSION.SDK_INT>=30){window.setDecorFitsSystemWindows(false);window.insetsController?.let{it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars());it.systemBarsBehavior=WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE}}else{@Suppress("DEPRECATION") window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE}}
    override fun onPause(){if(::ads.isInitialized)ads.pause();super.onPause()}
    override fun onDestroy(){if(::ads.isInitialized)ads.destroy();super.onDestroy()}
}
