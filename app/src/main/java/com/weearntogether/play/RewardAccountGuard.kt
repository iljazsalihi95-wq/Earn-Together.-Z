package com.weearntogether.play

import android.content.Context
import java.util.UUID

/**
 * Local client-side foundation for reward-account integrity.
 * This is NOT sufficient for real-money enforcement: the backend must enforce
 * unique installation/account/payment identities and provider postbacks.
 */
class RewardAccountGuard(context: Context) {
    private val prefs = context.getSharedPreferences("wet_reward_identity", Context.MODE_PRIVATE)

    val installationId: String
        get() {
            val existing = prefs.getString("installation_id", null)
            if (!existing.isNullOrBlank()) return existing
            val created = UUID.randomUUID().toString()
            prefs.edit().putString("installation_id", created).apply()
            return created
        }

    val payoutEmail: String? get() = prefs.getString("payout_email", null)
    val payoutProfileComplete: Boolean get() = prefs.getBoolean("payout_profile_complete", false)

    fun savePayoutProfile(email: String, fullName: String, address: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) return false
        if (fullName.trim().length < 3 || address.trim().length < 5) return false
        prefs.edit()
            .putString("payout_email", normalizedEmail)
            .putString("payout_name", fullName.trim())
            .putString("payout_address", address.trim())
            .putBoolean("payout_profile_complete", true)
            .apply()
        return true
    }

    /** Real payout eligibility remains false until an approved backend/provider enables it. */
    fun realPayoutEnabled(): Boolean = false
}
