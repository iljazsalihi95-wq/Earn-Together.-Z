package com.weearntogether.play

/**
 * Read-only presentation state for the future server-backed wallet.
 * The client must never manufacture, increment or approve real-money balances.
 */
data class WalletPresentation(
    val connectedToBackend: Boolean = false,
    val currency: String = "EUR",
    val pendingCents: Long = 0,
    val verifiedCents: Long = 0,
    val payableCents: Long = 0,
    val payoutProcessingCents: Long = 0,
    val paidCents: Long = 0,
    val payoutProfileReady: Boolean = false,
    val partnerRevenueEnabled: Boolean = false,
    val lastServerSyncEpochMs: Long? = null
) {
    val canRequestRealPayout: Boolean
        get() = connectedToBackend && partnerRevenueEnabled && payoutProfileReady && payableCents > 0

    fun statusLabel(): String = when {
        !connectedToBackend -> "Wallet real ende nuk është lidhur me backend."
        !partnerRevenueEnabled -> "Revenue reale është e çaktivizuar deri në miratimin e partnerit."
        !payoutProfileReady -> "Duhet përfunduar profili i pagesës."
        payableCents <= 0 -> "Nuk ka ende shumë të verifikuar për pagesë."
        else -> "Ka shumë të verifikuar për pagesë."
    }
}

object MoneyText {
    fun euro(cents: Long): String {
        val safe = cents.coerceAtLeast(0)
        return "%d.%02d €".format(safe / 100, safe % 100)
    }
}
