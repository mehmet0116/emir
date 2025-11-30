package com.sekerpatlatma.game.model

// Özel şeker türleri
enum class SpecialType {
    NONE,           // Normal şeker
    STRIPED_H,      // Yatay çizgili (tüm satırı patlatır)
    STRIPED_V,      // Dikey çizgili (tüm sütunu patlatır)
    WRAPPED,        // Paketli bomba (3x3 alan patlatır)
    COLOR_BOMB      // Renk bombası (aynı renkteki tüm şekerleri patlatır)
}

data class Candy(
    var type: CandyType,
    var row: Int,
    var col: Int,
    var isMatched: Boolean = false,
    var isSelected: Boolean = false,
    var isHint: Boolean = false,
    var specialType: SpecialType = SpecialType.NONE
) {
    fun copy(): Candy = Candy(type, row, col, isMatched, isSelected, isHint, specialType)
    
    // Özel şeker mi kontrol et
    fun isSpecial(): Boolean = specialType != SpecialType.NONE
    
    // Özel şeker emojisini al
    fun getDisplayEmoji(): String {
        return when (specialType) {
            SpecialType.STRIPED_H -> "↔️"
            SpecialType.STRIPED_V -> "↕️"
            SpecialType.WRAPPED -> "💣"
            SpecialType.COLOR_BOMB -> "🌈"
            SpecialType.NONE -> type.emoji
        }
    }
}
