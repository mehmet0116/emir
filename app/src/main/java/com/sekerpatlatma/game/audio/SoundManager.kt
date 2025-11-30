package com.sekerpatlatma.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val sounds = mutableMapOf<SoundType, Int>()
    private var soundEnabled = true

    enum class SoundType {
        SELECT, MATCH, INVALID, STAR, WIN, LOSE, HINT, SHUFFLE, COMBO
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // We'll generate sounds programmatically using ToneGenerator instead of loading files
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun playSound(type: SoundType) {
        if (!soundEnabled) return
        
        // For now, we'll use the sound pool with generated tones
        // In a production app, you would load actual sound files
        sounds[type]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
