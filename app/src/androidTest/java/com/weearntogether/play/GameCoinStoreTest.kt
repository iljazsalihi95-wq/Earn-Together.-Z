package com.weearntogether.play

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class GameCoinStoreTest {
    @Test fun pilotRatesKeepAccountsSeparateAndDoNotAwardReplayedLevels() {
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val session=ctx.getSharedPreferences("wet_cloud_session",Context.MODE_PRIVATE)
        val previousUser=session.getString("user_id",null)
        val coins=ctx.getSharedPreferences("wet_gamecoins_pilot_v1",Context.MODE_PRIVATE)
        val user="qa-coins-${java.util.UUID.randomUUID()}"
        try {
            session.edit().putString("user_id",user).commit()
            val first=GameCoinStore(ctx)
            assertEquals(0L,first.balance())
            assertTrue(first.awardCompletedLevel("block_puzzle_hd2d",1))
            assertEquals(20L,GameCoinStore(ctx).balance())
            assertFalse(GameCoinStore(ctx).awardCompletedLevel("block_puzzle_hd2d",1))
            assertTrue(first.awardCompletedLevel("bubble_shooter_hd2d",1))
            assertTrue(first.awardCompletedVideo())
            assertEquals(140L,first.balance())
            assertFalse(first.spend(141L))
            assertFalse(first.spend(-1L))
            assertTrue(first.spend(20L))
            assertEquals(120L,GameCoinStore(ctx).balance())
            val successes=java.util.concurrent.atomic.AtomicInteger()
            val threads=List(8){Thread{if(GameCoinStore(ctx).awardCompletedLevel("block_puzzle_hd2d",2))successes.incrementAndGet()}}
            threads.forEach{it.start()};threads.forEach{it.join()}
            assertEquals(1,successes.get())
            assertEquals(140L,first.balance())
            session.edit().putString("user_id","$user-other").commit()
            assertEquals(0L,GameCoinStore(ctx).balance())
            session.edit().remove("user_id").commit()
            assertFalse(first.awardCompletedVideo())
            assertFalse(first.awardCompletedLevel("block_puzzle_hd2d",3))
        } finally {
            session.edit().apply{if(previousUser==null)remove("user_id")else putString("user_id",previousUser)}.commit()
            val edit=coins.edit()
            coins.all.keys.filter{it.contains(user)}.forEach{edit.remove(it)}
            edit.commit()
        }
    }
}
