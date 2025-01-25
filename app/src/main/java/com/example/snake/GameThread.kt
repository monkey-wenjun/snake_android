package com.example.snake

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {
    
    @Volatile
    private var running = false
    private var targetFPS = 60  // Keep high FPS for smooth rendering
    private var lastUpdateTime = System.nanoTime()
    private val updateInterval = (1000000000 / 30).toLong()  // Increase update frequency to 30 times per second
    
    fun setRunning(isRunning: Boolean) {
        running = isRunning
        if (isRunning) {
            lastUpdateTime = System.nanoTime()
        }
    }
    
    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long
        val targetTime = (1000 / targetFPS).toLong()
        
        while (running) {
            startTime = System.nanoTime()
            var canvas: Canvas? = null
            
            try {
                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    // Update game logic at a fixed rate
                    val currentTime = System.nanoTime()
                    if (currentTime - lastUpdateTime >= updateInterval) {
                        gameView.update()
                        lastUpdateTime = currentTime
                    }
                    
                    // Draw as fast as possible within target FPS
                    canvas?.let { gameView.draw(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    canvas?.let {
                        surfaceHolder.unlockCanvasAndPost(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            timeMillis = (System.nanoTime() - startTime) / 1_000_000
            waitTime = targetTime - timeMillis
            
            if (waitTime > 0) {
                try {
                    sleep(waitTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
} 