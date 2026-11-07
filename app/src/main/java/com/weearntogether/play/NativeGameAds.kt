package com.weearntogether.play

import android.app.Activity
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.gms.ads.interstitial.*

/** One AdMob controller per native screen. Only SDK completion callbacks create receipts. */
class NativeGameAds(private val activity:Activity,private val game:String,private val gameplayOnly:Boolean=false){
    private var rewarded:RewardedAd?=null;private var interstitial:InterstitialAd?=null;private var banner:AdView?=null
    private var disposed=false;private var lastBreak=0L
    private val account=AccountStore(activity)
    private val receipts=if(gameplayOnly)null else account.currentUserId()?.let{Html5VideoRewards(activity,it)}
    private val qa=BuildConfig.DEBUG&&activity.intent.getBooleanExtra("native_qa",false)
    init{
        // Do not let an advertising SDK touch the Activity window while a native game is still
        // configuring its features/fullscreen state in onCreate(). Several game Activities request
        // window features immediately after constructing this controller. Initialising MobileAds
        // synchronously here can race that setup on real devices even though CI launch tests pass.
        // Posting also keeps gameplay startup independent from ad availability.
        if(!qa){
            activity.window.decorView.post{
                if(!disposed&&!activity.isFinishing&&!activity.isDestroyed){
                    runCatching{receipts?.prepare(game)}
                    runCatching{
                        MobileAds.initialize(activity){
                            activity.runOnUiThread{
                                if(!disposed&&!activity.isFinishing&&!activity.isDestroyed){
                                    loadRewarded();if(!gameplayOnly)loadInterstitial()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun loadRewarded(){if(disposed||activity.isFinishing||activity.isDestroyed)return;RewardedAd.load(activity,BuildConfig.ADMOB_REWARDED_ID,AdRequest.Builder().build(),object:RewardedAdLoadCallback(){override fun onAdLoaded(ad:RewardedAd){if(!disposed)rewarded=ad};override fun onAdFailedToLoad(error:LoadAdError){rewarded=null}})}
    private fun loadInterstitial(){if(disposed||activity.isFinishing||activity.isDestroyed)return;InterstitialAd.load(activity,BuildConfig.ADMOB_INTERSTITIAL_ID,AdRequest.Builder().build(),object:InterstitialAdLoadCallback(){override fun onAdLoaded(ad:InterstitialAd){if(!disposed)interstitial=ad};override fun onAdFailedToLoad(error:LoadAdError){interstitial=null}})}
    fun banner(parent:LinearLayout){if(qa||disposed||activity.isFinishing||activity.isDestroyed)return;banner?.destroy();banner=AdView(activity).also{view->view.adUnitId=BuildConfig.ADMOB_BANNER_ID;parent.addView(view,LinearLayout.LayoutParams(-1,-2));view.post{if(!disposed&&!activity.isFinishing&&!activity.isDestroyed){runCatching{val width=(parent.width/activity.resources.displayMetrics.density).toInt().coerceAtLeast(250);view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,width));view.loadAd(AdRequest.Builder().build())}}}}}
    fun rewarded(onGameplayReward:()->Unit={},onFinished:()->Unit={}):Boolean =
        showRewarded(onGameplayReward,onFinished,notifyUnavailable=true)

    fun rewardedForGameplay(onReward: () -> Unit, onFinished: () -> Unit): Boolean =
        showRewarded(onReward,onFinished,notifyUnavailable=false)

    private fun showRewarded(onGameplayReward: () -> Unit, onFinished: () -> Unit, notifyUnavailable: Boolean): Boolean {
        if(qa||disposed||activity.isFinishing||activity.isDestroyed)return false
        val ad=rewarded?:run{
            if(notifyUnavailable)Toast.makeText(activity,"Video nuk është gati ende.",Toast.LENGTH_SHORT).show()
            loadRewarded()
            return false
        }
        if(receipts?.canWatch()==false)return false
        rewarded=null
        receipts?.prepare(game)
        ad.fullScreenContentCallback=object:FullScreenContentCallback(){
            override fun onAdDismissedFullScreenContent(){onFinished();if(!disposed)loadRewarded()}
            override fun onAdFailedToShowFullScreenContent(error:AdError){onFinished();if(!disposed)loadRewarded()}
        }
        var granted=false
        ad.show(activity){
            if(!granted&&!disposed){
                granted=true
                if(!gameplayOnly){
                    receipts?.completed(game)
                    val awarded=GameCoinStore(activity).awardCompletedVideo()
                    Toast.makeText(activity,if(awarded)"+100 Game Coins. Earn Coins presin verifikimin e serverit." else "Video u përfundua. Bonusi i lojës u aplikua.",Toast.LENGTH_LONG).show()
                }
                onGameplayReward()
            }
        }
        return true
    }
    private var commercialIntro:ArenaCommercialIntro?=null
    fun naturalBreak(onFinished:()->Unit={}):Boolean{if(qa||disposed||activity.isFinishing||activity.isDestroyed||System.currentTimeMillis()-lastBreak<90_000)return false;val ad=interstitial?:return false;interstitial=null;lastBreak=System.currentTimeMillis();ad.fullScreenContentCallback=object:FullScreenContentCallback(){override fun onAdDismissedFullScreenContent(){onFinished();if(!disposed)loadInterstitial()};override fun onAdFailedToShowFullScreenContent(error:AdError){onFinished();if(!disposed)loadInterstitial()}};if(game.startsWith("quiz_arena")){
            commercialIntro?.cancel()
            commercialIntro=ArenaCommercialIntro(activity,{
                commercialIntro=null
                if(!disposed&&!activity.isFinishing&&!activity.isDestroyed)ad.show(activity) else onFinished()
            },{commercialIntro=null;if(!disposed)loadInterstitial();onFinished()}).also{it.show()}
        }else ad.show(activity)
        return true}
    fun resume(){if(!disposed)runCatching{banner?.resume()};runCatching{receipts?.sync()}}
    fun pause(){if(!disposed)runCatching{banner?.pause()}}
    fun destroy(){disposed=true;commercialIntro?.cancel();commercialIntro=null;runCatching{banner?.destroy()};banner=null;rewarded=null;interstitial=null}
}
