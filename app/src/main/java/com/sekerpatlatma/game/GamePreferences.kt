package com.sekerpatlatma.game

import android.content.Context
import android.content.SharedPreferences
import com.sekerpatlatma.game.model.GameProgress

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("SekerPatlatma", Context.MODE_PRIVATE)

    var totalStars: Int
        get() = prefs.getInt("totalStars", 0)
        set(value) = prefs.edit().putInt("totalStars", value).apply()

    var maxUnlockedLevel: Int
        get() = prefs.getInt("maxUnlockedLevel", 1)
        set(value) = prefs.edit().putInt("maxUnlockedLevel", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("soundEnabled", true)
        set(value) = prefs.edit().putBoolean("soundEnabled", value).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean("musicEnabled", true)
        set(value) = prefs.edit().putBoolean("musicEnabled", value).apply()

    var animationsEnabled: Boolean
        get() = prefs.getBoolean("animationsEnabled", true)
        set(value) = prefs.edit().putBoolean("animationsEnabled", value).apply()

    fun getLevelStars(level: Int): Int {
        return prefs.getInt("level_${level}_stars", 0)
    }

    fun setLevelStars(level: Int, stars: Int) {
        val currentStars = getLevelStars(level)
        if (stars > currentStars) {
            totalStars += (stars - currentStars)
            prefs.edit().putInt("level_${level}_stars", stars).apply()
        }
    }

    fun getGameProgress(): GameProgress {
        val levelStars = mutableMapOf<Int, Int>()
        for (i in 1..50) {
            val stars = getLevelStars(i)
            if (stars > 0) levelStars[i] = stars
        }
        return GameProgress(
            totalStars = totalStars,
            maxUnlockedLevel = maxUnlockedLevel,
            levelStars = levelStars,
            soundEnabled = soundEnabled,
            musicEnabled = musicEnabled,
            animationsEnabled = animationsEnabled
        )
    }

    fun unlockNextLevel(currentLevel: Int) {
        if (currentLevel >= maxUnlockedLevel && currentLevel < 50) {
            maxUnlockedLevel = currentLevel + 1
        }
    }
}
