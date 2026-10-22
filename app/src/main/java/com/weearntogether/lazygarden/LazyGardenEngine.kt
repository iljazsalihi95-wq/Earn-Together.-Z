package com.weearntogether.lazygarden

import kotlin.math.abs
import kotlin.random.Random

data class Cell(val row: Int, val col: Int)
data class MatchResult(
    val cells: Set<Cell>,
    val tiles: Map<GardenTile, Int>,
    val obstacles: Map<GardenObstacle, Int> = emptyMap()
)

class LazyGardenEngine(val level: LazyGardenLevel, private val random: Random = Random.Default) {
    val board: Array<Array<GardenTile>> = Array(level.rows) { Array(level.columns) { GardenTile.LEAF } }
    val obstacles: Array<Array<GardenObstacle?>> = Array(level.rows) { arrayOfNulls(level.columns) }
    var movesLeft: Int = level.moves
        private set

    init { fillPlayableBoard(); seedObstacles() }

    fun restoreMoves(savedMoves: Int) { movesLeft = savedMoves.coerceIn(0, level.moves) }
    fun obstacleAt(cell: Cell): GardenObstacle? = if (inside(cell)) obstacles[cell.row][cell.col] else null

    fun swap(a: Cell, b: Cell): MatchResult? {
        if (movesLeft <= 0 || !inside(a) || !inside(b) || abs(a.row-b.row)+abs(a.col-b.col) != 1) return null
        // Stone is a hard blocker: it cannot be swapped. Crates and ivy are cleared by matches beside them.
        if (obstacleAt(a) == GardenObstacle.STONE || obstacleAt(b) == GardenObstacle.STONE) return null
        swapTiles(a, b)
        var matches = findMatches()
        if (matches.cells.isEmpty()) { swapTiles(a, b); return null }
        movesLeft--
        val collected = mutableMapOf<GardenTile, Int>()
        val clearedObstacles = mutableMapOf<GardenObstacle, Int>()
        while (matches.cells.isNotEmpty()) {
            matches.tiles.forEach { (tile, count) -> collected[tile] = (collected[tile] ?: 0) + count }
            clearAdjacentObstacles(matches.cells).forEach { (type, count) -> clearedObstacles[type] = (clearedObstacles[type] ?: 0) + count }
            collapseAndRefill(matches.cells)
            matches = findMatches()
        }
        ensurePlayableBoard()
        return MatchResult(emptySet(), collected, clearedObstacles)
    }

    fun findMatches(): MatchResult {
        val cells = linkedSetOf<Cell>()
        for (r in board.indices) {
            var start = 0
            while (start < level.columns) {
                var end = start + 1
                while (end < level.columns && board[r][end] == board[r][start]) end++
                if (end-start >= 3) for (c in start until end) if (obstacles[r][c] != GardenObstacle.STONE) cells += Cell(r,c)
                start = end
            }
        }
        for (c in 0 until level.columns) {
            var start = 0
            while (start < level.rows) {
                var end = start + 1
                while (end < level.rows && board[end][c] == board[start][c]) end++
                if (end-start >= 3) for (r in start until end) if (obstacles[r][c] != GardenObstacle.STONE) cells += Cell(r,c)
                start = end
            }
        }
        return MatchResult(cells, cells.groupingBy { board[it.row][it.col] }.eachCount())
    }

    fun hasValidMove(): Boolean {
        for (r in 0 until level.rows) for (c in 0 until level.columns) {
            val here = Cell(r,c)
            if (obstacleAt(here) == GardenObstacle.STONE) continue
            if (c+1 < level.columns && obstacleAt(Cell(r,c+1)) != GardenObstacle.STONE && createsMatch(here,Cell(r,c+1))) return true
            if (r+1 < level.rows && obstacleAt(Cell(r+1,c)) != GardenObstacle.STONE && createsMatch(here,Cell(r+1,c))) return true
        }
        return false
    }

    private fun seedObstacles() {
        val candidates = mutableListOf<Cell>()
        for (r in 1 until level.rows-1) for (c in 1 until level.columns-1) candidates += Cell(r,c)
        candidates.shuffle(random)
        var cursor = 0
        level.obstacleGoals.forEach { goal ->
            repeat(goal.target.coerceAtMost(candidates.size-cursor)) {
                val cell = candidates[cursor++]
                obstacles[cell.row][cell.col] = goal.obstacle
            }
        }
        // Preserve a guaranteed playable area around the deterministic fallback pattern.
        for (r in 0..1) for (c in 0..2) obstacles[r][c] = null
        ensurePlayableBoard()
    }

    private fun clearAdjacentObstacles(matched: Set<Cell>): Map<GardenObstacle,Int> {
        val cleared = mutableMapOf<GardenObstacle,Int>()
        val targets = linkedSetOf<Cell>()
        matched.forEach { cell ->
            targets += cell
            targets += Cell(cell.row-1,cell.col); targets += Cell(cell.row+1,cell.col)
            targets += Cell(cell.row,cell.col-1); targets += Cell(cell.row,cell.col+1)
        }
        targets.filter(::inside).forEach { cell ->
            val obstacle = obstacles[cell.row][cell.col] ?: return@forEach
            obstacles[cell.row][cell.col] = null
            cleared[obstacle] = (cleared[obstacle] ?: 0) + 1
        }
        return cleared
    }

    private fun createsMatch(a: Cell,b: Cell): Boolean { swapTiles(a,b); val valid=findMatches().cells.isNotEmpty(); swapTiles(a,b); return valid }
    private fun swapTiles(a: Cell,b: Cell) { val first=board[a.row][a.col]; board[a.row][a.col]=board[b.row][b.col]; board[b.row][b.col]=first }

    private fun fillPlayableBoard() {
        var attempts=0
        do { fillBoardWithoutImmediateMatches(); attempts++ } while (!hasValidMove() && attempts < MAX_RESHUFFLE_ATTEMPTS)
        if (!hasValidMove()) seedGuaranteedMove()
    }

    private fun fillBoardWithoutImmediateMatches() {
        for (r in board.indices) for (c in board[r].indices) {
            do { board[r][c]=GardenTile.entries[random.nextInt(GardenTile.entries.size)] }
            while ((c>=2 && board[r][c]==board[r][c-1] && board[r][c]==board[r][c-2]) || (r>=2 && board[r][c]==board[r-1][c] && board[r][c]==board[r-2][c]))
        }
    }

    private fun ensurePlayableBoard() {
        if (hasValidMove()) return
        var attempts=0
        do { fillBoardWithoutImmediateMatches(); attempts++ } while (!hasValidMove() && attempts < MAX_RESHUFFLE_ATTEMPTS)
        if (!hasValidMove()) seedGuaranteedMove()
    }

    private fun seedGuaranteedMove() {
        if (level.rows<2 || level.columns<3) return
        board[0][0]=GardenTile.LEAF; board[0][1]=GardenTile.FLOWER; board[0][2]=GardenTile.LEAF
        board[1][0]=GardenTile.ROSE; board[1][1]=GardenTile.LEAF; board[1][2]=GardenTile.BERRY
    }

    private fun collapseAndRefill(matched:Set<Cell>) {
        for (c in 0 until level.columns) {
            val survivors=(level.rows-1 downTo 0).filter { Cell(it,c) !in matched }.map { board[it][c] }
            var index=0
            for (r in level.rows-1 downTo 0) board[r][c]=if(index<survivors.size) survivors[index++] else GardenTile.entries[random.nextInt(GardenTile.entries.size)]
        }
    }

    private fun inside(cell:Cell)=cell.row in 0 until level.rows && cell.col in 0 until level.columns
    companion object { private const val MAX_RESHUFFLE_ATTEMPTS=40 }
}
