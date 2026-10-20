package com.weearntogether.play

import android.app.Activity
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import com.unity3d.mediation.LevelPlayReward
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener
import com.unity3d.mediation.LevelPlayAdError

/**
 * Session-scoped LevelPlay controller.
 * Rewards are gameplay-only. This class never creates or approves cash balances.
 */
class LevelPlayManager(
    private val activity: Activity,
    private val userId: String,
    private val status: (String) -> Unit = {}
) {
    private var initialized = false
    private var rewarded: LevelPlayRewardedAd? = null
    private var interstitial: LevelPlayInterstitialAd? = null
    private var pendingGameplayReward: (() -> Unit)? = null

    fun initialize() {
        if (initialized) return
        val request = LevelPlayInitRequest.Builder(BuildConfig.LEVELPLAY_APP_KEY)
            .withUserId(userId)
            .build()
        LevelPlay.init(activity, request, object : LevelPlayInitListener {
            override fun onInitSuccess(configuration: LevelPlayConfiguration) {
                initialized = true
                createRewarded()
                createInterstitial()
                status("LevelPlay ready")
            }

            override fun onInitFailed(error: LevelPlayInitError) {
                status("LevelPlay unavailable")
            }
        })
    }

    private fun createRewarded() {
        rewarded = LevelPlayRewardedAd(BuildConfig.LEVELPLAY_REWARDED_AD_UNIT).also { ad ->
            ad.setListener(object : LevelPlayRewardedAdListener {
                override fun onAdLoaded(adInfo: LevelPlayAdInfo) { status("Rewarded ready") }
                override fun onAdLoadFailed(error: LevelPlayAdError) { status("Rewarded unavailable") }
                override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {}
                override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) {
                    pendingGameplayReward = null
                    ad.loadAd()
                }
                override fun onAdClicked(adInfo: LevelPlayAdInfo) {}
                override fun onAdClosed(adInfo: LevelPlayAdInfo) { ad.loadAd() }
                override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) {}
                override fun onAdRewarded(reward: LevelPlayReward, adInfo: LevelPlayAdInfo) {
                    pendingGameplayReward?.invoke()
                    pendingGameplayReward = null
                }
            })
            ad.loadAd()
        }
    }

    private fun createInterstitial() {
        interstitial = LevelPlayInterstitialAd(BuildConfig.LEVELPLAY_INTERSTITIAL_AD_UNIT).also { ad ->
            ad.setListener(object : LevelPlayInterstitialAdListener {
                override fun onAdLoaded(adInfo: LevelPlayAdInfo) { status("Interstitial ready") }
                override fun onAdLoadFailed(error: LevelPlayAdError) { status("Interstitial unavailable") }
                override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {}
                override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) { ad.loadAd() }
                override fun onAdClicked(adInfo: LevelPlayAdInfo) {}
                override fun onAdClosed(adInfo: LevelPlayAdInfo) { ad.loadAd() }
                override fun onAdInfoChanged(adInfo: LevelPlayAdInfo) {}
            })
            ad.loadAd()
        }
    }

    fun showRewardedForGameplay(onReward: () -> Unit): Boolean {
        val ad = rewarded ?: return false
        if (!ad.isAdReady) return false
        pendingGameplayReward = onReward
        ad.showAd(activity)
        return true
    }

    fun showInterstitialAtNaturalBreak(): Boolean {
        val ad = interstitial ?: return false
        if (!ad.isAdReady) return false
        ad.showAd(activity)
        return true
    }
}
