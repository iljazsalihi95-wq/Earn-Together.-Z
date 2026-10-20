package com.weearntogether.play

/**
 * Financial domain model for the future verified-revenue backend.
 * IMPORTANT: the Android client is never the authority for real money.
 * Amounts use integer euro-cents; no floating point money calculations.
 */
enum class RevenueStatus {
    PENDING,
    VERIFIED,
    REVERSED,
    PAYABLE,
    PAYOUT_PROCESSING,
    PAID,
    PAYOUT_FAILED
}

data class RevenueSplitSnapshot(
    val playerBasisPoints: Int = 7000,
    val platformBasisPoints: Int = 3000
) {
    init {
        require(playerBasisPoints >= 0 && platformBasisPoints >= 0)
        require(playerBasisPoints + platformBasisPoints == 10_000)
    }

    fun playerShare(cents: Long): Long = cents * playerBasisPoints / 10_000L
    fun platformShare(cents: Long): Long = cents - playerShare(cents)
}

data class RevenueEvent(
    val eventId: String,
    val partner: String,
    val partnerConversionId: String,
    val userId: String,
    val verifiedRevenueCents: Long,
    val status: RevenueStatus,
    val split: RevenueSplitSnapshot,
    val playerShareCents: Long,
    val platformShareCents: Long,
    val createdAtEpochMs: Long,
    val verifiedAtEpochMs: Long? = null,
    val reversedAtEpochMs: Long? = null
) {
    init {
        require(eventId.isNotBlank())
        require(partnerConversionId.isNotBlank())
        require(userId.isNotBlank())
        require(verifiedRevenueCents >= 0)
        require(playerShareCents >= 0 && platformShareCents >= 0)
        require(playerShareCents + platformShareCents == verifiedRevenueCents)
    }

    companion object {
        fun verified(
            eventId: String,
            partner: String,
            partnerConversionId: String,
            userId: String,
            verifiedRevenueCents: Long,
            split: RevenueSplitSnapshot,
            timestamp: Long
        ): RevenueEvent {
            val player = split.playerShare(verifiedRevenueCents)
            return RevenueEvent(
                eventId = eventId,
                partner = partner,
                partnerConversionId = partnerConversionId,
                userId = userId,
                verifiedRevenueCents = verifiedRevenueCents,
                status = RevenueStatus.VERIFIED,
                split = split,
                playerShareCents = player,
                platformShareCents = verifiedRevenueCents - player,
                createdAtEpochMs = timestamp,
                verifiedAtEpochMs = timestamp
            )
        }
    }
}

data class WalletSnapshot(
    val pendingCents: Long,
    val verifiedCents: Long,
    val payableCents: Long,
    val payoutProcessingCents: Long,
    val paidCents: Long,
    val currency: String = "EUR"
)

data class PayoutPolicy(
    val minimumPayoutCents: Long,
    val automaticPayoutEnabled: Boolean
) {
    init { require(minimumPayoutCents >= 0) }
}
