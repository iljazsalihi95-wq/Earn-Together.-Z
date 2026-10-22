package com.weearntogether.play

/**
 * Resolves device/cloud progress without silently destroying either copy.
 *
 * A local save that has never shared a base with the cloud is a real conflict:
 * we cannot prove which side is newer. A brand-new device still downloads the
 * cloud save, while an unchanged device can safely restore a newer cloud copy.
 */
object ProgressSyncPolicy {
    enum class Decision { SAME, DOWNLOAD, UPLOAD, CONFLICT }

    fun decide(
        hasLocal: Boolean,
        local: String,
        cloud: String,
        base: String?,
        cloudVersion: String,
        baseVersion: String?
    ): Decision = when {
        local == cloud -> Decision.SAME
        !hasLocal -> Decision.DOWNLOAD
        base != null && local == base -> Decision.DOWNLOAD
        base != null && cloudVersion == baseVersion -> Decision.UPLOAD
        else -> Decision.CONFLICT
    }
}
