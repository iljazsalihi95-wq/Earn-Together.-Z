package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ayet.sdk.AyetSdk
import org.json.JSONObject

/** Native ayeT SDK entry point. Client callbacks never grant coins or cash. */
class AyetOfferwallActivity : Activity() {
    private val navy = 0xff080e22.toInt()
    private val gold = 0xffffce70.toInt()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var accounts: AccountStore
    private lateinit var status: TextView
    private lateinit var offers: Button
    private lateinit var rewardStatus: Button
    private var generation = 0
    private var waiting = false
    private var timeout: Runnable? = null

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accounts = AccountStore(this)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(30))
            setBackgroundColor(navy)
        }
        fun label(value: String, size: Float = 15f, color: Int = Color.WHITE) {
            root.addView(TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                setPadding(0, dp(10), 0, dp(10))
                if (size >= 24f) setTypeface(null, Typeface.BOLD)
            })
        }
        fun button(value: String, action: () -> Unit): Button {
            val result = Button(this).apply {
                text = value
                isAllCaps = false
                setTextColor(navy)
                setTypeface(null, Typeface.BOLD)
                background = GradientDrawable().apply {
                    setColor(gold)
                    cornerRadius = dp(18).toFloat()
                }
                setOnClickListener { action() }
            }
            root.addView(result, LinearLayout.LayoutParams(-1, dp(56)).apply {
                topMargin = dp(14)
            })
            return result
        }
        label("OFFERS & REWARDS", 28f, gold)
        label("ayeT Offerwall", 20f)
        label("Discover partner offers and check their progress. Offer availability depends on ayeT approval and your region.")
        label("Rewards appear only after verified server callbacks and processing by our backend. Returning from an offer does not confirm completion or add money.")
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(0xffbecce8.toInt())
            setPadding(0, dp(16), 0, dp(16))
        }
        root.addView(status)
        offers = button("OPEN OFFERWALL") { openVerified(false) }
        rewardStatus = button("OFFER PROGRESS & SUPPORT") { openVerified(true) }
        button("MY WALLET") {
            startActivity(Intent(this, WalletActivity::class.java))
        }
        button("BACK TO APP") { finish() }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        scroll.setOnApplyWindowInsetsListener { _, insets ->
            root.setPadding(dp(22) + insets.systemWindowInsetLeft,
                dp(24) + insets.systemWindowInsetTop,
                dp(22) + insets.systemWindowInsetRight,
                dp(30) + insets.systemWindowInsetBottom)
            insets
        }
        setContentView(scroll)
        scroll.requestApplyInsets()
        status.text = "Sign in is checked securely before opening ayeT. Your password and session token are never sent to ayeT."
        if (!accounts.isSignedIn()) {
            offers.isEnabled = false
            rewardStatus.isEnabled = false
            status.text = "Please return to the app and sign in first."
        }
    }

    private fun setBusy(value: Boolean) {
        waiting = value
        offers.isEnabled = !value
        rewardStatus.isEnabled = !value
    }

    private fun openVerified(showProgress: Boolean) {
        if (waiting || isFinishing || isDestroyed) return
        if (!accounts.isSignedIn()) {
            status.text = "Your session expired. Return to the app and sign in again."
            return
        }
        val localUser = accounts.currentUserId()
        val sessionToken = try { accounts.requireToken() } catch (_: Exception) {
            status.text = "Please sign in again."
            return
        }
        val requestId = ++generation
        setBusy(true)
        status.text = "Verifying your player account…"
        timeout = Runnable {
            if (requestId == generation && waiting && !isFinishing && !isDestroyed) {
                generation++
                setBusy(false)
                status.text = "Account verification timed out. Try again."
            }
        }.also { handler.postDelayed(it, 70_000L) }
        AuthWorker.executor.execute {
            val result = runCatching {
                BackendClient().request("validateSession",
                    JSONObject().put("session_token", sessionToken))
            }
            runOnUiThread {
                if (requestId != generation || !waiting || isFinishing || isDestroyed)
                    return@runOnUiThread
                timeout?.let { handler.removeCallbacks(it) }
                timeout = null
                setBusy(false)
                result.onSuccess { response ->
                    val verifiedUser = response.optString("user_id")
                    // Refuse stale requests after logout/account change or a mismatched server identity.
                    if (!accounts.isSignedIn() ||
                        accounts.currentUserId() != localUser ||
                        runCatching { accounts.requireToken() }.getOrNull() != sessionToken ||
                        response.optString("status") != "ACTIVE" ||
                        verifiedUser != localUser ||
                        !Regex("^[A-Za-z0-9_-]{3,63}$").matches(verifiedUser)) {
                        status.text = "Your account changed or expired. Return to the app and sign in again."
                        return@onSuccess
                    }
                    try {
                        // Only the backend-confirmed immutable user ID is shared.
                        // Reinitialize for every verified opening so another account cannot reuse it.
                        AyetSdk.init(this, AyetOfferwallConfig.PLACEMENT_ID, verifiedUser)
                        if (showProgress) {
                            AyetSdk.showRewardStatus(this)
                        } else {
                            AyetSdk.showOfferwall(this, AyetOfferwallConfig.ADSLOT_NAME)
                        }
                        status.text = "After returning, open My Wallet to refresh confirmed balances. Completing an offer does not guarantee an immediate reward."
                    } catch (_: Exception) {
                        status.text = "ayeT could not open. Your placement may still be under review, or the service may be unavailable. Try again later."
                    }
                }.onFailure {
                    status.text = it.message ?: "Could not verify your account. Try again."
                }
            }
        }
    }

    override fun onDestroy() {
        generation++
        timeout?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}

internal object AyetOfferwallConfig {
    const val PLACEMENT_ID = 25740
    const val ADSLOT_ID = 30651
    const val ADSLOT_NAME = "We Earn Together Offerwall"
    const val CURRENCY_IDENTIFIER = "WETCOIN"
}
