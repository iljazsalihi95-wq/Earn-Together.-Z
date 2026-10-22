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
        val first = board[a.row][a.col]
        board[a.row][a.col] = board[b.row][b.col]
        board[b.row][b.col] = first
        var matches = findMatches()
        if (matches.cells.isEmpty()) {
            val back = board[a.row][a.col]
            board[a.row][a.col] = board[b.row][b.col]
            board[b.row][b.col] = back
            return null
        }
        movesLeft--
        val collected = mutableMapOf<GardenTile, Int>()
        while (matches.cells.isNotEmpty()) {
            matches.tiles.forEach { (tile, count) -> collected[tile] = (collected[tile] ?: 0) + count }
            collapseAndRefill(matches.cells)
            matches = findMatches()
        }
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

    private fun fillPlayableBoard() {
        for (r in board.indices) for (c in board[r].indices) {
            do { board[r][c] = GardenTile.entries[random.nextInt(GardenTile.entries.size)] }
            while ((c >= 2 && board[r][c] == board[r][c-1] && board[r][c] == board[r][c-2]) ||
                   (r >= 2 && board[r][c] == board[r-1][c] && board[r][c] == board[r-2][c]))
        }
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
}
