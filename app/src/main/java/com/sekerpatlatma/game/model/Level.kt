package com.sekerpatlatma.game.model

data class Level(
    val number: Int,
    val targetScore: Int,
    val moves: Int,
    val starThresholds: List<Int> // [1 star, 2 stars, 3 stars]
) {
    companion object {
        fun generateLevels(count: Int = 50): List<Level> {
            return (1..count).map { i ->
                val targetScore = 500 + (i * 300) + (i / 5) * 500
                val moves = maxOf(15, 35 - i / 3)
                Level(
                    number = i,
                    targetScore = targetScore,
                    moves = moves,
                    starThresholds = listOf(
                        500 + (i * 300),
                        700 + (i * 400),
                        1000 + (i * 500)
                    )
                )
            }
        }
    }
}
