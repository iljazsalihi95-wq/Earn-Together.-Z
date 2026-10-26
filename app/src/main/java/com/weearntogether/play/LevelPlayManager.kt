package com.weearntogether.play

import android.app.Activity
import android.util.Log
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener
import com.unity3d.mediation.rewarded.LevelPlayReward
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener
import com.unity3d.mediation.LevelPlayAdError

/** Session-scoped LevelPlay controller. Rewards are gameplay-only; no cash is created here. */
class LevelPlayManager(
    private val activity: Activity,
    private val userId: String,
    private val status: (String) -> Unit = {}
) {
    companion object {
        @Volatile private var active: LevelPlayManager? = null
        fun showLevelCompleteVideo(): Boolean = active?.showInterstitialAtNaturalBreak() ?: false
    }
    private fun trace(event:String){ Log.i("WET_LevelPlay", event); status(event) }
    private var disposed=false
    private var rewardFinished:(()->Unit)?=null
    private var breakFinished:(()->Unit)?=null
    private var initialized=false
    private var rewarded:LevelPlayRewardedAd?=null
    private var interstitial:LevelPlayInterstitialAd?=null
    private var pendingGameplayReward:(()->Unit)?=null
    private var lastInterstitialAt=0L
    private val minInterstitialGapMs=15_000L

    fun initialize(makeActive:Boolean=true){
        if(makeActive)active=this
        if(initialized)return
        val request=LevelPlayInitRequest.Builder(BuildConfig.LEVELPLAY_APP_KEY).withUserId(userId).build()
        LevelPlay.init(activity,request,object:LevelPlayInitListener{
            override fun onInitSuccess(configuration:LevelPlayConfiguration){if(disposed)return;initialized=true;createRewarded();createInterstitial();trace("LevelPlay initialized; loading ad units")}
            override fun onInitFailed(error:LevelPlayInitError){trace("LevelPlay initialization failed: $error")}
        })
    }
    private fun createRewarded(){
        rewarded=LevelPlayRewardedAd(BuildConfig.LEVELPLAY_REWARDED_AD_UNIT).also{ad->
            ad.setListener(object:LevelPlayRewardedAdListener{
                override fun onAdLoaded(adInfo:LevelPlayAdInfo){trace("Rewarded loaded")}
                override fun onAdLoadFailed(error:LevelPlayAdError){trace("Rewarded load failed: $error")}
                override fun onAdDisplayed(adInfo:LevelPlayAdInfo){trace("Rewarded impression displayed")}
                override fun onAdDisplayFailed(error:LevelPlayAdError,adInfo:LevelPlayAdInfo){trace("Rewarded display failed: $error");pendingGameplayReward=null;rewardFinished?.invoke();rewardFinished=null;if(!disposed)ad.loadAd()}
                override fun onAdClicked(adInfo:LevelPlayAdInfo){}
                override fun onAdClosed(adInfo:LevelPlayAdInfo){pendingGameplayReward=null;rewardFinished?.invoke();rewardFinished=null;if(!disposed)ad.loadAd()}
                override fun onAdInfoChanged(adInfo:LevelPlayAdInfo){}
                override fun onAdRewarded(reward:LevelPlayReward,adInfo:LevelPlayAdInfo){trace("Rewarded completion callback");pendingGameplayReward?.invoke();pendingGameplayReward=null}
            });if(!disposed)ad.loadAd()
        }
    }
    private fun createInterstitial(){
        interstitial=LevelPlayInterstitialAd(BuildConfig.LEVELPLAY_INTERSTITIAL_AD_UNIT).also{ad->
            ad.setListener(object:LevelPlayInterstitialAdListener{
                override fun onAdLoaded(adInfo:LevelPlayAdInfo){trace("Interstitial loaded")}
                override fun onAdLoadFailed(error:LevelPlayAdError){trace("Interstitial load failed: $error")}
                override fun onAdDisplayed(adInfo:LevelPlayAdInfo){trace("Interstitial impression displayed");lastInterstitialAt=System.currentTimeMillis()}
                override fun onAdDisplayFailed(error:LevelPlayAdError,adInfo:LevelPlayAdInfo){trace("Interstitial display failed: $error");breakFinished?.invoke();breakFinished=null;if(!disposed)ad.loadAd()}
                override fun onAdClicked(adInfo:LevelPlayAdInfo){}
                override fun onAdClosed(adInfo:LevelPlayAdInfo){breakFinished?.invoke();breakFinished=null;if(!disposed)ad.loadAd()}
                override fun onAdInfoChanged(adInfo:LevelPlayAdInfo){}
            });if(!disposed)ad.loadAd()
        }
    }
    fun showRewardedForGameplay(onReward:()->Unit):Boolean = showRewardedForGameplayWithCompletion(onReward,{})
    fun showRewardedForGameplayWithCompletion(onReward:()->Unit,onFinished:()->Unit):Boolean{if(disposed||rewardFinished!=null)return false;val ad=rewarded?:return false;if(!ad.isAdReady)return false;pendingGameplayReward=onReward;rewardFinished=onFinished;ad.showAd(activity);return true}
    /** Called only at genuine game/level breaks. It attempts a video every completed level when inventory is ready. */
    fun showInterstitialAtNaturalBreak(onFinished:(()->Unit)?=null):Boolean{
        if(disposed||breakFinished!=null)return false;
        if(System.currentTimeMillis()-lastInterstitialAt<minInterstitialGapMs)return false
        val ad=interstitial?:return false
        if(!ad.isAdReady){if(!disposed)ad.loadAd();return false}
        breakFinished=onFinished;ad.showAd(activity);return true
    }
    fun dispose(){disposed=true;pendingGameplayReward=null;rewardFinished=null;breakFinished=null;if(active===this)active=null}
}

