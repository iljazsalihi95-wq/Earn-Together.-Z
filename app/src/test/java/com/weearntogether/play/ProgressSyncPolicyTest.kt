package com.weearntogether.play

import org.junit.Assert.assertEquals
import org.junit.Test
import com.weearntogether.play.ProgressSyncPolicy.Decision.*

class ProgressSyncPolicyTest {
    @Test
    fun testFirstLoginWithTwoDifferentBoardsPreservesBoth() {
        assertEquals(CONFLICT, ProgressSyncPolicy.decide(true, "device-board", "cloud-board", null, "server-2", null))
    }

    @Test
    fun testAnotherDeviceCannotSilentlyOverwriteOfflineMoves() {
        assertEquals(CONFLICT, ProgressSyncPolicy.decide(true, "offline-moves", "other-device-moves", "shared-old-board", "server-3", "server-2"))
    }

    @Test
    fun testUnchangedDeviceCanRestoreNewCloudSave() {
        assertEquals(DOWNLOAD, ProgressSyncPolicy.decide(true, "shared-old-board", "other-device-moves", "shared-old-board", "server-3", "server-2"))
    }

    @Test
    fun testOfflineMovesUploadOnlyWhenCloudHasNotChanged() {
        assertEquals(UPLOAD, ProgressSyncPolicy.decide(true, "offline-moves", "shared-old-board", "shared-old-board", "server-2", "server-2"))
    }

    @Test
    fun testNewDeviceRestoresCloudInsteadOfUploadingDefaults() {
        assertEquals(DOWNLOAD, ProgressSyncPolicy.decide(false, "empty", "cloud-board", null, "server-2", null))
    }

    @Test
    fun testIdenticalBoardsNeedNoWriteDespiteNewVersion() {
        assertEquals(SAME, ProgressSyncPolicy.decide(true, "same", "same", "old", "server-3", "server-2"))
    }

    @Test
    fun testMissingBaseNeverTreatsMatchingNullVersionsAsSafeUpload() {
        assertEquals(CONFLICT, ProgressSyncPolicy.decide(true, "offline-moves", "cloud-board", null, "server-2", null))
    }
}
