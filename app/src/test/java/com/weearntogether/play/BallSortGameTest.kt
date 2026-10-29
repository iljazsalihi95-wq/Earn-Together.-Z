package com.weearntogether.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BallSortGameTest {
    @Test fun groupedPourMovesWholeExposedRun() {
        val game = BallSortGame(1, listOf(listOf(1, 2, 2), listOf(2), emptyList(), emptyList(), emptyList()))
        assertTrue(game.move(0, 1))
        assertEquals(listOf(1), game.tubes[0])
        assertEquals(listOf(2, 2, 2), game.tubes[1])
        assertEquals(2, game.lastPourCount)
        assertEquals(1, game.moves)
    }

    @Test fun groupedPourStopsAtTubeCapacity() {
        val game = BallSortGame(1, listOf(listOf(1, 1, 1), listOf(1, 1), emptyList(), emptyList(), emptyList()))
        assertTrue(game.move(0, 1))
        assertEquals(listOf(1), game.tubes[0])
        assertEquals(4, game.tubes[1].size)
        assertEquals(2, game.lastPourCount)
    }

    @Test fun incompatibleColourCannotReceivePour() {
        val game = BallSortGame(1, listOf(listOf(1, 1), listOf(2), emptyList(), emptyList(), emptyList()))
        assertFalse(game.move(0, 1))
        assertEquals(listOf(1, 1), game.tubes[0])
        assertEquals(listOf(2), game.tubes[1])
    }

    @Test fun undoRestoresWholeGroupedPour() {
        val before = listOf(listOf(1, 2, 2), listOf(2), emptyList(), emptyList(), emptyList())
        val game = BallSortGame(1, before)
        assertTrue(game.move(0, 1))
        assertTrue(game.undo())
        assertEquals(before, game.tubes.map { it.toList() })
        assertEquals(0, game.moves)
        assertEquals(0, game.lastPourCount)
    }

    @Test fun difficultyScalesBeyondOldEightColourCeiling() {
        assertEquals(3, BallSortGame(1).colors)
        assertEquals(9, BallSortGame(19).colors)
        assertEquals(9, BallSortGame(100).colors)
    }
}
