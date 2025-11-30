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
    private val onNoValidMoves: () -> Unit = {},
    private val onSpecialCreated: (Int, Int, SpecialType) -> Unit = {},
    private val onBombExplosion: (List<Pair<Int, Int>>, SpecialType) -> Unit = {}
) {
    private var board: Array<Array<Candy?>> = Array(boardSize) { arrayOfNulls(boardSize) }
    private var gameState = GameState()
    private var currentLevel: Level? = null
    
    // Bomba patlamalarını takip etmek için
    private var pendingExplosions = mutableListOf<Pair<Int, Int>>()

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
    
    // Direct swap method for swipe gestures
    fun swapCandies(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        if (gameState.isAnimating) return false
        if (!isAdjacent(row1, col1, row2, col2)) return false
        
        // Clear any selection
        gameState.selectedCandy?.let { (selRow, selCol) ->
            board[selRow][selCol]?.isSelected = false
        }
        gameState.selectedCandy = null
        
        return trySwap(row1, col1, row2, col2)
    }

    private fun trySwap(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        gameState.selectedCandy = null
        gameState.isAnimating = true
        
        val candy1 = board[row1][col1]
        val candy2 = board[row2][col2]
        
        // Özel bomba kombinasyonlarını kontrol et
        if (candy1 != null && candy2 != null) {
            val special1 = candy1.specialType
            val special2 = candy2.specialType
            
            // İki özel şeker kombinasyonu - süper patlama!
            if (special1 != SpecialType.NONE && special2 != SpecialType.NONE) {
                gameState.movesLeft--
                onMovesChanged(gameState.movesLeft)
                gameState.comboCount = 0
                handleSpecialCombination(row1, col1, row2, col2)
                return true
            }
        }
        
        // Perform swap
        performSwap(row1, col1, row2, col2)
        
        val matches = findAllMatches()
        if (matches.isNotEmpty()) {
            // Valid move
            gameState.movesLeft--
            onMovesChanged(gameState.movesLeft)
            gameState.comboCount = 0
            processMatches(matches, row1, col1, row2, col2)
            return true
        } else {
            // Invalid move - swap back
            performSwap(row1, col1, row2, col2)
            gameState.isAnimating = false
            onBoardChanged()
            return false
        }
    }

    private fun performSwap(row1: Int, col1: Int, row2: Int, col2: Int) {
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

    // Eşleşme analizi - yatay mı dikey mi ve uzunluğu
    private data class MatchInfo(
        val positions: List<Pair<Int, Int>>,
        val isHorizontal: Boolean,
        val length: Int,
        val centerRow: Int,
        val centerCol: Int
    )
    
    private fun analyzeMatches(matches: Set<Pair<Int, Int>>): List<MatchInfo> {
        val result = mutableListOf<MatchInfo>()
        val processed = mutableSetOf<Pair<Int, Int>>()
        
        // Yatay eşleşmeleri bul
        for (row in 0 until boardSize) {
            var startCol = -1
            var length = 0
            for (col in 0 until boardSize) {
                val pos = Pair(row, col)
                if (matches.contains(pos) && !processed.contains(pos)) {
                    if (startCol == -1) startCol = col
                    length++
                } else if (startCol != -1 && length >= 3) {
                    val positions = (startCol until startCol + length).map { Pair(row, it) }
                    result.add(MatchInfo(
                        positions = positions,
                        isHorizontal = true,
                        length = length,
                        centerRow = row,
                        centerCol = startCol + length / 2
                    ))
                    processed.addAll(positions)
                    startCol = -1
                    length = 0
                } else {
                    startCol = -1
                    length = 0
                }
            }
            if (startCol != -1 && length >= 3) {
                val positions = (startCol until startCol + length).map { Pair(row, it) }
                result.add(MatchInfo(
                    positions = positions,
                    isHorizontal = true,
                    length = length,
                    centerRow = row,
                    centerCol = startCol + length / 2
                ))
                processed.addAll(positions)
            }
        }
        
        // Dikey eşleşmeleri bul
        for (col in 0 until boardSize) {
            var startRow = -1
            var length = 0
            for (row in 0 until boardSize) {
                val pos = Pair(row, col)
                if (matches.contains(pos) && !processed.contains(pos)) {
                    if (startRow == -1) startRow = row
                    length++
                } else if (startRow != -1 && length >= 3) {
                    val positions = (startRow until startRow + length).map { Pair(it, col) }
                    result.add(MatchInfo(
                        positions = positions,
                        isHorizontal = false,
                        length = length,
                        centerRow = startRow + length / 2,
                        centerCol = col
                    ))
                    processed.addAll(positions)
                    startRow = -1
                    length = 0
                } else {
                    startRow = -1
                    length = 0
                }
            }
            if (startRow != -1 && length >= 3) {
                val positions = (startRow until startRow + length).map { Pair(it, col) }
                result.add(MatchInfo(
                    positions = positions,
                    isHorizontal = false,
                    length = length,
                    centerRow = startRow + length / 2,
                    centerCol = col
                ))
                processed.addAll(positions)
            }
        }
        
        return result
    }
    
    // Özel şeker oluşturma mantığı
    private fun determineSpecialCandy(matchInfo: MatchInfo, swapRow1: Int, swapCol1: Int, swapRow2: Int, swapCol2: Int): Pair<Pair<Int, Int>, SpecialType>? {
        return when {
            // 5'li veya daha fazla eşleşme -> Renk Bombası
            matchInfo.length >= 5 -> {
                val pos = findSwappedPosition(matchInfo.positions, swapRow1, swapCol1, swapRow2, swapCol2)
                Pair(pos, SpecialType.COLOR_BOMB)
            }
            // 4'lü eşleşme -> Çizgili veya Paketli
            matchInfo.length == 4 -> {
                val pos = findSwappedPosition(matchInfo.positions, swapRow1, swapCol1, swapRow2, swapCol2)
                // Yatay eşleşme -> dikey çizgili, dikey eşleşme -> yatay çizgili
                val specialType = if (matchInfo.isHorizontal) SpecialType.STRIPED_V else SpecialType.STRIPED_H
                Pair(pos, specialType)
            }
            else -> null
        }
    }
    
    // L veya T şeklinde eşleşme kontrolü (Paketli bomba için)
    private fun checkForLOrTShape(matches: Set<Pair<Int, Int>>): Pair<Pair<Int, Int>, Boolean>? {
        // Her pozisyonu kontrol et
        for ((row, col) in matches) {
            var horizontalCount = 1
            var verticalCount = 1
            
            // Yatay komşuları say
            var left = col - 1
            while (left >= 0 && matches.contains(Pair(row, left))) {
                horizontalCount++
                left--
            }
            var right = col + 1
            while (right < boardSize && matches.contains(Pair(row, right))) {
                horizontalCount++
                right++
            }
            
            // Dikey komşuları say
            var top = row - 1
            while (top >= 0 && matches.contains(Pair(top, col))) {
                verticalCount++
                top--
            }
            var bottom = row + 1
            while (bottom < boardSize && matches.contains(Pair(bottom, col))) {
                verticalCount++
                bottom++
            }
            
            // L veya T şekli: hem yatay hem dikey en az 3'er tane
            if (horizontalCount >= 3 && verticalCount >= 3) {
                return Pair(Pair(row, col), true)
            }
        }
        return null
    }
    
    private fun findSwappedPosition(positions: List<Pair<Int, Int>>, swapRow1: Int, swapCol1: Int, swapRow2: Int, swapCol2: Int): Pair<Int, Int> {
        // Swap edilen pozisyonlardan biri eşleşmenin içindeyse onu döndür
        val swap1 = Pair(swapRow1, swapCol1)
        val swap2 = Pair(swapRow2, swapCol2)
        
        if (positions.contains(swap1)) return swap1
        if (positions.contains(swap2)) return swap2
        
        // Hiçbiri yoksa ortadaki pozisyonu döndür
        return positions[positions.size / 2]
    }

    private fun processMatches(matches: Set<Pair<Int, Int>>, swapRow1: Int = -1, swapCol1: Int = -1, swapRow2: Int = -1, swapCol2: Int = -1) {
        gameState.comboCount++
        
        if (gameState.comboCount > 1) {
            onCombo(gameState.comboCount)
        }
        
        // Bomba patlamalarını işle
        val allExplosions = mutableSetOf<Pair<Int, Int>>()
        allExplosions.addAll(matches)
        
        // Eşleşen özel şekerlerin patlamalarını ekle
        for ((row, col) in matches) {
            val candy = board[row][col] ?: continue
            if (candy.specialType != SpecialType.NONE) {
                val explosionPositions = triggerSpecialCandy(row, col, candy.specialType)
                allExplosions.addAll(explosionPositions)
                onBombExplosion(explosionPositions, candy.specialType)
            }
        }
        
        // L veya T şeklinde eşleşme kontrolü (Paketli bomba oluşturma)
        val lOrTShape = checkForLOrTShape(matches)
        
        // Eşleşmeleri analiz et ve özel şeker oluştur
        val matchInfos = analyzeMatches(matches)
        var specialCandyCreated: Triple<Int, Int, SpecialType>? = null
        
        if (lOrTShape != null) {
            // L veya T şeklinde eşleşme -> Paketli bomba
            val (pos, _) = lOrTShape
            specialCandyCreated = Triple(pos.first, pos.second, SpecialType.WRAPPED)
        } else {
            // En uzun eşleşmeyi bul
            val longestMatch = matchInfos.maxByOrNull { it.length }
            if (longestMatch != null) {
                val special = determineSpecialCandy(longestMatch, swapRow1, swapCol1, swapRow2, swapCol2)
                if (special != null) {
                    specialCandyCreated = Triple(special.first.first, special.first.second, special.second)
                }
            }
        }
        
        // Calculate score
        val baseScore = 10
        var score = allExplosions.size * baseScore
        
        // Match bonus
        when {
            allExplosions.size >= 10 -> score += 500  // Büyük patlama bonusu
            allExplosions.size >= 6 -> score += 200
            allExplosions.size >= 5 -> score += 100
            allExplosions.size >= 4 -> score += 50
        }
        
        // Özel şeker bonusu
        val specialCount = matches.count { (r, c) -> board[r][c]?.specialType != SpecialType.NONE }
        score += specialCount * 50
        
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
        
        // Mark matched candies (özel şeker oluşturulacak pozisyon hariç)
        allExplosions.forEach { (row, col) ->
            if (specialCandyCreated == null || 
                row != specialCandyCreated.first || col != specialCandyCreated.second) {
                board[row][col]?.isMatched = true
            }
        }
        
        // Özel şekeri oluştur
        specialCandyCreated?.let { (row, col, specialType) ->
            val candy = board[row][col]
            if (candy != null) {
                candy.specialType = specialType
                candy.isMatched = false
                onSpecialCreated(row, col, specialType)
            }
        }
        
        onMatchFound(allExplosions.toList())
        
        // Remove matches and refill (will be called with animation delay)
    }
    
    // Özel şeker patlatma fonksiyonu
    private fun triggerSpecialCandy(row: Int, col: Int, specialType: SpecialType): Set<Pair<Int, Int>> {
        val explosions = mutableSetOf<Pair<Int, Int>>()
        
        when (specialType) {
            SpecialType.STRIPED_H -> {
                // Tüm satırı patlat
                for (c in 0 until boardSize) {
                    explosions.add(Pair(row, c))
                }
            }
            SpecialType.STRIPED_V -> {
                // Tüm sütunu patlat
                for (r in 0 until boardSize) {
                    explosions.add(Pair(r, col))
                }
            }
            SpecialType.WRAPPED -> {
                // 3x3 alanı patlat (bomba efekti)
                for (r in maxOf(0, row - 1)..minOf(boardSize - 1, row + 1)) {
                    for (c in maxOf(0, col - 1)..minOf(boardSize - 1, col + 1)) {
                        explosions.add(Pair(r, c))
                    }
                }
            }
            SpecialType.COLOR_BOMB -> {
                // Rastgele bir renkteki tüm şekerleri patlat
                val candy = board[row][col]
                if (candy != null) {
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            if (board[r][c]?.type == candy.type) {
                                explosions.add(Pair(r, c))
                            }
                        }
                    }
                }
            }
            SpecialType.NONE -> { }
        }
        
        // Zincirleme patlamaları kontrol et
        val chainExplosions = mutableSetOf<Pair<Int, Int>>()
        for ((r, c) in explosions) {
            val targetCandy = board[r][c]
            if (targetCandy != null && targetCandy.specialType != SpecialType.NONE 
                && Pair(r, c) != Pair(row, col)) {
                chainExplosions.addAll(triggerSpecialCandy(r, c, targetCandy.specialType))
            }
        }
        explosions.addAll(chainExplosions)
        
        return explosions
    }
    
    // İki özel şeker kombinasyonu
    private fun handleSpecialCombination(row1: Int, col1: Int, row2: Int, col2: Int) {
        val candy1 = board[row1][col1] ?: return
        val candy2 = board[row2][col2] ?: return
        val special1 = candy1.specialType
        val special2 = candy2.specialType
        
        val allExplosions = mutableSetOf<Pair<Int, Int>>()
        allExplosions.add(Pair(row1, col1))
        allExplosions.add(Pair(row2, col2))
        
        // Kombinasyon türlerine göre süper patlamalar
        when {
            // Renk bombası + Renk bombası = Tüm tahtayı temizle!
            special1 == SpecialType.COLOR_BOMB && special2 == SpecialType.COLOR_BOMB -> {
                for (r in 0 until boardSize) {
                    for (c in 0 until boardSize) {
                        allExplosions.add(Pair(r, c))
                    }
                }
            }
            // Renk bombası + Herhangi özel = O renkteki tüm şekerleri özel yap
            special1 == SpecialType.COLOR_BOMB || special2 == SpecialType.COLOR_BOMB -> {
                val colorBombPos = if (special1 == SpecialType.COLOR_BOMB) Pair(row1, col1) else Pair(row2, col2)
                val otherPos = if (special1 == SpecialType.COLOR_BOMB) Pair(row2, col2) else Pair(row1, col1)
                val otherCandy = board[otherPos.first][otherPos.second] ?: return
                val targetType = otherCandy.type
                
                for (r in 0 until boardSize) {
                    for (c in 0 until boardSize) {
                        if (board[r][c]?.type == targetType) {
                            allExplosions.add(Pair(r, c))
                            // Ayrıca özel şeker patlamalarını da tetikle
                            if (otherCandy.specialType != SpecialType.COLOR_BOMB) {
                                allExplosions.addAll(triggerSpecialCandy(r, c, otherCandy.specialType))
                            }
                        }
                    }
                }
            }
            // Paketli + Paketli = 5x5 mega patlama
            special1 == SpecialType.WRAPPED && special2 == SpecialType.WRAPPED -> {
                val centerRow = (row1 + row2) / 2
                val centerCol = (col1 + col2) / 2
                for (r in maxOf(0, centerRow - 2)..minOf(boardSize - 1, centerRow + 2)) {
                    for (c in maxOf(0, centerCol - 2)..minOf(boardSize - 1, centerCol + 2)) {
                        allExplosions.add(Pair(r, c))
                    }
                }
            }
            // Çizgili + Çizgili = Artı şeklinde patlama (satır + sütun)
            (special1 == SpecialType.STRIPED_H || special1 == SpecialType.STRIPED_V) &&
            (special2 == SpecialType.STRIPED_H || special2 == SpecialType.STRIPED_V) -> {
                // Her iki yönde de patlat
                for (c in 0 until boardSize) {
                    allExplosions.add(Pair(row1, c))
                    allExplosions.add(Pair(row2, c))
                }
                for (r in 0 until boardSize) {
                    allExplosions.add(Pair(r, col1))
                    allExplosions.add(Pair(r, col2))
                }
            }
            // Paketli + Çizgili = 3 satır ve 3 sütun patlat
            (special1 == SpecialType.WRAPPED && (special2 == SpecialType.STRIPED_H || special2 == SpecialType.STRIPED_V)) ||
            (special2 == SpecialType.WRAPPED && (special1 == SpecialType.STRIPED_H || special1 == SpecialType.STRIPED_V)) -> {
                val wrappedPos = if (special1 == SpecialType.WRAPPED) Pair(row1, col1) else Pair(row2, col2)
                // 3 satır patlat
                for (r in maxOf(0, wrappedPos.first - 1)..minOf(boardSize - 1, wrappedPos.first + 1)) {
                    for (c in 0 until boardSize) {
                        allExplosions.add(Pair(r, c))
                    }
                }
                // 3 sütun patlat
                for (c in maxOf(0, wrappedPos.second - 1)..minOf(boardSize - 1, wrappedPos.second + 1)) {
                    for (r in 0 until boardSize) {
                        allExplosions.add(Pair(r, c))
                    }
                }
            }
        }
        
        onBombExplosion(allExplosions.toList(), SpecialType.WRAPPED)
        
        // Puanı hesapla
        val score = allExplosions.size * 20 // Süper patlama için ekstra puan
        gameState.score += score
        onScoreChanged(gameState.score)
        
        // Yıldızları güncelle
        currentLevel?.let { level ->
            val newStars = level.starThresholds.count { gameState.score >= it }
            if (newStars > gameState.stars) {
                gameState.stars = newStars
            }
        }
        
        // Eşleşmeleri işaretle
        allExplosions.forEach { (row, col) ->
            board[row][col]?.isMatched = true
        }
        
        onMatchFound(allExplosions.toList())
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
                    performSwap(row, col, row, col + 1)
                    if (findAllMatches().isNotEmpty()) {
                        performSwap(row, col, row, col + 1)
                        return true
                    }
                    performSwap(row, col, row, col + 1)
                }
                
                // Check down swap
                if (row < boardSize - 1) {
                    performSwap(row, col, row + 1, col)
                    if (findAllMatches().isNotEmpty()) {
                        performSwap(row, col, row + 1, col)
                        return true
                    }
                    performSwap(row, col, row + 1, col)
                }
            }
        }
        return false
    }

    fun shuffleBoard() {
        val types = mutableListOf<CandyType>()
        val specials = mutableListOf<SpecialType>()
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                board[row][col]?.let { 
                    types.add(it.type) 
                    specials.add(it.specialType)
                }
            }
        }
        types.shuffle()
        specials.shuffle()
        
        var index = 0
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                board[row][col]?.let {
                    it.type = types[index]
                    it.specialType = specials[index]
                    index++
                }
            }
        }
        
        // Ensure no matches and valid moves exist
        while (findAllMatches().isNotEmpty() || !hasValidMoves()) {
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    board[row][col]?.let {
                        it.type = CandyType.random()
                        // Özel şekerleri koru
                    }
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
                    performSwap(row, col, row, col + 1)
                    if (findAllMatches().isNotEmpty()) {
                        performSwap(row, col, row, col + 1)
                        board[row][col]?.isHint = true
                        board[row][col + 1]?.isHint = true
                        onBoardChanged()
                        return Pair(Pair(row, col), Pair(row, col + 1))
                    }
                    performSwap(row, col, row, col + 1)
                }
                
                // Check down swap
                if (row < boardSize - 1) {
                    performSwap(row, col, row + 1, col)
                    if (findAllMatches().isNotEmpty()) {
                        performSwap(row, col, row + 1, col)
                        board[row][col]?.isHint = true
                        board[row + 1][col]?.isHint = true
                        onBoardChanged()
                        return Pair(Pair(row, col), Pair(row + 1, col))
                    }
                    performSwap(row, col, row + 1, col)
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
