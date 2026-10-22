package com.weearntogether.lazygarden

import kotlin.math.abs
import kotlin.random.Random

data class Cell(val row: Int, val col: Int)
data class MatchResult(val cells: Set<Cell>, val tiles: Map<GardenTile, Int>)

class LazyGardenEngine(
    val level: LazyGardenLevel,
    private val random: Random = Random.Default
) {
    val board: Array<Array<GardenTile>> = Array(level.rows) { Array(level.columns) { GardenTile.LEAF } }
    var movesLeft: Int = level.moves
        private set

    init { fillPlayableBoard() }

    /** Restores the move budget for a saved run without allowing values outside this level. */
    fun restoreMoves(savedMoves: Int) {
        movesLeft = savedMoves.coerceIn(0, level.moves)
    }

    fun swap(a: Cell, b: Cell): MatchResult? {
        if (movesLeft <= 0 || !inside(a) || !inside(b) || abs(a.row-b.row)+abs(a.col-b.col) != 1) return null
        swapTiles(a, b)
        var matches = findMatches()
        if (matches.cells.isEmpty()) {
            swapTiles(a, b)
            return null
        }
        movesLeft--
        val collected = mutableMapOf<GardenTile, Int>()
        while (matches.cells.isNotEmpty()) {
            matches.tiles.forEach { (tile, count) -> collected[tile] = (collected[tile] ?: 0) + count }
            collapseAndRefill(matches.cells)
            matches = findMatches()
        }
        ensurePlayableBoard()
        return MatchResult(emptySet(), collected)
    }

    fun findMatches(): MatchResult {
        val cells = linkedSetOf<Cell>()
        for (r in board.indices) {
            var start = 0
            while (start < level.columns) {
                var end = start + 1
                while (end < level.columns && board[r][end] == board[r][start]) end++
                if (end-start >= 3) for (c in start until end) cells += Cell(r,c)
                start = end
            }
        }
        for (c in 0 until level.columns) {
            var start = 0
            while (start < level.rows) {
                var end = start + 1
                while (end < level.rows && board[end][c] == board[start][c]) end++
                if (end-start >= 3) for (r in start until end) cells += Cell(r,c)
                start = end
            }
        }
        return MatchResult(cells, cells.groupingBy { board[it.row][it.col] }.eachCount())
    }

    /** True when at least one adjacent swap can create a match. */
    fun hasValidMove(): Boolean {
        for (r in 0 until level.rows) {
            for (c in 0 until level.columns) {
                val here = Cell(r, c)
                if (c + 1 < level.columns && createsMatch(here, Cell(r, c + 1))) return true
                if (r + 1 < level.rows && createsMatch(here, Cell(r + 1, c))) return true
            }
        }
        return false
    }

    private fun createsMatch(a: Cell, b: Cell): Boolean {
        swapTiles(a, b)
        val valid = findMatches().cells.isNotEmpty()
        swapTiles(a, b)
        return valid
    }

    private fun swapTiles(a: Cell, b: Cell) {
        val first = board[a.row][a.col]
        board[a.row][a.col] = board[b.row][b.col]
        board[b.row][b.col] = first
    }

    private fun fillPlayableBoard() {
        var attempts = 0
        do {
            fillBoardWithoutImmediateMatches()
            attempts++
        } while (!hasValidMove() && attempts < MAX_RESHUFFLE_ATTEMPTS)

        // The fallback is extremely unlikely, but guarantees a playable board instead of trapping the player.
        if (!hasValidMove()) seedGuaranteedMove()
    }

    private fun fillBoardWithoutImmediateMatches() {
        for (r in board.indices) for (c in board[r].indices) {
            do { board[r][c] = GardenTile.entries[random.nextInt(GardenTile.entries.size)] }
            while ((c >= 2 && board[r][c] == board[r][c-1] && board[r][c] == board[r][c-2]) ||
                   (r >= 2 && board[r][c] == board[r-1][c] && board[r][c] == board[r-2][c]))
        }
    }

    private fun ensurePlayableBoard() {
        if (hasValidMove()) return
        var attempts = 0
        do {
            fillBoardWithoutImmediateMatches()
            attempts++
        } while (!hasValidMove() && attempts < MAX_RESHUFFLE_ATTEMPTS)
        if (!hasValidMove()) seedGuaranteedMove()
    }

    /**
     * Creates an original, deterministic swap opportunity in the top-left 3x2 area:
     * A B A
     * C A D
     * Swapping B with the A below it produces A A A.
     */
    private fun seedGuaranteedMove() {
        if (level.rows < 2 || level.columns < 3) return
        val a = GardenTile.LEAF
        val b = GardenTile.FLOWER
        val c = GardenTile.ROSE
        val d = GardenTile.BERRY
        board[0][0] = a
        board[0][1] = b
        board[0][2] = a
        board[1][0] = c
        board[1][1] = a
        board[1][2] = d
    }

    private fun collapseAndRefill(matched: Set<Cell>) {
        for (c in 0 until level.columns) {
            val survivors = (level.rows-1 downTo 0).filter { Cell(it,c) !in matched }.map { board[it][c] }
            var index = 0
            for (r in level.rows-1 downTo 0) {
                board[r][c] = if (index < survivors.size) survivors[index++] else GardenTile.entries[random.nextInt(GardenTile.entries.size)]
            }
        }
    }

    private fun inside(cell: Cell) = cell.row in 0 until level.rows && cell.col in 0 until level.columns

    companion object {
        private const val MAX_RESHUFFLE_ATTEMPTS = 40
    }
}
