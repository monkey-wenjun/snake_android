package com.example.snake

import android.graphics.Canvas
import android.view.SurfaceHolder
import android.util.Log

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {
    
    companion object {
        private const val TAG = "GameThread"
        private const val TARGET_FPS = 60  // Keep rendering at 60 FPS for smooth graphics
        private const val UPDATE_FPS = 15  // But update game logic at 15 FPS for slower movement
        private const val MAX_FRAME_SKIPS = 5
        private const val FRAME_PERIOD = (1000.0 / TARGET_FPS).toLong()
        private const val UPDATE_PERIOD = (1000.0 / UPDATE_FPS).toLong()
    }
    
    @Volatile
    private var running = false
    private var lastTime = System.nanoTime()
    private var lastUpdateTime = System.nanoTime()
    
    fun setRunning(isRunning: Boolean) {
        running = isRunning
        if (!isRunning) {
            interrupt()
        }
    }
    
    override fun run() {
        var canvas: Canvas? = null
        var frameSkips = 0
        
        while (running && !isInterrupted) {
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastTime) / 1_000_000L // Convert to milliseconds
            val updateDeltaTime = (currentTime - lastUpdateTime) / 1_000_000L
            
            try {
                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    if (canvas != null) {
                        // Update game logic at a slower rate
                        if (updateDeltaTime >= UPDATE_PERIOD) {
                            gameView.update()
                            lastUpdateTime = currentTime
                        }
                        
                        // Render at full frame rate
                        if (deltaTime >= FRAME_PERIOD || frameSkips >= MAX_FRAME_SKIPS) {
                            gameView.draw(canvas)
                            frameSkips = 0
                            lastTime = currentTime
                        } else {
                            frameSkips++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in game loop: ${e.message}")
                running = false
            } finally {
                try {
                    canvas?.let { surfaceHolder.unlockCanvasAndPost(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error posting canvas: ${e.message}")
                    running = false
                }
            }
            
            val sleepTime = FRAME_PERIOD - deltaTime
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime)
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Thread interrupted")
                    running = false
                }
            }
        }
    }
} 