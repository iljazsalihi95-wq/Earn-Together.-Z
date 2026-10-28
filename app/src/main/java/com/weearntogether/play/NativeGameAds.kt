package com.weearntogether.play

import android.app.Activity
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.gms.ads.interstitial.*

/** One AdMob controller per native screen. Only SDK completion callbacks create receipts. */
class NativeGameAds(private val activity:Activity,private val game:String){
    private var rewarded:RewardedAd?=null;private var interstitial:InterstitialAd?=null;private var banner:AdView?=null
    private var disposed=false;private var lastBreak=0L
    private val account=AccountStore(activity)
    private val receipts=account.currentUserId()?.let{Html5VideoRewards(activity,it)}
    private val qa=BuildConfig.DEBUG&&activity.intent.getBooleanExtra("native_qa",false)
    init{if(!qa){receipts?.prepare(game);MobileAds.initialize(activity){activity.runOnUiThread{if(!disposed){loadRewarded();loadInterstitial()}}}}}
    private fun loadRewarded(){RewardedAd.load(activity,BuildConfig.ADMOB_REWARDED_ID,AdRequest.Builder().build(),object:RewardedAdLoadCallback(){override fun onAdLoaded(ad:RewardedAd){if(!disposed)rewarded=ad};override fun onAdFailedToLoad(error:LoadAdError){rewarded=null}})}
    private fun loadInterstitial(){InterstitialAd.load(activity,BuildConfig.ADMOB_INTERSTITIAL_ID,AdRequest.Builder().build(),object:InterstitialAdLoadCallback(){override fun onAdLoaded(ad:InterstitialAd){if(!disposed)interstitial=ad};override fun onAdFailedToLoad(error:LoadAdError){interstitial=null}})}
    fun banner(parent:LinearLayout){if(qa||disposed)return;banner?.destroy();banner=AdView(activity).also{view->view.adUnitId=BuildConfig.ADMOB_BANNER_ID;parent.addView(view,LinearLayout.LayoutParams(-1,-2));view.post{if(!disposed){val width=(parent.width/activity.resources.displayMetrics.density).toInt().coerceAtLeast(250);view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,width));view.loadAd(AdRequest.Builder().build())}}}}
    fun rewarded(onGameplayReward:()->Unit={},onFinished:()->Unit={}):Boolean{
        if(qa||disposed)return false
        val ad=rewarded?:run{Toast.makeText(activity,"Video nuk është gati ende.",Toast.LENGTH_SHORT).show();loadRewarded();return false}
        if(receipts?.canWatch()==false)return false
        rewarded=null;receipts?.prepare(game)
        ad.fullScreenContentCallback=object:FullScreenContentCallback(){override fun onAdDismissedFullScreenContent(){onFinished();if(!disposed)loadRewarded()};override fun onAdFailedToShowFullScreenContent(error:AdError){onFinished();if(!disposed)loadRewarded()}}
        var granted=false
        ad.show(activity){if(!granted&&!disposed){granted=true;receipts?.completed(game);onGameplayReward();Toast.makeText(activity,"Video u përfundua. Bonusi pret verifikimin e serverit.",Toast.LENGTH_LONG).show()}}
        return true
    }
    fun naturalBreak(onFinished:()->Unit={}):Boolean{if(qa||disposed||System.currentTimeMillis()-lastBreak<90_000)return false;val ad=interstitial?:return false;interstitial=null;lastBreak=System.currentTimeMillis();ad.fullScreenContentCallback=object:FullScreenContentCallback(){override fun onAdDismissedFullScreenContent(){onFinished();if(!disposed)loadInterstitial()};override fun onAdFailedToShowFullScreenContent(error:AdError){onFinished();if(!disposed)loadInterstitial()}};ad.show(activity);return true}
    fun resume(){banner?.resume();receipts?.sync()}
    fun pause(){banner?.pause()}
    fun destroy(){disposed=true;banner?.destroy();banner=null;rewarded=null;interstitial=null}
}
