package com.weearntogether.play

import android.app.Activity
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions

/** Private test-only integration. No monetary rewards, no automatic ad playback. */
class UnityAdsTestManager(private val activity: Activity, private val status: (String) -> Unit) {
    private var ready = false
    private var loadedRewarded = false
    private var loadedInterstitial = false

    fun initializeAfterConsent() {
        if (ready) { status("SDK gati"); return }
        UnityAds.initialize(activity, BuildConfig.UNITY_ANDROID_GAME_ID, true,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    ready = true
                    activity.runOnUiThread { status("Unity Ads TEST u inicializua") }
                }
                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                    activity.runOnUiThread { status("Gabim inicializimi: $error – $message") }
                }
            })
    }

    fun loadRewarded() = load(BuildConfig.UNITY_REWARDED_AD_UNIT, true)
    fun loadInterstitial() = load(BuildConfig.UNITY_INTERSTITIAL_AD_UNIT, false)

    private fun load(unit: String, rewarded: Boolean) {
        if (!ready) { status("Aktivizo fillimisht SDK-në"); return }
        UnityAds.load(unit, UnityAdsLoadOptions(), object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                if (rewarded) loadedRewarded = true else loadedInterstitial = true
                activity.runOnUiThread { status("Reklama TEST u ngarkua: $placementId") }
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                activity.runOnUiThread { status("Nuk u ngarkua $placementId: $error – $message") }
            }
        })
    }

    fun showRewarded() = show(BuildConfig.UNITY_REWARDED_AD_UNIT, true)
    fun showInterstitial() = show(BuildConfig.UNITY_INTERSTITIAL_AD_UNIT, false)

    private fun show(unit: String, rewarded: Boolean) {
        if (!(if (rewarded) loadedRewarded else loadedInterstitial)) {
            status("Së pari ngarko reklamën TEST")
            return
        }
        if (rewarded) loadedRewarded = false else loadedInterstitial = false
        UnityAds.show(activity, unit, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                activity.runOnUiThread { status("Shfaqja dështoi: $error – $message") }
            }
            override fun onUnityAdsShowStart(placementId: String) {
                activity.runOnUiThread { status("Reklama TEST filloi") }
            }
            override fun onUnityAdsShowClick(placementId: String) {}
            override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                activity.runOnUiThread { status("Reklama TEST përfundoi: $state. Pa shpërblim monetar.") }
            }
        })
    }
}
