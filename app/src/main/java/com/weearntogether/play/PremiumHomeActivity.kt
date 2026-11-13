package com.weearntogether.play

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*

/** Premium discovery home using only WET artwork/components. */
class PremiumHomeActivity : Activity() {
    private val navy=0xff07152f.toInt(); private val blue=0xff3978f6.toInt(); private val purple=0xff713fe8.toInt(); private val ink=0xff202432.toInt(); private val muted=0xff697386.toInt(); private val gold=0xffffc857.toInt()
    private lateinit var ads:NativeGameAds;private lateinit var root:LinearLayout; private lateinit var host:FrameLayout; private lateinit var rewardButton:Button; private lateinit var coinText:TextView
    private val loyalty by lazy { LoyaltyProgress(this) }; private val accounts by lazy { AccountStore(this) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun round(color:Int,r:Int=22)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    private fun premiumGradient(r:Int=22)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(navy,purple,blue)).apply{cornerRadius=dp(r).toFloat()}
    private fun text(s:String,size:Int,color:Int=ink,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);if(bold)setTypeface(null,Typeface.BOLD)}
    override fun onCreate(state:Bundle?){super.onCreate(state);window.statusBarColor=navy;window.navigationBarColor=navy
        if(!accounts.isSignedIn()&&!(BuildConfig.DEBUG&&intent.getBooleanExtra("native_qa",false))){startActivity(Intent(this,MainActivity::class.java));finish();return}
        ads=NativeGameAds(this,"home");build();rewardButton.isEnabled=true
    }
    private fun launchRealLazyGarden(){
        try {
            startActivity(Intent().setClassName("com.lazygarden.game", "com.lazygarden.game.MainActivity"))
        } catch (_: ActivityNotFoundException) {
            AlertDialog.Builder(this)
                .setTitle("LazyGarden")
                .setMessage("Për të luajtur, instalo aplikacionin origjinal LazyGarden në këtë telefon.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
    private fun build(){host=FrameLayout(this).apply{setBackgroundColor(0xfff5f7ff.toInt())};val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false};root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),0,dp(16),dp(108))};scroll.addView(root);host.addView(scroll,FrameLayout.LayoutParams(-1,-1));setContentView(host);host.setOnApplyWindowInsetsListener{v,i->v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom);i};host.requestApplyInsets();header();sectionTitle("✦","Lojërat e tua","LUAJ • VAZHDO");gameCatalog();rewardCard();bottomNav()}
    private fun header(){val h=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(12),dp(16),dp(14));background=premiumGradient(0);minimumHeight=dp(82)};val portrait=PlayerPortrait.image(this).apply{setOnClickListener{PlayerPortrait.pick(this@PremiumHomeActivity)}};h.addView(portrait,LinearLayout.LayoutParams(dp(48),dp(48)).apply{rightMargin=dp(12)});val identity=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};identity.addView(text("WE EARN TOGETHER",11,gold,true));identity.addView(text(accounts.currentName()?:"Lojtari",20,Color.WHITE,true));h.addView(identity,LinearLayout.LayoutParams(0,-2,1f));coinText=text("★ ${loyalty.points}",16,gold,true).apply{gravity=Gravity.CENTER;background=round(0x33202a55,18);setPadding(dp(12),dp(8),dp(12),dp(8))};h.addView(coinText);root.addView(h,LinearLayout.LayoutParams(-1,-2).apply{leftMargin=-dp(16);rightMargin=-dp(16);bottomMargin=dp(14)})}
    private fun sectionTitle(icon:String,title:String,more:String){val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};r.addView(text("$icon  $title",20,ink,true),LinearLayout.LayoutParams(0,-2,1f));r.addView(text(more,11,purple,true));root.addView(r,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8);bottomMargin=dp(12)})}
    private data class HomeGame(val key:String,val title:String,val subtitle:String,val activity:Class<out Activity>?=null,val htmlKey:String?=null)
    private fun gameCatalog(){
        val games=listOf(
            HomeGame("arena","QUIZ ARENA","Pyetje • studio • nivele • zë",ArenaCloudActivity::class.java),
            HomeGame("garden","LAZYGARDEN","Loja origjinale • Match-3 • restaurim"),
            HomeGame("knife","BLADE QUEST","Thika • botë magjike • arsenal",KnifeHitActivity::class.java),
            HomeGame("block","BLOCK PUZZLE","250 nivele • kombinime • progres",BlockPuzzleActivity::class.java),
            HomeGame("bubble","BUBBLE SHOOTER","250 nivele • shënjim • kombinime",BubbleShooterActivity::class.java),
            HomeGame("balls","Ball Sort","Rendit ngjyrat • sfida progresive",BallSortActivity::class.java),
            HomeGame("cups","Cup Shuffle","Ndiq topin • 250 nivele",CupShuffleActivity::class.java),
            HomeGame("word","Word Trails","Gjej fjalët • hartë nivelesh",htmlKey="word"),
            HomeGame("memory","Memory Bloom","Gjej çiftet • ushtro kujtesën",htmlKey="memory"),
            HomeGame("merge","2048","Bashko numrat • arrij objektivin",htmlKey="merge"),
            HomeGame("falling","Falling Blocks","Vendos figurat • pastro rreshtat",htmlKey="falling"),
            HomeGame("arrows","Arrow Escape","Liro shigjetat • zgjidh enigmën",htmlKey="arrows"),
            HomeGame("treasure","Treasure Quest","Zbulo thesarin • sfida logjike",htmlKey="treasure"),
            HomeGame("water","Water Sort","Ndaj ngjyrat • mbush enët",htmlKey="water"),
            HomeGame("park","Parking Escape","Liro makinat • hap rrugën",htmlKey="park"),
            HomeGame("lines","Color Lines","Lidh ngjyrat • planifiko lëvizjet",htmlKey="lines"),
            HomeGame("stack","Sky Stack","Ndërto kullën • saktësi dhe ritëm",htmlKey="stack"),
            HomeGame("gamepix","GamePix","Eksploro bibliotekën e lojërave",GamePixActivity::class.java)
        )
        games.forEach { game ->
            val card=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
                tag="game:"+game.key;contentDescription=game.title+". "+game.subtitle+". LUAJ"
                isFocusable=true;setPadding(dp(10),dp(10),dp(14),dp(10))
                background=premiumGradient(24);elevation=dp(4).toFloat()
                setOnClickListener{
                    when{
                        game.key=="garden"->launchRealLazyGarden()
                        game.htmlKey!=null->startActivity(Intent(this@PremiumHomeActivity,Html5GamesActivity::class.java).putExtra("game",game.htmlKey))
                        game.activity!=null->startActivity(Intent(this@PremiumHomeActivity,game.activity))
                    }
                }
            }
            val art:android.view.View=if(game.key in setOf("arena","garden","knife","block","bubble","balls"))StudioArtwork(this,game.key) else GameArtwork(this,game.key)
            art.apply{background=round(0x22ffffff,18);clipToOutline=true;importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO}
            card.addView(art,LinearLayout.LayoutParams(dp(108),dp(112)).apply{rightMargin=dp(14)})
            val words=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            words.addView(text(game.title,19,Color.WHITE,true))
            words.addView(text(game.subtitle,12,0xffdce5ff.toInt()).apply{setPadding(0,dp(5),0,dp(8))})
            words.addView(text("LUAJ  ›",14,gold,true))
            card.addView(words,LinearLayout.LayoutParams(0,-2,1f))
            root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(13)})
        }
    }
    private fun rewardCard(){val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(10));background=round(0xfffff8e8.toInt(),18)};val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};row.addView(text("★",30,gold,true),LinearLayout.LayoutParams(dp(42),-2));val words=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};words.addView(text("Bonus me video",15,ink,true));words.addView(text("Vetëm pas verifikimit",12,muted));row.addView(words,LinearLayout.LayoutParams(0,-2,1f));rewardButton=Button(this).apply{text="Shiko";isAllCaps=false;textSize=14f;setTextColor(Color.WHITE);background=round(purple,14);setOnClickListener{showRewarded()}};row.addView(rewardButton,LinearLayout.LayoutParams(dp(82),dp(44)));card.addView(row);card.addView(text("Bonusi regjistrohet vetëm pas përfundimit dhe verifikimit të videos; nuk premtohet shpërblim monetar.",10,muted),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)});root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})}
    private fun bottomNav(){val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=premiumGradient(26);elevation=dp(14).toFloat();setPadding(dp(8),dp(6),dp(8),dp(6))};fun nav(icon:String,label:String,go:()->Unit){val v=text("$icon\n$label",12,Color.WHITE,true).apply{gravity=Gravity.CENTER;setOnClickListener{go()}};bar.addView(v,LinearLayout.LayoutParams(0,dp(60),1f))};nav("◉","Lojëra"){};nav("✦","Oferta"){startActivity(Intent(this,AyetOfferwallActivity::class.java))};nav("▣","Portofoli"){startActivity(Intent(this,WalletActivity::class.java))};nav("☺","Profili"){startActivity(Intent(this,MainActivity::class.java).putExtra("open_profile",true))};host.addView(bar,FrameLayout.LayoutParams(-1,dp(78),Gravity.BOTTOM).apply{leftMargin=dp(10);rightMargin=dp(10);bottomMargin=dp(10)})}
    private fun showRewarded(){ads.rewarded(onGameplayReward={coinText.text="★ Në pritje";refreshBalance()})}
    private fun refreshBalance(){if(!accounts.isSignedIn()||!::coinText.isInitialized)return;CloudWorker.executor.execute{val balance=runCatching{BackendClient().request("getBalance",org.json.JSONObject().put("session_token",accounts.requireToken()))}.getOrNull();runOnUiThread{if(!isFinishing&&!isDestroyed&&balance!=null){val pending=balance.optLong("pending_coins");coinText.text="★ ${balance.optLong("verified_coins")}"+if(pending>0)" (+$pending)" else ""}}}}
    override fun onResume(){super.onResume();if(::ads.isInitialized)ads.resume();refreshBalance()}
    override fun onPause(){if(::ads.isInitialized)ads.pause();super.onPause()}
    override fun onDestroy(){if(::ads.isInitialized)ads.destroy();super.onDestroy()}
}
