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
    private val userId: String?,
    private val status: (String) -> Unit = {}
) {
    companion object {
        @Volatile private var active: LevelPlayManager? = null
        fun showLevelCompleteVideo(): Boolean = active?.showInterstitialAtNaturalBreak() ?: false
    }
    private fun trace(event:String){ Log.i("WET_LevelPlay", event); status(event) }
    private var disposed=false
    private var breakFinished:(()->Unit)?=null
    private var initialized=false
    private class RewardAttempt(val grant: () -> Unit, val finish: () -> Unit, val fail: () -> Unit, val allowLateReward: Boolean)
    private class RewardSlot(val ad: LevelPlayRewardedAd) {
        var attempt: RewardAttempt? = null
        var closed = false
        var granted = false
    }
    private var rewarded:RewardSlot?=null
    private var interstitial:LevelPlayInterstitialAd?=null
    private var lastInterstitialAt=0L
    private val minInterstitialGapMs=15_000L

    fun initialize(makeActive:Boolean=true, rewardedOnly:Boolean=false){
        if(makeActive)active=this
        if(initialized)return
        val request=LevelPlayInitRequest.Builder(BuildConfig.LEVELPLAY_APP_KEY).apply { if(!userId.isNullOrBlank()) withUserId(userId) }.build()
        LevelPlay.init(activity,request,object:LevelPlayInitListener{
            override fun onInitSuccess(configuration:LevelPlayConfiguration){if(disposed)return;initialized=true;createRewarded();if(!rewardedOnly)createInterstitial();trace("LevelPlay initialized; loading ad units")}
            override fun onInitFailed(error:LevelPlayInitError){trace("LevelPlay initialization failed: $error")}
        })
    }
    private fun createRewarded() {
        if (disposed) return
        val slot = RewardSlot(LevelPlayRewardedAd(BuildConfig.LEVELPLAY_REWARDED_AD_UNIT))
        rewarded = slot
        val ad = slot.ad
        ad.setListener(object : LevelPlayRewardedAdListener {
            override fun onAdLoaded(adInfo: LevelPlayAdInfo) { trace("Rewarded loaded") }
            override fun onAdLoadFailed(error: LevelPlayAdError) { trace("Rewarded load failed: $error") }
            override fun onAdDisplayed(adInfo: LevelPlayAdInfo) { trace("Rewarded impression displayed") }
            override fun onAdClicked(adInfo: LevelPlayAdInfo) {}
            override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) {}
            override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) {
                if (disposed || slot.closed) return
                trace("Rewarded display failed: $error")
                slot.closed = true
                val attempt = slot.attempt
                slot.attempt = null
                createRewarded()
                attempt?.fail?.invoke()
            }
            override fun onAdClosed(adInfo: LevelPlayAdInfo) {
                if (disposed || slot.closed) return
                slot.closed = true
                // LevelPlay can confirm the reward after close. Keep its callback on this slot,
                // so a late confirmation cannot be credited to the next impression.
                val attempt = slot.attempt
                if (slot.granted || attempt?.allowLateReward != true) slot.attempt = null
                createRewarded()
                attempt?.finish?.invoke()
            }
            override fun onAdRewarded(reward: LevelPlayReward, adInfo: LevelPlayAdInfo) {
                if (disposed || slot.granted) return
                val attempt = slot.attempt ?: return
                slot.granted = true
                if (slot.closed) slot.attempt = null
                trace("Rewarded completion callback")
                attempt.grant()
            }
        })
        ad.loadAd()
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
    fun showRewardedForGameplayWithCompletion(onReward: () -> Unit, onFinished: () -> Unit): Boolean =
        showRewarded(onReward, onFinished, onFinished, allowLateReward = false)

    fun showRewardedForGameplayWithFallback(onReward: () -> Unit, onFinished: () -> Unit, onDisplayFailed: () -> Unit): Boolean =
        showRewarded(onReward, onFinished, onDisplayFailed, allowLateReward = true)

    private fun showRewarded(onReward: () -> Unit, onFinished: () -> Unit, onDisplayFailed: () -> Unit, allowLateReward: Boolean): Boolean {
        if (disposed) return false
        val slot = rewarded ?: return false
        if (slot.attempt != null) return false
        if (!slot.ad.isAdReady) {
            slot.ad.loadAd()
            return false
        }
        slot.attempt = RewardAttempt(onReward, onFinished, onDisplayFailed, allowLateReward)
        return runCatching { slot.ad.showAd(activity) }.isSuccess.also { shown ->
            if (!shown) {
                slot.attempt = null
                slot.closed = true
                createRewarded()
            }
        }
    }
    /** Called only at genuine game/level breaks. It attempts a video every completed level when inventory is ready. */
    fun showInterstitialAtNaturalBreak(onFinished:(()->Unit)?=null):Boolean{
        if(disposed||breakFinished!=null)return false;
        if(System.currentTimeMillis()-lastInterstitialAt<minInterstitialGapMs)return false
        val ad=interstitial?:return false
        if(!ad.isAdReady){if(!disposed)ad.loadAd();return false}
        breakFinished=onFinished;ad.showAd(activity);return true
    }
    fun dispose(){disposed=true;rewarded?.attempt=null;rewarded=null;breakFinished=null;if(active===this)active=null}
}

