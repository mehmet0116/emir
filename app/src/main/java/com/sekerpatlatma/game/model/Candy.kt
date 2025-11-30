package com.sekerpatlatma.game.model

data class Candy(
    var type: CandyType,
    var row: Int,
    var col: Int,
    var isMatched: Boolean = false,
    var isSelected: Boolean = false,
    var isHint: Boolean = false
) {
    fun copy(): Candy = Candy(type, row, col, isMatched, isSelected, isHint)
}
