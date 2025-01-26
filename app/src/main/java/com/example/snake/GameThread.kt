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
        private const val TARGET_FPS = 60
        private const val TARGET_FRAME_TIME = (1000000000L / TARGET_FPS)  // in nanoseconds
    }
    
    @Volatile
    private var running = false
    
    fun setRunning(isRunning: Boolean) {
        running = isRunning
        if (!isRunning) {
            interrupt()
        }
    }
    
    override fun run() {
        var lastTime = System.nanoTime()
        var currentTime: Long
        var deltaTime: Long
        var frameCount = 0
        var lastFPSTime = lastTime
        
        while (running && !isInterrupted) {
            currentTime = System.nanoTime()
            deltaTime = currentTime - lastTime
            
            if (deltaTime >= TARGET_FRAME_TIME) {
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    synchronized(surfaceHolder) {
                        gameView.update()
                        if (canvas != null) {
                            gameView.draw(canvas)
                        }
                    }
                    
                    // FPS calculation
                    frameCount++
                    if (currentTime - lastFPSTime >= 1000000000L) {  // Every second
                        Log.d(TAG, "FPS: $frameCount")
                        frameCount = 0
                        lastFPSTime = currentTime
                    }
                    
                    lastTime = currentTime
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
            }
            
            // Sleep to save CPU
            val sleepTime = (TARGET_FRAME_TIME - (System.nanoTime() - currentTime)) / 1000000L
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