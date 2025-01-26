package com.example.snake

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val GAME_TIME_LIMIT_MS = 5 * 60 * 1000L // 5 minutes
        private const val WARNING_INTERVAL_MS = 60 * 1000L // Show warning every minute
    }

    private var gameView: GameView? = null
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private val handler = Handler(Looper.getMainLooper())
    private var gameStartTime = 0L
    private var isGameLocked = false

    private val gameTimeRunnable = object : Runnable {
        override fun run() {
            val elapsedTime = System.currentTimeMillis() - gameStartTime
            val remainingTime = GAME_TIME_LIMIT_MS - elapsedTime
            
            when {
                remainingTime <= 0 -> {
                    if (!isGameLocked) {
                        lockGame()
                    }
                }
                remainingTime <= WARNING_INTERVAL_MS -> {
                    // Show warning in last minute
                    showTimeWarning(remainingTime)
                    handler.postDelayed(this, 15000) // Check every 15 seconds in last minute
                }
                else -> {
                    // Regular interval check
                    handler.postDelayed(this, WARNING_INTERVAL_MS)
                }
            }
        }
    }

    private fun showTimeWarning(remainingTime: Long) {
        val remainingSeconds = (remainingTime / 1000).toInt()
        Toast.makeText(
            this,
            "小朋友，还剩 ${remainingSeconds} 秒游戏时间哦，请注意休息~",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun lockGame() {
        isGameLocked = true
        gameView?.pauseGame()
        
        AlertDialog.Builder(this)
            .setTitle("温馨提醒")
            .setMessage("小朋友，你已经玩了5分钟啦！\n该休息一下，保护眼睛了哦！")
            .setCancelable(false)
            .setPositiveButton("好的，我去休息") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        
        try {
            // Enable edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Configure window insets controller
            windowInsetsController = WindowInsetsControllerCompat(window, window.decorView).apply {
                // Hide system bars
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            // Keep screen on
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            gameView = GameView(this)
            setContentView(gameView)
            
            // Start tracking game time
            gameStartTime = System.currentTimeMillis()
            handler.post(gameTimeRunnable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "游戏启动出错，请重试", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "Window focus changed: $hasFocus")
        try {
            if (hasFocus) {
                // Re-hide system bars when focus is regained
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                gameView?.pauseGame()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onWindowFocusChanged: ${e.message}", e)
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        try {
            super.onPause()
            gameView?.pauseGame()
            handler.removeCallbacks(gameTimeRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause: ${e.message}", e)
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        try {
            super.onResume()
            if (!isGameLocked) {
                // Add a slight delay before resuming to ensure surface is ready
                gameView?.postDelayed({
                    gameView?.resumeGame()
                }, 100)
                handler.post(gameTimeRunnable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}", e)
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        try {
            super.onStop()
            gameView?.pauseGame()
            handler.removeCallbacks(gameTimeRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStop: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        try {
            super.onDestroy()
            handler.removeCallbacks(gameTimeRunnable)
            gameView?.cleanup()
            gameView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
    }

    override fun finish() {
        Log.d(TAG, "finish")
        try {
            gameView?.cleanup()
            handler.removeCallbacks(gameTimeRunnable)
            super.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error in finish: ${e.message}", e)
        }
    }
}