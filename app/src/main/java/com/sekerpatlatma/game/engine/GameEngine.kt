package com.sekerpatlatma.game.engine

import com.sekerpatlatma.game.model.*
import kotlin.math.abs

class GameEngine(
    private val boardSize: Int = 8,
    private val onScoreChanged: (Int) -> Unit = {},
    private val onMovesChanged: (Int) -> Unit = {},
    private val onBoardChanged: () -> Unit = {},
    private val onMatchFound: (List<Pair<Int, Int>>) -> Unit = {},
    private val onCombo: (Int) -> Unit = {},
    private val onGameWon: (Int) -> Unit = {},
    private val onGameLost: () -> Unit = {},
    private val onNoValidMoves: () -> Unit = {}
) {
    private var board: Array<Array<Candy?>> = Array(boardSize) { arrayOfNulls(boardSize) }
    private var gameState = GameState()
    private var currentLevel: Level? = null

    val currentBoard: Array<Array<Candy?>> get() = board
    val state: GameState get() = gameState

    fun startLevel(level: Level) {
        currentLevel = level
        gameState = GameState(
            currentLevel = level.number,
            movesLeft = level.moves,
            targetScore = level.targetScore
        )
        createBoard()
        onScoreChanged(0)
        onMovesChanged(level.moves)
    }

    private fun createBoard() {
        board = Array(boardSize) { row ->
            Array(boardSize) { col ->
                Candy(CandyType.random(), row, col)
            }
        }
        
        // Ensure no initial matches
        var attempts = 0
        while (findAllMatches().isNotEmpty() && attempts < 100) {
            attempts++
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    if (findMatchesAt(row, col).size >= 3) {
                        board[row][col]?.type = CandyType.random()
                    }
                }
            }
        }
        onBoardChanged()
    }

    fun selectCandy(row: Int, col: Int): Boolean {
        if (gameState.isAnimating) return false
        
        val candy = board[row][col] ?: return false
        
        val selected = gameState.selectedCandy
        if (selected == null) {
            // First selection
            gameState.selectedCandy = Pair(row, col)
            candy.isSelected = true
            onBoardChanged()
            return true
        } else {
            // Second selection
            val (firstRow, firstCol) = selected
            board[firstRow][firstCol]?.isSelected = false
            
            if (isAdjacent(firstRow, firstCol, row, col)) {
                // Try swap
                return trySwap(firstRow, firstCol, row, col)
            } else {
                // New selection
                gameState.selectedCandy = Pair(row, col)
                candy.isSelected = true
                onBoardChanged()
                return true
            }
        }
    }

    private fun isAdjacent(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        return abs(row1 - row2) + abs(col1 - col2) == 1
    }

    private fun trySwap(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        gameState.selectedCandy = null
        gameState.isAnimating = true
        
        // Perform swap
        swapCandies(row1, col1, row2, col2)
        
        val matches = findAllMatches()
        if (matches.isNotEmpty()) {
            // Valid move
            gameState.movesLeft--
            onMovesChanged(gameState.movesLeft)
            gameState.comboCount = 0
            processMatches(matches)
            return true
        } else {
            // Invalid move - swap back
            swapCandies(row1, col1, row2, col2)
            gameState.isAnimating = false
            onBoardChanged()
            return false
        }
    }

    private fun swapCandies(row1: Int, col1: Int, row2: Int, col2: Int) {
        val temp = board[row1][col1]
        board[row1][col1] = board[row2][col2]
        board[row2][col2] = temp
        
        board[row1][col1]?.let {
            it.row = row1
            it.col = col1
        }
        board[row2][col2]?.let {
            it.row = row2
            it.col = col2
        }
    }

    fun findAllMatches(): Set<Pair<Int, Int>> {
        val matches = mutableSetOf<Pair<Int, Int>>()
        
        // Horizontal matches
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize - 2) {
                val match = findHorizontalMatch(row, col)
                matches.addAll(match)
            }
        }
        
        // Vertical matches
        for (row in 0 until boardSize - 2) {
            for (col in 0 until boardSize) {
                val match = findVerticalMatch(row, col)
                matches.addAll(match)
            }
        }
        
        return matches
    }

    private fun findHorizontalMatch(row: Int, startCol: Int): List<Pair<Int, Int>> {
        val candy = board[row][startCol] ?: return emptyList()
        val type = candy.type
        val match = mutableListOf(Pair(row, startCol))
        
        for (col in startCol + 1 until boardSize) {
            if (board[row][col]?.type == type) {
                match.add(Pair(row, col))
            } else {
                break
            }
        }
        
        return if (match.size >= 3) match else emptyList()
    }

    private fun findVerticalMatch(startRow: Int, col: Int): List<Pair<Int, Int>> {
        val candy = board[startRow][col] ?: return emptyList()
        val type = candy.type
        val match = mutableListOf(Pair(startRow, col))
        
        for (row in startRow + 1 until boardSize) {
            if (board[row][col]?.type == type) {
                match.add(Pair(row, col))
            } else {
                break
            }
        }
        
        return if (match.size >= 3) match else emptyList()
    }

    private fun findMatchesAt(row: Int, col: Int): Set<Pair<Int, Int>> {
        val matches = mutableSetOf<Pair<Int, Int>>()
        val candy = board[row][col] ?: return matches
        val type = candy.type
        
        // Horizontal
        var left = col
        while (left > 0 && board[row][left - 1]?.type == type) left--
        var right = col
        while (right < boardSize - 1 && board[row][right + 1]?.type == type) right++
        
        if (right - left >= 2) {
            for (c in left..right) matches.add(Pair(row, c))
        }
        
        // Vertical
        var top = row
        while (top > 0 && board[top - 1][col]?.type == type) top--
        var bottom = row
        while (bottom < boardSize - 1 && board[bottom + 1][col]?.type == type) bottom++
        
        if (bottom - top >= 2) {
            for (r in top..bottom) matches.add(Pair(r, col))
        }
        
        return matches
    }

    private fun processMatches(matches: Set<Pair<Int, Int>>) {
        gameState.comboCount++
        
        if (gameState.comboCount > 1) {
            onCombo(gameState.comboCount)
        }
        
        // Calculate score
        val baseScore = 10
        var score = matches.size * baseScore
        
        // Match bonus
        when {
            matches.size >= 6 -> score += 200
            matches.size >= 5 -> score += 100
            matches.size >= 4 -> score += 50
        }
        
        // Combo multiplier
        val comboMultiplier = 1.5
        var multipliedScore = score.toDouble()
        repeat(gameState.comboCount - 1) {
            multipliedScore *= comboMultiplier
        }
        score = multipliedScore.toInt()
        
        gameState.score += score
        onScoreChanged(gameState.score)
        
        // Update stars
        currentLevel?.let { level ->
            val newStars = level.starThresholds.count { gameState.score >= it }
            if (newStars > gameState.stars) {
                gameState.stars = newStars
            }
        }
        
        // Mark matched candies
        matches.forEach { (row, col) ->
            board[row][col]?.isMatched = true
        }
        onMatchFound(matches.toList())
        
        // Remove matches and refill (will be called with animation delay)
    }

    fun removeMatchesAndRefill(): Boolean {
        // Remove matched candies
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col]?.isMatched == true) {
                    board[row][col] = null
                }
            }
        }
        
        // Drop candies
        dropCandies()
        
        // Fill empty spaces
        fillEmptySpaces()
        
        onBoardChanged()
        
        // Check for new matches
        val newMatches = findAllMatches()
        if (newMatches.isNotEmpty()) {
            processMatches(newMatches)
            return true
        }
        
        // Check for valid moves
        if (!hasValidMoves()) {
            onNoValidMoves()
            shuffleBoard()
        }
        
        checkGameStatus()
        gameState.isAnimating = false
        return false
    }

    private fun dropCandies() {
        for (col in 0 until boardSize) {
            var emptySpaces = 0
            for (row in boardSize - 1 downTo 0) {
                if (board[row][col] == null) {
                    emptySpaces++
                } else if (emptySpaces > 0) {
                    val candy = board[row][col]!!
                    val newRow = row + emptySpaces
                    board[newRow][col] = candy
                    board[row][col] = null
                    candy.row = newRow
                }
            }
        }
    }

    private fun fillEmptySpaces() {
        for (col in 0 until boardSize) {
            for (row in 0 until boardSize) {
                if (board[row][col] == null) {
                    board[row][col] = Candy(CandyType.random(), row, col)
                }
            }
        }
    }

    fun hasValidMoves(): Boolean {
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Check right swap
                if (col < boardSize - 1) {
                    swapCandies(row, col, row, col + 1)
                    if (findAllMatches().isNotEmpty()) {
                        swapCandies(row, col, row, col + 1)
                        return true
                    }
                    swapCandies(row, col, row, col + 1)
                }
                
                // Check down swap
                if (row < boardSize - 1) {
                    swapCandies(row, col, row + 1, col)
                    if (findAllMatches().isNotEmpty()) {
                        swapCandies(row, col, row + 1, col)
                        return true
                    }
                    swapCandies(row, col, row + 1, col)
                }
            }
        }
        return false
    }

    fun shuffleBoard() {
        val types = mutableListOf<CandyType>()
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                board[row][col]?.let { types.add(it.type) }
            }
        }
        types.shuffle()
        
        var index = 0
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                board[row][col]?.type = types[index++]
            }
        }
        
        // Ensure no matches and valid moves exist
        while (findAllMatches().isNotEmpty() || !hasValidMoves()) {
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    board[row][col]?.type = CandyType.random()
                }
            }
        }
        
        onBoardChanged()
    }

    fun showHint(): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        // Clear previous hints
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                board[row][col]?.isHint = false
            }
        }
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Check right swap
                if (col < boardSize - 1) {
                    swapCandies(row, col, row, col + 1)
                    if (findAllMatches().isNotEmpty()) {
                        swapCandies(row, col, row, col + 1)
                        board[row][col]?.isHint = true
                        board[row][col + 1]?.isHint = true
                        onBoardChanged()
                        return Pair(Pair(row, col), Pair(row, col + 1))
                    }
                    swapCandies(row, col, row, col + 1)
                }
                
                // Check down swap
                if (row < boardSize - 1) {
                    swapCandies(row, col, row + 1, col)
                    if (findAllMatches().isNotEmpty()) {
                        swapCandies(row, col, row + 1, col)
                        board[row][col]?.isHint = true
                        board[row + 1][col]?.isHint = true
                        onBoardChanged()
                        return Pair(Pair(row, col), Pair(row + 1, col))
                    }
                    swapCandies(row, col, row + 1, col)
                }
            }
        }
        return null
    }

    fun clearHints() {
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                board[row][col]?.isHint = false
            }
        }
        onBoardChanged()
    }

    private fun checkGameStatus() {
        val level = currentLevel ?: return
        
        // Check win condition
        if (gameState.score >= level.targetScore) {
            onGameWon(gameState.stars)
            return
        }
        
        // Check lose condition
        if (gameState.movesLeft <= 0) {
            if (gameState.score >= level.targetScore) {
                onGameWon(gameState.stars)
            } else {
                onGameLost()
            }
        }
    }

    fun calculateBonus(): Int {
        return (gameState.movesLeft * 20) + (gameState.stars * 100)
    }
}
