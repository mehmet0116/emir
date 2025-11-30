package com.sekerpatlatma.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(context: Context) {
    private var soundEnabled = true
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO)

    enum class SoundType {
        SELECT, MATCH, INVALID, STAR, WIN, LOSE, HINT, SHUFFLE, COMBO
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun playSound(type: SoundType) {
        if (!soundEnabled) return
        
        scope.launch {
            when (type) {
                SoundType.SELECT -> playTone(880, 50, 0.3f)
                SoundType.MATCH -> playMatchSound()
                SoundType.INVALID -> playInvalidSound()
                SoundType.STAR -> playStarSound()
                SoundType.WIN -> playWinSound()
                SoundType.LOSE -> playLoseSound()
                SoundType.HINT -> playTone(660, 100, 0.4f)
                SoundType.SHUFFLE -> playShuffleSound()
                SoundType.COMBO -> playComboSound()
            }
        }
    }

    private fun playTone(frequency: Int, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = if (i < numSamples / 10) {
                i.toFloat() / (numSamples / 10)
            } else if (i > numSamples * 9 / 10) {
                (numSamples - i).toFloat() / (numSamples / 10)
            } else {
                1f
            }
            samples[i] = (sin(angle) * Short.MAX_VALUE * volume * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private fun playMatchSound() {
        val sampleRate = 44100
        val durationMs = 150
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val frequency = 440 + (progress * 440).toInt()
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = 1f - progress * 0.5f
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.4f * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private fun playInvalidSound() {
        val sampleRate = 44100
        val durationMs = 100
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val frequency = 200 - (progress * 50).toInt()
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = 1f - progress
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.3f * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private fun playStarSound() {
        val sampleRate = 44100
        val durationMs = 200
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        val frequencies = listOf(523, 659, 784, 1046)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freqIndex = (progress * frequencies.size).toInt().coerceIn(0, frequencies.size - 1)
            val frequency = frequencies[freqIndex]
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = if (progress < 0.1f) progress / 0.1f else 1f - (progress - 0.1f) * 0.5f
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.5f * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private fun playWinSound() {
        val sampleRate = 44100
        val durationMs = 400
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        val notes = listOf(523, 659, 784, 1046, 1318)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val noteIndex = (progress * notes.size).toInt().coerceIn(0, notes.size - 1)
            val frequency = notes[noteIndex]
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = 0.6f * (1f - progress * 0.3f)
            samples[i] = (sin(angle) * Short.MAX_VALUE * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private fun playLoseSound() {
        val sampleRate = 44100
        val durationMs = 300
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val frequency = (400 - progress * 200).toInt()
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = 1f - progress
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.4f * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private fun playShuffleSound() {
        val sampleRate = 44100
        val durationMs = 200
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val frequency = (300 + (progress * 500 * kotlin.math.sin(progress * 20))).toInt().coerceIn(200, 800)
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = 0.5f - progress * 0.3f
            samples[i] = (sin(angle) * Short.MAX_VALUE * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private fun playComboSound() {
        val sampleRate = 44100
        val durationMs = 150
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val frequency = (600 + progress * 600).toInt()
            val angle = 2.0 * PI * i / (sampleRate.toDouble() / frequency)
            val envelope = if (progress < 0.2f) progress / 0.2f else 1f - (progress - 0.2f) * 0.8f
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.5f * envelope).toInt().toShort()
        }
        
        playShortArraySound(samples, sampleRate)
    }

    private suspend fun playShortArraySound(samples: ShortArray, sampleRate: Int) {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(samples.size * 2, minBufferSize))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            // Use delay for coroutines
            kotlinx.coroutines.delay((samples.size * 1000L / sampleRate) + 50)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: IllegalStateException) {
            android.util.Log.w("SoundManager", "AudioTrack state error: ${e.message}")
        } catch (e: IllegalArgumentException) {
            android.util.Log.w("SoundManager", "AudioTrack argument error: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Unexpected audio error", e)
        }
    }

    fun release() {
        // No longer using SoundPool, cleanup is handled per-sound
    }
}
