package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.weearntogether.lazygarden.LazyGardenActivity
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Premium discovery home using only WET artwork/components. */
class PremiumHomeActivity : Activity() {
    private val blue=0xff4d7df3.toInt(); private val ink=0xff202432.toInt(); private val muted=0xff697386.toInt(); private val gold=0xffffbd4a.toInt()
    private lateinit var ads:NativeGameAds;private lateinit var root:LinearLayout; private lateinit var host:FrameLayout; private var rewarded:RewardedAd?=null; private lateinit var rewardButton:Button; private lateinit var coinText:TextView
    private val loyalty by lazy { LoyaltyProgress(this) }; private val accounts by lazy { AccountStore(this) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun round(color:Int,r:Int=22)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    private fun text(s:String,size:Int,color:Int=ink,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);if(bold)setTypeface(null,Typeface.BOLD)}
    override fun onCreate(state:Bundle?){super.onCreate(state);window.statusBarColor=blue;window.navigationBarColor=Color.WHITE
        if(!accounts.isSignedIn()&&!(BuildConfig.DEBUG&&intent.getBooleanExtra("native_qa",false))){startActivity(Intent(this,MainActivity::class.java));finish();return}
        ads=NativeGameAds(this,"home");build();rewardButton.isEnabled=true;rewardButton.text="SHIKO VIDEO"
    }
    private fun build(){
        host=FrameLayout(this).apply{setBackgroundColor(Color.WHITE)}
        val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false}
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),0,dp(16),dp(108))};scroll.addView(root)
        host.addView(scroll,FrameLayout.LayoutParams(-1,-1));setContentView(host)
        host.setOnApplyWindowInsetsListener{v,i->v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom);i};host.requestApplyInsets()
        header();sectionTitle("★","Zgjedhja e sotme","Arena");featured();rewardCard();sectionTitle("🔥","Më të luajturat","Të gjitha");gameRow();sectionTitle("✦","Eksploro lojërat","Më shumë");compactGames();bottomNav()
    }
    private fun header(){
        val h=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(20),dp(16),dp(22));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xff476aef.toInt(),0xff569cf9.toInt()));minimumHeight=dp(104)}
        val portrait=PlayerPortrait.image(this).apply{setOnClickListener{PlayerPortrait.pick(this@PremiumHomeActivity)}}
        h.addView(portrait,LinearLayout.LayoutParams(dp(48),dp(48)).apply{rightMargin=dp(12)})
        h.addView(text(accounts.currentName()?:"Lojtari",21,Color.WHITE,true).apply{maxLines=2},LinearLayout.LayoutParams(0,-2,1f))
        coinText=text("★ ${loyalty.points}",16,Color.WHITE,true);h.addView(coinText)
        root.addView(h,LinearLayout.LayoutParams(-1,-2).apply{leftMargin=-dp(16);rightMargin=-dp(16);bottomMargin=dp(14)})
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PlayerPortrait.REQUEST){if(PlayerPortrait.receive(this,resultCode,data))build()}}

    private fun sectionTitle(icon:String,title:String,more:String){
        val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        r.addView(text("$icon  $title",21,ink,true),LinearLayout.LayoutParams(0,-2,1f))
        r.addView(text("$more ›",12,blue,true).apply{setOnClickListener{startActivity(Intent(this@PremiumHomeActivity,Html5GamesActivity::class.java))}})
        root.addView(r,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10);bottomMargin=dp(14)})
    }
    private fun featured(){
        val c=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(10),dp(12),dp(10));background=round(0xffeff7ff.toInt(),18);elevation=dp(3).toFloat();setOnClickListener{startActivity(Intent(this@PremiumHomeActivity,QuizArenaActivity::class.java))}}
        c.addView(StudioArtwork(this,"arena").apply{background=round(Color.WHITE,14);clipToOutline=true},LinearLayout.LayoutParams(dp(94),dp(96)).apply{rightMargin=dp(12)})
        val words=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};words.addView(text("Quiz Arena",21,ink,true));words.addView(text("LOJA JONË",11,blue,true));words.addView(text("Sfidë diturie • 60 sekonda",13,muted));c.addView(words,LinearLayout.LayoutParams(0,-2,1f))
        c.addView(Button(this).apply{text="Luaj";isAllCaps=false;textSize=16f;setTextColor(Color.WHITE);background=round(blue,14);setOnClickListener{startActivity(Intent(this@PremiumHomeActivity,QuizArenaActivity::class.java))}},LinearLayout.LayoutParams(dp(72),dp(46)))
        root.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(18)})
    }
    private fun rewardCard(){val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=round(0xfff8f9fc.toInt(),22)};val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};top.addView(text("🪙",35,gold,true));val words=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0)};words.addView(text("Bonus Coins me Video",17,muted,true));words.addView(text("Shiko video për bonus",19,gold,true));top.addView(words,LinearLayout.LayoutParams(0,-2,1f));c.addView(top);rewardButton=Button(this).apply{text="VIDEO DUKE U PËRGATITUR…";isAllCaps=false;isEnabled=false;setTextColor(Color.WHITE);background=round(blue,16);setOnClickListener{showRewarded()}};c.addView(rewardButton,LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(12)});c.addView(text("Shpërblimi jepet vetëm pas përfundimit të verifikuar të videos. Bonus coins janë pikë loje, jo para të garantuara.",11,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)});root.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(22)})}
    private fun gameRow(){val hsv=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;clipToPadding=false};val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        gameTile(row,"garden","LazyGarden","Restauro pronën"){startActivity(Intent(this,LazyGardenActivity::class.java))}
        gameTile(row,"block","Block Puzzle","Bashko dhe pastro"){startActivity(Intent(this,BlockPuzzleActivity::class.java))}
        gameTile(row,"bubble","Bubble Shooter","Shënjo dhe gjuaj"){startActivity(Intent(this,BubbleShooterActivity::class.java))}
        gameTile(row,"arena","Quiz Arena","Loja jonë"){startActivity(Intent(this,QuizArenaActivity::class.java))}
        hsv.addView(row);root.addView(hsv,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(20)})}

    private fun gameTile(row:LinearLayout,key:String,title:String,subtitle:String,go:()->Unit){
        val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(5),dp(5),dp(5),dp(8));background=round(Color.WHITE,18);elevation=dp(3).toFloat();setOnClickListener{go()}}
        c.addView(StudioArtwork(this,key).apply{background=round(blue,14);clipToOutline=true;contentDescription=title},LinearLayout.LayoutParams(-1,dp(232)))
        c.addView(text(title,18,ink,true).apply{setPadding(dp(8),dp(8),0,0)})
        c.addView(Button(this).apply{text="LUAJ";textSize=16f;setTextColor(blue);background=round(0xffedf3ff.toInt(),12);setOnClickListener{go()}},LinearLayout.LayoutParams(-1,dp(46)).apply{topMargin=dp(6)})
        row.addView(c,LinearLayout.LayoutParams(dp(174),-2).apply{rightMargin=dp(12);topMargin=dp(5);bottomMargin=dp(7)})
    }
    private fun compactGames(){
        val ball=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=round(0xfff2f7ff.toInt(),18);setOnClickListener{startActivity(Intent(this@PremiumHomeActivity,BallSortActivity::class.java))}}
        ball.addView(BallSortCover(this),LinearLayout.LayoutParams(dp(80),dp(80)).apply{rightMargin=dp(14)});ball.addView(text("Ball Sort",20,ink,true),LinearLayout.LayoutParams(0,-2,1f));ball.addView(text("LUAJ ›",15,blue,true));root.addView(ball,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        val names=listOf("2048" to "2048", "word" to "Word Trails","memory" to "Memory Bloom","cups" to "Cup Shuffle","gamepix" to "GamePix")
        for((key,title) in names){val c=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));background=round(0xfff7f8fb.toInt(),18);setOnClickListener{when(key){"cups"->startActivity(Intent(this@PremiumHomeActivity,CupShuffleActivity::class.java));"gamepix"->startActivity(Intent(this@PremiumHomeActivity,GamePixActivity::class.java));else->startActivity(Intent(this@PremiumHomeActivity,Html5GamesActivity::class.java).putExtra("game",key))}}};c.addView(GameArtwork(this,key),LinearLayout.LayoutParams(dp(60),dp(60)).apply{rightMargin=dp(14)});c.addView(text(title,18,ink,true),LinearLayout.LayoutParams(0,-2,1f));c.addView(text("LUAJ ›",14,blue,true));root.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})}
    }
    private fun bottomNav(){
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=round(Color.WHITE,26);elevation=dp(12).toFloat();setPadding(dp(8),dp(6),dp(8),dp(6))}
        fun nav(icon:String,label:String,go:()->Unit){val v=text("$icon\n$label",12,blue,true).apply{gravity=Gravity.CENTER;setOnClickListener{go()}};bar.addView(v,LinearLayout.LayoutParams(0,dp(60),1f))}
        nav("◉","Lojëra"){};nav("♧","Oferta"){startActivity(Intent(this,AyetOfferwallActivity::class.java))};nav("▣","Portofoli"){startActivity(Intent(this,WalletActivity::class.java))};nav("☺","Profili"){startActivity(Intent(this,MainActivity::class.java).putExtra("open_profile",true))}
        host.addView(bar,FrameLayout.LayoutParams(-1,dp(78),Gravity.BOTTOM).apply{leftMargin=dp(10);rightMargin=dp(10);bottomMargin=dp(10)})
    }
    private fun loadRewarded(){RewardedAd.load(this,BuildConfig.ADMOB_REWARDED_ID,AdRequest.Builder().build(),object:RewardedAdLoadCallback(){override fun onAdLoaded(ad:RewardedAd){rewarded=ad;rewardButton.isEnabled=true;rewardButton.text="SHIKO VIDEO • +25 COINS"};override fun onAdFailedToLoad(e:LoadAdError){rewarded=null;rewardButton.isEnabled=false;rewardButton.text="VIDEO NUK ËSHTË GATI"}})}
    private fun showRewarded(){ads.rewarded()}
    override fun onResume(){super.onResume();if(::ads.isInitialized)ads.resume()}
    override fun onPause(){if(::ads.isInitialized)ads.pause();super.onPause()}
    override fun onDestroy(){if(::ads.isInitialized)ads.destroy();super.onDestroy()}
}
