package com.sekerpatlatma.game.model

data class GameState(
    var currentLevel: Int = 1,
    var score: Int = 0,
    var movesLeft: Int = 30,
    var targetScore: Int = 1000,
    var stars: Int = 0,
    var comboCount: Int = 0,
    var isAnimating: Boolean = false,
    var selectedCandy: Pair<Int, Int>? = null
)

data class GameProgress(
    var totalStars: Int = 0,
    var maxUnlockedLevel: Int = 1,
    var levelStars: MutableMap<Int, Int> = mutableMapOf(),
    var soundEnabled: Boolean = true,
    var musicEnabled: Boolean = true,
    var animationsEnabled: Boolean = true
)
