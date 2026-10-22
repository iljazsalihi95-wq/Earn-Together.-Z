package com.weearntogether.play

/** A remote save is known to exist. Lack of a shared base always preserves both copies. */
object ProgressSyncPolicy {
    enum class Decision { SAME, DOWNLOAD, UPLOAD, CONFLICT }
    fun decide(hasLocal: Boolean, local: String, cloud: String, base: String?, cloudVersion: String, baseVersion: String?): Decision = when {
        local == cloud -> Decision.SAME
        !hasLocal || local == base -> Decision.DOWNLOAD
        cloudVersion == baseVersion -> Decision.UPLOAD
        else -> Decision.CONFLICT
    }
}
