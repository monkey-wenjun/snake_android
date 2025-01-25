package com.example.snake

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

class SoundManager(context: Context) {
    companion object {
        private const val TAG = "SoundManager"
    }

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<Char, Int>()

    init {
        // Create AudioAttributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Create SoundPool
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        try {
            // Load number sounds (0-9)
            for (i in 0..9) {
                try {
                    val resourceId = context.resources.getIdentifier(
                        "sound_$i", "raw", context.packageName)
                    if (resourceId != 0) {
                        val soundId = soundPool.load(context, resourceId, 1)
                        soundMap[i.toString()[0]] = soundId
                        Log.d(TAG, "Loaded sound for number: $i")
                    } else {
                        Log.e(TAG, "Could not find sound resource for number: $i")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading sound for number $i: ${e.message}")
                }
            }

            // Load letter sounds (A-Z)
            for (c in 'A'..'Z') {
                try {
                    val resourceId = context.resources.getIdentifier(
                        "sound_${c.lowercase()}", "raw", context.packageName)
                    if (resourceId != 0) {
                        val soundId = soundPool.load(context, resourceId, 1)
                        soundMap[c] = soundId
                        soundMap[c.lowercaseChar()] = soundId  // Use same sound for lowercase
                        Log.d(TAG, "Loaded sound for letter: $c")
                    } else {
                        Log.e(TAG, "Could not find sound resource for letter: $c")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading sound for letter $c: ${e.message}")
                }
            }

            Log.d(TAG, "Successfully loaded ${soundMap.size} sounds")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing sounds: ${e.message}")
        }
    }

    fun playSound(character: Char) {
        try {
            soundMap[character]?.let { soundId ->
                val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                if (streamId == 0) {
                    Log.e(TAG, "Failed to play sound for character: $character")
                } else {
                    Log.d(TAG, "Playing sound for character: $character (stream: $streamId)")
                }
            } ?: Log.e(TAG, "No sound loaded for character: $character")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound for character $character: ${e.message}")
        }
    }

    fun release() {
        try {
            soundPool.release()
            Log.d(TAG, "SoundPool released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SoundPool: ${e.message}")
        }
    }
} 