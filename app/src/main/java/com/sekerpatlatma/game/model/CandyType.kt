package com.sekerpatlatma.game.model

import androidx.annotation.ColorRes
import com.sekerpatlatma.game.R

enum class CandyType(
    val emoji: String,
    @ColorRes val colorRes: Int
) {
    RED("🍬", R.color.candy_red),
    BLUE("🍭", R.color.candy_blue),
    GREEN("🍫", R.color.candy_green),
    YELLOW("🧁", R.color.candy_yellow),
    PURPLE("🍩", R.color.candy_purple),
    ORANGE("🍪", R.color.candy_orange);

    companion object {
        fun random(): CandyType = entries.random()
        
        // Normal şeker türlerini döndür
        fun normalTypes(): List<CandyType> = entries.toList()
    }
}
