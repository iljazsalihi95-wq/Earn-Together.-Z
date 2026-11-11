package com.weearntogether.play

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Verifies the agreed question clock and manual explanation/Next boundary. */
class ArenaTimingContractTest {
    @Test fun narrationPrecedesSixtySecondsAndAnswerReplayNeverAddsPoints() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val user="arena-sixty-second-contract"
        val prefs=context.getSharedPreferences("arena_offline_v1_$user",0)
        prefs.edit().clear().commit()
        try {
            val store=ArenaOfflineStore(context,user)
            val round=store.start("Teknologji")
            assertEquals(60,round.getJSONObject("config").getInt("seconds"))
            assertEquals(60000L,round.getLong("remaining_ms"))
            assertTrue(round.getBoolean("presentation_pending"))
            assertTrue(round.getInt("total")>=10)
            round.put("presentation_pending",false)
            val question=round.getJSONObject("question")
            store.answer(round,question.getInt("correct"))
            val score=round.getInt("score")
            assertTrue(score>0)
            assertEquals("The explanation remains on the same question until Next",0,round.getInt("index"))
            assertTrue(round.getJSONObject("feedback").getString("explanation").isNotBlank())
            store.answer(round,question.getInt("correct"))
            assertEquals(score,round.getInt("score"))
            assertEquals(1,round.getInt("correct"))
            store.next(round)
            assertEquals(1,round.getInt("index"))
            assertTrue(round.getBoolean("presentation_pending"))
            assertFalse(round.has("feedback"))
        } finally { prefs.edit().clear().commit() }
    }
}
